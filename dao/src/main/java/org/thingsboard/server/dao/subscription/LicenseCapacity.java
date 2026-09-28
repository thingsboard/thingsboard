// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * When a deployment holds more entities than the licence it is on - or is about to move to - covers, and the
 * one wording every refusal derived from that uses. Shared by the startup gate, the upgrade pre-flight and the
 * licence-key replacement, so the three cannot drift apart.
 */
public final class LicenseCapacity {

    /** The entity kinds whose licensed count is enforced. The singular doubles as the table name. */
    @Getter
    @RequiredArgsConstructor
    public enum CappedEntity {

        DEVICE("device", "devices"),
        ASSET("asset", "assets");

        private final String singular;
        private final String plural;

    }

    private LicenseCapacity() {
    }

    /**
     * Strictly greater, not {@code >=}: the creation guard blocks the {@code (limit + 1)}-th entity, so an
     * instance holding exactly {@code limit} of them is at its cap and fully compliant.
     * <p>
     * Only the boundary - each site keeps its own "no cap applies" guard, because a zero means something
     * different in the normalised quota than in the raw plan value.
     */
    public static boolean exceedsCap(long count, long limit) {
        return count > limit;
    }

    /**
     * What the operator is told when the instance holds more entities than its licence covers. It is read by
     * people who did not configure the licence, so it names both numbers and every way out and nothing else.
     * It also serves plans and editions the two grant-only call sites never see, so it names neither.
     */
    public static String capExceededMessage(CappedEntity entity, long count, long limit) {
        return capExceededMessage(entity, count + " " + entity.getPlural() + " are present", limit);
    }

    /**
     * The same sentence when only the bound is known - the cap has been established but the exact count could
     * not be read. The refusal stands either way, so the operator gets the bound rather than nothing.
     */
    public static String capExceededMessage(CappedEntity entity, long limit) {
        return capExceededMessage(entity, "more than " + limit + " " + entity.getPlural() + " are present", limit);
    }

    private static String capExceededMessage(CappedEntity entity, String presence, long limit) {
        String singular = entity.getSingular();
        String plural = entity.getPlural();
        return StringUtils.capitalize(singular) + " count exceeds the limit this instance is licensed for: " + presence +
                ", the license allows " + limit + ". " +
                "Reduce the number of " + plural + " to " + limit + " or fewer, buy an add-on at " +
                LicensePortal.URL + " to raise the limit, or use a different license key, and try again.";
    }

}
