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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.smpp.Connection;
import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.TimeoutException;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.Address;
import org.smpp.pdu.BindReceiver;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.rule.engine.api.sms.exception.SmsSendException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class SmppSmsSender extends AbstractSmsSender {
    protected SmppSmsProviderConfiguration config;

    protected Session smppSession;

    public SmppSmsSender(SmppSmsProviderConfiguration config) {
        if (config.getBindType() == null) {
            config.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        }
        if (StringUtils.isNotEmpty(config.getSourceAddress())) {
            if (config.getSourceTon() == null) {
                config.setSourceTon((byte) 5);
            }
            if (config.getSourceNpi() == null) {
                config.setSourceNpi((byte) 0);
            }
        }
        if (config.getDestinationTon() == null) {
            config.setDestinationTon((byte) 5);
        }
        if (config.getDestinationNpi() == null) {
            config.setDestinationNpi((byte) 0);
        }

        this.config = config;
        this.smppSession = initSmppSession();
    }

    private SmppSmsSender() {} // for testing purposes


    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        try {
            checkSmppSession();

            SubmitSM request = new SubmitSM();
            if (StringUtils.isNotEmpty(config.getServiceType())) {
                request.setServiceType(config.getServiceType());
            }
            if (StringUtils.isNotEmpty(config.getSourceAddress())) {
                request.setSourceAddr(new Address(config.getSourceTon(), config.getSourceNpi(), config.getSourceAddress()));
            }
            request.setDestAddr(new Address(config.getDestinationTon(), config.getDestinationNpi(), prepareNumber(numberTo)));
            byte dataCoding = Optional.ofNullable(config.getCodingScheme()).orElse((byte) 0);
            request.setShortMessage(message, charsetFor(dataCoding));
            request.setDataCoding(dataCoding);
            request.setReplaceIfPresentFlag((byte) 0);
            request.setEsmClass((byte) 0);
            request.setProtocolId((byte) 0);
            request.setPriorityFlag((byte) 0);
            request.setRegisteredDelivery((byte) 0);
            request.setSmDefaultMsgId((byte) 0);

            SubmitSMResp response = smppSession.submit(request);

            log.debug("SMPP submit command status: {}", response.getCommandStatus());
        } catch (Exception e) {
            throw new SmsSendException("Failed to send SMS message - " + ExceptionUtils.getRootCauseMessage(e), e);
        }

        return countMessageSegments(message);
    }

    private static String charsetFor(byte dataCoding) {
        return switch (dataCoding) {
            case 0 -> Data.ENC_GSM7BIT; // SMSC Default Alphabet (GSM 7-bit) — opensmpp-charset SPI charset "X-Gsm7Bit"
            case 1 -> "US-ASCII"; // IA5 (CCITT T.50) / ASCII
            // DCS 2, 4 are "Octet unspecified" (binary) — ISO-8859-1 is bijective for bytes 0-255, so it passes bytes through unchanged.
            case 2 -> "ISO-8859-1"; // Octet unspecified (binary pass-through)
            case 3 -> "ISO-8859-1"; // Latin 1
            case 4 -> "ISO-8859-1"; // Octet unspecified (binary pass-through)
            case 5 -> "Shift_JIS"; // JIS
            case 6 -> "ISO-8859-5"; // Cyrillic
            case 7 -> "ISO-8859-8"; // Latin/Hebrew
            case 8 -> "UTF-16BE"; // UCS-2
            // FIXME: Pictogram encoding not natively supported, falling back to ISO-8859-1.
            case 9 -> "ISO-8859-1"; // Pictogram
            case 10 -> "ISO-2022-JP"; // Music Codes
            case 13 -> "JIS_X0212-1990"; // Extended Kanji JIS
            case 14 -> "EUC-KR"; // KS C 5601
            default -> {
                // Unknown/unsupported data_coding (the @Schema allowableValues are documentation only and not enforced,
                // so out-of-range values may be persisted). Fall back to a byte-transparent charset instead of failing the
                // send, since SMS backs 2FA and notifications. ISO-8859-1 is bijective for bytes 0-255 and never substitutes.
                log.warn("Unsupported SMPP data_coding {}, falling back to ISO-8859-1 encoding", dataCoding);
                yield "ISO-8859-1";
            }
        };
    }

    private synchronized void checkSmppSession() {
        if (smppSession == null || !smppSession.isOpened()) {
            smppSession = initSmppSession();
        }
    }

    protected Session initSmppSession() {
        try {
            Connection connection = new TCPIPConnection(config.getHost(), config.getPort());
            Session session = new Session(connection);

            BindRequest bindRequest;
            switch (config.getBindType()) {
                case TX:
                    bindRequest = new BindTransmitter();
                    break;
                case RX:
                    bindRequest = new BindReceiver();
                    break;
                case TRX:
                    bindRequest = new BindTransciever();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported bind type " + config.getBindType());
            }

            bindRequest.setSystemId(config.getSystemId());
            bindRequest.setPassword(config.getPassword());

            byte interfaceVersion;
            switch (config.getProtocolVersion()) {
                case "3.3":
                    interfaceVersion = Data.SMPP_V33;
                    break;
                case "3.4":
                    interfaceVersion = Data.SMPP_V34;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported SMPP version: " + config.getProtocolVersion());
            }
            bindRequest.setInterfaceVersion(interfaceVersion);

            if (StringUtils.isNotEmpty(config.getSystemType())) {
                bindRequest.setSystemType(config.getSystemType());
            }
            if (StringUtils.isNotEmpty(config.getAddressRange())) {
                bindRequest.setAddressRange(config.getDestinationTon(), config.getDestinationNpi(), config.getAddressRange());
            }

            BindResponse bindResponse = session.bind(bindRequest);
            log.debug("SMPP bind response: {}", bindResponse.debugString());

            if (bindResponse.getCommandStatus() != 0) {
                throw new IllegalStateException("Error status when binding: " + bindResponse.getCommandStatus());
            }

            return session;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to establish SMPP session: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    private String prepareNumber(String number) {
        if (config.getDestinationTon() == Data.GSM_TON_INTERNATIONAL) {
            return StringUtils.removeStart(number, "+");
        }
        return number;
    }

    @Override
    public void destroy() {
        try {
            smppSession.unbind();
            smppSession.close();
        } catch (TimeoutException | PDUException | IOException | WrongSessionStateException e) {
            throw new RuntimeException(e);
        }

    }
}
