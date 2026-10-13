// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.rule.engine.action.TbChangeOwnerNode;
import org.thingsboard.rule.engine.action.TbSaveToCustomCassandraTableNode;
import org.thingsboard.rule.engine.ai.TbAiNode;
import org.thingsboard.rule.engine.aws.lambda.TbAwsLambdaNode;
import org.thingsboard.rule.engine.report.TbGenerateReportV2Node;
import org.thingsboard.rule.engine.rest.TbSendRestApiCallReplyNode;
import org.thingsboard.rule.engine.telemetry.TbCalculatedFieldsNode;
import org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ApiKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AttributeDeleteMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomMenuProto;
import org.thingsboard.server.gen.edge.v1.CustomTranslationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceGroupOtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.EncryptionKeyUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityGroupUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationRuleUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTargetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.NotificationTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RoleProto;
import org.thingsboard.server.gen.edge.v1.RpcRequestMsg;
import org.thingsboard.server.gen.edge.v1.RpcResponseMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SecretUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WhiteLabelingProto;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class EdgeMsgConstructorUtils {
    public static final Map<EdgeVersion, Map<String, String>> IGNORED_PARAMS_BY_EDGE_VERSION = Map.of(
            EdgeVersion.V_3_9_0,
            Map.of(
                    TbMsgTimeseriesNode.class.getName(), "processingSettings",
                    TbMsgAttributesNode.class.getName(), "processingSettings"
            ),
            EdgeVersion.V_3_8_0,
            Map.of(
                    TbMsgTimeseriesNode.class.getName(), "processingSettings",
                    TbMsgAttributesNode.class.getName(), "processingSettings",
                    TbSaveToCustomCassandraTableNode.class.getName(), "defaultTtl"
            ),
            EdgeVersion.V_3_7_0,
            Map.of(
                    TbMsgTimeseriesNode.class.getName(), "processingSettings",
                    TbMsgAttributesNode.class.getName(), "processingSettings",
                    TbSaveToCustomCassandraTableNode.class.getName(), "defaultTtl",
                    TbChangeOwnerNode.class.getName(), "createOwnerOnOriginatorLevel"
            )
    );

    public static final Map<EdgeVersion, Set<String>> EXCLUDED_NODES_BY_EDGE_VERSION = Map.of(
            EdgeVersion.V_4_1_0,
            Set.of(
                    TbGenerateReportV2Node.class.getName(),
                    TbAiNode.class.getName()
            ),
            EdgeVersion.V_4_0_0,
            Set.of(
                    TbGenerateReportV2Node.class.getName(),
                    TbAiNode.class.getName()
            ),
            EdgeVersion.V_3_9_0,
            Set.of(
                    TbCalculatedFieldsNode.class.getName()
            ),
            EdgeVersion.V_3_8_0,
            Set.of(
                    TbCalculatedFieldsNode.class.getName()
            ),
            EdgeVersion.V_3_7_0,
            Set.of(
                    TbCalculatedFieldsNode.class.getName(),
                    TbSendRestApiCallReplyNode.class.getName(),
                    TbAwsLambdaNode.class.getName()
            )
    );

    private static void resetVersion(HasVersion entity) {
        entity.setVersion(null);
    }

    public static AlarmUpdateMsg constructAlarmUpdatedMsg(UpdateMsgType msgType, Alarm alarm) {
        return AlarmUpdateMsg.newBuilder().setMsgType(msgType)
                .setEntity(JacksonUtil.toString(alarm))
                .setIdMSB(alarm.getId().getId().getMostSignificantBits())
                .setIdLSB(alarm.getId().getId().getLeastSignificantBits()).build();
    }

    public static AlarmCommentUpdateMsg constructAlarmCommentUpdatedMsg(UpdateMsgType msgType, AlarmComment alarmComment) {
        return AlarmCommentUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(alarmComment)).build();
    }

    public static AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset, EntityGroupId entityGroupId) {
        resetVersion(asset);
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(msgType).setEntity(JacksonUtil.toString(asset))
                .setIdMSB(asset.getUuidId().getMostSignificantBits())
                .setIdLSB(asset.getUuidId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static AssetUpdateMsg constructAssetDeleteMsg(AssetId assetId, EntityGroupId entityGroupId) {
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetId.getId().getMostSignificantBits())
                .setIdLSB(assetId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, AssetProfile assetProfile) {
        resetVersion(assetProfile);
        return AssetProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(assetProfile))
                .setIdMSB(assetProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(assetProfile.getId().getId().getLeastSignificantBits()).build();
    }

    public static AssetProfileUpdateMsg constructAssetProfileDeleteMsg(AssetProfileId assetProfileId) {
        return AssetProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetProfileId.getId().getMostSignificantBits())
                .setIdLSB(assetProfileId.getId().getLeastSignificantBits()).build();
    }

    public static CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, Customer customer, EntityGroupId entityGroupId) {
        resetVersion(customer);
        CustomerUpdateMsg.Builder builder = CustomerUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(customer))
                .setIdMSB(customer.getId().getId().getMostSignificantBits())
                .setIdLSB(customer.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static CustomerUpdateMsg constructCustomerDeleteMsg(CustomerId customerId) {
        return CustomerUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(customerId.getId().getMostSignificantBits())
                .setIdLSB(customerId.getId().getLeastSignificantBits()).build();
    }

    public static DashboardUpdateMsg constructDashboardUpdatedMsg(UpdateMsgType msgType, Dashboard dashboard, EntityGroupId entityGroupId) {
        resetVersion(dashboard);
        DashboardUpdateMsg.Builder builder = DashboardUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(dashboard))
                .setIdMSB(dashboard.getId().getId().getMostSignificantBits())
                .setIdLSB(dashboard.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static DashboardUpdateMsg constructDashboardDeleteMsg(DashboardId dashboardId, EntityGroupId entityGroupId) {
        DashboardUpdateMsg.Builder builder = DashboardUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(dashboardId.getId().getMostSignificantBits())
                .setIdLSB(dashboardId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device, EntityGroupId entityGroupId) {
        resetVersion(device);
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(device))
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId, EntityGroupId entityGroupId) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceId.getId().getMostSignificantBits())
                .setIdLSB(deviceId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        resetVersion(deviceCredentials);
        return DeviceCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(deviceCredentials)).build();
    }

    public static DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile, EdgeVersion edgeVersion) {
        resetVersion(deviceProfile);
        String entity = getEntityAndFixLwm2mBootstrapShortServerId(deviceProfile, edgeVersion);
        return DeviceProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(entity)
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits()).build();
    }

    public static String getEntityAndFixLwm2mBootstrapShortServerId(DeviceProfile deviceProfile, EdgeVersion edgeVersion) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (!(transportConfiguration instanceof Lwm2mDeviceProfileTransportConfiguration) || EdgeVersionComparator.INSTANCE.compare(edgeVersion, EdgeVersion.V_4_3_0) >= 0) {
            return JacksonUtil.toString(deviceProfile);
        }
        JsonNode jsonNode = JacksonUtil.valueToTree(deviceProfile);
        JsonNode profileDataNode = jsonNode.get("profileData");
        if (profileDataNode != null && profileDataNode.has("transportConfiguration")) {
            JsonNode transportConfigNode = profileDataNode.get("transportConfiguration");
            JsonNode bootstrapNode = transportConfigNode.get("bootstrap");
            if (bootstrapNode != null && bootstrapNode.isArray()) {
                for (JsonNode bootstrapServerNode : bootstrapNode) {
                    if (bootstrapServerNode.isObject()) {
                        ObjectNode serverObjectNode = (ObjectNode) bootstrapServerNode;
                        JsonNode isBootstrapNode = serverObjectNode.get("bootstrapServerIs");
                        boolean isBootstrapServer = isBootstrapNode != null && isBootstrapNode.asBoolean(false);
                        JsonNode shortServerIdNode = serverObjectNode.get("shortServerId");
                        if (isBootstrapServer && (shortServerIdNode == null || shortServerIdNode.isNull())) {
                            serverObjectNode.put("shortServerId", 0);
                        }
                    }
                }
            }
        }
        return JacksonUtil.toString(jsonNode);
    }

    public static DeviceProfileUpdateMsg constructDeviceProfileDeleteMsg(DeviceProfileId deviceProfileId) {
        return DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(deviceProfileId.getId().getMostSignificantBits())
                .setIdLSB(deviceProfileId.getId().getLeastSignificantBits()).build();
    }

    public static DeviceRpcCallMsg constructDeviceRpcCallMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = constructDeviceRpcMsg(deviceId, body);
        if (body.has("error") || body.has("response")) {
            RpcResponseMsg.Builder responseBuilder = RpcResponseMsg.newBuilder();
            if (body.has("error")) {
                responseBuilder.setError(body.get("error").asText());
            } else {
                responseBuilder.setResponse(body.get("response").asText());
            }
            builder.setResponseMsg(responseBuilder.build());
        } else {
            RpcRequestMsg.Builder requestBuilder = RpcRequestMsg.newBuilder();
            requestBuilder.setMethod(body.get("method").asText());
            requestBuilder.setParams(body.get("params").asText());
            builder.setRequestMsg(requestBuilder.build());
        }
        return builder.build();
    }

    private static DeviceRpcCallMsg.Builder constructDeviceRpcMsg(UUID deviceId, JsonNode body) {
        DeviceRpcCallMsg.Builder builder = DeviceRpcCallMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getLeastSignificantBits())
                .setRequestId(body.get("requestId").asInt());
        if (body.get("oneway") != null) {
            builder.setOneway(body.get("oneway").asBoolean());
        }
        if (body.get("requestUUID") != null) {
            UUID requestUUID = UUID.fromString(body.get("requestUUID").asText());
            builder.setRequestUuidMSB(requestUUID.getMostSignificantBits())
                    .setRequestUuidLSB(requestUUID.getLeastSignificantBits());
        }
        if (body.get("expirationTime") != null) {
            builder.setExpirationTime(body.get("expirationTime").asLong());
        }
        if (body.get("persisted") != null) {
            builder.setPersisted(body.get("persisted").asBoolean());
        }
        if (body.get("retries") != null) {
            builder.setRetries(body.get("retries").asInt());
        }
        if (body.get("additionalInfo") != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(body.get("additionalInfo")));
        }
        if (body.get("serviceId") != null) {
            builder.setServiceId(body.get("serviceId").asText());
        }
        if (body.get("sessionId") != null) {
            builder.setSessionId(body.get("sessionId").asText());
        }
        return builder;
    }

    public static EdgeConfiguration constructEdgeConfiguration(Edge edge, int licenseVersion) {
        EdgeConfiguration.Builder builder = EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setType(edge.getType())
                .setRoutingKey(edge.getRoutingKey())
                .setSecret(edge.getSecret())
                .setEdgeLicenseKey(edge.getEdgeLicenseKey() != null ? edge.getEdgeLicenseKey() : "")
                .setCloudEndpoint(edge.getCloudEndpoint() != null ? edge.getCloudEndpoint() : "")
                .setAdditionalInfo(JacksonUtil.toString(edge.getAdditionalInfo()))
                .setCloudType("PE")
                .setLicenseVersion(licenseVersion);
        if (edge.getCustomerId() != null) {
            builder.setCustomerIdMSB(edge.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(edge.getCustomerId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView, EntityGroupId entityGroupId) {
        resetVersion(entityView);
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(entityView))
                .setIdMSB(entityView.getId().getId().getMostSignificantBits())
                .setIdLSB(entityView.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static EntityViewUpdateMsg constructEntityViewDeleteMsg(EntityViewId entityViewId, EntityGroupId entityGroupId) {
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(entityViewId.getId().getMostSignificantBits())
                .setIdLSB(entityViewId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static NotificationRuleUpdateMsg constructNotificationRuleUpdateMsg(UpdateMsgType msgType, NotificationRule notificationRule) {
        return NotificationRuleUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationRule)).build();
    }

    public static NotificationRuleUpdateMsg constructNotificationRuleDeleteMsg(NotificationRuleId notificationRuleId) {
        return NotificationRuleUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationRuleId.getId().getMostSignificantBits())
                .setIdLSB(notificationRuleId.getId().getLeastSignificantBits()).build();
    }

    public static NotificationTargetUpdateMsg constructNotificationTargetUpdateMsg(UpdateMsgType msgType, NotificationTarget notificationTarget) {
        return NotificationTargetUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationTarget)).build();
    }

    public static NotificationTargetUpdateMsg constructNotificationTargetDeleteMsg(NotificationTargetId notificationTargetId) {
        return NotificationTargetUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationTargetId.getId().getMostSignificantBits())
                .setIdLSB(notificationTargetId.getId().getLeastSignificantBits()).build();
    }

    public static NotificationTemplateUpdateMsg constructNotificationTemplateUpdateMsg(UpdateMsgType msgType, NotificationTemplate notificationTemplate) {
        return NotificationTemplateUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(notificationTemplate)).build();
    }

    public static NotificationTemplateUpdateMsg constructNotificationTemplateDeleteMsg(NotificationTemplateId notificationTemplateId) {
        return NotificationTemplateUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(notificationTemplateId.getId().getMostSignificantBits())
                .setIdLSB(notificationTemplateId.getId().getLeastSignificantBits()).build();
    }

    public static OAuth2ClientUpdateMsg constructOAuth2ClientUpdateMsg(UpdateMsgType msgType, OAuth2Client oAuth2Client) {
        return OAuth2ClientUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(oAuth2Client))
                .setIdMSB(oAuth2Client.getId().getId().getMostSignificantBits())
                .setIdLSB(oAuth2Client.getId().getId().getLeastSignificantBits()).build();
    }

    public static OAuth2ClientUpdateMsg constructOAuth2ClientDeleteMsg(OAuth2ClientId oAuth2ClientId) {
        return OAuth2ClientUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(oAuth2ClientId.getId().getMostSignificantBits())
                .setIdLSB(oAuth2ClientId.getId().getLeastSignificantBits()).build();
    }

    public static OAuth2DomainUpdateMsg constructOAuth2DomainUpdateMsg(UpdateMsgType msgType, DomainInfo domainInfo) {
        return OAuth2DomainUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(domainInfo))
                .setIdMSB(domainInfo.getId().getId().getMostSignificantBits())
                .setIdLSB(domainInfo.getId().getId().getLeastSignificantBits()).build();
    }

    public static OAuth2DomainUpdateMsg constructOAuth2DomainDeleteMsg(DomainId domainId) {
        return OAuth2DomainUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(domainId.getId().getMostSignificantBits())
                .setIdLSB(domainId.getId().getLeastSignificantBits())
                .build();
    }

    public static OtaPackageUpdateMsg constructOtaPackageUpdatedMsg(UpdateMsgType msgType, OtaPackage otaPackage) {
        return OtaPackageUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(otaPackage))
                .setIdMSB(otaPackage.getId().getId().getMostSignificantBits())
                .setIdLSB(otaPackage.getId().getId().getLeastSignificantBits()).build();
    }

    public static OtaPackageUpdateMsg constructOtaPackageDeleteMsg(OtaPackageId otaPackageId) {
        return OtaPackageUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(otaPackageId.getId().getMostSignificantBits())
                .setIdLSB(otaPackageId.getId().getLeastSignificantBits()).build();
    }

    public static QueueUpdateMsg constructQueueUpdatedMsg(UpdateMsgType msgType, Queue queue) {
        return QueueUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(queue))
                .setIdMSB(queue.getId().getId().getMostSignificantBits())
                .setIdLSB(queue.getId().getId().getLeastSignificantBits()).build();
    }

    public static QueueUpdateMsg constructQueueDeleteMsg(QueueId queueId) {
        return QueueUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(queueId.getId().getMostSignificantBits())
                .setIdLSB(queueId.getId().getLeastSignificantBits()).build();
    }

    public static RelationUpdateMsg constructRelationUpdatedMsg(UpdateMsgType msgType, EntityRelation entityRelation) {
        resetVersion(entityRelation);
        return RelationUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(entityRelation)).build();
    }

    public static ResourceUpdateMsg constructResourceUpdatedMsg(UpdateMsgType msgType, TbResource tbResource) {
        return ResourceUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tbResource))
                .setIdMSB(tbResource.getId().getId().getMostSignificantBits())
                .setIdLSB(tbResource.getId().getId().getLeastSignificantBits()).build();
    }

    public static ResourceUpdateMsg constructResourceDeleteMsg(TbResourceId tbResourceId) {
        return ResourceUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(tbResourceId.getId().getMostSignificantBits())
                .setIdLSB(tbResourceId.getId().getLeastSignificantBits()).build();
    }

    public static RuleChainUpdateMsg constructRuleChainUpdatedMsg(UpdateMsgType msgType, RuleChain ruleChain, boolean isRoot) {
        resetVersion(ruleChain);
        boolean isTemplateRoot = ruleChain.isRoot();
        ruleChain.setRoot(isRoot);
        RuleChainUpdateMsg result = RuleChainUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(ruleChain))
                .setIdMSB(ruleChain.getId().getId().getMostSignificantBits())
                .setIdLSB(ruleChain.getId().getId().getLeastSignificantBits()).build();
        ruleChain.setRoot(isTemplateRoot);
        return result;
    }

    public static RuleChainUpdateMsg constructRuleChainDeleteMsg(RuleChainId ruleChainId) {
        return RuleChainUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setIdLSB(ruleChainId.getId().getLeastSignificantBits()).build();
    }

    public static RuleChainMetadataUpdateMsg constructRuleChainMetadataUpdatedMsg(UpdateMsgType msgType, RuleChainMetaData ruleChainMetaData, EdgeVersion edgeVersion) {
        resetVersion(ruleChainMetaData);
        String metaData = sanitizeMetadataForLegacyEdgeVersion(ruleChainMetaData, edgeVersion);

        return RuleChainMetadataUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setEntity(metaData)
                .build();
    }

    private static String sanitizeMetadataForLegacyEdgeVersion(RuleChainMetaData ruleChainMetaData, EdgeVersion edgeVersion) {
        JsonNode jsonNode = JacksonUtil.valueToTree(ruleChainMetaData);
        JsonNode nodes = jsonNode.get("nodes");

        updateNodeConfigurationsForLegacyEdge(nodes, edgeVersion);
        removeExcludedNodesForLegacyEdge(nodes, edgeVersion);

        return JacksonUtil.toString(jsonNode);
    }

    private static void updateNodeConfigurationsForLegacyEdge(JsonNode nodes, EdgeVersion edgeVersion) {
        nodes.forEach(node -> {
            if (node.isObject() && node.has("configuration")) {
                String nodeType = node.get("type").asText();
                Map<String, String> ignoredParams = IGNORED_PARAMS_BY_EDGE_VERSION.get(edgeVersion);

                if (ignoredParams != null && ignoredParams.containsKey(nodeType)) {
                    ((ObjectNode) node.get("configuration")).remove(ignoredParams.get(nodeType));
                }
            }
        });
    }

    private static void removeExcludedNodesForLegacyEdge(JsonNode nodes, EdgeVersion edgeVersion) {
        Iterator<JsonNode> iterator = nodes.iterator();

        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String type = node.get("type").asText();
            Set<String> missNodes = EXCLUDED_NODES_BY_EDGE_VERSION.get(edgeVersion);

            if (missNodes != null && missNodes.contains(type)) {
                iterator.remove();
            }
        }
    }

    public static EntityDataProto constructEntityDataMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        long ts = extractTs(entityData.getAsJsonObject());
        switch (actionType) {
            case TIMESERIES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    builder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data.getAsJsonObject("data"), ts));
                } catch (Exception e) {
                    log.trace("[{}][{}] Can't convert to telemetry proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg attributesUpdatedMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    if (data.has("isPostAttributes") && data.getAsJsonPrimitive("isPostAttributes").getAsBoolean()) {
                        builder.setPostAttributesMsg(attributesUpdatedMsg);
                    } else {
                        builder.setAttributesUpdatedMsg(attributesUpdatedMsg);
                    }
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                    builder.setAttributeTs(ts);
                } catch (Exception e) {
                    log.trace("[{}][{}] Can't convert to AttributesUpdatedMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case POST_ATTRIBUTES:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    builder.setPostAttributesMsg(postAttributesMsg);
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                    builder.setAttributeTs(ts);
                } catch (Exception e) {
                    log.trace("[{}][{}] Can't convert to PostAttributesMsg, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_DELETED:
                try {
                    AttributeDeleteMsg.Builder attributeDeleteMsg = AttributeDeleteMsg.newBuilder();
                    attributeDeleteMsg.setScope(entityData.getAsJsonObject().getAsJsonPrimitive("scope").getAsString());
                    JsonArray jsonArray = entityData.getAsJsonObject().getAsJsonArray("keys");
                    List<String> keys = new Gson().fromJson(jsonArray.toString(), new TypeToken<>() {}.getType());
                    attributeDeleteMsg.addAllAttributeNames(keys);
                    attributeDeleteMsg.build();
                    builder.setAttributeDeleteMsg(attributeDeleteMsg);
                } catch (Exception e) {
                    log.trace("[{}][{}] Can't convert to AttributeDeleteMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
        }
        return builder.build();
    }

    private static long extractTs(JsonObject data) {
        if (data.has("ts") && data.get("ts").isJsonPrimitive()) {
            return data.getAsJsonPrimitive("ts").getAsLong();
        }
        return System.currentTimeMillis();
    }

    private static String getScopeOfDefault(JsonObject data) {
        JsonPrimitive scope = data.getAsJsonPrimitive("scope");
        String result = DataConstants.SERVER_SCOPE;
        if (scope != null && StringUtils.isNotBlank(scope.getAsString())) {
            result = scope.getAsString();
        }
        return result;
    }

    public static TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant) {
        resetVersion(tenant);
        return TenantUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenant)).build();
    }

    public static TenantProfileUpdateMsg constructTenantProfileUpdateMsg(UpdateMsgType msgType, TenantProfile tenantProfile) {
        return TenantProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenantProfile)).build();
    }

    public static UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user, EntityGroupId entityGroupId) {
        resetVersion(user);
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(user))
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static UserUpdateMsg constructUserDeleteMsg(UserId userId, EntityGroupId entityGroupId) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(userId.getId().getMostSignificantBits())
                .setIdLSB(userId.getId().getLeastSignificantBits());
        if (entityGroupId != null) {
            builder.setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                    .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits());
        }
        return builder.build();
    }

    public static UserCredentialsUpdateMsg constructUserCredentialsUpdatedMsg(UserCredentials userCredentials) {
        return UserCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(userCredentials)).build();
    }

    public static WidgetsBundleUpdateMsg constructWidgetsBundleUpdateMsg(UpdateMsgType msgType, WidgetsBundle widgetsBundle, List<String> widgets) {
        resetVersion(widgetsBundle);
        return WidgetsBundleUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(widgetsBundle))
                .setWidgets(JacksonUtil.toString(widgets))
                .setIdMSB(widgetsBundle.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetsBundle.getId().getId().getLeastSignificantBits()).build();
    }

    public static WidgetsBundleUpdateMsg constructWidgetsBundleDeleteMsg(WidgetsBundleId widgetsBundleId) {
        return WidgetsBundleUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetsBundleId.getId().getMostSignificantBits())
                .setIdLSB(widgetsBundleId.getId().getLeastSignificantBits())
                .build();
    }

    public static WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, WidgetTypeDetails widgetTypeDetails) {
        resetVersion(widgetTypeDetails);
        return WidgetTypeUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(widgetTypeDetails))
                .setIdMSB(widgetTypeDetails.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetTypeDetails.getId().getId().getLeastSignificantBits()).build();
    }

    public static WidgetTypeUpdateMsg constructWidgetTypeDeleteMsg(WidgetTypeId widgetTypeId) {
        return WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(widgetTypeId.getId().getMostSignificantBits())
                .setIdLSB(widgetTypeId.getId().getLeastSignificantBits())
                .build();
    }

    public static CalculatedFieldUpdateMsg constructCalculatedFieldUpdatedMsg(UpdateMsgType msgType, CalculatedField calculatedField) {
        resetVersion(calculatedField);
        return CalculatedFieldUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(calculatedField))
                .setIdMSB(calculatedField.getId().getId().getMostSignificantBits())
                .setIdLSB(calculatedField.getId().getId().getLeastSignificantBits()).build();
    }

    public static CalculatedFieldUpdateMsg constructCalculatedFieldDeleteMsg(CalculatedFieldId calculatedFieldId) {
        return CalculatedFieldUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(calculatedFieldId.getId().getMostSignificantBits())
                .setIdLSB(calculatedFieldId.getId().getLeastSignificantBits()).build();
    }

    public static AiModelUpdateMsg constructAiModelUpdatedMsg(UpdateMsgType msgType, AiModel aiModel) {
        resetVersion(aiModel);
        return AiModelUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(aiModel))
                .setIdMSB(aiModel.getId().getId().getMostSignificantBits())
                .setIdLSB(aiModel.getId().getId().getLeastSignificantBits()).build();
    }

    public static AiModelUpdateMsg constructAiModelDeleteMsg(AiModelId aiModelId) {
        return AiModelUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(aiModelId.getId().getMostSignificantBits())
                .setIdLSB(aiModelId.getId().getLeastSignificantBits()).build();
    }

    public static EncryptionKeyUpdateMsg constructEncryptionKeyUpdatedMsg(UpdateMsgType msgType, EncryptionKey encryptionKey) {
        return EncryptionKeyUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(encryptionKey))
                .setIdMSB(encryptionKey.getId().getId().getMostSignificantBits())
                .setIdLSB(encryptionKey.getId().getId().getLeastSignificantBits()).build();
    }

    public static SecretUpdateMsg constructSecretUpdatedMsg(UpdateMsgType msgType, Secret secret) {
        return SecretUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(secret))
                .setIdMSB(secret.getId().getId().getMostSignificantBits())
                .setIdLSB(secret.getId().getId().getLeastSignificantBits()).build();
    }

    public static SecretUpdateMsg constructSecretDeleteMsg(SecretId secretId) {
        return SecretUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(secretId.getId().getMostSignificantBits())
                .setIdLSB(secretId.getId().getLeastSignificantBits()).build();
    }

    public static ApiKeyUpdateMsg constructApiKeyUpdatedMsg(UpdateMsgType msgType, ApiKey apiKey) {
        return ApiKeyUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(apiKey))
                .setIdMSB(apiKey.getId().getId().getMostSignificantBits())
                .setIdLSB(apiKey.getId().getId().getLeastSignificantBits()).build();
    }

    public static ApiKeyUpdateMsg constructApiKeyDeleteMsg(ApiKeyId apiKeyId) {
        return ApiKeyUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(apiKeyId.getId().getMostSignificantBits())
                .setIdLSB(apiKeyId.getId().getLeastSignificantBits()).build();
    }

    // PE constructors:

    public static ConverterUpdateMsg constructConverterUpdateMsg(UpdateMsgType msgType, Converter converter) {
        resetVersion(converter);
        return ConverterUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(converter))
                .setIdMSB(converter.getUuidId().getMostSignificantBits())
                .setIdLSB(converter.getUuidId().getLeastSignificantBits()).build();
    }

    public static CustomMenuProto constructCustomMenuMsg(UpdateMsgType msgType, CustomMenu customMenu, List<EntityId> entityIds) {
        CustomMenuProto.Builder builder = CustomMenuProto.newBuilder();
        if (CollectionUtils.isNotEmpty(entityIds)) {
            builder.setAssigneeList(JacksonUtil.toString(entityIds));
        }
        return builder.setMsgType(msgType).setEntity(JacksonUtil.toString(customMenu)).build();
    }

    public static CustomTranslationUpdateMsg constructCustomTranslationMsg(UpdateMsgType msgType, CustomTranslation customTranslation) {
        return CustomTranslationUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(customTranslation)).build();
    }

    public static DeviceGroupOtaPackageUpdateMsg constructDeviceGroupOtaUpdateMsg(UpdateMsgType msgType, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        return DeviceGroupOtaPackageUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(deviceGroupOtaPackage)).build();
    }

    public static EntityGroupUpdateMsg constructEntityGroupUpdatedMsg(UpdateMsgType msgType, EntityGroup entityGroup) {
        resetVersion(entityGroup);
        return EntityGroupUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(entityGroup)).setName(entityGroup.getName())
                .setIdMSB(entityGroup.getId().getId().getMostSignificantBits())
                .setIdLSB(entityGroup.getId().getId().getLeastSignificantBits()).build();
    }

    public static EntityGroupUpdateMsg constructEntityGroupDeleteMsg(EntityGroupId entityGroupId) {
        return EntityGroupUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(entityGroupId.getId().getMostSignificantBits())
                .setIdLSB(entityGroupId.getId().getLeastSignificantBits()).build();
    }

    public static GroupPermissionProto constructGroupPermissionProto(UpdateMsgType msgType, GroupPermission groupPermission) {
        return GroupPermissionProto.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(groupPermission))
                .setIdMSB(groupPermission.getId().getId().getMostSignificantBits())
                .setIdLSB(groupPermission.getId().getId().getLeastSignificantBits()).build();
    }

    public static GroupPermissionProto constructGroupPermissionDeleteMsg(GroupPermissionId groupPermissionId) {
        return GroupPermissionProto.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(groupPermissionId.getId().getMostSignificantBits())
                .setIdLSB(groupPermissionId.getId().getLeastSignificantBits()).build();
    }

    public static IntegrationUpdateMsg constructIntegrationUpdateMsg(UpdateMsgType msgType, Integration integration, JsonNode configuration) {
        resetVersion(integration);
        // Work on a defensive copy: the caller may pass a cached Integration instance that is shared
        // across concurrent edge sessions. Mutating configuration / version on it would race with
        // other threads serializing the same entity for their own edges.
        Integration copy = new Integration(integration);
        copy.setConfiguration(configuration);
        return IntegrationUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(copy))
                .setIdMSB(copy.getId().getId().getMostSignificantBits())
                .setIdLSB(copy.getId().getId().getLeastSignificantBits()).build();
    }

    public static IntegrationUpdateMsg constructIntegrationDeleteMsg(IntegrationId integrationId) {
        return IntegrationUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(integrationId.getId().getMostSignificantBits())
                .setIdLSB(integrationId.getId().getLeastSignificantBits()).build();
    }

    public static RoleProto constructRoleProto(UpdateMsgType msgType, Role role) {
        resetVersion(role);
        return RoleProto.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(role))
                .setIdMSB(role.getId().getId().getMostSignificantBits())
                .setIdLSB(role.getId().getId().getLeastSignificantBits()).build();
    }

    public static RoleProto constructRoleDeleteMsg(RoleId roleId) {
        return RoleProto.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(roleId.getId().getMostSignificantBits())
                .setIdLSB(roleId.getId().getLeastSignificantBits()).build();
    }

    public static SchedulerEventUpdateMsg constructSchedulerEventUpdatedMsg(UpdateMsgType msgType, SchedulerEvent schedulerEvent) {
        resetVersion(schedulerEvent);
        return SchedulerEventUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(schedulerEvent))
                .setIdMSB(schedulerEvent.getId().getId().getMostSignificantBits())
                .setIdLSB(schedulerEvent.getId().getId().getLeastSignificantBits()).build();
    }

    public static SchedulerEventUpdateMsg constructSchedulerEventDeleteMsg(SchedulerEventId schedulerEventId) {
        return SchedulerEventUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(schedulerEventId.getId().getMostSignificantBits())
                .setIdLSB(schedulerEventId.getId().getLeastSignificantBits()).build();
    }

    public static WhiteLabelingProto constructWhiteLabeling(UpdateMsgType msgType, WhiteLabeling whiteLabeling) {
        return WhiteLabelingProto.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(whiteLabeling)).build();
    }

    public static ReportTemplateUpdateMsg constructReportTemplateUpdatedMsg(UpdateMsgType msgType, ReportTemplate reportTemplate) {
        resetVersion(reportTemplate);
        return ReportTemplateUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(reportTemplate))
                .setIdMSB(reportTemplate.getId().getId().getMostSignificantBits())
                .setIdLSB(reportTemplate.getId().getId().getLeastSignificantBits()).build();
    }

    public static ReportTemplateUpdateMsg constructReportTemplateDeleteMsg(ReportTemplateId reportTemplateId) {
        return ReportTemplateUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(reportTemplateId.getId().getMostSignificantBits())
                .setIdLSB(reportTemplateId.getId().getLeastSignificantBits()).build();
    }

    public static List<EdgeEvent> mergeAndFilterDownlinkDuplicates(List<EdgeEvent> edgeEvents) {
        try {
            edgeEvents = removeDownlinkDuplicates(edgeEvents);

            List<AttrUpdateMsg> attrUpdateMsgs = new ArrayList<>();
            for (EdgeEvent edgeEvent : edgeEvents) {
                if (EdgeEventActionType.ATTRIBUTES_UPDATED.equals(edgeEvent.getAction())) {
                    attrUpdateMsgs.add(new AttrUpdateMsg(edgeEvent.getEntityId(), edgeEvent.getBody()));
                }
            }
            Map<UUID, Map<String, Long>> latestTsByEntityAndKey = computeLatestTsByEntityAndKey(attrUpdateMsgs);

            List<EdgeEvent> result = new ArrayList<>();
            for (EdgeEvent edgeEvent : edgeEvents) {
                if (!EdgeEventActionType.ATTRIBUTES_UPDATED.equals(edgeEvent.getAction())) {
                    result.add(edgeEvent);
                    continue;
                }

                Map<String, Long> latestByKey = latestTsByEntityAndKey.get(edgeEvent.getEntityId());
                JsonNode filteredBody = filterAttributesBody(edgeEvent.getBody(), latestByKey);
                if (filteredBody == null) {
                    continue;
                }

                result.add(createFilteredEdgeEvent(edgeEvent, filteredBody));
            }

            result.sort(Comparator.comparingLong(EdgeEvent::getSeqId));
            return result;
        } catch (Exception e) {
            log.info("Can't merge downlink duplicates. Sending downlinks without merge. Original edgeEvents [{}]", edgeEvents, e);
            return edgeEvents;
        }
    }

    private static AttrsTs extractAttributes(JsonNode body) {
        if (body == null) {
            return new AttrsTs(0L, List.of());
        }
        String bodyStr = JacksonUtil.toString(body);
        var jsonObject = JsonParser.parseString(bodyStr).getAsJsonObject();
        if (!jsonObject.has("ts")) {
            return new AttrsTs(0L, List.of());
        }
        long ts = jsonObject.get("ts").getAsLong();
        var kv = jsonObject.getAsJsonObject("kv");
        List<AttributeKvEntry> attrs = JsonConverter.convertToAttributes(
                JsonUtils.getJsonObject(
                        JsonConverter.convertToAttributesProto(kv).getKvList()
                ), ts);
        return new AttrsTs(ts, attrs);
    }

    private static JsonNode filterAttributesBody(JsonNode body, Map<String, Long> latestByKey) {
        if (body == null) {
            return null;
        }
        String bodyStr = JacksonUtil.toString(body);
        JsonObject jsonObject = JsonParser.parseString(bodyStr).getAsJsonObject();
        if (jsonObject.has("ts") && latestByKey != null && !latestByKey.isEmpty()) {
            long ts = jsonObject.get("ts").getAsLong();
            JsonObject kv = jsonObject.getAsJsonObject("kv");
            for (Iterator<Map.Entry<String, JsonElement>> it = kv.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, JsonElement> e = it.next();
                Long latestTs = latestByKey.get(e.getKey());
                if (latestTs == null || !latestTs.equals(ts)) {
                    it.remove();
                }
            }
            if (kv.isEmpty()) {
                return null;
            }
        }
        return JacksonUtil.toJsonNode(jsonObject.toString());
    }

    private static Map<UUID, Map<String, Long>> computeLatestTsByEntityAndKey(List<AttrUpdateMsg> attrUpdateMsgs) {
        Map<UUID, Map<String, Long>> latestTsByEntityAndKey = new HashMap<>();
        for (AttrUpdateMsg attrUpdateMsg : attrUpdateMsgs) {
            UUID entityId = attrUpdateMsg.entityId();
            AttrsTs attrsTs = extractAttributes(attrUpdateMsg.body());
            Map<String, Long> map = latestTsByEntityAndKey.computeIfAbsent(entityId, id -> new HashMap<>());
            long ts = attrsTs.ts();
            for (AttributeKvEntry attr : attrsTs.attrs()) {
                map.merge(attr.getKey(), ts, Math::max);
            }
        }
        return latestTsByEntityAndKey;
    }

    private static EdgeEvent createFilteredEdgeEvent(EdgeEvent edgeEvent, JsonNode filteredBody) {
        EdgeEvent filtered = new EdgeEvent();
        filtered.setSeqId(edgeEvent.getSeqId());
        filtered.setTenantId(edgeEvent.getTenantId());
        filtered.setEdgeId(edgeEvent.getEdgeId());
        filtered.setAction(edgeEvent.getAction());
        filtered.setEntityId(edgeEvent.getEntityId());
        filtered.setUid(edgeEvent.getUid());
        filtered.setType(edgeEvent.getType());
        filtered.setEntityGroupId(edgeEvent.getEntityGroupId());
        filtered.setBody(filteredBody);
        return filtered;
    }

    private static List<EdgeEvent> removeDownlinkDuplicates(List<EdgeEvent> edgeEvents) {
        Set<EventKey> seen = new HashSet<>();
        return edgeEvents.stream()
                .filter(e -> seen.add(new EventKey(
                        e.getTenantId(),
                        e.getAction(),
                        e.getEntityId(),
                        e.getType().name(),
                        (e.getBody() != null ? e.getBody().toString() : "null"),
                        e.getEntityGroupId())))
                .collect(Collectors.toList());
    }

    private record EventKey(TenantId tenantId,
                            EdgeEventActionType action,
                            UUID entityId,
                            String type,
                            String body,
                            UUID entityGroupId) {
    }

    private record AttrsTs(long ts, List<AttributeKvEntry> attrs) {
    }

    private record AttrUpdateMsg(UUID entityId, JsonNode body) {
    }

}
