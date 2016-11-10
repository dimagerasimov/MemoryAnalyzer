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
    // Symbol delimiter in protocol
    public final static String DELIMITER = "~";
    
    // Commands of protocol below
    public final static String OK = "ok";
    public final static String ERROR = "err";
    public final static String CLOSE = "cl";
    public final static String PIN_INIT = "p_in";
    public final static String PIN_EXEC = "p_ex";
}
