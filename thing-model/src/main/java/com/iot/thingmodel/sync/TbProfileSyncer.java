package com.iot.thingmodel.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.tb.TbExceptionTranslator;
import com.iot.thingmodel.tb.TbSyncException;
import com.iot.thingmodel.tb.ThingsboardClientProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetProfileInfo;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.NameConflictPolicy;
import org.thingsboard.client.model.PageDataDeviceProfile;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.UniquifyStrategy;

@Component
public class TbProfileSyncer {

    private final ThingsboardClientProvider clientProvider;
    private final ProfileGenerator profileGenerator;
    private final TbRuleChainProvisioner ruleChainProvisioner;
    private final ObjectMapper objectMapper;

    public TbProfileSyncer(
            ThingsboardClientProvider clientProvider,
            ProfileGenerator profileGenerator,
            TbRuleChainProvisioner ruleChainProvisioner,
            ObjectMapper objectMapper) {
        this.clientProvider = clientProvider;
        this.profileGenerator = profileGenerator;
        this.ruleChainProvisioner = ruleChainProvisioner;
        this.objectMapper = objectMapper;
    }

    public SyncResult sync(TbSyncRequest request) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return doSync(request);
            } catch (org.thingsboard.client.ApiException ex) {
                if (attempt == 0 && ex.getCode() == 401) {
                    clientProvider.evict(request.tenantId());
                    continue;
                }
                throw TbExceptionTranslator.translate("同步模板", ex);
            }
        }
        throw new TbSyncException("ThingsBoard 同步模板失败: 认证重试后仍然失败", 401, null);
    }

    private SyncResult doSync(TbSyncRequest request) throws org.thingsboard.client.ApiException {
        ThingsboardClient client = clientProvider.get(request.tenantId());
        UUID ruleChainId = ruleChainProvisioner.ensure(request);
        String profileName = profileGenerator.profileName(request.name(), request.model());
        DeviceProfile existingProfile = findProfile(client, profileName);

        DeviceProfile profile = profileGenerator.generate(
                request.name(),
                request.model(),
                request.productId(),
                request.categoryId(),
                request.tslVersion(),
                request.definition().properties().size(),
                request.definition().events().size(),
                request.definition().actions().size());

        if (existingProfile != null) {
            profile.setId(existingProfile.getId());
            preserveProfileFields(existingProfile, profile);
        }
        if (ruleChainId != null) {
            profile.setDefaultRuleChainId(new RuleChainId().id(ruleChainId));
        }
        DeviceProfile savedProfile = client.saveDeviceProfile(profile);

        String assetName = "产品 " + request.productId();
        Asset existingAsset = findAsset(client, assetName);
        AssetProfileInfo defaultAssetProfile = client.getDefaultAssetProfileInfo();
        if (defaultAssetProfile == null || defaultAssetProfile.getId() == null) {
            throw new TbSyncException("ThingsBoard 未配置默认资产模板", 500, null);
        }

        Asset asset = existingAsset != null ? existingAsset : new Asset();
        asset.setName(assetName);
        asset.setType("product");
        asset.setLabel(request.name());
        asset.setAssetProfileId(defaultAssetProfile.getId());
        if (existingAsset != null) {
            asset.setId(existingAsset.getId());
        }
        Asset savedAsset = client.saveAsset(org.thingsboard.client.api.ThingsboardApi.SaveAssetArgs.builder()
                .asset(asset)
                .nameConflictPolicy(NameConflictPolicy.FAIL)
                .uniquifySeparator("_")
                .uniquifyStrategy(UniquifyStrategy.RANDOM)
                .build());

        UUID profileId = savedProfile.getId() == null ? null : savedProfile.getId().getId();
        UUID assetId = savedAsset.getId() == null ? null : savedAsset.getId().getId();
        if (profileId == null || assetId == null) {
            throw new TbSyncException("ThingsBoard 未返回模板或资产 ID", 500, null);
        }

        writeAttributes(client, request, assetId);
        return new SyncResult(profileId, assetId, ruleChainId, request.compact().bytes());
    }

    private DeviceProfile findProfile(ThingsboardClient client, String profileName)
            throws org.thingsboard.client.ApiException {
        for (int page = 0; page < 10; page++) {
            PageDataDeviceProfile result = client.getDeviceProfiles(100, page, profileName, "name", "ASC");
            if (result == null || result.getData() == null) {
                return null;
            }
            for (DeviceProfile profile : result.getData()) {
                if (profileName.equals(profile.getName())) {
                    return profile;
                }
            }
            if (!Boolean.TRUE.equals(result.getHasNext())) {
                return null;
            }
        }
        return null;
    }

    private Asset findAsset(ThingsboardClient client, String assetName) throws org.thingsboard.client.ApiException {
        try {
            return client.getTenantAssetByName(assetName);
        } catch (org.thingsboard.client.ApiException ex) {
            if (ex.getCode() == 404) {
                return null;
            }
            throw ex;
        }
    }

    private void writeAttributes(ThingsboardClient client, TbSyncRequest request, UUID assetId)
            throws org.thingsboard.client.ApiException {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("product_id", request.productId());
        if (request.categoryId() != null) {
            attributes.put("category_id", request.categoryId());
        }
        attributes.put("tsl_version", request.tslVersion());
        attributes.put("property_count", request.definition().properties().size());
        attributes.put("event_count", request.definition().events().size());
        attributes.put("action_count", request.definition().actions().size());
        attributes.put("tsl_json", request.rawTsl());
        attributes.put("tsl_schema", request.compact().json());
        attributes.put("strict_unknown", request.compact().node().path("strictUnknown").asBoolean(false));
        try {
            String body = objectMapper.writeValueAsString(attributes);
            client.saveEntityAttributesV2("ASSET", assetId.toString(), "SERVER_SCOPE", body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new TbSyncException("产品资产属性序列化失败", 500, null);
        }
    }

    private void preserveProfileFields(DeviceProfile existing, DeviceProfile target) {
        target.setDefaultRuleChainId(existing.getDefaultRuleChainId());
        target.setDefaultEdgeRuleChainId(existing.getDefaultEdgeRuleChainId());
        target.setDefaultDashboardId(existing.getDefaultDashboardId());
        target.setDefaultQueueName(existing.getDefaultQueueName());
        target.setImage(existing.getImage());
        target.setProvisionDeviceKey(existing.getProvisionDeviceKey());
        target.setFirmwareId(existing.getFirmwareId());
        target.setSoftwareId(existing.getSoftwareId());
        target.setDefault(existing.getDefault());
        target.setVersion(existing.getVersion());
    }
}
