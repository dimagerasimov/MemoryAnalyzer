#include "pin.H"
#include <iostream>
#include <sys/time.h>
#include <gnu/lib-names.h>

#include "bin_journal.h"
#include "net.h"

/* ===================================================================== */
/* Type declarations							*/
/* ===================================================================== */

#define MAX_STR_LEN 256

void (*p_realfree) ( size_t ); //pointer to real free function
int (*p_backtrace) ( void**, int ); //pointer to backtrace function
char** (*p_backtrace_symbols) ( void**, int ); //pointer to backtrace_symbols function

//The types for all function pointers
typedef VOID * ( *TYPE_FP_MALLOC )( size_t );
typedef VOID ( *TYPE_FP_FREE )( size_t );
typedef ADDRINT ( *TYPE_FP_MAIN)( int, char** );
typedef VOID ( *TYPE_FP_EXIT )( int );

/* ===================================================================== */
/* Global variables							*/
/* ===================================================================== */

bool bWasMainInvoked; //after main function in traced application it's true
bool bWasBacktraceTaken; //the flag indicates backtrace was taken or not
bool bUseUsualMallocFree; //the flag to use usual malloc and free in places where it's needed

// Shared data
PIN_MUTEX shared_pin_mutex;
type_long shared_last_time;
struct timeval shared_start_time;
Net* shared_transmitter;
BinJournal* shared_bin_journal;

/* ===================================================================== */
/* Commandline Switches */
/* ===================================================================== */

KNOB<string> KnobOutputFile(KNOB_MODE_WRITEONCE, "pintool",
	"o", "memory_trace_net.out", "specify trace file name");

/* ===================================================================== */
/* Names of library for replacement functions				*/
/* ===================================================================== */

#if defined(TARGET_LINUX)
#define STDLIB LIBC_SO
#elif defined(TARGET_MAC)
#define STDLIB "something for mac"
#elif defined(TARGET_WINDOWS)
#define STDLIB "something for windows"
#endif

/* ===================================================================== */
/* Names of replacement functions					*/
/* ===================================================================== */

#define MAIN "main"

#if defined(STDLIB)
	#define EXIT "exit"
	#if defined(TARGET_MAC)
	#define MALLOC "_malloc"
	#define FREE "_free"
	#else
	#define MALLOC "malloc"
	#define FREE "free"
	#endif
	#if defined(TARGET_LINUX)
	#define BACKTRACE "backtrace"
	#define BACKTRACE_SYMBOLS "backtrace_symbols"
	#endif
#endif

/* ===================================================================== */
/* Log functions							*/
/* ===================================================================== */

void LogInfo(char msg[]) {
	cout << msg << endl;
}
void LogError(char msg[]) {
	cerr << msg << endl;
}

/* ===================================================================== */

void UpdateLastTimeInMilisec(type_long& out_last_time) {
	struct timeval current_time;
	gettimeofday(&current_time, NULL);
	type_long current_time_in_ms = (current_time.tv_sec - shared_start_time.tv_sec) * 1000000
		+ (current_time.tv_usec - shared_start_time.tv_usec);
	if(shared_last_time >= current_time_in_ms){
		shared_last_time += 1;
	}
	else {
		shared_last_time = current_time_in_ms;
	}
	out_last_time = shared_last_time;
}
int ParseToIP(int argc, char* argv[]) {
	char s_ip[] = "ip=";
	char* p;
	in_addr tmp;
	tmp.s_addr = -1;
	for(int i = 0; i < argc; i++) {
		p = strstr(argv[i], s_ip);
		if(p != NULL) {
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
	for(int i = 0; i < argc; i++) {
		p = strstr(argv[i], s_port);
		if(p != NULL) {
			port = atoi(p + strlen(s_port));
			break;
		}
	}
	return port;
}
VOID Ini( int argc, char* argv[] ) {
	LogInfo((char*)"\n*** MEMORY TRACE ***\n");
	// Init pin mutex
	PIN_MutexInit(&shared_pin_mutex);
	// Init a binary journal
	shared_bin_journal = new BinJournal(KnobOutputFile.Value().c_str());
	shared_transmitter = new Net(ParseToIP(argc, argv), ParseToPort(argc, argv));
	gettimeofday(&shared_start_time, NULL);
	// Init variables
	shared_last_time = 0;
	bWasMainInvoked = false;
	bWasBacktraceTaken = false;
	bUseUsualMallocFree = false;
}
VOID Fini( VOID ) {
	if(shared_transmitter == NULL || shared_bin_journal == NULL) {
		return;
	}

	PIN_MutexLock(&shared_pin_mutex);

	bool pushOk = shared_bin_journal->Push();
	if(!pushOk) {
		LogError((char*)
		"Error: The binary journal hasn't been recorded. Try again.");
	}
	shared_transmitter->FlushAll();
	
	delete shared_transmitter;
	shared_transmitter = NULL;
	delete shared_bin_journal;
	shared_bin_journal = NULL;

	PIN_MutexUnlock(&shared_pin_mutex);
	PIN_MutexFini(&shared_pin_mutex);
}

/* ===================================================================== */
/* Replacement routines							*/
/* ===================================================================== */

VOID * NewMalloc( TYPE_FP_MALLOC orgFuncptr, size_t arg0, ADDRINT returnIp )
{
	// Call the relocated entry point of the original (replaced) routine.
	//
	VOID * v = orgFuncptr( arg0 );

	if(bUseUsualMallocFree) {
		return v;
	}

	if(bWasMainInvoked) {
		if(bWasBacktraceTaken) {
			const int BACKTRACE_BUFFER_SIZE = 25;
			void* buffer[BACKTRACE_BUFFER_SIZE];
			cout << "/// Backtrace test ///" << endl;

			bUseUsualMallocFree = true;
			p_backtrace(buffer, BACKTRACE_BUFFER_SIZE);
			char** strings = p_backtrace_symbols(buffer, BACKTRACE_BUFFER_SIZE);
			for(int i = 0; i < BACKTRACE_BUFFER_SIZE; i++) {
				cout << strings[i] << " ";
			}
			cout << endl;
			p_realfree((size_t)strings);
			bUseUsualMallocFree = false;
		}

		PIN_MutexLock(&shared_pin_mutex);

		type_long current_time;
		UpdateLastTimeInMilisec(current_time);

		byte count = 4;
		byte size_of_data = sizeof(type_ptr) + sizeof(type_size_t) + sizeof(type_ptr) + sizeof(type_long);

		byte* types = new byte[count * sizeof(byte)];	
		types[0] = TCODE_PTR;
		types[1] = TCODE_SIZE_T;
		types[2] = TCODE_PTR;//ADDRINT equal type_ptr
		types[3] = TCODE_LONG;

		byte* data = new byte[size_of_data];
		type_ptr copy_v = (type_ptr)v;
		type_size_t copy_arg0 = (type_size_t)arg0;
		type_ptr copy_returnIp = (type_ptr)returnIp;
		type_long copy_current_time = (type_long)current_time;

		Help :: hton((byte*)&copy_v, sizeof(type_ptr));
		Help :: hton((byte*)&copy_arg0, sizeof(type_size_t));
		Help :: hton((byte*)&copy_returnIp, sizeof(type_ptr));
		Help :: hton((byte*)&copy_current_time, sizeof(type_long));

		memcpy(&data[0], &copy_v, sizeof(type_ptr));
		memcpy(&data[0 + sizeof(type_ptr)], &copy_arg0, sizeof(type_size_t));
		memcpy(&data[0 + sizeof(type_ptr) + sizeof(type_size_t)], &copy_returnIp, sizeof(type_ptr));
		memcpy(&data[0 + sizeof(type_ptr) + sizeof(type_size_t) + sizeof(type_ptr)],
			&copy_current_time, sizeof(type_long));

		BinfElement binfElement(FCODE_MALLOC, count, types, size_of_data, data);
		shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
		shared_transmitter->SendBinf(binfElement, current_time);
	
		delete[] types;
		delete[] data;

		PIN_MutexUnlock(&shared_pin_mutex);
	}
	return v;
}

VOID NewFree( TYPE_FP_FREE orgFuncptr, VOID* arg0, ADDRINT returnIp )
{
	// Call the relocated entry point of the original (replaced) routine.
	//
	orgFuncptr( (size_t)arg0 );

	if(bUseUsualMallocFree) {
		return;
	}

	if(bWasMainInvoked) {
		PIN_MutexLock(&shared_pin_mutex);

		// Current time
		type_long current_time;
		UpdateLastTimeInMilisec(current_time);

		byte count = 3;
		byte size_of_data = sizeof(type_ptr) + sizeof(type_ptr) + sizeof(type_long);

		byte* types = new byte[count * sizeof(byte)];
		types[0] = TCODE_PTR;
		types[1] = TCODE_PTR;//ADDRINT equal type_ptr
		types[2] = TCODE_LONG;

		byte* data = new byte[size_of_data];
		type_ptr copy_arg0 = (type_ptr)arg0;
		type_ptr copy_returnIp = (type_ptr)returnIp;
		type_long copy_current_time = (type_long)current_time;

		Help :: hton((byte*)&copy_arg0, sizeof(type_ptr));
		Help :: hton((byte*)&copy_returnIp, sizeof(type_ptr));
		Help :: hton((byte*)&copy_current_time, sizeof(type_long));

		memcpy(&data[0], &copy_arg0, sizeof(type_ptr));
		memcpy(&data[0 + sizeof(type_ptr)], &copy_returnIp, sizeof(type_ptr));
		memcpy(&data[0 + sizeof(type_ptr) + sizeof(type_ptr)], &copy_current_time, sizeof(type_long));

		BinfElement binfElement(FCODE_FREE, count, types, size_of_data, data);
		shared_bin_journal->AddBinf(binfElement, MFREE_SECTION);
		shared_transmitter->SendBinf(binfElement, current_time);

		delete[] types;
		delete[] data;

		PIN_MutexUnlock(&shared_pin_mutex);
	}
}

VOID NewMain( TYPE_FP_MAIN orgFuncptr, ADDRINT arg0, VOID* arg1) {
	// Call the relocated entry point of the original (replaced) routine.
	//
	bWasMainInvoked = true;
	orgFuncptr( arg0, (char**)arg1 );
	Fini();
}

VOID NewExit( TYPE_FP_EXIT orgFuncptr, ADDRINT arg0) {
	// Call the relocated entry point of the original (replaced) routine.
	//
	Fini();
	orgFuncptr( arg0 );
}

/* ===================================================================== */
/* Instrumentation routines						*/
/* ===================================================================== */

bool ReplaceFunction(IMG* img, char* funcName) {
	bool bResult = true;
	char* imgName = (char*)IMG_Name(*img).c_str();
	RTN rtn = RTN_FindByName( *img, funcName );

	bResult = RTN_Valid(rtn);
	if ( bResult )
	{
		char message[MAX_STR_LEN];
		sprintf(message, "\nFound \"%s\" function in module \"%s\"",
			funcName, imgName);
		LogInfo(message);

		bResult = RTN_Valid(rtn);
		if(bResult)
		{
			if(strcmp(funcName, MAIN) == 0) {
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
			else if(strcmp(funcName, MALLOC) == 0) {
				PROTO proto_malloc = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
					MALLOC, PIN_PARG(size_t), PIN_PARG_END() );
				RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewMalloc),
					IARG_PROTOTYPE, proto_malloc,
					IARG_ORIG_FUNCPTR,
					IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
					IARG_RETURN_IP,
					IARG_END);
				PROTO_Free( proto_malloc );
			}
			else if(strcmp(funcName, FREE) == 0) {
				ADDRINT addr = RTN_Address(rtn);
				p_realfree = (void (*)( size_t ))addr;
				PROTO proto_free = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
					FREE, PIN_PARG(size_t), PIN_PARG_END() );
				RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewFree),
					IARG_PROTOTYPE, proto_free,
					IARG_ORIG_FUNCPTR,
					IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
					IARG_RETURN_IP,
					IARG_END);
				PROTO_Free(proto_free);
			}
			else if(strcmp(funcName, EXIT) == 0) {
				PROTO proto_exit = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
					EXIT, PIN_PARG(int), PIN_PARG_END() );
				RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewExit),
					IARG_PROTOTYPE, proto_exit,
					IARG_ORIG_FUNCPTR,
					IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
					IARG_END);
				PROTO_Free(proto_exit);
			}
			else if(strcmp(funcName, BACKTRACE) == 0) {
				ADDRINT addr = RTN_Address(rtn);
				p_backtrace = (int (*)( void**, int ))addr;
			}
			else if(strcmp(funcName, BACKTRACE_SYMBOLS) == 0) {
				ADDRINT addr = RTN_Address(rtn);
				p_backtrace_symbols = (char** (*)( void**, int ))addr;
			}
		}
		sprintf(message, "\tWas it replaced? %s\n",
			bResult == true ? "YES" : "NO");
		LogInfo(message);
	}
	return bResult;
}

// Pin calls this function every time a new img is loaded.
// It is best to do probe replacement when the image is loaded,
// because only one thread knows about the image at this time.
//
VOID ImageLoad( IMG img, VOID *v )
{
	char* imgName = (char*)IMG_Name(img).c_str();

	if(!bWasMainInvoked)
	{// Replace the "main" function with my function only once
		ReplaceFunction(&img, (char*)MAIN);
	}
	// Look for stdlib library
	bool bStdlib = ( string(imgName).find(STDLIB) < MAX_STR_LEN );
	if(bStdlib)
	{
		// Replace the "malloc" function with my function
		ReplaceFunction(&img, (char*)MALLOC);
		// Replace the "free" function with my function
		ReplaceFunction(&img, (char*)FREE);
		// Replace the "exit" function with my function
		ReplaceFunction(&img, (char*)EXIT);
		// Replace the "backtrace" function with my function
		ReplaceFunction(&img, (char*)BACKTRACE);
		// Replace the "backtrace_symbols" function with my function
		ReplaceFunction(&img, (char*)BACKTRACE_SYMBOLS);
		bWasBacktraceTaken = (p_backtrace != NULL) && (p_backtrace_symbols != NULL);
	}
	else
	{
		char message[MAX_STR_LEN];
		sprintf(message, "<--- Module \"%s\" ignored... --->", imgName);
		LogInfo(message);
	}
}

/* ===================================================================== */
/* Print Help Message							*/
/* ===================================================================== */

INT32 Usage( VOID )
{
	cerr << "This tool replace an original function \"malloc\"" << endl
		<< " with a custom function \"MyMalloc\" defined in the tool " << endl
		<< " using probes.  The replacement function has a different " << endl
		<< " signature from that of the original replaced function." << endl
		<< endl << KNOB_BASE::StringKnobSummary() << endl;
	return -1;
}

/* ===================================================================== */
/* Main: Initialize and start Pin in Probe mode.			*/
/* ===================================================================== */

int main( INT32 argc, CHAR *argv[] )
{
	// Initialize symbol processing
	//
	PIN_InitSymbols();

	// Initialize pin
	//
	if (PIN_Init(argc, argv))
		{ return Usage(); }

	// Register ImageLoad to be called when an image is loaded
	//
	IMG_AddInstrumentFunction( ImageLoad, 0 );

	//Init function
	Ini(argc, argv);
	
	// Start the program in probe mode, never returns
	//
	PIN_StartProgramProbed();

	return 0;
}

