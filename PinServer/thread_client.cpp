/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <memory.h>
#include <unistd.h>

#include "thread_client.hpp"
#include "pin_loader.hpp"
#include "gdb_loader.hpp"

#define MAX_SIZE_PACKET_4FILE (1<<22) // equals 4MB

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
    int real_length = ((int*)binary)[0]; // only the size of data
    ntoh((byte*)&real_length, sizeof(int)); // the network byte order to the host byte order
    real_length += sizeof(int); // the full size of the transmitted data
    
    int offset, write_length, size_part, num_packets;
    num_packets = real_length / MAX_SIZE_PACKET_4FILE;
    offset = 0;
    size_part = MAX_SIZE_PACKET_4FILE;
    for(int i = 0; i < num_packets; i++) {
        write_length = send(sock_fd, &binary[offset], size_part, MSG_NOSIGNAL);
        if(write_length < size_part) {
            return false;
        }
        offset += size_part;
    }
    size_part = real_length - num_packets * MAX_SIZE_PACKET_4FILE;
    if(size_part > 0) {
        write_length = send(sock_fd, &binary[offset], size_part, MSG_NOSIGNAL);
        if(write_length < size_part) {
            return false;
        }
    }
    return true;
}
bool checkCommand(const char* command, const char* text) {
    return strcmp(command, text) == 0;
}
void* start_routine(void* arg) {
    const unsigned int MAX_ERRORS_COUNT = 3U;
    unsigned int errors_count = 0U;
    UTF_text utf_text;
    char command[MAX_COMMAND_LENGTH];
    char text_command[MAX_MESSAGE_LENGTH];
    char text_arg[MAX_MESSAGE_LENGTH];

    PinLoader pinLoader;
    GdbLoader gdbLoader;
    ThreadParams* threadParams = (ThreadParams*)arg;
    while(1) {
        bool error_caused = false;
        // Reading and parsing input command
        if(!readUTF(threadParams->sock_fd, &utf_text)) {
            print_notice(CLIENT_HUNG_UP, &threadParams->sock_fd);
            break;
        }
        parseUTFtext_command(&utf_text, command, text_command);

        // Recognizing input command
        if(checkCommand(HI, command))
        {
            utf_text.setText(HI, strlen(HI));
        }
        else if(checkCommand(BYE, command))
        {
            print_notice(FINI_CLIENT_NOTICE, &threadParams->sock_fd);
            break;
        }
        else if(checkCommand(PIN_INIT, command))
        {
            bool operation_result = parse_get_argument(text_command, text_arg, 0, ARGS_DELIMITER);
            if (operation_result) {
                pinLoader.setPort(text_arg);
                operation_result = parse_get_argument(text_command, text_arg, 1, ARGS_DELIMITER);
            }
            if (operation_result) {
                pinLoader.setPathToApp(text_arg);
                operation_result = parse_get_argument(text_command, text_arg, 2, ARGS_DELIMITER);
            }
            if (operation_result) {
                pinLoader.setArgsForApp(text_arg);
                pinLoader.setKey(threadParams->sock_fd);
                operation_result = pinLoader.isPinReady();
            }
            if (operation_result) {
                utf_text.setText(SUCCESS, strlen(SUCCESS));
            }
            error_caused = !operation_result;
        }
        else if(checkCommand(PIN_EXEC, command))
        {
            struct sockaddr_in *tmp = (struct sockaddr_in *)&threadParams->client_addr;           
            inet_ntop(AF_INET, (const void*)&(tmp->sin_addr),
                    text_arg, sizeof(threadParams->client_addr));
            error_caused = !pinLoader.pinExec(text_arg);
            if (!error_caused) {
                utf_text.setText(SUCCESS, strlen(SUCCESS));
            }
        }
        else if(checkCommand(GDB_RUN, command))
        {
            bool operation_result = parse_get_argument(
                    text_command, text_arg, 0, ARGS_DELIMITER);
            if (operation_result) {
                gdbLoader.SetPathToApp(text_arg);
                operation_result = gdbLoader.GdbRun();
            }
            if (operation_result) {
                utf_text.setText(SUCCESS, strlen(SUCCESS));
            }
            error_caused = !operation_result;
        }
        else if(checkCommand(GDB_REQUEST, command))
        {
            char output_buffer[4096];
            sprintf(output_buffer, "%s ", GDB_REQUEST);
            bool operation_result = parse_get_argument(
                    text_command, text_arg, 0, ARGS_DELIMITER);
            if (operation_result) {
                char request_message[96];
                sprintf(request_message, "list *%s\n", text_arg);
                unsigned int bufferOffset = strlen(GDB_REQUEST) + 1U;
                operation_result = gdbLoader.GdbRequest(request_message,
                        output_buffer + bufferOffset, sizeof(output_buffer) - bufferOffset);
            }
            if (operation_result) {
                utf_text.setText(output_buffer, strlen(output_buffer));
            }
            error_caused = !operation_result;
        }
        else if(checkCommand(GDB_STOP, command))
        {
            gdbLoader.GdbStop();
            utf_text.setText(SUCCESS, strlen(SUCCESS));
        }
        else if(checkCommand(GET_BINARY, command)) {
            error_caused = !pinLoader.pinBlockWait();
            if (!error_caused) {
                byte* binary = pinLoader.getBinary();
                if(binary == NULL) {
                    error_caused = true;
                } else {
                    utf_text.setText(SUCCESS, strlen(SUCCESS));
                    if(!writeUTF(threadParams->sock_fd, &utf_text)) {
                        delete[] binary;
                        print_notice(CLIENT_HUNG_UP, &threadParams->sock_fd);
                        break;
                    }
                    if(!writeBinFile(threadParams->sock_fd, binary)) {
                        delete[] binary;
                        print_notice(CLIENT_HUNG_UP, &threadParams->sock_fd);
                        break;
                    }
                    delete[] binary;
                    continue;
                }
            }
        }
        else
        {
            error_caused = true;
        }

        if (error_caused) {
            utf_text.setText(ERROR, strlen(ERROR));
            errors_count++;
        }
        
        if(!writeUTF(threadParams->sock_fd, &utf_text)) {
            print_notice(CLIENT_HUNG_UP, &threadParams->sock_fd);
            break;
        }
        if(errors_count >= MAX_ERRORS_COUNT) {
            print_notice(CONNECTION_RESET, &threadParams->sock_fd);
            break;
        }
    }
    close(threadParams->sock_fd);
    
    delete threadParams;
    return NULL;
}
