#include "pin.H"
#include <iostream>
#include <sys/time.h>
#include <gnu/lib-names.h>
#include <map>

#include "bin_journal.h"
#include "net.h"

/* ===================================================================== */
/* Configs                                                               */
/* ===================================================================== */

//#define DEBUG_INFO

/* ===================================================================== */
/* Debug defines to logging control                                      */
/* ===================================================================== */

#ifdef DEBUG_INFO
#define LOG_INFO(...) LogInfo(__VA_ARGS__)
#define LOG_ERR(...) LogError(__VA_ARGS__)
#else
#define LOG_INFO(...)
#define LOG_ERR(...)
#endif /* DEBUG_INFO */

/* ===================================================================== */
/* Log functions                                                         */
/* ===================================================================== */

void LogInfo(char msg[]) {
  cout << msg << endl;
}

void LogError(char msg[]) {
  cerr << "ERROR: " << msg << endl;
}

/* ===================================================================== */
/* Declarations regarding to replaced functions and libraries            */
/* ===================================================================== */

const char* __LIBC_START_MAIN = "__libc_start_main";
typedef int ( *TYPE_FP_LIBC_START_MAIN)( int (*main) (int, char **, char **), int argc, char **argv,
        __typeof (main) init, void (*fini) (void), void (*rtld_fini) (void), void *stack_end );

const char* MALLOC    = "malloc";
typedef VOID * ( *TYPE_FP_MALLOC )( size_t );

const char* CALLOC    = "calloc";
typedef VOID * ( *TYPE_FP_CALLOC )( size_t, size_t );

const char* REALLOC    = "realloc";
typedef VOID * ( *TYPE_FP_REALLOC )( void*, size_t );

const char* FREE      = "free";
typedef VOID ( *TYPE_FP_FREE )( size_t );

const char* OPERATOR_NEW1    = "operator new";
const char* OPERATOR_NEW2    = "operator new[]";
typedef VOID * ( *TYPE_FP_OPERATOR_NEW )( size_t );

const char* FINALIZE         = "__cxa_finalize";
typedef VOID ( *TYPE_FP_FINALIZE )( void* );

const char* BACKTRACE = "backtrace";
int (*p_backtrace) ( void**, int ) = NULL; //just a pointer to the backtrace

#ifdef DEBUG_INFO
const char* BACKTRACE_SYMBOLS = "backtrace_symbols";
char** (*p_backtrace_symbols) ( void* const*, int ); //just a pointer to the backtrace_symbols
#endif

/* ===================================================================== */
/* Global variables                                                      */
/* ===================================================================== */

const uint32_t MAX_STR_LEN = 512U;

PIN_MUTEX threadLockMutex;
map<OS_THREAD_ID, bool> mfQueue;

// Shared data
bool shared_bInsideMain = false;

PIN_MUTEX shared_mutex;
type_long shared_last_time;
struct timeval shared_start_time;
Net* shared_transmitter;
BinJournal* shared_bin_journal;
map<void*, bool> shared_memory_map;

KNOB<string> KnobOutputFile(KNOB_MODE_WRITEONCE, "pintool",
  "o", "memory_trace_net.out", "specify trace file name");

/* ===================================================================== */
/* Functions regarding to parsing input arguments                        */
/* ===================================================================== */

int ParseToIP(int argc, char* argv[], int& ip) {
  int argNum;
  char s_ip[] = "ip=";
  char* p;
  in_addr tmp;
  tmp.s_addr = -1;
  for (argNum = 0; argNum < argc; argNum++) {
    p = strstr(argv[argNum], s_ip);
    if (p != NULL) {
      inet_pton(AF_INET, p + strlen(s_ip), &tmp);
      break;
    }
  }
  ip = tmp.s_addr;
  return argNum;
}

int ParseToPort(int argc, char* argv[], int& port) {
  int argNum;
  char s_port[] = "port=";
  char* p;
  port = -1;
  for (argNum = 0; argNum < argc; argNum++) {
    p = strstr(argv[argNum], s_port);
    if (p != NULL) {
      port = atoi(p + strlen(s_port));
      break;
    }
  }
  return argNum;
}

/* ===================================================================== */
/* Functions regarding to QUEUE with thread flags                        */
/* ===================================================================== */

bool IsThreadLocked(const map<OS_THREAD_ID, bool>& threadList) {
  PIN_MutexLock(&threadLockMutex);
  const bool bResult = (threadList.find(PIN_GetTid()) != threadList.end());
  PIN_MutexUnlock(&threadLockMutex);
  return bResult;
}

void LockThread(map<OS_THREAD_ID, bool>& threadList) {
  if (IsThreadLocked(threadList)) {
    PIN_ExitProcess(1234);
  }
  PIN_MutexLock(&threadLockMutex);
  threadList[PIN_GetTid()] = true;
  PIN_MutexUnlock(&threadLockMutex);
}

void UnlockThread(map<OS_THREAD_ID, bool>& threadList) {
  if (!IsThreadLocked(threadList)) {
    PIN_ExitProcess(1234);
  }
  PIN_MutexLock(&threadLockMutex);
  threadList.erase(threadList.find(PIN_GetTid()));
  PIN_MutexUnlock(&threadLockMutex);
}

/* ===================================================================== */
/* Supported functions.                                                  */
/* ===================================================================== */

void UpdateLastTimeInMilisec(type_long& out_last_time) {
  struct timeval current_time;
  gettimeofday(&current_time, NULL);
  type_long current_time_in_ms = (current_time.tv_sec - shared_start_time.tv_sec) * 1000000
    + (current_time.tv_usec - shared_start_time.tv_usec);
  if (shared_last_time >= current_time_in_ms){
    shared_last_time += 1;
  }
  else {
    shared_last_time = current_time_in_ms;
  }
  out_last_time = shared_last_time;
}

/* ===================================================================== */
/* Replacement routines                                                  */
/* ===================================================================== */

const byte mallocTypes[4] = { TCODE_PTR, TCODE_SIZE_T, TCODE_PTR, TCODE_LONG };

struct AllocationInfo {
  type_ptr returned_value;
  type_size_t argument;
  type_ptr backtrace_pointer;
  type_long time;
};

bool IsLibraryAddress(const void* pAddress)
{
  const uint64_t THRESHOLD = 0xFFFFFFFFU;
  return ((uint64_t)pAddress > THRESHOLD);
}

bool GetBacktracePointer(type_ptr& bactrace_pointer)
{
  const uint32_t MAXIMAL_BUFFER_SIZE = 5U;
  uint32_t NEEDED_INDEX = 4U;
  bool bResult = false;
  if (p_backtrace != NULL) {
    void* buffer[MAXIMAL_BUFFER_SIZE];
    LockThread(mfQueue);
    // There is malloc in the backtrace function, it matters to use
    // usual malloc inside otherwise invocation is recursived
    p_backtrace(buffer, MAXIMAL_BUFFER_SIZE);
#ifdef DEBUG_INFO
    char** symbols = p_backtrace_symbols(buffer, MAXIMAL_BUFFER_SIZE);
    for (uint32_t i = 0; i < MAXIMAL_BUFFER_SIZE; i++) {
      LOG_INFO(symbols[i]);
    }
    LOG_INFO((char*)"\r\n");
#endif
    UnlockThread(mfQueue);
    if (!IsLibraryAddress(buffer[NEEDED_INDEX])) { 
      bactrace_pointer = (type_ptr)buffer[NEEDED_INDEX];
      bResult = true;
    }
  }
  return bResult;
}

void HandleAllocation(const VOID * v, const size_t arg0)
{
  if (shared_bInsideMain //this malloc or calloc is after main function
    && !IsThreadLocked(mfQueue)) { //use my malloc or calloc

    PIN_MutexLock(&shared_mutex);

    struct AllocationInfo data;
    data.returned_value = (type_ptr)v;
    data.argument = (type_size_t)arg0;
    UpdateLastTimeInMilisec(data.time);
    const bool bValidPointer = GetBacktracePointer(data.backtrace_pointer);

    if (bValidPointer) {
      shared_memory_map[(void*)data.returned_value] = true;

      Help :: hton((byte*)&data.returned_value, sizeof(data.returned_value));
      Help :: hton((byte*)&data.argument, sizeof(data.argument));
      Help :: hton((byte*)&data.backtrace_pointer, sizeof(data.backtrace_pointer));
      Help :: hton((byte*)&data.time, sizeof(data.time));

      BinfElement binfElement(FCODE_MALLOC, sizeof(mallocTypes), mallocTypes, sizeof(data), (byte*)&data);
      shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
      shared_transmitter->SendBinf(binfElement, data.time);
    }
    PIN_MutexUnlock(&shared_mutex);
  }
}

VOID * NewOperatorNew( TYPE_FP_OPERATOR_NEW orgFuncptr, size_t arg0 )
{
  const bool bHandle = !IsThreadLocked(mfQueue);
  if (bHandle) {
    LockThread(mfQueue);
  }
  VOID * v = orgFuncptr( arg0 );
  if (bHandle) {
    UnlockThread(mfQueue);
    HandleAllocation(v, arg0);
  }
  return v;
}

VOID * NewMalloc( TYPE_FP_MALLOC orgFuncptr, size_t arg0 )
{
  VOID * v = orgFuncptr( arg0 );
  HandleAllocation(v, arg0);
  return v;
}

VOID * NewCalloc( TYPE_FP_CALLOC orgFuncptr, size_t arg0, size_t arg1 )
{
  VOID * v = orgFuncptr( arg0, arg1 );
  HandleAllocation(v, arg0 * arg1);
  return v;
}

void HandleDeallocation(const void * arg0);

VOID * NewRealloc( TYPE_FP_REALLOC orgFuncptr, void* arg0, size_t arg1 )
{
  VOID * v = orgFuncptr( arg0, arg1 );
  if (arg0 != NULL) {
    HandleDeallocation(arg0);
  }
  if (arg1 != 0U) {
    HandleAllocation(v, arg1);
  }
  return v;
}

const byte freeTypes[3] = { TCODE_PTR, TCODE_PTR, TCODE_LONG };

struct DeallocationInfo {
  type_ptr argument;
  type_ptr reserved;
  type_long time;
};

void HandleDeallocation(const void * arg0)
{
  if (!IsThreadLocked(mfQueue))
  { //use my free

    PIN_MutexLock(&shared_mutex);

    struct DeallocationInfo data;
    data.argument = (type_ptr)arg0;
    data.reserved = (type_ptr)arg0;
    UpdateLastTimeInMilisec(data.time);
    const bool bValidPointer = (shared_memory_map.erase((void*)data.argument) != 0U);

    if (bValidPointer) {
      Help :: hton((byte*)&data.argument, sizeof(data.argument));
      Help :: hton((byte*)&data.reserved, sizeof(data.reserved));
      Help :: hton((byte*)&data.time, sizeof(data.time));

      BinfElement binfElement(FCODE_FREE, sizeof(freeTypes), freeTypes, sizeof(data), (byte*)&data);
      shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
      shared_transmitter->SendBinf(binfElement, data.time);
    }
    PIN_MutexUnlock(&shared_mutex);
  }
}

VOID NewFree( TYPE_FP_FREE orgFuncptr, VOID* arg0 )
{
  orgFuncptr( (size_t)arg0 );
  HandleDeallocation(arg0);
}

// Fini declaration
VOID Fini( VOID );

void NewFinalize( TYPE_FP_FINALIZE orgFuncptr, void* d) {
  orgFuncptr(d);
  Fini();
}

VOID NewLibcStartMain( TYPE_FP_LIBC_START_MAIN orgFuncptr,
  int (*main) (int, char **, char **), int argc, char **argv,
        __typeof (main) init, void (*fini) (void), void (*rtld_fini) (void), void *stack_end ) {
  shared_bInsideMain = true;
  orgFuncptr( main, argc, argv, init, fini, rtld_fini, stack_end);
  shared_bInsideMain = false;
}

/* ===================================================================== */
/* Instrumentation routines                                              */
/* ===================================================================== */

void HandleOperator(IMG* img, char* operatorName) {
  // Walk through the symbols in the symbol table.
  //
  for (SYM sym = IMG_RegsymHead(*img); SYM_Valid(sym); sym = SYM_Next(sym))
  {
    string undFuncName = PIN_UndecorateSymbolName(SYM_Name(sym), UNDECORATION_NAME_ONLY);
    ADDRINT funcAddr = IMG_LowAddress(*img) + SYM_Value(sym);
    RTN rtn = RTN_FindByAddress(funcAddr);
    const bool bResult = RTN_Valid(rtn) && RTN_IsSafeForProbedInsertion(rtn);
#ifdef DEBUG_INFO
    char* imgName = (char*)IMG_Name(*img).c_str();
    bool bPrintInfo = bResult;
#endif
    if ( bResult ) {
      if (undFuncName == operatorName && (operatorName == OPERATOR_NEW1
          || operatorName == OPERATOR_NEW2))
      {
        PROTO proto_operator_new = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
          operatorName, PIN_PARG(size_t), PIN_PARG_END() );
        RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewOperatorNew),
          IARG_PROTOTYPE, proto_operator_new,
          IARG_ORIG_FUNCPTR,
          IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
          IARG_END);
        PROTO_Free( proto_operator_new );
      }
#ifdef DEBUG_INFO
      else {
        bPrintInfo = false;
      }
#endif
    }
#ifdef DEBUG_INFO
    if (bPrintInfo) {
      char message[MAX_STR_LEN];
      sprintf(message, "\nReplaced \"%s\" (addr. \"%p\") function in \"%s\"",
          undFuncName.c_str(), (void*)funcAddr, imgName);
      LOG_INFO(message);
    }
#endif
  }
}

void HandleFunction(IMG* img, char* funcName) {
  RTN rtn = RTN_FindByName( *img, funcName );

  const bool bResult = RTN_Valid(rtn) && RTN_IsSafeForProbedInsertion(rtn);
#ifdef DEBUG_INFO
  char* imgName = (char*)IMG_Name(*img).c_str();
  bool bPrintInfo = bResult;
#endif
  if (bResult)
  {
    if (strcmp(funcName, __LIBC_START_MAIN) == 0) {
      PROTO proto_libc_start_main = PROTO_Allocate( PIN_PARG(int), CALLINGSTD_DEFAULT,
        funcName, PIN_PARG(void*), PIN_PARG(int), PIN_PARG(char**),
        PIN_PARG(int), PIN_PARG(void*), PIN_PARG(void*), PIN_PARG(void*), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewLibcStartMain),
        IARG_PROTOTYPE, proto_libc_start_main,
        IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 1,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 2,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 3,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 4,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 5,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 6,
        IARG_END);
      PROTO_Free( proto_libc_start_main );
    }
    else if (strcmp(funcName, MALLOC) == 0) {
      PROTO proto_malloc = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
        MALLOC, PIN_PARG(size_t), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewMalloc),
        IARG_PROTOTYPE, proto_malloc,
        IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_END);
      PROTO_Free( proto_malloc );
    }
    else if (strcmp(funcName, CALLOC) == 0) {
      PROTO proto_calloc = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
        CALLOC, PIN_PARG(size_t), PIN_PARG(size_t), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewCalloc),
        IARG_PROTOTYPE, proto_calloc,
        IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 1,
        IARG_END);
      PROTO_Free( proto_calloc );
    }
    else if (strcmp(funcName, REALLOC) == 0) {
      PROTO proto_realloc = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
        REALLOC, PIN_PARG(void*), PIN_PARG(size_t), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewRealloc),
        IARG_PROTOTYPE, proto_realloc,
          IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 1,
        IARG_END);
      PROTO_Free( proto_realloc );
    }
    else if (strcmp(funcName, FREE) == 0) {
      PROTO proto_free = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
        FREE, PIN_PARG(size_t), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewFree),
        IARG_PROTOTYPE, proto_free,
        IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_END);
      PROTO_Free( proto_free );
    }
    else if (strcmp(funcName, FINALIZE) == 0) {
      PROTO proto_finalize = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
        FINALIZE, PIN_PARG(void*), PIN_PARG_END() );
      RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewFinalize),
        IARG_PROTOTYPE, proto_finalize,
        IARG_ORIG_FUNCPTR,
        IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
        IARG_END);
      PROTO_Free( proto_finalize );
    }
    else if (strcmp(funcName, BACKTRACE) == 0) {
      ADDRINT addr = RTN_Address(rtn);
      p_backtrace = (int (*)( void**, int ))addr;
    }
#ifdef DEBUG_INFO
    else if (strcmp(funcName, BACKTRACE_SYMBOLS) == 0) {
      ADDRINT addr = RTN_Address(rtn);
      p_backtrace_symbols = (char** (*)( void* const*, int ))addr;
    }
    else {
      bPrintInfo = false;
    }
#endif
  }
#ifdef DEBUG_INFO
  if (bPrintInfo) {
    char message[MAX_STR_LEN];
    sprintf(message, "\nReplaced \"%s\" (addr. \"%p\") function in \"%s\"",
        funcName, (void*)RTN_Address(rtn), imgName);
    LOG_INFO(message);
  }
#endif
}

VOID ImageLoad( IMG img, VOID *v )
{
  HandleOperator(&img, (char*)OPERATOR_NEW1);
  HandleOperator(&img, (char*)OPERATOR_NEW2);
  HandleFunction(&img, (char*)__LIBC_START_MAIN);
  HandleFunction(&img, (char*)MALLOC);
  HandleFunction(&img, (char*)CALLOC);
  HandleFunction(&img, (char*)REALLOC);
  HandleFunction(&img, (char*)FREE);
  HandleFunction(&img, (char*)BACKTRACE);
  HandleFunction(&img, (char*)FINALIZE);
#ifdef DEBUG_INFO
  HandleFunction(&img, (char*)BACKTRACE_SYMBOLS);
#endif
}

/* ===================================================================== */
/* Initializing, finalizing funtions.                                    */
/* ===================================================================== */

INT32 Help( VOID )
{
  char message[MAX_STR_LEN];
  sprintf(message, "%s\r\n", (char*)KNOB_BASE::StringKnobSummary().c_str());
  LOG_ERR(message);
  return -1;
}

VOID Ini(int i32Ip, int i32Port) {
  LOG_INFO((char*)"\n*** MEMORY TRACE ***\n");
  // Init mutexes
  PIN_MutexInit(&threadLockMutex);
  PIN_MutexInit(&shared_mutex);
  // Init a binary journal
  shared_bin_journal = new BinJournal(KnobOutputFile.Value().c_str());
  shared_transmitter = new Net(i32Ip, i32Port);
  gettimeofday(&shared_start_time, NULL);
  // Init variables
  shared_last_time = 0;
}

VOID Fini( VOID ) {
  if (shared_transmitter == NULL || shared_bin_journal == NULL) {
    return;
  }

  PIN_MutexLock(&shared_mutex);

  if (!shared_bin_journal->Push()) {
    LOG_ERR((char*)"The journal hasn't been written correctly.");
  }
  shared_transmitter->FlushAll();

  //Just in case to prevent possible different errors
  delete shared_transmitter;
  delete shared_bin_journal;
  //To be sure the functions aren't invoked anymore
  shared_transmitter = NULL;
  shared_bin_journal = NULL;

  PIN_MutexUnlock(&shared_mutex);
  PIN_MutexFini(&threadLockMutex);
  PIN_MutexFini(&shared_mutex);
}

CHAR** AllocateNewArgsMem(INT32 argc_new)
{
  const int MAX_STR_LEN_ARGS = 128;

  const size_t ARR_POINTERS_SIZE = argc_new * sizeof(void*);
  const size_t ARR_STRINGS_SIZE = argc_new * MAX_STR_LEN_ARGS;
  CHAR** argv_new = (char**)new char[ARR_POINTERS_SIZE + ARR_STRINGS_SIZE];
  char* base = ARR_POINTERS_SIZE + (char*)argv_new;
  for (int i = 0; i < argc_new; i++) {
    argv_new[i] = base + i * MAX_STR_LEN_ARGS;
  }
  return argv_new;
}

void InitNewArgs(INT32 argc, CHAR *argv[], INT32& argc_new, CHAR *argv_new[])
{
  int stub;
  const int numIpArg = ParseToIP(argc, argv, stub);
  const int numPortArg = ParseToPort(argc, argv, stub);

  int realNumArg = 0;
  const int max_args = argc_new;
  for (int i = 0; i < max_args; i++) {
    if (i == numIpArg || i == numPortArg) {
      argc_new--;
    }
    else {
      strcpy(argv_new[realNumArg], argv[i]);
      realNumArg++;
    }
  }
}

int main( INT32 argc, CHAR *argv[] )
{
  INT32 argc_new = min(32, argc);
  CHAR** argv_new = AllocateNewArgsMem(argc_new);
  InitNewArgs(argc, argv, argc_new, argv_new);

  PIN_InitSymbols();
  if (PIN_Init(argc_new, argv_new))
    { return Help(); }

  IMG_AddInstrumentFunction( ImageLoad, 0 );

  int i32Ip, i32Port;
  ParseToIP(argc, argv, i32Ip);
  ParseToPort(argc, argv, i32Port);
  Ini(i32Ip, i32Port);

  PIN_StartProgramProbed();

  return 0;
}

