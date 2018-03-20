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

const char* EXIT      = "exit";
typedef VOID ( *TYPE_FP_EXIT )( int );

const char* MALLOC    = "malloc";
typedef VOID * ( *TYPE_FP_MALLOC )( size_t );

const char* FREE      = "free";
typedef VOID ( *TYPE_FP_FREE )( size_t );

const char* OPERATOR_NEW1    = "_Znwm";
void* p_operator_new1; //just a pointer to the operator new1

const char* OPERATOR_NEW2    = "_Znam";
void* p_operator_new2; //just a pointer to the operator new2

const char* BACKTRACE = "backtrace";
int (*p_backtrace) ( void**, int ); //just a pointer to the backtrace

/* ===================================================================== */
/* Global variables                                                      */
/* ===================================================================== */

const uint32_t MAX_STR_LEN = 512U;

PIN_MUTEX threadLockMutex;
map<OS_THREAD_ID, bool> mfQueue;

// Shared data
bool shared_bInsideMain;

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

int ParseToIP(int argc, char* argv[]) {
  char s_ip[] = "ip=";
  char* p;
  in_addr tmp;
  tmp.s_addr = -1;
  for (int i = 0; i < argc; i++) {
    p = strstr(argv[i], s_ip);
    if (p != NULL) {
      inet_pton(AF_INET, p + strlen(s_ip), &tmp);
      break;
    }
  }
  return tmp.s_addr;
}

int ParseToPort(int argc, char* argv[]) {
  int port = -1;
  char s_port[] = "port=";
  char* p;
  for (int i = 0; i < argc; i++) {
    p = strstr(argv[i], s_port);
    if (p != NULL) {
      port = atoi(p + strlen(s_port));
      break;
    }
  }
  return port;
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

struct MallocData {
  type_ptr returned_value;
  type_size_t argument;
  type_ptr backtrace_pointer;
  type_long time;
};

enum EFunctionId {
  MALLOC_ID,
  FREE_ID,
};

bool IsLibraryAddress(const void* pAddress)
{
  const uint64_t THRESHOLD = 0xFFFFFFFFU;
  return ((uint64_t)pAddress > THRESHOLD);
}

bool IsThisFunction(const void* pFunction, const void* pAddress)
{
  const uint64_t MAX_FUNCTION_OFFSET = 0x2F;
  return ((uint64_t)pAddress - (uint64_t)pFunction < MAX_FUNCTION_OFFSET);
}

bool GetBacktracePointer(const EFunctionId eFuncId, type_ptr& bactrace_pointer)
{
  const uint32_t MAXIMAL_BUFFER_SIZE = 6U;
  uint32_t NEEDED_INDEX = 3U;
  bool bResult = false;
  if (p_backtrace != NULL) {
    void* buffer[MAXIMAL_BUFFER_SIZE];
    LockThread(mfQueue);
    // There is malloc in the backtrace function, it matters to use
    // usual malloc inside otherwise invocation is recursived
    p_backtrace(buffer, MAXIMAL_BUFFER_SIZE);
    UnlockThread(mfQueue);
    if (!IsLibraryAddress(buffer[NEEDED_INDEX])) { 
      bactrace_pointer = (type_ptr)buffer[NEEDED_INDEX];
      bResult = true;
    }
    if (!bResult && eFuncId == MALLOC_ID) {
      if (IsThisFunction(p_operator_new1, buffer[NEEDED_INDEX])) {
        if (!IsLibraryAddress(buffer[NEEDED_INDEX + 1U])) {
          bactrace_pointer = (type_ptr)buffer[NEEDED_INDEX + 1U];
          bResult = true;
        }
        if (IsThisFunction(p_operator_new2, buffer[NEEDED_INDEX + 1U])
            && !IsLibraryAddress(buffer[NEEDED_INDEX + 2U])) {
          bactrace_pointer = (type_ptr)buffer[NEEDED_INDEX + 2U];
          bResult = true;
        }
      }
    }
  }
  return bResult;
}

VOID * NewMalloc( TYPE_FP_MALLOC orgFuncptr, size_t arg0 )
{
  VOID * v = orgFuncptr( arg0 );

  if (shared_bInsideMain //this malloc is after main function
    && !IsThreadLocked(mfQueue)) { //use my malloc

    PIN_MutexLock(&shared_mutex);

    struct MallocData data;
    data.returned_value = (type_ptr)v;
    data.argument = (type_size_t)arg0;
    UpdateLastTimeInMilisec(data.time);
    const bool bValidPointer = GetBacktracePointer(MALLOC_ID, data.backtrace_pointer);

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
  return v;
}

const byte freeTypes[3] = { TCODE_PTR, TCODE_PTR, TCODE_LONG };

struct FreeData {
  type_ptr argument;
  type_ptr reserved;
  type_long time;
};

VOID NewFree( TYPE_FP_FREE orgFuncptr, VOID* arg0 )
{
  orgFuncptr( (size_t)arg0 );

  if (shared_bInsideMain && //this free is after main function
    !IsThreadLocked(mfQueue)) { //use my free

    PIN_MutexLock(&shared_mutex);

    struct FreeData data;
    data.argument = (type_ptr)arg0;
    data.reserved = (type_ptr)arg0;
    UpdateLastTimeInMilisec(data.time);
    type_ptr mockup;
    bool bValidPointer = GetBacktracePointer(FREE_ID, mockup);

    if (bValidPointer) {
      bValidPointer = (shared_memory_map.erase((void*)data.argument) != 0U);
    }
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

VOID NewLibcStartMain( TYPE_FP_LIBC_START_MAIN orgFuncptr,
  int (*main) (int, char **, char **), int argc, char **argv,
        __typeof (main) init, void (*fini) (void), void (*rtld_fini) (void), void *stack_end ) {
  shared_bInsideMain = true;
  orgFuncptr( main, argc, argv, init, fini, rtld_fini, stack_end);
}

// Fini declaration
VOID Fini( VOID );

VOID NewExit( TYPE_FP_EXIT orgFuncptr, ADDRINT arg0) {
  shared_bInsideMain = false;
  Fini();
  orgFuncptr( arg0 );
}

/* ===================================================================== */
/* Instrumentation routines                                              */
/* ===================================================================== */

bool HandleFunction(IMG* img, char* funcName) {
  bool bResult = true;
  char* imgName = (char*)IMG_Name(*img).c_str();
  RTN rtn = RTN_FindByName( *img, funcName );

  bResult = RTN_Valid(rtn);
  if ( bResult )
  {
    char message[MAX_STR_LEN];
    sprintf(message, "\nFound \"%s\" function in \"%s\"", funcName, imgName);
    LOG_INFO(message);

    bResult = RTN_Valid(rtn);
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
      else if (strcmp(funcName, EXIT) == 0) {
        PROTO proto_exit = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
          EXIT, PIN_PARG(int), PIN_PARG_END() );
        RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewExit),
          IARG_PROTOTYPE, proto_exit,
          IARG_ORIG_FUNCPTR,
          IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
          IARG_END);
        PROTO_Free( proto_exit );
      }
      else if (strcmp(funcName, OPERATOR_NEW1) == 0) {
        ADDRINT addr = RTN_Address(rtn);
        p_operator_new1 = (void*)addr;
      }
      else if (strcmp(funcName, OPERATOR_NEW2) == 0) {
        ADDRINT addr = RTN_Address(rtn);
        p_operator_new2 = (void*)addr;
      }
      else if (strcmp(funcName, BACKTRACE) == 0) {
        ADDRINT addr = RTN_Address(rtn);
        p_backtrace = (int (*)( void**, int ))addr;
      }
    }
    sprintf(message, "\tReplaced? %s\n", bResult == true ? "YES" : "NO");
    LOG_INFO(message);
  }
  return bResult;
}

VOID ImageLoad( IMG img, VOID *v )
{
  char* imgName = (char*)IMG_Name(img).c_str();

  // Looking for the stdc
  const bool bStdc = ( string(imgName).find((char*)"libstdc++.so") < MAX_STR_LEN );
  if (bStdc)
  {
    // Replace "operator new" with my function
    HandleFunction(&img, (char*)OPERATOR_NEW1);
    // Replace "operator new" with my function
    HandleFunction(&img, (char*)OPERATOR_NEW2);
  }
  // Looking for the libc
  const bool bLibc = ( string(imgName).find(LIBC_SO) < MAX_STR_LEN );
  if (bLibc)
  {
    // Replace "__libc_start_main" with my function
    HandleFunction(&img, (char*)__LIBC_START_MAIN);
    // Replace "malloc" with my function
    HandleFunction(&img, (char*)MALLOC);
    // Replace "free" with my function
    HandleFunction(&img, (char*)FREE);
    // Replace "exit" with my function
    HandleFunction(&img, (char*)EXIT);
    // Replace "backtrace" with my function
    HandleFunction(&img, (char*)BACKTRACE);
  }
  else
  {
    char message[MAX_STR_LEN];
    sprintf(message, "<--- \"%s\" ignored... --->", imgName);
    LOG_INFO(message);
  }
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

VOID Ini( int argc, char* argv[] ) {
  LOG_INFO((char*)"\n*** MEMORY TRACE ***\n");
  // Init mutexes
  PIN_MutexInit(&threadLockMutex);
  PIN_MutexInit(&shared_mutex);
  // Init a binary journal
  shared_bin_journal = new BinJournal(KnobOutputFile.Value().c_str());
  shared_transmitter = new Net(ParseToIP(argc, argv), ParseToPort(argc, argv));
  gettimeofday(&shared_start_time, NULL);
  // Init variables
  p_backtrace = NULL;
  shared_last_time = 0;
  shared_bInsideMain = false;
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

int main( INT32 argc, CHAR *argv[] )
{
  PIN_InitSymbols();
  if (PIN_Init(argc, argv))
    { return Help(); }

  IMG_AddInstrumentFunction( ImageLoad, 0 );
  Ini(argc, argv);
  PIN_StartProgramProbed();

  return 0;
}

