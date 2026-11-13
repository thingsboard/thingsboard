// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * The last offline report: a durable row plus a node-local copy that outranks it until a newer write lands, so
 * a node whose durable write failed still serves what it produced. Ordering relies on wall clocks across
 * nodes. {@link #clear()} writes a tombstone so "cleared" differs from "never stored".
 */
@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class CommunityGrantOfflineReportStore {

    public static final String SETTINGS_KEY = "communityGrantReport";
    private static final String REPORT_FIELD = "report";
    private static final String UPDATED_AT_FIELD = "updatedAt";

    private final AdminSettingsService adminSettingsService;

    private final AtomicReference<LocalReport> nodeLocalReport = new AtomicReference<>();

    @Setter(AccessLevel.PACKAGE)
    private LongSupplier clock = System::currentTimeMillis;

    /**
     * The last report, or empty when there is none. A failed read propagates unless this node has a copy of
     * its own to answer with: a read that did not reach the database is no evidence of absence.
     */
    public Optional<String> get() {
        LocalReport localReport = nodeLocalReport.get();
        Optional<String> local = Optional.ofNullable(localReport)
                .map(LocalReport::report)
                .filter(StringUtils::isNotEmpty);
        StoredReport stored;
        try {
            stored = readStored();
        } catch (RuntimeException e) {
            if (localReport == null) {
                throw e;
            }
            log.warn("Failed to read the stored community grant offline report; "
                    + "serving what this node last did itself", e);
            return local;
        }
        if (stored == null) {
            return local;
        }
        if (localReport != null && !localReport.durable()
                && stored.updatedAt() <= localReport.writtenAt()) {
            return local;
        }
        if (stored.report() == null) {
            nodeLocalReport.compareAndSet(localReport, null);
            return Optional.empty();
        }
        if (localReport != null && !localReport.durable()) {
            // Dropped so a later write carrying an earlier instant cannot revive it.
            nodeLocalReport.compareAndSet(localReport, null);
        }
        return Optional.of(stored.report());
    }

    public void put(String report) {
        long writtenAt = clock.getAsLong();
        LocalReport pending = new LocalReport(report, false, writtenAt);
        nodeLocalReport.set(pending);
        if (writeStored(report, writtenAt)) {
            nodeLocalReport.compareAndSet(pending, new LocalReport(report, true, writtenAt));
        }
    }

    /**
     * Retires the report along with the enrollment it belonged to. Until the durable write lands, the local
     * copy answers empty on this node.
     */
    public void clear() {
        long writtenAt = clock.getAsLong();
        LocalReport pending = new LocalReport(null, false, writtenAt);
        nodeLocalReport.set(pending);
        if (writeStored(null, writtenAt)) {
            nodeLocalReport.compareAndSet(pending, null);
        }
    }

    /** {@code null} for no usable row; a {@link StoredReport} with a {@code null} report for the tombstone. */
    private StoredReport readStored() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
        if (settings == null || settings.getJsonValue() == null) {
            return null;
        }
        JsonNode value = settings.getJsonValue();
        JsonNode report = value.get(REPORT_FIELD);
        if (report == null) {
            return null;
        }
        long updatedAt = value.path(UPDATED_AT_FIELD).asLong();
        if (report.isNull()) {
            return new StoredReport(null, updatedAt);
        }
        return report.isTextual() && !report.asText().isEmpty()
                ? new StoredReport(report.asText(), updatedAt)
                : null;
    }

    /** A null report writes the tombstone. Returns whether the write reached the database. */
    private boolean writeStored(String report, long updatedAt) {
        try {
            AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, SETTINGS_KEY);
            if (settings == null) {
                settings = new AdminSettings();
                settings.setTenantId(TenantId.SYS_TENANT_ID);
                settings.setKey(SETTINGS_KEY);
            }
            ObjectNode value = JacksonUtil.newObjectNode();
            if (report == null) {
                value.putNull(REPORT_FIELD);
            } else {
                value.put(REPORT_FIELD, report);
            }
            value.put(UPDATED_AT_FIELD, updatedAt);
            settings.setJsonValue(value);
            adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, settings);
            return true;
        } catch (RuntimeException e) {
            log.warn("Failed to store the community grant offline report", e);
            return false;
        }
    }

    /** A {@code null} report stands for a {@link #clear()}. */
    private record LocalReport(String report, boolean durable, long writtenAt) {}

    private record StoredReport(String report, long updatedAt) {}

}
