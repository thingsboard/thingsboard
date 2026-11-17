[#ftl]
[#--
    SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
    SPDX-License-Identifier: BUSL-1.1

    The errors of a scan that stopped the notices, by package: notices.sh runs it only then. ORT gives those that
    ort-config/resolutions.yml does not accept, of the severity that stops it, and the template stops with them as
    THIRD-PARTY-NOTICES.ftl stops, between the same marks.
--]
[#assign issues = ortResult.getScannerIssues(true, true, statics["org.ossreviewtoolkit.model.Severity"].WARNING)]
[#assign lines = []]
[#list issues as id, found]
    [#list found as issue]
        [#assign lines = lines + [id.toCoordinates() + ": " + issue.message?trim?replace("\n", "\n    ")]]
    [/#list]
[/#list]
[#if lines?has_content]
    [#stop "\nNOTICES-MESSAGE-START\nThird-party notices: the scan of these packages reported errors, so their notices may be incomplete.\n"
        + "  " + lines?join("\n  ") + "\n"
        + "To fix:\n"
        + "  - a download error is often passing: run the build again;\n"
        + "  - if the error does not matter for the notices (for example a generated file that is also scanned from its\n"
        + "    sources), add it to tools/src/main/notices/ort-config/resolutions.yml with the reason.\nNOTICES-MESSAGE-END"]
[/#if]
No errors.
