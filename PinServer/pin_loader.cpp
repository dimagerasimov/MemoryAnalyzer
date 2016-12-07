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
#include "parser_protocol.hpp"

#define ERR_EXIT_CODE 1234

PinLoader :: PinLoader() {
    hardReset();
}
PinLoader :: ~PinLoader() {
    pinKill();
}
void PinLoader :: hardReset() {
    init = false;
    pin_proc = -1;
    unique_key_of_file = -1;
    portToConnect = 0;
    pathToApp[0] = '\0';
}
void setTmpFilePath(char buffer[], pid_t pin_proc) {
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
bool PinLoader :: setArgsForApp(const char* argsForApp) {
    try {
        strcpy(this->argsForApp, argsForApp);
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
bool PinLoader :: isPinExist() {
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
    fclose(tmp);
    tmp = fopen(PIN_TOOL_PATH, "rb");
    if(tmp == NULL) {
        return false;
    }
    fclose(tmp);
    if(unique_key_of_file == -1) {
        return false;
    }
    char tmp_file_path[128];
    setTmpFilePath(tmp_file_path, unique_key_of_file);
    remove(tmp_file_path);
    
    init = isPinExist();
    return init;
}
bool PinLoader :: pinExec(char* ip) {
    // Test re-run
    if(!init || pin_proc != -1) {
        return false;
    }
    // Make fork current process
    pin_proc = fork();
    if(pin_proc == -1) {
        return false;
    }
    else if(pin_proc == 0) {      
        // Close stdout that child will not wrote on the screen
        fclose(stdout);
        // Give to possibility the other party accept my
        // incoming connection and close previous connection
        sleep(3);
        // 64 is limit of arguments
        char** args = new char*[64];
        args[0] = new char[strlen("") + 1];
        strcpy(args[0], "");
        
        args[1] = new char[strlen("-t") + 1];
        strcpy(args[1], "-t");
        
        args[2] = new char[strlen(PIN_TOOL_PATH) + 1];
        strcpy(args[2], PIN_TOOL_PATH);
        
        args[3] = new char[strlen("-o") + 1];
        strcpy(args[3], "-o");
        
        args[4] = new char[256];
        setTmpFilePath(args[4], unique_key_of_file);
       
        args[5] = new char[strlen("--") + 1];
        strcpy(args[5], "--");
        
        args[6] = new char[256];
        strcpy(args[6], pathToApp);
        
        int count = -1;
        bool parse_result = true;
        do {
            count += 1;
            args[7 + count] = new char[256];
            parse_result = parse_get_argument(
                    argsForApp, args[7 + count], count, ' ');
        } while(parse_result);      
                        
        // For this string memory allocated before (in loop)
        sprintf(args[7 + count], "ip=%s", ip);
        
        args[8 + count] = new char[32];
        sprintf(args[8 + count], "port=%d", portToConnect);
        
        args[9 + count] = NULL;
        
        execvp("pin", (char* const*)args);
        exit(ERR_EXIT_CODE);
    }
    return true;
}
bool PinLoader :: pinBlockWait() {
    int status;
    if(!init || pin_proc == -1) {
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
    if(!init || pin_proc == -1) {
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
    
    setTmpFilePath(tmp_file_path, unique_key_of_file);
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