// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sms.twilio;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsParseException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.util.regex.Pattern;

public class TwilioSmsSender extends AbstractSmsSender {

    private static final Pattern PHONE_NUMBERS_SID_MESSAGE_SERVICE_SID = Pattern.compile("^(PN|MG).*$");

    private TwilioRestClient twilioRestClient;
    private String numberFrom;

    private String validatePhoneTwilioNumber(String phoneNumber) throws SmsParseException {
        phoneNumber = phoneNumber.trim();
        if (!E_164_PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches() && !PHONE_NUMBERS_SID_MESSAGE_SERVICE_SID.matcher(phoneNumber).matches()) {
            throw new SmsParseException("Invalid phone number format. Phone number must be in E.164 format/Phone Number's SID/Messaging Service SID.");
        }
        return phoneNumber;
    }

    public TwilioSmsSender(TwilioSmsProviderConfiguration config) {
        if (StringUtils.isEmpty(config.getAccountSid()) || StringUtils.isEmpty(config.getAccountToken()) || StringUtils.isEmpty(config.getNumberFrom())) {
            throw new IllegalArgumentException("Invalid twilio sms provider configuration: accountSid, accountToken and numberFrom should be specified!");
        }
        this.numberFrom = this.validatePhoneTwilioNumber(config.getNumberFrom());
        this.twilioRestClient = new TwilioRestClient.Builder(config.getAccountSid(), config.getAccountToken()).build();
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        numberTo = this.validatePhoneNumber(numberTo);
        message = this.prepareMessage(message);
        try {
            String numSegments = Message.creator(new PhoneNumber(numberTo), new PhoneNumber(this.numberFrom), message).create(this.twilioRestClient).getNumSegments();
            return Integer.valueOf(numSegments);
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {

    }
}
