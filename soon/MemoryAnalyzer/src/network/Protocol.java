/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

/**
 *
 * @author master
 */
public class Protocol {    
    // Constant define a timeouts
    public static final int CONNECTION_TIMEOUT = 10;
    
    // Symbol delimiters of protocol
    public final static String COM_DELIMITER = "`";
    public final static String ARGS_DELIMITER = "~";
    
    // Answers of protocol
    public final static String SUCCESS = "suc";
    public final static String ERROR = "err";
    
    // Commands of protocol
    public final static String HI = "hi";
    public final static String BYE = "bye";
    public final static String PIN_INIT = "p_in";
    public final static String PIN_EXEC = "p_ex";
    public final static String GET_BINARY = "gbin";
}
