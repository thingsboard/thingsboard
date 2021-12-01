package org.thingsboard.server.service.sms.smpp;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class SmppSmsSender extends AbstractSmsSender {
    private final SmppSmsProviderConfiguration config;

    private Session smppSession;

    public SmppSmsSender(SmppSmsProviderConfiguration config) {
        this.config = config;
    }

    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        try {
            checkSmppSession();

            SubmitSM request = new SubmitSM();
            if (StringUtils.isNotEmpty(config.getServiceType())) {
                request.setServiceType(config.getServiceType());
            }
            if (StringUtils.isNotEmpty(config.getSourceAddress())) {
                request.setSourceAddr(new Address(config.getTon(), config.getNpi(), config.getSourceAddress()));
            }
            numberTo = prepareNumber(numberTo);
            request.setDestAddr(new Address(config.getDestinationTon(), config.getDestinationNpi(), numberTo));
            request.setShortMessage(message);
            request.setDataCoding(Optional.ofNullable(config.getCodingScheme()).orElse((byte) 0));
            request.setReplaceIfPresentFlag((byte) 0);
            request.setEsmClass((byte) 0);
            request.setProtocolId((byte) 0);
            request.setPriorityFlag((byte) 0);
            request.setRegisteredDelivery((byte) 0);
            request.setSmDefaultMsgId((byte) 0);

            SubmitSMResp response = smppSession.submit(request);

            log.info("SMPP submit command status: {}", response.getCommandStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return countMessageSegments(message);
    }

    public synchronized void checkSmppSession() {
        if (smppSession == null || !smppSession.isOpened()) {
            smppSession = initSmppSession();
        }
    }

    private Session initSmppSession() {
        try {
            Connection connection = new TCPIPConnection(config.getHost(), config.getPort());
            Session session = new Session(connection);

            BindRequest bindRequest;
            if (config.getBindType() == null) {
                bindRequest = new BindTransmitter();
            } else {
                switch (config.getBindType()) {
                    case TX:
                        bindRequest =  new BindTransmitter();
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
            throw new IllegalArgumentException("Failed to establish SMPP session: " + ExceptionUtils.getRootCauseMessage(e));
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
