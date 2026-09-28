// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Performs the in-memory transformation of an Integration entity JSON tree into the
 * tokenized export form: every annotated field becomes a {@code ${formKey}} placeholder,
 * with a parallel form.json entry that carries widget hints (incl. {@code secretSupport}
 * for fields that should render the install-time secret picker). The placeholder is the
 * same shape for plain and secret-supporting fields — the install dialog substitutes the
 * picker's output (plain text or a {@code ${secret:NAME;type:TYPE}} reference) directly,
 * and PE's {@code SecretConfigurationService} resolves any embedded references at runtime.
 */
@Service
@RequiredArgsConstructor
public class IntegrationPackageExportService {

    private final IntegrationConfigPojoRegistry registry;
    private final PojoFieldWalker walker;
    private final IntegrationJsonCleaner integrationCleaner;
    private final ConverterJsonCleaner converterCleaner;
    private final ObjectMapper mapper;

    /** Result of in-memory tokenization. */
    public record TokenizationResult(JsonNode json, List<Map<String, Object>> formEntries) {}

    /** Public entry: clean + tokenize an Integration entity JSON tree. */
    public TokenizationResult tokenize(IntegrationType type, JsonNode rawIntegrationJson) {
        ObjectNode cleaned = (ObjectNode) integrationCleaner.clean(type, rawIntegrationJson);

        List<IntegrationPojoSpec> specs = new ArrayList<>();
        specs.add(new IntegrationPojoSpec(Integration.class, ""));
        specs.addAll(registry.getSpecs(type));

        List<Map<String, Object>> formEntries = new ArrayList<>();

        for (IntegrationPojoSpec spec : specs) {
            JsonNode walkRoot = spec.jsonPathPrefix().isEmpty()
                ? cleaned
                : readPath(cleaned, spec.jsonPathPrefix());
            if (walkRoot == null || walkRoot.isMissingNode() || walkRoot.isNull()) {
                continue;
            }
            List<PojoFieldWalker.WalkedField> walked =
                walker.walk(spec.rootClass(), walkRoot, spec.jsonPathPrefix());

            for (PojoFieldWalker.WalkedField wf : walked) {
                applyOne(cleaned, wf, formEntries);
            }
        }
        return new TokenizationResult(cleaned, formEntries);
    }

    /** Cleans a Converter entity JSON tree (no annotations applied to converters in v1). */
    public JsonNode cleanConverter(JsonNode rawConverterJson) {
        return converterCleaner.clean(rawConverterJson);
    }

    private void applyOne(ObjectNode integrationJson, PojoFieldWalker.WalkedField wf,
                          List<Map<String, Object>> formEntries) {
        TemplateField tf = wf.annotation();
        JsonNode original = readPath(integrationJson, wf.jsonPath());
        if (original == null || original.isMissingNode() || original.isNull()) {
            return;
        }

        // Always emit ${formKey} — the install-time form value (plaintext or a ${secret:...} reference
        // produced by the secret picker) is substituted as-is. Secret-vs-plaintext is a UI/runtime
        // concern, not an export-time one.
        String formKey = tf.key();
        String placeholderValue = "${" + formKey + "}";
        String defaultValue = tf.secret()
            ? ""
            : (original.isValueNode() ? original.asText() : original.toString());

        writePath(integrationJson, wf.jsonPath(), TextNode.valueOf(placeholderValue));
        formEntries.add(buildFormEntry(formKey, tf, tf.secret(), defaultValue));
    }

    private Map<String, Object> buildFormEntry(String key, TemplateField tf,
                                                boolean secretSupport, String defaultValue) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        if (!tf.label().isEmpty())     entry.put("label", tf.label());
        entry.put("type", tf.type().name());
        if (defaultValue != null)      entry.put("defaultValue", defaultValue);
        if (secretSupport) {
            entry.put("secretSupport", true);
            entry.put("secretType", tf.secretType().name());
        }
        if (!tf.helpText().isEmpty())  entry.put("helpText", tf.helpText());
        if (!tf.group().isEmpty())     entry.put("group", tf.group());
        if (tf.options().length > 0)   entry.put("options", buildOptions(tf.options()));
        if (tf.required())             entry.put("required", true);
        return entry;
    }

    private static List<Map<String, String>> buildOptions(String[] options) {
        List<Map<String, String>> out = new ArrayList<>(options.length);
        for (String o : options) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("value", o);
            entry.put("label", o);
            out.add(entry);
        }
        return out;
    }

    private JsonNode readPath(JsonNode root, String dottedPath) {
        if (dottedPath.isEmpty()) return root;
        JsonNode cur = root;
        for (String seg : dottedPath.split("\\.")) {
            if (cur == null || cur.isMissingNode() || cur.isNull()) return null;
            cur = cur.get(seg);
        }
        return cur;
    }

    private void writePath(ObjectNode root, String dottedPath, JsonNode value) {
        String[] segs = dottedPath.split("\\.");
        ObjectNode cur = root;
        for (int i = 0; i < segs.length - 1; i++) {
            JsonNode child = cur.get(segs[i]);
            if (child instanceof ObjectNode obj) {
                cur = obj;
            } else {
                cur = cur.putObject(segs[i]);
            }
        }
        cur.set(segs[segs.length - 1], value);
    }

    /** Assemble a ZIP from a tokenized integration JSON, optional uplink/downlink converter
     *  JSONs, and the accumulated form entries. Caller passes already-cleaned converters. */
    public byte[] zip(JsonNode integrationJson,
                      JsonNode uplinkConverterJson,
                      JsonNode downlinkConverterJson,
                      List<Map<String, Object>> formEntries) throws IOException {
        com.fasterxml.jackson.databind.ObjectWriter pretty = mapper.writerWithDefaultPrettyPrinter();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(baos)) {

            zip.putNextEntry(new ZipEntry("integration.json"));
            zip.write(pretty.writeValueAsBytes(integrationJson));
            zip.closeEntry();

            if (uplinkConverterJson != null) {
                zip.putNextEntry(new ZipEntry("uplink.json"));
                zip.write(pretty.writeValueAsBytes(uplinkConverterJson));
                zip.closeEntry();
            }
            if (downlinkConverterJson != null) {
                zip.putNextEntry(new ZipEntry("downlink.json"));
                zip.write(pretty.writeValueAsBytes(downlinkConverterJson));
                zip.closeEntry();
            }

            zip.putNextEntry(new ZipEntry("form.json"));
            zip.write(pretty.writeValueAsBytes(formEntries));
            zip.closeEntry();

            zip.finish();
            return baos.toByteArray();
        }
    }
}
