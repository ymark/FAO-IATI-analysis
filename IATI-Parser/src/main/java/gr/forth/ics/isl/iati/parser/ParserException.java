package gr.forth.ics.isl.iati.parser;

/** Generic exception for IATI Parser
 * 
 * @author Yannis Marketakis (marketak 'at' ics 'dot' forth 'dot' gr)
 */
public class ParserException extends Exception{

    public ParserException(String msg){
        super(msg);
    }
    
    public ParserException(String msg, Throwable thr){
        super(msg,thr);
    }
}