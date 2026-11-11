// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.license.shared.PlanData;
import org.thingsboard.license.shared.PlanItem;
import org.thingsboard.license.shared.SubscriptionData;

import java.util.Map;
import java.util.Optional;

/**
 * A real {@link AbstractTbLicenseClient} whose plan data is whatever the test hands it, for the assertions
 * that pin properties of the published client itself rather than of any code here - a mock would happily
 * agree with whatever we assumed about it.
 * <p>
 * Everything the client derives from a key - hasPlanItem, getPlanBooleanValue, isPlanFeatureEnabled - is
 * built on getPlanItemOpt, so a fixed map behind that one method reproduces any plan a test needs, and an
 * empty map reproduces a plan that says nothing about anything.
 * <p>
 * Shared rather than repeated per test because the no-op overrides below are exactly the client's abstract
 * surface: a client upgrade that adds to it must be answered in one place, and that upgrade is the very
 * event these tests exist to detect.
 */
class FixedPlanLicenseClient extends AbstractTbLicenseClient {

    private final Map<String, PlanItem> planItems;

    FixedPlanLicenseClient(Map<String, PlanItem> planItems) {
        // The three constructor arguments are the listener, instanceLicenseCheckPeriodSeconds and
        // checkInstanceRequired. Nothing here talks to a licence server, so the listener never fires and the
        // period is never waited on; checkInstanceRequired is false so no instance verification is required.
        super(licenseException -> {}, 3600, Boolean.FALSE);
        this.planItems = planItems;
    }

    @Override
    protected void checkInstanceTask() {
    }

    @Override
    public SubscriptionData getSubscriptionData() {
        return new SubscriptionData();
    }

    @Override
    public String getAiToken() {
        return null;
    }

    @Override
    public long getDataTs() {
        return 0;
    }

    @Override
    public String getLicenseServerEndpoint() {
        return null;
    }

    @Override
    protected PlanItem getPlanItem(String key) {
        return getPlanItemOpt(key).orElse(null);
    }

    @Override
    protected Optional<PlanItem> getPlanItemOpt(String key) {
        return Optional.ofNullable(planItems.get(key));
    }

    @Override
    public PlanData getPlanData() {
        // Built on each call rather than held as a field, so a test that mutates the map it handed in still
        // sees its change here, exactly as it does through getPlanItemOpt.
        PlanData planData = new PlanData();
        planData.putAll(planItems);
        return planData;
    }

}
