/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   thread_client.hpp
 * Author: master
 *
 * Created on November 16, 2016, 12:20 AM
 */

#ifndef THREAD_CLIENT_HPP
#define THREAD_CLIENT_HPP

#include <arpa/inet.h>

#include "parser_protocol.hpp"
#include "notice_manager.hpp"

typedef struct __ThreadParams {
    int sock_fd;
    struct sockaddr client_addr;
}ThreadParams;

bool readUTF(int sock_fd, UTF_text* utf_text);
bool writeUTF(int sock_fd, UTF_text* utf_text);
void* start_routine(void* arg);

#endif /* THREAD_CLIENT_HPP */

