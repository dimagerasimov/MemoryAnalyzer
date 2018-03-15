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

const char* STDLIB    = LIBC_SO;

const char* MAIN      = "main";
typedef ADDRINT ( *TYPE_FP_MAIN)( int, char** );

const char* EXIT      = "exit";
typedef VOID ( *TYPE_FP_EXIT )( int );

const char* MALLOC    = "malloc";
typedef VOID * ( *TYPE_FP_MALLOC )( size_t );

const char* FREE      = "free";
typedef VOID ( *TYPE_FP_FREE )( size_t );

const char* BACKTRACE = "backtrace";
int (*p_backtrace) ( void**, int ); //just a pointer to backtrace

/* ===================================================================== */
/* Global variables                                                      */
/* ===================================================================== */

const uint32_t MAX_STR_LEN = 512U;

PIN_MUTEX mfMutex;
map<OS_THREAD_ID, bool> mfQueue;

// Shared data
bool shared_bInsideMain;

PIN_MUTEX shared_mutex;
type_long shared_last_time;
struct timeval shared_start_time;
Net* shared_transmitter;
BinJournal* shared_bin_journal;

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

bool IsThreadMfLocked() {
  PIN_MutexLock(&mfMutex);
  const bool bResult = (mfQueue.find(PIN_GetTid()) != mfQueue.end());
  PIN_MutexUnlock(&mfMutex);
  return bResult;
}

void LockThreadMf() {
  if (IsThreadMfLocked()) {
    PIN_ExitProcess(1234);
  }
  PIN_MutexLock(&mfMutex);
  mfQueue[PIN_GetTid()] = true;
  PIN_MutexUnlock(&mfMutex);
}

void UnlockThreadMf() {
  if (!IsThreadMfLocked()) {
    PIN_ExitProcess(1234);
  }
  PIN_MutexLock(&mfMutex);
  mfQueue.erase(mfQueue.find(PIN_GetTid()));
  PIN_MutexUnlock(&mfMutex);
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

type_ptr GetBacktracePointer()
{
  const uint32_t BACKTRACE_BUFFER_SIZE = 16U;
  type_ptr result = (type_ptr)0xFFFFFFFF;
  if (p_backtrace != NULL) {
    void* buffer[BACKTRACE_BUFFER_SIZE];
    LockThreadMf();
    // There is malloc in the backtrace function, it matters to use
    // usual malloc inside otherwise invocation is recursived
    p_backtrace(buffer, BACKTRACE_BUFFER_SIZE);
    UnlockThreadMf();
    for (uint32_t i = 0U; i < BACKTRACE_BUFFER_SIZE; i++) {
      if (result > (type_ptr)buffer[i]) { 
        result = (type_ptr)buffer[i];
        break;
      }
    }
  }
  return result;
}

VOID * NewMalloc( TYPE_FP_MALLOC orgFuncptr, size_t arg0 )
{
  VOID * v = orgFuncptr( arg0 );

  if (shared_bInsideMain //this malloc is after main function
    && !IsThreadMfLocked()) { //use my malloc

    PIN_MutexLock(&shared_mutex);

    struct MallocData data;
    data.returned_value = (type_ptr)v;
    data.argument = (type_size_t)arg0;
    data.backtrace_pointer = GetBacktracePointer();
    UpdateLastTimeInMilisec(data.time);

    Help :: hton((byte*)&data.returned_value, sizeof(data.returned_value));
    Help :: hton((byte*)&data.argument, sizeof(data.argument));
    Help :: hton((byte*)&data.backtrace_pointer, sizeof(data.backtrace_pointer));
    Help :: hton((byte*)&data.time, sizeof(data.time));

    BinfElement binfElement(FCODE_MALLOC, sizeof(mallocTypes), mallocTypes, sizeof(data), (byte*)&data);
    shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
    shared_transmitter->SendBinf(binfElement, data.time);

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
    !IsThreadMfLocked()) { //use my free

    PIN_MutexLock(&shared_mutex);

    struct FreeData data;
    data.argument = (type_ptr)arg0;
    data.reserved = (type_ptr)arg0;
    UpdateLastTimeInMilisec(data.time);

    Help :: hton((byte*)&data.argument, sizeof(data.argument));
    Help :: hton((byte*)&data.reserved, sizeof(data.reserved));
    Help :: hton((byte*)&data.time, sizeof(data.time));

    BinfElement binfElement(FCODE_FREE, sizeof(freeTypes), freeTypes, sizeof(data), (byte*)&data);
    shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
    shared_transmitter->SendBinf(binfElement, data.time);

    PIN_MutexUnlock(&shared_mutex);
  }
}

VOID NewMain( TYPE_FP_MAIN orgFuncptr, ADDRINT arg0, VOID* arg1) {
  shared_bInsideMain = true;
  orgFuncptr( arg0, (char**)arg1 );
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

bool ReplaceFunction(IMG* img, char* funcName) {
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
      if (strcmp(funcName, MAIN) == 0) {
        PROTO proto_main = PROTO_Allocate( PIN_PARG(int), CALLINGSTD_DEFAULT,
          funcName, PIN_PARG(int), PIN_PARG(char**), PIN_PARG_END() );
        RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewMain),
          IARG_PROTOTYPE, proto_main,
          IARG_ORIG_FUNCPTR,
          IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
          IARG_FUNCARG_ENTRYPOINT_VALUE, 1,
          IARG_END);
        PROTO_Free( proto_main );
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

  if (!shared_bInsideMain)
  {// Replace the "main" function with my function only once
    ReplaceFunction(&img, (char*)MAIN);
  }
  // Look for stdlib library
  const bool bStdlib = ( string(imgName).find(STDLIB) < MAX_STR_LEN );
  if (bStdlib)
  {
    // Replace the "malloc" function with my function
    ReplaceFunction(&img, (char*)MALLOC);
    // Replace the "free" function with my function
    ReplaceFunction(&img, (char*)FREE);
    // Replace the "exit" function with my function
    ReplaceFunction(&img, (char*)EXIT);
    // Replace the "backtrace" function with my function
    ReplaceFunction(&img, (char*)BACKTRACE);
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
  PIN_MutexInit(&mfMutex);
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
  PIN_MutexFini(&mfMutex);
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

