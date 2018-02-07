package org.thingsboard.rule.engine.api;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
public final class TbMsg implements Serializable {

    private final UUID id;
    private final String type;
    private final EntityId originator;
    private final TbMsgMetaData metaData;

    private final byte[] data;

}
