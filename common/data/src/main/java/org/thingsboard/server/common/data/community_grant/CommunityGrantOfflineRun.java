// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * A by-hand checker run, recorded on the flow state so every node can report a run executing on one of them.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommunityGrantOfflineRun implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A completion updates the record only while it still carries its own id. */
    private UUID runId;
    private CommunityGrantOfflineRunStatus status;
    private Long startedAt;
    /** Why the run failed, safe to show to the operator. Set only on {@code FAILED}. */
    private String error;

    public static CommunityGrantOfflineRun running(UUID runId, long startedAt) {
        CommunityGrantOfflineRun run = new CommunityGrantOfflineRun();
        run.setRunId(runId);
        run.setStatus(CommunityGrantOfflineRunStatus.RUNNING);
        run.setStartedAt(startedAt);
        return run;
    }

    public boolean isOwnedBy(UUID runId) {
        return this.runId != null && this.runId.equals(runId);
    }

}
