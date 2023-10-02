package org.thingsboard.server.service.queue.ruleengine;

import java.io.Serializable;

public enum QueueEvent implements Serializable {

    CREATED, LAUNCHED, UPDATED, PARTITION_CHANGE, STOP, DELETED

}
