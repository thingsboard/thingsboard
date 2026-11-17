[#ftl]
[#--
    SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
    SPDX-License-Identifier: BUSL-1.1

    The errors of a scan that stopped the notices, by package: notices.sh runs it only then. ORT gives those that
    ort-config/resolutions.yml does not accept, of the severity that stops it, and stops with them as stop.ftl does.
--]
[#import "stop.ftl" as stop]
[#-- The severity from which the scan fails: ort.severeIssueThreshold in ort-config/config.yml --]
[#assign issues = ortResult.getScannerIssues(true, true, statics["org.ossreviewtoolkit.model.Severity"].WARNING)]
[#assign lines = []]
[#list issues as id, found]
    [#list found as issue]
        [#assign lines = lines + [id.toCoordinates() + ": " + issue.message?trim?replace("\n", "\n    ")]]
    [/#list]
[/#list]
[#if lines?has_content]
    [#stop stop.message("the scan of these packages reported errors, so their notices may be incomplete.\n"
        + stop.lines(lines) + "\n"
        + "To fix:\n"
        + "  - a download error is often passing: run the build again;\n"
        + "  - if the error does not matter for the notices (for example a generated file that is also scanned from its\n"
        + "    sources), add it to tools/src/main/notices/ort-config/resolutions.yml with the reason.")]
[/#if]
No errors.
