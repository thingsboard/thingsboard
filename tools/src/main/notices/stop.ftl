[#ftl]
[#--
    SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
    SPDX-License-Identifier: BUSL-1.1

    How a template stops the notices, for THIRD-PARTY-NOTICES.ftl and scan-issues.ftl. notices.sh prints the text
    between the two marks, and not the stack trace around it: stop_message() there looks for the same marks.
--]
[#function message text][#return "\nNOTICES-MESSAGE-START\nThird-party notices: " + text + "\nNOTICES-MESSAGE-END"][/#function]
[#-- The packages of a stop, one to a line --]
[#function lines items][#return "  " + items?join("\n  ")][/#function]
