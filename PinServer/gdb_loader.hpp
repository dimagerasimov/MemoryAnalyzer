/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   gdb_loader.hpp
 * Author: master
 *
 * Created on November 16, 2016, 12:24 AM
 */

#ifndef GDB_LOADER_HPP
#define GDB_LOADER_HPP

#include "protocol.hpp"

class GdbLoader {
    public:
        GdbLoader();
        ~GdbLoader();
        
        bool SetPathToApp(const char* pathToApp);
        bool ReadStdout(char text[]);
        bool GdbRun(char answer[]);
        bool GdbRequest(char requestText[], char* output_buffer, const int size_output_buffer);
        bool GdbStop();
        
    private:
        void HardReset();
        
        pid_t gdb_proc;
        int gdb_stdin;
        int gdb_stdout;
        char pathToApp[MAX_MESSAGE_LENGTH];
};

#endif /* GDB_LOADER_HPP */

