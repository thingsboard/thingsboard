// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.client.NonProductionTbLicenseClient;
import org.thingsboard.license.shared.PlanDataConstants;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionEntry;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service
@Slf4j
@Profile("!install & !test")
public class BasicSubscriptionService implements SubscriptionService {

    private static final Map<String, Predicate<String>> solutionTemplateLevelFilters = new HashMap<>();

    private static final Set<String> makerSolutionTemplateLevelPlans = Set.of("Maker", "Prototype", "Pilot", "Startup", "Business", "Enterprise", "Perpetual");
    private static final Set<String> prototypeSolutionTemplateLevelPlans = Set.of("Prototype", "Pilot", "Startup", "Business", "Enterprise", "Perpetual");
    private static final Set<String> startUpSolutionTemplateLevelPlans = Set.of("Pilot", "Startup", "Business", "Enterprise", "Perpetual");

    static {
        solutionTemplateLevelFilters.put("MAKER", planName -> makerSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
        solutionTemplateLevelFilters.put("PROTOTYPE", planName -> prototypeSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
        solutionTemplateLevelFilters.put("STARTUP", planName -> startUpSolutionTemplateLevelPlans.stream().anyMatch(planName::contains));
    }

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EdgeService edgeService;

    @Autowired
    protected AgentService agentService;

    @Autowired
    private LicenseActivationService licenseActivationService;

    /** Device count above which {@link #warnIfNonProductionAtScale(TenantId)} logs the warning. */
    static final long NON_PRODUCTION_SCALE_WARN_THRESHOLD = 100L;
    /** How often {@link #warnIfNonProductionAtScale(TenantId)} is willing to open a new check window. */
    static final long NON_PRODUCTION_SCALE_WARN_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);
    /** The subscription plan name reported for a keyless non-production instance. */
    static final String NON_PRODUCTION_PLAN_NAME = "Non-production";
    /**
     * When {@link #warnIfNonProductionAtScale(TenantId)} last opened a check window. An {@link AtomicLong}
     * because that method's check-then-act must single-flight concurrent callers.
     */
    private final AtomicLong lastScaleWarnTs = new AtomicLong(0);

    @Override
    public LicenseInfo getLicenseInfo() {
        AbstractTbLicenseClient client = requireLicenseClient("the instance is not activated");
        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setMaxDevices(reportedQuota(client, PlanDataConstants.MAX_DEVICES_KEY));
        licenseInfo.setMaxAssets(reportedQuota(client, PlanDataConstants.MAX_ASSETS_KEY));
        licenseInfo.setMaxEdges(reportedQuota(client, PlanDataConstants.MAX_EDGES_KEY));
        licenseInfo.setMaxAgents(reportedQuota(client, PlanDataConstants.MAX_AGENTS_KEY));
        licenseInfo.setWhiteLabelingEnabled(client.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY));
        licenseInfo.setDevelopment(resolveDevelopment());
        try {
            licenseInfo.setPlan(client.getPlanStringValue(PlanDataConstants.PLAN_KEY));
        } catch (Exception e) {
            licenseInfo.setPlan("Unknown");
        }
        return licenseInfo;
    }

    /**
     * Answered from the version {@link LicenseActivationService} mirrors alongside the client, so this and
     * the quota helpers cannot disagree about the same licence.
     */
    @Override
    public int getLicenseVersion() {
        return licenseActivationService.getLicenseVersion();
    }

    @Override
    public SubscriptionInfo getSubscriptionInfo() {
        AbstractTbLicenseClient client = requireLicenseClient("the instance is not activated");
        SubscriptionInfo subscriptionInfo = SubscriptionInfoMapper.toSubscriptionInfo(client.getPlanData(),
                client.getSubscriptionData(), getLicenseVersion(), licenseActivationService.isOfflineLicense());
        subscriptionInfo.setDataTs(client.getDataTs());
        subscriptionInfo.setLicenseServerEndpoint(client.getLicenseServerEndpoint());
        // Re-read from the client rather than left as the mapper read it off the plan: a client that cannot
        // answer the question at all has to report non-production, which plan data alone cannot express.
        subscriptionInfo.setDevelopment(resolveDevelopment());
        // From the same snapshot as everything above, so a client replaced midway cannot make them contradict.
        subscriptionInfo.setNonProduction(client instanceof NonProductionTbLicenseClient);
        subscriptionInfo.setDevicesCount(countDevices());
        subscriptionInfo.setAssetsCount(countAssets());
        subscriptionInfo.setEdgesCount(countEdges());
        subscriptionInfo.setAgentsCount(countAgents());
        if (subscriptionInfo.isNonProduction()) {
            applyNonProductionSubscription(subscriptionInfo);
        }
        return subscriptionInfo;
    }

    /**
     * Rewrites the subscription-shaped fields of a keyless instance, which has no subscription behind any of
     * them, so they are not reported as Java defaults. {@code perpetual = false} with null period timestamps
     * describes a state the client contract says cannot exist, and consumers would take the periodic branch
     * and render nulls. The subscription id stays null so consumers drop the actions that need it.
     */
    private static void applyNonProductionSubscription(SubscriptionInfo subscriptionInfo) {
        subscriptionInfo.setSubscriptionPlanName(NON_PRODUCTION_PLAN_NAME);
        subscriptionInfo.setPerpetual(true);
        subscriptionInfo.setCurrentPeriodStartTs(null);
        subscriptionInfo.setCurrentPeriodEndTs(null);
        subscriptionInfo.setEndTs(null);
        subscriptionInfo.setUpcomingInvoiceDate(null);
        subscriptionInfo.setUpcomingInvoiceAmountDue(null);
        subscriptionInfo.setLicenseServerEndpoint(null);
    }

    /**
     * Serialized on purpose. The license client encodes a monotonic {@code requestSequenceNumber} in every
     * {@code checkInstance} call, and two concurrent {@code refreshInstance()} invocations both encode the
     * <em>same</em> current sequence number: one wins on the license server, the other's sequence is now
     * stale and the server answers {@code INVALID_LICENSE_CHECK_SECRET(107)}. The client marks that as
     * critical and {@link BasicLicenseActivationService#onError} then locks the management plane over a
     * false positive. Concurrent callers here (a router resolver and a manual "refresh" button clicking
     * within ~60&nbsp;ms is enough) are far cheaper than a spurious lockout, so blocking them on the same
     * monitor and re-issuing the refresh in order is the right cost: the second caller gets a licence view
     * that is at worst one round-trip fresher than the first.
     */
    @Override
    public synchronized SubscriptionInfo refreshLicense() {
        requireLicenseClient("the instance is not activated").refreshInstance();
        return this.getSubscriptionInfo();
    }

    /**
     * The client this node is currently running on, or a failure if there is none. Reads only - the field
     * itself belongs to {@link LicenseActivationService}, which is the one place it changes. It hands the
     * client back rather than only checking for it, and callers must use what they are given: a lock can null
     * the field at any moment, turning a re-read into a {@code NullPointerException} instead of the refusal.
     *
     * @param whatIsUnavailable the rest of the operator-facing sentence after "License required: ".
     */
    private AbstractTbLicenseClient requireLicenseClient(String whatIsUnavailable) {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            throw new SubscriptionException("License required: " + whatIsUnavailable,
                    SubscriptionErrorCode.FEATURE_DISABLED);
        }
        return client;
    }

    @Override
    public boolean isLicenseActivated() {
        return licenseActivationService.isLicenseActivated();
    }

    @Override
    public void pickUpStoredLicenseSecret() {
        licenseActivationService.pickUpStoredLicenseSecret();
    }

    @Override
    public void reconcileLicenseState() {
        licenseActivationService.reconcileLicenseState();
    }

    @Override
    public boolean isNonProductionMode() {
        return licenseActivationService.isNonProductionMode();
    }

    @Override
    public boolean revokeNonProductionEntitlement() {
        return licenseActivationService.revokeNonProductionEntitlement();
    }

    @Override
    public void applyLicenseKey(String secret) {
        licenseActivationService.applyLicenseKey(secret);
    }

    @Override
    public SubscriptionInfo previewLicenseKey(String secret) {
        return licenseActivationService.previewLicenseKey(secret);
    }

    @Override
    public void clearLicense() {
        licenseActivationService.clearLicense();
    }

    @Override
    public String getAiToken() {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return null;
        }
        return client.getAiToken();
    }

    /** Answers from the client it is handed, which a lock cannot null underneath the caller. */
    private boolean limitReached(AbstractTbLicenseClient client, long actual, String key) {
        long limit = client.getPlanLongValue(key);
        if (limit > 0) {
            return actual >= limit;
        } else {
            return getLicenseVersion() > 1;
        }
    }

    /**
     * Whether the quota behind {@code key} is unlimited on the licence this node runs on. The rule itself
     * lives in {@link SubscriptionInfoMapper}, so the enforcement paths and the reporting ones cannot drift.
     */
    private boolean isUnlimited(AbstractTbLicenseClient client, String key) {
        return SubscriptionInfoMapper.isUnlimited(client.getPlanLongValue(key), getLicenseVersion());
    }

    /**
     * The quota as {@link LicenseInfo} and the licensed-limit getters report it: the real limit, or 0 when
     * unlimited. Their 0 is load-bearing - the home-page widget renders it as the infinity glyph, and the
     * licensed-limit getters already answer 0 for a node with no licence client - so it stays, while
     * {@link SubscriptionInfo} reports the unlimited sentinel instead.
     */
    private long reportedQuota(AbstractTbLicenseClient client, String key) {
        long limit = client.getPlanLongValue(key);
        return SubscriptionInfoMapper.isUnlimited(limit, getLicenseVersion()) ? 0 : limit;
    }

    @Override
    public void createDeviceAllowed(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = requireLicenseClient("device creation is disabled until the instance is activated");
        warnIfNonProductionAtScale(tenantId);
        if (isUnlimited(client, PlanDataConstants.MAX_DEVICES_KEY)) {
            return;
        }
        long actualCount = countDevices();
        if (limitReached(client, actualCount, PlanDataConstants.MAX_DEVICES_KEY)) {
            log.error("Maximum allowed devices limit reached!");
            throw new SubscriptionException("Maximum allowed devices limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.DEVICE_COUNT, client.getPlanLongValue(PlanDataConstants.MAX_DEVICES_KEY));
        }
    }

    @Override
    public void createAssetAllowed(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = requireLicenseClient("asset creation is disabled until the instance is activated");
        if (isUnlimited(client, PlanDataConstants.MAX_ASSETS_KEY)) {
            return;
        }
        long actualCount = countAssets();
        if (limitReached(client, actualCount, PlanDataConstants.MAX_ASSETS_KEY)) {
            log.error("Maximum allowed assets limit reached!");
            throw new SubscriptionException("Maximum allowed assets limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.ASSET_COUNT, client.getPlanLongValue(PlanDataConstants.MAX_ASSETS_KEY));
        }
    }

    @Override
    public void createEdgeAllowed(TenantId tenantId) throws SubscriptionException {
        requireLicenseClient("edge creation is disabled until the instance is activated");
        if (!isCreateEdgeAllowed(tenantId)) {
            log.error("Maximum allowed edges limit reached!");
            throw new SubscriptionException("Maximum allowed edges limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.EDGE_COUNT, 0);
        }
    }

    /**
     * The edge quota is enforced here and only here, at creation: unlike the device and asset quotas it is not
     * a startup or upgrade condition, so an instance already over the limit runs normally.
     */
    @Override
    public boolean isCreateEdgeAllowed(TenantId tenantId) {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        if (getLicenseVersion() < 2 || isUnlimited(client, PlanDataConstants.MAX_EDGES_KEY)) {
            return true;
        }
        long actualCount = countEdges();
        return !limitReached(client, actualCount, PlanDataConstants.MAX_EDGES_KEY);
    }

    /**
     * An <b>absent</b> {@code maxagents} is a cap of none on a v2 licence - the same reading
     * {@link #isCreateEdgeAllowed} gives an absent {@code maxedges}. Nothing is taken away by that: agents
     * ship for the first time in this release, so no licence issued earlier ever granted any, and a plan
     * that means to include some says so. Reading absent as "no limit" instead would hand unlimited agents
     * to every plan that includes none - the free, Development and Community Grant tiers among them, whose
     * plan data is built outside {@code setupPlanData} and carries no agent quota at all.
     * <p>
     * A v1 licence predates explicit quotas entirely and stays permissive, through {@link #isUnlimited}.
     */
    @Override
    public void createAgentAllowed(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = requireLicenseClient("agent creation is disabled until the instance is activated");
        if (isUnlimited(client, PlanDataConstants.MAX_AGENTS_KEY)) {
            return;
        }
        long limit = client.getPlanLongValue(PlanDataConstants.MAX_AGENTS_KEY);
        if (countAgents() >= limit) {
            log.error("Maximum allowed agents limit reached!");
            throw new SubscriptionException("Maximum allowed agents limit reached!",
                    SubscriptionErrorCode.LIMIT_REACHED, SubscriptionEntry.AGENT_COUNT, limit);
        }
    }

    @Override
    public void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException {
        // Both refusals - no licence client at all, and a plan that withholds white labeling - are the same.
        if (!whiteLabelingEnabled(tenantId)) {
            throw new SubscriptionException("White Labeling feature is disabled!",
                    SubscriptionErrorCode.FEATURE_DISABLED, SubscriptionEntry.WHITE_LABELING, 0);
        }
    }

    @Override
    public boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        return client.getPlanBooleanValue(PlanDataConstants.WHITELABELING_KEY);
    }

    @Override
    public boolean edgeEnabled(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        return client.getPlanBooleanValue(PlanDataConstants.EDGE_KEY);
    }

    @Override
    public boolean trendzEnabled(TenantId tenantId) throws SubscriptionException {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        return client.getPlanBooleanValue(PlanDataConstants.TRENDZ_KEY);
    }

    @Override
    public boolean isFeatureEnabled(TenantId tenantId, PlatformFeature feature) {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        // An absent client is not an absent plan-data key: the absent key means a licence answered and says
        // nothing about this feature, which grants it, whereas no client means no licence answered at all, so
        // it fails closed. The keyless deployment is unaffected - its client carries none of these keys.
        if (client == null) {
            return false;
        }
        return client.isPlanFeatureEnabled(feature.getPlanDataKey());
    }

    @Override
    public void checkFeatureAllowed(TenantId tenantId, PlatformFeature feature) throws SubscriptionException {
        if (!isFeatureEnabled(tenantId, feature)) {
            throw new SubscriptionException(feature.getDisplayName() + " feature is disabled!",
                    SubscriptionErrorCode.FEATURE_DISABLED, feature.getSubscriptionEntry(), 0);
        }
    }

    @Override
    public boolean isDevelopment(TenantId tenantId) throws SubscriptionException {
        return resolveDevelopment();
    }

    /**
     * Read live rather than cached: the licence client refreshes its plan in place when the portal answers, so
     * a plan changed there reaches every node on its own check period without anything here to invalidate. An
     * unknown answer - no client, or one that cannot answer - reads as development, so only a licence removes
     * the notice.
     */
    boolean resolveDevelopment() {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return true;
        }
        try {
            return client.getPlanBooleanValue(PlanDataConstants.DEVELOPMENT_KEY);
        } catch (Exception e) {
            log.debug("Failed to resolve the development flag from the license; assuming non-production", e);
            return true;
        }
    }

    @Override
    public boolean isCommunityGrantLicense() {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        try {
            return client.getPlanBooleanValue(CommunityGrantPlan.COMMUNITY_GRANT_KEY);
        } catch (Exception e) {
            log.debug("Failed to resolve the community grant flag from the license", e);
            return false;
        }
    }

    @Override
    public long getLicensedDeviceLimit() {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return 0;
        }
        try {
            return reportedQuota(client, PlanDataConstants.MAX_DEVICES_KEY);
        } catch (Exception e) {
            log.debug("Failed to resolve the device quota from the license", e);
            return 0;
        }
    }

    @Override
    public long getLicensedAssetLimit() {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return 0;
        }
        try {
            return reportedQuota(client, PlanDataConstants.MAX_ASSETS_KEY);
        } catch (Exception e) {
            log.debug("Failed to resolve the asset quota from the license", e);
            return 0;
        }
    }

    @Override
    public boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException {
        AbstractTbLicenseClient client = licenseActivationService.getClient();
        if (client == null) {
            return false;
        }
        String planName = client.getPlanStringValue(PlanDataConstants.PLAN_KEY);
        return solutionTemplateLevelFilters.get(solutionTemplateLevel).test(planName);
    }

    /**
     * Reminds an operator running at production scale that development mode is for non-production use. Sits
     * above the isUnlimited short-circuit in {@link #createDeviceAllowed(TenantId)} because a development
     * licence has an unlimited device quota, and the wording has to hold for both deployments
     * {@link #isDevelopment(TenantId)} is true for - keyless, and the portal's Development plan. Throttled to
     * once an hour because it needs its own device count; failures are caught, never propagated.
     */
    private void warnIfNonProductionAtScale(TenantId tenantId) {
        long now = System.currentTimeMillis();
        long seenAt = lastScaleWarnTs.get();
        if (seenAt != 0 && now >= seenAt && now - seenAt < NON_PRODUCTION_SCALE_WARN_INTERVAL_MS) {
            return;
        }
        if (!lastScaleWarnTs.compareAndSet(seenAt, now)) {
            // Another thread already claimed this window; it - not this caller - evaluates the warning for it.
            return;
        }
        try {
            if (!isDevelopment(tenantId)) {
                return;
            }
            long deviceCount = countDevices();
            if (deviceCount < NON_PRODUCTION_SCALE_WARN_THRESHOLD) {
                return;
            }
            log.warn("{} devices provisioned. {}. This instance has unlimited device, asset and edge quotas, " +
                            "so this is not a limit warning. If the deployment is production, obtain a free " +
                            "production key at " + LicensePortal.URL,
                    deviceCount, DataConstants.NON_PRODUCTION_NOTICE);
        } catch (Exception e) {
            log.warn("Failed to evaluate the non-production device-count warning", e);
        }
    }

    private long countDevices() {
        return deviceService.countDevices();
    }

    private long countAssets() {
        return assetService.countAssets();
    }

    private long countEdges() {
        return edgeService.countEdges();
    }

    private long countAgents() {
        return agentService.countAgents();
    }

}
