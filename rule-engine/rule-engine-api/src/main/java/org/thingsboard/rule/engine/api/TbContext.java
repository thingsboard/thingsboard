package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.dao.attributes.AttributesService;

import java.util.UUID;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    void tellNext(TbMsg msg);

    void tellNext(TbMsg msg, String relationType);

    void tellSelf(TbMsg msg, long delayMs);

    void tellOthers(TbMsg msg);

    void tellSibling(TbMsg msg, ServerAddress address);

    void spawn(TbMsg msg);

    void ack(UUID msg);

    AttributesService getAttributesService();

}
