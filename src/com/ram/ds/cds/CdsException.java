package com.ram.ds.cds;

/**
 */
public class CdsException extends RuntimeException {

    private static final long serialVersionUID = -1079731074584808363L;
    String message;
    Exception innerException;

    public CdsException( String message ) {
    	super(message);
        this.message = message;
    }


    public CdsException( Exception e ) {
    	super(e);
        innerException = e;
        message = e.getMessage();
    }
}


