package org.thingsboard.server;

public interface OnPingCallback {

    void onSuccess(TransportType transportType);
    void onFailure(TransportType transportType);

}