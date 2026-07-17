/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SendEmailUplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.service.mail.EdgeMailRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Verifies the send-email uplink: when the Edge delegates a mail send, the Cloud receives the
 * SendEmailUplinkMsg and invokes its MailService (which resolves the config, renders and transmits). Here
 * the EdgeImitator plays the Edge and sends the uplink; the Cloud's MailService is mocked so we can assert
 * the delegated call arrives with the expected arguments (the Cloud, not the Edge, uses the tenant id of
 * the connected edge).
 */
@DaoSqlTest
public class SendEmailEdgeTest extends AbstractEdgeTest {

    @MockitoBean
    private MailService mailService;

    @Test
    public void testSendBasicEmailFromEdgeProcessedByCloud() throws Exception {
        EdgeMailRequest request = EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.SEND_BASIC)
                .to("recipient@thingsboard.io")
                .subject("Edge test subject")
                .message("<b>Edge test body</b>")
                .build();

        sendEmailUplink(request);

        verify(mailService, timeout(30000))
                .sendEmail(eq(tenantId), eq("recipient@thingsboard.io"), eq("Edge test subject"), eq("<b>Edge test body</b>"));
    }

    @Test
    public void testSendTbEmailFromEdgeProcessedByCloud() throws Exception {
        TbEmail tbEmail = TbEmail.builder()
                .from("noreply@thingsboard.io")
                .to("recipient@thingsboard.io")
                .subject("Rule chain email")
                .body("Alarm!")
                .html(false)
                .build();
        EdgeMailRequest request = EdgeMailRequest.builder()
                .method(EdgeMailRequest.MailMethod.SEND_TB_EMAIL)
                .tbEmail(tbEmail)
                .build();

        sendEmailUplink(request);

        verify(mailService, timeout(30000)).send(eq(tenantId), isNull(), any(TbEmail.class));
    }

    private void sendEmailUplink(EdgeMailRequest request) throws Exception {
        SendEmailUplinkMsg sendEmailUplinkMsg = SendEmailUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(request))
                .build();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        uplinkMsgBuilder.setUplinkMsgId(EdgeUtils.nextPositiveInt());
        uplinkMsgBuilder.addSendEmailUplinkMsg(sendEmailUplinkMsg);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
    }

}
