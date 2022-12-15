/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

/**
 * Created by nickAS21 on 13.12.22
 */
public final class SparkplugBProto {
    private SparkplugBProto() {}
    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
    }
    public interface PayloadOrBuilder extends
            // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload)
            com.google.protobuf.GeneratedMessage.
                    ExtendableMessageOrBuilder<Payload> {

        /**
         * <code>optional uint64 timestamp = 1;</code>
         *
         * <pre>
         * Timestamp at message sending time
         * </pre>
         */
        boolean hasTimestamp();
        /**
         * <code>optional uint64 timestamp = 1;</code>
         *
         * <pre>
         * Timestamp at message sending time
         * </pre>
         */
        long getTimestamp();

        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>
        getMetricsList();
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index);
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        int getMetricsCount();
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
        getMetricsOrBuilderList();
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                int index);

        /**
         * <code>optional uint64 seq = 3;</code>
         *
         * <pre>
         * Sequence number
         * </pre>
         */
        boolean hasSeq();
        /**
         * <code>optional uint64 seq = 3;</code>
         *
         * <pre>
         * Sequence number
         * </pre>
         */
        long getSeq();

        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        boolean hasUuid();
        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        java.lang.String getUuid();
        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        com.google.protobuf.ByteString
        getUuidBytes();

        /**
         * <code>optional bytes body = 5;</code>
         *
         * <pre>
         * To optionally bypass the whole definition above
         * </pre>
         */
        boolean hasBody();
        /**
         * <code>optional bytes body = 5;</code>
         *
         * <pre>
         * To optionally bypass the whole definition above
         * </pre>
         */
        com.google.protobuf.ByteString getBody();
    }
    /**
     * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload}
     *
     * <pre>
     * // Indexes of Data Types
     * // Unknown placeholder for future expansion.
     *Unknown         = 0;
     * // Basic Types
     *Int8            = 1;
     *Int16           = 2;
     *Int32           = 3;
     *Int64           = 4;
     *UInt8           = 5;
     *UInt16          = 6;
     *UInt32          = 7;
     *UInt64          = 8;
     *Float           = 9;
     *Double          = 10;
     *Boolean         = 11;
     *String          = 12;
     *DateTime        = 13;
     *Text            = 14;
     * // Additional Metric Types
     *UUID            = 15;
     *DataSet         = 16;
     *Bytes           = 17;
     *File            = 18;
     *Template        = 19;
     * // Additional PropertyValue Types
     *PropertySet     = 20;
     *PropertySetList = 21;
     * </pre>
     */
    public static final class Payload extends
            com.google.protobuf.GeneratedMessage.ExtendableMessage<
                    Payload> implements
            // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload)
            PayloadOrBuilder {
        // Use Payload.newBuilder() to construct.
        private Payload(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload, ?> builder) {
            super(builder);
            this.unknownFields = builder.getUnknownFields();
        }
        private Payload(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

        private static final Payload defaultInstance;
        public static Payload getDefaultInstance() {
            return defaultInstance;
        }

        public Payload getDefaultInstanceForType() {
            return defaultInstance;
        }

        private final com.google.protobuf.UnknownFieldSet unknownFields;
        @java.lang.Override
        public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
            return this.unknownFields;
        }
        private Payload(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            initFields();
            int mutable_bitField0_ = 0;
            com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                    com.google.protobuf.UnknownFieldSet.newBuilder();
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        default: {
                            if (!parseUnknownField(input, unknownFields,
                                    extensionRegistry, tag)) {
                                done = true;
                            }
                            break;
                        }
                        case 8: {
                            bitField0_ |= 0x00000001;
                            timestamp_ = input.readUInt64();
                            break;
                        }
                        case 18: {
                            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                                metrics_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>();
                                mutable_bitField0_ |= 0x00000002;
                            }
                            metrics_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.PARSER, extensionRegistry));
                            break;
                        }
                        case 24: {
                            bitField0_ |= 0x00000002;
                            seq_ = input.readUInt64();
                            break;
                        }
                        case 34: {
                            com.google.protobuf.ByteString bs = input.readBytes();
                            bitField0_ |= 0x00000004;
                            uuid_ = bs;
                            break;
                        }
                        case 42: {
                            bitField0_ |= 0x00000008;
                            body_ = input.readBytes();
                            break;
                        }
                    }
                }
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(this);
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(
                        e.getMessage()).setUnfinishedMessage(this);
            } finally {
                if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                    metrics_ = java.util.Collections.unmodifiableList(metrics_);
                }
                this.unknownFields = unknownFields.build();
                makeExtensionsImmutable();
            }
        }
        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor;
        }

        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Builder.class);
        }

        public static com.google.protobuf.Parser<Payload> PARSER =
                new com.google.protobuf.AbstractParser<Payload>() {
                    public Payload parsePartialFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return new Payload(input, extensionRegistry);
                    }
                };

        @java.lang.Override
        public com.google.protobuf.Parser<Payload> getParserForType() {
            return PARSER;
        }

        public interface TemplateOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template)
                com.google.protobuf.GeneratedMessage.
                        ExtendableMessageOrBuilder<Template> {

            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            boolean hasVersion();
            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            java.lang.String getVersion();
            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            com.google.protobuf.ByteString
            getVersionBytes();

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>
            getMetricsList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index);
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            int getMetricsCount();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
            getMetricsOrBuilderList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                    int index);

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter>
            getParametersList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter getParameters(int index);
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            int getParametersCount();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder>
            getParametersOrBuilderList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder getParametersOrBuilder(
                    int index);

            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            boolean hasTemplateRef();
            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            java.lang.String getTemplateRef();
            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            com.google.protobuf.ByteString
            getTemplateRefBytes();

            /**
             * <code>optional bool is_definition = 5;</code>
             */
            boolean hasIsDefinition();
            /**
             * <code>optional bool is_definition = 5;</code>
             */
            boolean getIsDefinition();
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template}
         */
        public static final class Template extends
                com.google.protobuf.GeneratedMessage.ExtendableMessage<
                        Template> implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template)
                TemplateOrBuilder {
            // Use Template.newBuilder() to construct.
            private Template(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template, ?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private Template(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final Template defaultInstance;
            public static Template getDefaultInstance() {
                return defaultInstance;
            }

            public Template getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private Template(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 10: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000001;
                                version_ = bs;
                                break;
                            }
                            case 18: {
                                if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                                    metrics_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>();
                                    mutable_bitField0_ |= 0x00000002;
                                }
                                metrics_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.PARSER, extensionRegistry));
                                break;
                            }
                            case 26: {
                                if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                                    parameters_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter>();
                                    mutable_bitField0_ |= 0x00000004;
                                }
                                parameters_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.PARSER, extensionRegistry));
                                break;
                            }
                            case 34: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000002;
                                templateRef_ = bs;
                                break;
                            }
                            case 40: {
                                bitField0_ |= 0x00000004;
                                isDefinition_ = input.readBool();
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                        metrics_ = java.util.Collections.unmodifiableList(metrics_);
                    }
                    if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                        parameters_ = java.util.Collections.unmodifiableList(parameters_);
                    }
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder.class);
            }

            public static com.google.protobuf.Parser<Template> PARSER =
                    new com.google.protobuf.AbstractParser<Template>() {
                        public Template parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new Template(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<Template> getParserForType() {
                return PARSER;
            }

            public interface ParameterOrBuilder extends
                    // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter)
                    com.google.protobuf.MessageOrBuilder {

                /**
                 * <code>optional string name = 1;</code>
                 */
                boolean hasName();
                /**
                 * <code>optional string name = 1;</code>
                 */
                java.lang.String getName();
                /**
                 * <code>optional string name = 1;</code>
                 */
                com.google.protobuf.ByteString
                getNameBytes();

                /**
                 * <code>optional uint32 type = 2;</code>
                 */
                boolean hasType();
                /**
                 * <code>optional uint32 type = 2;</code>
                 */
                int getType();

                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                boolean hasIntValue();
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                int getIntValue();

                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                boolean hasLongValue();
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                long getLongValue();

                /**
                 * <code>optional float float_value = 5;</code>
                 */
                boolean hasFloatValue();
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                float getFloatValue();

                /**
                 * <code>optional double double_value = 6;</code>
                 */
                boolean hasDoubleValue();
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                double getDoubleValue();

                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                boolean hasBooleanValue();
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                boolean getBooleanValue();

                /**
                 * <code>optional string string_value = 8;</code>
                 */
                boolean hasStringValue();
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                java.lang.String getStringValue();
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                com.google.protobuf.ByteString
                getStringValueBytes();

                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                boolean hasExtensionValue();
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension getExtensionValue();
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder getExtensionValueOrBuilder();
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter}
             */
            public static final class Parameter extends
                    com.google.protobuf.GeneratedMessage implements
                    // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter)
                    ParameterOrBuilder {
                // Use Parameter.newBuilder() to construct.
                private Parameter(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
                    super(builder);
                    this.unknownFields = builder.getUnknownFields();
                }
                private Parameter(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                private static final Parameter defaultInstance;
                public static Parameter getDefaultInstance() {
                    return defaultInstance;
                }

                public Parameter getDefaultInstanceForType() {
                    return defaultInstance;
                }

                private final com.google.protobuf.UnknownFieldSet unknownFields;
                @java.lang.Override
                public final com.google.protobuf.UnknownFieldSet
                getUnknownFields() {
                    return this.unknownFields;
                }
                private Parameter(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    initFields();
                    int mutable_bitField0_ = 0;
                    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                            com.google.protobuf.UnknownFieldSet.newBuilder();
                    try {
                        boolean done = false;
                        while (!done) {
                            int tag = input.readTag();
                            switch (tag) {
                                case 0:
                                    done = true;
                                    break;
                                default: {
                                    if (!parseUnknownField(input, unknownFields,
                                            extensionRegistry, tag)) {
                                        done = true;
                                    }
                                    break;
                                }
                                case 10: {
                                    com.google.protobuf.ByteString bs = input.readBytes();
                                    bitField0_ |= 0x00000001;
                                    name_ = bs;
                                    break;
                                }
                                case 16: {
                                    bitField0_ |= 0x00000002;
                                    type_ = input.readUInt32();
                                    break;
                                }
                                case 24: {
                                    valueCase_ = 3;
                                    value_ = input.readUInt32();
                                    break;
                                }
                                case 32: {
                                    valueCase_ = 4;
                                    value_ = input.readUInt64();
                                    break;
                                }
                                case 45: {
                                    valueCase_ = 5;
                                    value_ = input.readFloat();
                                    break;
                                }
                                case 49: {
                                    valueCase_ = 6;
                                    value_ = input.readDouble();
                                    break;
                                }
                                case 56: {
                                    valueCase_ = 7;
                                    value_ = input.readBool();
                                    break;
                                }
                                case 66: {
                                    com.google.protobuf.ByteString bs = input.readBytes();
                                    valueCase_ = 8;
                                    value_ = bs;
                                    break;
                                }
                                case 74: {
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder subBuilder = null;
                                    if (valueCase_ == 9) {
                                        subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_).toBuilder();
                                    }
                                    value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.PARSER, extensionRegistry);
                                    if (subBuilder != null) {
                                        subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_);
                                        value_ = subBuilder.buildPartial();
                                    }
                                    valueCase_ = 9;
                                    break;
                                }
                            }
                        }
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(this);
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(
                                e.getMessage()).setUnfinishedMessage(this);
                    } finally {
                        this.unknownFields = unknownFields.build();
                        makeExtensionsImmutable();
                    }
                }
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder.class);
                }

                public static com.google.protobuf.Parser<Parameter> PARSER =
                        new com.google.protobuf.AbstractParser<Parameter>() {
                            public Parameter parsePartialFrom(
                                    com.google.protobuf.CodedInputStream input,
                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                    throws com.google.protobuf.InvalidProtocolBufferException {
                                return new Parameter(input, extensionRegistry);
                            }
                        };

                @java.lang.Override
                public com.google.protobuf.Parser<Parameter> getParserForType() {
                    return PARSER;
                }

                public interface ParameterValueExtensionOrBuilder extends
                        // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension)
                        com.google.protobuf.GeneratedMessage.
                                ExtendableMessageOrBuilder<ParameterValueExtension> {
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension}
                 */
                public static final class ParameterValueExtension extends
                        com.google.protobuf.GeneratedMessage.ExtendableMessage<
                                ParameterValueExtension> implements
                        // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension)
                        ParameterValueExtensionOrBuilder {
                    // Use ParameterValueExtension.newBuilder() to construct.
                    private ParameterValueExtension(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension, ?> builder) {
                        super(builder);
                        this.unknownFields = builder.getUnknownFields();
                    }
                    private ParameterValueExtension(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                    private static final ParameterValueExtension defaultInstance;
                    public static ParameterValueExtension getDefaultInstance() {
                        return defaultInstance;
                    }

                    public ParameterValueExtension getDefaultInstanceForType() {
                        return defaultInstance;
                    }

                    private final com.google.protobuf.UnknownFieldSet unknownFields;
                    @java.lang.Override
                    public final com.google.protobuf.UnknownFieldSet
                    getUnknownFields() {
                        return this.unknownFields;
                    }
                    private ParameterValueExtension(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        initFields();
                        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                                com.google.protobuf.UnknownFieldSet.newBuilder();
                        try {
                            boolean done = false;
                            while (!done) {
                                int tag = input.readTag();
                                switch (tag) {
                                    case 0:
                                        done = true;
                                        break;
                                    default: {
                                        if (!parseUnknownField(input, unknownFields,
                                                extensionRegistry, tag)) {
                                            done = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            throw e.setUnfinishedMessage(this);
                        } catch (java.io.IOException e) {
                            throw new com.google.protobuf.InvalidProtocolBufferException(
                                    e.getMessage()).setUnfinishedMessage(this);
                        } finally {
                            this.unknownFields = unknownFields.build();
                            makeExtensionsImmutable();
                        }
                    }
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder.class);
                    }

                    public static com.google.protobuf.Parser<ParameterValueExtension> PARSER =
                            new com.google.protobuf.AbstractParser<ParameterValueExtension>() {
                                public ParameterValueExtension parsePartialFrom(
                                        com.google.protobuf.CodedInputStream input,
                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                        throws com.google.protobuf.InvalidProtocolBufferException {
                                    return new ParameterValueExtension(input, extensionRegistry);
                                }
                            };

                    @java.lang.Override
                    public com.google.protobuf.Parser<ParameterValueExtension> getParserForType() {
                        return PARSER;
                    }

                    private void initFields() {
                    }
                    private byte memoizedIsInitialized = -1;
                    public final boolean isInitialized() {
                        byte isInitialized = memoizedIsInitialized;
                        if (isInitialized == 1) return true;
                        if (isInitialized == 0) return false;

                        if (!extensionsAreInitialized()) {
                            memoizedIsInitialized = 0;
                            return false;
                        }
                        memoizedIsInitialized = 1;
                        return true;
                    }

                    public void writeTo(com.google.protobuf.CodedOutputStream output)
                            throws java.io.IOException {
                        getSerializedSize();
                        com.google.protobuf.GeneratedMessage
                                .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension>.ExtensionWriter extensionWriter =
                                newExtensionWriter();
                        extensionWriter.writeUntil(536870912, output);
                        getUnknownFields().writeTo(output);
                    }

                    private int memoizedSerializedSize = -1;
                    public int getSerializedSize() {
                        int size = memoizedSerializedSize;
                        if (size != -1) return size;

                        size = 0;
                        size += extensionsSerializedSize();
                        size += getUnknownFields().getSerializedSize();
                        memoizedSerializedSize = size;
                        return size;
                    }

                    private static final long serialVersionUID = 0L;
                    @java.lang.Override
                    protected java.lang.Object writeReplace()
                            throws java.io.ObjectStreamException {
                        return super.writeReplace();
                    }

                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            com.google.protobuf.ByteString data)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            com.google.protobuf.ByteString data,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(byte[] data)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            byte[] data,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(java.io.InputStream input)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            java.io.InputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseDelimitedFrom(java.io.InputStream input)
                            throws java.io.IOException {
                        return PARSER.parseDelimitedFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseDelimitedFrom(
                            java.io.InputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseDelimitedFrom(input, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            com.google.protobuf.CodedInputStream input)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parseFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input, extensionRegistry);
                    }

                    public static Builder newBuilder() { return Builder.create(); }
                    public Builder newBuilderForType() { return newBuilder(); }
                    public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension prototype) {
                        return newBuilder().mergeFrom(prototype);
                    }
                    public Builder toBuilder() { return newBuilder(this); }

                    @java.lang.Override
                    protected Builder newBuilderForType(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        Builder builder = new Builder(parent);
                        return builder;
                    }
                    /**
                     * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension}
                     */
                    public static final class Builder extends
                            com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension, Builder> implements
                            // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension)
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder {
                        public static final com.google.protobuf.Descriptors.Descriptor
                        getDescriptor() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor;
                        }

                        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                        internalGetFieldAccessorTable() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_fieldAccessorTable
                                    .ensureFieldAccessorsInitialized(
                                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder.class);
                        }

                        // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.newBuilder()
                        private Builder() {
                            maybeForceBuilderInitialization();
                        }

                        private Builder(
                                com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                            super(parent);
                            maybeForceBuilderInitialization();
                        }
                        private void maybeForceBuilderInitialization() {
                            if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                            }
                        }
                        private static Builder create() {
                            return new Builder();
                        }

                        public Builder clear() {
                            super.clear();
                            return this;
                        }

                        public Builder clone() {
                            return create().mergeFrom(buildPartial());
                        }

                        public com.google.protobuf.Descriptors.Descriptor
                        getDescriptorForType() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor;
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension getDefaultInstanceForType() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension build() {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension result = buildPartial();
                            if (!result.isInitialized()) {
                                throw newUninitializedMessageException(result);
                            }
                            return result;
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension buildPartial() {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension(this);
                            onBuilt();
                            return result;
                        }

                        public Builder mergeFrom(com.google.protobuf.Message other) {
                            if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) {
                                return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension)other);
                            } else {
                                super.mergeFrom(other);
                                return this;
                            }
                        }

                        public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension other) {
                            if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance()) return this;
                            this.mergeExtensionFields(other);
                            this.mergeUnknownFields(other.getUnknownFields());
                            return this;
                        }

                        public final boolean isInitialized() {
                            if (!extensionsAreInitialized()) {

                                return false;
                            }
                            return true;
                        }

                        public Builder mergeFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws java.io.IOException {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension parsedMessage = null;
                            try {
                                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                                parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) e.getUnfinishedMessage();
                                throw e;
                            } finally {
                                if (parsedMessage != null) {
                                    mergeFrom(parsedMessage);
                                }
                            }
                            return this;
                        }

                        // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension)
                    }

                    static {
                        defaultInstance = new ParameterValueExtension(true);
                        defaultInstance.initFields();
                    }

                    // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension)
                }

                private int bitField0_;
                private int valueCase_ = 0;
                private java.lang.Object value_;
                public enum ValueCase
                        implements com.google.protobuf.Internal.EnumLite {
                    INT_VALUE(3),
                    LONG_VALUE(4),
                    FLOAT_VALUE(5),
                    DOUBLE_VALUE(6),
                    BOOLEAN_VALUE(7),
                    STRING_VALUE(8),
                    EXTENSION_VALUE(9),
                    VALUE_NOT_SET(0);
                    private int value = 0;
                    private ValueCase(int value) {
                        this.value = value;
                    }
                    public static ValueCase valueOf(int value) {
                        switch (value) {
                            case 3: return INT_VALUE;
                            case 4: return LONG_VALUE;
                            case 5: return FLOAT_VALUE;
                            case 6: return DOUBLE_VALUE;
                            case 7: return BOOLEAN_VALUE;
                            case 8: return STRING_VALUE;
                            case 9: return EXTENSION_VALUE;
                            case 0: return VALUE_NOT_SET;
                            default: throw new java.lang.IllegalArgumentException(
                                    "Value is undefined for this oneof enum.");
                        }
                    }
                    public int getNumber() {
                        return this.value;
                    }
                };

                public ValueCase
                getValueCase() {
                    return ValueCase.valueOf(
                            valueCase_);
                }

                public static final int NAME_FIELD_NUMBER = 1;
                private java.lang.Object name_;
                /**
                 * <code>optional string name = 1;</code>
                 */
                public boolean hasName() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional string name = 1;</code>
                 */
                public java.lang.String getName() {
                    java.lang.Object ref = name_;
                    if (ref instanceof java.lang.String) {
                        return (java.lang.String) ref;
                    } else {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            name_ = s;
                        }
                        return s;
                    }
                }
                /**
                 * <code>optional string name = 1;</code>
                 */
                public com.google.protobuf.ByteString
                getNameBytes() {
                    java.lang.Object ref = name_;
                    if (ref instanceof java.lang.String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        name_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }

                public static final int TYPE_FIELD_NUMBER = 2;
                private int type_;
                /**
                 * <code>optional uint32 type = 2;</code>
                 */
                public boolean hasType() {
                    return ((bitField0_ & 0x00000002) == 0x00000002);
                }
                /**
                 * <code>optional uint32 type = 2;</code>
                 */
                public int getType() {
                    return type_;
                }

                public static final int INT_VALUE_FIELD_NUMBER = 3;
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public boolean hasIntValue() {
                    return valueCase_ == 3;
                }
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public int getIntValue() {
                    if (valueCase_ == 3) {
                        return (java.lang.Integer) value_;
                    }
                    return 0;
                }

                public static final int LONG_VALUE_FIELD_NUMBER = 4;
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public boolean hasLongValue() {
                    return valueCase_ == 4;
                }
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public long getLongValue() {
                    if (valueCase_ == 4) {
                        return (java.lang.Long) value_;
                    }
                    return 0L;
                }

                public static final int FLOAT_VALUE_FIELD_NUMBER = 5;
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public boolean hasFloatValue() {
                    return valueCase_ == 5;
                }
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public float getFloatValue() {
                    if (valueCase_ == 5) {
                        return (java.lang.Float) value_;
                    }
                    return 0F;
                }

                public static final int DOUBLE_VALUE_FIELD_NUMBER = 6;
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public boolean hasDoubleValue() {
                    return valueCase_ == 6;
                }
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public double getDoubleValue() {
                    if (valueCase_ == 6) {
                        return (java.lang.Double) value_;
                    }
                    return 0D;
                }

                public static final int BOOLEAN_VALUE_FIELD_NUMBER = 7;
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public boolean hasBooleanValue() {
                    return valueCase_ == 7;
                }
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public boolean getBooleanValue() {
                    if (valueCase_ == 7) {
                        return (java.lang.Boolean) value_;
                    }
                    return false;
                }

                public static final int STRING_VALUE_FIELD_NUMBER = 8;
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public boolean hasStringValue() {
                    return valueCase_ == 8;
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public java.lang.String getStringValue() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 8) {
                        ref = value_;
                    }
                    if (ref instanceof java.lang.String) {
                        return (java.lang.String) ref;
                    } else {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8() && (valueCase_ == 8)) {
                            value_ = s;
                        }
                        return s;
                    }
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public com.google.protobuf.ByteString
                getStringValueBytes() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 8) {
                        ref = value_;
                    }
                    if (ref instanceof java.lang.String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        if (valueCase_ == 8) {
                            value_ = b;
                        }
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }

                public static final int EXTENSION_VALUE_FIELD_NUMBER = 9;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                public boolean hasExtensionValue() {
                    return valueCase_ == 9;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension getExtensionValue() {
                    if (valueCase_ == 9) {
                        return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_;
                    }
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder getExtensionValueOrBuilder() {
                    if (valueCase_ == 9) {
                        return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_;
                    }
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                }

                private void initFields() {
                    name_ = "";
                    type_ = 0;
                }
                private byte memoizedIsInitialized = -1;
                public final boolean isInitialized() {
                    byte isInitialized = memoizedIsInitialized;
                    if (isInitialized == 1) return true;
                    if (isInitialized == 0) return false;

                    if (hasExtensionValue()) {
                        if (!getExtensionValue().isInitialized()) {
                            memoizedIsInitialized = 0;
                            return false;
                        }
                    }
                    memoizedIsInitialized = 1;
                    return true;
                }

                public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
                    getSerializedSize();
                    if (((bitField0_ & 0x00000001) == 0x00000001)) {
                        output.writeBytes(1, getNameBytes());
                    }
                    if (((bitField0_ & 0x00000002) == 0x00000002)) {
                        output.writeUInt32(2, type_);
                    }
                    if (valueCase_ == 3) {
                        output.writeUInt32(
                                3, (int)((java.lang.Integer) value_));
                    }
                    if (valueCase_ == 4) {
                        output.writeUInt64(
                                4, (long)((java.lang.Long) value_));
                    }
                    if (valueCase_ == 5) {
                        output.writeFloat(
                                5, (float)((java.lang.Float) value_));
                    }
                    if (valueCase_ == 6) {
                        output.writeDouble(
                                6, (double)((java.lang.Double) value_));
                    }
                    if (valueCase_ == 7) {
                        output.writeBool(
                                7, (boolean)((java.lang.Boolean) value_));
                    }
                    if (valueCase_ == 8) {
                        output.writeBytes(8, getStringValueBytes());
                    }
                    if (valueCase_ == 9) {
                        output.writeMessage(9, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_);
                    }
                    getUnknownFields().writeTo(output);
                }

                private int memoizedSerializedSize = -1;
                public int getSerializedSize() {
                    int size = memoizedSerializedSize;
                    if (size != -1) return size;

                    size = 0;
                    if (((bitField0_ & 0x00000001) == 0x00000001)) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeBytesSize(1, getNameBytes());
                    }
                    if (((bitField0_ & 0x00000002) == 0x00000002)) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeUInt32Size(2, type_);
                    }
                    if (valueCase_ == 3) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeUInt32Size(
                                        3, (int)((java.lang.Integer) value_));
                    }
                    if (valueCase_ == 4) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeUInt64Size(
                                        4, (long)((java.lang.Long) value_));
                    }
                    if (valueCase_ == 5) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeFloatSize(
                                        5, (float)((java.lang.Float) value_));
                    }
                    if (valueCase_ == 6) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeDoubleSize(
                                        6, (double)((java.lang.Double) value_));
                    }
                    if (valueCase_ == 7) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeBoolSize(
                                        7, (boolean)((java.lang.Boolean) value_));
                    }
                    if (valueCase_ == 8) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeBytesSize(8, getStringValueBytes());
                    }
                    if (valueCase_ == 9) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeMessageSize(9, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_);
                    }
                    size += getUnknownFields().getSerializedSize();
                    memoizedSerializedSize = size;
                    return size;
                }

                private static final long serialVersionUID = 0L;
                @java.lang.Override
                protected java.lang.Object writeReplace()
                        throws java.io.ObjectStreamException {
                    return super.writeReplace();
                }

                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        com.google.protobuf.ByteString data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        com.google.protobuf.ByteString data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(byte[] data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        byte[] data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseDelimitedFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseDelimitedFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        com.google.protobuf.CodedInputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parseFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }

                public static Builder newBuilder() { return Builder.create(); }
                public Builder newBuilderForType() { return newBuilder(); }
                public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter prototype) {
                    return newBuilder().mergeFrom(prototype);
                }
                public Builder toBuilder() { return newBuilder(this); }

                @java.lang.Override
                protected Builder newBuilderForType(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    Builder builder = new Builder(parent);
                    return builder;
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter}
                 */
                public static final class Builder extends
                        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
                        // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter)
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder {
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder.class);
                    }

                    // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.newBuilder()
                    private Builder() {
                        maybeForceBuilderInitialization();
                    }

                    private Builder(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        super(parent);
                        maybeForceBuilderInitialization();
                    }
                    private void maybeForceBuilderInitialization() {
                        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        }
                    }
                    private static Builder create() {
                        return new Builder();
                    }

                    public Builder clear() {
                        super.clear();
                        name_ = "";
                        bitField0_ = (bitField0_ & ~0x00000001);
                        type_ = 0;
                        bitField0_ = (bitField0_ & ~0x00000002);
                        valueCase_ = 0;
                        value_ = null;
                        return this;
                    }

                    public Builder clone() {
                        return create().mergeFrom(buildPartial());
                    }

                    public com.google.protobuf.Descriptors.Descriptor
                    getDescriptorForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter getDefaultInstanceForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.getDefaultInstance();
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter build() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter result = buildPartial();
                        if (!result.isInitialized()) {
                            throw newUninitializedMessageException(result);
                        }
                        return result;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter buildPartial() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter(this);
                        int from_bitField0_ = bitField0_;
                        int to_bitField0_ = 0;
                        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                            to_bitField0_ |= 0x00000001;
                        }
                        result.name_ = name_;
                        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
                            to_bitField0_ |= 0x00000002;
                        }
                        result.type_ = type_;
                        if (valueCase_ == 3) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 4) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 5) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 6) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 7) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 8) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 9) {
                            if (extensionValueBuilder_ == null) {
                                result.value_ = value_;
                            } else {
                                result.value_ = extensionValueBuilder_.build();
                            }
                        }
                        result.bitField0_ = to_bitField0_;
                        result.valueCase_ = valueCase_;
                        onBuilt();
                        return result;
                    }

                    public Builder mergeFrom(com.google.protobuf.Message other) {
                        if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter) {
                            return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter)other);
                        } else {
                            super.mergeFrom(other);
                            return this;
                        }
                    }

                    public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter other) {
                        if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.getDefaultInstance()) return this;
                        if (other.hasName()) {
                            bitField0_ |= 0x00000001;
                            name_ = other.name_;
                            onChanged();
                        }
                        if (other.hasType()) {
                            setType(other.getType());
                        }
                        switch (other.getValueCase()) {
                            case INT_VALUE: {
                                setIntValue(other.getIntValue());
                                break;
                            }
                            case LONG_VALUE: {
                                setLongValue(other.getLongValue());
                                break;
                            }
                            case FLOAT_VALUE: {
                                setFloatValue(other.getFloatValue());
                                break;
                            }
                            case DOUBLE_VALUE: {
                                setDoubleValue(other.getDoubleValue());
                                break;
                            }
                            case BOOLEAN_VALUE: {
                                setBooleanValue(other.getBooleanValue());
                                break;
                            }
                            case STRING_VALUE: {
                                valueCase_ = 8;
                                value_ = other.value_;
                                onChanged();
                                break;
                            }
                            case EXTENSION_VALUE: {
                                mergeExtensionValue(other.getExtensionValue());
                                break;
                            }
                            case VALUE_NOT_SET: {
                                break;
                            }
                        }
                        this.mergeUnknownFields(other.getUnknownFields());
                        return this;
                    }

                    public final boolean isInitialized() {
                        if (hasExtensionValue()) {
                            if (!getExtensionValue().isInitialized()) {

                                return false;
                            }
                        }
                        return true;
                    }

                    public Builder mergeFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter parsedMessage = null;
                        try {
                            parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter) e.getUnfinishedMessage();
                            throw e;
                        } finally {
                            if (parsedMessage != null) {
                                mergeFrom(parsedMessage);
                            }
                        }
                        return this;
                    }
                    private int valueCase_ = 0;
                    private java.lang.Object value_;
                    public ValueCase
                    getValueCase() {
                        return ValueCase.valueOf(
                                valueCase_);
                    }

                    public Builder clearValue() {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                        return this;
                    }

                    private int bitField0_;

                    private java.lang.Object name_ = "";
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public boolean hasName() {
                        return ((bitField0_ & 0x00000001) == 0x00000001);
                    }
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public java.lang.String getName() {
                        java.lang.Object ref = name_;
                        if (!(ref instanceof java.lang.String)) {
                            com.google.protobuf.ByteString bs =
                                    (com.google.protobuf.ByteString) ref;
                            java.lang.String s = bs.toStringUtf8();
                            if (bs.isValidUtf8()) {
                                name_ = s;
                            }
                            return s;
                        } else {
                            return (java.lang.String) ref;
                        }
                    }
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public com.google.protobuf.ByteString
                    getNameBytes() {
                        java.lang.Object ref = name_;
                        if (ref instanceof String) {
                            com.google.protobuf.ByteString b =
                                    com.google.protobuf.ByteString.copyFromUtf8(
                                            (java.lang.String) ref);
                            name_ = b;
                            return b;
                        } else {
                            return (com.google.protobuf.ByteString) ref;
                        }
                    }
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public Builder setName(
                            java.lang.String value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        bitField0_ |= 0x00000001;
                        name_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public Builder clearName() {
                        bitField0_ = (bitField0_ & ~0x00000001);
                        name_ = getDefaultInstance().getName();
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional string name = 1;</code>
                     */
                    public Builder setNameBytes(
                            com.google.protobuf.ByteString value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        bitField0_ |= 0x00000001;
                        name_ = value;
                        onChanged();
                        return this;
                    }

                    private int type_ ;
                    /**
                     * <code>optional uint32 type = 2;</code>
                     */
                    public boolean hasType() {
                        return ((bitField0_ & 0x00000002) == 0x00000002);
                    }
                    /**
                     * <code>optional uint32 type = 2;</code>
                     */
                    public int getType() {
                        return type_;
                    }
                    /**
                     * <code>optional uint32 type = 2;</code>
                     */
                    public Builder setType(int value) {
                        bitField0_ |= 0x00000002;
                        type_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional uint32 type = 2;</code>
                     */
                    public Builder clearType() {
                        bitField0_ = (bitField0_ & ~0x00000002);
                        type_ = 0;
                        onChanged();
                        return this;
                    }

                    /**
                     * <code>optional uint32 int_value = 3;</code>
                     */
                    public boolean hasIntValue() {
                        return valueCase_ == 3;
                    }
                    /**
                     * <code>optional uint32 int_value = 3;</code>
                     */
                    public int getIntValue() {
                        if (valueCase_ == 3) {
                            return (java.lang.Integer) value_;
                        }
                        return 0;
                    }
                    /**
                     * <code>optional uint32 int_value = 3;</code>
                     */
                    public Builder setIntValue(int value) {
                        valueCase_ = 3;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional uint32 int_value = 3;</code>
                     */
                    public Builder clearIntValue() {
                        if (valueCase_ == 3) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional uint64 long_value = 4;</code>
                     */
                    public boolean hasLongValue() {
                        return valueCase_ == 4;
                    }
                    /**
                     * <code>optional uint64 long_value = 4;</code>
                     */
                    public long getLongValue() {
                        if (valueCase_ == 4) {
                            return (java.lang.Long) value_;
                        }
                        return 0L;
                    }
                    /**
                     * <code>optional uint64 long_value = 4;</code>
                     */
                    public Builder setLongValue(long value) {
                        valueCase_ = 4;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional uint64 long_value = 4;</code>
                     */
                    public Builder clearLongValue() {
                        if (valueCase_ == 4) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional float float_value = 5;</code>
                     */
                    public boolean hasFloatValue() {
                        return valueCase_ == 5;
                    }
                    /**
                     * <code>optional float float_value = 5;</code>
                     */
                    public float getFloatValue() {
                        if (valueCase_ == 5) {
                            return (java.lang.Float) value_;
                        }
                        return 0F;
                    }
                    /**
                     * <code>optional float float_value = 5;</code>
                     */
                    public Builder setFloatValue(float value) {
                        valueCase_ = 5;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional float float_value = 5;</code>
                     */
                    public Builder clearFloatValue() {
                        if (valueCase_ == 5) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional double double_value = 6;</code>
                     */
                    public boolean hasDoubleValue() {
                        return valueCase_ == 6;
                    }
                    /**
                     * <code>optional double double_value = 6;</code>
                     */
                    public double getDoubleValue() {
                        if (valueCase_ == 6) {
                            return (java.lang.Double) value_;
                        }
                        return 0D;
                    }
                    /**
                     * <code>optional double double_value = 6;</code>
                     */
                    public Builder setDoubleValue(double value) {
                        valueCase_ = 6;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional double double_value = 6;</code>
                     */
                    public Builder clearDoubleValue() {
                        if (valueCase_ == 6) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional bool boolean_value = 7;</code>
                     */
                    public boolean hasBooleanValue() {
                        return valueCase_ == 7;
                    }
                    /**
                     * <code>optional bool boolean_value = 7;</code>
                     */
                    public boolean getBooleanValue() {
                        if (valueCase_ == 7) {
                            return (java.lang.Boolean) value_;
                        }
                        return false;
                    }
                    /**
                     * <code>optional bool boolean_value = 7;</code>
                     */
                    public Builder setBooleanValue(boolean value) {
                        valueCase_ = 7;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional bool boolean_value = 7;</code>
                     */
                    public Builder clearBooleanValue() {
                        if (valueCase_ == 7) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public boolean hasStringValue() {
                        return valueCase_ == 8;
                    }
                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public java.lang.String getStringValue() {
                        java.lang.Object ref = "";
                        if (valueCase_ == 8) {
                            ref = value_;
                        }
                        if (!(ref instanceof java.lang.String)) {
                            com.google.protobuf.ByteString bs =
                                    (com.google.protobuf.ByteString) ref;
                            java.lang.String s = bs.toStringUtf8();
                            if (valueCase_ == 8) {
                                if (bs.isValidUtf8()) {
                                    value_ = s;
                                }
                            }
                            return s;
                        } else {
                            return (java.lang.String) ref;
                        }
                    }
                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public com.google.protobuf.ByteString
                    getStringValueBytes() {
                        java.lang.Object ref = "";
                        if (valueCase_ == 8) {
                            ref = value_;
                        }
                        if (ref instanceof String) {
                            com.google.protobuf.ByteString b =
                                    com.google.protobuf.ByteString.copyFromUtf8(
                                            (java.lang.String) ref);
                            if (valueCase_ == 8) {
                                value_ = b;
                            }
                            return b;
                        } else {
                            return (com.google.protobuf.ByteString) ref;
                        }
                    }
                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public Builder setStringValue(
                            java.lang.String value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        valueCase_ = 8;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public Builder clearStringValue() {
                        if (valueCase_ == 8) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }
                    /**
                     * <code>optional string string_value = 8;</code>
                     */
                    public Builder setStringValueBytes(
                            com.google.protobuf.ByteString value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        valueCase_ = 8;
                        value_ = value;
                        onChanged();
                        return this;
                    }

                    private com.google.protobuf.SingleFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder> extensionValueBuilder_;
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public boolean hasExtensionValue() {
                        return valueCase_ == 9;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension getExtensionValue() {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 9) {
                                return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_;
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                        } else {
                            if (valueCase_ == 9) {
                                return extensionValueBuilder_.getMessage();
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                        }
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public Builder setExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension value) {
                        if (extensionValueBuilder_ == null) {
                            if (value == null) {
                                throw new NullPointerException();
                            }
                            value_ = value;
                            onChanged();
                        } else {
                            extensionValueBuilder_.setMessage(value);
                        }
                        valueCase_ = 9;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public Builder setExtensionValue(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder builderForValue) {
                        if (extensionValueBuilder_ == null) {
                            value_ = builderForValue.build();
                            onChanged();
                        } else {
                            extensionValueBuilder_.setMessage(builderForValue.build());
                        }
                        valueCase_ = 9;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public Builder mergeExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension value) {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 9 &&
                                    value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance()) {
                                value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_)
                                        .mergeFrom(value).buildPartial();
                            } else {
                                value_ = value;
                            }
                            onChanged();
                        } else {
                            if (valueCase_ == 9) {
                                extensionValueBuilder_.mergeFrom(value);
                            }
                            extensionValueBuilder_.setMessage(value);
                        }
                        valueCase_ = 9;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public Builder clearExtensionValue() {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 9) {
                                valueCase_ = 0;
                                value_ = null;
                                onChanged();
                            }
                        } else {
                            if (valueCase_ == 9) {
                                valueCase_ = 0;
                                value_ = null;
                            }
                            extensionValueBuilder_.clear();
                        }
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder getExtensionValueBuilder() {
                        return getExtensionValueFieldBuilder().getBuilder();
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder getExtensionValueOrBuilder() {
                        if ((valueCase_ == 9) && (extensionValueBuilder_ != null)) {
                            return extensionValueBuilder_.getMessageOrBuilder();
                        } else {
                            if (valueCase_ == 9) {
                                return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_;
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                        }
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter.ParameterValueExtension extension_value = 9;</code>
                     */
                    private com.google.protobuf.SingleFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder>
                    getExtensionValueFieldBuilder() {
                        if (extensionValueBuilder_ == null) {
                            if (!(valueCase_ == 9)) {
                                value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.getDefaultInstance();
                            }
                            extensionValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtensionOrBuilder>(
                                    (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.ParameterValueExtension) value_,
                                    getParentForChildren(),
                                    isClean());
                            value_ = null;
                        }
                        valueCase_ = 9;
                        return extensionValueBuilder_;
                    }

                    // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter)
                }

                static {
                    defaultInstance = new Parameter(true);
                    defaultInstance.initFields();
                }

                // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter)
            }

            private int bitField0_;
            public static final int VERSION_FIELD_NUMBER = 1;
            private java.lang.Object version_;
            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            public boolean hasVersion() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            public java.lang.String getVersion() {
                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        version_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string version = 1;</code>
             *
             * <pre>
             * The version of the Template to prevent mismatches
             * </pre>
             */
            public com.google.protobuf.ByteString
            getVersionBytes() {
                java.lang.Object ref = version_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    version_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int METRICS_FIELD_NUMBER = 2;
            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> metrics_;
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> getMetricsList() {
                return metrics_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
            getMetricsOrBuilderList() {
                return metrics_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            public int getMetricsCount() {
                return metrics_.size();
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index) {
                return metrics_.get(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Each metric is the name of the metric and the datatype of the member but does not contain a value
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                    int index) {
                return metrics_.get(index);
            }

            public static final int PARAMETERS_FIELD_NUMBER = 3;
            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter> parameters_;
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter> getParametersList() {
                return parameters_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder>
            getParametersOrBuilderList() {
                return parameters_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            public int getParametersCount() {
                return parameters_.size();
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter getParameters(int index) {
                return parameters_.get(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder getParametersOrBuilder(
                    int index) {
                return parameters_.get(index);
            }

            public static final int TEMPLATE_REF_FIELD_NUMBER = 4;
            private java.lang.Object templateRef_;
            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            public boolean hasTemplateRef() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }
            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            public java.lang.String getTemplateRef() {
                java.lang.Object ref = templateRef_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        templateRef_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string template_ref = 4;</code>
             *
             * <pre>
             * Reference to a template if this is extending a Template or an instance - must exist if an instance
             * </pre>
             */
            public com.google.protobuf.ByteString
            getTemplateRefBytes() {
                java.lang.Object ref = templateRef_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    templateRef_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int IS_DEFINITION_FIELD_NUMBER = 5;
            private boolean isDefinition_;
            /**
             * <code>optional bool is_definition = 5;</code>
             */
            public boolean hasIsDefinition() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }
            /**
             * <code>optional bool is_definition = 5;</code>
             */
            public boolean getIsDefinition() {
                return isDefinition_;
            }

            private void initFields() {
                version_ = "";
                metrics_ = java.util.Collections.emptyList();
                parameters_ = java.util.Collections.emptyList();
                templateRef_ = "";
                isDefinition_ = false;
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                for (int i = 0; i < getMetricsCount(); i++) {
                    if (!getMetrics(i).isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                for (int i = 0; i < getParametersCount(); i++) {
                    if (!getParameters(i).isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (!extensionsAreInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                com.google.protobuf.GeneratedMessage
                        .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template>.ExtensionWriter extensionWriter =
                        newExtensionWriter();
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    output.writeBytes(1, getVersionBytes());
                }
                for (int i = 0; i < metrics_.size(); i++) {
                    output.writeMessage(2, metrics_.get(i));
                }
                for (int i = 0; i < parameters_.size(); i++) {
                    output.writeMessage(3, parameters_.get(i));
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    output.writeBytes(4, getTemplateRefBytes());
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    output.writeBool(5, isDefinition_);
                }
                extensionWriter.writeUntil(536870912, output);
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(1, getVersionBytes());
                }
                for (int i = 0; i < metrics_.size(); i++) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(2, metrics_.get(i));
                }
                for (int i = 0; i < parameters_.size(); i++) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(3, parameters_.get(i));
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(4, getTemplateRefBytes());
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(5, isDefinition_);
                }
                size += extensionsSerializedSize();
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template, Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        getMetricsFieldBuilder();
                        getParametersFieldBuilder();
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    version_ = "";
                    bitField0_ = (bitField0_ & ~0x00000001);
                    if (metricsBuilder_ == null) {
                        metrics_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000002);
                    } else {
                        metricsBuilder_.clear();
                    }
                    if (parametersBuilder_ == null) {
                        parameters_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000004);
                    } else {
                        parametersBuilder_.clear();
                    }
                    templateRef_ = "";
                    bitField0_ = (bitField0_ & ~0x00000008);
                    isDefinition_ = false;
                    bitField0_ = (bitField0_ & ~0x00000010);
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template(this);
                    int from_bitField0_ = bitField0_;
                    int to_bitField0_ = 0;
                    if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                        to_bitField0_ |= 0x00000001;
                    }
                    result.version_ = version_;
                    if (metricsBuilder_ == null) {
                        if (((bitField0_ & 0x00000002) == 0x00000002)) {
                            metrics_ = java.util.Collections.unmodifiableList(metrics_);
                            bitField0_ = (bitField0_ & ~0x00000002);
                        }
                        result.metrics_ = metrics_;
                    } else {
                        result.metrics_ = metricsBuilder_.build();
                    }
                    if (parametersBuilder_ == null) {
                        if (((bitField0_ & 0x00000004) == 0x00000004)) {
                            parameters_ = java.util.Collections.unmodifiableList(parameters_);
                            bitField0_ = (bitField0_ & ~0x00000004);
                        }
                        result.parameters_ = parameters_;
                    } else {
                        result.parameters_ = parametersBuilder_.build();
                    }
                    if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
                        to_bitField0_ |= 0x00000002;
                    }
                    result.templateRef_ = templateRef_;
                    if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
                        to_bitField0_ |= 0x00000004;
                    }
                    result.isDefinition_ = isDefinition_;
                    result.bitField0_ = to_bitField0_;
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance()) return this;
                    if (other.hasVersion()) {
                        bitField0_ |= 0x00000001;
                        version_ = other.version_;
                        onChanged();
                    }
                    if (metricsBuilder_ == null) {
                        if (!other.metrics_.isEmpty()) {
                            if (metrics_.isEmpty()) {
                                metrics_ = other.metrics_;
                                bitField0_ = (bitField0_ & ~0x00000002);
                            } else {
                                ensureMetricsIsMutable();
                                metrics_.addAll(other.metrics_);
                            }
                            onChanged();
                        }
                    } else {
                        if (!other.metrics_.isEmpty()) {
                            if (metricsBuilder_.isEmpty()) {
                                metricsBuilder_.dispose();
                                metricsBuilder_ = null;
                                metrics_ = other.metrics_;
                                bitField0_ = (bitField0_ & ~0x00000002);
                                metricsBuilder_ =
                                        com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                getMetricsFieldBuilder() : null;
                            } else {
                                metricsBuilder_.addAllMessages(other.metrics_);
                            }
                        }
                    }
                    if (parametersBuilder_ == null) {
                        if (!other.parameters_.isEmpty()) {
                            if (parameters_.isEmpty()) {
                                parameters_ = other.parameters_;
                                bitField0_ = (bitField0_ & ~0x00000004);
                            } else {
                                ensureParametersIsMutable();
                                parameters_.addAll(other.parameters_);
                            }
                            onChanged();
                        }
                    } else {
                        if (!other.parameters_.isEmpty()) {
                            if (parametersBuilder_.isEmpty()) {
                                parametersBuilder_.dispose();
                                parametersBuilder_ = null;
                                parameters_ = other.parameters_;
                                bitField0_ = (bitField0_ & ~0x00000004);
                                parametersBuilder_ =
                                        com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                getParametersFieldBuilder() : null;
                            } else {
                                parametersBuilder_.addAllMessages(other.parameters_);
                            }
                        }
                    }
                    if (other.hasTemplateRef()) {
                        bitField0_ |= 0x00000008;
                        templateRef_ = other.templateRef_;
                        onChanged();
                    }
                    if (other.hasIsDefinition()) {
                        setIsDefinition(other.getIsDefinition());
                    }
                    this.mergeExtensionFields(other);
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    for (int i = 0; i < getMetricsCount(); i++) {
                        if (!getMetrics(i).isInitialized()) {

                            return false;
                        }
                    }
                    for (int i = 0; i < getParametersCount(); i++) {
                        if (!getParameters(i).isInitialized()) {

                            return false;
                        }
                    }
                    if (!extensionsAreInitialized()) {

                        return false;
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int bitField0_;

                private java.lang.Object version_ = "";
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public boolean hasVersion() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public java.lang.String getVersion() {
                    java.lang.Object ref = version_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            version_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getVersionBytes() {
                    java.lang.Object ref = version_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        version_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public Builder setVersion(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000001;
                    version_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public Builder clearVersion() {
                    bitField0_ = (bitField0_ & ~0x00000001);
                    version_ = getDefaultInstance().getVersion();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string version = 1;</code>
                 *
                 * <pre>
                 * The version of the Template to prevent mismatches
                 * </pre>
                 */
                public Builder setVersionBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000001;
                    version_ = value;
                    onChanged();
                    return this;
                }

                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> metrics_ =
                        java.util.Collections.emptyList();
                private void ensureMetricsIsMutable() {
                    if (!((bitField0_ & 0x00000002) == 0x00000002)) {
                        metrics_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>(metrics_);
                        bitField0_ |= 0x00000002;
                    }
                }

                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder> metricsBuilder_;

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> getMetricsList() {
                    if (metricsBuilder_ == null) {
                        return java.util.Collections.unmodifiableList(metrics_);
                    } else {
                        return metricsBuilder_.getMessageList();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public int getMetricsCount() {
                    if (metricsBuilder_ == null) {
                        return metrics_.size();
                    } else {
                        return metricsBuilder_.getCount();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index) {
                    if (metricsBuilder_ == null) {
                        return metrics_.get(index);
                    } else {
                        return metricsBuilder_.getMessage(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder setMetrics(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                    if (metricsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureMetricsIsMutable();
                        metrics_.set(index, value);
                        onChanged();
                    } else {
                        metricsBuilder_.setMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder setMetrics(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                    if (metricsBuilder_ == null) {
                        ensureMetricsIsMutable();
                        metrics_.set(index, builderForValue.build());
                        onChanged();
                    } else {
                        metricsBuilder_.setMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder addMetrics(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                    if (metricsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureMetricsIsMutable();
                        metrics_.add(value);
                        onChanged();
                    } else {
                        metricsBuilder_.addMessage(value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder addMetrics(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                    if (metricsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureMetricsIsMutable();
                        metrics_.add(index, value);
                        onChanged();
                    } else {
                        metricsBuilder_.addMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder addMetrics(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                    if (metricsBuilder_ == null) {
                        ensureMetricsIsMutable();
                        metrics_.add(builderForValue.build());
                        onChanged();
                    } else {
                        metricsBuilder_.addMessage(builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder addMetrics(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                    if (metricsBuilder_ == null) {
                        ensureMetricsIsMutable();
                        metrics_.add(index, builderForValue.build());
                        onChanged();
                    } else {
                        metricsBuilder_.addMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder addAllMetrics(
                        java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> values) {
                    if (metricsBuilder_ == null) {
                        ensureMetricsIsMutable();
                        com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                values, metrics_);
                        onChanged();
                    } else {
                        metricsBuilder_.addAllMessages(values);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder clearMetrics() {
                    if (metricsBuilder_ == null) {
                        metrics_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000002);
                        onChanged();
                    } else {
                        metricsBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public Builder removeMetrics(int index) {
                    if (metricsBuilder_ == null) {
                        ensureMetricsIsMutable();
                        metrics_.remove(index);
                        onChanged();
                    } else {
                        metricsBuilder_.remove(index);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder getMetricsBuilder(
                        int index) {
                    return getMetricsFieldBuilder().getBuilder(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                        int index) {
                    if (metricsBuilder_ == null) {
                        return metrics_.get(index);  } else {
                        return metricsBuilder_.getMessageOrBuilder(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
                getMetricsOrBuilderList() {
                    if (metricsBuilder_ != null) {
                        return metricsBuilder_.getMessageOrBuilderList();
                    } else {
                        return java.util.Collections.unmodifiableList(metrics_);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder addMetricsBuilder() {
                    return getMetricsFieldBuilder().addBuilder(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder addMetricsBuilder(
                        int index) {
                    return getMetricsFieldBuilder().addBuilder(
                            index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
                 *
                 * <pre>
                 * Each metric is the name of the metric and the datatype of the member but does not contain a value
                 * </pre>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder>
                getMetricsBuilderList() {
                    return getMetricsFieldBuilder().getBuilderList();
                }
                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
                getMetricsFieldBuilder() {
                    if (metricsBuilder_ == null) {
                        metricsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>(
                                metrics_,
                                ((bitField0_ & 0x00000002) == 0x00000002),
                                getParentForChildren(),
                                isClean());
                        metrics_ = null;
                    }
                    return metricsBuilder_;
                }

                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter> parameters_ =
                        java.util.Collections.emptyList();
                private void ensureParametersIsMutable() {
                    if (!((bitField0_ & 0x00000004) == 0x00000004)) {
                        parameters_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter>(parameters_);
                        bitField0_ |= 0x00000004;
                    }
                }

                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder> parametersBuilder_;

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter> getParametersList() {
                    if (parametersBuilder_ == null) {
                        return java.util.Collections.unmodifiableList(parameters_);
                    } else {
                        return parametersBuilder_.getMessageList();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public int getParametersCount() {
                    if (parametersBuilder_ == null) {
                        return parameters_.size();
                    } else {
                        return parametersBuilder_.getCount();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter getParameters(int index) {
                    if (parametersBuilder_ == null) {
                        return parameters_.get(index);
                    } else {
                        return parametersBuilder_.getMessage(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder setParameters(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter value) {
                    if (parametersBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureParametersIsMutable();
                        parameters_.set(index, value);
                        onChanged();
                    } else {
                        parametersBuilder_.setMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder setParameters(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder builderForValue) {
                    if (parametersBuilder_ == null) {
                        ensureParametersIsMutable();
                        parameters_.set(index, builderForValue.build());
                        onChanged();
                    } else {
                        parametersBuilder_.setMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder addParameters(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter value) {
                    if (parametersBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureParametersIsMutable();
                        parameters_.add(value);
                        onChanged();
                    } else {
                        parametersBuilder_.addMessage(value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder addParameters(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter value) {
                    if (parametersBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureParametersIsMutable();
                        parameters_.add(index, value);
                        onChanged();
                    } else {
                        parametersBuilder_.addMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder addParameters(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder builderForValue) {
                    if (parametersBuilder_ == null) {
                        ensureParametersIsMutable();
                        parameters_.add(builderForValue.build());
                        onChanged();
                    } else {
                        parametersBuilder_.addMessage(builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder addParameters(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder builderForValue) {
                    if (parametersBuilder_ == null) {
                        ensureParametersIsMutable();
                        parameters_.add(index, builderForValue.build());
                        onChanged();
                    } else {
                        parametersBuilder_.addMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder addAllParameters(
                        java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter> values) {
                    if (parametersBuilder_ == null) {
                        ensureParametersIsMutable();
                        com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                values, parameters_);
                        onChanged();
                    } else {
                        parametersBuilder_.addAllMessages(values);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder clearParameters() {
                    if (parametersBuilder_ == null) {
                        parameters_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000004);
                        onChanged();
                    } else {
                        parametersBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public Builder removeParameters(int index) {
                    if (parametersBuilder_ == null) {
                        ensureParametersIsMutable();
                        parameters_.remove(index);
                        onChanged();
                    } else {
                        parametersBuilder_.remove(index);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder getParametersBuilder(
                        int index) {
                    return getParametersFieldBuilder().getBuilder(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder getParametersOrBuilder(
                        int index) {
                    if (parametersBuilder_ == null) {
                        return parameters_.get(index);  } else {
                        return parametersBuilder_.getMessageOrBuilder(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder>
                getParametersOrBuilderList() {
                    if (parametersBuilder_ != null) {
                        return parametersBuilder_.getMessageOrBuilderList();
                    } else {
                        return java.util.Collections.unmodifiableList(parameters_);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder addParametersBuilder() {
                    return getParametersFieldBuilder().addBuilder(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder addParametersBuilder(
                        int index) {
                    return getParametersFieldBuilder().addBuilder(
                            index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template.Parameter parameters = 3;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder>
                getParametersBuilderList() {
                    return getParametersFieldBuilder().getBuilderList();
                }
                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder>
                getParametersFieldBuilder() {
                    if (parametersBuilder_ == null) {
                        parametersBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Parameter.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.ParameterOrBuilder>(
                                parameters_,
                                ((bitField0_ & 0x00000004) == 0x00000004),
                                getParentForChildren(),
                                isClean());
                        parameters_ = null;
                    }
                    return parametersBuilder_;
                }

                private java.lang.Object templateRef_ = "";
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public boolean hasTemplateRef() {
                    return ((bitField0_ & 0x00000008) == 0x00000008);
                }
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public java.lang.String getTemplateRef() {
                    java.lang.Object ref = templateRef_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            templateRef_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getTemplateRefBytes() {
                    java.lang.Object ref = templateRef_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        templateRef_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public Builder setTemplateRef(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000008;
                    templateRef_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public Builder clearTemplateRef() {
                    bitField0_ = (bitField0_ & ~0x00000008);
                    templateRef_ = getDefaultInstance().getTemplateRef();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string template_ref = 4;</code>
                 *
                 * <pre>
                 * Reference to a template if this is extending a Template or an instance - must exist if an instance
                 * </pre>
                 */
                public Builder setTemplateRefBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000008;
                    templateRef_ = value;
                    onChanged();
                    return this;
                }

                private boolean isDefinition_ ;
                /**
                 * <code>optional bool is_definition = 5;</code>
                 */
                public boolean hasIsDefinition() {
                    return ((bitField0_ & 0x00000010) == 0x00000010);
                }
                /**
                 * <code>optional bool is_definition = 5;</code>
                 */
                public boolean getIsDefinition() {
                    return isDefinition_;
                }
                /**
                 * <code>optional bool is_definition = 5;</code>
                 */
                public Builder setIsDefinition(boolean value) {
                    bitField0_ |= 0x00000010;
                    isDefinition_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_definition = 5;</code>
                 */
                public Builder clearIsDefinition() {
                    bitField0_ = (bitField0_ & ~0x00000010);
                    isDefinition_ = false;
                    onChanged();
                    return this;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template)
            }

            static {
                defaultInstance = new Template(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template)
        }

        public interface DataSetOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet)
                com.google.protobuf.GeneratedMessage.
                        ExtendableMessageOrBuilder<DataSet> {

            /**
             * <code>optional uint64 num_of_columns = 1;</code>
             */
            boolean hasNumOfColumns();
            /**
             * <code>optional uint64 num_of_columns = 1;</code>
             */
            long getNumOfColumns();

            /**
             * <code>repeated string columns = 2;</code>
             */
            com.google.protobuf.ProtocolStringList
            getColumnsList();
            /**
             * <code>repeated string columns = 2;</code>
             */
            int getColumnsCount();
            /**
             * <code>repeated string columns = 2;</code>
             */
            java.lang.String getColumns(int index);
            /**
             * <code>repeated string columns = 2;</code>
             */
            com.google.protobuf.ByteString
            getColumnsBytes(int index);

            /**
             * <code>repeated uint32 types = 3;</code>
             */
            java.util.List<java.lang.Integer> getTypesList();
            /**
             * <code>repeated uint32 types = 3;</code>
             */
            int getTypesCount();
            /**
             * <code>repeated uint32 types = 3;</code>
             */
            int getTypes(int index);

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row>
            getRowsList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row getRows(int index);
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            int getRowsCount();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder>
            getRowsOrBuilderList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder getRowsOrBuilder(
                    int index);
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet}
         */
        public static final class DataSet extends
                com.google.protobuf.GeneratedMessage.ExtendableMessage<
                        DataSet> implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet)
                DataSetOrBuilder {
            // Use DataSet.newBuilder() to construct.
            private DataSet(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet, ?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private DataSet(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final DataSet defaultInstance;
            public static DataSet getDefaultInstance() {
                return defaultInstance;
            }

            public DataSet getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private DataSet(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 8: {
                                bitField0_ |= 0x00000001;
                                numOfColumns_ = input.readUInt64();
                                break;
                            }
                            case 18: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                                    columns_ = new com.google.protobuf.LazyStringArrayList();
                                    mutable_bitField0_ |= 0x00000002;
                                }
                                columns_.add(bs);
                                break;
                            }
                            case 24: {
                                if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                                    types_ = new java.util.ArrayList<java.lang.Integer>();
                                    mutable_bitField0_ |= 0x00000004;
                                }
                                types_.add(input.readUInt32());
                                break;
                            }
                            case 26: {
                                int length = input.readRawVarint32();
                                int limit = input.pushLimit(length);
                                if (!((mutable_bitField0_ & 0x00000004) == 0x00000004) && input.getBytesUntilLimit() > 0) {
                                    types_ = new java.util.ArrayList<java.lang.Integer>();
                                    mutable_bitField0_ |= 0x00000004;
                                }
                                while (input.getBytesUntilLimit() > 0) {
                                    types_.add(input.readUInt32());
                                }
                                input.popLimit(limit);
                                break;
                            }
                            case 34: {
                                if (!((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
                                    rows_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row>();
                                    mutable_bitField0_ |= 0x00000008;
                                }
                                rows_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.PARSER, extensionRegistry));
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                        columns_ = columns_.getUnmodifiableView();
                    }
                    if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                        types_ = java.util.Collections.unmodifiableList(types_);
                    }
                    if (((mutable_bitField0_ & 0x00000008) == 0x00000008)) {
                        rows_ = java.util.Collections.unmodifiableList(rows_);
                    }
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder.class);
            }

            public static com.google.protobuf.Parser<DataSet> PARSER =
                    new com.google.protobuf.AbstractParser<DataSet>() {
                        public DataSet parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new DataSet(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<DataSet> getParserForType() {
                return PARSER;
            }

            public interface DataSetValueOrBuilder extends
                    // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue)
                    com.google.protobuf.MessageOrBuilder {

                /**
                 * <code>optional uint32 int_value = 1;</code>
                 */
                boolean hasIntValue();
                /**
                 * <code>optional uint32 int_value = 1;</code>
                 */
                int getIntValue();

                /**
                 * <code>optional uint64 long_value = 2;</code>
                 */
                boolean hasLongValue();
                /**
                 * <code>optional uint64 long_value = 2;</code>
                 */
                long getLongValue();

                /**
                 * <code>optional float float_value = 3;</code>
                 */
                boolean hasFloatValue();
                /**
                 * <code>optional float float_value = 3;</code>
                 */
                float getFloatValue();

                /**
                 * <code>optional double double_value = 4;</code>
                 */
                boolean hasDoubleValue();
                /**
                 * <code>optional double double_value = 4;</code>
                 */
                double getDoubleValue();

                /**
                 * <code>optional bool boolean_value = 5;</code>
                 */
                boolean hasBooleanValue();
                /**
                 * <code>optional bool boolean_value = 5;</code>
                 */
                boolean getBooleanValue();

                /**
                 * <code>optional string string_value = 6;</code>
                 */
                boolean hasStringValue();
                /**
                 * <code>optional string string_value = 6;</code>
                 */
                java.lang.String getStringValue();
                /**
                 * <code>optional string string_value = 6;</code>
                 */
                com.google.protobuf.ByteString
                getStringValueBytes();

                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                boolean hasExtensionValue();
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension getExtensionValue();
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder getExtensionValueOrBuilder();
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue}
             */
            public static final class DataSetValue extends
                    com.google.protobuf.GeneratedMessage implements
                    // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue)
                    DataSetValueOrBuilder {
                // Use DataSetValue.newBuilder() to construct.
                private DataSetValue(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
                    super(builder);
                    this.unknownFields = builder.getUnknownFields();
                }
                private DataSetValue(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                private static final DataSetValue defaultInstance;
                public static DataSetValue getDefaultInstance() {
                    return defaultInstance;
                }

                public DataSetValue getDefaultInstanceForType() {
                    return defaultInstance;
                }

                private final com.google.protobuf.UnknownFieldSet unknownFields;
                @java.lang.Override
                public final com.google.protobuf.UnknownFieldSet
                getUnknownFields() {
                    return this.unknownFields;
                }
                private DataSetValue(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    initFields();
                    int mutable_bitField0_ = 0;
                    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                            com.google.protobuf.UnknownFieldSet.newBuilder();
                    try {
                        boolean done = false;
                        while (!done) {
                            int tag = input.readTag();
                            switch (tag) {
                                case 0:
                                    done = true;
                                    break;
                                default: {
                                    if (!parseUnknownField(input, unknownFields,
                                            extensionRegistry, tag)) {
                                        done = true;
                                    }
                                    break;
                                }
                                case 8: {
                                    valueCase_ = 1;
                                    value_ = input.readUInt32();
                                    break;
                                }
                                case 16: {
                                    valueCase_ = 2;
                                    value_ = input.readUInt64();
                                    break;
                                }
                                case 29: {
                                    valueCase_ = 3;
                                    value_ = input.readFloat();
                                    break;
                                }
                                case 33: {
                                    valueCase_ = 4;
                                    value_ = input.readDouble();
                                    break;
                                }
                                case 40: {
                                    valueCase_ = 5;
                                    value_ = input.readBool();
                                    break;
                                }
                                case 50: {
                                    com.google.protobuf.ByteString bs = input.readBytes();
                                    valueCase_ = 6;
                                    value_ = bs;
                                    break;
                                }
                                case 58: {
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder subBuilder = null;
                                    if (valueCase_ == 7) {
                                        subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_).toBuilder();
                                    }
                                    value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.PARSER, extensionRegistry);
                                    if (subBuilder != null) {
                                        subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_);
                                        value_ = subBuilder.buildPartial();
                                    }
                                    valueCase_ = 7;
                                    break;
                                }
                            }
                        }
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(this);
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(
                                e.getMessage()).setUnfinishedMessage(this);
                    } finally {
                        this.unknownFields = unknownFields.build();
                        makeExtensionsImmutable();
                    }
                }
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder.class);
                }

                public static com.google.protobuf.Parser<DataSetValue> PARSER =
                        new com.google.protobuf.AbstractParser<DataSetValue>() {
                            public DataSetValue parsePartialFrom(
                                    com.google.protobuf.CodedInputStream input,
                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                    throws com.google.protobuf.InvalidProtocolBufferException {
                                return new DataSetValue(input, extensionRegistry);
                            }
                        };

                @java.lang.Override
                public com.google.protobuf.Parser<DataSetValue> getParserForType() {
                    return PARSER;
                }

                public interface DataSetValueExtensionOrBuilder extends
                        // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension)
                        com.google.protobuf.GeneratedMessage.
                                ExtendableMessageOrBuilder<DataSetValueExtension> {
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension}
                 */
                public static final class DataSetValueExtension extends
                        com.google.protobuf.GeneratedMessage.ExtendableMessage<
                                DataSetValueExtension> implements
                        // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension)
                        DataSetValueExtensionOrBuilder {
                    // Use DataSetValueExtension.newBuilder() to construct.
                    private DataSetValueExtension(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension, ?> builder) {
                        super(builder);
                        this.unknownFields = builder.getUnknownFields();
                    }
                    private DataSetValueExtension(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                    private static final DataSetValueExtension defaultInstance;
                    public static DataSetValueExtension getDefaultInstance() {
                        return defaultInstance;
                    }

                    public DataSetValueExtension getDefaultInstanceForType() {
                        return defaultInstance;
                    }

                    private final com.google.protobuf.UnknownFieldSet unknownFields;
                    @java.lang.Override
                    public final com.google.protobuf.UnknownFieldSet
                    getUnknownFields() {
                        return this.unknownFields;
                    }
                    private DataSetValueExtension(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        initFields();
                        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                                com.google.protobuf.UnknownFieldSet.newBuilder();
                        try {
                            boolean done = false;
                            while (!done) {
                                int tag = input.readTag();
                                switch (tag) {
                                    case 0:
                                        done = true;
                                        break;
                                    default: {
                                        if (!parseUnknownField(input, unknownFields,
                                                extensionRegistry, tag)) {
                                            done = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            throw e.setUnfinishedMessage(this);
                        } catch (java.io.IOException e) {
                            throw new com.google.protobuf.InvalidProtocolBufferException(
                                    e.getMessage()).setUnfinishedMessage(this);
                        } finally {
                            this.unknownFields = unknownFields.build();
                            makeExtensionsImmutable();
                        }
                    }
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder.class);
                    }

                    public static com.google.protobuf.Parser<DataSetValueExtension> PARSER =
                            new com.google.protobuf.AbstractParser<DataSetValueExtension>() {
                                public DataSetValueExtension parsePartialFrom(
                                        com.google.protobuf.CodedInputStream input,
                                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                        throws com.google.protobuf.InvalidProtocolBufferException {
                                    return new DataSetValueExtension(input, extensionRegistry);
                                }
                            };

                    @java.lang.Override
                    public com.google.protobuf.Parser<DataSetValueExtension> getParserForType() {
                        return PARSER;
                    }

                    private void initFields() {
                    }
                    private byte memoizedIsInitialized = -1;
                    public final boolean isInitialized() {
                        byte isInitialized = memoizedIsInitialized;
                        if (isInitialized == 1) return true;
                        if (isInitialized == 0) return false;

                        if (!extensionsAreInitialized()) {
                            memoizedIsInitialized = 0;
                            return false;
                        }
                        memoizedIsInitialized = 1;
                        return true;
                    }

                    public void writeTo(com.google.protobuf.CodedOutputStream output)
                            throws java.io.IOException {
                        getSerializedSize();
                        com.google.protobuf.GeneratedMessage
                                .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension>.ExtensionWriter extensionWriter =
                                newExtensionWriter();
                        extensionWriter.writeUntil(536870912, output);
                        getUnknownFields().writeTo(output);
                    }

                    private int memoizedSerializedSize = -1;
                    public int getSerializedSize() {
                        int size = memoizedSerializedSize;
                        if (size != -1) return size;

                        size = 0;
                        size += extensionsSerializedSize();
                        size += getUnknownFields().getSerializedSize();
                        memoizedSerializedSize = size;
                        return size;
                    }

                    private static final long serialVersionUID = 0L;
                    @java.lang.Override
                    protected java.lang.Object writeReplace()
                            throws java.io.ObjectStreamException {
                        return super.writeReplace();
                    }

                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            com.google.protobuf.ByteString data)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            com.google.protobuf.ByteString data,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(byte[] data)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            byte[] data,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws com.google.protobuf.InvalidProtocolBufferException {
                        return PARSER.parseFrom(data, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(java.io.InputStream input)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            java.io.InputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseDelimitedFrom(java.io.InputStream input)
                            throws java.io.IOException {
                        return PARSER.parseDelimitedFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseDelimitedFrom(
                            java.io.InputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseDelimitedFrom(input, extensionRegistry);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            com.google.protobuf.CodedInputStream input)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input);
                    }
                    public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parseFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        return PARSER.parseFrom(input, extensionRegistry);
                    }

                    public static Builder newBuilder() { return Builder.create(); }
                    public Builder newBuilderForType() { return newBuilder(); }
                    public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension prototype) {
                        return newBuilder().mergeFrom(prototype);
                    }
                    public Builder toBuilder() { return newBuilder(this); }

                    @java.lang.Override
                    protected Builder newBuilderForType(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        Builder builder = new Builder(parent);
                        return builder;
                    }
                    /**
                     * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension}
                     */
                    public static final class Builder extends
                            com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension, Builder> implements
                            // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension)
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder {
                        public static final com.google.protobuf.Descriptors.Descriptor
                        getDescriptor() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor;
                        }

                        protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                        internalGetFieldAccessorTable() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_fieldAccessorTable
                                    .ensureFieldAccessorsInitialized(
                                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder.class);
                        }

                        // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.newBuilder()
                        private Builder() {
                            maybeForceBuilderInitialization();
                        }

                        private Builder(
                                com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                            super(parent);
                            maybeForceBuilderInitialization();
                        }
                        private void maybeForceBuilderInitialization() {
                            if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                            }
                        }
                        private static Builder create() {
                            return new Builder();
                        }

                        public Builder clear() {
                            super.clear();
                            return this;
                        }

                        public Builder clone() {
                            return create().mergeFrom(buildPartial());
                        }

                        public com.google.protobuf.Descriptors.Descriptor
                        getDescriptorForType() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor;
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension getDefaultInstanceForType() {
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension build() {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension result = buildPartial();
                            if (!result.isInitialized()) {
                                throw newUninitializedMessageException(result);
                            }
                            return result;
                        }

                        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension buildPartial() {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension(this);
                            onBuilt();
                            return result;
                        }

                        public Builder mergeFrom(com.google.protobuf.Message other) {
                            if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) {
                                return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension)other);
                            } else {
                                super.mergeFrom(other);
                                return this;
                            }
                        }

                        public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension other) {
                            if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance()) return this;
                            this.mergeExtensionFields(other);
                            this.mergeUnknownFields(other.getUnknownFields());
                            return this;
                        }

                        public final boolean isInitialized() {
                            if (!extensionsAreInitialized()) {

                                return false;
                            }
                            return true;
                        }

                        public Builder mergeFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws java.io.IOException {
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension parsedMessage = null;
                            try {
                                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                                parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) e.getUnfinishedMessage();
                                throw e;
                            } finally {
                                if (parsedMessage != null) {
                                    mergeFrom(parsedMessage);
                                }
                            }
                            return this;
                        }

                        // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension)
                    }

                    static {
                        defaultInstance = new DataSetValueExtension(true);
                        defaultInstance.initFields();
                    }

                    // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension)
                }

                private int bitField0_;
                private int valueCase_ = 0;
                private java.lang.Object value_;
                public enum ValueCase
                        implements com.google.protobuf.Internal.EnumLite {
                    INT_VALUE(1),
                    LONG_VALUE(2),
                    FLOAT_VALUE(3),
                    DOUBLE_VALUE(4),
                    BOOLEAN_VALUE(5),
                    STRING_VALUE(6),
                    EXTENSION_VALUE(7),
                    VALUE_NOT_SET(0);
                    private int value = 0;
                    private ValueCase(int value) {
                        this.value = value;
                    }
                    public static ValueCase valueOf(int value) {
                        switch (value) {
                            case 1: return INT_VALUE;
                            case 2: return LONG_VALUE;
                            case 3: return FLOAT_VALUE;
                            case 4: return DOUBLE_VALUE;
                            case 5: return BOOLEAN_VALUE;
                            case 6: return STRING_VALUE;
                            case 7: return EXTENSION_VALUE;
                            case 0: return VALUE_NOT_SET;
                            default: throw new java.lang.IllegalArgumentException(
                                    "Value is undefined for this oneof enum.");
                        }
                    }
                    public int getNumber() {
                        return this.value;
                    }
                };

                public ValueCase
                getValueCase() {
                    return ValueCase.valueOf(
                            valueCase_);
                }

                public static final int INT_VALUE_FIELD_NUMBER = 1;
                /**
                 * <code>optional uint32 int_value = 1;</code>
                 */
                public boolean hasIntValue() {
                    return valueCase_ == 1;
                }
                /**
                 * <code>optional uint32 int_value = 1;</code>
                 */
                public int getIntValue() {
                    if (valueCase_ == 1) {
                        return (java.lang.Integer) value_;
                    }
                    return 0;
                }

                public static final int LONG_VALUE_FIELD_NUMBER = 2;
                /**
                 * <code>optional uint64 long_value = 2;</code>
                 */
                public boolean hasLongValue() {
                    return valueCase_ == 2;
                }
                /**
                 * <code>optional uint64 long_value = 2;</code>
                 */
                public long getLongValue() {
                    if (valueCase_ == 2) {
                        return (java.lang.Long) value_;
                    }
                    return 0L;
                }

                public static final int FLOAT_VALUE_FIELD_NUMBER = 3;
                /**
                 * <code>optional float float_value = 3;</code>
                 */
                public boolean hasFloatValue() {
                    return valueCase_ == 3;
                }
                /**
                 * <code>optional float float_value = 3;</code>
                 */
                public float getFloatValue() {
                    if (valueCase_ == 3) {
                        return (java.lang.Float) value_;
                    }
                    return 0F;
                }

                public static final int DOUBLE_VALUE_FIELD_NUMBER = 4;
                /**
                 * <code>optional double double_value = 4;</code>
                 */
                public boolean hasDoubleValue() {
                    return valueCase_ == 4;
                }
                /**
                 * <code>optional double double_value = 4;</code>
                 */
                public double getDoubleValue() {
                    if (valueCase_ == 4) {
                        return (java.lang.Double) value_;
                    }
                    return 0D;
                }

                public static final int BOOLEAN_VALUE_FIELD_NUMBER = 5;
                /**
                 * <code>optional bool boolean_value = 5;</code>
                 */
                public boolean hasBooleanValue() {
                    return valueCase_ == 5;
                }
                /**
                 * <code>optional bool boolean_value = 5;</code>
                 */
                public boolean getBooleanValue() {
                    if (valueCase_ == 5) {
                        return (java.lang.Boolean) value_;
                    }
                    return false;
                }

                public static final int STRING_VALUE_FIELD_NUMBER = 6;
                /**
                 * <code>optional string string_value = 6;</code>
                 */
                public boolean hasStringValue() {
                    return valueCase_ == 6;
                }
                /**
                 * <code>optional string string_value = 6;</code>
                 */
                public java.lang.String getStringValue() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 6) {
                        ref = value_;
                    }
                    if (ref instanceof java.lang.String) {
                        return (java.lang.String) ref;
                    } else {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8() && (valueCase_ == 6)) {
                            value_ = s;
                        }
                        return s;
                    }
                }
                /**
                 * <code>optional string string_value = 6;</code>
                 */
                public com.google.protobuf.ByteString
                getStringValueBytes() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 6) {
                        ref = value_;
                    }
                    if (ref instanceof java.lang.String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        if (valueCase_ == 6) {
                            value_ = b;
                        }
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }

                public static final int EXTENSION_VALUE_FIELD_NUMBER = 7;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                public boolean hasExtensionValue() {
                    return valueCase_ == 7;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension getExtensionValue() {
                    if (valueCase_ == 7) {
                        return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_;
                    }
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder getExtensionValueOrBuilder() {
                    if (valueCase_ == 7) {
                        return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_;
                    }
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                }

                private void initFields() {
                }
                private byte memoizedIsInitialized = -1;
                public final boolean isInitialized() {
                    byte isInitialized = memoizedIsInitialized;
                    if (isInitialized == 1) return true;
                    if (isInitialized == 0) return false;

                    if (hasExtensionValue()) {
                        if (!getExtensionValue().isInitialized()) {
                            memoizedIsInitialized = 0;
                            return false;
                        }
                    }
                    memoizedIsInitialized = 1;
                    return true;
                }

                public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
                    getSerializedSize();
                    if (valueCase_ == 1) {
                        output.writeUInt32(
                                1, (int)((java.lang.Integer) value_));
                    }
                    if (valueCase_ == 2) {
                        output.writeUInt64(
                                2, (long)((java.lang.Long) value_));
                    }
                    if (valueCase_ == 3) {
                        output.writeFloat(
                                3, (float)((java.lang.Float) value_));
                    }
                    if (valueCase_ == 4) {
                        output.writeDouble(
                                4, (double)((java.lang.Double) value_));
                    }
                    if (valueCase_ == 5) {
                        output.writeBool(
                                5, (boolean)((java.lang.Boolean) value_));
                    }
                    if (valueCase_ == 6) {
                        output.writeBytes(6, getStringValueBytes());
                    }
                    if (valueCase_ == 7) {
                        output.writeMessage(7, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_);
                    }
                    getUnknownFields().writeTo(output);
                }

                private int memoizedSerializedSize = -1;
                public int getSerializedSize() {
                    int size = memoizedSerializedSize;
                    if (size != -1) return size;

                    size = 0;
                    if (valueCase_ == 1) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeUInt32Size(
                                        1, (int)((java.lang.Integer) value_));
                    }
                    if (valueCase_ == 2) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeUInt64Size(
                                        2, (long)((java.lang.Long) value_));
                    }
                    if (valueCase_ == 3) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeFloatSize(
                                        3, (float)((java.lang.Float) value_));
                    }
                    if (valueCase_ == 4) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeDoubleSize(
                                        4, (double)((java.lang.Double) value_));
                    }
                    if (valueCase_ == 5) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeBoolSize(
                                        5, (boolean)((java.lang.Boolean) value_));
                    }
                    if (valueCase_ == 6) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeBytesSize(6, getStringValueBytes());
                    }
                    if (valueCase_ == 7) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeMessageSize(7, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_);
                    }
                    size += getUnknownFields().getSerializedSize();
                    memoizedSerializedSize = size;
                    return size;
                }

                private static final long serialVersionUID = 0L;
                @java.lang.Override
                protected java.lang.Object writeReplace()
                        throws java.io.ObjectStreamException {
                    return super.writeReplace();
                }

                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        com.google.protobuf.ByteString data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        com.google.protobuf.ByteString data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(byte[] data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        byte[] data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseDelimitedFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseDelimitedFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        com.google.protobuf.CodedInputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parseFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }

                public static Builder newBuilder() { return Builder.create(); }
                public Builder newBuilderForType() { return newBuilder(); }
                public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue prototype) {
                    return newBuilder().mergeFrom(prototype);
                }
                public Builder toBuilder() { return newBuilder(this); }

                @java.lang.Override
                protected Builder newBuilderForType(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    Builder builder = new Builder(parent);
                    return builder;
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue}
                 */
                public static final class Builder extends
                        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
                        // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue)
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder {
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder.class);
                    }

                    // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.newBuilder()
                    private Builder() {
                        maybeForceBuilderInitialization();
                    }

                    private Builder(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        super(parent);
                        maybeForceBuilderInitialization();
                    }
                    private void maybeForceBuilderInitialization() {
                        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        }
                    }
                    private static Builder create() {
                        return new Builder();
                    }

                    public Builder clear() {
                        super.clear();
                        valueCase_ = 0;
                        value_ = null;
                        return this;
                    }

                    public Builder clone() {
                        return create().mergeFrom(buildPartial());
                    }

                    public com.google.protobuf.Descriptors.Descriptor
                    getDescriptorForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue getDefaultInstanceForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.getDefaultInstance();
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue build() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue result = buildPartial();
                        if (!result.isInitialized()) {
                            throw newUninitializedMessageException(result);
                        }
                        return result;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue buildPartial() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue(this);
                        int from_bitField0_ = bitField0_;
                        int to_bitField0_ = 0;
                        if (valueCase_ == 1) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 2) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 3) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 4) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 5) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 6) {
                            result.value_ = value_;
                        }
                        if (valueCase_ == 7) {
                            if (extensionValueBuilder_ == null) {
                                result.value_ = value_;
                            } else {
                                result.value_ = extensionValueBuilder_.build();
                            }
                        }
                        result.bitField0_ = to_bitField0_;
                        result.valueCase_ = valueCase_;
                        onBuilt();
                        return result;
                    }

                    public Builder mergeFrom(com.google.protobuf.Message other) {
                        if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue) {
                            return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue)other);
                        } else {
                            super.mergeFrom(other);
                            return this;
                        }
                    }

                    public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue other) {
                        if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.getDefaultInstance()) return this;
                        switch (other.getValueCase()) {
                            case INT_VALUE: {
                                setIntValue(other.getIntValue());
                                break;
                            }
                            case LONG_VALUE: {
                                setLongValue(other.getLongValue());
                                break;
                            }
                            case FLOAT_VALUE: {
                                setFloatValue(other.getFloatValue());
                                break;
                            }
                            case DOUBLE_VALUE: {
                                setDoubleValue(other.getDoubleValue());
                                break;
                            }
                            case BOOLEAN_VALUE: {
                                setBooleanValue(other.getBooleanValue());
                                break;
                            }
                            case STRING_VALUE: {
                                valueCase_ = 6;
                                value_ = other.value_;
                                onChanged();
                                break;
                            }
                            case EXTENSION_VALUE: {
                                mergeExtensionValue(other.getExtensionValue());
                                break;
                            }
                            case VALUE_NOT_SET: {
                                break;
                            }
                        }
                        this.mergeUnknownFields(other.getUnknownFields());
                        return this;
                    }

                    public final boolean isInitialized() {
                        if (hasExtensionValue()) {
                            if (!getExtensionValue().isInitialized()) {

                                return false;
                            }
                        }
                        return true;
                    }

                    public Builder mergeFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue parsedMessage = null;
                        try {
                            parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue) e.getUnfinishedMessage();
                            throw e;
                        } finally {
                            if (parsedMessage != null) {
                                mergeFrom(parsedMessage);
                            }
                        }
                        return this;
                    }
                    private int valueCase_ = 0;
                    private java.lang.Object value_;
                    public ValueCase
                    getValueCase() {
                        return ValueCase.valueOf(
                                valueCase_);
                    }

                    public Builder clearValue() {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                        return this;
                    }

                    private int bitField0_;

                    /**
                     * <code>optional uint32 int_value = 1;</code>
                     */
                    public boolean hasIntValue() {
                        return valueCase_ == 1;
                    }
                    /**
                     * <code>optional uint32 int_value = 1;</code>
                     */
                    public int getIntValue() {
                        if (valueCase_ == 1) {
                            return (java.lang.Integer) value_;
                        }
                        return 0;
                    }
                    /**
                     * <code>optional uint32 int_value = 1;</code>
                     */
                    public Builder setIntValue(int value) {
                        valueCase_ = 1;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional uint32 int_value = 1;</code>
                     */
                    public Builder clearIntValue() {
                        if (valueCase_ == 1) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional uint64 long_value = 2;</code>
                     */
                    public boolean hasLongValue() {
                        return valueCase_ == 2;
                    }
                    /**
                     * <code>optional uint64 long_value = 2;</code>
                     */
                    public long getLongValue() {
                        if (valueCase_ == 2) {
                            return (java.lang.Long) value_;
                        }
                        return 0L;
                    }
                    /**
                     * <code>optional uint64 long_value = 2;</code>
                     */
                    public Builder setLongValue(long value) {
                        valueCase_ = 2;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional uint64 long_value = 2;</code>
                     */
                    public Builder clearLongValue() {
                        if (valueCase_ == 2) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional float float_value = 3;</code>
                     */
                    public boolean hasFloatValue() {
                        return valueCase_ == 3;
                    }
                    /**
                     * <code>optional float float_value = 3;</code>
                     */
                    public float getFloatValue() {
                        if (valueCase_ == 3) {
                            return (java.lang.Float) value_;
                        }
                        return 0F;
                    }
                    /**
                     * <code>optional float float_value = 3;</code>
                     */
                    public Builder setFloatValue(float value) {
                        valueCase_ = 3;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional float float_value = 3;</code>
                     */
                    public Builder clearFloatValue() {
                        if (valueCase_ == 3) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional double double_value = 4;</code>
                     */
                    public boolean hasDoubleValue() {
                        return valueCase_ == 4;
                    }
                    /**
                     * <code>optional double double_value = 4;</code>
                     */
                    public double getDoubleValue() {
                        if (valueCase_ == 4) {
                            return (java.lang.Double) value_;
                        }
                        return 0D;
                    }
                    /**
                     * <code>optional double double_value = 4;</code>
                     */
                    public Builder setDoubleValue(double value) {
                        valueCase_ = 4;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional double double_value = 4;</code>
                     */
                    public Builder clearDoubleValue() {
                        if (valueCase_ == 4) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional bool boolean_value = 5;</code>
                     */
                    public boolean hasBooleanValue() {
                        return valueCase_ == 5;
                    }
                    /**
                     * <code>optional bool boolean_value = 5;</code>
                     */
                    public boolean getBooleanValue() {
                        if (valueCase_ == 5) {
                            return (java.lang.Boolean) value_;
                        }
                        return false;
                    }
                    /**
                     * <code>optional bool boolean_value = 5;</code>
                     */
                    public Builder setBooleanValue(boolean value) {
                        valueCase_ = 5;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional bool boolean_value = 5;</code>
                     */
                    public Builder clearBooleanValue() {
                        if (valueCase_ == 5) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }

                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public boolean hasStringValue() {
                        return valueCase_ == 6;
                    }
                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public java.lang.String getStringValue() {
                        java.lang.Object ref = "";
                        if (valueCase_ == 6) {
                            ref = value_;
                        }
                        if (!(ref instanceof java.lang.String)) {
                            com.google.protobuf.ByteString bs =
                                    (com.google.protobuf.ByteString) ref;
                            java.lang.String s = bs.toStringUtf8();
                            if (valueCase_ == 6) {
                                if (bs.isValidUtf8()) {
                                    value_ = s;
                                }
                            }
                            return s;
                        } else {
                            return (java.lang.String) ref;
                        }
                    }
                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public com.google.protobuf.ByteString
                    getStringValueBytes() {
                        java.lang.Object ref = "";
                        if (valueCase_ == 6) {
                            ref = value_;
                        }
                        if (ref instanceof String) {
                            com.google.protobuf.ByteString b =
                                    com.google.protobuf.ByteString.copyFromUtf8(
                                            (java.lang.String) ref);
                            if (valueCase_ == 6) {
                                value_ = b;
                            }
                            return b;
                        } else {
                            return (com.google.protobuf.ByteString) ref;
                        }
                    }
                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public Builder setStringValue(
                            java.lang.String value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        valueCase_ = 6;
                        value_ = value;
                        onChanged();
                        return this;
                    }
                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public Builder clearStringValue() {
                        if (valueCase_ == 6) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                        return this;
                    }
                    /**
                     * <code>optional string string_value = 6;</code>
                     */
                    public Builder setStringValueBytes(
                            com.google.protobuf.ByteString value) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        valueCase_ = 6;
                        value_ = value;
                        onChanged();
                        return this;
                    }

                    private com.google.protobuf.SingleFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder> extensionValueBuilder_;
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public boolean hasExtensionValue() {
                        return valueCase_ == 7;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension getExtensionValue() {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 7) {
                                return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_;
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                        } else {
                            if (valueCase_ == 7) {
                                return extensionValueBuilder_.getMessage();
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                        }
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public Builder setExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension value) {
                        if (extensionValueBuilder_ == null) {
                            if (value == null) {
                                throw new NullPointerException();
                            }
                            value_ = value;
                            onChanged();
                        } else {
                            extensionValueBuilder_.setMessage(value);
                        }
                        valueCase_ = 7;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public Builder setExtensionValue(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder builderForValue) {
                        if (extensionValueBuilder_ == null) {
                            value_ = builderForValue.build();
                            onChanged();
                        } else {
                            extensionValueBuilder_.setMessage(builderForValue.build());
                        }
                        valueCase_ = 7;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public Builder mergeExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension value) {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 7 &&
                                    value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance()) {
                                value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_)
                                        .mergeFrom(value).buildPartial();
                            } else {
                                value_ = value;
                            }
                            onChanged();
                        } else {
                            if (valueCase_ == 7) {
                                extensionValueBuilder_.mergeFrom(value);
                            }
                            extensionValueBuilder_.setMessage(value);
                        }
                        valueCase_ = 7;
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public Builder clearExtensionValue() {
                        if (extensionValueBuilder_ == null) {
                            if (valueCase_ == 7) {
                                valueCase_ = 0;
                                value_ = null;
                                onChanged();
                            }
                        } else {
                            if (valueCase_ == 7) {
                                valueCase_ = 0;
                                value_ = null;
                            }
                            extensionValueBuilder_.clear();
                        }
                        return this;
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder getExtensionValueBuilder() {
                        return getExtensionValueFieldBuilder().getBuilder();
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder getExtensionValueOrBuilder() {
                        if ((valueCase_ == 7) && (extensionValueBuilder_ != null)) {
                            return extensionValueBuilder_.getMessageOrBuilder();
                        } else {
                            if (valueCase_ == 7) {
                                return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_;
                            }
                            return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                        }
                    }
                    /**
                     * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue.DataSetValueExtension extension_value = 7;</code>
                     */
                    private com.google.protobuf.SingleFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder>
                    getExtensionValueFieldBuilder() {
                        if (extensionValueBuilder_ == null) {
                            if (!(valueCase_ == 7)) {
                                value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.getDefaultInstance();
                            }
                            extensionValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtensionOrBuilder>(
                                    (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.DataSetValueExtension) value_,
                                    getParentForChildren(),
                                    isClean());
                            value_ = null;
                        }
                        valueCase_ = 7;
                        return extensionValueBuilder_;
                    }

                    // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue)
                }

                static {
                    defaultInstance = new DataSetValue(true);
                    defaultInstance.initFields();
                }

                // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue)
            }

            public interface RowOrBuilder extends
                    // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row)
                    com.google.protobuf.GeneratedMessage.
                            ExtendableMessageOrBuilder<Row> {

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue>
                getElementsList();
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue getElements(int index);
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                int getElementsCount();
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder>
                getElementsOrBuilderList();
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder getElementsOrBuilder(
                        int index);
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row}
             */
            public static final class Row extends
                    com.google.protobuf.GeneratedMessage.ExtendableMessage<
                            Row> implements
                    // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row)
                    RowOrBuilder {
                // Use Row.newBuilder() to construct.
                private Row(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row, ?> builder) {
                    super(builder);
                    this.unknownFields = builder.getUnknownFields();
                }
                private Row(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                private static final Row defaultInstance;
                public static Row getDefaultInstance() {
                    return defaultInstance;
                }

                public Row getDefaultInstanceForType() {
                    return defaultInstance;
                }

                private final com.google.protobuf.UnknownFieldSet unknownFields;
                @java.lang.Override
                public final com.google.protobuf.UnknownFieldSet
                getUnknownFields() {
                    return this.unknownFields;
                }
                private Row(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    initFields();
                    int mutable_bitField0_ = 0;
                    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                            com.google.protobuf.UnknownFieldSet.newBuilder();
                    try {
                        boolean done = false;
                        while (!done) {
                            int tag = input.readTag();
                            switch (tag) {
                                case 0:
                                    done = true;
                                    break;
                                default: {
                                    if (!parseUnknownField(input, unknownFields,
                                            extensionRegistry, tag)) {
                                        done = true;
                                    }
                                    break;
                                }
                                case 10: {
                                    if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                                        elements_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue>();
                                        mutable_bitField0_ |= 0x00000001;
                                    }
                                    elements_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.PARSER, extensionRegistry));
                                    break;
                                }
                            }
                        }
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(this);
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(
                                e.getMessage()).setUnfinishedMessage(this);
                    } finally {
                        if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                            elements_ = java.util.Collections.unmodifiableList(elements_);
                        }
                        this.unknownFields = unknownFields.build();
                        makeExtensionsImmutable();
                    }
                }
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder.class);
                }

                public static com.google.protobuf.Parser<Row> PARSER =
                        new com.google.protobuf.AbstractParser<Row>() {
                            public Row parsePartialFrom(
                                    com.google.protobuf.CodedInputStream input,
                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                    throws com.google.protobuf.InvalidProtocolBufferException {
                                return new Row(input, extensionRegistry);
                            }
                        };

                @java.lang.Override
                public com.google.protobuf.Parser<Row> getParserForType() {
                    return PARSER;
                }

                public static final int ELEMENTS_FIELD_NUMBER = 1;
                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue> elements_;
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue> getElementsList() {
                    return elements_;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder>
                getElementsOrBuilderList() {
                    return elements_;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                public int getElementsCount() {
                    return elements_.size();
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue getElements(int index) {
                    return elements_.get(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder getElementsOrBuilder(
                        int index) {
                    return elements_.get(index);
                }

                private void initFields() {
                    elements_ = java.util.Collections.emptyList();
                }
                private byte memoizedIsInitialized = -1;
                public final boolean isInitialized() {
                    byte isInitialized = memoizedIsInitialized;
                    if (isInitialized == 1) return true;
                    if (isInitialized == 0) return false;

                    for (int i = 0; i < getElementsCount(); i++) {
                        if (!getElements(i).isInitialized()) {
                            memoizedIsInitialized = 0;
                            return false;
                        }
                    }
                    if (!extensionsAreInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                    memoizedIsInitialized = 1;
                    return true;
                }

                public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
                    getSerializedSize();
                    com.google.protobuf.GeneratedMessage
                            .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row>.ExtensionWriter extensionWriter =
                            newExtensionWriter();
                    for (int i = 0; i < elements_.size(); i++) {
                        output.writeMessage(1, elements_.get(i));
                    }
                    extensionWriter.writeUntil(536870912, output);
                    getUnknownFields().writeTo(output);
                }

                private int memoizedSerializedSize = -1;
                public int getSerializedSize() {
                    int size = memoizedSerializedSize;
                    if (size != -1) return size;

                    size = 0;
                    for (int i = 0; i < elements_.size(); i++) {
                        size += com.google.protobuf.CodedOutputStream
                                .computeMessageSize(1, elements_.get(i));
                    }
                    size += extensionsSerializedSize();
                    size += getUnknownFields().getSerializedSize();
                    memoizedSerializedSize = size;
                    return size;
                }

                private static final long serialVersionUID = 0L;
                @java.lang.Override
                protected java.lang.Object writeReplace()
                        throws java.io.ObjectStreamException {
                    return super.writeReplace();
                }

                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        com.google.protobuf.ByteString data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        com.google.protobuf.ByteString data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(byte[] data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        byte[] data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseDelimitedFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseDelimitedFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        com.google.protobuf.CodedInputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parseFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }

                public static Builder newBuilder() { return Builder.create(); }
                public Builder newBuilderForType() { return newBuilder(); }
                public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row prototype) {
                    return newBuilder().mergeFrom(prototype);
                }
                public Builder toBuilder() { return newBuilder(this); }

                @java.lang.Override
                protected Builder newBuilderForType(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    Builder builder = new Builder(parent);
                    return builder;
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row}
                 */
                public static final class Builder extends
                        com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row, Builder> implements
                        // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row)
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder {
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder.class);
                    }

                    // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.newBuilder()
                    private Builder() {
                        maybeForceBuilderInitialization();
                    }

                    private Builder(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        super(parent);
                        maybeForceBuilderInitialization();
                    }
                    private void maybeForceBuilderInitialization() {
                        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                            getElementsFieldBuilder();
                        }
                    }
                    private static Builder create() {
                        return new Builder();
                    }

                    public Builder clear() {
                        super.clear();
                        if (elementsBuilder_ == null) {
                            elements_ = java.util.Collections.emptyList();
                            bitField0_ = (bitField0_ & ~0x00000001);
                        } else {
                            elementsBuilder_.clear();
                        }
                        return this;
                    }

                    public Builder clone() {
                        return create().mergeFrom(buildPartial());
                    }

                    public com.google.protobuf.Descriptors.Descriptor
                    getDescriptorForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row getDefaultInstanceForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.getDefaultInstance();
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row build() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row result = buildPartial();
                        if (!result.isInitialized()) {
                            throw newUninitializedMessageException(result);
                        }
                        return result;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row buildPartial() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row(this);
                        int from_bitField0_ = bitField0_;
                        if (elementsBuilder_ == null) {
                            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                                elements_ = java.util.Collections.unmodifiableList(elements_);
                                bitField0_ = (bitField0_ & ~0x00000001);
                            }
                            result.elements_ = elements_;
                        } else {
                            result.elements_ = elementsBuilder_.build();
                        }
                        onBuilt();
                        return result;
                    }

                    public Builder mergeFrom(com.google.protobuf.Message other) {
                        if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row) {
                            return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row)other);
                        } else {
                            super.mergeFrom(other);
                            return this;
                        }
                    }

                    public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row other) {
                        if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.getDefaultInstance()) return this;
                        if (elementsBuilder_ == null) {
                            if (!other.elements_.isEmpty()) {
                                if (elements_.isEmpty()) {
                                    elements_ = other.elements_;
                                    bitField0_ = (bitField0_ & ~0x00000001);
                                } else {
                                    ensureElementsIsMutable();
                                    elements_.addAll(other.elements_);
                                }
                                onChanged();
                            }
                        } else {
                            if (!other.elements_.isEmpty()) {
                                if (elementsBuilder_.isEmpty()) {
                                    elementsBuilder_.dispose();
                                    elementsBuilder_ = null;
                                    elements_ = other.elements_;
                                    bitField0_ = (bitField0_ & ~0x00000001);
                                    elementsBuilder_ =
                                            com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                    getElementsFieldBuilder() : null;
                                } else {
                                    elementsBuilder_.addAllMessages(other.elements_);
                                }
                            }
                        }
                        this.mergeExtensionFields(other);
                        this.mergeUnknownFields(other.getUnknownFields());
                        return this;
                    }

                    public final boolean isInitialized() {
                        for (int i = 0; i < getElementsCount(); i++) {
                            if (!getElements(i).isInitialized()) {

                                return false;
                            }
                        }
                        if (!extensionsAreInitialized()) {

                            return false;
                        }
                        return true;
                    }

                    public Builder mergeFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row parsedMessage = null;
                        try {
                            parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row) e.getUnfinishedMessage();
                            throw e;
                        } finally {
                            if (parsedMessage != null) {
                                mergeFrom(parsedMessage);
                            }
                        }
                        return this;
                    }
                    private int bitField0_;

                    private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue> elements_ =
                            java.util.Collections.emptyList();
                    private void ensureElementsIsMutable() {
                        if (!((bitField0_ & 0x00000001) == 0x00000001)) {
                            elements_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue>(elements_);
                            bitField0_ |= 0x00000001;
                        }
                    }

                    private com.google.protobuf.RepeatedFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder> elementsBuilder_;

                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue> getElementsList() {
                        if (elementsBuilder_ == null) {
                            return java.util.Collections.unmodifiableList(elements_);
                        } else {
                            return elementsBuilder_.getMessageList();
                        }
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public int getElementsCount() {
                        if (elementsBuilder_ == null) {
                            return elements_.size();
                        } else {
                            return elementsBuilder_.getCount();
                        }
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue getElements(int index) {
                        if (elementsBuilder_ == null) {
                            return elements_.get(index);
                        } else {
                            return elementsBuilder_.getMessage(index);
                        }
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder setElements(
                            int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue value) {
                        if (elementsBuilder_ == null) {
                            if (value == null) {
                                throw new NullPointerException();
                            }
                            ensureElementsIsMutable();
                            elements_.set(index, value);
                            onChanged();
                        } else {
                            elementsBuilder_.setMessage(index, value);
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder setElements(
                            int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder builderForValue) {
                        if (elementsBuilder_ == null) {
                            ensureElementsIsMutable();
                            elements_.set(index, builderForValue.build());
                            onChanged();
                        } else {
                            elementsBuilder_.setMessage(index, builderForValue.build());
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder addElements(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue value) {
                        if (elementsBuilder_ == null) {
                            if (value == null) {
                                throw new NullPointerException();
                            }
                            ensureElementsIsMutable();
                            elements_.add(value);
                            onChanged();
                        } else {
                            elementsBuilder_.addMessage(value);
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder addElements(
                            int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue value) {
                        if (elementsBuilder_ == null) {
                            if (value == null) {
                                throw new NullPointerException();
                            }
                            ensureElementsIsMutable();
                            elements_.add(index, value);
                            onChanged();
                        } else {
                            elementsBuilder_.addMessage(index, value);
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder addElements(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder builderForValue) {
                        if (elementsBuilder_ == null) {
                            ensureElementsIsMutable();
                            elements_.add(builderForValue.build());
                            onChanged();
                        } else {
                            elementsBuilder_.addMessage(builderForValue.build());
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder addElements(
                            int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder builderForValue) {
                        if (elementsBuilder_ == null) {
                            ensureElementsIsMutable();
                            elements_.add(index, builderForValue.build());
                            onChanged();
                        } else {
                            elementsBuilder_.addMessage(index, builderForValue.build());
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder addAllElements(
                            java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue> values) {
                        if (elementsBuilder_ == null) {
                            ensureElementsIsMutable();
                            com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                    values, elements_);
                            onChanged();
                        } else {
                            elementsBuilder_.addAllMessages(values);
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder clearElements() {
                        if (elementsBuilder_ == null) {
                            elements_ = java.util.Collections.emptyList();
                            bitField0_ = (bitField0_ & ~0x00000001);
                            onChanged();
                        } else {
                            elementsBuilder_.clear();
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public Builder removeElements(int index) {
                        if (elementsBuilder_ == null) {
                            ensureElementsIsMutable();
                            elements_.remove(index);
                            onChanged();
                        } else {
                            elementsBuilder_.remove(index);
                        }
                        return this;
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder getElementsBuilder(
                            int index) {
                        return getElementsFieldBuilder().getBuilder(index);
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder getElementsOrBuilder(
                            int index) {
                        if (elementsBuilder_ == null) {
                            return elements_.get(index);  } else {
                            return elementsBuilder_.getMessageOrBuilder(index);
                        }
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder>
                    getElementsOrBuilderList() {
                        if (elementsBuilder_ != null) {
                            return elementsBuilder_.getMessageOrBuilderList();
                        } else {
                            return java.util.Collections.unmodifiableList(elements_);
                        }
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder addElementsBuilder() {
                        return getElementsFieldBuilder().addBuilder(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.getDefaultInstance());
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder addElementsBuilder(
                            int index) {
                        return getElementsFieldBuilder().addBuilder(
                                index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.getDefaultInstance());
                    }
                    /**
                     * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.DataSetValue elements = 1;</code>
                     */
                    public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder>
                    getElementsBuilderList() {
                        return getElementsFieldBuilder().getBuilderList();
                    }
                    private com.google.protobuf.RepeatedFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder>
                    getElementsFieldBuilder() {
                        if (elementsBuilder_ == null) {
                            elementsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.DataSetValueOrBuilder>(
                                    elements_,
                                    ((bitField0_ & 0x00000001) == 0x00000001),
                                    getParentForChildren(),
                                    isClean());
                            elements_ = null;
                        }
                        return elementsBuilder_;
                    }

                    // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row)
                }

                static {
                    defaultInstance = new Row(true);
                    defaultInstance.initFields();
                }

                // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row)
            }

            private int bitField0_;
            public static final int NUM_OF_COLUMNS_FIELD_NUMBER = 1;
            private long numOfColumns_;
            /**
             * <code>optional uint64 num_of_columns = 1;</code>
             */
            public boolean hasNumOfColumns() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional uint64 num_of_columns = 1;</code>
             */
            public long getNumOfColumns() {
                return numOfColumns_;
            }

            public static final int COLUMNS_FIELD_NUMBER = 2;
            private com.google.protobuf.LazyStringList columns_;
            /**
             * <code>repeated string columns = 2;</code>
             */
            public com.google.protobuf.ProtocolStringList
            getColumnsList() {
                return columns_;
            }
            /**
             * <code>repeated string columns = 2;</code>
             */
            public int getColumnsCount() {
                return columns_.size();
            }
            /**
             * <code>repeated string columns = 2;</code>
             */
            public java.lang.String getColumns(int index) {
                return columns_.get(index);
            }
            /**
             * <code>repeated string columns = 2;</code>
             */
            public com.google.protobuf.ByteString
            getColumnsBytes(int index) {
                return columns_.getByteString(index);
            }

            public static final int TYPES_FIELD_NUMBER = 3;
            private java.util.List<java.lang.Integer> types_;
            /**
             * <code>repeated uint32 types = 3;</code>
             */
            public java.util.List<java.lang.Integer>
            getTypesList() {
                return types_;
            }
            /**
             * <code>repeated uint32 types = 3;</code>
             */
            public int getTypesCount() {
                return types_.size();
            }
            /**
             * <code>repeated uint32 types = 3;</code>
             */
            public int getTypes(int index) {
                return types_.get(index);
            }

            public static final int ROWS_FIELD_NUMBER = 4;
            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row> rows_;
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row> getRowsList() {
                return rows_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder>
            getRowsOrBuilderList() {
                return rows_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            public int getRowsCount() {
                return rows_.size();
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row getRows(int index) {
                return rows_.get(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder getRowsOrBuilder(
                    int index) {
                return rows_.get(index);
            }

            private void initFields() {
                numOfColumns_ = 0L;
                columns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                types_ = java.util.Collections.emptyList();
                rows_ = java.util.Collections.emptyList();
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                for (int i = 0; i < getRowsCount(); i++) {
                    if (!getRows(i).isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (!extensionsAreInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                com.google.protobuf.GeneratedMessage
                        .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet>.ExtensionWriter extensionWriter =
                        newExtensionWriter();
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    output.writeUInt64(1, numOfColumns_);
                }
                for (int i = 0; i < columns_.size(); i++) {
                    output.writeBytes(2, columns_.getByteString(i));
                }
                for (int i = 0; i < types_.size(); i++) {
                    output.writeUInt32(3, types_.get(i));
                }
                for (int i = 0; i < rows_.size(); i++) {
                    output.writeMessage(4, rows_.get(i));
                }
                extensionWriter.writeUntil(536870912, output);
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(1, numOfColumns_);
                }
                {
                    int dataSize = 0;
                    for (int i = 0; i < columns_.size(); i++) {
                        dataSize += com.google.protobuf.CodedOutputStream
                                .computeBytesSizeNoTag(columns_.getByteString(i));
                    }
                    size += dataSize;
                    size += 1 * getColumnsList().size();
                }
                {
                    int dataSize = 0;
                    for (int i = 0; i < types_.size(); i++) {
                        dataSize += com.google.protobuf.CodedOutputStream
                                .computeUInt32SizeNoTag(types_.get(i));
                    }
                    size += dataSize;
                    size += 1 * getTypesList().size();
                }
                for (int i = 0; i < rows_.size(); i++) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(4, rows_.get(i));
                }
                size += extensionsSerializedSize();
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet, Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        getRowsFieldBuilder();
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    numOfColumns_ = 0L;
                    bitField0_ = (bitField0_ & ~0x00000001);
                    columns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                    bitField0_ = (bitField0_ & ~0x00000002);
                    types_ = java.util.Collections.emptyList();
                    bitField0_ = (bitField0_ & ~0x00000004);
                    if (rowsBuilder_ == null) {
                        rows_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000008);
                    } else {
                        rowsBuilder_.clear();
                    }
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet(this);
                    int from_bitField0_ = bitField0_;
                    int to_bitField0_ = 0;
                    if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                        to_bitField0_ |= 0x00000001;
                    }
                    result.numOfColumns_ = numOfColumns_;
                    if (((bitField0_ & 0x00000002) == 0x00000002)) {
                        columns_ = columns_.getUnmodifiableView();
                        bitField0_ = (bitField0_ & ~0x00000002);
                    }
                    result.columns_ = columns_;
                    if (((bitField0_ & 0x00000004) == 0x00000004)) {
                        types_ = java.util.Collections.unmodifiableList(types_);
                        bitField0_ = (bitField0_ & ~0x00000004);
                    }
                    result.types_ = types_;
                    if (rowsBuilder_ == null) {
                        if (((bitField0_ & 0x00000008) == 0x00000008)) {
                            rows_ = java.util.Collections.unmodifiableList(rows_);
                            bitField0_ = (bitField0_ & ~0x00000008);
                        }
                        result.rows_ = rows_;
                    } else {
                        result.rows_ = rowsBuilder_.build();
                    }
                    result.bitField0_ = to_bitField0_;
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance()) return this;
                    if (other.hasNumOfColumns()) {
                        setNumOfColumns(other.getNumOfColumns());
                    }
                    if (!other.columns_.isEmpty()) {
                        if (columns_.isEmpty()) {
                            columns_ = other.columns_;
                            bitField0_ = (bitField0_ & ~0x00000002);
                        } else {
                            ensureColumnsIsMutable();
                            columns_.addAll(other.columns_);
                        }
                        onChanged();
                    }
                    if (!other.types_.isEmpty()) {
                        if (types_.isEmpty()) {
                            types_ = other.types_;
                            bitField0_ = (bitField0_ & ~0x00000004);
                        } else {
                            ensureTypesIsMutable();
                            types_.addAll(other.types_);
                        }
                        onChanged();
                    }
                    if (rowsBuilder_ == null) {
                        if (!other.rows_.isEmpty()) {
                            if (rows_.isEmpty()) {
                                rows_ = other.rows_;
                                bitField0_ = (bitField0_ & ~0x00000008);
                            } else {
                                ensureRowsIsMutable();
                                rows_.addAll(other.rows_);
                            }
                            onChanged();
                        }
                    } else {
                        if (!other.rows_.isEmpty()) {
                            if (rowsBuilder_.isEmpty()) {
                                rowsBuilder_.dispose();
                                rowsBuilder_ = null;
                                rows_ = other.rows_;
                                bitField0_ = (bitField0_ & ~0x00000008);
                                rowsBuilder_ =
                                        com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                getRowsFieldBuilder() : null;
                            } else {
                                rowsBuilder_.addAllMessages(other.rows_);
                            }
                        }
                    }
                    this.mergeExtensionFields(other);
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    for (int i = 0; i < getRowsCount(); i++) {
                        if (!getRows(i).isInitialized()) {

                            return false;
                        }
                    }
                    if (!extensionsAreInitialized()) {

                        return false;
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int bitField0_;

                private long numOfColumns_ ;
                /**
                 * <code>optional uint64 num_of_columns = 1;</code>
                 */
                public boolean hasNumOfColumns() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional uint64 num_of_columns = 1;</code>
                 */
                public long getNumOfColumns() {
                    return numOfColumns_;
                }
                /**
                 * <code>optional uint64 num_of_columns = 1;</code>
                 */
                public Builder setNumOfColumns(long value) {
                    bitField0_ |= 0x00000001;
                    numOfColumns_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 num_of_columns = 1;</code>
                 */
                public Builder clearNumOfColumns() {
                    bitField0_ = (bitField0_ & ~0x00000001);
                    numOfColumns_ = 0L;
                    onChanged();
                    return this;
                }

                private com.google.protobuf.LazyStringList columns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                private void ensureColumnsIsMutable() {
                    if (!((bitField0_ & 0x00000002) == 0x00000002)) {
                        columns_ = new com.google.protobuf.LazyStringArrayList(columns_);
                        bitField0_ |= 0x00000002;
                    }
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public com.google.protobuf.ProtocolStringList
                getColumnsList() {
                    return columns_.getUnmodifiableView();
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public int getColumnsCount() {
                    return columns_.size();
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public java.lang.String getColumns(int index) {
                    return columns_.get(index);
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public com.google.protobuf.ByteString
                getColumnsBytes(int index) {
                    return columns_.getByteString(index);
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public Builder setColumns(
                        int index, java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureColumnsIsMutable();
                    columns_.set(index, value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public Builder addColumns(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureColumnsIsMutable();
                    columns_.add(value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public Builder addAllColumns(
                        java.lang.Iterable<java.lang.String> values) {
                    ensureColumnsIsMutable();
                    com.google.protobuf.AbstractMessageLite.Builder.addAll(
                            values, columns_);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public Builder clearColumns() {
                    columns_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                    bitField0_ = (bitField0_ & ~0x00000002);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string columns = 2;</code>
                 */
                public Builder addColumnsBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureColumnsIsMutable();
                    columns_.add(value);
                    onChanged();
                    return this;
                }

                private java.util.List<java.lang.Integer> types_ = java.util.Collections.emptyList();
                private void ensureTypesIsMutable() {
                    if (!((bitField0_ & 0x00000004) == 0x00000004)) {
                        types_ = new java.util.ArrayList<java.lang.Integer>(types_);
                        bitField0_ |= 0x00000004;
                    }
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public java.util.List<java.lang.Integer>
                getTypesList() {
                    return java.util.Collections.unmodifiableList(types_);
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public int getTypesCount() {
                    return types_.size();
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public int getTypes(int index) {
                    return types_.get(index);
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public Builder setTypes(
                        int index, int value) {
                    ensureTypesIsMutable();
                    types_.set(index, value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public Builder addTypes(int value) {
                    ensureTypesIsMutable();
                    types_.add(value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public Builder addAllTypes(
                        java.lang.Iterable<? extends java.lang.Integer> values) {
                    ensureTypesIsMutable();
                    com.google.protobuf.AbstractMessageLite.Builder.addAll(
                            values, types_);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated uint32 types = 3;</code>
                 */
                public Builder clearTypes() {
                    types_ = java.util.Collections.emptyList();
                    bitField0_ = (bitField0_ & ~0x00000004);
                    onChanged();
                    return this;
                }

                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row> rows_ =
                        java.util.Collections.emptyList();
                private void ensureRowsIsMutable() {
                    if (!((bitField0_ & 0x00000008) == 0x00000008)) {
                        rows_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row>(rows_);
                        bitField0_ |= 0x00000008;
                    }
                }

                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder> rowsBuilder_;

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row> getRowsList() {
                    if (rowsBuilder_ == null) {
                        return java.util.Collections.unmodifiableList(rows_);
                    } else {
                        return rowsBuilder_.getMessageList();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public int getRowsCount() {
                    if (rowsBuilder_ == null) {
                        return rows_.size();
                    } else {
                        return rowsBuilder_.getCount();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row getRows(int index) {
                    if (rowsBuilder_ == null) {
                        return rows_.get(index);
                    } else {
                        return rowsBuilder_.getMessage(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder setRows(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row value) {
                    if (rowsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureRowsIsMutable();
                        rows_.set(index, value);
                        onChanged();
                    } else {
                        rowsBuilder_.setMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder setRows(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder builderForValue) {
                    if (rowsBuilder_ == null) {
                        ensureRowsIsMutable();
                        rows_.set(index, builderForValue.build());
                        onChanged();
                    } else {
                        rowsBuilder_.setMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder addRows(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row value) {
                    if (rowsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureRowsIsMutable();
                        rows_.add(value);
                        onChanged();
                    } else {
                        rowsBuilder_.addMessage(value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder addRows(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row value) {
                    if (rowsBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureRowsIsMutable();
                        rows_.add(index, value);
                        onChanged();
                    } else {
                        rowsBuilder_.addMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder addRows(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder builderForValue) {
                    if (rowsBuilder_ == null) {
                        ensureRowsIsMutable();
                        rows_.add(builderForValue.build());
                        onChanged();
                    } else {
                        rowsBuilder_.addMessage(builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder addRows(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder builderForValue) {
                    if (rowsBuilder_ == null) {
                        ensureRowsIsMutable();
                        rows_.add(index, builderForValue.build());
                        onChanged();
                    } else {
                        rowsBuilder_.addMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder addAllRows(
                        java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row> values) {
                    if (rowsBuilder_ == null) {
                        ensureRowsIsMutable();
                        com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                values, rows_);
                        onChanged();
                    } else {
                        rowsBuilder_.addAllMessages(values);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder clearRows() {
                    if (rowsBuilder_ == null) {
                        rows_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000008);
                        onChanged();
                    } else {
                        rowsBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public Builder removeRows(int index) {
                    if (rowsBuilder_ == null) {
                        ensureRowsIsMutable();
                        rows_.remove(index);
                        onChanged();
                    } else {
                        rowsBuilder_.remove(index);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder getRowsBuilder(
                        int index) {
                    return getRowsFieldBuilder().getBuilder(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder getRowsOrBuilder(
                        int index) {
                    if (rowsBuilder_ == null) {
                        return rows_.get(index);  } else {
                        return rowsBuilder_.getMessageOrBuilder(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder>
                getRowsOrBuilderList() {
                    if (rowsBuilder_ != null) {
                        return rowsBuilder_.getMessageOrBuilderList();
                    } else {
                        return java.util.Collections.unmodifiableList(rows_);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder addRowsBuilder() {
                    return getRowsFieldBuilder().addBuilder(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder addRowsBuilder(
                        int index) {
                    return getRowsFieldBuilder().addBuilder(
                            index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet.Row rows = 4;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder>
                getRowsBuilderList() {
                    return getRowsFieldBuilder().getBuilderList();
                }
                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder>
                getRowsFieldBuilder() {
                    if (rowsBuilder_ == null) {
                        rowsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Row.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.RowOrBuilder>(
                                rows_,
                                ((bitField0_ & 0x00000008) == 0x00000008),
                                getParentForChildren(),
                                isClean());
                        rows_ = null;
                    }
                    return rowsBuilder_;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet)
            }

            static {
                defaultInstance = new DataSet(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet)
        }

        public interface PropertyValueOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue)
                com.google.protobuf.MessageOrBuilder {

            /**
             * <code>optional uint32 type = 1;</code>
             */
            boolean hasType();
            /**
             * <code>optional uint32 type = 1;</code>
             */
            int getType();

            /**
             * <code>optional bool is_null = 2;</code>
             */
            boolean hasIsNull();
            /**
             * <code>optional bool is_null = 2;</code>
             */
            boolean getIsNull();

            /**
             * <code>optional uint32 int_value = 3;</code>
             */
            boolean hasIntValue();
            /**
             * <code>optional uint32 int_value = 3;</code>
             */
            int getIntValue();

            /**
             * <code>optional uint64 long_value = 4;</code>
             */
            boolean hasLongValue();
            /**
             * <code>optional uint64 long_value = 4;</code>
             */
            long getLongValue();

            /**
             * <code>optional float float_value = 5;</code>
             */
            boolean hasFloatValue();
            /**
             * <code>optional float float_value = 5;</code>
             */
            float getFloatValue();

            /**
             * <code>optional double double_value = 6;</code>
             */
            boolean hasDoubleValue();
            /**
             * <code>optional double double_value = 6;</code>
             */
            double getDoubleValue();

            /**
             * <code>optional bool boolean_value = 7;</code>
             */
            boolean hasBooleanValue();
            /**
             * <code>optional bool boolean_value = 7;</code>
             */
            boolean getBooleanValue();

            /**
             * <code>optional string string_value = 8;</code>
             */
            boolean hasStringValue();
            /**
             * <code>optional string string_value = 8;</code>
             */
            java.lang.String getStringValue();
            /**
             * <code>optional string string_value = 8;</code>
             */
            com.google.protobuf.ByteString
            getStringValueBytes();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            boolean hasPropertysetValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertysetValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetValueOrBuilder();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            boolean hasPropertysetsValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList getPropertysetsValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder getPropertysetsValueOrBuilder();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            boolean hasExtensionValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension getExtensionValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder getExtensionValueOrBuilder();
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue}
         */
        public static final class PropertyValue extends
                com.google.protobuf.GeneratedMessage implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue)
                PropertyValueOrBuilder {
            // Use PropertyValue.newBuilder() to construct.
            private PropertyValue(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private PropertyValue(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final PropertyValue defaultInstance;
            public static PropertyValue getDefaultInstance() {
                return defaultInstance;
            }

            public PropertyValue getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private PropertyValue(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 8: {
                                bitField0_ |= 0x00000001;
                                type_ = input.readUInt32();
                                break;
                            }
                            case 16: {
                                bitField0_ |= 0x00000002;
                                isNull_ = input.readBool();
                                break;
                            }
                            case 24: {
                                valueCase_ = 3;
                                value_ = input.readUInt32();
                                break;
                            }
                            case 32: {
                                valueCase_ = 4;
                                value_ = input.readUInt64();
                                break;
                            }
                            case 45: {
                                valueCase_ = 5;
                                value_ = input.readFloat();
                                break;
                            }
                            case 49: {
                                valueCase_ = 6;
                                value_ = input.readDouble();
                                break;
                            }
                            case 56: {
                                valueCase_ = 7;
                                value_ = input.readBool();
                                break;
                            }
                            case 66: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                valueCase_ = 8;
                                value_ = bs;
                                break;
                            }
                            case 74: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder subBuilder = null;
                                if (valueCase_ == 9) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 9;
                                break;
                            }
                            case 82: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder subBuilder = null;
                                if (valueCase_ == 10) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 10;
                                break;
                            }
                            case 90: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder subBuilder = null;
                                if (valueCase_ == 11) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 11;
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder.class);
            }

            public static com.google.protobuf.Parser<PropertyValue> PARSER =
                    new com.google.protobuf.AbstractParser<PropertyValue>() {
                        public PropertyValue parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new PropertyValue(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<PropertyValue> getParserForType() {
                return PARSER;
            }

            public interface PropertyValueExtensionOrBuilder extends
                    // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension)
                    com.google.protobuf.GeneratedMessage.
                            ExtendableMessageOrBuilder<PropertyValueExtension> {
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension}
             */
            public static final class PropertyValueExtension extends
                    com.google.protobuf.GeneratedMessage.ExtendableMessage<
                            PropertyValueExtension> implements
                    // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension)
                    PropertyValueExtensionOrBuilder {
                // Use PropertyValueExtension.newBuilder() to construct.
                private PropertyValueExtension(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension, ?> builder) {
                    super(builder);
                    this.unknownFields = builder.getUnknownFields();
                }
                private PropertyValueExtension(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                private static final PropertyValueExtension defaultInstance;
                public static PropertyValueExtension getDefaultInstance() {
                    return defaultInstance;
                }

                public PropertyValueExtension getDefaultInstanceForType() {
                    return defaultInstance;
                }

                private final com.google.protobuf.UnknownFieldSet unknownFields;
                @java.lang.Override
                public final com.google.protobuf.UnknownFieldSet
                getUnknownFields() {
                    return this.unknownFields;
                }
                private PropertyValueExtension(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    initFields();
                    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                            com.google.protobuf.UnknownFieldSet.newBuilder();
                    try {
                        boolean done = false;
                        while (!done) {
                            int tag = input.readTag();
                            switch (tag) {
                                case 0:
                                    done = true;
                                    break;
                                default: {
                                    if (!parseUnknownField(input, unknownFields,
                                            extensionRegistry, tag)) {
                                        done = true;
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(this);
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(
                                e.getMessage()).setUnfinishedMessage(this);
                    } finally {
                        this.unknownFields = unknownFields.build();
                        makeExtensionsImmutable();
                    }
                }
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder.class);
                }

                public static com.google.protobuf.Parser<PropertyValueExtension> PARSER =
                        new com.google.protobuf.AbstractParser<PropertyValueExtension>() {
                            public PropertyValueExtension parsePartialFrom(
                                    com.google.protobuf.CodedInputStream input,
                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                    throws com.google.protobuf.InvalidProtocolBufferException {
                                return new PropertyValueExtension(input, extensionRegistry);
                            }
                        };

                @java.lang.Override
                public com.google.protobuf.Parser<PropertyValueExtension> getParserForType() {
                    return PARSER;
                }

                private void initFields() {
                }
                private byte memoizedIsInitialized = -1;
                public final boolean isInitialized() {
                    byte isInitialized = memoizedIsInitialized;
                    if (isInitialized == 1) return true;
                    if (isInitialized == 0) return false;

                    if (!extensionsAreInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                    memoizedIsInitialized = 1;
                    return true;
                }

                public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
                    getSerializedSize();
                    com.google.protobuf.GeneratedMessage
                            .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension>.ExtensionWriter extensionWriter =
                            newExtensionWriter();
                    extensionWriter.writeUntil(536870912, output);
                    getUnknownFields().writeTo(output);
                }

                private int memoizedSerializedSize = -1;
                public int getSerializedSize() {
                    int size = memoizedSerializedSize;
                    if (size != -1) return size;

                    size = 0;
                    size += extensionsSerializedSize();
                    size += getUnknownFields().getSerializedSize();
                    memoizedSerializedSize = size;
                    return size;
                }

                private static final long serialVersionUID = 0L;
                @java.lang.Override
                protected java.lang.Object writeReplace()
                        throws java.io.ObjectStreamException {
                    return super.writeReplace();
                }

                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        com.google.protobuf.ByteString data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        com.google.protobuf.ByteString data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(byte[] data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        byte[] data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseDelimitedFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseDelimitedFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        com.google.protobuf.CodedInputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parseFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }

                public static Builder newBuilder() { return Builder.create(); }
                public Builder newBuilderForType() { return newBuilder(); }
                public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension prototype) {
                    return newBuilder().mergeFrom(prototype);
                }
                public Builder toBuilder() { return newBuilder(this); }

                @java.lang.Override
                protected Builder newBuilderForType(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    Builder builder = new Builder(parent);
                    return builder;
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension}
                 */
                public static final class Builder extends
                        com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension, Builder> implements
                        // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension)
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder {
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder.class);
                    }

                    // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.newBuilder()
                    private Builder() {
                        maybeForceBuilderInitialization();
                    }

                    private Builder(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        super(parent);
                        maybeForceBuilderInitialization();
                    }
                    private void maybeForceBuilderInitialization() {
                        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        }
                    }
                    private static Builder create() {
                        return new Builder();
                    }

                    public Builder clear() {
                        super.clear();
                        return this;
                    }

                    public Builder clone() {
                        return create().mergeFrom(buildPartial());
                    }

                    public com.google.protobuf.Descriptors.Descriptor
                    getDescriptorForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension getDefaultInstanceForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension build() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension result = buildPartial();
                        if (!result.isInitialized()) {
                            throw newUninitializedMessageException(result);
                        }
                        return result;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension buildPartial() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension(this);
                        onBuilt();
                        return result;
                    }

                    public Builder mergeFrom(com.google.protobuf.Message other) {
                        if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) {
                            return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension)other);
                        } else {
                            super.mergeFrom(other);
                            return this;
                        }
                    }

                    public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension other) {
                        if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance()) return this;
                        this.mergeExtensionFields(other);
                        this.mergeUnknownFields(other.getUnknownFields());
                        return this;
                    }

                    public final boolean isInitialized() {
                        if (!extensionsAreInitialized()) {

                            return false;
                        }
                        return true;
                    }

                    public Builder mergeFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension parsedMessage = null;
                        try {
                            parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) e.getUnfinishedMessage();
                            throw e;
                        } finally {
                            if (parsedMessage != null) {
                                mergeFrom(parsedMessage);
                            }
                        }
                        return this;
                    }

                    // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension)
                }

                static {
                    defaultInstance = new PropertyValueExtension(true);
                    defaultInstance.initFields();
                }

                // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension)
            }

            private int bitField0_;
            private int valueCase_ = 0;
            private java.lang.Object value_;
            public enum ValueCase
                    implements com.google.protobuf.Internal.EnumLite {
                INT_VALUE(3),
                LONG_VALUE(4),
                FLOAT_VALUE(5),
                DOUBLE_VALUE(6),
                BOOLEAN_VALUE(7),
                STRING_VALUE(8),
                PROPERTYSET_VALUE(9),
                PROPERTYSETS_VALUE(10),
                EXTENSION_VALUE(11),
                VALUE_NOT_SET(0);
                private int value = 0;
                private ValueCase(int value) {
                    this.value = value;
                }
                public static ValueCase valueOf(int value) {
                    switch (value) {
                        case 3: return INT_VALUE;
                        case 4: return LONG_VALUE;
                        case 5: return FLOAT_VALUE;
                        case 6: return DOUBLE_VALUE;
                        case 7: return BOOLEAN_VALUE;
                        case 8: return STRING_VALUE;
                        case 9: return PROPERTYSET_VALUE;
                        case 10: return PROPERTYSETS_VALUE;
                        case 11: return EXTENSION_VALUE;
                        case 0: return VALUE_NOT_SET;
                        default: throw new java.lang.IllegalArgumentException(
                                "Value is undefined for this oneof enum.");
                    }
                }
                public int getNumber() {
                    return this.value;
                }
            };

            public ValueCase
            getValueCase() {
                return ValueCase.valueOf(
                        valueCase_);
            }

            public static final int TYPE_FIELD_NUMBER = 1;
            private int type_;
            /**
             * <code>optional uint32 type = 1;</code>
             */
            public boolean hasType() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional uint32 type = 1;</code>
             */
            public int getType() {
                return type_;
            }

            public static final int IS_NULL_FIELD_NUMBER = 2;
            private boolean isNull_;
            /**
             * <code>optional bool is_null = 2;</code>
             */
            public boolean hasIsNull() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }
            /**
             * <code>optional bool is_null = 2;</code>
             */
            public boolean getIsNull() {
                return isNull_;
            }

            public static final int INT_VALUE_FIELD_NUMBER = 3;
            /**
             * <code>optional uint32 int_value = 3;</code>
             */
            public boolean hasIntValue() {
                return valueCase_ == 3;
            }
            /**
             * <code>optional uint32 int_value = 3;</code>
             */
            public int getIntValue() {
                if (valueCase_ == 3) {
                    return (java.lang.Integer) value_;
                }
                return 0;
            }

            public static final int LONG_VALUE_FIELD_NUMBER = 4;
            /**
             * <code>optional uint64 long_value = 4;</code>
             */
            public boolean hasLongValue() {
                return valueCase_ == 4;
            }
            /**
             * <code>optional uint64 long_value = 4;</code>
             */
            public long getLongValue() {
                if (valueCase_ == 4) {
                    return (java.lang.Long) value_;
                }
                return 0L;
            }

            public static final int FLOAT_VALUE_FIELD_NUMBER = 5;
            /**
             * <code>optional float float_value = 5;</code>
             */
            public boolean hasFloatValue() {
                return valueCase_ == 5;
            }
            /**
             * <code>optional float float_value = 5;</code>
             */
            public float getFloatValue() {
                if (valueCase_ == 5) {
                    return (java.lang.Float) value_;
                }
                return 0F;
            }

            public static final int DOUBLE_VALUE_FIELD_NUMBER = 6;
            /**
             * <code>optional double double_value = 6;</code>
             */
            public boolean hasDoubleValue() {
                return valueCase_ == 6;
            }
            /**
             * <code>optional double double_value = 6;</code>
             */
            public double getDoubleValue() {
                if (valueCase_ == 6) {
                    return (java.lang.Double) value_;
                }
                return 0D;
            }

            public static final int BOOLEAN_VALUE_FIELD_NUMBER = 7;
            /**
             * <code>optional bool boolean_value = 7;</code>
             */
            public boolean hasBooleanValue() {
                return valueCase_ == 7;
            }
            /**
             * <code>optional bool boolean_value = 7;</code>
             */
            public boolean getBooleanValue() {
                if (valueCase_ == 7) {
                    return (java.lang.Boolean) value_;
                }
                return false;
            }

            public static final int STRING_VALUE_FIELD_NUMBER = 8;
            /**
             * <code>optional string string_value = 8;</code>
             */
            public boolean hasStringValue() {
                return valueCase_ == 8;
            }
            /**
             * <code>optional string string_value = 8;</code>
             */
            public java.lang.String getStringValue() {
                java.lang.Object ref = "";
                if (valueCase_ == 8) {
                    ref = value_;
                }
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8() && (valueCase_ == 8)) {
                        value_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string string_value = 8;</code>
             */
            public com.google.protobuf.ByteString
            getStringValueBytes() {
                java.lang.Object ref = "";
                if (valueCase_ == 8) {
                    ref = value_;
                }
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    if (valueCase_ == 8) {
                        value_ = b;
                    }
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int PROPERTYSET_VALUE_FIELD_NUMBER = 9;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            public boolean hasPropertysetValue() {
                return valueCase_ == 9;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertysetValue() {
                if (valueCase_ == 9) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetValueOrBuilder() {
                if (valueCase_ == 9) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
            }

            public static final int PROPERTYSETS_VALUE_FIELD_NUMBER = 10;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            public boolean hasPropertysetsValue() {
                return valueCase_ == 10;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList getPropertysetsValue() {
                if (valueCase_ == 10) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
             *
             * <pre>
             * List of Property Values
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder getPropertysetsValueOrBuilder() {
                if (valueCase_ == 10) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
            }

            public static final int EXTENSION_VALUE_FIELD_NUMBER = 11;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            public boolean hasExtensionValue() {
                return valueCase_ == 11;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension getExtensionValue() {
                if (valueCase_ == 11) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder getExtensionValueOrBuilder() {
                if (valueCase_ == 11) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
            }

            private void initFields() {
                type_ = 0;
                isNull_ = false;
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                if (hasPropertysetValue()) {
                    if (!getPropertysetValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasPropertysetsValue()) {
                    if (!getPropertysetsValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasExtensionValue()) {
                    if (!getExtensionValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    output.writeUInt32(1, type_);
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    output.writeBool(2, isNull_);
                }
                if (valueCase_ == 3) {
                    output.writeUInt32(
                            3, (int)((java.lang.Integer) value_));
                }
                if (valueCase_ == 4) {
                    output.writeUInt64(
                            4, (long)((java.lang.Long) value_));
                }
                if (valueCase_ == 5) {
                    output.writeFloat(
                            5, (float)((java.lang.Float) value_));
                }
                if (valueCase_ == 6) {
                    output.writeDouble(
                            6, (double)((java.lang.Double) value_));
                }
                if (valueCase_ == 7) {
                    output.writeBool(
                            7, (boolean)((java.lang.Boolean) value_));
                }
                if (valueCase_ == 8) {
                    output.writeBytes(8, getStringValueBytes());
                }
                if (valueCase_ == 9) {
                    output.writeMessage(9, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_);
                }
                if (valueCase_ == 10) {
                    output.writeMessage(10, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_);
                }
                if (valueCase_ == 11) {
                    output.writeMessage(11, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_);
                }
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt32Size(1, type_);
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(2, isNull_);
                }
                if (valueCase_ == 3) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt32Size(
                                    3, (int)((java.lang.Integer) value_));
                }
                if (valueCase_ == 4) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(
                                    4, (long)((java.lang.Long) value_));
                }
                if (valueCase_ == 5) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeFloatSize(
                                    5, (float)((java.lang.Float) value_));
                }
                if (valueCase_ == 6) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeDoubleSize(
                                    6, (double)((java.lang.Double) value_));
                }
                if (valueCase_ == 7) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(
                                    7, (boolean)((java.lang.Boolean) value_));
                }
                if (valueCase_ == 8) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(8, getStringValueBytes());
                }
                if (valueCase_ == 9) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(9, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_);
                }
                if (valueCase_ == 10) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(10, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_);
                }
                if (valueCase_ == 11) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(11, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_);
                }
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    type_ = 0;
                    bitField0_ = (bitField0_ & ~0x00000001);
                    isNull_ = false;
                    bitField0_ = (bitField0_ & ~0x00000002);
                    valueCase_ = 0;
                    value_ = null;
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue(this);
                    int from_bitField0_ = bitField0_;
                    int to_bitField0_ = 0;
                    if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                        to_bitField0_ |= 0x00000001;
                    }
                    result.type_ = type_;
                    if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
                        to_bitField0_ |= 0x00000002;
                    }
                    result.isNull_ = isNull_;
                    if (valueCase_ == 3) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 4) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 5) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 6) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 7) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 8) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 9) {
                        if (propertysetValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = propertysetValueBuilder_.build();
                        }
                    }
                    if (valueCase_ == 10) {
                        if (propertysetsValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = propertysetsValueBuilder_.build();
                        }
                    }
                    if (valueCase_ == 11) {
                        if (extensionValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = extensionValueBuilder_.build();
                        }
                    }
                    result.bitField0_ = to_bitField0_;
                    result.valueCase_ = valueCase_;
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.getDefaultInstance()) return this;
                    if (other.hasType()) {
                        setType(other.getType());
                    }
                    if (other.hasIsNull()) {
                        setIsNull(other.getIsNull());
                    }
                    switch (other.getValueCase()) {
                        case INT_VALUE: {
                            setIntValue(other.getIntValue());
                            break;
                        }
                        case LONG_VALUE: {
                            setLongValue(other.getLongValue());
                            break;
                        }
                        case FLOAT_VALUE: {
                            setFloatValue(other.getFloatValue());
                            break;
                        }
                        case DOUBLE_VALUE: {
                            setDoubleValue(other.getDoubleValue());
                            break;
                        }
                        case BOOLEAN_VALUE: {
                            setBooleanValue(other.getBooleanValue());
                            break;
                        }
                        case STRING_VALUE: {
                            valueCase_ = 8;
                            value_ = other.value_;
                            onChanged();
                            break;
                        }
                        case PROPERTYSET_VALUE: {
                            mergePropertysetValue(other.getPropertysetValue());
                            break;
                        }
                        case PROPERTYSETS_VALUE: {
                            mergePropertysetsValue(other.getPropertysetsValue());
                            break;
                        }
                        case EXTENSION_VALUE: {
                            mergeExtensionValue(other.getExtensionValue());
                            break;
                        }
                        case VALUE_NOT_SET: {
                            break;
                        }
                    }
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    if (hasPropertysetValue()) {
                        if (!getPropertysetValue().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasPropertysetsValue()) {
                        if (!getPropertysetsValue().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasExtensionValue()) {
                        if (!getExtensionValue().isInitialized()) {

                            return false;
                        }
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int valueCase_ = 0;
                private java.lang.Object value_;
                public ValueCase
                getValueCase() {
                    return ValueCase.valueOf(
                            valueCase_);
                }

                public Builder clearValue() {
                    valueCase_ = 0;
                    value_ = null;
                    onChanged();
                    return this;
                }

                private int bitField0_;

                private int type_ ;
                /**
                 * <code>optional uint32 type = 1;</code>
                 */
                public boolean hasType() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional uint32 type = 1;</code>
                 */
                public int getType() {
                    return type_;
                }
                /**
                 * <code>optional uint32 type = 1;</code>
                 */
                public Builder setType(int value) {
                    bitField0_ |= 0x00000001;
                    type_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint32 type = 1;</code>
                 */
                public Builder clearType() {
                    bitField0_ = (bitField0_ & ~0x00000001);
                    type_ = 0;
                    onChanged();
                    return this;
                }

                private boolean isNull_ ;
                /**
                 * <code>optional bool is_null = 2;</code>
                 */
                public boolean hasIsNull() {
                    return ((bitField0_ & 0x00000002) == 0x00000002);
                }
                /**
                 * <code>optional bool is_null = 2;</code>
                 */
                public boolean getIsNull() {
                    return isNull_;
                }
                /**
                 * <code>optional bool is_null = 2;</code>
                 */
                public Builder setIsNull(boolean value) {
                    bitField0_ |= 0x00000002;
                    isNull_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_null = 2;</code>
                 */
                public Builder clearIsNull() {
                    bitField0_ = (bitField0_ & ~0x00000002);
                    isNull_ = false;
                    onChanged();
                    return this;
                }

                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public boolean hasIntValue() {
                    return valueCase_ == 3;
                }
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public int getIntValue() {
                    if (valueCase_ == 3) {
                        return (java.lang.Integer) value_;
                    }
                    return 0;
                }
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public Builder setIntValue(int value) {
                    valueCase_ = 3;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint32 int_value = 3;</code>
                 */
                public Builder clearIntValue() {
                    if (valueCase_ == 3) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public boolean hasLongValue() {
                    return valueCase_ == 4;
                }
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public long getLongValue() {
                    if (valueCase_ == 4) {
                        return (java.lang.Long) value_;
                    }
                    return 0L;
                }
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public Builder setLongValue(long value) {
                    valueCase_ = 4;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 long_value = 4;</code>
                 */
                public Builder clearLongValue() {
                    if (valueCase_ == 4) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public boolean hasFloatValue() {
                    return valueCase_ == 5;
                }
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public float getFloatValue() {
                    if (valueCase_ == 5) {
                        return (java.lang.Float) value_;
                    }
                    return 0F;
                }
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public Builder setFloatValue(float value) {
                    valueCase_ = 5;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional float float_value = 5;</code>
                 */
                public Builder clearFloatValue() {
                    if (valueCase_ == 5) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public boolean hasDoubleValue() {
                    return valueCase_ == 6;
                }
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public double getDoubleValue() {
                    if (valueCase_ == 6) {
                        return (java.lang.Double) value_;
                    }
                    return 0D;
                }
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public Builder setDoubleValue(double value) {
                    valueCase_ = 6;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional double double_value = 6;</code>
                 */
                public Builder clearDoubleValue() {
                    if (valueCase_ == 6) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public boolean hasBooleanValue() {
                    return valueCase_ == 7;
                }
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public boolean getBooleanValue() {
                    if (valueCase_ == 7) {
                        return (java.lang.Boolean) value_;
                    }
                    return false;
                }
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public Builder setBooleanValue(boolean value) {
                    valueCase_ = 7;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool boolean_value = 7;</code>
                 */
                public Builder clearBooleanValue() {
                    if (valueCase_ == 7) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public boolean hasStringValue() {
                    return valueCase_ == 8;
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public java.lang.String getStringValue() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 8) {
                        ref = value_;
                    }
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (valueCase_ == 8) {
                            if (bs.isValidUtf8()) {
                                value_ = s;
                            }
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public com.google.protobuf.ByteString
                getStringValueBytes() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 8) {
                        ref = value_;
                    }
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        if (valueCase_ == 8) {
                            value_ = b;
                        }
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public Builder setStringValue(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    valueCase_ = 8;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public Builder clearStringValue() {
                    if (valueCase_ == 8) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }
                /**
                 * <code>optional string string_value = 8;</code>
                 */
                public Builder setStringValueBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    valueCase_ = 8;
                    value_ = value;
                    onChanged();
                    return this;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder> propertysetValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public boolean hasPropertysetValue() {
                    return valueCase_ == 9;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertysetValue() {
                    if (propertysetValueBuilder_ == null) {
                        if (valueCase_ == 9) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                    } else {
                        if (valueCase_ == 9) {
                            return propertysetValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public Builder setPropertysetValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertysetValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        propertysetValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 9;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public Builder setPropertysetValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder builderForValue) {
                    if (propertysetValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        propertysetValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 9;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public Builder mergePropertysetValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertysetValueBuilder_ == null) {
                        if (valueCase_ == 9 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 9) {
                            propertysetValueBuilder_.mergeFrom(value);
                        }
                        propertysetValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 9;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public Builder clearPropertysetValue() {
                    if (propertysetValueBuilder_ == null) {
                        if (valueCase_ == 9) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 9) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        propertysetValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder getPropertysetValueBuilder() {
                    return getPropertysetValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetValueOrBuilder() {
                    if ((valueCase_ == 9) && (propertysetValueBuilder_ != null)) {
                        return propertysetValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 9) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset_value = 9;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
                getPropertysetValueFieldBuilder() {
                    if (propertysetValueBuilder_ == null) {
                        if (!(valueCase_ == 9)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                        }
                        propertysetValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 9;
                    return propertysetValueBuilder_;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder> propertysetsValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public boolean hasPropertysetsValue() {
                    return valueCase_ == 10;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList getPropertysetsValue() {
                    if (propertysetsValueBuilder_ == null) {
                        if (valueCase_ == 10) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
                    } else {
                        if (valueCase_ == 10) {
                            return propertysetsValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public Builder setPropertysetsValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList value) {
                    if (propertysetsValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        propertysetsValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 10;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public Builder setPropertysetsValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder builderForValue) {
                    if (propertysetsValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        propertysetsValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 10;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public Builder mergePropertysetsValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList value) {
                    if (propertysetsValueBuilder_ == null) {
                        if (valueCase_ == 10 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 10) {
                            propertysetsValueBuilder_.mergeFrom(value);
                        }
                        propertysetsValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 10;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public Builder clearPropertysetsValue() {
                    if (propertysetsValueBuilder_ == null) {
                        if (valueCase_ == 10) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 10) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        propertysetsValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder getPropertysetsValueBuilder() {
                    return getPropertysetsValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder getPropertysetsValueOrBuilder() {
                    if ((valueCase_ == 10) && (propertysetsValueBuilder_ != null)) {
                        return propertysetsValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 10) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList propertysets_value = 10;</code>
                 *
                 * <pre>
                 * List of Property Values
                 * </pre>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder>
                getPropertysetsValueFieldBuilder() {
                    if (propertysetsValueBuilder_ == null) {
                        if (!(valueCase_ == 10)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
                        }
                        propertysetsValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 10;
                    return propertysetsValueBuilder_;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder> extensionValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public boolean hasExtensionValue() {
                    return valueCase_ == 11;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension getExtensionValue() {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 11) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
                    } else {
                        if (valueCase_ == 11) {
                            return extensionValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public Builder setExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension value) {
                    if (extensionValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        extensionValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 11;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public Builder setExtensionValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder builderForValue) {
                    if (extensionValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        extensionValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 11;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public Builder mergeExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension value) {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 11 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 11) {
                            extensionValueBuilder_.mergeFrom(value);
                        }
                        extensionValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 11;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public Builder clearExtensionValue() {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 11) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 11) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        extensionValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder getExtensionValueBuilder() {
                    return getExtensionValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder getExtensionValueOrBuilder() {
                    if ((valueCase_ == 11) && (extensionValueBuilder_ != null)) {
                        return extensionValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 11) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue.PropertyValueExtension extension_value = 11;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder>
                getExtensionValueFieldBuilder() {
                    if (extensionValueBuilder_ == null) {
                        if (!(valueCase_ == 11)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.getDefaultInstance();
                        }
                        extensionValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtensionOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PropertyValueExtension) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 11;
                    return extensionValueBuilder_;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue)
            }

            static {
                defaultInstance = new PropertyValue(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue)
        }

        public interface PropertySetOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet)
                com.google.protobuf.GeneratedMessage.
                        ExtendableMessageOrBuilder<PropertySet> {

            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            com.google.protobuf.ProtocolStringList
            getKeysList();
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            int getKeysCount();
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            java.lang.String getKeys(int index);
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            com.google.protobuf.ByteString
            getKeysBytes(int index);

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue>
            getValuesList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue getValues(int index);
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            int getValuesCount();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder>
            getValuesOrBuilderList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder getValuesOrBuilder(
                    int index);
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet}
         */
        public static final class PropertySet extends
                com.google.protobuf.GeneratedMessage.ExtendableMessage<
                        PropertySet> implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet)
                PropertySetOrBuilder {
            // Use PropertySet.newBuilder() to construct.
            private PropertySet(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, ?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private PropertySet(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final PropertySet defaultInstance;
            public static PropertySet getDefaultInstance() {
                return defaultInstance;
            }

            public PropertySet getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private PropertySet(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 10: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                                    keys_ = new com.google.protobuf.LazyStringArrayList();
                                    mutable_bitField0_ |= 0x00000001;
                                }
                                keys_.add(bs);
                                break;
                            }
                            case 18: {
                                if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                                    values_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue>();
                                    mutable_bitField0_ |= 0x00000002;
                                }
                                values_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.PARSER, extensionRegistry));
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                        keys_ = keys_.getUnmodifiableView();
                    }
                    if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
                        values_ = java.util.Collections.unmodifiableList(values_);
                    }
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder.class);
            }

            public static com.google.protobuf.Parser<PropertySet> PARSER =
                    new com.google.protobuf.AbstractParser<PropertySet>() {
                        public PropertySet parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new PropertySet(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<PropertySet> getParserForType() {
                return PARSER;
            }

            public static final int KEYS_FIELD_NUMBER = 1;
            private com.google.protobuf.LazyStringList keys_;
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            public com.google.protobuf.ProtocolStringList
            getKeysList() {
                return keys_;
            }
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            public int getKeysCount() {
                return keys_.size();
            }
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            public java.lang.String getKeys(int index) {
                return keys_.get(index);
            }
            /**
             * <code>repeated string keys = 1;</code>
             *
             * <pre>
             * Names of the properties
             * </pre>
             */
            public com.google.protobuf.ByteString
            getKeysBytes(int index) {
                return keys_.getByteString(index);
            }

            public static final int VALUES_FIELD_NUMBER = 2;
            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue> values_;
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue> getValuesList() {
                return values_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder>
            getValuesOrBuilderList() {
                return values_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            public int getValuesCount() {
                return values_.size();
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue getValues(int index) {
                return values_.get(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder getValuesOrBuilder(
                    int index) {
                return values_.get(index);
            }

            private void initFields() {
                keys_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                values_ = java.util.Collections.emptyList();
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                for (int i = 0; i < getValuesCount(); i++) {
                    if (!getValues(i).isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (!extensionsAreInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                com.google.protobuf.GeneratedMessage
                        .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet>.ExtensionWriter extensionWriter =
                        newExtensionWriter();
                for (int i = 0; i < keys_.size(); i++) {
                    output.writeBytes(1, keys_.getByteString(i));
                }
                for (int i = 0; i < values_.size(); i++) {
                    output.writeMessage(2, values_.get(i));
                }
                extensionWriter.writeUntil(536870912, output);
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                {
                    int dataSize = 0;
                    for (int i = 0; i < keys_.size(); i++) {
                        dataSize += com.google.protobuf.CodedOutputStream
                                .computeBytesSizeNoTag(keys_.getByteString(i));
                    }
                    size += dataSize;
                    size += 1 * getKeysList().size();
                }
                for (int i = 0; i < values_.size(); i++) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(2, values_.get(i));
                }
                size += extensionsSerializedSize();
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        getValuesFieldBuilder();
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    keys_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                    bitField0_ = (bitField0_ & ~0x00000001);
                    if (valuesBuilder_ == null) {
                        values_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000002);
                    } else {
                        valuesBuilder_.clear();
                    }
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet(this);
                    int from_bitField0_ = bitField0_;
                    if (((bitField0_ & 0x00000001) == 0x00000001)) {
                        keys_ = keys_.getUnmodifiableView();
                        bitField0_ = (bitField0_ & ~0x00000001);
                    }
                    result.keys_ = keys_;
                    if (valuesBuilder_ == null) {
                        if (((bitField0_ & 0x00000002) == 0x00000002)) {
                            values_ = java.util.Collections.unmodifiableList(values_);
                            bitField0_ = (bitField0_ & ~0x00000002);
                        }
                        result.values_ = values_;
                    } else {
                        result.values_ = valuesBuilder_.build();
                    }
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance()) return this;
                    if (!other.keys_.isEmpty()) {
                        if (keys_.isEmpty()) {
                            keys_ = other.keys_;
                            bitField0_ = (bitField0_ & ~0x00000001);
                        } else {
                            ensureKeysIsMutable();
                            keys_.addAll(other.keys_);
                        }
                        onChanged();
                    }
                    if (valuesBuilder_ == null) {
                        if (!other.values_.isEmpty()) {
                            if (values_.isEmpty()) {
                                values_ = other.values_;
                                bitField0_ = (bitField0_ & ~0x00000002);
                            } else {
                                ensureValuesIsMutable();
                                values_.addAll(other.values_);
                            }
                            onChanged();
                        }
                    } else {
                        if (!other.values_.isEmpty()) {
                            if (valuesBuilder_.isEmpty()) {
                                valuesBuilder_.dispose();
                                valuesBuilder_ = null;
                                values_ = other.values_;
                                bitField0_ = (bitField0_ & ~0x00000002);
                                valuesBuilder_ =
                                        com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                getValuesFieldBuilder() : null;
                            } else {
                                valuesBuilder_.addAllMessages(other.values_);
                            }
                        }
                    }
                    this.mergeExtensionFields(other);
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    for (int i = 0; i < getValuesCount(); i++) {
                        if (!getValues(i).isInitialized()) {

                            return false;
                        }
                    }
                    if (!extensionsAreInitialized()) {

                        return false;
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int bitField0_;

                private com.google.protobuf.LazyStringList keys_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                private void ensureKeysIsMutable() {
                    if (!((bitField0_ & 0x00000001) == 0x00000001)) {
                        keys_ = new com.google.protobuf.LazyStringArrayList(keys_);
                        bitField0_ |= 0x00000001;
                    }
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public com.google.protobuf.ProtocolStringList
                getKeysList() {
                    return keys_.getUnmodifiableView();
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public int getKeysCount() {
                    return keys_.size();
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public java.lang.String getKeys(int index) {
                    return keys_.get(index);
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getKeysBytes(int index) {
                    return keys_.getByteString(index);
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public Builder setKeys(
                        int index, java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureKeysIsMutable();
                    keys_.set(index, value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public Builder addKeys(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureKeysIsMutable();
                    keys_.add(value);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public Builder addAllKeys(
                        java.lang.Iterable<java.lang.String> values) {
                    ensureKeysIsMutable();
                    com.google.protobuf.AbstractMessageLite.Builder.addAll(
                            values, keys_);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public Builder clearKeys() {
                    keys_ = com.google.protobuf.LazyStringArrayList.EMPTY;
                    bitField0_ = (bitField0_ & ~0x00000001);
                    onChanged();
                    return this;
                }
                /**
                 * <code>repeated string keys = 1;</code>
                 *
                 * <pre>
                 * Names of the properties
                 * </pre>
                 */
                public Builder addKeysBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureKeysIsMutable();
                    keys_.add(value);
                    onChanged();
                    return this;
                }

                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue> values_ =
                        java.util.Collections.emptyList();
                private void ensureValuesIsMutable() {
                    if (!((bitField0_ & 0x00000002) == 0x00000002)) {
                        values_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue>(values_);
                        bitField0_ |= 0x00000002;
                    }
                }

                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder> valuesBuilder_;

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue> getValuesList() {
                    if (valuesBuilder_ == null) {
                        return java.util.Collections.unmodifiableList(values_);
                    } else {
                        return valuesBuilder_.getMessageList();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public int getValuesCount() {
                    if (valuesBuilder_ == null) {
                        return values_.size();
                    } else {
                        return valuesBuilder_.getCount();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue getValues(int index) {
                    if (valuesBuilder_ == null) {
                        return values_.get(index);
                    } else {
                        return valuesBuilder_.getMessage(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder setValues(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue value) {
                    if (valuesBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureValuesIsMutable();
                        values_.set(index, value);
                        onChanged();
                    } else {
                        valuesBuilder_.setMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder setValues(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder builderForValue) {
                    if (valuesBuilder_ == null) {
                        ensureValuesIsMutable();
                        values_.set(index, builderForValue.build());
                        onChanged();
                    } else {
                        valuesBuilder_.setMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder addValues(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue value) {
                    if (valuesBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureValuesIsMutable();
                        values_.add(value);
                        onChanged();
                    } else {
                        valuesBuilder_.addMessage(value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder addValues(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue value) {
                    if (valuesBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensureValuesIsMutable();
                        values_.add(index, value);
                        onChanged();
                    } else {
                        valuesBuilder_.addMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder addValues(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder builderForValue) {
                    if (valuesBuilder_ == null) {
                        ensureValuesIsMutable();
                        values_.add(builderForValue.build());
                        onChanged();
                    } else {
                        valuesBuilder_.addMessage(builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder addValues(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder builderForValue) {
                    if (valuesBuilder_ == null) {
                        ensureValuesIsMutable();
                        values_.add(index, builderForValue.build());
                        onChanged();
                    } else {
                        valuesBuilder_.addMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder addAllValues(
                        java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue> values) {
                    if (valuesBuilder_ == null) {
                        ensureValuesIsMutable();
                        com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                values, values_);
                        onChanged();
                    } else {
                        valuesBuilder_.addAllMessages(values);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder clearValues() {
                    if (valuesBuilder_ == null) {
                        values_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000002);
                        onChanged();
                    } else {
                        valuesBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public Builder removeValues(int index) {
                    if (valuesBuilder_ == null) {
                        ensureValuesIsMutable();
                        values_.remove(index);
                        onChanged();
                    } else {
                        valuesBuilder_.remove(index);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder getValuesBuilder(
                        int index) {
                    return getValuesFieldBuilder().getBuilder(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder getValuesOrBuilder(
                        int index) {
                    if (valuesBuilder_ == null) {
                        return values_.get(index);  } else {
                        return valuesBuilder_.getMessageOrBuilder(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder>
                getValuesOrBuilderList() {
                    if (valuesBuilder_ != null) {
                        return valuesBuilder_.getMessageOrBuilderList();
                    } else {
                        return java.util.Collections.unmodifiableList(values_);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder addValuesBuilder() {
                    return getValuesFieldBuilder().addBuilder(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder addValuesBuilder(
                        int index) {
                    return getValuesFieldBuilder().addBuilder(
                            index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertyValue values = 2;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder>
                getValuesBuilderList() {
                    return getValuesFieldBuilder().getBuilderList();
                }
                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder>
                getValuesFieldBuilder() {
                    if (valuesBuilder_ == null) {
                        valuesBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValue.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertyValueOrBuilder>(
                                values_,
                                ((bitField0_ & 0x00000002) == 0x00000002),
                                getParentForChildren(),
                                isClean());
                        values_ = null;
                    }
                    return valuesBuilder_;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet)
            }

            static {
                defaultInstance = new PropertySet(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet)
        }

        public interface PropertySetListOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList)
                com.google.protobuf.GeneratedMessage.
                        ExtendableMessageOrBuilder<PropertySetList> {

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet>
            getPropertysetList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertyset(int index);
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            int getPropertysetCount();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
            getPropertysetOrBuilderList();
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetOrBuilder(
                    int index);
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList}
         */
        public static final class PropertySetList extends
                com.google.protobuf.GeneratedMessage.ExtendableMessage<
                        PropertySetList> implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList)
                PropertySetListOrBuilder {
            // Use PropertySetList.newBuilder() to construct.
            private PropertySetList(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList, ?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private PropertySetList(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final PropertySetList defaultInstance;
            public static PropertySetList getDefaultInstance() {
                return defaultInstance;
            }

            public PropertySetList getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private PropertySetList(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 10: {
                                if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                                    propertyset_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet>();
                                    mutable_bitField0_ |= 0x00000001;
                                }
                                propertyset_.add(input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.PARSER, extensionRegistry));
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                        propertyset_ = java.util.Collections.unmodifiableList(propertyset_);
                    }
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder.class);
            }

            public static com.google.protobuf.Parser<PropertySetList> PARSER =
                    new com.google.protobuf.AbstractParser<PropertySetList>() {
                        public PropertySetList parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new PropertySetList(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<PropertySetList> getParserForType() {
                return PARSER;
            }

            public static final int PROPERTYSET_FIELD_NUMBER = 1;
            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet> propertyset_;
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet> getPropertysetList() {
                return propertyset_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
            getPropertysetOrBuilderList() {
                return propertyset_;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            public int getPropertysetCount() {
                return propertyset_.size();
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertyset(int index) {
                return propertyset_.get(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetOrBuilder(
                    int index) {
                return propertyset_.get(index);
            }

            private void initFields() {
                propertyset_ = java.util.Collections.emptyList();
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                for (int i = 0; i < getPropertysetCount(); i++) {
                    if (!getPropertyset(i).isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (!extensionsAreInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                com.google.protobuf.GeneratedMessage
                        .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList>.ExtensionWriter extensionWriter =
                        newExtensionWriter();
                for (int i = 0; i < propertyset_.size(); i++) {
                    output.writeMessage(1, propertyset_.get(i));
                }
                extensionWriter.writeUntil(536870912, output);
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                for (int i = 0; i < propertyset_.size(); i++) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(1, propertyset_.get(i));
                }
                size += extensionsSerializedSize();
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList, Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetListOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        getPropertysetFieldBuilder();
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    if (propertysetBuilder_ == null) {
                        propertyset_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000001);
                    } else {
                        propertysetBuilder_.clear();
                    }
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList(this);
                    int from_bitField0_ = bitField0_;
                    if (propertysetBuilder_ == null) {
                        if (((bitField0_ & 0x00000001) == 0x00000001)) {
                            propertyset_ = java.util.Collections.unmodifiableList(propertyset_);
                            bitField0_ = (bitField0_ & ~0x00000001);
                        }
                        result.propertyset_ = propertyset_;
                    } else {
                        result.propertyset_ = propertysetBuilder_.build();
                    }
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList.getDefaultInstance()) return this;
                    if (propertysetBuilder_ == null) {
                        if (!other.propertyset_.isEmpty()) {
                            if (propertyset_.isEmpty()) {
                                propertyset_ = other.propertyset_;
                                bitField0_ = (bitField0_ & ~0x00000001);
                            } else {
                                ensurePropertysetIsMutable();
                                propertyset_.addAll(other.propertyset_);
                            }
                            onChanged();
                        }
                    } else {
                        if (!other.propertyset_.isEmpty()) {
                            if (propertysetBuilder_.isEmpty()) {
                                propertysetBuilder_.dispose();
                                propertysetBuilder_ = null;
                                propertyset_ = other.propertyset_;
                                bitField0_ = (bitField0_ & ~0x00000001);
                                propertysetBuilder_ =
                                        com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                                getPropertysetFieldBuilder() : null;
                            } else {
                                propertysetBuilder_.addAllMessages(other.propertyset_);
                            }
                        }
                    }
                    this.mergeExtensionFields(other);
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    for (int i = 0; i < getPropertysetCount(); i++) {
                        if (!getPropertyset(i).isInitialized()) {

                            return false;
                        }
                    }
                    if (!extensionsAreInitialized()) {

                        return false;
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetList) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int bitField0_;

                private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet> propertyset_ =
                        java.util.Collections.emptyList();
                private void ensurePropertysetIsMutable() {
                    if (!((bitField0_ & 0x00000001) == 0x00000001)) {
                        propertyset_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet>(propertyset_);
                        bitField0_ |= 0x00000001;
                    }
                }

                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder> propertysetBuilder_;

                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet> getPropertysetList() {
                    if (propertysetBuilder_ == null) {
                        return java.util.Collections.unmodifiableList(propertyset_);
                    } else {
                        return propertysetBuilder_.getMessageList();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public int getPropertysetCount() {
                    if (propertysetBuilder_ == null) {
                        return propertyset_.size();
                    } else {
                        return propertysetBuilder_.getCount();
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getPropertyset(int index) {
                    if (propertysetBuilder_ == null) {
                        return propertyset_.get(index);
                    } else {
                        return propertysetBuilder_.getMessage(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder setPropertyset(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertysetBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensurePropertysetIsMutable();
                        propertyset_.set(index, value);
                        onChanged();
                    } else {
                        propertysetBuilder_.setMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder setPropertyset(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder builderForValue) {
                    if (propertysetBuilder_ == null) {
                        ensurePropertysetIsMutable();
                        propertyset_.set(index, builderForValue.build());
                        onChanged();
                    } else {
                        propertysetBuilder_.setMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder addPropertyset(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertysetBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensurePropertysetIsMutable();
                        propertyset_.add(value);
                        onChanged();
                    } else {
                        propertysetBuilder_.addMessage(value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder addPropertyset(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertysetBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        ensurePropertysetIsMutable();
                        propertyset_.add(index, value);
                        onChanged();
                    } else {
                        propertysetBuilder_.addMessage(index, value);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder addPropertyset(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder builderForValue) {
                    if (propertysetBuilder_ == null) {
                        ensurePropertysetIsMutable();
                        propertyset_.add(builderForValue.build());
                        onChanged();
                    } else {
                        propertysetBuilder_.addMessage(builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder addPropertyset(
                        int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder builderForValue) {
                    if (propertysetBuilder_ == null) {
                        ensurePropertysetIsMutable();
                        propertyset_.add(index, builderForValue.build());
                        onChanged();
                    } else {
                        propertysetBuilder_.addMessage(index, builderForValue.build());
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder addAllPropertyset(
                        java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet> values) {
                    if (propertysetBuilder_ == null) {
                        ensurePropertysetIsMutable();
                        com.google.protobuf.AbstractMessageLite.Builder.addAll(
                                values, propertyset_);
                        onChanged();
                    } else {
                        propertysetBuilder_.addAllMessages(values);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder clearPropertyset() {
                    if (propertysetBuilder_ == null) {
                        propertyset_ = java.util.Collections.emptyList();
                        bitField0_ = (bitField0_ & ~0x00000001);
                        onChanged();
                    } else {
                        propertysetBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public Builder removePropertyset(int index) {
                    if (propertysetBuilder_ == null) {
                        ensurePropertysetIsMutable();
                        propertyset_.remove(index);
                        onChanged();
                    } else {
                        propertysetBuilder_.remove(index);
                    }
                    return this;
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder getPropertysetBuilder(
                        int index) {
                    return getPropertysetFieldBuilder().getBuilder(index);
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertysetOrBuilder(
                        int index) {
                    if (propertysetBuilder_ == null) {
                        return propertyset_.get(index);  } else {
                        return propertysetBuilder_.getMessageOrBuilder(index);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
                getPropertysetOrBuilderList() {
                    if (propertysetBuilder_ != null) {
                        return propertysetBuilder_.getMessageOrBuilderList();
                    } else {
                        return java.util.Collections.unmodifiableList(propertyset_);
                    }
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder addPropertysetBuilder() {
                    return getPropertysetFieldBuilder().addBuilder(
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder addPropertysetBuilder(
                        int index) {
                    return getPropertysetFieldBuilder().addBuilder(
                            index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance());
                }
                /**
                 * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet propertyset = 1;</code>
                 */
                public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder>
                getPropertysetBuilderList() {
                    return getPropertysetFieldBuilder().getBuilderList();
                }
                private com.google.protobuf.RepeatedFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
                getPropertysetFieldBuilder() {
                    if (propertysetBuilder_ == null) {
                        propertysetBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>(
                                propertyset_,
                                ((bitField0_ & 0x00000001) == 0x00000001),
                                getParentForChildren(),
                                isClean());
                        propertyset_ = null;
                    }
                    return propertysetBuilder_;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList)
            }

            static {
                defaultInstance = new PropertySetList(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySetList)
        }

        public interface MetaDataOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData)
                com.google.protobuf.GeneratedMessage.
                        ExtendableMessageOrBuilder<MetaData> {

            /**
             * <code>optional bool is_multi_part = 1;</code>
             *
             * <pre>
             * Bytes specific metadata
             * </pre>
             */
            boolean hasIsMultiPart();
            /**
             * <code>optional bool is_multi_part = 1;</code>
             *
             * <pre>
             * Bytes specific metadata
             * </pre>
             */
            boolean getIsMultiPart();

            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            boolean hasContentType();
            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            java.lang.String getContentType();
            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            com.google.protobuf.ByteString
            getContentTypeBytes();

            /**
             * <code>optional uint64 size = 3;</code>
             *
             * <pre>
             * File size, String size, Multi-part size, etc
             * </pre>
             */
            boolean hasSize();
            /**
             * <code>optional uint64 size = 3;</code>
             *
             * <pre>
             * File size, String size, Multi-part size, etc
             * </pre>
             */
            long getSize();

            /**
             * <code>optional uint64 seq = 4;</code>
             *
             * <pre>
             * Sequence number for multi-part messages
             * </pre>
             */
            boolean hasSeq();
            /**
             * <code>optional uint64 seq = 4;</code>
             *
             * <pre>
             * Sequence number for multi-part messages
             * </pre>
             */
            long getSeq();

            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            boolean hasFileName();
            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            java.lang.String getFileName();
            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            com.google.protobuf.ByteString
            getFileNameBytes();

            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            boolean hasFileType();
            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            java.lang.String getFileType();
            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            com.google.protobuf.ByteString
            getFileTypeBytes();

            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            boolean hasMd5();
            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            java.lang.String getMd5();
            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            com.google.protobuf.ByteString
            getMd5Bytes();

            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            boolean hasDescription();
            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            java.lang.String getDescription();
            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            com.google.protobuf.ByteString
            getDescriptionBytes();
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData}
         */
        public static final class MetaData extends
                com.google.protobuf.GeneratedMessage.ExtendableMessage<
                        MetaData> implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData)
                MetaDataOrBuilder {
            // Use MetaData.newBuilder() to construct.
            private MetaData(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData, ?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private MetaData(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final MetaData defaultInstance;
            public static MetaData getDefaultInstance() {
                return defaultInstance;
            }

            public MetaData getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private MetaData(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 8: {
                                bitField0_ |= 0x00000001;
                                isMultiPart_ = input.readBool();
                                break;
                            }
                            case 18: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000002;
                                contentType_ = bs;
                                break;
                            }
                            case 24: {
                                bitField0_ |= 0x00000004;
                                size_ = input.readUInt64();
                                break;
                            }
                            case 32: {
                                bitField0_ |= 0x00000008;
                                seq_ = input.readUInt64();
                                break;
                            }
                            case 42: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000010;
                                fileName_ = bs;
                                break;
                            }
                            case 50: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000020;
                                fileType_ = bs;
                                break;
                            }
                            case 58: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000040;
                                md5_ = bs;
                                break;
                            }
                            case 66: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000080;
                                description_ = bs;
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder.class);
            }

            public static com.google.protobuf.Parser<MetaData> PARSER =
                    new com.google.protobuf.AbstractParser<MetaData>() {
                        public MetaData parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new MetaData(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<MetaData> getParserForType() {
                return PARSER;
            }

            private int bitField0_;
            public static final int IS_MULTI_PART_FIELD_NUMBER = 1;
            private boolean isMultiPart_;
            /**
             * <code>optional bool is_multi_part = 1;</code>
             *
             * <pre>
             * Bytes specific metadata
             * </pre>
             */
            public boolean hasIsMultiPart() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional bool is_multi_part = 1;</code>
             *
             * <pre>
             * Bytes specific metadata
             * </pre>
             */
            public boolean getIsMultiPart() {
                return isMultiPart_;
            }

            public static final int CONTENT_TYPE_FIELD_NUMBER = 2;
            private java.lang.Object contentType_;
            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            public boolean hasContentType() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }
            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            public java.lang.String getContentType() {
                java.lang.Object ref = contentType_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        contentType_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string content_type = 2;</code>
             *
             * <pre>
             * General metadata
             * </pre>
             */
            public com.google.protobuf.ByteString
            getContentTypeBytes() {
                java.lang.Object ref = contentType_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    contentType_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int SIZE_FIELD_NUMBER = 3;
            private long size_;
            /**
             * <code>optional uint64 size = 3;</code>
             *
             * <pre>
             * File size, String size, Multi-part size, etc
             * </pre>
             */
            public boolean hasSize() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }
            /**
             * <code>optional uint64 size = 3;</code>
             *
             * <pre>
             * File size, String size, Multi-part size, etc
             * </pre>
             */
            public long getSize() {
                return size_;
            }

            public static final int SEQ_FIELD_NUMBER = 4;
            private long seq_;
            /**
             * <code>optional uint64 seq = 4;</code>
             *
             * <pre>
             * Sequence number for multi-part messages
             * </pre>
             */
            public boolean hasSeq() {
                return ((bitField0_ & 0x00000008) == 0x00000008);
            }
            /**
             * <code>optional uint64 seq = 4;</code>
             *
             * <pre>
             * Sequence number for multi-part messages
             * </pre>
             */
            public long getSeq() {
                return seq_;
            }

            public static final int FILE_NAME_FIELD_NUMBER = 5;
            private java.lang.Object fileName_;
            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            public boolean hasFileName() {
                return ((bitField0_ & 0x00000010) == 0x00000010);
            }
            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            public java.lang.String getFileName() {
                java.lang.Object ref = fileName_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        fileName_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string file_name = 5;</code>
             *
             * <pre>
             * File metadata
             * </pre>
             */
            public com.google.protobuf.ByteString
            getFileNameBytes() {
                java.lang.Object ref = fileName_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    fileName_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int FILE_TYPE_FIELD_NUMBER = 6;
            private java.lang.Object fileType_;
            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            public boolean hasFileType() {
                return ((bitField0_ & 0x00000020) == 0x00000020);
            }
            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            public java.lang.String getFileType() {
                java.lang.Object ref = fileType_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        fileType_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string file_type = 6;</code>
             *
             * <pre>
             * File type (i.e. xml, json, txt, cpp, etc)
             * </pre>
             */
            public com.google.protobuf.ByteString
            getFileTypeBytes() {
                java.lang.Object ref = fileType_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    fileType_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int MD5_FIELD_NUMBER = 7;
            private java.lang.Object md5_;
            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            public boolean hasMd5() {
                return ((bitField0_ & 0x00000040) == 0x00000040);
            }
            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            public java.lang.String getMd5() {
                java.lang.Object ref = md5_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        md5_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string md5 = 7;</code>
             *
             * <pre>
             * md5 of data
             * </pre>
             */
            public com.google.protobuf.ByteString
            getMd5Bytes() {
                java.lang.Object ref = md5_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    md5_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int DESCRIPTION_FIELD_NUMBER = 8;
            private java.lang.Object description_;
            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            public boolean hasDescription() {
                return ((bitField0_ & 0x00000080) == 0x00000080);
            }
            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            public java.lang.String getDescription() {
                java.lang.Object ref = description_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        description_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string description = 8;</code>
             *
             * <pre>
             * Catchalls and future expansion
             * </pre>
             */
            public com.google.protobuf.ByteString
            getDescriptionBytes() {
                java.lang.Object ref = description_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    description_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            private void initFields() {
                isMultiPart_ = false;
                contentType_ = "";
                size_ = 0L;
                seq_ = 0L;
                fileName_ = "";
                fileType_ = "";
                md5_ = "";
                description_ = "";
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                if (!extensionsAreInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                com.google.protobuf.GeneratedMessage
                        .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData>.ExtensionWriter extensionWriter =
                        newExtensionWriter();
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    output.writeBool(1, isMultiPart_);
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    output.writeBytes(2, getContentTypeBytes());
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    output.writeUInt64(3, size_);
                }
                if (((bitField0_ & 0x00000008) == 0x00000008)) {
                    output.writeUInt64(4, seq_);
                }
                if (((bitField0_ & 0x00000010) == 0x00000010)) {
                    output.writeBytes(5, getFileNameBytes());
                }
                if (((bitField0_ & 0x00000020) == 0x00000020)) {
                    output.writeBytes(6, getFileTypeBytes());
                }
                if (((bitField0_ & 0x00000040) == 0x00000040)) {
                    output.writeBytes(7, getMd5Bytes());
                }
                if (((bitField0_ & 0x00000080) == 0x00000080)) {
                    output.writeBytes(8, getDescriptionBytes());
                }
                extensionWriter.writeUntil(536870912, output);
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(1, isMultiPart_);
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(2, getContentTypeBytes());
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(3, size_);
                }
                if (((bitField0_ & 0x00000008) == 0x00000008)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(4, seq_);
                }
                if (((bitField0_ & 0x00000010) == 0x00000010)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(5, getFileNameBytes());
                }
                if (((bitField0_ & 0x00000020) == 0x00000020)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(6, getFileTypeBytes());
                }
                if (((bitField0_ & 0x00000040) == 0x00000040)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(7, getMd5Bytes());
                }
                if (((bitField0_ & 0x00000080) == 0x00000080)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(8, getDescriptionBytes());
                }
                size += extensionsSerializedSize();
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData, Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    isMultiPart_ = false;
                    bitField0_ = (bitField0_ & ~0x00000001);
                    contentType_ = "";
                    bitField0_ = (bitField0_ & ~0x00000002);
                    size_ = 0L;
                    bitField0_ = (bitField0_ & ~0x00000004);
                    seq_ = 0L;
                    bitField0_ = (bitField0_ & ~0x00000008);
                    fileName_ = "";
                    bitField0_ = (bitField0_ & ~0x00000010);
                    fileType_ = "";
                    bitField0_ = (bitField0_ & ~0x00000020);
                    md5_ = "";
                    bitField0_ = (bitField0_ & ~0x00000040);
                    description_ = "";
                    bitField0_ = (bitField0_ & ~0x00000080);
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData(this);
                    int from_bitField0_ = bitField0_;
                    int to_bitField0_ = 0;
                    if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                        to_bitField0_ |= 0x00000001;
                    }
                    result.isMultiPart_ = isMultiPart_;
                    if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
                        to_bitField0_ |= 0x00000002;
                    }
                    result.contentType_ = contentType_;
                    if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
                        to_bitField0_ |= 0x00000004;
                    }
                    result.size_ = size_;
                    if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
                        to_bitField0_ |= 0x00000008;
                    }
                    result.seq_ = seq_;
                    if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
                        to_bitField0_ |= 0x00000010;
                    }
                    result.fileName_ = fileName_;
                    if (((from_bitField0_ & 0x00000020) == 0x00000020)) {
                        to_bitField0_ |= 0x00000020;
                    }
                    result.fileType_ = fileType_;
                    if (((from_bitField0_ & 0x00000040) == 0x00000040)) {
                        to_bitField0_ |= 0x00000040;
                    }
                    result.md5_ = md5_;
                    if (((from_bitField0_ & 0x00000080) == 0x00000080)) {
                        to_bitField0_ |= 0x00000080;
                    }
                    result.description_ = description_;
                    result.bitField0_ = to_bitField0_;
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance()) return this;
                    if (other.hasIsMultiPart()) {
                        setIsMultiPart(other.getIsMultiPart());
                    }
                    if (other.hasContentType()) {
                        bitField0_ |= 0x00000002;
                        contentType_ = other.contentType_;
                        onChanged();
                    }
                    if (other.hasSize()) {
                        setSize(other.getSize());
                    }
                    if (other.hasSeq()) {
                        setSeq(other.getSeq());
                    }
                    if (other.hasFileName()) {
                        bitField0_ |= 0x00000010;
                        fileName_ = other.fileName_;
                        onChanged();
                    }
                    if (other.hasFileType()) {
                        bitField0_ |= 0x00000020;
                        fileType_ = other.fileType_;
                        onChanged();
                    }
                    if (other.hasMd5()) {
                        bitField0_ |= 0x00000040;
                        md5_ = other.md5_;
                        onChanged();
                    }
                    if (other.hasDescription()) {
                        bitField0_ |= 0x00000080;
                        description_ = other.description_;
                        onChanged();
                    }
                    this.mergeExtensionFields(other);
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    if (!extensionsAreInitialized()) {

                        return false;
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int bitField0_;

                private boolean isMultiPart_ ;
                /**
                 * <code>optional bool is_multi_part = 1;</code>
                 *
                 * <pre>
                 * Bytes specific metadata
                 * </pre>
                 */
                public boolean hasIsMultiPart() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional bool is_multi_part = 1;</code>
                 *
                 * <pre>
                 * Bytes specific metadata
                 * </pre>
                 */
                public boolean getIsMultiPart() {
                    return isMultiPart_;
                }
                /**
                 * <code>optional bool is_multi_part = 1;</code>
                 *
                 * <pre>
                 * Bytes specific metadata
                 * </pre>
                 */
                public Builder setIsMultiPart(boolean value) {
                    bitField0_ |= 0x00000001;
                    isMultiPart_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_multi_part = 1;</code>
                 *
                 * <pre>
                 * Bytes specific metadata
                 * </pre>
                 */
                public Builder clearIsMultiPart() {
                    bitField0_ = (bitField0_ & ~0x00000001);
                    isMultiPart_ = false;
                    onChanged();
                    return this;
                }

                private java.lang.Object contentType_ = "";
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public boolean hasContentType() {
                    return ((bitField0_ & 0x00000002) == 0x00000002);
                }
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public java.lang.String getContentType() {
                    java.lang.Object ref = contentType_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            contentType_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getContentTypeBytes() {
                    java.lang.Object ref = contentType_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        contentType_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public Builder setContentType(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000002;
                    contentType_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public Builder clearContentType() {
                    bitField0_ = (bitField0_ & ~0x00000002);
                    contentType_ = getDefaultInstance().getContentType();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string content_type = 2;</code>
                 *
                 * <pre>
                 * General metadata
                 * </pre>
                 */
                public Builder setContentTypeBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000002;
                    contentType_ = value;
                    onChanged();
                    return this;
                }

                private long size_ ;
                /**
                 * <code>optional uint64 size = 3;</code>
                 *
                 * <pre>
                 * File size, String size, Multi-part size, etc
                 * </pre>
                 */
                public boolean hasSize() {
                    return ((bitField0_ & 0x00000004) == 0x00000004);
                }
                /**
                 * <code>optional uint64 size = 3;</code>
                 *
                 * <pre>
                 * File size, String size, Multi-part size, etc
                 * </pre>
                 */
                public long getSize() {
                    return size_;
                }
                /**
                 * <code>optional uint64 size = 3;</code>
                 *
                 * <pre>
                 * File size, String size, Multi-part size, etc
                 * </pre>
                 */
                public Builder setSize(long value) {
                    bitField0_ |= 0x00000004;
                    size_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 size = 3;</code>
                 *
                 * <pre>
                 * File size, String size, Multi-part size, etc
                 * </pre>
                 */
                public Builder clearSize() {
                    bitField0_ = (bitField0_ & ~0x00000004);
                    size_ = 0L;
                    onChanged();
                    return this;
                }

                private long seq_ ;
                /**
                 * <code>optional uint64 seq = 4;</code>
                 *
                 * <pre>
                 * Sequence number for multi-part messages
                 * </pre>
                 */
                public boolean hasSeq() {
                    return ((bitField0_ & 0x00000008) == 0x00000008);
                }
                /**
                 * <code>optional uint64 seq = 4;</code>
                 *
                 * <pre>
                 * Sequence number for multi-part messages
                 * </pre>
                 */
                public long getSeq() {
                    return seq_;
                }
                /**
                 * <code>optional uint64 seq = 4;</code>
                 *
                 * <pre>
                 * Sequence number for multi-part messages
                 * </pre>
                 */
                public Builder setSeq(long value) {
                    bitField0_ |= 0x00000008;
                    seq_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 seq = 4;</code>
                 *
                 * <pre>
                 * Sequence number for multi-part messages
                 * </pre>
                 */
                public Builder clearSeq() {
                    bitField0_ = (bitField0_ & ~0x00000008);
                    seq_ = 0L;
                    onChanged();
                    return this;
                }

                private java.lang.Object fileName_ = "";
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public boolean hasFileName() {
                    return ((bitField0_ & 0x00000010) == 0x00000010);
                }
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public java.lang.String getFileName() {
                    java.lang.Object ref = fileName_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            fileName_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getFileNameBytes() {
                    java.lang.Object ref = fileName_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        fileName_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public Builder setFileName(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000010;
                    fileName_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public Builder clearFileName() {
                    bitField0_ = (bitField0_ & ~0x00000010);
                    fileName_ = getDefaultInstance().getFileName();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string file_name = 5;</code>
                 *
                 * <pre>
                 * File metadata
                 * </pre>
                 */
                public Builder setFileNameBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000010;
                    fileName_ = value;
                    onChanged();
                    return this;
                }

                private java.lang.Object fileType_ = "";
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public boolean hasFileType() {
                    return ((bitField0_ & 0x00000020) == 0x00000020);
                }
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public java.lang.String getFileType() {
                    java.lang.Object ref = fileType_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            fileType_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getFileTypeBytes() {
                    java.lang.Object ref = fileType_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        fileType_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public Builder setFileType(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000020;
                    fileType_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public Builder clearFileType() {
                    bitField0_ = (bitField0_ & ~0x00000020);
                    fileType_ = getDefaultInstance().getFileType();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string file_type = 6;</code>
                 *
                 * <pre>
                 * File type (i.e. xml, json, txt, cpp, etc)
                 * </pre>
                 */
                public Builder setFileTypeBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000020;
                    fileType_ = value;
                    onChanged();
                    return this;
                }

                private java.lang.Object md5_ = "";
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public boolean hasMd5() {
                    return ((bitField0_ & 0x00000040) == 0x00000040);
                }
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public java.lang.String getMd5() {
                    java.lang.Object ref = md5_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            md5_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getMd5Bytes() {
                    java.lang.Object ref = md5_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        md5_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public Builder setMd5(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000040;
                    md5_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public Builder clearMd5() {
                    bitField0_ = (bitField0_ & ~0x00000040);
                    md5_ = getDefaultInstance().getMd5();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string md5 = 7;</code>
                 *
                 * <pre>
                 * md5 of data
                 * </pre>
                 */
                public Builder setMd5Bytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000040;
                    md5_ = value;
                    onChanged();
                    return this;
                }

                private java.lang.Object description_ = "";
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public boolean hasDescription() {
                    return ((bitField0_ & 0x00000080) == 0x00000080);
                }
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public java.lang.String getDescription() {
                    java.lang.Object ref = description_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            description_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getDescriptionBytes() {
                    java.lang.Object ref = description_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        description_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public Builder setDescription(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000080;
                    description_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public Builder clearDescription() {
                    bitField0_ = (bitField0_ & ~0x00000080);
                    description_ = getDefaultInstance().getDescription();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string description = 8;</code>
                 *
                 * <pre>
                 * Catchalls and future expansion
                 * </pre>
                 */
                public Builder setDescriptionBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000080;
                    description_ = value;
                    onChanged();
                    return this;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData)
            }

            static {
                defaultInstance = new MetaData(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData)
        }

        public interface MetricOrBuilder extends
                // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric)
                com.google.protobuf.MessageOrBuilder {

            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            boolean hasName();
            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            java.lang.String getName();
            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            com.google.protobuf.ByteString
            getNameBytes();

            /**
             * <code>optional uint64 alias = 2;</code>
             *
             * <pre>
             * Metric alias - tied to name on birth and included in all later DATA messages
             * </pre>
             */
            boolean hasAlias();
            /**
             * <code>optional uint64 alias = 2;</code>
             *
             * <pre>
             * Metric alias - tied to name on birth and included in all later DATA messages
             * </pre>
             */
            long getAlias();

            /**
             * <code>optional uint64 timestamp = 3;</code>
             *
             * <pre>
             * Timestamp associated with data acquisition time
             * </pre>
             */
            boolean hasTimestamp();
            /**
             * <code>optional uint64 timestamp = 3;</code>
             *
             * <pre>
             * Timestamp associated with data acquisition time
             * </pre>
             */
            long getTimestamp();

            /**
             * <code>optional uint32 datatype = 4;</code>
             *
             * <pre>
             * DataType of the metric/tag value
             * </pre>
             */
            boolean hasDatatype();
            /**
             * <code>optional uint32 datatype = 4;</code>
             *
             * <pre>
             * DataType of the metric/tag value
             * </pre>
             */
            int getDatatype();

            /**
             * <code>optional bool is_historical = 5;</code>
             *
             * <pre>
             * If this is historical data and should not update real time tag
             * </pre>
             */
            boolean hasIsHistorical();
            /**
             * <code>optional bool is_historical = 5;</code>
             *
             * <pre>
             * If this is historical data and should not update real time tag
             * </pre>
             */
            boolean getIsHistorical();

            /**
             * <code>optional bool is_transient = 6;</code>
             *
             * <pre>
             * Tells consuming clients such as MQTT Engine to not store this as a tag
             * </pre>
             */
            boolean hasIsTransient();
            /**
             * <code>optional bool is_transient = 6;</code>
             *
             * <pre>
             * Tells consuming clients such as MQTT Engine to not store this as a tag
             * </pre>
             */
            boolean getIsTransient();

            /**
             * <code>optional bool is_null = 7;</code>
             *
             * <pre>
             * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
             * </pre>
             */
            boolean hasIsNull();
            /**
             * <code>optional bool is_null = 7;</code>
             *
             * <pre>
             * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
             * </pre>
             */
            boolean getIsNull();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            boolean hasMetadata();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData getMetadata();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder getMetadataOrBuilder();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            boolean hasProperties();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getProperties();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertiesOrBuilder();

            /**
             * <code>optional uint32 int_value = 10;</code>
             */
            boolean hasIntValue();
            /**
             * <code>optional uint32 int_value = 10;</code>
             */
            int getIntValue();

            /**
             * <code>optional uint64 long_value = 11;</code>
             */
            boolean hasLongValue();
            /**
             * <code>optional uint64 long_value = 11;</code>
             */
            long getLongValue();

            /**
             * <code>optional float float_value = 12;</code>
             */
            boolean hasFloatValue();
            /**
             * <code>optional float float_value = 12;</code>
             */
            float getFloatValue();

            /**
             * <code>optional double double_value = 13;</code>
             */
            boolean hasDoubleValue();
            /**
             * <code>optional double double_value = 13;</code>
             */
            double getDoubleValue();

            /**
             * <code>optional bool boolean_value = 14;</code>
             */
            boolean hasBooleanValue();
            /**
             * <code>optional bool boolean_value = 14;</code>
             */
            boolean getBooleanValue();

            /**
             * <code>optional string string_value = 15;</code>
             */
            boolean hasStringValue();
            /**
             * <code>optional string string_value = 15;</code>
             */
            java.lang.String getStringValue();
            /**
             * <code>optional string string_value = 15;</code>
             */
            com.google.protobuf.ByteString
            getStringValueBytes();

            /**
             * <code>optional bytes bytes_value = 16;</code>
             *
             * <pre>
             * Bytes, File
             * </pre>
             */
            boolean hasBytesValue();
            /**
             * <code>optional bytes bytes_value = 16;</code>
             *
             * <pre>
             * Bytes, File
             * </pre>
             */
            com.google.protobuf.ByteString getBytesValue();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            boolean hasDatasetValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet getDatasetValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder getDatasetValueOrBuilder();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            boolean hasTemplateValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template getTemplateValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder getTemplateValueOrBuilder();

            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            boolean hasExtensionValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension getExtensionValue();
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder getExtensionValueOrBuilder();
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric}
         */
        public static final class Metric extends
                com.google.protobuf.GeneratedMessage implements
                // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric)
                MetricOrBuilder {
            // Use Metric.newBuilder() to construct.
            private Metric(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
                super(builder);
                this.unknownFields = builder.getUnknownFields();
            }
            private Metric(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

            private static final Metric defaultInstance;
            public static Metric getDefaultInstance() {
                return defaultInstance;
            }

            public Metric getDefaultInstanceForType() {
                return defaultInstance;
            }

            private final com.google.protobuf.UnknownFieldSet unknownFields;
            @java.lang.Override
            public final com.google.protobuf.UnknownFieldSet
            getUnknownFields() {
                return this.unknownFields;
            }
            private Metric(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                initFields();
                int mutable_bitField0_ = 0;
                com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                        com.google.protobuf.UnknownFieldSet.newBuilder();
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            default: {
                                if (!parseUnknownField(input, unknownFields,
                                        extensionRegistry, tag)) {
                                    done = true;
                                }
                                break;
                            }
                            case 10: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                bitField0_ |= 0x00000001;
                                name_ = bs;
                                break;
                            }
                            case 16: {
                                bitField0_ |= 0x00000002;
                                alias_ = input.readUInt64();
                                break;
                            }
                            case 24: {
                                bitField0_ |= 0x00000004;
                                timestamp_ = input.readUInt64();
                                break;
                            }
                            case 32: {
                                bitField0_ |= 0x00000008;
                                datatype_ = input.readUInt32();
                                break;
                            }
                            case 40: {
                                bitField0_ |= 0x00000010;
                                isHistorical_ = input.readBool();
                                break;
                            }
                            case 48: {
                                bitField0_ |= 0x00000020;
                                isTransient_ = input.readBool();
                                break;
                            }
                            case 56: {
                                bitField0_ |= 0x00000040;
                                isNull_ = input.readBool();
                                break;
                            }
                            case 66: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder subBuilder = null;
                                if (((bitField0_ & 0x00000080) == 0x00000080)) {
                                    subBuilder = metadata_.toBuilder();
                                }
                                metadata_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom(metadata_);
                                    metadata_ = subBuilder.buildPartial();
                                }
                                bitField0_ |= 0x00000080;
                                break;
                            }
                            case 74: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder subBuilder = null;
                                if (((bitField0_ & 0x00000100) == 0x00000100)) {
                                    subBuilder = properties_.toBuilder();
                                }
                                properties_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom(properties_);
                                    properties_ = subBuilder.buildPartial();
                                }
                                bitField0_ |= 0x00000100;
                                break;
                            }
                            case 80: {
                                valueCase_ = 10;
                                value_ = input.readUInt32();
                                break;
                            }
                            case 88: {
                                valueCase_ = 11;
                                value_ = input.readUInt64();
                                break;
                            }
                            case 101: {
                                valueCase_ = 12;
                                value_ = input.readFloat();
                                break;
                            }
                            case 105: {
                                valueCase_ = 13;
                                value_ = input.readDouble();
                                break;
                            }
                            case 112: {
                                valueCase_ = 14;
                                value_ = input.readBool();
                                break;
                            }
                            case 122: {
                                com.google.protobuf.ByteString bs = input.readBytes();
                                valueCase_ = 15;
                                value_ = bs;
                                break;
                            }
                            case 130: {
                                valueCase_ = 16;
                                value_ = input.readBytes();
                                break;
                            }
                            case 138: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder subBuilder = null;
                                if (valueCase_ == 17) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 17;
                                break;
                            }
                            case 146: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder subBuilder = null;
                                if (valueCase_ == 18) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 18;
                                break;
                            }
                            case 154: {
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder subBuilder = null;
                                if (valueCase_ == 19) {
                                    subBuilder = ((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_).toBuilder();
                                }
                                value_ = input.readMessage(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.PARSER, extensionRegistry);
                                if (subBuilder != null) {
                                    subBuilder.mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_);
                                    value_ = subBuilder.buildPartial();
                                }
                                valueCase_ = 19;
                                break;
                            }
                        }
                    }
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(this);
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this);
                } finally {
                    this.unknownFields = unknownFields.build();
                    makeExtensionsImmutable();
                }
            }
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder.class);
            }

            public static com.google.protobuf.Parser<Metric> PARSER =
                    new com.google.protobuf.AbstractParser<Metric>() {
                        public Metric parsePartialFrom(
                                com.google.protobuf.CodedInputStream input,
                                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                throws com.google.protobuf.InvalidProtocolBufferException {
                            return new Metric(input, extensionRegistry);
                        }
                    };

            @java.lang.Override
            public com.google.protobuf.Parser<Metric> getParserForType() {
                return PARSER;
            }

            public interface MetricValueExtensionOrBuilder extends
                    // @@protoc_insertion_point(interface_extends:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension)
                    com.google.protobuf.GeneratedMessage.
                            ExtendableMessageOrBuilder<MetricValueExtension> {
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension}
             */
            public static final class MetricValueExtension extends
                    com.google.protobuf.GeneratedMessage.ExtendableMessage<
                            MetricValueExtension> implements
                    // @@protoc_insertion_point(message_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension)
                    MetricValueExtensionOrBuilder {
                // Use MetricValueExtension.newBuilder() to construct.
                private MetricValueExtension(com.google.protobuf.GeneratedMessage.ExtendableBuilder<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension, ?> builder) {
                    super(builder);
                    this.unknownFields = builder.getUnknownFields();
                }
                private MetricValueExtension(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

                private static final MetricValueExtension defaultInstance;
                public static MetricValueExtension getDefaultInstance() {
                    return defaultInstance;
                }

                public MetricValueExtension getDefaultInstanceForType() {
                    return defaultInstance;
                }

                private final com.google.protobuf.UnknownFieldSet unknownFields;
                @java.lang.Override
                public final com.google.protobuf.UnknownFieldSet
                getUnknownFields() {
                    return this.unknownFields;
                }
                private MetricValueExtension(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    initFields();
                    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                            com.google.protobuf.UnknownFieldSet.newBuilder();
                    try {
                        boolean done = false;
                        while (!done) {
                            int tag = input.readTag();
                            switch (tag) {
                                case 0:
                                    done = true;
                                    break;
                                default: {
                                    if (!parseUnknownField(input, unknownFields,
                                            extensionRegistry, tag)) {
                                        done = true;
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        throw e.setUnfinishedMessage(this);
                    } catch (java.io.IOException e) {
                        throw new com.google.protobuf.InvalidProtocolBufferException(
                                e.getMessage()).setUnfinishedMessage(this);
                    } finally {
                        this.unknownFields = unknownFields.build();
                        makeExtensionsImmutable();
                    }
                }
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder.class);
                }

                public static com.google.protobuf.Parser<MetricValueExtension> PARSER =
                        new com.google.protobuf.AbstractParser<MetricValueExtension>() {
                            public MetricValueExtension parsePartialFrom(
                                    com.google.protobuf.CodedInputStream input,
                                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                                    throws com.google.protobuf.InvalidProtocolBufferException {
                                return new MetricValueExtension(input, extensionRegistry);
                            }
                        };

                @java.lang.Override
                public com.google.protobuf.Parser<MetricValueExtension> getParserForType() {
                    return PARSER;
                }

                private void initFields() {
                }
                private byte memoizedIsInitialized = -1;
                public final boolean isInitialized() {
                    byte isInitialized = memoizedIsInitialized;
                    if (isInitialized == 1) return true;
                    if (isInitialized == 0) return false;

                    if (!extensionsAreInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                    memoizedIsInitialized = 1;
                    return true;
                }

                public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
                    getSerializedSize();
                    com.google.protobuf.GeneratedMessage
                            .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension>.ExtensionWriter extensionWriter =
                            newExtensionWriter();
                    extensionWriter.writeUntil(536870912, output);
                    getUnknownFields().writeTo(output);
                }

                private int memoizedSerializedSize = -1;
                public int getSerializedSize() {
                    int size = memoizedSerializedSize;
                    if (size != -1) return size;

                    size = 0;
                    size += extensionsSerializedSize();
                    size += getUnknownFields().getSerializedSize();
                    memoizedSerializedSize = size;
                    return size;
                }

                private static final long serialVersionUID = 0L;
                @java.lang.Override
                protected java.lang.Object writeReplace()
                        throws java.io.ObjectStreamException {
                    return super.writeReplace();
                }

                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        com.google.protobuf.ByteString data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        com.google.protobuf.ByteString data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(byte[] data)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        byte[] data,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws com.google.protobuf.InvalidProtocolBufferException {
                    return PARSER.parseFrom(data, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseDelimitedFrom(java.io.InputStream input)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseDelimitedFrom(
                        java.io.InputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseDelimitedFrom(input, extensionRegistry);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        com.google.protobuf.CodedInputStream input)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input);
                }
                public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parseFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    return PARSER.parseFrom(input, extensionRegistry);
                }

                public static Builder newBuilder() { return Builder.create(); }
                public Builder newBuilderForType() { return newBuilder(); }
                public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension prototype) {
                    return newBuilder().mergeFrom(prototype);
                }
                public Builder toBuilder() { return newBuilder(this); }

                @java.lang.Override
                protected Builder newBuilderForType(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    Builder builder = new Builder(parent);
                    return builder;
                }
                /**
                 * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension}
                 */
                public static final class Builder extends
                        com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension, Builder> implements
                        // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension)
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder {
                    public static final com.google.protobuf.Descriptors.Descriptor
                    getDescriptor() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor;
                    }

                    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                    internalGetFieldAccessorTable() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_fieldAccessorTable
                                .ensureFieldAccessorsInitialized(
                                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder.class);
                    }

                    // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.newBuilder()
                    private Builder() {
                        maybeForceBuilderInitialization();
                    }

                    private Builder(
                            com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                        super(parent);
                        maybeForceBuilderInitialization();
                    }
                    private void maybeForceBuilderInitialization() {
                        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        }
                    }
                    private static Builder create() {
                        return new Builder();
                    }

                    public Builder clear() {
                        super.clear();
                        return this;
                    }

                    public Builder clone() {
                        return create().mergeFrom(buildPartial());
                    }

                    public com.google.protobuf.Descriptors.Descriptor
                    getDescriptorForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension getDefaultInstanceForType() {
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension build() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension result = buildPartial();
                        if (!result.isInitialized()) {
                            throw newUninitializedMessageException(result);
                        }
                        return result;
                    }

                    public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension buildPartial() {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension(this);
                        onBuilt();
                        return result;
                    }

                    public Builder mergeFrom(com.google.protobuf.Message other) {
                        if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) {
                            return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension)other);
                        } else {
                            super.mergeFrom(other);
                            return this;
                        }
                    }

                    public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension other) {
                        if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance()) return this;
                        this.mergeExtensionFields(other);
                        this.mergeUnknownFields(other.getUnknownFields());
                        return this;
                    }

                    public final boolean isInitialized() {
                        if (!extensionsAreInitialized()) {

                            return false;
                        }
                        return true;
                    }

                    public Builder mergeFrom(
                            com.google.protobuf.CodedInputStream input,
                            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                            throws java.io.IOException {
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension parsedMessage = null;
                        try {
                            parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                            parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) e.getUnfinishedMessage();
                            throw e;
                        } finally {
                            if (parsedMessage != null) {
                                mergeFrom(parsedMessage);
                            }
                        }
                        return this;
                    }

                    // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension)
                }

                static {
                    defaultInstance = new MetricValueExtension(true);
                    defaultInstance.initFields();
                }

                // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension)
            }

            private int bitField0_;
            private int valueCase_ = 0;
            private java.lang.Object value_;
            public enum ValueCase
                    implements com.google.protobuf.Internal.EnumLite {
                INT_VALUE(10),
                LONG_VALUE(11),
                FLOAT_VALUE(12),
                DOUBLE_VALUE(13),
                BOOLEAN_VALUE(14),
                STRING_VALUE(15),
                BYTES_VALUE(16),
                DATASET_VALUE(17),
                TEMPLATE_VALUE(18),
                EXTENSION_VALUE(19),
                VALUE_NOT_SET(0);
                private int value = 0;
                private ValueCase(int value) {
                    this.value = value;
                }
                public static ValueCase valueOf(int value) {
                    switch (value) {
                        case 10: return INT_VALUE;
                        case 11: return LONG_VALUE;
                        case 12: return FLOAT_VALUE;
                        case 13: return DOUBLE_VALUE;
                        case 14: return BOOLEAN_VALUE;
                        case 15: return STRING_VALUE;
                        case 16: return BYTES_VALUE;
                        case 17: return DATASET_VALUE;
                        case 18: return TEMPLATE_VALUE;
                        case 19: return EXTENSION_VALUE;
                        case 0: return VALUE_NOT_SET;
                        default: throw new java.lang.IllegalArgumentException(
                                "Value is undefined for this oneof enum.");
                    }
                }
                public int getNumber() {
                    return this.value;
                }
            };

            public ValueCase
            getValueCase() {
                return ValueCase.valueOf(
                        valueCase_);
            }

            public static final int NAME_FIELD_NUMBER = 1;
            private java.lang.Object name_;
            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            public boolean hasName() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            public java.lang.String getName() {
                java.lang.Object ref = name_;
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        name_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string name = 1;</code>
             *
             * <pre>
             * Metric name - should only be included on birth
             * </pre>
             */
            public com.google.protobuf.ByteString
            getNameBytes() {
                java.lang.Object ref = name_;
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    name_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int ALIAS_FIELD_NUMBER = 2;
            private long alias_;
            /**
             * <code>optional uint64 alias = 2;</code>
             *
             * <pre>
             * Metric alias - tied to name on birth and included in all later DATA messages
             * </pre>
             */
            public boolean hasAlias() {
                return ((bitField0_ & 0x00000002) == 0x00000002);
            }
            /**
             * <code>optional uint64 alias = 2;</code>
             *
             * <pre>
             * Metric alias - tied to name on birth and included in all later DATA messages
             * </pre>
             */
            public long getAlias() {
                return alias_;
            }

            public static final int TIMESTAMP_FIELD_NUMBER = 3;
            private long timestamp_;
            /**
             * <code>optional uint64 timestamp = 3;</code>
             *
             * <pre>
             * Timestamp associated with data acquisition time
             * </pre>
             */
            public boolean hasTimestamp() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }
            /**
             * <code>optional uint64 timestamp = 3;</code>
             *
             * <pre>
             * Timestamp associated with data acquisition time
             * </pre>
             */
            public long getTimestamp() {
                return timestamp_;
            }

            public static final int DATATYPE_FIELD_NUMBER = 4;
            private int datatype_;
            /**
             * <code>optional uint32 datatype = 4;</code>
             *
             * <pre>
             * DataType of the metric/tag value
             * </pre>
             */
            public boolean hasDatatype() {
                return ((bitField0_ & 0x00000008) == 0x00000008);
            }
            /**
             * <code>optional uint32 datatype = 4;</code>
             *
             * <pre>
             * DataType of the metric/tag value
             * </pre>
             */
            public int getDatatype() {
                return datatype_;
            }

            public static final int IS_HISTORICAL_FIELD_NUMBER = 5;
            private boolean isHistorical_;
            /**
             * <code>optional bool is_historical = 5;</code>
             *
             * <pre>
             * If this is historical data and should not update real time tag
             * </pre>
             */
            public boolean hasIsHistorical() {
                return ((bitField0_ & 0x00000010) == 0x00000010);
            }
            /**
             * <code>optional bool is_historical = 5;</code>
             *
             * <pre>
             * If this is historical data and should not update real time tag
             * </pre>
             */
            public boolean getIsHistorical() {
                return isHistorical_;
            }

            public static final int IS_TRANSIENT_FIELD_NUMBER = 6;
            private boolean isTransient_;
            /**
             * <code>optional bool is_transient = 6;</code>
             *
             * <pre>
             * Tells consuming clients such as MQTT Engine to not store this as a tag
             * </pre>
             */
            public boolean hasIsTransient() {
                return ((bitField0_ & 0x00000020) == 0x00000020);
            }
            /**
             * <code>optional bool is_transient = 6;</code>
             *
             * <pre>
             * Tells consuming clients such as MQTT Engine to not store this as a tag
             * </pre>
             */
            public boolean getIsTransient() {
                return isTransient_;
            }

            public static final int IS_NULL_FIELD_NUMBER = 7;
            private boolean isNull_;
            /**
             * <code>optional bool is_null = 7;</code>
             *
             * <pre>
             * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
             * </pre>
             */
            public boolean hasIsNull() {
                return ((bitField0_ & 0x00000040) == 0x00000040);
            }
            /**
             * <code>optional bool is_null = 7;</code>
             *
             * <pre>
             * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
             * </pre>
             */
            public boolean getIsNull() {
                return isNull_;
            }

            public static final int METADATA_FIELD_NUMBER = 8;
            private org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData metadata_;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            public boolean hasMetadata() {
                return ((bitField0_ & 0x00000080) == 0x00000080);
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData getMetadata() {
                return metadata_;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
             *
             * <pre>
             * Metadata for the payload
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder getMetadataOrBuilder() {
                return metadata_;
            }

            public static final int PROPERTIES_FIELD_NUMBER = 9;
            private org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet properties_;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            public boolean hasProperties() {
                return ((bitField0_ & 0x00000100) == 0x00000100);
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getProperties() {
                return properties_;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertiesOrBuilder() {
                return properties_;
            }

            public static final int INT_VALUE_FIELD_NUMBER = 10;
            /**
             * <code>optional uint32 int_value = 10;</code>
             */
            public boolean hasIntValue() {
                return valueCase_ == 10;
            }
            /**
             * <code>optional uint32 int_value = 10;</code>
             */
            public int getIntValue() {
                if (valueCase_ == 10) {
                    return (java.lang.Integer) value_;
                }
                return 0;
            }

            public static final int LONG_VALUE_FIELD_NUMBER = 11;
            /**
             * <code>optional uint64 long_value = 11;</code>
             */
            public boolean hasLongValue() {
                return valueCase_ == 11;
            }
            /**
             * <code>optional uint64 long_value = 11;</code>
             */
            public long getLongValue() {
                if (valueCase_ == 11) {
                    return (java.lang.Long) value_;
                }
                return 0L;
            }

            public static final int FLOAT_VALUE_FIELD_NUMBER = 12;
            /**
             * <code>optional float float_value = 12;</code>
             */
            public boolean hasFloatValue() {
                return valueCase_ == 12;
            }
            /**
             * <code>optional float float_value = 12;</code>
             */
            public float getFloatValue() {
                if (valueCase_ == 12) {
                    return (java.lang.Float) value_;
                }
                return 0F;
            }

            public static final int DOUBLE_VALUE_FIELD_NUMBER = 13;
            /**
             * <code>optional double double_value = 13;</code>
             */
            public boolean hasDoubleValue() {
                return valueCase_ == 13;
            }
            /**
             * <code>optional double double_value = 13;</code>
             */
            public double getDoubleValue() {
                if (valueCase_ == 13) {
                    return (java.lang.Double) value_;
                }
                return 0D;
            }

            public static final int BOOLEAN_VALUE_FIELD_NUMBER = 14;
            /**
             * <code>optional bool boolean_value = 14;</code>
             */
            public boolean hasBooleanValue() {
                return valueCase_ == 14;
            }
            /**
             * <code>optional bool boolean_value = 14;</code>
             */
            public boolean getBooleanValue() {
                if (valueCase_ == 14) {
                    return (java.lang.Boolean) value_;
                }
                return false;
            }

            public static final int STRING_VALUE_FIELD_NUMBER = 15;
            /**
             * <code>optional string string_value = 15;</code>
             */
            public boolean hasStringValue() {
                return valueCase_ == 15;
            }
            /**
             * <code>optional string string_value = 15;</code>
             */
            public java.lang.String getStringValue() {
                java.lang.Object ref = "";
                if (valueCase_ == 15) {
                    ref = value_;
                }
                if (ref instanceof java.lang.String) {
                    return (java.lang.String) ref;
                } else {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8() && (valueCase_ == 15)) {
                        value_ = s;
                    }
                    return s;
                }
            }
            /**
             * <code>optional string string_value = 15;</code>
             */
            public com.google.protobuf.ByteString
            getStringValueBytes() {
                java.lang.Object ref = "";
                if (valueCase_ == 15) {
                    ref = value_;
                }
                if (ref instanceof java.lang.String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    if (valueCase_ == 15) {
                        value_ = b;
                    }
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }

            public static final int BYTES_VALUE_FIELD_NUMBER = 16;
            /**
             * <code>optional bytes bytes_value = 16;</code>
             *
             * <pre>
             * Bytes, File
             * </pre>
             */
            public boolean hasBytesValue() {
                return valueCase_ == 16;
            }
            /**
             * <code>optional bytes bytes_value = 16;</code>
             *
             * <pre>
             * Bytes, File
             * </pre>
             */
            public com.google.protobuf.ByteString getBytesValue() {
                if (valueCase_ == 16) {
                    return (com.google.protobuf.ByteString) value_;
                }
                return com.google.protobuf.ByteString.EMPTY;
            }

            public static final int DATASET_VALUE_FIELD_NUMBER = 17;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            public boolean hasDatasetValue() {
                return valueCase_ == 17;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet getDatasetValue() {
                if (valueCase_ == 17) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder getDatasetValueOrBuilder() {
                if (valueCase_ == 17) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
            }

            public static final int TEMPLATE_VALUE_FIELD_NUMBER = 18;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            public boolean hasTemplateValue() {
                return valueCase_ == 18;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template getTemplateValue() {
                if (valueCase_ == 18) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder getTemplateValueOrBuilder() {
                if (valueCase_ == 18) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
            }

            public static final int EXTENSION_VALUE_FIELD_NUMBER = 19;
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            public boolean hasExtensionValue() {
                return valueCase_ == 19;
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension getExtensionValue() {
                if (valueCase_ == 19) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
            }
            /**
             * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder getExtensionValueOrBuilder() {
                if (valueCase_ == 19) {
                    return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_;
                }
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
            }

            private void initFields() {
                name_ = "";
                alias_ = 0L;
                timestamp_ = 0L;
                datatype_ = 0;
                isHistorical_ = false;
                isTransient_ = false;
                isNull_ = false;
                metadata_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance();
                properties_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
            }
            private byte memoizedIsInitialized = -1;
            public final boolean isInitialized() {
                byte isInitialized = memoizedIsInitialized;
                if (isInitialized == 1) return true;
                if (isInitialized == 0) return false;

                if (hasMetadata()) {
                    if (!getMetadata().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasProperties()) {
                    if (!getProperties().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasDatasetValue()) {
                    if (!getDatasetValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasTemplateValue()) {
                    if (!getTemplateValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                if (hasExtensionValue()) {
                    if (!getExtensionValue().isInitialized()) {
                        memoizedIsInitialized = 0;
                        return false;
                    }
                }
                memoizedIsInitialized = 1;
                return true;
            }

            public void writeTo(com.google.protobuf.CodedOutputStream output)
                    throws java.io.IOException {
                getSerializedSize();
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    output.writeBytes(1, getNameBytes());
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    output.writeUInt64(2, alias_);
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    output.writeUInt64(3, timestamp_);
                }
                if (((bitField0_ & 0x00000008) == 0x00000008)) {
                    output.writeUInt32(4, datatype_);
                }
                if (((bitField0_ & 0x00000010) == 0x00000010)) {
                    output.writeBool(5, isHistorical_);
                }
                if (((bitField0_ & 0x00000020) == 0x00000020)) {
                    output.writeBool(6, isTransient_);
                }
                if (((bitField0_ & 0x00000040) == 0x00000040)) {
                    output.writeBool(7, isNull_);
                }
                if (((bitField0_ & 0x00000080) == 0x00000080)) {
                    output.writeMessage(8, metadata_);
                }
                if (((bitField0_ & 0x00000100) == 0x00000100)) {
                    output.writeMessage(9, properties_);
                }
                if (valueCase_ == 10) {
                    output.writeUInt32(
                            10, (int)((java.lang.Integer) value_));
                }
                if (valueCase_ == 11) {
                    output.writeUInt64(
                            11, (long)((java.lang.Long) value_));
                }
                if (valueCase_ == 12) {
                    output.writeFloat(
                            12, (float)((java.lang.Float) value_));
                }
                if (valueCase_ == 13) {
                    output.writeDouble(
                            13, (double)((java.lang.Double) value_));
                }
                if (valueCase_ == 14) {
                    output.writeBool(
                            14, (boolean)((java.lang.Boolean) value_));
                }
                if (valueCase_ == 15) {
                    output.writeBytes(15, getStringValueBytes());
                }
                if (valueCase_ == 16) {
                    output.writeBytes(
                            16, (com.google.protobuf.ByteString)((com.google.protobuf.ByteString) value_));
                }
                if (valueCase_ == 17) {
                    output.writeMessage(17, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_);
                }
                if (valueCase_ == 18) {
                    output.writeMessage(18, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_);
                }
                if (valueCase_ == 19) {
                    output.writeMessage(19, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_);
                }
                getUnknownFields().writeTo(output);
            }

            private int memoizedSerializedSize = -1;
            public int getSerializedSize() {
                int size = memoizedSerializedSize;
                if (size != -1) return size;

                size = 0;
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(1, getNameBytes());
                }
                if (((bitField0_ & 0x00000002) == 0x00000002)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(2, alias_);
                }
                if (((bitField0_ & 0x00000004) == 0x00000004)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(3, timestamp_);
                }
                if (((bitField0_ & 0x00000008) == 0x00000008)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt32Size(4, datatype_);
                }
                if (((bitField0_ & 0x00000010) == 0x00000010)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(5, isHistorical_);
                }
                if (((bitField0_ & 0x00000020) == 0x00000020)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(6, isTransient_);
                }
                if (((bitField0_ & 0x00000040) == 0x00000040)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(7, isNull_);
                }
                if (((bitField0_ & 0x00000080) == 0x00000080)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(8, metadata_);
                }
                if (((bitField0_ & 0x00000100) == 0x00000100)) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(9, properties_);
                }
                if (valueCase_ == 10) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt32Size(
                                    10, (int)((java.lang.Integer) value_));
                }
                if (valueCase_ == 11) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeUInt64Size(
                                    11, (long)((java.lang.Long) value_));
                }
                if (valueCase_ == 12) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeFloatSize(
                                    12, (float)((java.lang.Float) value_));
                }
                if (valueCase_ == 13) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeDoubleSize(
                                    13, (double)((java.lang.Double) value_));
                }
                if (valueCase_ == 14) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBoolSize(
                                    14, (boolean)((java.lang.Boolean) value_));
                }
                if (valueCase_ == 15) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(15, getStringValueBytes());
                }
                if (valueCase_ == 16) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeBytesSize(
                                    16, (com.google.protobuf.ByteString)((com.google.protobuf.ByteString) value_));
                }
                if (valueCase_ == 17) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(17, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_);
                }
                if (valueCase_ == 18) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(18, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_);
                }
                if (valueCase_ == 19) {
                    size += com.google.protobuf.CodedOutputStream
                            .computeMessageSize(19, (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_);
                }
                size += getUnknownFields().getSerializedSize();
                memoizedSerializedSize = size;
                return size;
            }

            private static final long serialVersionUID = 0L;
            @java.lang.Override
            protected java.lang.Object writeReplace()
                    throws java.io.ObjectStreamException {
                return super.writeReplace();
            }

            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    com.google.protobuf.ByteString data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    com.google.protobuf.ByteString data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(byte[] data)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    byte[] data,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                return PARSER.parseFrom(data, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseDelimitedFrom(java.io.InputStream input)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseDelimitedFrom(
                    java.io.InputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseDelimitedFrom(input, extensionRegistry);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    com.google.protobuf.CodedInputStream input)
                    throws java.io.IOException {
                return PARSER.parseFrom(input);
            }
            public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parseFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                return PARSER.parseFrom(input, extensionRegistry);
            }

            public static Builder newBuilder() { return Builder.create(); }
            public Builder newBuilderForType() { return newBuilder(); }
            public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric prototype) {
                return newBuilder().mergeFrom(prototype);
            }
            public Builder toBuilder() { return newBuilder(this); }

            @java.lang.Override
            protected Builder newBuilderForType(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                Builder builder = new Builder(parent);
                return builder;
            }
            /**
             * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric}
             */
            public static final class Builder extends
                    com.google.protobuf.GeneratedMessage.Builder<Builder> implements
                    // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric)
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder {
                public static final com.google.protobuf.Descriptors.Descriptor
                getDescriptor() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor;
                }

                protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
                internalGetFieldAccessorTable() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_fieldAccessorTable
                            .ensureFieldAccessorsInitialized(
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder.class);
                }

                // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.newBuilder()
                private Builder() {
                    maybeForceBuilderInitialization();
                }

                private Builder(
                        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                    super(parent);
                    maybeForceBuilderInitialization();
                }
                private void maybeForceBuilderInitialization() {
                    if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                        getMetadataFieldBuilder();
                        getPropertiesFieldBuilder();
                    }
                }
                private static Builder create() {
                    return new Builder();
                }

                public Builder clear() {
                    super.clear();
                    name_ = "";
                    bitField0_ = (bitField0_ & ~0x00000001);
                    alias_ = 0L;
                    bitField0_ = (bitField0_ & ~0x00000002);
                    timestamp_ = 0L;
                    bitField0_ = (bitField0_ & ~0x00000004);
                    datatype_ = 0;
                    bitField0_ = (bitField0_ & ~0x00000008);
                    isHistorical_ = false;
                    bitField0_ = (bitField0_ & ~0x00000010);
                    isTransient_ = false;
                    bitField0_ = (bitField0_ & ~0x00000020);
                    isNull_ = false;
                    bitField0_ = (bitField0_ & ~0x00000040);
                    if (metadataBuilder_ == null) {
                        metadata_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance();
                    } else {
                        metadataBuilder_.clear();
                    }
                    bitField0_ = (bitField0_ & ~0x00000080);
                    if (propertiesBuilder_ == null) {
                        properties_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                    } else {
                        propertiesBuilder_.clear();
                    }
                    bitField0_ = (bitField0_ & ~0x00000100);
                    valueCase_ = 0;
                    value_ = null;
                    return this;
                }

                public Builder clone() {
                    return create().mergeFrom(buildPartial());
                }

                public com.google.protobuf.Descriptors.Descriptor
                getDescriptorForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getDefaultInstanceForType() {
                    return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance();
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric build() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric result = buildPartial();
                    if (!result.isInitialized()) {
                        throw newUninitializedMessageException(result);
                    }
                    return result;
                }

                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric buildPartial() {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric(this);
                    int from_bitField0_ = bitField0_;
                    int to_bitField0_ = 0;
                    if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                        to_bitField0_ |= 0x00000001;
                    }
                    result.name_ = name_;
                    if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
                        to_bitField0_ |= 0x00000002;
                    }
                    result.alias_ = alias_;
                    if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
                        to_bitField0_ |= 0x00000004;
                    }
                    result.timestamp_ = timestamp_;
                    if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
                        to_bitField0_ |= 0x00000008;
                    }
                    result.datatype_ = datatype_;
                    if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
                        to_bitField0_ |= 0x00000010;
                    }
                    result.isHistorical_ = isHistorical_;
                    if (((from_bitField0_ & 0x00000020) == 0x00000020)) {
                        to_bitField0_ |= 0x00000020;
                    }
                    result.isTransient_ = isTransient_;
                    if (((from_bitField0_ & 0x00000040) == 0x00000040)) {
                        to_bitField0_ |= 0x00000040;
                    }
                    result.isNull_ = isNull_;
                    if (((from_bitField0_ & 0x00000080) == 0x00000080)) {
                        to_bitField0_ |= 0x00000080;
                    }
                    if (metadataBuilder_ == null) {
                        result.metadata_ = metadata_;
                    } else {
                        result.metadata_ = metadataBuilder_.build();
                    }
                    if (((from_bitField0_ & 0x00000100) == 0x00000100)) {
                        to_bitField0_ |= 0x00000100;
                    }
                    if (propertiesBuilder_ == null) {
                        result.properties_ = properties_;
                    } else {
                        result.properties_ = propertiesBuilder_.build();
                    }
                    if (valueCase_ == 10) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 11) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 12) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 13) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 14) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 15) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 16) {
                        result.value_ = value_;
                    }
                    if (valueCase_ == 17) {
                        if (datasetValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = datasetValueBuilder_.build();
                        }
                    }
                    if (valueCase_ == 18) {
                        if (templateValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = templateValueBuilder_.build();
                        }
                    }
                    if (valueCase_ == 19) {
                        if (extensionValueBuilder_ == null) {
                            result.value_ = value_;
                        } else {
                            result.value_ = extensionValueBuilder_.build();
                        }
                    }
                    result.bitField0_ = to_bitField0_;
                    result.valueCase_ = valueCase_;
                    onBuilt();
                    return result;
                }

                public Builder mergeFrom(com.google.protobuf.Message other) {
                    if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric) {
                        return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric)other);
                    } else {
                        super.mergeFrom(other);
                        return this;
                    }
                }

                public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric other) {
                    if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance()) return this;
                    if (other.hasName()) {
                        bitField0_ |= 0x00000001;
                        name_ = other.name_;
                        onChanged();
                    }
                    if (other.hasAlias()) {
                        setAlias(other.getAlias());
                    }
                    if (other.hasTimestamp()) {
                        setTimestamp(other.getTimestamp());
                    }
                    if (other.hasDatatype()) {
                        setDatatype(other.getDatatype());
                    }
                    if (other.hasIsHistorical()) {
                        setIsHistorical(other.getIsHistorical());
                    }
                    if (other.hasIsTransient()) {
                        setIsTransient(other.getIsTransient());
                    }
                    if (other.hasIsNull()) {
                        setIsNull(other.getIsNull());
                    }
                    if (other.hasMetadata()) {
                        mergeMetadata(other.getMetadata());
                    }
                    if (other.hasProperties()) {
                        mergeProperties(other.getProperties());
                    }
                    switch (other.getValueCase()) {
                        case INT_VALUE: {
                            setIntValue(other.getIntValue());
                            break;
                        }
                        case LONG_VALUE: {
                            setLongValue(other.getLongValue());
                            break;
                        }
                        case FLOAT_VALUE: {
                            setFloatValue(other.getFloatValue());
                            break;
                        }
                        case DOUBLE_VALUE: {
                            setDoubleValue(other.getDoubleValue());
                            break;
                        }
                        case BOOLEAN_VALUE: {
                            setBooleanValue(other.getBooleanValue());
                            break;
                        }
                        case STRING_VALUE: {
                            valueCase_ = 15;
                            value_ = other.value_;
                            onChanged();
                            break;
                        }
                        case BYTES_VALUE: {
                            setBytesValue(other.getBytesValue());
                            break;
                        }
                        case DATASET_VALUE: {
                            mergeDatasetValue(other.getDatasetValue());
                            break;
                        }
                        case TEMPLATE_VALUE: {
                            mergeTemplateValue(other.getTemplateValue());
                            break;
                        }
                        case EXTENSION_VALUE: {
                            mergeExtensionValue(other.getExtensionValue());
                            break;
                        }
                        case VALUE_NOT_SET: {
                            break;
                        }
                    }
                    this.mergeUnknownFields(other.getUnknownFields());
                    return this;
                }

                public final boolean isInitialized() {
                    if (hasMetadata()) {
                        if (!getMetadata().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasProperties()) {
                        if (!getProperties().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasDatasetValue()) {
                        if (!getDatasetValue().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasTemplateValue()) {
                        if (!getTemplateValue().isInitialized()) {

                            return false;
                        }
                    }
                    if (hasExtensionValue()) {
                        if (!getExtensionValue().isInitialized()) {

                            return false;
                        }
                    }
                    return true;
                }

                public Builder mergeFrom(
                        com.google.protobuf.CodedInputStream input,
                        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                        throws java.io.IOException {
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric parsedMessage = null;
                    try {
                        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                        parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric) e.getUnfinishedMessage();
                        throw e;
                    } finally {
                        if (parsedMessage != null) {
                            mergeFrom(parsedMessage);
                        }
                    }
                    return this;
                }
                private int valueCase_ = 0;
                private java.lang.Object value_;
                public ValueCase
                getValueCase() {
                    return ValueCase.valueOf(
                            valueCase_);
                }

                public Builder clearValue() {
                    valueCase_ = 0;
                    value_ = null;
                    onChanged();
                    return this;
                }

                private int bitField0_;

                private java.lang.Object name_ = "";
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public boolean hasName() {
                    return ((bitField0_ & 0x00000001) == 0x00000001);
                }
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public java.lang.String getName() {
                    java.lang.Object ref = name_;
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (bs.isValidUtf8()) {
                            name_ = s;
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public com.google.protobuf.ByteString
                getNameBytes() {
                    java.lang.Object ref = name_;
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        name_ = b;
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public Builder setName(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000001;
                    name_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public Builder clearName() {
                    bitField0_ = (bitField0_ & ~0x00000001);
                    name_ = getDefaultInstance().getName();
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string name = 1;</code>
                 *
                 * <pre>
                 * Metric name - should only be included on birth
                 * </pre>
                 */
                public Builder setNameBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    bitField0_ |= 0x00000001;
                    name_ = value;
                    onChanged();
                    return this;
                }

                private long alias_ ;
                /**
                 * <code>optional uint64 alias = 2;</code>
                 *
                 * <pre>
                 * Metric alias - tied to name on birth and included in all later DATA messages
                 * </pre>
                 */
                public boolean hasAlias() {
                    return ((bitField0_ & 0x00000002) == 0x00000002);
                }
                /**
                 * <code>optional uint64 alias = 2;</code>
                 *
                 * <pre>
                 * Metric alias - tied to name on birth and included in all later DATA messages
                 * </pre>
                 */
                public long getAlias() {
                    return alias_;
                }
                /**
                 * <code>optional uint64 alias = 2;</code>
                 *
                 * <pre>
                 * Metric alias - tied to name on birth and included in all later DATA messages
                 * </pre>
                 */
                public Builder setAlias(long value) {
                    bitField0_ |= 0x00000002;
                    alias_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 alias = 2;</code>
                 *
                 * <pre>
                 * Metric alias - tied to name on birth and included in all later DATA messages
                 * </pre>
                 */
                public Builder clearAlias() {
                    bitField0_ = (bitField0_ & ~0x00000002);
                    alias_ = 0L;
                    onChanged();
                    return this;
                }

                private long timestamp_ ;
                /**
                 * <code>optional uint64 timestamp = 3;</code>
                 *
                 * <pre>
                 * Timestamp associated with data acquisition time
                 * </pre>
                 */
                public boolean hasTimestamp() {
                    return ((bitField0_ & 0x00000004) == 0x00000004);
                }
                /**
                 * <code>optional uint64 timestamp = 3;</code>
                 *
                 * <pre>
                 * Timestamp associated with data acquisition time
                 * </pre>
                 */
                public long getTimestamp() {
                    return timestamp_;
                }
                /**
                 * <code>optional uint64 timestamp = 3;</code>
                 *
                 * <pre>
                 * Timestamp associated with data acquisition time
                 * </pre>
                 */
                public Builder setTimestamp(long value) {
                    bitField0_ |= 0x00000004;
                    timestamp_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 timestamp = 3;</code>
                 *
                 * <pre>
                 * Timestamp associated with data acquisition time
                 * </pre>
                 */
                public Builder clearTimestamp() {
                    bitField0_ = (bitField0_ & ~0x00000004);
                    timestamp_ = 0L;
                    onChanged();
                    return this;
                }

                private int datatype_ ;
                /**
                 * <code>optional uint32 datatype = 4;</code>
                 *
                 * <pre>
                 * DataType of the metric/tag value
                 * </pre>
                 */
                public boolean hasDatatype() {
                    return ((bitField0_ & 0x00000008) == 0x00000008);
                }
                /**
                 * <code>optional uint32 datatype = 4;</code>
                 *
                 * <pre>
                 * DataType of the metric/tag value
                 * </pre>
                 */
                public int getDatatype() {
                    return datatype_;
                }
                /**
                 * <code>optional uint32 datatype = 4;</code>
                 *
                 * <pre>
                 * DataType of the metric/tag value
                 * </pre>
                 */
                public Builder setDatatype(int value) {
                    bitField0_ |= 0x00000008;
                    datatype_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint32 datatype = 4;</code>
                 *
                 * <pre>
                 * DataType of the metric/tag value
                 * </pre>
                 */
                public Builder clearDatatype() {
                    bitField0_ = (bitField0_ & ~0x00000008);
                    datatype_ = 0;
                    onChanged();
                    return this;
                }

                private boolean isHistorical_ ;
                /**
                 * <code>optional bool is_historical = 5;</code>
                 *
                 * <pre>
                 * If this is historical data and should not update real time tag
                 * </pre>
                 */
                public boolean hasIsHistorical() {
                    return ((bitField0_ & 0x00000010) == 0x00000010);
                }
                /**
                 * <code>optional bool is_historical = 5;</code>
                 *
                 * <pre>
                 * If this is historical data and should not update real time tag
                 * </pre>
                 */
                public boolean getIsHistorical() {
                    return isHistorical_;
                }
                /**
                 * <code>optional bool is_historical = 5;</code>
                 *
                 * <pre>
                 * If this is historical data and should not update real time tag
                 * </pre>
                 */
                public Builder setIsHistorical(boolean value) {
                    bitField0_ |= 0x00000010;
                    isHistorical_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_historical = 5;</code>
                 *
                 * <pre>
                 * If this is historical data and should not update real time tag
                 * </pre>
                 */
                public Builder clearIsHistorical() {
                    bitField0_ = (bitField0_ & ~0x00000010);
                    isHistorical_ = false;
                    onChanged();
                    return this;
                }

                private boolean isTransient_ ;
                /**
                 * <code>optional bool is_transient = 6;</code>
                 *
                 * <pre>
                 * Tells consuming clients such as MQTT Engine to not store this as a tag
                 * </pre>
                 */
                public boolean hasIsTransient() {
                    return ((bitField0_ & 0x00000020) == 0x00000020);
                }
                /**
                 * <code>optional bool is_transient = 6;</code>
                 *
                 * <pre>
                 * Tells consuming clients such as MQTT Engine to not store this as a tag
                 * </pre>
                 */
                public boolean getIsTransient() {
                    return isTransient_;
                }
                /**
                 * <code>optional bool is_transient = 6;</code>
                 *
                 * <pre>
                 * Tells consuming clients such as MQTT Engine to not store this as a tag
                 * </pre>
                 */
                public Builder setIsTransient(boolean value) {
                    bitField0_ |= 0x00000020;
                    isTransient_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_transient = 6;</code>
                 *
                 * <pre>
                 * Tells consuming clients such as MQTT Engine to not store this as a tag
                 * </pre>
                 */
                public Builder clearIsTransient() {
                    bitField0_ = (bitField0_ & ~0x00000020);
                    isTransient_ = false;
                    onChanged();
                    return this;
                }

                private boolean isNull_ ;
                /**
                 * <code>optional bool is_null = 7;</code>
                 *
                 * <pre>
                 * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
                 * </pre>
                 */
                public boolean hasIsNull() {
                    return ((bitField0_ & 0x00000040) == 0x00000040);
                }
                /**
                 * <code>optional bool is_null = 7;</code>
                 *
                 * <pre>
                 * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
                 * </pre>
                 */
                public boolean getIsNull() {
                    return isNull_;
                }
                /**
                 * <code>optional bool is_null = 7;</code>
                 *
                 * <pre>
                 * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
                 * </pre>
                 */
                public Builder setIsNull(boolean value) {
                    bitField0_ |= 0x00000040;
                    isNull_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool is_null = 7;</code>
                 *
                 * <pre>
                 * If this is null - explicitly say so rather than using -1, false, etc for some datatypes.
                 * </pre>
                 */
                public Builder clearIsNull() {
                    bitField0_ = (bitField0_ & ~0x00000040);
                    isNull_ = false;
                    onChanged();
                    return this;
                }

                private org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData metadata_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance();
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder> metadataBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public boolean hasMetadata() {
                    return ((bitField0_ & 0x00000080) == 0x00000080);
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData getMetadata() {
                    if (metadataBuilder_ == null) {
                        return metadata_;
                    } else {
                        return metadataBuilder_.getMessage();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public Builder setMetadata(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData value) {
                    if (metadataBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        metadata_ = value;
                        onChanged();
                    } else {
                        metadataBuilder_.setMessage(value);
                    }
                    bitField0_ |= 0x00000080;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public Builder setMetadata(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder builderForValue) {
                    if (metadataBuilder_ == null) {
                        metadata_ = builderForValue.build();
                        onChanged();
                    } else {
                        metadataBuilder_.setMessage(builderForValue.build());
                    }
                    bitField0_ |= 0x00000080;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public Builder mergeMetadata(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData value) {
                    if (metadataBuilder_ == null) {
                        if (((bitField0_ & 0x00000080) == 0x00000080) &&
                                metadata_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance()) {
                            metadata_ =
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.newBuilder(metadata_).mergeFrom(value).buildPartial();
                        } else {
                            metadata_ = value;
                        }
                        onChanged();
                    } else {
                        metadataBuilder_.mergeFrom(value);
                    }
                    bitField0_ |= 0x00000080;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public Builder clearMetadata() {
                    if (metadataBuilder_ == null) {
                        metadata_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.getDefaultInstance();
                        onChanged();
                    } else {
                        metadataBuilder_.clear();
                    }
                    bitField0_ = (bitField0_ & ~0x00000080);
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder getMetadataBuilder() {
                    bitField0_ |= 0x00000080;
                    onChanged();
                    return getMetadataFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder getMetadataOrBuilder() {
                    if (metadataBuilder_ != null) {
                        return metadataBuilder_.getMessageOrBuilder();
                    } else {
                        return metadata_;
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.MetaData metadata = 8;</code>
                 *
                 * <pre>
                 * Metadata for the payload
                 * </pre>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder>
                getMetadataFieldBuilder() {
                    if (metadataBuilder_ == null) {
                        metadataBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaData.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetaDataOrBuilder>(
                                getMetadata(),
                                getParentForChildren(),
                                isClean());
                        metadata_ = null;
                    }
                    return metadataBuilder_;
                }

                private org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet properties_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder> propertiesBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public boolean hasProperties() {
                    return ((bitField0_ & 0x00000100) == 0x00000100);
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet getProperties() {
                    if (propertiesBuilder_ == null) {
                        return properties_;
                    } else {
                        return propertiesBuilder_.getMessage();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public Builder setProperties(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertiesBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        properties_ = value;
                        onChanged();
                    } else {
                        propertiesBuilder_.setMessage(value);
                    }
                    bitField0_ |= 0x00000100;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public Builder setProperties(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder builderForValue) {
                    if (propertiesBuilder_ == null) {
                        properties_ = builderForValue.build();
                        onChanged();
                    } else {
                        propertiesBuilder_.setMessage(builderForValue.build());
                    }
                    bitField0_ |= 0x00000100;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public Builder mergeProperties(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet value) {
                    if (propertiesBuilder_ == null) {
                        if (((bitField0_ & 0x00000100) == 0x00000100) &&
                                properties_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance()) {
                            properties_ =
                                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.newBuilder(properties_).mergeFrom(value).buildPartial();
                        } else {
                            properties_ = value;
                        }
                        onChanged();
                    } else {
                        propertiesBuilder_.mergeFrom(value);
                    }
                    bitField0_ |= 0x00000100;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public Builder clearProperties() {
                    if (propertiesBuilder_ == null) {
                        properties_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.getDefaultInstance();
                        onChanged();
                    } else {
                        propertiesBuilder_.clear();
                    }
                    bitField0_ = (bitField0_ & ~0x00000100);
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder getPropertiesBuilder() {
                    bitField0_ |= 0x00000100;
                    onChanged();
                    return getPropertiesFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder getPropertiesOrBuilder() {
                    if (propertiesBuilder_ != null) {
                        return propertiesBuilder_.getMessageOrBuilder();
                    } else {
                        return properties_;
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.PropertySet properties = 9;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>
                getPropertiesFieldBuilder() {
                    if (propertiesBuilder_ == null) {
                        propertiesBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.PropertySetOrBuilder>(
                                getProperties(),
                                getParentForChildren(),
                                isClean());
                        properties_ = null;
                    }
                    return propertiesBuilder_;
                }

                /**
                 * <code>optional uint32 int_value = 10;</code>
                 */
                public boolean hasIntValue() {
                    return valueCase_ == 10;
                }
                /**
                 * <code>optional uint32 int_value = 10;</code>
                 */
                public int getIntValue() {
                    if (valueCase_ == 10) {
                        return (java.lang.Integer) value_;
                    }
                    return 0;
                }
                /**
                 * <code>optional uint32 int_value = 10;</code>
                 */
                public Builder setIntValue(int value) {
                    valueCase_ = 10;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint32 int_value = 10;</code>
                 */
                public Builder clearIntValue() {
                    if (valueCase_ == 10) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional uint64 long_value = 11;</code>
                 */
                public boolean hasLongValue() {
                    return valueCase_ == 11;
                }
                /**
                 * <code>optional uint64 long_value = 11;</code>
                 */
                public long getLongValue() {
                    if (valueCase_ == 11) {
                        return (java.lang.Long) value_;
                    }
                    return 0L;
                }
                /**
                 * <code>optional uint64 long_value = 11;</code>
                 */
                public Builder setLongValue(long value) {
                    valueCase_ = 11;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional uint64 long_value = 11;</code>
                 */
                public Builder clearLongValue() {
                    if (valueCase_ == 11) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional float float_value = 12;</code>
                 */
                public boolean hasFloatValue() {
                    return valueCase_ == 12;
                }
                /**
                 * <code>optional float float_value = 12;</code>
                 */
                public float getFloatValue() {
                    if (valueCase_ == 12) {
                        return (java.lang.Float) value_;
                    }
                    return 0F;
                }
                /**
                 * <code>optional float float_value = 12;</code>
                 */
                public Builder setFloatValue(float value) {
                    valueCase_ = 12;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional float float_value = 12;</code>
                 */
                public Builder clearFloatValue() {
                    if (valueCase_ == 12) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional double double_value = 13;</code>
                 */
                public boolean hasDoubleValue() {
                    return valueCase_ == 13;
                }
                /**
                 * <code>optional double double_value = 13;</code>
                 */
                public double getDoubleValue() {
                    if (valueCase_ == 13) {
                        return (java.lang.Double) value_;
                    }
                    return 0D;
                }
                /**
                 * <code>optional double double_value = 13;</code>
                 */
                public Builder setDoubleValue(double value) {
                    valueCase_ = 13;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional double double_value = 13;</code>
                 */
                public Builder clearDoubleValue() {
                    if (valueCase_ == 13) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional bool boolean_value = 14;</code>
                 */
                public boolean hasBooleanValue() {
                    return valueCase_ == 14;
                }
                /**
                 * <code>optional bool boolean_value = 14;</code>
                 */
                public boolean getBooleanValue() {
                    if (valueCase_ == 14) {
                        return (java.lang.Boolean) value_;
                    }
                    return false;
                }
                /**
                 * <code>optional bool boolean_value = 14;</code>
                 */
                public Builder setBooleanValue(boolean value) {
                    valueCase_ = 14;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bool boolean_value = 14;</code>
                 */
                public Builder clearBooleanValue() {
                    if (valueCase_ == 14) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public boolean hasStringValue() {
                    return valueCase_ == 15;
                }
                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public java.lang.String getStringValue() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 15) {
                        ref = value_;
                    }
                    if (!(ref instanceof java.lang.String)) {
                        com.google.protobuf.ByteString bs =
                                (com.google.protobuf.ByteString) ref;
                        java.lang.String s = bs.toStringUtf8();
                        if (valueCase_ == 15) {
                            if (bs.isValidUtf8()) {
                                value_ = s;
                            }
                        }
                        return s;
                    } else {
                        return (java.lang.String) ref;
                    }
                }
                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public com.google.protobuf.ByteString
                getStringValueBytes() {
                    java.lang.Object ref = "";
                    if (valueCase_ == 15) {
                        ref = value_;
                    }
                    if (ref instanceof String) {
                        com.google.protobuf.ByteString b =
                                com.google.protobuf.ByteString.copyFromUtf8(
                                        (java.lang.String) ref);
                        if (valueCase_ == 15) {
                            value_ = b;
                        }
                        return b;
                    } else {
                        return (com.google.protobuf.ByteString) ref;
                    }
                }
                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public Builder setStringValue(
                        java.lang.String value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    valueCase_ = 15;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public Builder clearStringValue() {
                    if (valueCase_ == 15) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }
                /**
                 * <code>optional string string_value = 15;</code>
                 */
                public Builder setStringValueBytes(
                        com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    valueCase_ = 15;
                    value_ = value;
                    onChanged();
                    return this;
                }

                /**
                 * <code>optional bytes bytes_value = 16;</code>
                 *
                 * <pre>
                 * Bytes, File
                 * </pre>
                 */
                public boolean hasBytesValue() {
                    return valueCase_ == 16;
                }
                /**
                 * <code>optional bytes bytes_value = 16;</code>
                 *
                 * <pre>
                 * Bytes, File
                 * </pre>
                 */
                public com.google.protobuf.ByteString getBytesValue() {
                    if (valueCase_ == 16) {
                        return (com.google.protobuf.ByteString) value_;
                    }
                    return com.google.protobuf.ByteString.EMPTY;
                }
                /**
                 * <code>optional bytes bytes_value = 16;</code>
                 *
                 * <pre>
                 * Bytes, File
                 * </pre>
                 */
                public Builder setBytesValue(com.google.protobuf.ByteString value) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    valueCase_ = 16;
                    value_ = value;
                    onChanged();
                    return this;
                }
                /**
                 * <code>optional bytes bytes_value = 16;</code>
                 *
                 * <pre>
                 * Bytes, File
                 * </pre>
                 */
                public Builder clearBytesValue() {
                    if (valueCase_ == 16) {
                        valueCase_ = 0;
                        value_ = null;
                        onChanged();
                    }
                    return this;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder> datasetValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public boolean hasDatasetValue() {
                    return valueCase_ == 17;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet getDatasetValue() {
                    if (datasetValueBuilder_ == null) {
                        if (valueCase_ == 17) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
                    } else {
                        if (valueCase_ == 17) {
                            return datasetValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public Builder setDatasetValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet value) {
                    if (datasetValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        datasetValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 17;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public Builder setDatasetValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder builderForValue) {
                    if (datasetValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        datasetValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 17;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public Builder mergeDatasetValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet value) {
                    if (datasetValueBuilder_ == null) {
                        if (valueCase_ == 17 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 17) {
                            datasetValueBuilder_.mergeFrom(value);
                        }
                        datasetValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 17;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public Builder clearDatasetValue() {
                    if (datasetValueBuilder_ == null) {
                        if (valueCase_ == 17) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 17) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        datasetValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder getDatasetValueBuilder() {
                    return getDatasetValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder getDatasetValueOrBuilder() {
                    if ((valueCase_ == 17) && (datasetValueBuilder_ != null)) {
                        return datasetValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 17) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.DataSet dataset_value = 17;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder>
                getDatasetValueFieldBuilder() {
                    if (datasetValueBuilder_ == null) {
                        if (!(valueCase_ == 17)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.getDefaultInstance();
                        }
                        datasetValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSetOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.DataSet) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 17;
                    return datasetValueBuilder_;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder> templateValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public boolean hasTemplateValue() {
                    return valueCase_ == 18;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template getTemplateValue() {
                    if (templateValueBuilder_ == null) {
                        if (valueCase_ == 18) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
                    } else {
                        if (valueCase_ == 18) {
                            return templateValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public Builder setTemplateValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template value) {
                    if (templateValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        templateValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 18;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public Builder setTemplateValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder builderForValue) {
                    if (templateValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        templateValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 18;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public Builder mergeTemplateValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template value) {
                    if (templateValueBuilder_ == null) {
                        if (valueCase_ == 18 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 18) {
                            templateValueBuilder_.mergeFrom(value);
                        }
                        templateValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 18;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public Builder clearTemplateValue() {
                    if (templateValueBuilder_ == null) {
                        if (valueCase_ == 18) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 18) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        templateValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder getTemplateValueBuilder() {
                    return getTemplateValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder getTemplateValueOrBuilder() {
                    if ((valueCase_ == 18) && (templateValueBuilder_ != null)) {
                        return templateValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 18) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Template template_value = 18;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder>
                getTemplateValueFieldBuilder() {
                    if (templateValueBuilder_ == null) {
                        if (!(valueCase_ == 18)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.getDefaultInstance();
                        }
                        templateValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.TemplateOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Template) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 18;
                    return templateValueBuilder_;
                }

                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder> extensionValueBuilder_;
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public boolean hasExtensionValue() {
                    return valueCase_ == 19;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension getExtensionValue() {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 19) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
                    } else {
                        if (valueCase_ == 19) {
                            return extensionValueBuilder_.getMessage();
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public Builder setExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension value) {
                    if (extensionValueBuilder_ == null) {
                        if (value == null) {
                            throw new NullPointerException();
                        }
                        value_ = value;
                        onChanged();
                    } else {
                        extensionValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 19;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public Builder setExtensionValue(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder builderForValue) {
                    if (extensionValueBuilder_ == null) {
                        value_ = builderForValue.build();
                        onChanged();
                    } else {
                        extensionValueBuilder_.setMessage(builderForValue.build());
                    }
                    valueCase_ = 19;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public Builder mergeExtensionValue(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension value) {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 19 &&
                                value_ != org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance()) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.newBuilder((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_)
                                    .mergeFrom(value).buildPartial();
                        } else {
                            value_ = value;
                        }
                        onChanged();
                    } else {
                        if (valueCase_ == 19) {
                            extensionValueBuilder_.mergeFrom(value);
                        }
                        extensionValueBuilder_.setMessage(value);
                    }
                    valueCase_ = 19;
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public Builder clearExtensionValue() {
                    if (extensionValueBuilder_ == null) {
                        if (valueCase_ == 19) {
                            valueCase_ = 0;
                            value_ = null;
                            onChanged();
                        }
                    } else {
                        if (valueCase_ == 19) {
                            valueCase_ = 0;
                            value_ = null;
                        }
                        extensionValueBuilder_.clear();
                    }
                    return this;
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder getExtensionValueBuilder() {
                    return getExtensionValueFieldBuilder().getBuilder();
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder getExtensionValueOrBuilder() {
                    if ((valueCase_ == 19) && (extensionValueBuilder_ != null)) {
                        return extensionValueBuilder_.getMessageOrBuilder();
                    } else {
                        if (valueCase_ == 19) {
                            return (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_;
                        }
                        return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
                    }
                }
                /**
                 * <code>optional .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric.MetricValueExtension extension_value = 19;</code>
                 */
                private com.google.protobuf.SingleFieldBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder>
                getExtensionValueFieldBuilder() {
                    if (extensionValueBuilder_ == null) {
                        if (!(valueCase_ == 19)) {
                            value_ = org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.getDefaultInstance();
                        }
                        extensionValueBuilder_ = new com.google.protobuf.SingleFieldBuilder<
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtensionOrBuilder>(
                                (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.MetricValueExtension) value_,
                                getParentForChildren(),
                                isClean());
                        value_ = null;
                    }
                    valueCase_ = 19;
                    return extensionValueBuilder_;
                }

                // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric)
            }

            static {
                defaultInstance = new Metric(true);
                defaultInstance.initFields();
            }

            // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric)
        }

        private int bitField0_;
        public static final int TIMESTAMP_FIELD_NUMBER = 1;
        private long timestamp_;
        /**
         * <code>optional uint64 timestamp = 1;</code>
         *
         * <pre>
         * Timestamp at message sending time
         * </pre>
         */
        public boolean hasTimestamp() {
            return ((bitField0_ & 0x00000001) == 0x00000001);
        }
        /**
         * <code>optional uint64 timestamp = 1;</code>
         *
         * <pre>
         * Timestamp at message sending time
         * </pre>
         */
        public long getTimestamp() {
            return timestamp_;
        }

        public static final int METRICS_FIELD_NUMBER = 2;
        private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> metrics_;
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> getMetricsList() {
            return metrics_;
        }
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
        getMetricsOrBuilderList() {
            return metrics_;
        }
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        public int getMetricsCount() {
            return metrics_.size();
        }
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index) {
            return metrics_.get(index);
        }
        /**
         * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
         *
         * <pre>
         * Repeated forever - no limit in Google Protobufs
         * </pre>
         */
        public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                int index) {
            return metrics_.get(index);
        }

        public static final int SEQ_FIELD_NUMBER = 3;
        private long seq_;
        /**
         * <code>optional uint64 seq = 3;</code>
         *
         * <pre>
         * Sequence number
         * </pre>
         */
        public boolean hasSeq() {
            return ((bitField0_ & 0x00000002) == 0x00000002);
        }
        /**
         * <code>optional uint64 seq = 3;</code>
         *
         * <pre>
         * Sequence number
         * </pre>
         */
        public long getSeq() {
            return seq_;
        }

        public static final int UUID_FIELD_NUMBER = 4;
        private java.lang.Object uuid_;
        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        public boolean hasUuid() {
            return ((bitField0_ & 0x00000004) == 0x00000004);
        }
        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        public java.lang.String getUuid() {
            java.lang.Object ref = uuid_;
            if (ref instanceof java.lang.String) {
                return (java.lang.String) ref;
            } else {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                if (bs.isValidUtf8()) {
                    uuid_ = s;
                }
                return s;
            }
        }
        /**
         * <code>optional string uuid = 4;</code>
         *
         * <pre>
         * UUID to track message type in terms of schema definitions
         * </pre>
         */
        public com.google.protobuf.ByteString
        getUuidBytes() {
            java.lang.Object ref = uuid_;
            if (ref instanceof java.lang.String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                uuid_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        public static final int BODY_FIELD_NUMBER = 5;
        private com.google.protobuf.ByteString body_;
        /**
         * <code>optional bytes body = 5;</code>
         *
         * <pre>
         * To optionally bypass the whole definition above
         * </pre>
         */
        public boolean hasBody() {
            return ((bitField0_ & 0x00000008) == 0x00000008);
        }
        /**
         * <code>optional bytes body = 5;</code>
         *
         * <pre>
         * To optionally bypass the whole definition above
         * </pre>
         */
        public com.google.protobuf.ByteString getBody() {
            return body_;
        }

        private void initFields() {
            timestamp_ = 0L;
            metrics_ = java.util.Collections.emptyList();
            seq_ = 0L;
            uuid_ = "";
            body_ = com.google.protobuf.ByteString.EMPTY;
        }
        private byte memoizedIsInitialized = -1;
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized == 1) return true;
            if (isInitialized == 0) return false;

            for (int i = 0; i < getMetricsCount(); i++) {
                if (!getMetrics(i).isInitialized()) {
                    memoizedIsInitialized = 0;
                    return false;
                }
            }
            if (!extensionsAreInitialized()) {
                memoizedIsInitialized = 0;
                return false;
            }
            memoizedIsInitialized = 1;
            return true;
        }

        public void writeTo(com.google.protobuf.CodedOutputStream output)
                throws java.io.IOException {
            getSerializedSize();
            com.google.protobuf.GeneratedMessage
                    .ExtendableMessage<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload>.ExtensionWriter extensionWriter =
                    newExtensionWriter();
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                output.writeUInt64(1, timestamp_);
            }
            for (int i = 0; i < metrics_.size(); i++) {
                output.writeMessage(2, metrics_.get(i));
            }
            if (((bitField0_ & 0x00000002) == 0x00000002)) {
                output.writeUInt64(3, seq_);
            }
            if (((bitField0_ & 0x00000004) == 0x00000004)) {
                output.writeBytes(4, getUuidBytes());
            }
            if (((bitField0_ & 0x00000008) == 0x00000008)) {
                output.writeBytes(5, body_);
            }
            extensionWriter.writeUntil(536870912, output);
            getUnknownFields().writeTo(output);
        }

        private int memoizedSerializedSize = -1;
        public int getSerializedSize() {
            int size = memoizedSerializedSize;
            if (size != -1) return size;

            size = 0;
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeUInt64Size(1, timestamp_);
            }
            for (int i = 0; i < metrics_.size(); i++) {
                size += com.google.protobuf.CodedOutputStream
                        .computeMessageSize(2, metrics_.get(i));
            }
            if (((bitField0_ & 0x00000002) == 0x00000002)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeUInt64Size(3, seq_);
            }
            if (((bitField0_ & 0x00000004) == 0x00000004)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeBytesSize(4, getUuidBytes());
            }
            if (((bitField0_ & 0x00000008) == 0x00000008)) {
                size += com.google.protobuf.CodedOutputStream
                        .computeBytesSize(5, body_);
            }
            size += extensionsSerializedSize();
            size += getUnknownFields().getSerializedSize();
            memoizedSerializedSize = size;
            return size;
        }

        private static final long serialVersionUID = 0L;
        @java.lang.Override
        protected java.lang.Object writeReplace()
                throws java.io.ObjectStreamException {
            return super.writeReplace();
        }

        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                com.google.protobuf.ByteString data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                com.google.protobuf.ByteString data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(java.io.InputStream input)
                throws java.io.IOException {
            return PARSER.parseFrom(input);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseFrom(input, extensionRegistry);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseDelimitedFrom(java.io.InputStream input)
                throws java.io.IOException {
            return PARSER.parseDelimitedFrom(input);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseDelimitedFrom(
                java.io.InputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseDelimitedFrom(input, extensionRegistry);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                com.google.protobuf.CodedInputStream input)
                throws java.io.IOException {
            return PARSER.parseFrom(input);
        }
        public static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parseFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return PARSER.parseFrom(input, extensionRegistry);
        }

        public static Builder newBuilder() { return Builder.create(); }
        public Builder newBuilderForType() { return newBuilder(); }
        public static Builder newBuilder(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload prototype) {
            return newBuilder().mergeFrom(prototype);
        }
        public Builder toBuilder() { return newBuilder(this); }

        @java.lang.Override
        protected Builder newBuilderForType(
                com.google.protobuf.GeneratedMessage.BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }
        /**
         * Protobuf type {@code org.thingsboard.server.transport.mqtt.util.sparkplug.Payload}
         *
         * <pre>
         * // Indexes of Data Types
         * // Unknown placeholder for future expansion.
         *Unknown         = 0;
         * // Basic Types
         *Int8            = 1;
         *Int16           = 2;
         *Int32           = 3;
         *Int64           = 4;
         *UInt8           = 5;
         *UInt16          = 6;
         *UInt32          = 7;
         *UInt64          = 8;
         *Float           = 9;
         *Double          = 10;
         *Boolean         = 11;
         *String          = 12;
         *DateTime        = 13;
         *Text            = 14;
         * // Additional Metric Types
         *UUID            = 15;
         *DataSet         = 16;
         *Bytes           = 17;
         *File            = 18;
         *Template        = 19;
         * // Additional PropertyValue Types
         *PropertySet     = 20;
         *PropertySetList = 21;
         * </pre>
         */
        public static final class Builder extends
                com.google.protobuf.GeneratedMessage.ExtendableBuilder<
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload, Builder> implements
                // @@protoc_insertion_point(builder_implements:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload)
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.PayloadOrBuilder {
            public static final com.google.protobuf.Descriptors.Descriptor
            getDescriptor() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor;
            }

            protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internalGetFieldAccessorTable() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.class, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Builder.class);
            }

            // Construct using org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.newBuilder()
            private Builder() {
                maybeForceBuilderInitialization();
            }

            private Builder(
                    com.google.protobuf.GeneratedMessage.BuilderParent parent) {
                super(parent);
                maybeForceBuilderInitialization();
            }
            private void maybeForceBuilderInitialization() {
                if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
                    getMetricsFieldBuilder();
                }
            }
            private static Builder create() {
                return new Builder();
            }

            public Builder clear() {
                super.clear();
                timestamp_ = 0L;
                bitField0_ = (bitField0_ & ~0x00000001);
                if (metricsBuilder_ == null) {
                    metrics_ = java.util.Collections.emptyList();
                    bitField0_ = (bitField0_ & ~0x00000002);
                } else {
                    metricsBuilder_.clear();
                }
                seq_ = 0L;
                bitField0_ = (bitField0_ & ~0x00000004);
                uuid_ = "";
                bitField0_ = (bitField0_ & ~0x00000008);
                body_ = com.google.protobuf.ByteString.EMPTY;
                bitField0_ = (bitField0_ & ~0x00000010);
                return this;
            }

            public Builder clone() {
                return create().mergeFrom(buildPartial());
            }

            public com.google.protobuf.Descriptors.Descriptor
            getDescriptorForType() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor;
            }

            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload getDefaultInstanceForType() {
                return org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.getDefaultInstance();
            }

            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload build() {
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload buildPartial() {
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload result = new org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload(this);
                int from_bitField0_ = bitField0_;
                int to_bitField0_ = 0;
                if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
                    to_bitField0_ |= 0x00000001;
                }
                result.timestamp_ = timestamp_;
                if (metricsBuilder_ == null) {
                    if (((bitField0_ & 0x00000002) == 0x00000002)) {
                        metrics_ = java.util.Collections.unmodifiableList(metrics_);
                        bitField0_ = (bitField0_ & ~0x00000002);
                    }
                    result.metrics_ = metrics_;
                } else {
                    result.metrics_ = metricsBuilder_.build();
                }
                if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
                    to_bitField0_ |= 0x00000002;
                }
                result.seq_ = seq_;
                if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
                    to_bitField0_ |= 0x00000004;
                }
                result.uuid_ = uuid_;
                if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
                    to_bitField0_ |= 0x00000008;
                }
                result.body_ = body_;
                result.bitField0_ = to_bitField0_;
                onBuilt();
                return result;
            }

            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload) {
                    return mergeFrom((org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload)other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload other) {
                if (other == org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.getDefaultInstance()) return this;
                if (other.hasTimestamp()) {
                    setTimestamp(other.getTimestamp());
                }
                if (metricsBuilder_ == null) {
                    if (!other.metrics_.isEmpty()) {
                        if (metrics_.isEmpty()) {
                            metrics_ = other.metrics_;
                            bitField0_ = (bitField0_ & ~0x00000002);
                        } else {
                            ensureMetricsIsMutable();
                            metrics_.addAll(other.metrics_);
                        }
                        onChanged();
                    }
                } else {
                    if (!other.metrics_.isEmpty()) {
                        if (metricsBuilder_.isEmpty()) {
                            metricsBuilder_.dispose();
                            metricsBuilder_ = null;
                            metrics_ = other.metrics_;
                            bitField0_ = (bitField0_ & ~0x00000002);
                            metricsBuilder_ =
                                    com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                                            getMetricsFieldBuilder() : null;
                        } else {
                            metricsBuilder_.addAllMessages(other.metrics_);
                        }
                    }
                }
                if (other.hasSeq()) {
                    setSeq(other.getSeq());
                }
                if (other.hasUuid()) {
                    bitField0_ |= 0x00000008;
                    uuid_ = other.uuid_;
                    onChanged();
                }
                if (other.hasBody()) {
                    setBody(other.getBody());
                }
                this.mergeExtensionFields(other);
                this.mergeUnknownFields(other.getUnknownFields());
                return this;
            }

            public final boolean isInitialized() {
                for (int i = 0; i < getMetricsCount(); i++) {
                    if (!getMetrics(i).isInitialized()) {

                        return false;
                    }
                }
                if (!extensionsAreInitialized()) {

                    return false;
                }
                return true;
            }

            public Builder mergeFrom(
                    com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws java.io.IOException {
                org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload parsedMessage = null;
                try {
                    parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    parsedMessage = (org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload) e.getUnfinishedMessage();
                    throw e;
                } finally {
                    if (parsedMessage != null) {
                        mergeFrom(parsedMessage);
                    }
                }
                return this;
            }
            private int bitField0_;

            private long timestamp_ ;
            /**
             * <code>optional uint64 timestamp = 1;</code>
             *
             * <pre>
             * Timestamp at message sending time
             * </pre>
             */
            public boolean hasTimestamp() {
                return ((bitField0_ & 0x00000001) == 0x00000001);
            }
            /**
             * <code>optional uint64 timestamp = 1;</code>
             *
             * <pre>
             * Timestamp at message sending time
             * </pre>
             */
            public long getTimestamp() {
                return timestamp_;
            }
            /**
             * <code>optional uint64 timestamp = 1;</code>
             *
             * <pre>
             * Timestamp at message sending time
             * </pre>
             */
            public Builder setTimestamp(long value) {
                bitField0_ |= 0x00000001;
                timestamp_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional uint64 timestamp = 1;</code>
             *
             * <pre>
             * Timestamp at message sending time
             * </pre>
             */
            public Builder clearTimestamp() {
                bitField0_ = (bitField0_ & ~0x00000001);
                timestamp_ = 0L;
                onChanged();
                return this;
            }

            private java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> metrics_ =
                    java.util.Collections.emptyList();
            private void ensureMetricsIsMutable() {
                if (!((bitField0_ & 0x00000002) == 0x00000002)) {
                    metrics_ = new java.util.ArrayList<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric>(metrics_);
                    bitField0_ |= 0x00000002;
                }
            }

            private com.google.protobuf.RepeatedFieldBuilder<
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder> metricsBuilder_;

            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> getMetricsList() {
                if (metricsBuilder_ == null) {
                    return java.util.Collections.unmodifiableList(metrics_);
                } else {
                    return metricsBuilder_.getMessageList();
                }
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public int getMetricsCount() {
                if (metricsBuilder_ == null) {
                    return metrics_.size();
                } else {
                    return metricsBuilder_.getCount();
                }
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric getMetrics(int index) {
                if (metricsBuilder_ == null) {
                    return metrics_.get(index);
                } else {
                    return metricsBuilder_.getMessage(index);
                }
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder setMetrics(
                    int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                if (metricsBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureMetricsIsMutable();
                    metrics_.set(index, value);
                    onChanged();
                } else {
                    metricsBuilder_.setMessage(index, value);
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder setMetrics(
                    int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                if (metricsBuilder_ == null) {
                    ensureMetricsIsMutable();
                    metrics_.set(index, builderForValue.build());
                    onChanged();
                } else {
                    metricsBuilder_.setMessage(index, builderForValue.build());
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder addMetrics(org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                if (metricsBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureMetricsIsMutable();
                    metrics_.add(value);
                    onChanged();
                } else {
                    metricsBuilder_.addMessage(value);
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder addMetrics(
                    int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric value) {
                if (metricsBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }
                    ensureMetricsIsMutable();
                    metrics_.add(index, value);
                    onChanged();
                } else {
                    metricsBuilder_.addMessage(index, value);
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder addMetrics(
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                if (metricsBuilder_ == null) {
                    ensureMetricsIsMutable();
                    metrics_.add(builderForValue.build());
                    onChanged();
                } else {
                    metricsBuilder_.addMessage(builderForValue.build());
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder addMetrics(
                    int index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder builderForValue) {
                if (metricsBuilder_ == null) {
                    ensureMetricsIsMutable();
                    metrics_.add(index, builderForValue.build());
                    onChanged();
                } else {
                    metricsBuilder_.addMessage(index, builderForValue.build());
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder addAllMetrics(
                    java.lang.Iterable<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric> values) {
                if (metricsBuilder_ == null) {
                    ensureMetricsIsMutable();
                    com.google.protobuf.AbstractMessageLite.Builder.addAll(
                            values, metrics_);
                    onChanged();
                } else {
                    metricsBuilder_.addAllMessages(values);
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder clearMetrics() {
                if (metricsBuilder_ == null) {
                    metrics_ = java.util.Collections.emptyList();
                    bitField0_ = (bitField0_ & ~0x00000002);
                    onChanged();
                } else {
                    metricsBuilder_.clear();
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public Builder removeMetrics(int index) {
                if (metricsBuilder_ == null) {
                    ensureMetricsIsMutable();
                    metrics_.remove(index);
                    onChanged();
                } else {
                    metricsBuilder_.remove(index);
                }
                return this;
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder getMetricsBuilder(
                    int index) {
                return getMetricsFieldBuilder().getBuilder(index);
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder getMetricsOrBuilder(
                    int index) {
                if (metricsBuilder_ == null) {
                    return metrics_.get(index);  } else {
                    return metricsBuilder_.getMessageOrBuilder(index);
                }
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public java.util.List<? extends org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
            getMetricsOrBuilderList() {
                if (metricsBuilder_ != null) {
                    return metricsBuilder_.getMessageOrBuilderList();
                } else {
                    return java.util.Collections.unmodifiableList(metrics_);
                }
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder addMetricsBuilder() {
                return getMetricsFieldBuilder().addBuilder(
                        org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance());
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder addMetricsBuilder(
                    int index) {
                return getMetricsFieldBuilder().addBuilder(
                        index, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.getDefaultInstance());
            }
            /**
             * <code>repeated .org.thingsboard.server.transport.mqtt.util.sparkplug.Payload.Metric metrics = 2;</code>
             *
             * <pre>
             * Repeated forever - no limit in Google Protobufs
             * </pre>
             */
            public java.util.List<org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder>
            getMetricsBuilderList() {
                return getMetricsFieldBuilder().getBuilderList();
            }
            private com.google.protobuf.RepeatedFieldBuilder<
                    org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>
            getMetricsFieldBuilder() {
                if (metricsBuilder_ == null) {
                    metricsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
                            org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.Metric.Builder, org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugBProto.Payload.MetricOrBuilder>(
                            metrics_,
                            ((bitField0_ & 0x00000002) == 0x00000002),
                            getParentForChildren(),
                            isClean());
                    metrics_ = null;
                }
                return metricsBuilder_;
            }

            private long seq_ ;
            /**
             * <code>optional uint64 seq = 3;</code>
             *
             * <pre>
             * Sequence number
             * </pre>
             */
            public boolean hasSeq() {
                return ((bitField0_ & 0x00000004) == 0x00000004);
            }
            /**
             * <code>optional uint64 seq = 3;</code>
             *
             * <pre>
             * Sequence number
             * </pre>
             */
            public long getSeq() {
                return seq_;
            }
            /**
             * <code>optional uint64 seq = 3;</code>
             *
             * <pre>
             * Sequence number
             * </pre>
             */
            public Builder setSeq(long value) {
                bitField0_ |= 0x00000004;
                seq_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional uint64 seq = 3;</code>
             *
             * <pre>
             * Sequence number
             * </pre>
             */
            public Builder clearSeq() {
                bitField0_ = (bitField0_ & ~0x00000004);
                seq_ = 0L;
                onChanged();
                return this;
            }

            private java.lang.Object uuid_ = "";
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public boolean hasUuid() {
                return ((bitField0_ & 0x00000008) == 0x00000008);
            }
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public java.lang.String getUuid() {
                java.lang.Object ref = uuid_;
                if (!(ref instanceof java.lang.String)) {
                    com.google.protobuf.ByteString bs =
                            (com.google.protobuf.ByteString) ref;
                    java.lang.String s = bs.toStringUtf8();
                    if (bs.isValidUtf8()) {
                        uuid_ = s;
                    }
                    return s;
                } else {
                    return (java.lang.String) ref;
                }
            }
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public com.google.protobuf.ByteString
            getUuidBytes() {
                java.lang.Object ref = uuid_;
                if (ref instanceof String) {
                    com.google.protobuf.ByteString b =
                            com.google.protobuf.ByteString.copyFromUtf8(
                                    (java.lang.String) ref);
                    uuid_ = b;
                    return b;
                } else {
                    return (com.google.protobuf.ByteString) ref;
                }
            }
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public Builder setUuid(
                    java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000008;
                uuid_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public Builder clearUuid() {
                bitField0_ = (bitField0_ & ~0x00000008);
                uuid_ = getDefaultInstance().getUuid();
                onChanged();
                return this;
            }
            /**
             * <code>optional string uuid = 4;</code>
             *
             * <pre>
             * UUID to track message type in terms of schema definitions
             * </pre>
             */
            public Builder setUuidBytes(
                    com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000008;
                uuid_ = value;
                onChanged();
                return this;
            }

            private com.google.protobuf.ByteString body_ = com.google.protobuf.ByteString.EMPTY;
            /**
             * <code>optional bytes body = 5;</code>
             *
             * <pre>
             * To optionally bypass the whole definition above
             * </pre>
             */
            public boolean hasBody() {
                return ((bitField0_ & 0x00000010) == 0x00000010);
            }
            /**
             * <code>optional bytes body = 5;</code>
             *
             * <pre>
             * To optionally bypass the whole definition above
             * </pre>
             */
            public com.google.protobuf.ByteString getBody() {
                return body_;
            }
            /**
             * <code>optional bytes body = 5;</code>
             *
             * <pre>
             * To optionally bypass the whole definition above
             * </pre>
             */
            public Builder setBody(com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                bitField0_ |= 0x00000010;
                body_ = value;
                onChanged();
                return this;
            }
            /**
             * <code>optional bytes body = 5;</code>
             *
             * <pre>
             * To optionally bypass the whole definition above
             * </pre>
             */
            public Builder clearBody() {
                bitField0_ = (bitField0_ & ~0x00000010);
                body_ = getDefaultInstance().getBody();
                onChanged();
                return this;
            }

            // @@protoc_insertion_point(builder_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload)
        }

        static {
            defaultInstance = new Payload(true);
            defaultInstance.initFields();
        }

        // @@protoc_insertion_point(class_scope:org.thingsboard.server.transport.mqtt.util.sparkplug.Payload)
    }

    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_fieldAccessorTable;
    private static final com.google.protobuf.Descriptors.Descriptor
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor;
    private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
            internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }
    private static com.google.protobuf.Descriptors.FileDescriptor
            descriptor;
    static {
        java.lang.String[] descriptorData = {
                "\n\035sparkplug_b/sparkplug_b.proto\022!com.cir" +
                        "ruslink.sparkplug.protobuf\"\366\026\n\007Payload\022\021" +
                        "\n\ttimestamp\030\001 \001(\004\022B\n\007metrics\030\002 \003(\01321.com" +
                        ".cirruslink.sparkplug.protobuf.Payload.M" +
                        "etric\022\013\n\003seq\030\003 \001(\004\022\014\n\004uuid\030\004 \001(\t\022\014\n\004body" +
                        "\030\005 \001(\014\032\276\004\n\010Template\022\017\n\007version\030\001 \001(\t\022B\n\007" +
                        "metrics\030\002 \003(\01321.com.cirruslink.sparkplug" +
                        ".protobuf.Payload.Metric\022Q\n\nparameters\030\003" +
                        " \003(\0132=.org.thingsboard.server.transport.mqtt.util.sparkplug" +
                        ".Payload.Template.Parameter\022\024\n\014template_",
                "ref\030\004 \001(\t\022\025\n\ris_definition\030\005 \001(\010\032\322\002\n\tPar" +
                        "ameter\022\014\n\004name\030\001 \001(\t\022\014\n\004type\030\002 \001(\r\022\023\n\tin" +
                        "t_value\030\003 \001(\rH\000\022\024\n\nlong_value\030\004 \001(\004H\000\022\025\n" +
                        "\013float_value\030\005 \001(\002H\000\022\026\n\014double_value\030\006 \001" +
                        "(\001H\000\022\027\n\rboolean_value\030\007 \001(\010H\000\022\026\n\014string_" +
                        "value\030\010 \001(\tH\000\022p\n\017extension_value\030\t \001(\0132U" +
                        ".org.thingsboard.server.transport.mqtt.util.sparkplug.Paylo" +
                        "ad.Template.Parameter.ParameterValueExte" +
                        "nsionH\000\032#\n\027ParameterValueExtension*\010\010\001\020\200" +
                        "\200\200\200\002B\007\n\005value*\010\010\006\020\200\200\200\200\002\032\257\004\n\007DataSet\022\026\n\016n",
                "um_of_columns\030\001 \001(\004\022\017\n\007columns\030\002 \003(\t\022\r\n\005" +
                        "types\030\003 \003(\r\022D\n\004rows\030\004 \003(\01326.com.cirrusli" +
                        "nk.sparkplug.protobuf.Payload.DataSet.Ro" +
                        "w\032\267\002\n\014DataSetValue\022\023\n\tint_value\030\001 \001(\rH\000\022" +
                        "\024\n\nlong_value\030\002 \001(\004H\000\022\025\n\013float_value\030\003 \001" +
                        "(\002H\000\022\026\n\014double_value\030\004 \001(\001H\000\022\027\n\rboolean_" +
                        "value\030\005 \001(\010H\000\022\026\n\014string_value\030\006 \001(\tH\000\022p\n" +
                        "\017extension_value\030\007 \001(\0132U.com.cirruslink." +
                        "sparkplug.protobuf.Payload.DataSet.DataS" +
                        "etValue.DataSetValueExtensionH\000\032!\n\025DataS",
                "etValueExtension*\010\010\001\020\200\200\200\200\002B\007\n\005value\032b\n\003R" +
                        "ow\022Q\n\010elements\030\001 \003(\0132?.com.cirruslink.sp" +
                        "arkplug.protobuf.Payload.DataSet.DataSet" +
                        "Value*\010\010\002\020\200\200\200\200\002*\010\010\005\020\200\200\200\200\002\032\201\004\n\rPropertyVa" +
                        "lue\022\014\n\004type\030\001 \001(\r\022\017\n\007is_null\030\002 \001(\010\022\023\n\tin" +
                        "t_value\030\003 \001(\rH\000\022\024\n\nlong_value\030\004 \001(\004H\000\022\025\n" +
                        "\013float_value\030\005 \001(\002H\000\022\026\n\014double_value\030\006 \001" +
                        "(\001H\000\022\027\n\rboolean_value\030\007 \001(\010H\000\022\026\n\014string_" +
                        "value\030\010 \001(\tH\000\022S\n\021propertyset_value\030\t \001(\013" +
                        "26.org.thingsboard.server.transport.mqtt.util.sparkplug.Pay",
                "load.PropertySetH\000\022X\n\022propertysets_value" +
                        "\030\n \001(\0132:.com.cirruslink.sparkplug.protob" +
                        "uf.Payload.PropertySetListH\000\022j\n\017extensio" +
                        "n_value\030\013 \001(\0132O.com.cirruslink.sparkplug" +
                        ".protobuf.Payload.PropertyValue.Property" +
                        "ValueExtensionH\000\032\"\n\026PropertyValueExtensi" +
                        "on*\010\010\001\020\200\200\200\200\002B\007\n\005value\032o\n\013PropertySet\022\014\n\004" +
                        "keys\030\001 \003(\t\022H\n\006values\030\002 \003(\01328.com.cirrusl" +
                        "ink.sparkplug.protobuf.Payload.PropertyV" +
                        "alue*\010\010\003\020\200\200\200\200\002\032h\n\017PropertySetList\022K\n\013pro",
                "pertyset\030\001 \003(\01326.com.cirruslink.sparkplu" +
                        "g.protobuf.Payload.PropertySet*\010\010\002\020\200\200\200\200\002" +
                        "\032\244\001\n\010MetaData\022\025\n\ris_multi_part\030\001 \001(\010\022\024\n\014" +
                        "content_type\030\002 \001(\t\022\014\n\004size\030\003 \001(\004\022\013\n\003seq\030" +
                        "\004 \001(\004\022\021\n\tfile_name\030\005 \001(\t\022\021\n\tfile_type\030\006 " +
                        "\001(\t\022\013\n\003md5\030\007 \001(\t\022\023\n\013description\030\010 \001(\t*\010\010" +
                        "\t\020\200\200\200\200\002\032\347\005\n\006Metric\022\014\n\004name\030\001 \001(\t\022\r\n\005alia" +
                        "s\030\002 \001(\004\022\021\n\ttimestamp\030\003 \001(\004\022\020\n\010datatype\030\004" +
                        " \001(\r\022\025\n\ris_historical\030\005 \001(\010\022\024\n\014is_transi" +
                        "ent\030\006 \001(\010\022\017\n\007is_null\030\007 \001(\010\022E\n\010metadata\030\010",
                " \001(\01323.org.thingsboard.server.transport.mqtt.util.sparkplug" +
                        ".Payload.MetaData\022J\n\nproperties\030\t \001(\01326." +
                        "org.thingsboard.server.transport.mqtt.util.sparkplug.Payloa" +
                        "d.PropertySet\022\023\n\tint_value\030\n \001(\rH\000\022\024\n\nlo" +
                        "ng_value\030\013 \001(\004H\000\022\025\n\013float_value\030\014 \001(\002H\000\022" +
                        "\026\n\014double_value\030\r \001(\001H\000\022\027\n\rboolean_value" +
                        "\030\016 \001(\010H\000\022\026\n\014string_value\030\017 \001(\tH\000\022\025\n\013byte" +
                        "s_value\030\020 \001(\014H\000\022K\n\rdataset_value\030\021 \001(\01322" +
                        ".org.thingsboard.server.transport.mqtt.util.sparkplug.Paylo" +
                        "ad.DataSetH\000\022M\n\016template_value\030\022 \001(\01323.c",
                "om.cirruslink.sparkplug.protobuf.Payload" +
                        ".TemplateH\000\022a\n\017extension_value\030\023 \001(\0132F.c" +
                        "om.cirruslink.sparkplug.protobuf.Payload" +
                        ".Metric.MetricValueExtensionH\000\032 \n\024Metric" +
                        "ValueExtension*\010\010\001\020\200\200\200\200\002B\007\n\005value*\010\010\006\020\200\200" +
                        "\200\200\002B4\n!org.thingsboard.server.transport.mqtt.util.sparkplug" +
                        "B\017SparkplugBProto"
        };
        com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
                new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
                    public com.google.protobuf.ExtensionRegistry assignDescriptors(
                            com.google.protobuf.Descriptors.FileDescriptor root) {
                        descriptor = root;
                        return null;
                    }
                };
        com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[] {
                        }, assigner);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor,
                new java.lang.String[] { "Timestamp", "Metrics", "Seq", "Uuid", "Body", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor,
                new java.lang.String[] { "Version", "Metrics", "Parameters", "TemplateRef", "IsDefinition", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor,
                new java.lang.String[] { "Name", "Type", "IntValue", "LongValue", "FloatValue", "DoubleValue", "BooleanValue", "StringValue", "ExtensionValue", "Value", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Template_Parameter_ParameterValueExtension_descriptor,
                new java.lang.String[] { });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(1);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor,
                new java.lang.String[] { "NumOfColumns", "Columns", "Types", "Rows", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor,
                new java.lang.String[] { "IntValue", "LongValue", "FloatValue", "DoubleValue", "BooleanValue", "StringValue", "ExtensionValue", "Value", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_DataSetValue_DataSetValueExtension_descriptor,
                new java.lang.String[] { });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_descriptor.getNestedTypes().get(1);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_DataSet_Row_descriptor,
                new java.lang.String[] { "Elements", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(2);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor,
                new java.lang.String[] { "Type", "IsNull", "IntValue", "LongValue", "FloatValue", "DoubleValue", "BooleanValue", "StringValue", "PropertysetValue", "PropertysetsValue", "ExtensionValue", "Value", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertyValue_PropertyValueExtension_descriptor,
                new java.lang.String[] { });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(3);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySet_descriptor,
                new java.lang.String[] { "Keys", "Values", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(4);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_PropertySetList_descriptor,
                new java.lang.String[] { "Propertyset", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(5);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_MetaData_descriptor,
                new java.lang.String[] { "IsMultiPart", "ContentType", "Size", "Seq", "FileName", "FileType", "Md5", "Description", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_descriptor.getNestedTypes().get(6);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor,
                new java.lang.String[] { "Name", "Alias", "Timestamp", "Datatype", "IsHistorical", "IsTransient", "IsNull", "Metadata", "Properties", "IntValue", "LongValue", "FloatValue", "DoubleValue", "BooleanValue", "StringValue", "BytesValue", "DatasetValue", "TemplateValue", "ExtensionValue", "Value", });
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor =
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_descriptor.getNestedTypes().get(0);
        internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessage.FieldAccessorTable(
                internal_static_com_cirruslink_sparkplug_protobuf_Payload_Metric_MetricValueExtension_descriptor,
                new java.lang.String[] { });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
