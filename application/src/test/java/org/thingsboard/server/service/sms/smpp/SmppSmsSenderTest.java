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
package org.thingsboard.server.service.sms.smpp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smpp.Session;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;

import java.lang.reflect.Constructor;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SmppSmsSenderTest {

    SmppSmsSender smppSmsSender;
    SmppSmsProviderConfiguration smppConfig;
    Session smppSession;

    @BeforeEach
    public void beforeEach() throws Exception {
        Constructor<SmppSmsSender> constructor = SmppSmsSender.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        smppSmsSender = spy(constructor.newInstance());

        smppSession = mock(Session.class);
        smppSmsSender.smppSession = smppSession;

        smppConfig = new SmppSmsProviderConfiguration();
        smppSmsSender.config = smppConfig;
    }

    @Test
    public void testSendSms() throws Exception {
        when(smppSession.isOpened()).thenReturn(true);
        when(smppSession.submit(any())).thenReturn(new SubmitSMResp());
        setDefaultSmppConfig();

        String number = "123545";
        String message = "message";
        smppSmsSender.sendSms(number, message);

        verify(smppSmsSender, never()).initSmppSession();
        verify(smppSession).submit(argThat(submitRequest -> {
            try {
                return submitRequest.getShortMessage().equals(message) &&
                        submitRequest.getDestAddr().getAddress().equals(number) &&
                        submitRequest.getServiceType().equals(smppConfig.getServiceType()) &&
                        (StringUtils.isEmpty(smppConfig.getSourceAddress()) ? submitRequest.getSourceAddr().getAddress().equals("")
                                : submitRequest.getSourceAddr().getAddress().equals(smppConfig.getSourceAddress()) &&
                                submitRequest.getSourceAddr().getTon() == smppConfig.getSourceTon() &&
                                submitRequest.getSourceAddr().getNpi() == smppConfig.getSourceNpi()) &&
                        submitRequest.getDestAddr().getTon() == smppConfig.getDestinationTon() &&
                        submitRequest.getDestAddr().getNpi() == smppConfig.getDestinationNpi() &&
                        submitRequest.getDataCoding() == smppConfig.getCodingScheme() &&
                        submitRequest.getReplaceIfPresentFlag() == 0 &&
                        submitRequest.getEsmClass() == 0 &&
                        submitRequest.getProtocolId() == 0 &&
                        submitRequest.getPriorityFlag() == 0 &&
                        submitRequest.getRegisteredDelivery() == 0 &&
                        submitRequest.getSmDefaultMsgId() == 0;
            } catch (Exception e) {
                fail(e.getMessage());
                return false;
            }
        }));
    }

    @Test
    public void testSendSms_dcs8_ucs2() throws Exception {
        // "Привіт" as UCS-2 (UTF-16BE) — exact wire bytes, two octets per code point.
        byte[] expected = {0x04, 0x1F, 0x04, 0x40, 0x04, 0x38, 0x04, 0x32, 0x04, 0x56, 0x04, 0x42};
        assertEncodedBytes((byte) 8, "Привіт", expected);
    }

    @Test
    public void testSendSms_dcs6_cyrillic() throws Exception {
        // "Привіт" as ISO-8859-5 — exact single-byte Cyrillic wire bytes.
        byte[] expected = {(byte) 0xBF, (byte) 0xE0, (byte) 0xD8, (byte) 0xD2, (byte) 0xF6, (byte) 0xE2};
        assertEncodedBytes((byte) 6, "Привіт", expected);
    }

    @Test
    public void testSendSms_dcs3_latin1() throws Exception {
        assertEncoded((byte) 3, "naïve café", "ISO-8859-1");
    }

    @Test
    public void testSendSms_dcs0_gsm7() throws Exception {
        // DCS 0 is the SMSC default alphabet (GSM 7-bit). 'é' is in the GSM alphabet (0x05); under a plain US-ASCII
        // fallback it would be lost to '?' (0x3F), so these bytes guard against that regression.
        byte[] expected = {0x63, 0x61, 0x66, 0x05}; // café
        assertEncodedBytes((byte) 0, "café", expected);
    }

    @Test
    public void testSendSms_dcs1_ia5() throws Exception {
        assertEncoded((byte) 1, "Hi", "US-ASCII");
    }

    @Test
    public void testSendSms_dcs2_octet() throws Exception {
        assertEncoded((byte) 2, "café", "ISO-8859-1");
    }

    @Test
    public void testSendSms_dcs4_octet() throws Exception {
        assertEncoded((byte) 4, "café", "ISO-8859-1");
    }

    @Test
    public void testSendSms_dcs5_jis() throws Exception {
        assertEncoded((byte) 5, "こんにちは", "Shift_JIS");
    }

    @Test
    public void testSendSms_dcs7_hebrew() throws Exception {
        assertEncoded((byte) 7, "שלום", "ISO-8859-8");
    }

    @Test
    public void testSendSms_dcs9_pictogram() throws Exception {
        assertEncoded((byte) 9, "café", "ISO-8859-1");
    }

    @Test
    public void testSendSms_dcs10_music() throws Exception {
        assertEncoded((byte) 10, "こんにちは", "ISO-2022-JP");
    }

    @Test
    public void testSendSms_dcs13_extendedKanji() throws Exception {
        // "Hi" triggers substitute bytes in JIS_X0212 (no ASCII mapping), which distinguishes it from a US-ASCII fallback.
        assertEncoded((byte) 13, "Hi", "JIS_X0212-1990");
    }

    @Test
    public void testSendSms_dcs14_korean() throws Exception {
        assertEncoded((byte) 14, "안녕하세요", "EUC-KR");
    }

    // Verifies the DCS -> charset mapping. The expected side mirrors the production charset, so it locks the mapping
    // but not the exact wire bytes; use assertEncodedBytes for charsets where a silent substitution could hide a bug.
    private void assertEncoded(byte dataCoding, String message, String expectedCharset) throws Exception {
        SubmitSM sent = captureSentPdu(dataCoding, message);
        assertArrayEquals(message.getBytes(expectedCharset), sent.getShortMessageData().getBuffer());
        assertEquals(dataCoding, sent.getDataCoding());
    }

    // Locks the exact bytes written into the SubmitSM PDU against a known-good sequence (independent of the production code).
    private void assertEncodedBytes(byte dataCoding, String message, byte[] expectedBytes) throws Exception {
        SubmitSM sent = captureSentPdu(dataCoding, message);
        assertArrayEquals(expectedBytes, sent.getShortMessageData().getBuffer());
        assertEquals(dataCoding, sent.getDataCoding());
    }

    private SubmitSM captureSentPdu(byte dataCoding, String message) throws Exception {
        when(smppSession.isOpened()).thenReturn(true);
        when(smppSession.submit(any())).thenReturn(new SubmitSMResp());
        setDefaultSmppConfig();
        smppConfig.setCodingScheme(dataCoding);

        smppSmsSender.sendSms("123545", message);

        ArgumentCaptor<SubmitSM> captor = ArgumentCaptor.forClass(SubmitSM.class);
        verify(smppSession).submit(captor.capture());
        return captor.getValue();
    }

    private void setDefaultSmppConfig() {
        smppConfig.setProtocolVersion("3.3");
        smppConfig.setHost("smpphost");
        smppConfig.setPort(5687);
        smppConfig.setSystemId("213131");
        smppConfig.setPassword("35125q");

        smppConfig.setSystemType("");
        smppConfig.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        smppConfig.setServiceType("");

        smppConfig.setSourceAddress("");
        smppConfig.setSourceTon((byte) 5);
        smppConfig.setSourceNpi((byte) 0);

        smppConfig.setDestinationTon((byte) 5);
        smppConfig.setDestinationNpi((byte) 0);

        smppConfig.setAddressRange("");
        smppConfig.setCodingScheme((byte) 0);
    }

}
