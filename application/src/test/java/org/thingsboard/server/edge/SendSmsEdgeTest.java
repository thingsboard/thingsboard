// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edge;

import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SendSmsUplinkMsg;
import org.thingsboard.server.service.sms.EdgeSmsRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Verifies the send-sms uplink: when the Edge delegates an SMS send, the Cloud receives the
 * SendSmsUplinkMsg and invokes its SmsService (which resolves the config and transmits). Here the
 * EdgeImitator plays the Edge and sends the uplink; the Cloud's SmsService is mocked so we can assert the
 * delegated call arrives with the expected arguments (the Cloud, not the Edge, uses the tenant id of the
 * connected edge).
 */
@DaoSqlTest
public class SendSmsEdgeTest extends AbstractEdgeTest {

    @MockitoBean
    private SmsService smsService;

    @Test
    public void testSendSmsFromEdgeProcessedByCloud() throws Exception {
        EdgeSmsRequest request = EdgeSmsRequest.builder()
                .method(EdgeSmsRequest.SmsMethod.SEND_SMS)
                .numbers(new String[]{"+15551234567"})
                .message("Edge test message")
                .build();

        sendSmsUplink(request);

        verify(smsService, timeout(30000))
                .sendSms(eq(tenantId), isNull(), any(String[].class), eq("Edge test message"));
    }

    @Test
    public void testSendTestSmsFromEdgeProcessedByCloud() throws Exception {
        TestSmsRequest testSmsRequest = new TestSmsRequest();
        testSmsRequest.setNumberTo("+15551234567");
        testSmsRequest.setMessage("Test message");
        EdgeSmsRequest request = EdgeSmsRequest.builder()
                .method(EdgeSmsRequest.SmsMethod.SEND_TEST_SMS)
                .testSmsRequest(testSmsRequest)
                .build();

        sendSmsUplink(request);

        verify(smsService, timeout(30000)).sendTestSms(any(TestSmsRequest.class));
    }

    private void sendSmsUplink(EdgeSmsRequest request) throws Exception {
        SendSmsUplinkMsg sendSmsUplinkMsg = SendSmsUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(request))
                .build();
        sendUplinkMsgAndWaitForResponse(builder -> builder.addSendSmsUplinkMsg(sendSmsUplinkMsg));
    }

}
