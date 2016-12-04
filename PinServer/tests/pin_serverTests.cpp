/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   pin_serverTests.cpp
 * Author: master
 *
 * Created on December 2, 2016, 2:03 AM
 */

#include <stdlib.h>
#include <iostream>
#include <unistd.h>
#include <memory.h>
#include <stdio.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <arpa/inet.h>

#include "../protocol.hpp"
#include "../utf_text.hpp"

/*
 * Simple C++ Test Suite
 */

#define PAUSE_USEC 500000
#define NUM_CLIENTS 30
#define DEFAULT_PORT 4028
#define PATH_TO_EXEC "./dist/Release/GNU-Linux/pinserver"

// Global variables
pid_t pin_proc;
struct timeval start_time;

void initTestTime() {
    gettimeofday(&start_time, NULL);
}

double getTestTimePassed() {
    struct timeval current_time;
    gettimeofday(&current_time, NULL);
    int sec = (current_time.tv_sec - start_time.tv_sec);
    int ms = (current_time.tv_usec - start_time.tv_usec) / 1000;
    return  sec + ms / 1000.0;
}

bool recvUTF(int sock_fd, UTF_text* utf_text) {
    int read_length = recv(sock_fd, utf_text->getContent(),
            MAX_MESSAGE_LENGTH, MSG_NOSIGNAL);
    if(read_length <= 2) {
        return false;
    }
    utf_text->getContent()[read_length] = '\0';
    return true;
}

bool sendUTF(int sock_fd, UTF_text* utf_text) {
    int write_length = send(sock_fd, utf_text->getContent(),
            utf_text->getLength() + sizeof(short int), MSG_NOSIGNAL);
    if(write_length <= utf_text->getLength()) {
        return false;
    }
    return true;
}

void failedTest(char* test_name) {
    std::cout << "%TEST_FAILED% time=" << getTestTimePassed()
            << " testname=" << test_name << " (pin_serverTests) message=error message sample" << std::endl;
}

int connectToServer() {
    int sock_fd = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in sock_in;
    memset(&sock_in, 0, sizeof(sock_in));
    sock_in.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    sock_in.sin_family = AF_INET;
    sock_in.sin_port = htons(DEFAULT_PORT);
    if(connect(sock_fd, (const struct sockaddr *)&sock_in,
            sizeof(sock_in)) != 0) {
        close(sock_fd);
        sock_fd = -1;
    }
    return sock_fd;
}

bool disconnectToServer(int sock_fd) {
    if(sock_fd == -1) {
        return false;
    }
    else {
        close(sock_fd);
        return true;
    }
}

void testRunServer() {
    int status;
    pin_proc = fork();
    if(pin_proc == 0) {
        fclose(stdout);
        execl(PATH_TO_EXEC, "", NULL);
        exit(-1);
    }
    // Allow to run server
    sleep(1);
    if(pin_proc == -1 || waitpid(pin_proc, &status, WNOHANG) != 0) {
        // if server wasn't run
        failedTest((char*)"testRunServer");
    }
}

void* testBadClient(void* arg_test_result) {
    int sock_fd;
    bool sendOk, recvOk;
    UTF_text send_utf_text, recv_utf_text;
    
    sock_fd = connectToServer();
    if(sock_fd == -1) {
        *(bool*)arg_test_result = false;
        return arg_test_result;
    }
    while(1) {
        send_utf_text.setText(HI, strlen(HI));
        sendOk = sendUTF(sock_fd, &send_utf_text);
        if(!sendOk) {
            break;
        }
        recvOk = recvUTF(sock_fd, &recv_utf_text);
        if(!recvOk) {
            break;
        } else if(strcmp(HI, recv_utf_text.getText()) != 0) {
            *(bool*)arg_test_result = false;
            return arg_test_result;
        } 

        usleep(PAUSE_USEC);
        
        char tmp_pin_init[MAX_MESSAGE_LENGTH];
        sprintf(tmp_pin_init, "%s%c%s%c%s%c%s", (char*)PIN_INIT, COM_DELIMITER, (char*)"arg1",
                ARGS_DELIMITER, (char*)"arg2", ARGS_DELIMITER, (char*)"arg3");
        send_utf_text.setText(tmp_pin_init, strlen(tmp_pin_init));
        sendOk = sendUTF(sock_fd, &send_utf_text);
        if(!sendOk) {
            break;
        }
        recvOk = recvUTF(sock_fd, &recv_utf_text);
        if(!recvOk) {
            break;
        } else if(strcmp(ERROR, recv_utf_text.getText()) != 0) {
            *(bool*)arg_test_result = false;
            return arg_test_result;
        }

        usleep(PAUSE_USEC);

        send_utf_text.setText(PIN_EXEC, strlen(PIN_EXEC));
        sendOk = sendUTF(sock_fd, &send_utf_text);
        if(!sendOk) {
            break;
        }
        recvOk = recvUTF(sock_fd, &recv_utf_text);
        if(!recvOk) {
            break;
        } else if(strcmp(ERROR, recv_utf_text.getText()) != 0) {
            *(bool*)arg_test_result = false;
            return arg_test_result;
        }

        usleep(PAUSE_USEC);

        send_utf_text.setText(GET_BINARY, strlen(GET_BINARY));
        sendOk = sendUTF(sock_fd, &send_utf_text);
        if(!sendOk) {
            break;
        }
        recvOk = recvUTF(sock_fd, &recv_utf_text);
        if(!recvOk) {
            break;
        } else if(strcmp(ERROR, recv_utf_text.getText()) != 0) {
            *(bool*)arg_test_result = false;
            return arg_test_result;
        }

        usleep(PAUSE_USEC);
    }
    // try to disconnect to server
    if(!disconnectToServer(sock_fd)) {
        *(bool*)arg_test_result = false;
        return arg_test_result;
    }
    *(bool*)arg_test_result = true;
    return arg_test_result;
}

bool testIsServerAvailable() {
    int sock_fd;
    bool sendOk, recvOk;
    UTF_text send_utf_text, recv_utf_text;
    
    sock_fd = connectToServer();
    if(sock_fd == -1) {
        return false;
    }
    
    send_utf_text.setText(HI, strlen(HI));
    sendOk = sendUTF(sock_fd, &send_utf_text);
    if(!sendOk) {
        return false;
    }
    recvOk = recvUTF(sock_fd, &recv_utf_text);
    if(!recvOk || strcmp(HI, recv_utf_text.getText()) != 0) {
        return false;
    }
    
    send_utf_text.setText(BYE, strlen(BYE));
    sendOk = sendUTF(sock_fd, &send_utf_text);
    if(!sendOk) {
        return false;
    }
    
    // try to disconnect to server
    if(!disconnectToServer(sock_fd)) {
        return false;
    }
    return true;
}

void testManyClientsSingleThread() {
    bool resBadClientsTest[NUM_CLIENTS];
    for(int i = 0; i < NUM_CLIENTS; i++) {
        testBadClient(&resBadClientsTest[i]);
    }

    bool total_result = true;
    for(int i = 0; i < NUM_CLIENTS; i++) {
        std::cout << "%TEST_STARTED% testBadClient (pin_serverTests)" << std::endl;
        if(!resBadClientsTest[i]) {
            failedTest((char*)"testBadClient");
            total_result = total_result && false;
        }
        std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
                << " testBadClient (pin_serverTests)" << std::endl;
    }
    if(!total_result) {
        failedTest((char*)"testManyClientsSingleThread");
    }
}

void testManyClientsMultiThread(bool non_stop) {
    int err;
    bool resBadClientsTest[NUM_CLIENTS];
    pthread_t badClientsPid[NUM_CLIENTS];
    for(int i = 0; i < NUM_CLIENTS; i++) {
        err = pthread_create(&badClientsPid[i], 0, testBadClient, &resBadClientsTest[i]);
        if(err != 0) {
            std::cerr << "Error: Unable to create new thread." << std::endl;
            continue;
        }
        if(!non_stop) {
            usleep(PAUSE_USEC);
        }
    }
    
    int status;
    void* plug;
    bool total_result = true;
    for(int i = 0; i < NUM_CLIENTS; i++) {
        std::cout << "%TEST_STARTED% testBadClient (pin_serverTests)" << std::endl;
        waitpid(badClientsPid[i], &status, WCONTINUED);
        err = pthread_join(badClientsPid[i], &plug);
        if(err != 0) {
            std::cerr << "Error: Thread can't be terminated." << std::endl;
            continue;
        }
        if(!resBadClientsTest[i]) {
            failedTest((char*)"testBadClient");
            total_result = total_result && false;
        }
        std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
                << " testBadClient (pin_serverTests)" << std::endl;
    }
    if(!total_result) {
        failedTest((char*)"testManyClientsMultiThread");
    }
}

void testKillServer() {
    int status;
    if(waitpid(pin_proc, &status, WNOHANG) == 0) {
        kill(pin_proc, SIGKILL);
    } else {
        // is server already was finished (impossible)
        failedTest((char*)"testKillServer");
    }
}

int main(int argc, char** argv) {
    std::cout << "%SUITE_STARTING% pin_serverTests" << std::endl;
    std::cout << "%SUITE_STARTED%" << std::endl;
    
    initTestTime();
    
    std::cout << "%TEST_STARTED% testRunServer (pin_serverTests)" << std::endl;
    testRunServer();
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testRunServer (pin_serverTests)" << std::endl;

    std::cout << "%TEST_STARTED% testIsServerAvailable (pin_serverTests)" << std::endl;
    if(!testIsServerAvailable()) {
        failedTest((char*)"testIsServerAvailable");
    }
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testIsServerAvailable (pin_serverTests)" << std::endl;
    
    std::cout << "%TEST_STARTED% testManyClientsSingleThread (pin_serverTests)" << std::endl;
    testManyClientsSingleThread();
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testManyClientsSingleThread (pin_serverTests)" << std::endl;

    std::cout << "%TEST_STARTED% testManyClientsMultiThread (pin_serverTests)" << std::endl;
    testManyClientsMultiThread(true);
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testManyClientsMultiThread (pin_serverTests)" << std::endl;
    
    std::cout << "%TEST_STARTED% testManyClientsMultiThread (pin_serverTests)" << std::endl;
    testManyClientsMultiThread(false);
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testManyClientsMultiThread (pin_serverTests)" << std::endl;

    sleep(CONNECTION_TIMEOUT + 1);

    std::cout << "%TEST_STARTED% testIsServerAvailable (pin_serverTests)" << std::endl;
    if(!testIsServerAvailable()) {
        failedTest((char*)"testIsServerAvailable");
    }
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testIsServerAvailable (pin_serverTests)" << std::endl;

    std::cout << "%TEST_STARTED% testKillServer (pin_serverTests)" << std::endl;
    testKillServer();
    std::cout << "%TEST_FINISHED% time=" << getTestTimePassed()
            << " testKillServer (pin_serverTests)" << std::endl;
    
    std::cout << "%SUITE_FINISHED% time=" << getTestTimePassed() << std::endl;

    return (EXIT_SUCCESS);
}

