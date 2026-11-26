// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Describes edge protobuf messages without their payloads, so they can be logged or put into notifications
 * without exposing entity JSON, credentials, tokens or secrets.
 */
public final class EdgeMsgSummaryUtils {

    private static final String MSB = "MSB";
    private static final String LSB = "LSB";

    private EdgeMsgSummaryUtils() {
    }

    // e.g. DownlinkMsg[downlinkMsgId=12, deviceUpdateMsg x2, deviceCredentialsUpdateMsg x1]
    // e.g. DeviceUpdateMsg[msgType=ENTITY_UPDATED_RPC_MESSAGE, id=1d6f1e2c-...]
    public static String toSummary(Message msg) {
        if (msg == null) {
            return "null";
        }
        Map<FieldDescriptor, Object> fields = msg.getAllFields();
        StringJoiner summary = new StringJoiner(", ", msg.getDescriptorForType().getName() + "[", "]");
        fields.forEach((field, value) -> {
            String name = field.getName();
            if (field.isRepeated()) {
                summary.add(name + " x" + ((List<?>) value).size());
                return;
            }
            switch (field.getJavaType()) {
                case INT, LONG -> {
                    FieldDescriptor lsbField = name.endsWith(MSB) ? findPairField(msg, name, MSB, LSB) : null;
                    if (lsbField != null && value instanceof Long msb && msg.getField(lsbField) instanceof Long lsb) {
                        summary.add(name.substring(0, name.length() - MSB.length()) + "=" + new UUID(msb, lsb));
                    } else if (!isPrintedWithPair(msg, fields, name)) {
                        summary.add(name + "=" + value);
                    }
                }
                case BOOLEAN, ENUM -> summary.add(name + "=" + value);
                case MESSAGE -> summary.add(name);
                // strings and bytes may hold entity JSON, credentials or secrets
                default -> {
                }
            }
        });
        return summary.toString();
    }

    // LSB part of an id is printed together with its MSB part
    private static boolean isPrintedWithPair(Message msg, Map<FieldDescriptor, Object> fields, String name) {
        if (!name.endsWith(LSB)) {
            return false;
        }
        FieldDescriptor msbField = findPairField(msg, name, LSB, MSB);
        return msbField != null && fields.containsKey(msbField);
    }

    private static FieldDescriptor findPairField(Message msg, String name, String suffix, String pairSuffix) {
        return msg.getDescriptorForType().findFieldByName(name.substring(0, name.length() - suffix.length()) + pairSuffix);
    }

    public static String toSummary(Collection<? extends Message> msgs) {
        return msgs.stream().map(EdgeMsgSummaryUtils::toSummary).collect(Collectors.joining(", ", "[", "]"));
    }

}
