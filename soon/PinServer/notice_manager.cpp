/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include <stdlib.h>
#include <iostream>

#include "notice_manager.hpp"

using namespace std;

void bind_notice(short int port) {
    cout << "Bind on " << port << " port successfully." << endl;
}
void notice(const char* message) {
    cout << message << endl;
}
void err(const char* message) {
    cerr << message << endl;
    exit(-1);
}
void ini_client_notice(bool suc, int sock_fd) {
    if(!suc) {
        cout << "The client with key \""
                << sock_fd << "\" was ignored." << endl;
    }
    else {
        cout << "The client with key \""
                << sock_fd << "\" was added successfully." << endl;
    }
}
void fini_client_notice(int sock_fd) {
    cout << "The client with key \""
        << sock_fd << "\" was finished successfully." << endl;
}
void client_hung_up(int sock_fd) {
    cout << "The client with key \""
        << sock_fd << "\" hung up." << endl;
}
void connection_reset(int sock_fd) {
    cout << "The client with key \"" << sock_fd <<
        "\" has exceeded the limit of incorrect queries. Connection reset." << endl;
}