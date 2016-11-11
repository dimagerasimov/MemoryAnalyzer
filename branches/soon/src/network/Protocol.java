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
    public static final int TIMEOUT_CONNECTION = 20;
    public static final int TIMEOUT_ACCEPT = 100;
    
    // Symbol delimiters of protocol
    public final static String COM_DELIMITER = "`";
    public final static String ARGS_DELIMITER = "~";
    
    // Answers of protocol
    public final static String OK = "ok";
    public final static String NO = "no";
    
    // Commands of protocol
    public final static String ERROR = "err";
    public final static String IS_END = "end";
    public final static String CLOSE = "cl";
    public final static String PIN_INIT = "p_in";
    public final static String PIN_EXEC = "p_ex";
}
