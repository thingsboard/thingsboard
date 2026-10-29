// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Describes edge protobuf messages without their payloads, so they can be logged or put into notifications
 * without exposing entity JSON, credentials, tokens or secrets.
 */
public final class EdgeMsgSummaryUtils {

    private EdgeMsgSummaryUtils() {
    }

    // e.g. DownlinkMsg[downlinkMsgId=12, deviceUpdateMsg x2, deviceCredentialsUpdateMsg x1]
    public static String toSummary(Message msg) {
        if (msg == null) {
            return "null";
        }
        StringJoiner summary = new StringJoiner(", ", msg.getDescriptorForType().getName() + "[", "]");
        msg.getAllFields().forEach((field, value) -> {
            if (field.isRepeated()) {
                summary.add(field.getName() + " x" + ((List<?>) value).size());
            } else if (field.getJavaType() == FieldDescriptor.JavaType.INT || field.getJavaType() == FieldDescriptor.JavaType.LONG) {
                summary.add(field.getName() + "=" + value);
            } else if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
                summary.add(field.getName());
            }
        });
        return summary.toString();
    }

    public static String toSummary(Collection<? extends Message> msgs) {
        return msgs.stream().map(EdgeMsgSummaryUtils::toSummary).collect(Collectors.joining(", ", "[", "]"));
    }

}
