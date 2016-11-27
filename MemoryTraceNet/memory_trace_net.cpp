#include "pin.H"
#include <iostream>
#include <sys/times.h>

#include "bin_journal.h"
#include "net.h"

/* ===================================================================== */
/* Type declarations							*/
/* ===================================================================== */

#define MAX_STR_LEN 256

typedef VOID * ( *FP_MALLOC )( size_t );
typedef VOID ( *FP_FREE )( size_t );
typedef VOID ( *FP_EXIT )( int );

/* ===================================================================== */
/* Global variables							*/
/* ===================================================================== */

type_long last_time_in_ms;
clock_t start_time;
Net* transmitter;
BinJournal* binJournal;

/* ===================================================================== */
/* Commandline Switches */
/* ===================================================================== */

KNOB<string> KnobOutputFile(KNOB_MODE_WRITEONCE, "pintool",
	"o", "memory_trace_net.out", "specify trace file name");

/* ===================================================================== */
/* Names of library for replacement functions				*/
/* ===================================================================== */

#if defined(TARGET_LINUX)
#define STDLIB "libc.so"
#elif defined(TARGET_MAC)
#define STDLIB "something for mac"
#elif defined(TARGET_WINDOWS)
#define STDLIB "something for windows"
#endif

/* ===================================================================== */
/* Names of replacement functions					*/
/* ===================================================================== */

#if defined(STDLIB)
	#define EXIT "exit"
	#if defined(TARGET_MAC)
	#define MALLOC "_malloc"
	#define FREE "_free"
	#else
	#define MALLOC "malloc"
	#define FREE "free"
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

type_long GetCurrentTimeMilisec() {
	type_long current_time_in_ms = (double)(times(NULL) - start_time) * 10.0;
	if(last_time_in_ms >= current_time_in_ms){
		last_time_in_ms += 1;
	}
	else {
		last_time_in_ms = current_time_in_ms;
	}
	return last_time_in_ms;
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
	// Write to a binary journal
	binJournal = new BinJournal(KnobOutputFile.Value().c_str());
	transmitter = new Net(ParseToIP(argc, argv), ParseToPort(argc, argv));
	start_time = times(NULL);
	last_time_in_ms = 0;//Initialize
}
VOID Fini( VOID ) {
	bool pushOk = binJournal->Push();
	if(!pushOk) {
		LogError((char*)
		"Error: The binary journal hasn't been recorded. Try again.");
	}
	transmitter->FlushAll();
	delete transmitter;
	delete binJournal;
}

/* ===================================================================== */
/* Replacement routines							*/
/* ===================================================================== */

VOID * NewMalloc( FP_MALLOC orgFuncptr, size_t arg0, ADDRINT returnIp )
{
	// Call the relocated entry point of the original (replaced) routine.
	//
	VOID * v = orgFuncptr( arg0 );

	// Current time
	type_long current_time = GetCurrentTimeMilisec();

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
	binJournal->AddBinf(binfElement, MFREE_SECTION);
	transmitter->SendBinf(binfElement, current_time);

	delete[] types;
	delete[] data;

	return v;
}

VOID NewFree( FP_FREE orgFuncptr, VOID* arg0, ADDRINT returnIp )
{
	// Call the relocated entry point of the original (replaced) routine.
	//
	orgFuncptr( (size_t)arg0 );

	// Current time
	type_long current_time = GetCurrentTimeMilisec();

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
	binJournal->AddBinf(binfElement, MFREE_SECTION);
	transmitter->SendBinf(binfElement, current_time);

	delete[] types;
	delete[] data;
}

VOID NewExit( FP_EXIT orgFuncptr, ADDRINT arg0) {
	// Call the relocated entry point of the original (replaced) routine.
	//
	Fini();
	orgFuncptr( arg0 );
}

/* ===================================================================== */
/* Instrumentation routines						*/
/* ===================================================================== */

// Pin calls this function every time a new img is loaded.
// It is best to do probe replacement when the image is loaded,
// because only one thread knows about the image at this time.
//
VOID ImageLoad( IMG img, VOID *v )
{
	RTN rtn;
	
	char* imgName;
	char message[MAX_STR_LEN];

	imgName = (char*)IMG_Name(img).c_str();
	if(string(imgName).find(STDLIB) >= MAX_STR_LEN)
	{
		sprintf(message, "Module %s ignored...", imgName);
		LogInfo(message);
		return;
	};
	
	// See if malloc() is present in the image.  If so, replace it.
	//
	rtn = RTN_FindByName( img, MALLOC );
	if (RTN_Valid(rtn))
	{
		sprintf(message, "Function \"%s\" found in module %s",
			MALLOC, imgName);
		LogInfo(message);

		if(!RTN_IsSafeForProbedReplacement(rtn))
		{
			sprintf(message, "Unsafe to replace \"%s\" function in module %s",
				MALLOC, imgName);
			LogError(message);
		}
		else
		{
			sprintf(message, "Replaced \"%s\" function in module %s",
				MALLOC, imgName);
			LogInfo(message);

			// Define a function prototype that describes the application routine
			// that will be replaced.
			//
			PROTO proto_malloc = PROTO_Allocate( PIN_PARG(void *), CALLINGSTD_DEFAULT,
				MALLOC, PIN_PARG(size_t), PIN_PARG_END() );

			// Replace the application routine with the replacement function.
			// Additional arguments have been added to the replacement routine.
			//
			RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewMalloc),
				IARG_PROTOTYPE, proto_malloc,
				IARG_ORIG_FUNCPTR,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
				IARG_RETURN_IP,
				IARG_END);
			
			// Free the function prototype.
			//
			PROTO_Free( proto_malloc );
		}
	}

	// See if free() is present in the image.  If so, replace it.
	//
	rtn = RTN_FindByName( img, FREE );
	if (RTN_Valid(rtn))
	{
		sprintf(message, "Function \"%s\" found in module %s",
			FREE, imgName);
		LogInfo(message);

		if(!RTN_IsSafeForProbedReplacement(rtn))
		{
			sprintf(message, "Unsafe to replace \"%s\" function in module %s",
				FREE, imgName);
			LogError(message);
		}
		else
		{
			sprintf(message, "Replaced \"%s\" function in module %s",
				FREE, imgName);
			LogInfo(message);

			// Define a function prototype that describes the application routine
			// that will be replaced.
			//
			PROTO proto_free = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
				FREE, PIN_PARG(size_t), PIN_PARG_END() );

			// Replace the application routine with the replacement function.
			// Additional arguments have been added to the replacement routine.
			//
			RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewFree),
				IARG_PROTOTYPE, proto_free,
				IARG_ORIG_FUNCPTR,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
				IARG_RETURN_IP,
				IARG_END);
			
			// Free the function prototype.
			//
			PROTO_Free( proto_free );
		}
	}

	// See if exit() is present in the image.  If so, replace it.
	//
	rtn = RTN_FindByName( img, EXIT );
	if (RTN_Valid(rtn))
	{
		sprintf(message, "Function \"%s\" found in module %s",
			EXIT, imgName);
		LogInfo(message);

		if(!RTN_IsSafeForProbedReplacement(rtn))
		{
			sprintf(message, "Unsafe to replace \"%s\" function in module %s",
				EXIT, imgName);
			LogError(message);
		}
		else
		{
			sprintf(message, "Replaced \"%s\" function in module %s",
				EXIT, imgName);
			LogInfo(message);

			// Define a function prototype that describes the application routine
			// that will be replaced.
			//
			PROTO proto_exit = PROTO_Allocate( PIN_PARG(void), CALLINGSTD_DEFAULT,
				EXIT, PIN_PARG(int), PIN_PARG_END() );

			// Replace the application routine with the replacement function.
			// Additional arguments have been added to the replacement routine.
			//
			RTN_ReplaceSignatureProbed(rtn, AFUNPTR(NewExit),
				IARG_PROTOTYPE, proto_exit,
				IARG_ORIG_FUNCPTR,
				IARG_FUNCARG_ENTRYPOINT_VALUE, 0,
				IARG_END);
			
			// Free the function prototype.
			//
			PROTO_Free( proto_exit );
		}
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

