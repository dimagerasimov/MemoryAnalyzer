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

class PinLoader {
    public:
        PinLoader();
        ~PinLoader();
        
        bool setPathToApp(const char* pathToApp);
        bool setPort(const char* port);
        const char* getPathToApp();
        const int getPort();
        bool isReady();
        bool pinExec(char* ip);
        bool pinWait();
        
    private:
        pid_t pin_proc;
        int port4Connect;
        char pathToApp[MAX_MESSAGE_LENGTH];
};

#endif /* INITPINCLIENT_HPP */

