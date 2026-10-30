// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.gen.MsgProtos.TbMsgProto;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trace state travels in the processing context, not in the user-visible metadata.
 */
class TbMsgTraceStateTest {

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String TRACE_PARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
    private static final String SPAN_ID = "b7ad6b7169203331";

    @Test
    void givenTracedMsg_whenSerializedAndRead_thenTraceStateSurvivesOutsideTheMetadata() {
        TbMsg msg = msg(TbMsgMetaData.EMPTY);
        traceIt(msg);

        TbMsg read = TbMsg.fromProto("Main", TbMsg.toProto(msg), TbMsgCallback.EMPTY);

        assertTraced(read);
        assertThat(read.getMetaData().getData()).isEmpty();
    }

    @Test
    void givenUntracedMsg_whenSerializedAndRead_thenNoTraceState() {
        TbMsg read = TbMsg.fromProto("Main", TbMsg.toProto(msg(TbMsgMetaData.EMPTY)), TbMsgCallback.EMPTY);

        assertThat(read.isTraced()).isFalse();
        assertThat(read.getTraceParent()).isNull();
        assertThat(read.getSpanOnRetry()).isNull();
        assertThat(read.isRecordMsgPayload()).isFalse();
    }

    @Test
    void givenTracedMsg_whenTransformed_thenTheCopyIsTracedIndependently() {
        TbMsg msg = msg(TbMsgMetaData.EMPTY);
        traceIt(msg);

        TbMsg transformed = msg.transform().data("{}").build();
        transformed.setTraceParent("00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01");

        assertThat(transformed.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(msg.getTraceParent()).isEqualTo(TRACE_PARENT);
    }

    @Test
    void givenTracedMsg_whenClearTrace_thenNoTraceStateLeft() {
        TbMsg msg = msg(TbMsgMetaData.EMPTY);
        traceIt(msg);

        msg.clearTrace();

        assertThat(msg.isTraced()).isFalse();
        assertThat(msg.isTraceAuthorized()).isFalse();
        assertThat(msg.getTraceParent()).isNull();
        assertThat(msg.getSpanOnRetry()).isNull();
        assertThat(msg.isRecordMsgPayload()).isFalse();
    }

    private static void traceIt(TbMsg msg) {
        msg.setTraceAuthorized(true);
        msg.setTraceId(TRACE_ID);
        msg.setTraceParent(TRACE_PARENT);
        msg.setSpanOnRetry(SPAN_ID);
        msg.setRecordMsgPayload(true);
    }

    private static void assertTraced(TbMsg msg) {
        assertThat(msg.isTraceAuthorized()).isTrue();
        assertThat(msg.getTraceId()).isEqualTo(TRACE_ID);
        assertThat(msg.getTraceParent()).isEqualTo(TRACE_PARENT);
        assertThat(msg.getSpanOnRetry()).isEqualTo(SPAN_ID);
        assertThat(msg.isRecordMsgPayload()).isTrue();
    }

    private static TbMsg msg(TbMsgMetaData metaData) {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .copyMetaData(metaData)
                .data("{\"temperature\":42}")
                .build();
    }

}
