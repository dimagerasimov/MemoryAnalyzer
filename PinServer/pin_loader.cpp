/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>

#include "pin_loader.hpp"

#define ERR_EXIT_CODE 1234

PinLoader :: PinLoader() {
    pin_proc = 0;
    port4Connect = 0;
    pathToApp[0] = '\0';
}
PinLoader :: ~PinLoader() {
}
bool PinLoader :: setPathToApp(const char* pathToApp) {
    try {
        strcpy(this->pathToApp, pathToApp);
        return true;
    } catch(...) {
        return false;
    }
}
bool PinLoader :: setPort(const char* port) {
    try {
        this->port4Connect = atoi(port);
        return true;
    } catch(...) {
        return false;
    }
}
const char* PinLoader :: getPathToApp() {
    return (const char*)this->pathToApp;
}
const int PinLoader :: getPort() {
    return this->port4Connect;
}
bool isPinReady() {
    pid_t child_proc = fork();
    if(child_proc == 0) {
        // Close stdout that child will not wrote on the screen
        fclose(stdout);
        execlp("pin", "", NULL);
        exit(ERR_EXIT_CODE);
    }
    int status;
    waitpid(child_proc, &status, WCONTINUED);
    if(WIFEXITED(status) == 0) {
        return false;
    }
    if(WEXITSTATUS(status) == ERR_EXIT_CODE) {
        return false;
    }
    return true;
}
bool PinLoader :: isReady() {
    FILE* tmp;
    tmp = fopen(pathToApp, "rb");
    if(tmp == NULL) {
        return false;
    }
    tmp = fopen(PIN_TOOL_PATH, "rb");
    if(tmp == NULL) {
        return false;
    }
    return isPinReady();
}
bool PinLoader :: pinExec(char* ip) {
    pin_proc = fork();
    if(pin_proc == -1) {
        return false;
    }
    else if(pin_proc == 0) {
        char port_tmp[16];
        char ip_tmp[32];
        sprintf(port_tmp, "port=%d", port4Connect);
        sprintf(ip_tmp, "ip=%s", ip);
        // Close stdout that child will not wrote on the screen
        fclose(stdout);
        // Give to possibility the other party accept my
        // incoming connection and close previous connection
        sleep(3);
        execlp("pin", "", "-t", PIN_TOOL_PATH, "--", pathToApp,
                port_tmp, ip_tmp, NULL);
        exit(ERR_EXIT_CODE);
    }
    return true;
}
bool PinLoader :: pinWait() {
    int status;
    if(pin_proc == 0) {
        return false;
    }
    waitpid(pin_proc, &status, WCONTINUED);
    pin_proc = 0;
    if(WIFEXITED(status) == 0) {
        return true;
    }
    if(WEXITSTATUS(status) == ERR_EXIT_CODE) {
        return true;
    }
    return true;
}