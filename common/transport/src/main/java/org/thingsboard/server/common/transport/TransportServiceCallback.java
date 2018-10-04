package org.thingsboard.server.common.transport;

/**
 * Created by ashvayka on 04.10.18.
 */
public interface TransportServiceCallback<T> {

    void onSuccess(T msg);
    void onError(Exception e);

}
