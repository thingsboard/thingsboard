package org.thingsboard.server.service.install.exception;

public class IncompatibleSchemaVersion extends RuntimeException{
    private static final long serialVersionUID = 6753226763271917587L;
    public IncompatibleSchemaVersion(String msg) {
        super(msg);
    }
}
