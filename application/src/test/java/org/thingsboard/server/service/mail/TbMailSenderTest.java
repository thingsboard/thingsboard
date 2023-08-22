package org.thingsboard.server.service.mail;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TbMailSenderTest {

    @Test
    public void testDoSendSendMail() {
        ObjectNode jsonConfig = getBasicMailConfig();
        TbMailSender tbMailSender = spy(new TbMailSender(mock(TbMailContextComponent.class), jsonConfig));

        MimeMessage mimeMsg = new MimeMessage(Session.getInstance(new Properties()));
        List<MimeMessage> mimeMessages = new ArrayList<>(1);
        mimeMessages.add(mimeMsg);

        Mockito.doNothing().when(tbMailSender).updateOauth2PasswordIfExpired();
        Mockito.doNothing().when(tbMailSender).doSendSuper(any(), any());

        tbMailSender.doSend(mimeMessages.toArray(new MimeMessage[0]), null);

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
    }

    @Test
    public void testTestConnection() throws MessagingException {
        ObjectNode jsonConfig = getBasicMailConfig();
        TbMailSender tbMailSender = spy(new TbMailSender(mock(TbMailContextComponent.class), jsonConfig));

        Mockito.doNothing().when(tbMailSender).updateOauth2PasswordIfExpired();
        Mockito.doNothing().when(tbMailSender).testConnectionSuper();

        tbMailSender.testConnection();

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
    }

    @Test
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Enabled() {
        ObjectNode jsonConfig = getOauth2Config();
        TbMailSender tbMailSender = spy(new TbMailSender(mock(TbMailContextComponent.class), jsonConfig));

        Mockito.doNothing().when(tbMailSender).refreshAccessToken();

        tbMailSender.updateOauth2PasswordIfExpired();

        Mockito.verify(tbMailSender, times(1)).refreshAccessToken();
        Mockito.verify(tbMailSender, times(1)).setPassword(any());
    }

    @Test
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Disabled() {
        ObjectNode jsonConfig = getBasicMailConfig();
        TbMailSender tbMailSender = spy(new TbMailSender(mock(TbMailContextComponent.class), jsonConfig));

        tbMailSender.updateOauth2PasswordIfExpired();

        Mockito.verify(tbMailSender, Mockito.never()).refreshAccessToken();
        Mockito.verify(tbMailSender, Mockito.never()).setPassword(any());
    }

    private static ObjectNode getOauth2Config() {
        ObjectNode jsonConfig = JacksonUtil.newObjectNode();
        jsonConfig.put("smtpProtocol", "smtps");
        jsonConfig.put("timeout", "1000");
        jsonConfig.put("enableOauth2", true);
        jsonConfig.put("smtpHost", "smtp.gmail.com");
        jsonConfig.put("smtpPort", "465");
        jsonConfig.put("username", "testUser");
        return jsonConfig;
    }

    private static ObjectNode getBasicMailConfig() {
        ObjectNode jsonConfig = JacksonUtil.newObjectNode();
        jsonConfig.put("smtpProtocol", "smtps");
        jsonConfig.put("timeout", "1000");
        jsonConfig.put("smtpHost", "smtp.gmail.com");
        jsonConfig.put("smtpPort", "465");
        jsonConfig.put("username", "testUser");
        return jsonConfig;
    }
}
