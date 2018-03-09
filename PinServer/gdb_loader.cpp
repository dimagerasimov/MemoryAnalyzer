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

#include "gdb_loader.hpp"
#include "parser_protocol.hpp"

#define ERR_EXIT_CODE 1234

GdbLoader :: GdbLoader() {
    HardReset();
}
GdbLoader :: ~GdbLoader() {
    GdbStop();
}
void GdbLoader :: HardReset() {
    gdb_proc = -1;
    gdb_stdin = -1;
    gdb_stdout = -1;
    pathToApp[0] = '\0';
}
bool GdbLoader :: SetPathToApp(const char* pathToApp) {
    try {
        strcpy(this->pathToApp, pathToApp);
        return true;
    } catch(...) {
        return false;
    }
}
bool GdbLoader :: GdbRun() {
    // Test re-run
    if(gdb_proc != -1) {
        return false;
    }
    int readpair_fd[2];
    int writepair_fd[2];
    if (pipe(readpair_fd) == -1) {
        return false;
    }
    if (pipe(writepair_fd) == -1) {
        close(readpair_fd[0]);
        close(readpair_fd[1]);
        return false;
    }
    // Make fork of the current process
    gdb_proc = fork();
    if(gdb_proc == -1) {
        return false;
    }
    else if(gdb_proc == 0) {      
        // Close stdout since a child should not write to the screen
        //fclose(stdout);
        close(readpair_fd[0]); //close the read end
        dup2(readpair_fd[1], STDOUT_FILENO);
        dup2(readpair_fd[1], STDERR_FILENO);
        close(writepair_fd[1]); //close the write end
        dup2(writepair_fd[0], STDIN_FILENO);
        char* args[1] = { NULL };
        execvp("bash", args);
        exit(ERR_EXIT_CODE);
    }
    close(readpair_fd[1]); //close the write end
    close(writepair_fd[0]); //close the read end
    char run_comm[256];
    sprintf(run_comm, "gdb \"%s\"\n", pathToApp);
    if (write(writepair_fd[1], run_comm, strlen(run_comm)) != strlen(run_comm)) {
        return false;
    }
    usleep(200000U); //200 ms
    gdb_stdin = writepair_fd[1];
    gdb_stdout = readpair_fd[0];
    return true;
}
bool GdbLoader :: GdbRequest(char requestText[], char* output_buffer, const int size_output_buffer) {
    if (gdb_proc == -1 || gdb_stdin == -1 || gdb_stdout == -1) {
        return false;
    }
    if (write(gdb_stdin, requestText, strlen(requestText)) != strlen(requestText)) {
        return false;
    }
    usleep(100000U); //100 ms
    const int read_count = read(gdb_stdout, output_buffer, size_output_buffer);
    if (read_count == 0U) {
        return false;
    }
    output_buffer[read_count] = '\0';
    return (read_count != 0);
}
bool GdbLoader :: GdbStop() {
    int status;
    // If gdb was never run
    if(gdb_proc == -1) {
        return false;
    }
    // If gdb yet didn't terminate
    if(waitpid(gdb_proc, &status, WNOHANG) == 0) {
        kill(gdb_proc, SIGKILL);
    }
    HardReset();
    return true;
}