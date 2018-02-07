package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Created by ashvayka on 19.01.18.
 */
public class TbNodeException extends Exception {

    public TbNodeException(Exception e) {
        super(e);
    }

}
