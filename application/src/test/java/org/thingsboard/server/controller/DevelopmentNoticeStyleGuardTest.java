// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The development notice is drawn by a single element that the front end appends to the document body, and
 * custom CSS supplied by an operator reaches that same document. What keeps a one-line rule from hiding it is
 * that every style declaration is written with an importance flag, so no author rule can out-rank one. That
 * list of declarations is the defence, and it is asserted here because it fails silently: the notice keeps
 * rendering with a property missing, and only a stylesheet written against the gap would ever reveal it.
 * <p>
 * The unpredictable identifier is also asserted, but it is a cost, not a barrier - see
 * {@link #theNoticeElementCarriesNoPredictableIdentifier()} for why it cannot be more than that.
 * <p>
 * This lives in the server test sources, and asserts against front-end source text, because the front-end
 * project configures no test runner at all: it declares no test script, no test build target and no test
 * framework, so a specification file placed next to the service would never be executed and would give the
 * appearance of cover without any.
 * <p>
 * Being a source-text assertion, it pins what is written and not what a browser accepts. A declaration an
 * engine silently rejects still reads as present here - {@code offset}, {@code border-image} and
 * {@code -webkit-mask} are the plausible candidates - so this guards the list against edits, not the list
 * against browsers. Confirming the latter needs a browser, and there is none in this repository.
 */
public class DevelopmentNoticeStyleGuardTest {

    private static final String STYLE_LIST_DECLARATION = "const NOTICE_STYLES";

    /** The only call site of the probe that puts the notice on screen. */
    private static final String APP_COMPONENT_PATH = "ui-ngx/src/app/app.component.ts";

    /** The method that asks the server, and renders and re-renders the notice once the answer is yes. */
    private static final String DEVELOPMENT_PROBE = "checkIsDevelopment";

    /** The method that builds the notice element and applies every pinned declaration to it. */
    private static final String NOTICE_BUILDER = "createDevelopmentModeComponent";

    /** The constant the browser-generated exports stamp, and the wrapper that decides whether they stamp it. */
    private static final String EXPORT_NOTICE_CONSTANT = "NON_PRODUCTION_NOTICE";
    private static final String DEVELOPMENT_MODE_WRAPPER = "withDevelopmentMode";

    /** The one method that hands a blob to the browser; calling it is what makes an export a writer of files. */
    private static final String FILE_WRITER = "downloadFile";

    private static final Pattern PUBLIC_EXPORT_METHOD = Pattern.compile("public\\s+(export\\w*)\\s*\\(");

    /** The exports that compose the file themselves, so the notice is theirs to place. */
    private static final List<String> STAMPING_EXPORTS = List.of("exportCsv", "exportXls", "exportXlsx");

    /**
     * The exports that write back a payload the caller supplied - an entity definition, raw text, a zip of
     * either - where the file has no room for a notice that is not corruption of what was asked for. Listed
     * rather than simply left out, so the exact-set check below forces a new writer of files into one list or
     * the other instead of letting it ship unclassified.
     */
    private static final List<String> UNSTAMPED_EXPORTS = List.of("exportJson", "exportText", "exportJSZip");

    /**
     * Property and value both, because the property name alone pins nothing that matters: flipping
     * {@code opacity} to {@code 0} or {@code display} to {@code none} leaves the name in place and the notice
     * invisible. The value is either a quoted literal or - for the background image - a bare identifier.
     */
    private static final Pattern STYLE_DECLARATION =
            Pattern.compile("\\[\\s*'([^']+)'\\s*,\\s*(?:'([^']*)'|([A-Za-z_$][\\w$]*))\\s*\\]");

    /** The fill of the tiled notice text, captured so its alpha can be read as a number. */
    private static final Pattern TILE_FILL_ALPHA = Pattern.compile("fill='rgba\\([^)]*,\\s*([\\d.]+)\\s*\\)'");

    /** Captures the whole third argument of the id assignment, up to the end of the statement. */
    private static final Pattern ID_ASSIGNMENT = Pattern.compile("setAttribute\\([^,]+,\\s*'id'\\s*,\\s*([^;]*?)\\);");

    /** The argument must be a no-argument call, so that there is a named generator to follow and check. */
    private static final Pattern GENERATOR_CALL = Pattern.compile("([A-Za-z_$][\\w$]*)\\(\\)");

    /** Captures the buffer the cryptographic generator fills, so the return value can be tied back to it. */
    private static final Pattern RANDOM_BYTES =
            Pattern.compile("crypto\\.getRandomValues\\(\\s*([A-Za-z_$][\\w$]*)\\s*\\)");

    private static final String RETIRED_ELEMENT_ID = "dev-mode-component";

    /**
     * Split in two only to say which half is load bearing and why.
     * <p>
     * The first half places the notice and is what a reader expects to find. The second half is the reason
     * this test exists: each of those properties either paints over the notice's background image or moves,
     * shrinks or empties the box holding it, all without touching display, visibility, position or z-index.
     * The two halves are spelled out here rather than derived from the front-end source, because a list
     * derived from the thing it checks asserts nothing.
     */
    private static final List<String> PLACEMENT_DECLARATIONS = List.of(
            "position=fixed", "top=0", "bottom=0", "left=0", "right=0", "margin=0",
            "display=block", "visibility=visible", "z-index=100000", "pointer-events=none",
            "background-repeat=repeat", "background-image=NOTICE_BACKGROUND_IMAGE");

    private static final List<String> CONCEALMENT_DECLARATIONS = List.of(
            "content=normal", "box-shadow=none", "border=none", "border-image=none", "border-radius=0", "outline=none",
            "width=100%", "height=100%", "max-width=none", "max-height=none",
            "opacity=1", "filter=none", "mix-blend-mode=normal",
            "clip=auto", "clip-path=none", "-webkit-clip-path=none", "mask=none", "-webkit-mask=none",
            "transform=none", "scale=none", "translate=none", "rotate=none", "zoom=1", "offset=none",
            "background-size=auto", "background-clip=border-box", "background-color=transparent",
            "background-blend-mode=normal",
            "print-color-adjust=exact", "-webkit-print-color-adjust=exact");

    private static final List<String> EXPECTED_DECLARATIONS =
            Stream.concat(PLACEMENT_DECLARATIONS.stream(), CONCEALMENT_DECLARATIONS.stream()).toList();

    /**
     * Deliberately an exact match rather than a containment check, and in both directions. A one-way check
     * would let the source grow a declaration this test says nothing about, which is how the two lists drift
     * apart unnoticed; the failure for an unexpected addition is as useful as the one for a removal, because
     * it forces the new property to be justified here in the same terms as the rest.
     */
    @Test
    public void thePinnedDeclarationsAreExactlyTheOnesThisTestKnowsAbout() {
        assertThat(pinnedStyleDeclarations())
                .as("a property dropped here can be overridden by operator-supplied CSS, and one added without "
                        + "a corresponding entry in this test is a declaration nothing is asserting; the value "
                        + "is pinned with it because 'opacity' set to 0 hides the notice as thoroughly as "
                        + "deleting the line")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_DECLARATIONS);
    }

    /**
     * The one pinned value that is not a literal in the list above: the background image is the notice itself,
     * and a fill it is painted with at zero alpha empties the tile while every other guard here stays green.
     */
    @Test
    public void theNoticeTileIsPaintedInSomethingVisible() {
        Matcher fill = TILE_FILL_ALPHA.matcher(FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH));

        assertThat(fill.find()).as("no rgba fill in the notice tile to read an alpha off").isTrue();
        assertThat(Double.parseDouble(fill.group(1)))
                .as("a fully transparent fill tiles the whole viewport with nothing")
                .isGreaterThan(0);
    }

    /**
     * Read with comments stripped, and scoped to the method that applies the styles, because neither on its own
     * is enough. The explanation above the style list names the flag in prose, so a search of the raw file
     * finds it whether or not any code passes it: delete the argument from the {@code setStyle} call and the
     * pinned declarations all become overridable by a single operator CSS rule while this - the only test
     * asserting the flag - keeps passing. Scoping then makes the match structural rather than incidental: it
     * has to be the last argument of the {@code setStyle} call inside the method that builds the notice, not
     * the token appearing somewhere in a file that also happens to contain that call.
     */
    @Test
    public void theNoticeStylesAreAppliedWithImportance() {
        String body = FrontEndSource.bodyOf(FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH),
                "private " + NOTICE_BUILDER + "(", "\n  }");

        assertThat(body)
                .as("without the importance flag every pinned property loses to an author stylesheet")
                .contains("NOTICE_STYLES.forEach")
                .containsPattern("setStyle\\([^)]*,\\s*RendererStyleFlags2\\.Important\\s*\\)")
                .as("a styled element that is never attached to the document draws nothing")
                .contains("appendChild");
    }

    /**
     * Everything else here asserts how the notice element is BUILT; nothing asserted that it is ever shown.
     * Drop the append, or the single call site of the probe, and every other guard in this class stays green
     * while the notice disappears from the screen - the exact silent failure these source-text assertions
     * exist for, given the front end has no test runner of its own to catch it.
     * <p>
     * The re-scheduling matters as much as the first render: a single-page application replaces the document
     * body as the user navigates, and the periodic re-append is what puts the notice back afterwards.
     */
    @Test
    public void theNoticeIsAttachedAndKeptAttached() {
        String probeBody = FrontEndSource.bodyOf(FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH),
                "public " + DEVELOPMENT_PROBE + "(", "\n  }");

        assertThat(probeBody)
                .as("the probe has to build the notice, and to re-append it after a navigation replaces the body")
                .contains(NOTICE_BUILDER)
                .contains("setInterval");

        assertThat(FrontEndSource.readWithoutComments(APP_COMPONENT_PATH))
                .as("%s is the only place %s is called, so nothing renders the notice without it",
                        APP_COMPONENT_PATH, DEVELOPMENT_PROBE)
                .contains(DEVELOPMENT_PROBE + "(");
    }

    /**
     * The notice stamped into browser-generated CSV, XLS and XLSX exports has to read character for character
     * the same as the one the server stamps into the reports it generates, or the same instance produces two
     * differently worded files. The server side is pinned by its own tests; this ties the front-end literal to
     * the same constant, so a rewording or a changed dash fails here rather than diverging silently.
     */
    @Test
    public void theExportNoticeMatchesTheServerConstant() {
        assertThat(readDevelopmentService())
                .as("the front-end export notice must repeat the server's constant verbatim")
                .contains("'" + DataConstants.NON_PRODUCTION_NOTICE + "'");
    }

    /**
     * The constant matching the server's is only half of it: the browser-generated downloads each place the
     * notice themselves, and each of them can lose it on its own. Dropping the row from one format, or
     * dropping the {@code withDevelopmentMode} wrapper that resolves the mode at all, leaves every other guard
     * in this class green while that format's files ship unmarked.
     * <p>
     * Which exports those are is read out of the service rather than listed here, because a format added later
     * would otherwise be checked by nothing. The two lists it is checked against are the classification, not
     * the discovery: an export that writes a file and appears in neither fails this test.
     */
    @Test
    public void everyBrowserGeneratedExportStampsTheNotice() {
        String exports = FrontEndSource.readWithoutComments(FrontEndSource.IMPORT_EXPORT_SERVICE_PATH);

        assertThat(fileWritingExports(exports))
                .as("an export that writes a file has to be classified here: either it composes the file, and "
                        + "carries the notice, or it writes back a payload of the caller's that a notice would "
                        + "corrupt")
                .containsExactlyInAnyOrderElementsOf(
                        Stream.concat(STAMPING_EXPORTS.stream(), UNSTAMPED_EXPORTS.stream()).toList());

        for (String exportMethod : STAMPING_EXPORTS) {
            assertThat(exportBody(exports, exportMethod))
                    .as("%s writes a file that is saved and mailed on, so it has to carry the notice", exportMethod)
                    .contains(EXPORT_NOTICE_CONSTANT)
                    .as("%s must resolve the mode through the shared wrapper, not decide it for itself", exportMethod)
                    .contains(DEVELOPMENT_MODE_WRAPPER + "(");
        }

        for (String exportMethod : UNSTAMPED_EXPORTS) {
            assertThat(exportBody(exports, exportMethod))
                    .as("%s writes the caller's own payload back out, and a notice inside it is corruption of "
                            + "the artifact rather than a mark on it", exportMethod)
                    .doesNotContain(EXPORT_NOTICE_CONSTANT);
        }
    }

    /** Every public export that hands the browser a file itself, as opposed to delegating to one that does. */
    private static List<String> fileWritingExports(String exports) {
        List<String> writers = new ArrayList<>();
        Matcher matcher = PUBLIC_EXPORT_METHOD.matcher(exports);
        while (matcher.find()) {
            String exportMethod = matcher.group(1);
            if (exportBody(exports, exportMethod).contains("this." + FILE_WRITER + "(")) {
                writers.add(exportMethod);
            }
        }
        return writers;
    }

    private static String exportBody(String exports, String exportMethod) {
        return FrontEndSource.bodyOf(exports, "public " + exportMethod + "(", "\n  }");
    }

    /**
     * The identifier is drawn fresh from the platform's cryptographic generator, so the trivial rule that used
     * to name the notice no longer exists. That is worth keeping, and reinstating a constant identifier - the
     * retired one or any replacement - would quietly hand the handle back.
     * <p>
     * It is not, however, a barrier, and this test should not be read as claiming one: the element's inline
     * style attribute is itself long, unique and stable, so an attribute selector still names it exactly
     * whatever the identifier says. An unpredictable identifier raises the cost of writing the rule; the
     * pinned declarations are what defeat the rule once written.
     * <p>
     * The assertion therefore follows the identifier to its source instead of judging how it is spelled. A
     * check that only rejected a quoted literal would pass a named constant holding one, and a check that only
     * looked for the cryptographic call somewhere in the file would pass a generator left behind unused. So
     * the argument must be a call, and it is the body of the function it names - not the file - that has to
     * draw from the cryptographic generator.
     */
    @Test
    public void theNoticeElementCarriesNoPredictableIdentifier() {
        String source = readDevelopmentService();

        // Raw source, deliberately: an identifier this test forbids is worth failing on even where it appears
        // only in prose, since a comment naming it is how it comes back.
        assertThat(source)
                .as("the retired identifier was nameable by a one-line rule in operator-supplied CSS")
                .doesNotContain(RETIRED_ELEMENT_ID);

        // Everything below reasons about what the code DOES, so it reads the source with comments stripped: a
        // body whose only statement returned a fixed string would otherwise satisfy each check on the strength
        // of a comment mentioning the generator by name.
        String code = FrontEndSource.withoutComments(source);
        String generatorBody = functionBody(code, identifierGeneratorName(code));
        assertThat(generatorBody)
                .as("a sequence any script on the page can observe and continue is not unpredictable")
                .doesNotContain("Math.random");

        Matcher filled = RANDOM_BYTES.matcher(generatorBody);
        assertThat(filled.find())
                .as("the function supplying the identifier must draw it from the platform's cryptographic generator")
                .isTrue();
        // Drawing random bytes and then returning something else would satisfy every check above, so the
        // buffer that was filled has to be the one the return value is built from.
        String buffer = filled.group(1);
        assertThat(generatorBody)
                .as("the identifier must be derived from the random bytes in '%s', not merely alongside them", buffer)
                .containsPattern("return[^;]*\\b" + Pattern.quote(buffer) + "\\b");
    }

    private static String identifierGeneratorName(String source) {
        Matcher assignment = ID_ASSIGNMENT.matcher(source);
        assertThat(assignment.find()).as("no identifier assignment found to check").isTrue();
        String argument = assignment.group(1).trim();
        assertThat(assignment.find()).as("more than one identifier assignment, so one could go unchecked").isFalse();

        Matcher call = GENERATOR_CALL.matcher(argument);
        assertThat(call.matches())
                .as("the identifier must come from a generator call, but was '%s' - a literal or a constant "
                        + "holding one is exactly the fixed identifier this guard exists to prevent", argument)
                .isTrue();
        return call.group(1);
    }

    private static String functionBody(String source, String functionName) {
        // The generator is declared at the top level of the module, so its body is closed by the first brace
        // sitting in the first column.
        return FrontEndSource.bodyOf(source, "function " + functionName + "(", "\n}");
    }

    /**
     * Collected as a list rather than a set, so that the same property declared twice with different values
     * shows up as a mismatch instead of being silently folded into one entry.
     */
    private static List<String> pinnedStyleDeclarations() {
        // Stripped, so that an entry commented out inside the array is not counted as a pinned declaration: the
        // scrape has to see what is applied, not what is written down.
        String source = FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH);
        int start = source.indexOf(STYLE_LIST_DECLARATION);
        assertThat(start).as("style list '%s' not found in %s", STYLE_LIST_DECLARATION, FrontEndSource.DEVELOPMENT_SERVICE_PATH)
                .isNotNegative();
        int end = source.indexOf("];", start);
        assertThat(end).as("style list '%s' is not terminated", STYLE_LIST_DECLARATION).isGreaterThan(start);

        List<String> declarations = new ArrayList<>();
        Matcher matcher = STYLE_DECLARATION.matcher(source.substring(start, end));
        while (matcher.find()) {
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            declarations.add(matcher.group(1) + "=" + value);
        }
        return declarations;
    }

    private static String readDevelopmentService() {
        return FrontEndSource.read(FrontEndSource.DEVELOPMENT_SERVICE_PATH);
    }

}
