// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final long TS = 1_700_000_000_000L;

    @Getter
    @RequiredArgsConstructor
    enum PolicyDocument {

        PRIVACY_POLICY(DataConstants.PRIVACY_POLICY_ACCEPTED, DataConstants.PRIVACY_POLICY_ACCEPTED_TS,
                User::setPrivacyPolicyAccepted, User::setPrivacyPolicyAccepted, User::isPrivacyPolicyAccepted),
        TERMS_OF_USE(DataConstants.TERMS_OF_USE_ACCEPTED, DataConstants.TERMS_OF_USE_ACCEPTED_TS,
                User::setTermsOfUseAccepted, User::setTermsOfUseAccepted, User::isTermsOfUseAccepted);

        private final String acceptedKey;
        private final String acceptedTsKey;
        private final Consumer<User> accept;
        private final ObjLongConsumer<User> acceptAt;
        private final Predicate<User> accepted;

        PolicyDocument other() {
            return this == PRIVACY_POLICY ? TERMS_OF_USE : PRIVACY_POLICY;
        }

    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldCreateAdditionalInfo_whenAdditionalInfoIsNull(PolicyDocument document) {
        // GIVEN
        User user = new User();

        // WHEN
        document.getAcceptAt().accept(user, TS);

        // THEN
        assertAcceptedAt(user, document, TS);
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldReplaceAdditionalInfo_whenAdditionalInfoIsNotObject(PolicyDocument document) {
        // GIVEN
        User user = new User();
        user.setAdditionalInfo(TextNode.valueOf("not an object"));

        // WHEN
        document.getAcceptAt().accept(user, TS);

        // THEN
        assertAcceptedAt(user, document, TS);
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldPreserveExistingFields_whenAdditionalInfoIsObject(PolicyDocument document) {
        // GIVEN
        User user = new User();
        ObjectNode additionalInfo = JsonNodeFactory.instance.objectNode();
        additionalInfo.put("lang", "en_US");
        user.setAdditionalInfo(additionalInfo);

        // WHEN
        document.getAcceptAt().accept(user, TS);

        // THEN
        assertAcceptedAt(user, document, TS);
        assertThat(user.getAdditionalInfo().get("lang").asText()).isEqualTo("en_US");
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldOverwriteTimestamp_whenAcceptedAgain(PolicyDocument document) {
        // GIVEN
        User user = new User();
        document.getAcceptAt().accept(user, TS);

        // WHEN
        document.getAcceptAt().accept(user, TS + 1);

        // THEN
        assertAcceptedAt(user, document, TS + 1);
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldNotTouchOtherDocument(PolicyDocument document) {
        // GIVEN
        User user = new User();

        // WHEN
        document.getAcceptAt().accept(user, TS);

        // THEN
        PolicyDocument other = document.other();
        assertThat(user.getAdditionalInfo().has(other.getAcceptedKey())).isFalse();
        assertThat(user.getAdditionalInfo().has(other.getAcceptedTsKey())).isFalse();
        assertThat(other.getAccepted().test(user)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldStampCurrentTime_whenAcceptedWithoutTimestamp(PolicyDocument document) {
        // GIVEN
        User user = new User();
        long before = System.currentTimeMillis();

        // WHEN
        document.getAccept().accept(user);

        // THEN
        long after = System.currentTimeMillis();
        assertThat(document.getAccepted().test(user)).isTrue();
        assertThat(user.getAdditionalInfo().get(document.getAcceptedTsKey()).asLong()).isBetween(before, after);
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldReportAccepted_whenMarked(PolicyDocument document) {
        // GIVEN
        User user = new User();
        document.getAcceptAt().accept(user, TS);

        // WHEN-THEN
        assertThat(document.getAccepted().test(user)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldReportNotAccepted_whenAdditionalInfoIsNull(PolicyDocument document) {
        // GIVEN
        User user = new User();

        // WHEN-THEN
        assertThat(document.getAccepted().test(user)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldReportNotAccepted_whenFlagIsAbsent(PolicyDocument document) {
        // GIVEN
        User user = new User();
        user.setAdditionalInfo(JsonNodeFactory.instance.objectNode());

        // WHEN-THEN
        assertThat(document.getAccepted().test(user)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(PolicyDocument.class)
    void shouldReportNotAccepted_whenFlagIsFalse(PolicyDocument document) {
        // GIVEN
        User user = new User();
        ObjectNode additionalInfo = JsonNodeFactory.instance.objectNode();
        additionalInfo.put(document.getAcceptedKey(), false);
        user.setAdditionalInfo(additionalInfo);

        // WHEN-THEN
        assertThat(document.getAccepted().test(user)).isFalse();
    }

    private static void assertAcceptedAt(User user, PolicyDocument document, long expectedTs) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        assertThat(additionalInfo).isInstanceOf(ObjectNode.class);
        assertThat(additionalInfo.get(document.getAcceptedKey()).asBoolean()).isTrue();
        assertThat(additionalInfo.get(document.getAcceptedTsKey()).asLong()).isEqualTo(expectedTs);
        assertThat(document.getAccepted().test(user)).isTrue();
    }

}
