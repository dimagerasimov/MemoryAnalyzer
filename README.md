# Description
`MemoryAnalyzer` - the set of tools for analyzing memory consumption in third-party C/C++ application.

It consist of three programs:
- MemoryAnalyzer JAVA app - it's GUI app with user-friendly interface to make analysis more comfortable.
- MemoryTraceNet C++ Pin-Tool - a special tool whis uses Intel-PIN to get profiling data from third-party app.
- PinServer C++ - a server is intended to serve the bridge between JAVA app and Pin-Tool.

# Requirements
* Install Intel-PIN ([download here](https://software.intel.com/en-us/articles/pin-a-binary-instrumentation-tool-downloads)).
* Add Intel-PIN to PATH variable.
