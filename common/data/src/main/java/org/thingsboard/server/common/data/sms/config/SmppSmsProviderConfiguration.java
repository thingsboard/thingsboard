package org.thingsboard.server.common.data.sms.config;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SmppSmsProviderConfiguration implements SmsProviderConfiguration {
    @ApiModelProperty(allowableValues = "3.3, 3.4")
    private String protocolVersion;

    private String host;
    private Integer port;

    private String systemId;
    private String password;

    @ApiModelProperty(required = false)
    private String systemType;
    @ApiModelProperty(value = "TX - Transmitter, RX - Receiver, TRX - Transciever. By default TX is used", required = false)
    private SmppBindType bindType;
    @ApiModelProperty(required = false)
    private String serviceType;

    @ApiModelProperty(required = false)
    private Byte ton;
    @ApiModelProperty(required = false)
    private Byte npi;
    @ApiModelProperty(required = false)
    private String sourceAddress;

    @ApiModelProperty(required = false)
    private Byte destinationTon;
    @ApiModelProperty(required = false)
    private Byte destinationNpi;
    @ApiModelProperty(required = false)
    private String addressRange;

    @ApiModelProperty(allowableValues = "0-10,13-14",
            value = "0 - SMSC Default Alphabet (ASCII for short and long code and to GSM for toll-free, used as default)\n" +
                    "1 - IA5 (ASCII for short and long code, Latin 9 for toll-free (ISO-8859-9))\n" +
                    "2 - Octet Unspecified (8-bit binary)\n" +
                    "3 - Latin 1 (ISO-8859-1)\n" +
                    "4 - Octet Unspecified (8-bit binary)\n" +
                    "5 - JIS (X 0208-1990)\n" +
                    "6 - Cyrillic (ISO-8859-5)\n" +
                    "7 - Latin/Hebrew (ISO-8859-8)\n" +
                    "8 - UCS2/UTF-16 (ISO/IEC-10646)\n" +
                    "9 - Pictogram Encoding\n" +
                    "10 - Music Codes (ISO-2022-JP)\n" +
                    "13 - Extended Kanji JIS (X 0212-1990)\n" +
                    "14 - Korean Graphic Character Set (KS C 5601/KS X 1001)", required = false)
    private Byte codingScheme;

    @Override
    public SmsProviderType getType() {
        return SmsProviderType.SMPP;
    }

    public enum SmppBindType {
        TX, RX, TRX
    }

}
