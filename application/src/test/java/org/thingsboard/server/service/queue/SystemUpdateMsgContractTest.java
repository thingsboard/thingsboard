// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateType;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wire contract this message is relied on for. Field numbers and the zero enum value are not
 * refactorable details: a renumbering silently re-points a live cluster's messages mid-upgrade.
 */
public class SystemUpdateMsgContractTest {

    @Test
    public void licenseIsNotTheZeroValue() {
        // proto3 answers an unset field with the zero value, so LICENSE at 0 would make every envelope that
        // carries no SystemUpdateMsg look like a licence change.
        assertThat(SystemUpdateType.SYSTEM_UPDATE_UNKNOWN.getNumber()).isZero();
        assertThat(SystemUpdateType.LICENSE.getNumber()).isEqualTo(1);
    }

    @Test
    public void ridesBothEnvelopesOnPeOnlyFieldNumbers() {
        // PE-only fields start at 100 in both envelopes, so a CE change taking the next low number cannot
        // collide with one of these on a cluster that is mid-upgrade.
        SystemUpdateMsg msg = SystemUpdateMsg.newBuilder().setType(SystemUpdateType.LICENSE).build();

        assertThat(ToCoreNotificationMsg.newBuilder().setSystemUpdateMsg(msg).build().hasSystemUpdateMsg()).isTrue();
        assertThat(ToRuleEngineNotificationMsg.newBuilder().setSystemUpdateMsg(msg).build().hasSystemUpdateMsg()).isTrue();
        assertThat(ToCoreNotificationMsg.getDescriptor().findFieldByName("systemUpdateMsg").getNumber())
                .isEqualTo(102);
        assertThat(ToRuleEngineNotificationMsg.getDescriptor().findFieldByName("systemUpdateMsg").getNumber())
                .isEqualTo(100);
    }
}
