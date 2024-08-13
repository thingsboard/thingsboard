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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LatestMqttConnectorConfiguration {

    @NotNull
    @Valid
    private Broker broker;

    @NotNull
    @Valid
    private List<DataMapping> dataMapping;

    @NotNull
    @Valid
    private RequestsMapping requestsMapping;

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

        @Min(1)
        @Max(65535)
        @NotNull
        private Integer port;

        @NotBlank
        private String clientId;

        @Min(3)
        @Max(5)
        @NotNull
        private Integer version;

        @Positive
        @NotNull
        private Integer maxMessageNumberPerWorker;

        @Positive
        @NotNull
        private Integer maxNumberOfWorkers;

        private Boolean sendDataOnlyOnChange;

        @NotNull
        @Valid
        private Security security;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                defaultImpl = AnonymousSecurity.class,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = AnonymousSecurity.class, name = "anonymous"),
                @JsonSubTypes.Type(value = BasicSecurity.class, name = "basic"),
                @JsonSubTypes.Type(value = AccessTokenSecurity.class, name = "accessToken"),
                @JsonSubTypes.Type(value = CertificatesSecurity.class, name = "certificates")
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
        public static class AccessTokenSecurity extends Security {
            @NotBlank
            private String accessToken;
        }

        @Data
        public static class CertificatesSecurity extends Security {
            @NotBlank
            private String pathToCACert;

            @NotBlank
            private String pathToClientCert;

            @NotBlank
            private String pathToPrivateKey;
        }
    }

    @Data
    public static class DataMapping {
        @NotBlank
        private String topicFilter;

        @Min(0)
        @Max(2)
        @NotNull
        private Integer subscriptionQos;

        @NotNull
        @Valid
        private Converter converter;

        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = BasicConverter.class, names = {"json", "bytes"}),
                @JsonSubTypes.Type(value = CustomConverter.class, name = "custom")
        })

        @Data
        public static class Converter {
            @NotBlank
            private String type;
        }

        @Data
        public static class BasicConverter extends Converter {

            @NotNull
            @Valid
            private FullDeviceInfo deviceInfo;

            private boolean sendDataOnlyOnChange;

            @Positive
            private long timeout;

            @NotNull
            @Valid
            private List<Attribute> attributes;

            @NotNull
            @Valid
            private List<Timeseries> timeseries;

            @Data
            public static class Attribute {
                @NotBlank
                private String type;

                @NotBlank
                private String key;

                @NotBlank
                private String value;
            }

            @Data
            public static class Timeseries {
                @NotBlank
                private String type;

                @NotBlank
                private String key;

                @NotBlank
                private String value;
            }
        }

        @Data
        public static class CustomConverter extends Converter {
            @NotBlank
            private String extension;

            private Boolean cached;

            private Map<String, Object> extensionConfig;
        }
    }

    @Data
    public static class RequestsMapping {
        @NotNull
        @Valid
        private List<ConnectRequest> connectRequests;

        @NotNull
        @Valid
        private List<DisconnectRequest> disconnectRequests;

        @NotNull
        @Valid
        private List<AttributeRequest> attributeRequests;

        @NotNull
        @Valid
        private List<AttributeUpdate> attributeUpdates;

        @NotNull
        @Valid
        private List<ServerSideRpc> serverSideRpc;

        @Data
        public static class ConnectRequest {
            @NotBlank
            private String topicFilter;

            @NotNull
            @Valid
            private FullDeviceInfo deviceInfo;
        }

        @Data
        public static class DisconnectRequest {
            @NotBlank
            private String topicFilter;

            @NotNull
            @Valid
            private PartDeviceInfo deviceInfo;
        }

        @Data
        public static class AttributeRequest {
            private boolean retain;

            @NotBlank
            private String topicFilter;

            @NotNull
            @Valid
            private PartDeviceInfo deviceInfo;

            @NotBlank
            private String attributeNameExpressionSource;

            @NotBlank
            private String attributeNameExpression;

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


        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                visible = true,
                property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(value = OneWayServerSideRpc.class, name = "oneWay"),
                @JsonSubTypes.Type(value = TwoWayServerSideRpc.class, name = "twoWay"),
        })
        @Data
        public static class ServerSideRpc {
            @NotBlank
            private String type;

        }

        @Data
        public static class OneWayServerSideRpc extends ServerSideRpc {

            @NotBlank
            protected String deviceNameFilter;

            @NotBlank
            protected String methodFilter;

            @NotBlank
            protected String requestTopicExpression;

            @NotBlank
            protected String valueExpression;
        }

        @Data
        public static class TwoWayServerSideRpc extends OneWayServerSideRpc {

            @NotBlank
            private String responseTopicExpression;

            @Min(0)
            @Max(2)
            @NotNull
            private Integer responseTopicQoS;

            @Positive
            @NotNull
            private Long responseTimeout;
        }
    }

    @Data
    public static class PartDeviceInfo {
        @NotBlank
        protected String deviceNameExpressionSource;

        @NotBlank
        protected String deviceNameExpression;
    }

    @Data
    public static class FullDeviceInfo extends PartDeviceInfo {
        @NotBlank
        private String deviceProfileExpressionSource;

        @NotBlank
        private String deviceProfileExpression;
    }
}
