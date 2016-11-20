/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/* 
 * File:   Clients.hpp
 * Author: master
 *
 * Created on November 14, 2016, 3:20 PM
 */

#ifndef MAP_CLIENTS_HPP
#define MAP_CLIENTS_HPP

#include <map>

using namespace std;

#define MAX_NUMBER_CLIENTS 5
    
class MapClients {
    public:
        MapClients();
        bool addClient(int new_client_fds);
        void removeClient(const int old_client_fds);
        ~MapClients();
        
    private:
        map<int, int> map_clients;
};

#endif /* CLIENTS_HPP */

