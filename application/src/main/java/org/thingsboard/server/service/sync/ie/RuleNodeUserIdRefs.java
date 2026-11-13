// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Finds user references in rule node configurations:
 * <ul>
 *     <li>typed refs serialized from {@code UserId}: {@code {"entityType": "USER", "id": "<uuid>"}}
 *     (e.g. {@code TbGenerateReportV2Node});</li>
 *     <li>plain string fields named {@code userId} that hold a uuid
 *     (e.g. {@code reportConfig.userId} of the legacy {@code TbGenerateReportNode}).</li>
 * </ul>
 * Users are environment specific, so the same person has different ids on different installations.
 * Export records the email of every referenced user, and import uses it to find the local user.
 */
public final class RuleNodeUserIdRefs {

    private static final String ENTITY_TYPE = "entityType";
    private static final String ID = "id";
    private static final String USER_ID = "userId";

    private RuleNodeUserIdRefs() {
    }

    /**
     * Calls {@code replacer} for every user reference and writes back the returned uuid if it differs.
     */
    public static void replace(JsonNode node, UnaryOperator<UUID> replacer) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (isTypedUserRef(objectNode)) {
                replaceField(objectNode, ID, replacer);
                return;
            }
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                if (child.isObject() || child.isArray()) {
                    replace(child, replacer);
                } else if (USER_ID.equals(fieldName)) {
                    replaceField(objectNode, fieldName, replacer);
                }
            }
        } else if (node.isArray()) {
            node.elements().forEachRemaining(child -> replace(child, replacer));
        }
    }

    private static boolean isTypedUserRef(ObjectNode node) {
        JsonNode entityType = node.get(ENTITY_TYPE);
        return entityType != null && entityType.isTextual() && EntityType.USER.name().equals(entityType.asText());
    }

    private static void replaceField(ObjectNode node, String fieldName, UnaryOperator<UUID> replacer) {
        UUID userUuid = parseUuid(node.get(fieldName));
        if (userUuid == null) {
            return;
        }
        UUID newUuid = replacer.apply(userUuid);
        if (newUuid != null && !newUuid.equals(userUuid)) {
            node.put(fieldName, newUuid.toString());
        }
    }

    private static UUID parseUuid(JsonNode value) {
        if (value == null || !value.isTextual()) {
            return null;
        }
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
