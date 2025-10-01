package org.thingsboard.server.service.install.exception;

public class SchemaUpToDate extends RuntimeException{
    private static final long serialVersionUID = -2060200267208909638L;
    public SchemaUpToDate(String msg) {
        super(msg);
    }
}
