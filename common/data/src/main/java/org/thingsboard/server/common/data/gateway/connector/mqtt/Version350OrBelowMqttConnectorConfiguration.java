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
package org.thingsboard.server.common.data.gateway.connector.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.util.List;
import java.util.Map;

@Data
public class Version350OrBelowMqttConnectorConfiguration {

    @NotNull
    @Valid
    private Broker broker;

    @NotNull
    @Valid
    private List<Mapping> mapping;

    @Valid
    private List<ConnectRequest> connectRequests;

    @Valid
    private List<DisconnectRequest> disconnectRequests;

    @Valid
    private List<AttributeRequest> attributeRequests;

    @Valid
    private List<AttributeUpdate> attributeUpdates;

    @Valid
    private List<ServerSideRpc> serverSideRpc;

    private String logLevel;

    private String name;

    private Boolean enableRemoteLogging;

    private String id;

    private Boolean sendDataOnlyOnChange;

    @Data
    public static class Broker {

        private String name;

        @NotBlank
        private String host;

        @Positive
        @NotNull
        private Integer port;

        private String clientId;

        @NotNull
        @Min(3)
        @Max(5)
        private Integer version;

        @Positive
        @NotNull
        private int maxMessageNumberPerWorker;

        @Positive
        @NotNull
        private int maxNumberOfWorkers;

        private Boolean sendDataOnlyOnChange;

        @Valid
        private Security security;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = Broker.AnonymousSecurity.class, name = "anonymous"),
                @JsonSubTypes.Type(value = Broker.BasicSecurity.class, name = "basic"),
                @JsonSubTypes.Type(value = Broker.CertificatesSecurity.class, name = "tls")
        })
        @Data
        public static abstract class Security {
            @NotBlank
            protected String type;
        }

        @Data
        public static class AnonymousSecurity extends Security {
        }

        @Data
        public static class BasicSecurity extends Security {
            @NotBlank
            private String username;

            @NotBlank
            private String password;
        }

        @Data
        public static class CertificatesSecurity extends Security {
            @NotBlank
            private String caCert;

            @NotBlank
            private String cert;

            @NotBlank
            private String privateKey;
        }

    }

    @Data
    public static class Mapping {

        @NotBlank
        private String topicFilter;

        @NotNull(message = "configuration object with one of the following types " +
                "[\"json\", \"bytes\", \"custom\"] is required")
        @Valid
        private Converter converter;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = BasicConverter.class, name = "json"),
                @JsonSubTypes.Type(value = BytesConverter.class, name = "bytes"),
                @JsonSubTypes.Type(value = CustomConverter.class, name = "custom")
        })
        @Data
        public static abstract class Converter {

            @NotBlank
            private String type;
        }

        @Data
        public static class BasicConverter extends Converter {

            private String deviceNameJsonExpression;
            private String deviceNameTopicExpression;

            @AssertTrue(message = "deviceNameJsonExpression or deviceNameTopicExpression field should be present")
            private boolean isDeviceNameExpression() {
                return StringUtils.isNotBlank(deviceNameJsonExpression) || StringUtils.isNotBlank(deviceNameTopicExpression);
            }

            private String deviceTypeJsonExpression;
            private String deviceTypeTopicExpression;

            @AssertTrue(message = "deviceTypeJsonExpression or deviceTypeTopicExpression field should be present")
            private boolean isDeviceTypeExpression() {
                return StringUtils.isNotBlank(deviceTypeJsonExpression) || StringUtils.isNotBlank(deviceTypeTopicExpression);
            }

            private boolean sendDataOnlyOnChange;

            @Positive
            @NotNull
            private Long timeout;

            @NotNull
            @Valid
            private List<Attribute> attributes;

            @NotNull
            @Valid
            private List<Timeseries> timeseries;

            @Data
            public static class Attribute {
                private String type;
                private String key;
                private String value;
            }

            @Data
            public static class Timeseries {
                private String type;
                private String key;
                private String value;
            }
        }

        @Data
        public static class BytesConverter extends Converter {
            @NotBlank
            private String deviceNameExpression;
            @NotBlank
            private String deviceTypeExpression;

            private boolean sendDataOnlyOnChange;

            @Positive
            @NotNull
            private Long timeout;

            @NotNull
            @Valid
            private List<BasicConverter.Attribute> attributes;

            @NotNull
            @Valid
            private List<BasicConverter.Timeseries> timeseries;

            @Data
            public static class Attribute {
                private String type;
                private String key;
                private String value;
            }

            @Data
            public static class Timeseries {
                private String type;
                private String key;
                private String value;
            }
        }

        @Data
        public static class CustomConverter extends Converter {
            @NotBlank
            private String extension;

            private Boolean cached;

            @JsonProperty("extension-config")
            private Map<String, Object> extensionConfig;
        }

    }

    @Data
    public static class ConnectRequest {
        @NotBlank
        private String topicFilter;

        private String deviceNameJsonExpression;
        private String deviceNameTopicExpression;

        @AssertTrue(message = "deviceNameJsonExpression or deviceNameTopicExpression field should present")
        private boolean deviceNameExpression() {
            return StringUtils.isNotBlank(deviceNameJsonExpression) || StringUtils.isNotBlank(deviceNameTopicExpression);
        }

        private String deviceTypeJsonExpression;
        private String deviceTypeTopicExpression;
    }

    @Data
    public static class DisconnectRequest {
        @NotBlank
        private String topicFilter;

        private String deviceNameJsonExpression;
        private String deviceNameTopicExpression;

        @AssertTrue(message = "deviceNameJsonExpression or deviceNameTopicExpression field should present")
        private boolean deviceNameExpression() {
            return StringUtils.isNotBlank(deviceNameJsonExpression) || StringUtils.isNotBlank(deviceNameTopicExpression);
        }
    }

    @Data
    public static class AttributeRequest {
        private boolean retain;

        @NotBlank
        private String topicFilter;


        private String deviceNameJsonExpression;
        private String deviceNameTopicExpression;

        @AssertTrue(message = "deviceNameJsonExpression or deviceNameTopicExpression field should present")
        private boolean deviceNameExpression() {
            return StringUtils.isNotBlank(deviceNameJsonExpression) || StringUtils.isNotBlank(deviceNameTopicExpression);
        }

        @NotBlank
        private String attributeNameJsonExpression;

        @NotBlank
        private String topicExpression;

        @NotBlank
        private String valueExpression;
    }

    @Data
    public static class AttributeUpdate {

        private boolean retain;

        @NotBlank
        private String deviceNameFilter;

        @NotBlank
        private String attributeFilter;

        @NotBlank
        private String topicExpression;

        @NotBlank
        private String valueExpression;
    }

    @Data
    public static class ServerSideRpc {

        @NotBlank
        private String deviceNameFilter;

        @NotBlank
        private String methodFilter;

        @NotBlank
        private String requestTopicExpression;

        private String responseTopicExpression;
        private Long responseTimeout;

        @AssertTrue(message = "In case of two way RPC responseTopicExpression and responseTimeout - both required!")
        private boolean isValidTwoWayRpc() {
            return (responseTopicExpression == null && responseTimeout == null) ||
                    (StringUtils.isNotBlank(responseTopicExpression) && responseTimeout > 0);
        }

        @NotBlank
        private String valueExpression;
    }

}
