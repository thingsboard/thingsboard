/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.smpp.Data;

@Getter
@RequiredArgsConstructor
public enum SmppDataCoding {

    SMSC_DEFAULT_ALPHABET((byte) 0, Data.ENC_GSM7BIT),
    IA5((byte) 1, Data.ENC_ASCII),
    OCTET_UNSPECIFIED_2((byte) 2, null),
    LATIN1((byte) 3, Data.ENC_ISO8859_1),
    OCTET_UNSPECIFIED_4((byte) 4, null),
    JIS((byte) 5, null),
    CYRILLIC((byte) 6, null),
    LATIN_HEBREW((byte) 7,  null),
    UCS2((byte) 8, Data.ENC_UTF16_BE),
    PICTOGRAM((byte) 9, null),
    MUSIC_CODES((byte) 10, null),
    EXTENDED_KANJI((byte) 13, null),
    KOREAN((byte) 14, null);

    private final byte code;
    private final String encodingName;

    public static SmppDataCoding fromCode(byte code) {
        for (SmppDataCoding smppDataCoding : values()) {
            if (smppDataCoding.code == code) {
                return smppDataCoding;
            }
        }

        return null;
    }

}
