package org.thingsboard.server;

public interface TbQueueCallback {

    void onSuccess();

    void onFailure(Throwable t);
}
