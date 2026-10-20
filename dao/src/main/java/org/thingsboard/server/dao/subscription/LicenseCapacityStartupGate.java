// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.subscription.LicenseCapacity.CappedEntity;

import java.util.function.Supplier;

/**
 * Refuses to start an instance that holds more devices or assets than its licence covers, for the plans
 * {@link SubscriptionService#isCommunityGrantLicense()} identifies and for no others. Everywhere else - and
 * for edges under every licence - the quotas stay soft, creation-time caps
 * ({@link SubscriptionService#createDeviceAllowed}).
 * <p>
 * Hooked to {@link ApplicationReadyEvent} so the licence client is published and the database reachable, and
 * so a refusal fails the boot with this message as the top-level cause. Every uncertain answer lets the
 * instance boot: no licence client, no grant key, an unlimited quota or an unreadable count all fall through.
 * Enforcement is boot-only, because applying a key is the operator's way back into a locked instance.
 */
@Service
@Profile("!install & !test")
@RequiredArgsConstructor
@Slf4j
public class LicenseCapacityStartupGate implements ApplicationListener<ApplicationReadyEvent> {

    private final SubscriptionService subscriptionService;
    private final DeviceService deviceService;
    private final AssetService assetService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        check();
    }

    void check() {
        if (!subscriptionService.isCommunityGrantLicense()) {
            return;
        }
        verifyCap(CappedEntity.DEVICE, subscriptionService.getLicensedDeviceLimit(), deviceService::countDevices);
        verifyCap(CappedEntity.ASSET, subscriptionService.getLicensedAssetLimit(), assetService::countAssets);
    }

    private void verifyCap(CappedEntity entity, long limit, Supplier<Long> counter) {
        if (limit <= 0) {
            // The normalised quota reports unlimited as 0, and a negative value cannot be a cap either way.
            return;
        }
        Long count;
        try {
            count = counter.get();
        } catch (Exception e) {
            log.warn("Failed to count {} while validating the licensed limit; startup continues", entity.getPlural(), e);
            return;
        }
        if (count == null) {
            return;
        }
        if (LicenseCapacity.exceedsCap(count, limit)) {
            String message = LicenseCapacity.capExceededMessage(entity, count, limit);
            // Without a throwable, so the operator reads the sentence rather than a stack trace.
            log.error(message);
            throw new EntityCapExceededException(message);
        }
    }

}
