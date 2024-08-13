/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data.gateway.connector.modbus;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.List;

@Data
public class Version352OrBelowModbusConnectorConfiguration {

    @NotNull
    @Valid
    private Master master;

    @NotNull
    @Valid
    private Slave slave;

    private String logLevel;

    private String name;

    private Boolean enableRemoteLogging;

    private String id;

    private Boolean sendDataOnlyOnChange;

    @Data
    public static class Master {

        @Valid
        @NotNull
        private List<SlaveConfig> slaves;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = SerialSlaveConfig.class, name = "serial"),
                @JsonSubTypes.Type(value = SocketSlaveConfig.class, names = {"tcp", "udp"})
        })
        @Data
        public static class SlaveConfig {

            @NotBlank
            protected String type;

            @NotBlank
            protected String method;

            @Min(1)
            protected Integer timeout;

            protected String byteOrder;

            protected String wordOrder;

            protected Boolean retries;

            protected Boolean retryOnEmpty;

            protected Boolean retryOnInvalid;

            @Positive
            @NotNull
            protected Integer pollPeriod;

            @Positive
            protected Integer unitId;

            @NotBlank
            protected String deviceName;

            protected String deviceType;

            protected Boolean sendDataOnlyOnChange;

            @Positive
            protected Integer connectAttemptTimeMs;

            @Positive
            protected Integer connectAttemptCount;

            @Positive
            protected Integer waitAfterFailedAttemptsMs;

            @NotNull
            @Valid
            protected List<Attribute> attributes;

            @NotNull
            @Valid
            protected List<Timeseries> timeseries;

            @Valid
            protected List<AttributeUpdate> attributeUpdates;

            @Valid
            protected List<Rpc> rpc;

            @Data
            public static class Attribute {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(1)
                @Max(4)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Number multiplier;
                private Number divider;
            }

            @Data
            public static class Timeseries {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(1)
                @Max(4)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Number multiplier;
                private Number divider;
            }

            @Data
            public static class AttributeUpdate {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(5)
                @Max(16)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;
            }

            @Data
            public static class Rpc {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(1)
                @Max(16)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Number multiplier;

                private Number divider;
            }
        }

        @Data
        public static class SerialSlaveConfig extends SlaveConfig {

            @NotBlank
            private String port;

            @Positive
            private Integer baudrate;

            @Positive
            private Integer databits;

            @Positive
            private Integer stopbits;

            @Min(5)
            @Max(8)
            private Integer bytesize;

            private String parity;

            private Boolean strict;
        }

        @Data
        public static class SocketSlaveConfig extends SlaveConfig {

            @NotBlank
            private String host;

            @Min(1)
            @Max(65535)
            @NotNull
            private Integer port;
        }
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SerialSlave.class, name = "serial"),
            @JsonSubTypes.Type(value = SocketSlave.class, names = {"tcp", "udp"})
    })
    @Data
    public static class Slave {

        @NotBlank
        protected String type;

        @NotBlank
        protected String method;

        @NotBlank
        protected String deviceName;

        protected String deviceType;

        @Positive
        protected Integer pollPeriod;

        protected Boolean sendDataToThingsBoard;

        protected String byteOrder;

        protected String wordOrder;

        @NotNull
        @PositiveOrZero
        protected Integer unitId;

        @NotNull
        @Valid
        protected Values values;

        protected String vendorName;
        protected String productCode;
        protected String vendorUrl;
        protected String productName;
        protected String modelName;

        @Data
        public static class Values {

            @Valid
            private List<Registers> input_registers;

            @Valid
            private List<Registers> holding_registers;

            @Valid
            private List<Registers> coils_initializer;

            @Valid
            private List<Registers> discrete_inputs;
        }

        @Data
        public static class Registers {

            @Valid
            private List<Attribute> attributes;

            @Valid
            private List<Timeseries> timeseries;

            @Valid
            private List<AttributeUpdate> attributeUpdates;

            @Valid
            private List<Rpc> rpc;

            @Data
            public static class Attribute {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Number multiplier;
                private Number divider;

                private Object value;
            }

            @Data
            public static class Timeseries {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Number multiplier;
                private Number divider;

                private Object value;
            }

            @Data
            public static class AttributeUpdate {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(5)
                @Max(16)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Object value;
            }

            @Data
            public static class Rpc {

                @NotBlank
                private String type;

                @NotBlank
                private String tag;

                @Min(1)
                @Max(16)
                @NotNull
                private Integer functionCode;

                @Positive
                @NotNull
                private Integer objectsCount;

                @Positive
                @NotNull
                private Integer address;

                private Object value;
            }
        }
    }

    @Data
    public static class SocketSlave extends Slave {

        @NotBlank
        private String host;

        @Min(1)
        @Max(65535)
        @NotNull
        private Integer port;
    }

    @Data
    public static class SerialSlave extends Slave {

        @NotBlank
        private String port;

        @Positive
        private Integer baudrate;

        @Positive
        private Integer databits;

        @Positive
        private Integer stopbits;

        private String parity;

        private Boolean strict;
    }
}
