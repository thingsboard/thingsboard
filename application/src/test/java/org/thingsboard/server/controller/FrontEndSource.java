// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads front-end source files so that server-side tests can assert against them.
 * <p>
 * That is an unusual thing for a JVM test to do, and it is done because the front-end project configures no
 * test runner at all - no test script, no test build target, no test framework - so a specification file
 * placed next to the code under test would never be executed and would give the appearance of cover without
 * any. Assertions built on this therefore pin what the source says, not what a browser does with it.
 */
final class FrontEndSource {

    /** The front-end service that resolves the development mode and renders the notice. */
    static final String DEVELOPMENT_SERVICE_PATH = "ui-ngx/src/app/core/http/development.service.ts";

    /** The front-end service that writes the CSV, XLS and XLSX files the notice has to reach as well. */
    static final String IMPORT_EXPORT_SERVICE_PATH = "ui-ngx/src/app/shared/import-export/import-export.service.ts";

    /**
     * The server route the front-end asks for the development answer. Declared once because it is the whole
     * of the agreement between the two sides: the server tests call it and the source-text tests assert the
     * front-end calls it, and they only pin each other while both name the same path.
     */
    static final String DEVELOPMENT_PROBE_URL = "/api/noauth/system/development";

    private FrontEndSource() {
    }

    /**
     * Resolves a repository-relative path by walking up from the working directory rather than assuming one,
     * so a test behaves the same whether it runs from its module or from the repository root. A file that
     * cannot be found is a failure and never a skip: a silent pass would defeat the point of pinning anything.
     */
    static String read(String relativePath) {
        Path start = Paths.get("").toAbsolutePath();
        for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
            Path source = candidate.resolve(relativePath);
            if (Files.isRegularFile(source)) {
                try {
                    return Files.readString(source, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        throw new IllegalStateException("Could not find " + relativePath + " above " + start);
    }

    /**
     * {@link #read} followed by {@link #withoutComments}, which is what nearly every assertion here wants: a
     * call site that forgets the stripping still passes most of the time and then one day matches a comment
     * instead of code, so the pairing is offered as one call rather than left to be remembered.
     */
    static String readWithoutComments(String relativePath) {
        return withoutComments(read(relativePath));
    }

    /**
     * The same source with its comments removed, for assertions that reason about WHERE something appears.
     * Prose is not code: a comment mentioning a method by name will satisfy a search for it, and a comment
     * explaining an ordering will sit at an offset that has nothing to do with the ordering it explains.
     * <p>
     * String literals are preserved, so a URL inside one is not mistaken for the start of a comment. The one
     * construct this does not model is a regular-expression literal, which would need to distinguish division
     * from the start of a pattern. The import-export service asserted against carries several, and
     * {@code cellData.replace(/"/g, '""')} does put the scanner out of step: it reads the rest of that
     * statement as two string literals. Literals are appended verbatim, so nothing is dropped, and the scanner
     * is back in step within the same method - long before any body asserted against, all of which come
     * through intact. The case to re-check when a new regex-carrying file is added is a comment swallowed by a
     * mistaken literal, which would survive into the scanned text and could satisfy an assertion.
     */
    static String withoutComments(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (current == '\'' || current == '"' || current == '`') {
                int end = endOfStringLiteral(source, index);
                code.append(source, index, end);
                index = end;
            } else if (current == '/' && next == '/') {
                int end = source.indexOf('\n', index);
                index = end < 0 ? source.length() : end;
            } else if (current == '/' && next == '*') {
                int end = source.indexOf("*/", index + 2);
                index = end < 0 ? source.length() : end + 2;
            } else {
                code.append(current);
                index++;
            }
        }
        return code.toString();
    }

    private static int endOfStringLiteral(String source, int start) {
        char quote = source.charAt(start);
        for (int index = start + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '\\') {
                index++;
            } else if (current == quote) {
                return index + 1;
            }
        }
        return source.length();
    }

    /**
     * The body of a declaration, from its opening brace to the first closing brace at the given indentation.
     * Scoping an assertion to one body is what stops it being satisfied by an unrelated part of the file - a
     * generator left behind unused, or an explanatory comment naming the very thing being forbidden.
     * <p>
     * Both anchors are textual: the declaration is found by its first occurrence, and the body ends at the
     * indentation its closing brace sits at. That couples these assertions to the front-end file's formatting,
     * so a reformat, an extra wrapping block, or a function turned into an arrow constant shows up here rather
     * than on the side that made the change - which is why the failure is raised as an explicit message naming
     * the declaration instead of being allowed to silently return a truncated body.
     *
     * @param closingMarker a newline plus the indentation the declaration's closing brace sits at, which for a
     *                      module-level function is {@code "\n}"} and for a class member {@code "\n  }"}
     */
    static String bodyOf(String source, String declaration, String closingMarker) {
        int start = source.indexOf(declaration);
        if (start < 0) {
            throw new IllegalStateException("No declaration '" + declaration + "' to follow");
        }
        int open = source.indexOf('{', start);
        if (open < 0) {
            throw new IllegalStateException("Declaration '" + declaration + "' has no body");
        }
        int close = source.indexOf(closingMarker, open);
        if (close < 0) {
            throw new IllegalStateException("Body of '" + declaration + "' is not terminated - the closing marker "
                    + "assumes the declaration's original indentation, so a reformat of the front-end file needs "
                    + "the marker updated here");
        }
        return source.substring(open, close);
    }

}
