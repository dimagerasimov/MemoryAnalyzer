/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   notice_manager.hpp
 * Author: master
 *
 * Created on November 18, 2016, 5:01 PM
 */

#ifndef NOTICE_MANAGER_HPP
#define NOTICE_MANAGER_HPP

void notice(const char* message);
void ini_client_notice(bool suc, int sock_fd);
void fini_client_notice(int sock_fd);
void bind_notice(short int port);
void err(const char* message);

#endif /* NOTICE_MANAGER_HPP */

