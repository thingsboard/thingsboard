// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PojoFieldWalkerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PojoFieldWalker walker = new PojoFieldWalker();

    static class FlatFixture {
        @TemplateField(key = "host")
        String host;

        @TemplateField(key = "port", type = FormFieldType.INTEGER)
        int port;

        String unannotated;
    }

    @Test
    void walks_flat_class_emits_paths_relative_to_root() throws Exception {
        JsonNode persisted = mapper.readTree("""
            { "host": "broker.example", "port": 1883, "unannotated": "x" }
            """);

        List<PojoFieldWalker.WalkedField> fields = walker.walk(FlatFixture.class, persisted, "");

        assertThat(fields).hasSize(2);
        PojoFieldWalker.WalkedField host = fields.stream()
                .filter(f -> f.annotation().key().equals("host")).findFirst().orElseThrow();
        assertThat(host.jsonPath()).isEqualTo("host");

        PojoFieldWalker.WalkedField port = fields.stream()
                .filter(f -> f.annotation().key().equals("port")).findFirst().orElseThrow();
        assertThat(port.jsonPath()).isEqualTo("port");
    }

    static class WithPathOverride {
        @TemplateField(key = "tokenKey", path = "credentials.token")
        String tokenWhereverInJava;
    }

    @Test
    void honors_explicit_path_override_against_field_position() throws Exception {
        JsonNode persisted = mapper.readTree("""
            { "credentials": { "token": "abc" } }
            """);

        List<PojoFieldWalker.WalkedField> fields =
            walker.walk(WithPathOverride.class, persisted, "");

        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).jsonPath()).isEqualTo("credentials.token");
    }

    @com.fasterxml.jackson.annotation.JsonTypeInfo(
        use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME,
        property = "type")
    @com.fasterxml.jackson.annotation.JsonSubTypes({
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = BasicCreds.class, name = "basic"),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = TokenCreds.class, name = "token")
    })
    interface PolymorphicCreds {}

    static class BasicCreds implements PolymorphicCreds {
        @TemplateField(key = "user") String username;
        @TemplateField(key = "pass") String password;
    }

    static class TokenCreds implements PolymorphicCreds {
        @TemplateField(key = "tok") String token;
    }

    static class WithPolymorphic {
        @TemplateField(key = "host") String host;
        PolymorphicCreds credentials;          // intentionally not annotated
    }

    @Test
    void descends_into_polymorphic_field_using_persisted_discriminator_TOKEN() throws Exception {
        JsonNode persisted = mapper.readTree("""
            {
              "host": "broker",
              "credentials": { "type": "token", "token": "abc" }
            }
            """);

        List<PojoFieldWalker.WalkedField> fields =
            walker.walk(WithPolymorphic.class, persisted, "");

        assertThat(fields).extracting(f -> f.annotation().key())
            .containsExactlyInAnyOrder("host", "tok");

        PojoFieldWalker.WalkedField tok = fields.stream()
            .filter(f -> f.annotation().key().equals("tok")).findFirst().orElseThrow();
        assertThat(tok.jsonPath()).isEqualTo("credentials.token");
    }

    @Test
    void descends_into_polymorphic_field_using_persisted_discriminator_BASIC() throws Exception {
        JsonNode persisted = mapper.readTree("""
            {
              "host": "broker",
              "credentials": { "type": "basic", "username": "u", "password": "p" }
            }
            """);

        List<PojoFieldWalker.WalkedField> fields =
            walker.walk(WithPolymorphic.class, persisted, "");

        assertThat(fields).extracting(f -> f.annotation().key())
            .containsExactlyInAnyOrder("host", "user", "pass");

        assertThat(fields.stream().filter(f -> f.annotation().key().equals("user"))
            .findFirst().orElseThrow().jsonPath()).isEqualTo("credentials.username");
    }

    static class PlainCreds {
        @TemplateField(key = "particleToken") String token;
    }

    static class WithPlainComposition {
        @TemplateField(key = "ph") String ph;
        PlainCreds credentials;
    }

    @Test
    void descends_into_plain_composed_field_when_inner_type_has_template_fields() throws Exception {
        JsonNode persisted = mapper.readTree("""
            { "ph": "x", "credentials": { "token": "abc" } }
            """);

        List<PojoFieldWalker.WalkedField> fields =
            walker.walk(WithPlainComposition.class, persisted, "");

        assertThat(fields).extracting(f -> f.annotation().key())
            .containsExactlyInAnyOrder("ph", "particleToken");
        assertThat(fields.stream().filter(f -> f.annotation().key().equals("particleToken"))
            .findFirst().orElseThrow().jsonPath()).isEqualTo("credentials.token");
    }
}
