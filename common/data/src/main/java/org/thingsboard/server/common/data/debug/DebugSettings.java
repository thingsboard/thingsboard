// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.debug;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DebugSettings {

    private static DebugSettings DEBUG_OFF = new DebugSettings(false, 0);
    private static DebugSettings DEBUG_FAILURES = new DebugSettings(true, 0);

    public DebugSettings(boolean failuresEnabled, long allEnabledUntil) {
        this.failuresEnabled = failuresEnabled;
        this.allEnabled = false;
        this.allEnabledUntil = allEnabledUntil;
    }

    @Schema(description = "Debug failures. ", example = "false")
    private boolean failuresEnabled;
    @Schema(description = "Debug All. Used as a trigger for updating debugAllUntil.", example = "false")
    private boolean allEnabled;
    @Schema(description = "Timestamp of the end time for the processing debug events.")
    private long allEnabledUntil;

    public static DebugSettings off() {return DebugSettings.DEBUG_OFF;}

    public static DebugSettings failures() {return DebugSettings.DEBUG_FAILURES;}

    public static DebugSettings until(long ts) {return new DebugSettings(false, ts);}

    public static DebugSettings failuresOrUntil(long ts) {return new DebugSettings(true, ts);}

    public static DebugSettings all() {
        var ds = new DebugSettings();
        ds.setAllEnabled(true);
        return ds;
    }

    public DebugSettings copy(long maxDebugAllUntil) {
        return new DebugSettings(failuresEnabled, allEnabled ? maxDebugAllUntil : Math.min(allEnabledUntil, maxDebugAllUntil));
    }
}
