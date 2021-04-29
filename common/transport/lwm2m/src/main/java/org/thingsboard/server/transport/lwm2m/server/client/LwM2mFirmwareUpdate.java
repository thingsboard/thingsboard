/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;

import java.util.UUID;

@Data
public class LwM2mFirmwareUpdate {
    private volatile String clientFwVersion;
    private volatile String currentFwVersion;
    private volatile UUID currentFwId;
//    private Long fw_size;
//    private String fw_checksum_algorithm;
//    private String fw_checksum;


//    haredUpdated {
//        ts: 1619444153674
//        kv {
//            key: "fw_title"
//            type: STRING_V
//            string_v: "manufacture999"
//        }
//    }
//    sharedUpdated {
//        ts: 1619444153674
//        kv {
//            key: "fw_version"
//            type: STRING_V
//            string_v: "3.04"
//        }
//    }
//    sharedUpdated {
//        ts: 1619444153674
//        kv {
//            key: "fw_size"
//            type: LONG_V
//            long_v: 8781
//        }
//    }
//    sharedUpdated {
//        ts: 1619444153674
//        kv {
//            key: "fw_checksum_algorithm"
//            type: STRING_V
//            string_v: "sha256"
//        }
//    }
//    sharedUpdated {
//        ts: 1619444153674
//        kv {
//            key: "fw_checksum"
//            type: STRING_V
//            string_v: "93ba4e59aee372ea0a728b463d2604db928f0975a91caec5b564363b28e979c1"
//        }
//    }
}
