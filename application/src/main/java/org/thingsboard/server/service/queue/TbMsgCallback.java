package org.thingsboard.server.service.queue;

public interface TbMsgCallback {

    void onSuccess();

    void onFailure(Throwable t);

}
