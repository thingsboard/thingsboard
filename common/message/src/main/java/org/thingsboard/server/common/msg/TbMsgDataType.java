package org.thingsboard.server.common.msg;

/**
 * Created by ashvayka on 15.03.18.
 */
public enum TbMsgDataType {

    // Do not change ordering. We use ordinal to save some bytes on serialization
    JSON, TEXT, BINARY;

}
