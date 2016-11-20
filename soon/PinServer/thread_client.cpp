/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <stdlib.h>
#include <string.h>
#include <memory.h>
#include <unistd.h>

#include "thread_client.hpp"
#include "notice_manager.hpp"
#include "pin_loader.hpp"

bool readUTF(int sock_fd, UTF_text* utf_text) {
    int read_length = recv(sock_fd, utf_text->getContent(),
            MAX_MESSAGE_LENGTH, MSG_NOSIGNAL);
    if(read_length <= 2) {
        return false;
    }
    utf_text->getContent()[read_length] = '\0';
    return true;
}
bool writeUTF(int sock_fd, UTF_text* utf_text) {
    int write_length = send(sock_fd, utf_text->getContent(),
            utf_text->getLength() + sizeof(short int), MSG_NOSIGNAL);
    if(write_length <= utf_text->getLength()) {
        return false;
    }
    return true;
}
bool writeBinFile(int sock_fd, byte* binary) {
    int real_length = ((int*)binary)[0] + sizeof(int);
    reverseBytes((byte*)&real_length, sizeof(int));
    int write_length = send(sock_fd, binary, real_length, MSG_NOSIGNAL);
    if(write_length < real_length) {
        return false;
    }
    return true;
}
void* start_routine(void* arg) {
    int lim_error;
    bool parse_result;
    UTF_text utf_text;
    char command[MAX_COMMAND_LENGTH];
    char text_command[MAX_MESSAGE_LENGTH];
    char text_arg[MAX_MESSAGE_LENGTH];

    PinLoader pinLoader;
    ThreadParams* threadParams = (ThreadParams*)arg;
    // Count of errors
    lim_error = 0;
    while(lim_error < 3) {
        // Read and parse input command
        if(!readUTF(threadParams->sock_fd, &utf_text)) {
            break;
        }
        parseUTFtext_command(&utf_text, command, text_command);

        // Recognize input command
        if(strcmp(command, HI) == 0)
        {
            utf_text.setText(HI, strlen(HI));
        }
        else if(strcmp(command, BYE) == 0)
        {
            break;
        }
        else if(strcmp(command, PIN_INIT) == 0)
        {
            parse_result = parse_get_argument(text_command, text_arg, 0);
            pinLoader.setPathToApp(text_arg);
            parse_result &= parse_get_argument(text_command, text_arg, 1);
            pinLoader.setPort(text_arg);      
            
            pinLoader.setKey(threadParams->sock_fd);
            
            if(!parse_result) {
                utf_text.setText(ERROR, strlen(ERROR));
            }
            else {
                if(!pinLoader.isPinReady()) {
                    utf_text.setText(ERROR, strlen(ERROR));
                } else {
                    utf_text.setText(SUCCESS, strlen(SUCCESS));
                }
            }
        }
        else if(strcmp(command, PIN_EXEC) == 0)
        {
            struct sockaddr_in *tmp = (struct sockaddr_in *)&threadParams->client_addr;           
            inet_ntop(AF_INET, (const void*)&(tmp->sin_addr),
                    text_arg, sizeof(threadParams->client_addr));
            if(!pinLoader.pinExec(text_arg)) {
                utf_text.setText(ERROR, strlen(ERROR));
            } else {
                utf_text.setText(SUCCESS, strlen(SUCCESS));
            }
        }
        else if(strcmp(command, GET_BINARY) == 0) {
            if(!pinLoader.pinBlockWait()) {
                utf_text.setText(ERROR, strlen(ERROR));
            } else {
                byte* binary = pinLoader.getBinary();
                if(binary == NULL) {
                    utf_text.setText(ERROR, strlen(ERROR));
                } else {
                    utf_text.setText(SUCCESS, strlen(SUCCESS));
                    if(!writeUTF(threadParams->sock_fd, &utf_text)) {
                        delete[] binary;
                        break;
                    }
                    if(!writeBinFile(threadParams->sock_fd, binary)) {
                        delete[] binary;
                        break;
                    }
                    delete[] binary;
                }
            }
        }
        else
        {
            utf_text.setText(ERROR, strlen(ERROR));
        }
        
        // If client makes mistake then increase count of error
        parseUTFtext_command(&utf_text, command, text_command);
        if(strcmp(command, ERROR) == 0) {
            lim_error++;
        }
        
        if(!writeUTF(threadParams->sock_fd, &utf_text)) {
            break;
        }
    }
    pinLoader.pinKill();
    close(threadParams->sock_fd);
    fini_client_notice(threadParams->sock_fd);
    
    delete threadParams;
    return NULL;
}
