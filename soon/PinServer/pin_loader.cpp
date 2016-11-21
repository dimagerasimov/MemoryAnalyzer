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
    hardReset();
}
PinLoader :: ~PinLoader() {
    pinKill();
}
void PinLoader :: hardReset() {
    pin_proc = -1;
    portToConnect = 0;
    pathToApp[0] = '\0';
    unique_key_of_file = -1;
}
void getTmpFilePath(char buffer[], pid_t pin_proc) {
    sprintf(buffer, "%s/%d.out", TMP_FOLDER, pin_proc);
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
        this->portToConnect = atoi(port);
        return true;
    } catch(...) {
        return false;
    }
}
bool PinLoader :: setKey(const int unique_key_of_file) {
    if(unique_key_of_file == -1) {
        return false;
    }
    this->unique_key_of_file = unique_key_of_file;
    return true;
}
bool isPinExist() {
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
bool PinLoader :: isPinReady() {
    FILE* tmp;
    tmp = fopen(pathToApp, "rb");
    if(tmp == NULL) {
        return false;
    }
    tmp = fopen(PIN_TOOL_PATH, "rb");
    if(tmp == NULL) {
        return false;
    }
    if(unique_key_of_file == -1) {
        return false;
    }
    return isPinExist();
}
bool PinLoader :: pinExec(char* ip) {
    // Test re-run
    if(pin_proc != -1) {
        return false;
    }
    // Make fork current process
    pin_proc = fork();
    if(pin_proc == -1) {
        return false;
    }
    else if(pin_proc == 0) {
        char port_tmp[16];
        char ip_tmp[32];
        char tmp_file_path[128];
        sprintf(port_tmp, "port=%d", portToConnect);
        sprintf(ip_tmp, "ip=%s", ip);
        getTmpFilePath(tmp_file_path, unique_key_of_file);
        // Close stdout that child will not wrote on the screen
        fclose(stdout);
        // Give to possibility the other party accept my
        // incoming connection and close previous connection
        sleep(3);
        execlp("pin", "", "-t", PIN_TOOL_PATH, "-o", tmp_file_path,
                "--", pathToApp, port_tmp, ip_tmp, NULL);
        exit(ERR_EXIT_CODE);
    }
    return true;
}
bool PinLoader :: pinBlockWait() {
    int status;
    if(pin_proc == -1) {
        return false;
    }
    waitpid(pin_proc, &status, WCONTINUED);
    if(WIFEXITED(status) == 0) {
        return true;
    }
    if(WEXITSTATUS(status) == ERR_EXIT_CODE) {
        return true;
    }
    return true;
}
bool PinLoader :: pinKill() {
    int status;
    // If pin wasn't run
    if(pin_proc == -1) {
        return false;
    }
    // If pin yet didn't terminated
    if(waitpid(pin_proc, &status, WNOHANG) == 0) {
        kill(pin_proc, SIGKILL);
    }
    // Allow to run pin yet some times
    hardReset();
    return true;
}
byte* PinLoader :: getBinary() {
    char tmp_file_path[128];
    FILE* tmp_file;
    getTmpFilePath(tmp_file_path, unique_key_of_file);
 
    tmp_file = fopen(tmp_file_path, "rb");
    if(tmp_file == NULL) {
        return NULL;
    }
    
    fseek(tmp_file, 0, SEEK_END);
    int size, nsize;
    size = nsize = ftell(tmp_file);
    byte* binary = new byte[sizeof(int) + size];
    // Host byte order to network byte order
    hton((byte*)&nsize, sizeof(int));
    // Write length
    memcpy(&binary[0], &nsize, sizeof(int));
    // Write other data
    fseek(tmp_file, 0, SEEK_SET);
    size_t reading_items = fread(&binary[0 + sizeof(int)], sizeof(byte),
            size, tmp_file);
    if(reading_items < size) {
        delete[] binary;
        binary = NULL;
    }
    fclose(tmp_file);

    return binary;
}