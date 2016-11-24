/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   main.cpp
 * Author: master
 *
 * Created on November 14, 2016, 2:37 PM
 */

#include <unistd.h>
#include <memory.h>
#include <pthread.h>
#include <stdlib.h>

#include "map_clients.hpp"
#include "thread_client.hpp"

#define DEFAULT_PORT 4028

pthread_mutex_t mutex;

int parseToPort(int argc, char* argv[]) {
    int port = DEFAULT_PORT;
    char s_port[] = "port=";
    char* p;
    try {
        for(int i = 0; i < argc; i++) {
            p = strstr(argv[i], s_port);
            if(p != NULL) {
                port = atoi(p + strlen(s_port));
                if(port == 0) {
                    throw port;
                }
                break;
            }
        }
    } catch(...) {
        print_notice(NOTICE, (void*)"Invalid arguments command line! Will be used default port...");
        port = DEFAULT_PORT;
    }
    return port;
}

int main(int argc, char* argv[]) {
    // Declaration of variables
    int listen_fd, result;
    struct sockaddr_in server_addr;
    MapClients clients;
    
    // Initialization of mutex//////
    pthread_mutex_init(&mutex, NULL);
    /////////////////////////////////
    
    listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    if(listen_fd == -1) {
        print_notice(ERR, (void*)"Internal error! Not available file descriptors!");
    }
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(parseToPort(argc, argv));
    server_addr.sin_family = AF_INET;
    
    result = bind(listen_fd, (const struct sockaddr*) &server_addr, sizeof(server_addr));
    if(result != 0) {
        print_notice(ERR, (void*)"Internal error! Bind operation failed!");
    }
    else {
        int ntohport = ntohs(server_addr.sin_port);
        print_notice(BIND_NOTICE, (void*)&ntohport);
    }
    result = listen(listen_fd, MAX_NUMBER_CLIENTS);
    print_notice(NOTICE, (void*)"Pin server was started...");
    while(1) {
        struct sockaddr client_addr;
        socklen_t length_addr = sizeof(client_addr);
        int new_client = accept(listen_fd, &client_addr, &length_addr);
        if(new_client == -1) {
            print_notice(NOTICE, (void*)"Unable to receive incoming connection.");
        }
        else {
            bool isOk = clients.addClient(new_client);
            if(!isOk) {
                close(new_client);
            } else {
                // Set timeout on socket
                struct timeval timeout;
                timeout.tv_sec = CONNECTION_TIMEOUT;
                timeout.tv_usec = 0;               
                isOk = setsockopt(new_client, SOL_SOCKET,
                        SO_RCVTIMEO, (void*)&timeout, sizeof(timeout)) == 0;
                isOk &= setsockopt(new_client, SOL_SOCKET,
                        SO_SNDTIMEO, (void*)&timeout, sizeof(timeout)) == 0;
                if(!isOk) {
                    close(new_client);
                }
                ThreadParams* threadParams = new ThreadParams;
                threadParams->sock_fd = new_client;
                threadParams->client_addr = client_addr;       
                pthread_t threadId;
                int err = pthread_create(&threadId, 0, start_routine, threadParams);
                if(err != 0) {
                    print_notice(NOTICE, (void*)"Internal error! Server tries to continue...");
                    continue;
                }
            }
            // Type message on the console - "was or wasn't added"
            if(!isOk) {
                print_notice(INI_FAIL_CLIENT_NOTICE, &new_client);
            } else {
                print_notice(INI_SUC_CLIENT_NOTICE, &new_client);
            }
        }
    }   
    close(listen_fd);
    print_notice(NOTICE, (void*)"Pin server was finished...");
    
    // Initialization of mutex//////
    pthread_mutex_destroy(&mutex);
    /////////////////////////////////
    return 0;
}

