package com.iot.thingmodel.support;

import com.iot.thingmodel.common.Constants;
import java.util.List;
import java.util.UUID;
import org.thingsboard.client.ThingsboardClient;
import org.thingsboard.client.api.ThingsboardApi;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetId;
import org.thingsboard.client.model.AssetProfileId;
import org.thingsboard.client.model.AssetProfileInfo;
import org.thingsboard.client.model.DeviceProfile;
import org.thingsboard.client.model.DeviceProfileId;
import org.thingsboard.client.model.PageDataDeviceProfile;
import org.thingsboard.client.model.PageDataRuleChain;
import org.thingsboard.client.model.RuleChain;
import org.thingsboard.client.model.RuleChainId;
import org.thingsboard.client.model.RuleChainType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public final class TbClientStubs {

    private TbClientStubs() {
    }

    public static PageDataRuleChain ruleChainPage(List<RuleChain> chains, boolean hasNext) {
        PageDataRuleChain page = new PageDataRuleChain(null, null, hasNext);
        page.setData(chains);
        return page;
    }

    public static PageDataRuleChain emptyRuleChainPage() {
        return ruleChainPage(List.of(), false);
    }

    public static PageDataDeviceProfile emptyDeviceProfilePage() {
        return deviceProfilePage(List.of());
    }

    public static PageDataDeviceProfile deviceProfilePage(List<DeviceProfile> profiles) {
        PageDataDeviceProfile page = new PageDataDeviceProfile();
        page.setData(profiles);
        return page;
    }

    public static RuleChain ruleChain(UUID id, String name, boolean root) {
        RuleChain chain = new RuleChain();
        chain.setId(new RuleChainId().id(id));
        chain.setName(name);
        chain.setType(RuleChainType.CORE);
        chain.setRoot(root);
        return chain;
    }

    public static DeviceProfile deviceProfile(UUID id, String name, UUID ruleChainId) {
        DeviceProfile profile = new DeviceProfile();
        profile.setId(new DeviceProfileId().id(id));
        profile.setName(name);
        if (ruleChainId != null) {
            profile.setDefaultRuleChainId(new RuleChainId().id(ruleChainId));
        }
        return profile;
    }

    public static void stubNoRuleChains(ThingsboardClient client) {
        when(client.getRuleChains(
                anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(emptyRuleChainPage());
    }

    public static void stubRuleChainsByName(
            ThingsboardClient client, String name, PageDataRuleChain page) {
        when(client.getRuleChains(
                Constants.ThingsBoard.PAGE_SIZE,
                Constants.ThingsBoard.FIRST_PAGE,
                Constants.ThingsBoard.RULE_CHAIN_TYPE_CORE,
                name,
                Constants.ThingsBoard.SORT_PROPERTY_NAME,
                Constants.ThingsBoard.SORT_ORDER_ASC))
                .thenReturn(page);
    }

    public static void stubRuleChainCreated(ThingsboardClient client, UUID chainId) {
        when(client.saveRuleChain(any(RuleChain.class))).thenAnswer(invocation -> {
            RuleChain chain = invocation.getArgument(0);
            if (chain.getId() == null) {
                chain.setId(new RuleChainId().id(chainId));
            }
            return chain;
        });
    }

    public static void stubRuleChainSaved(ThingsboardClient client) {
        when(client.saveRuleChain(any(RuleChain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    public static void stubProfileLookup(ThingsboardClient client, PageDataDeviceProfile page) {
        when(client.getDeviceProfiles(
                anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(page);
    }

    public static void stubProfileSaved(ThingsboardClient client, UUID profileId) {
        when(client.saveDeviceProfile(any(DeviceProfile.class))).thenAnswer(invocation -> {
            DeviceProfile profile = invocation.getArgument(0);
            if (profile.getId() == null) {
                profile.setId(new DeviceProfileId().id(profileId));
            }
            return profile;
        });
    }

    public static void stubNoAsset(ThingsboardClient client) throws Exception {
        when(client.getTenantAssetByName(anyString()))
                .thenThrow(new org.thingsboard.client.ApiException(404, "not found"));
    }

    public static void stubExistingAsset(ThingsboardClient client, UUID assetId) throws Exception {
        Asset asset = new Asset();
        asset.setId(new AssetId().id(assetId));
        when(client.getTenantAssetByName(anyString())).thenReturn(asset);
    }

    public static void stubDefaultAssetProfile(ThingsboardClient client, UUID assetProfileId) {
        AssetProfileInfo info = new AssetProfileInfo();
        info.setId(new AssetProfileId().id(assetProfileId));
        when(client.getDefaultAssetProfileInfo()).thenReturn(info);
    }

    public static void stubAssetSaved(ThingsboardClient client, UUID assetId) throws Exception {
        Asset asset = new Asset();
        asset.setId(new AssetId().id(assetId));
        when(client.saveAsset(any(ThingsboardApi.SaveAssetArgs.class))).thenReturn(asset);
    }

    public static void stubAttributes(ThingsboardClient client) throws Exception {
        when(client.saveEntityAttributesV2(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{}");
    }
}
