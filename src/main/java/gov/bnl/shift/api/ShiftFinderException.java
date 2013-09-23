/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package gov.bnl.shift.api;

import javax.swing.text.html.parser.*;
import java.io.*;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Created with IntelliJ IDEA.
 * User: eschuhmacher
 * Date: 9/23/13
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShiftFinderException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 6279865221993808192L;

    private Status status;

    public ShiftFinderException() {
        super();
    }

    public ShiftFinderException(String message){
        super(message);
    }

    public ShiftFinderException(UniformInterfaceException cause) {
        super(parseErrorMsg(cause), cause);
        this.setStatus(Status.fromStatusCode(cause.getResponse().getStatus()));
    }

    private static String parseErrorMsg(UniformInterfaceException ex) {
        String entity = ex.getResponse().getEntity(String.class);
        try {
            ClientResponseParser callback = new ClientResponseParser();
            Reader reader = new StringReader(entity);
            new ParserDelegator().parse(reader, callback, false);
            return callback.getMessage();
        } catch (IOException e) {
            //e.printStackTrace();
            return "Could not retrieve message from server";
        }
    }

    public ShiftFinderException(Status status, String message) {
        super(message);
        this.setStatus(status);
    }

    /**
     *
     * @param status - the http error status code
     * @param cause - the original UniformInterfaceException
     * @param message - additional error information
     */
    public ShiftFinderException(Status status, Throwable cause ,String message) {
        super(message, cause);
        this.setStatus(status);
    }

    /**
     * Set the associated HTTP status code which caused this exception.
     *
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the associated HTTP status code which caused this exception.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }


}
