/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.duplicate;

import com.datastax.oss.driver.shaded.guava.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "duplicate to related",
        configClazz = DuplicateToRelatedNodeConfiguration.class,
        nodeDescription = "Duplicate message to related entity",
        nodeDetails = "This node duplicates the incoming message to a related entity."
)
@Slf4j
public class DuplicateToRelatedNode extends TbAbstractExternalNode {
    private DuplicateToRelatedNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, DuplicateToRelatedNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String relationType = config.getRelationType();
        String relationDirection = config.getRelationDirection();

        // Lấy entityId của thực thể hiện tại từ tin nhắn
        EntityId originator = msg.getOriginator();

        try {
            List<EntityRelation> relations;

            if ("FROM".equalsIgnoreCase(relationDirection)) {
                relations = ctx.getRelationService().findByFromAndType(ctx.getTenantId(), originator, relationType, RelationTypeGroup.COMMON);
            } else {
                relations = ctx.getRelationService().findByToAndType(ctx.getTenantId(), originator, relationType, RelationTypeGroup.COMMON);
            }

            if (relations.isEmpty()) {
                // Nếu không tìm thấy mối quan hệ nào, chuyển tin nhắn đến node "failure"
                ctx.tellFailure(msg, new RuntimeException("No related entities found"));
                return;
            }

            // Gửi tin nhắn ban đầu đến node "success"
            ctx.tellSuccess(msg);

            // Tạo và gửi bản sao của tin nhắn đến mỗi thực thể liên quan
            for (EntityRelation relation : relations) {
                EntityId targetId = "FROM".equalsIgnoreCase(relationDirection) ? relation.getTo() : relation.getFrom();

                if (targetId.getEntityType() == EntityType.ASSET) {
                    // lấy thông tin của thiết bị liên quan lưu vào metadata (assetId) (assetName)
                    String assetId = targetId.getId().toString();
                    String assetName = ctx.getAssetService().findAssetById(ctx.getTenantId(), (AssetId) targetId).getName();

                    // lưu vao metadata
                    TbMsgMetaData metaData = msg.getMetaData().copy();
                    metaData.putValue("assetId", assetId);
                    metaData.putValue("assetName", assetName);

                    // Gửi tin nhắn đến thiết bị liên quan
                    ctx.tellNext(copyMsg(msg, targetId, metaData), config.getRelatedMsgRelationName());
                }

                // Kiểm tra xem entity là device
                if (targetId.getEntityType() == EntityType.DEVICE) {
                    // lấy thông tin của thiết bị liên quan lưu vào metadata (deviceId) (deviceName)
                    String deviceId = targetId.getId().toString();
                    String deviceName = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) targetId).getName();

                    // lưu vao metadata
                    TbMsgMetaData metaData = msg.getMetaData().copy();
                    metaData.putValue("deviceId", deviceId);
                    metaData.putValue("deviceName", deviceName);


                    System.out.println("Sending message to related device: " + targetId);
                    // Gửi tin nhắn đến thiết bị liên quan
                    ctx.tellNext(copyMsg(msg, targetId, metaData), config.getRelatedMsgRelationName());
                }
            }
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private TbMsg copyMsg(TbMsg msg, EntityId targetId, TbMsgMetaData metaData) {
        return TbMsg.newMsg()
                .type(msg.getType())
                .originator(targetId)
                .copyMetaData(metaData)
                .data(msg.getData())
                .build();
    }
}