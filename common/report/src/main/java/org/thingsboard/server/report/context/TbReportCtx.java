// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder(toBuilder = true)
public abstract class TbReportCtx implements Closeable {

    private final TenantId tenantId;
    private final ReportTemplateConfig configuration;
    private final String timeZone;
    private final UserId userId;
    private final EntityId userOwnerId;
    private final String accessToken;
    private final long accessTokenExpTs;
    private final String reportCreatedTime;
    private final boolean nonProduction;

    private final Map<String, Object> params = new HashMap<>();
    private final Map<String, UUID> scripts = new HashMap<>();
    private final TbelInvokeService tbelInvokeService;

    public abstract TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig);

    @Override
    public void close() throws IOException {
        getScripts().values().forEach(scriptId -> {
            if (scriptId != null) {
                tbelInvokeService.release(scriptId);
            }
        });
    }

}
