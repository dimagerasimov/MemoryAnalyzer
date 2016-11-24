/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   InitPinClient.hpp
 * Author: master
 *
 * Created on November 16, 2016, 12:24 AM
 */

#ifndef PIN_LOADER_HPP
#define PIN_LOADER_HPP

#include "protocol.hpp"

#define PIN_TOOL_PATH "./memory_trace_net.so"
#define TMP_FOLDER "./temp"

class PinLoader {
    public:
        PinLoader();
        ~PinLoader();
        
        bool setPathToApp(const char* pathToApp);
        bool setArgsForApp(const char* argsForApp);
        bool setPort(const char* port);
        bool setKey(const int unique_key_of_file);
        bool isPinReady();
        bool pinExec(char* ip);
        bool pinBlockWait();
        bool pinKill();
        byte* getBinary();
        
    private:
        void hardReset();
        
        pid_t pin_proc;
        int portToConnect;
        char pathToApp[MAX_MESSAGE_LENGTH];
        char argsForApp[MAX_MESSAGE_LENGTH];
        int unique_key_of_file;
};

#endif /* PIN_LOADER_HPP */

