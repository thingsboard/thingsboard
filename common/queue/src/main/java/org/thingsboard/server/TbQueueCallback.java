package org.thingsboard.server;

public interface TbQueueCallback {

    void onSuccess(TbQueueMsgMetadata metadata);

    void onFailure(Throwable t);
}
