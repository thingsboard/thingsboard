// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.community_grant.CommunityGrantFlowState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantOfflineRun;
import org.thingsboard.server.common.data.community_grant.CommunityGrantParkReason;
import org.thingsboard.server.common.data.community_grant.CommunityGrantState;
import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityGrantStateInfoTest {

    @Test
    void testStateInfoSerializesExactlyTheNineAllowedFields() {
        CommunityGrantFlowState flowState = CommunityGrantFlowState.initial();
        flowState.setSignUpUrl("https://portal.example/signup");
        flowState.setLastPolledAt(7L);
        flowState.setParkReason(CommunityGrantParkReason.CHECK_FAILED);
        // Must not reach the widget.
        flowState.setStartedAt(3L);
        flowState.setOfflineRun(CommunityGrantOfflineRun.running(UUID.randomUUID(), 5L));

        JsonNode json = JacksonUtil.valueToTree(
                CommunityGrantStateInfo.of(flowState, "https://portal.example"));

        List<String> fieldNames = new ArrayList<>();
        json.fieldNames().forEachRemaining(fieldNames::add);
        assertThat(fieldNames).containsExactlyInAnyOrder("mode", "state", "signUpUrl", "lastPolledAt",
                "parkReason", "portalUrl", "offlineReport", "offlineRunInProgress", "offlineRunError");
        assertThat(json.get("parkReason").asText()).isEqualTo("CHECK_FAILED");
        assertThat(json.get("portalUrl").asText()).isEqualTo("https://portal.example");
        assertThat(json.get("offlineReport").isNull()).isTrue();
        assertThat(json.get("offlineRunInProgress").asBoolean()).isFalse();
        assertThat(json.get("offlineRunError").isNull()).isTrue();
    }

    @Test
    void testOnlyARecordedByHandRunOnTheReportStepCarriesTheReport() {
        CommunityGrantFlowState flowState = CommunityGrantFlowState.initial();
        flowState.setState(CommunityGrantState.VALIDATING);
        assertThat(CommunityGrantStateInfo.carriesOfflineReport(flowState)).isFalse();

        flowState.setLastOfflineCheckAt(11L);
        assertThat(CommunityGrantStateInfo.carriesOfflineReport(flowState)).isTrue();

        flowState.setState(CommunityGrantState.COLLECTING);
        assertThat(CommunityGrantStateInfo.carriesOfflineReport(flowState)).isFalse();
        flowState.setState(CommunityGrantState.REGISTERED);
        assertThat(CommunityGrantStateInfo.carriesOfflineReport(flowState)).isFalse();
    }

    @Test
    void testTheHandedOverStepKeepsCarryingTheReport() {
        CommunityGrantFlowState flowState = CommunityGrantFlowState.initial();
        flowState.setState(CommunityGrantState.REPORT_HANDED_OVER);
        flowState.setLastOfflineCheckAt(11L);

        assertThat(CommunityGrantStateInfo.carriesOfflineReport(flowState)).isTrue();
    }

}
