// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dashboard thumbnail is rasterised out of the page and then stored on the dashboard and shown to other
 * users, which makes it the one artifact on this path that outlives the browser and travels. The capture is
 * rooted at the dashboard element, so the body-level development notice was never inside it and no amount of
 * work on that element would ever put it there - the mark has to be applied by the capture path itself.
 * <p>
 * Three things have to stay true, and each of them fails silently if it stops being true: the mark is applied
 * before the canvas is serialised, it is drawn as text onto the bitmap rather than styled into the page, and
 * it is gated on the same server-resolved answer the on-screen notice uses rather than on a second flag that
 * could drift away from it. A thumbnail with no mark on it looks exactly like a thumbnail that was never
 * meant to have one.
 * <p>
 * Like its sibling {@link DevelopmentNoticeStyleGuardTest}, this asserts against front-end source text - see
 * {@link FrontEndSource} for why a JVM test is the only thing here that runs at all.
 */
public class DevelopmentNoticeCaptureStampTest {

    private static final String CAPTURE_PATH =
            "ui-ngx/src/app/modules/home/components/dashboard-page/dashboard-image-dialog.component.ts";

    private static final String STAMP_METHOD = "stampDevelopmentNotice";

    /** The alpha channel of the two paints the notice is drawn with, captured so it can be read as a number. */
    private static final Pattern FILL_STYLE_ALPHA = Pattern.compile("fillStyle\\s*=\\s*'rgba\\([^)]*,\\s*([\\d.]+)\\s*\\)'");
    private static final Pattern STROKE_STYLE_ALPHA = Pattern.compile("strokeStyle\\s*=\\s*'rgba\\([^)]*,\\s*([\\d.]+)\\s*\\)'");

    /**
     * The stamp returns a cold observable, so calling it is not the same as running it, and the shape of the
     * call is what tells the two apart. Two ways of calling without subscribing have to be excluded: an
     * operator that receives the observable and discards it ({@code tap}), which the operator alternation
     * rules out; and a block-bodied arrow that calls the stamp for its effect and returns something else,
     * which is why the body may only be the call itself - optionally behind an explicit {@code return}, since
     * that form does hand the observable back.
     */
    private static final Pattern SUBSCRIBED_STAMP_CALL = Pattern.compile(
            "(?:switchMap|concatMap|mergeMap|exhaustMap)\\(\\s*\\(?\\s*\\w+\\s*\\)?\\s*=>\\s*"
                    + "(?:\\{\\s*return\\s+)?[\\w$.]*" + STAMP_METHOD + "\\s*\\(");

    /**
     * Measured on the source with comments removed, and anchored on the subscribing call rather than on the
     * method name. Both matter, and the first one caught this test out: a comment in the capture path
     * explaining the stamp mentions it by name, and once that comment existed the assertion was measuring the
     * position of prose. It would then have accepted the stamp being moved after serialisation entirely.
     */
    @Test
    public void theCaptureIsStampedBeforeItIsSerialised() {
        String capture = FrontEndSource.readWithoutComments(CAPTURE_PATH);

        Matcher stamp = SUBSCRIBED_STAMP_CALL.matcher(capture);
        assertThat(stamp.find())
                .as("no subscribing call to %s in the capture path to order against", STAMP_METHOD)
                .isTrue();

        int captured = capture.indexOf("html2canvas(");
        int serialised = capture.indexOf("toDataURL");
        assertThat(captured).as("no capture call found to order against").isNotNegative();
        assertThat(serialised).as("no serialisation call found to order against").isNotNegative();
        assertThat(stamp.start())
                .as("stamping after serialisation would mark nothing: the stored image is made at toDataURL")
                .isGreaterThan(captured)
                .isLessThan(serialised);
    }

    @Test
    public void theStampCallIsActuallySubscribedTo() {
        assertThat(FrontEndSource.readWithoutComments(CAPTURE_PATH))
                .as("the stamp returns a cold observable, so calling it and dropping what it returns - from a "
                        + "tap, or from a block body that returns something else - draws nothing at all")
                .containsPattern(SUBSCRIBED_STAMP_CALL);
    }

    @Test
    public void theStampIsDrawnAsTextOntoTheBitmap() {
        String stamp = FrontEndSource.bodyOf(FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH),
                "function drawNoticeOnCanvas(", "\n}");

        assertThat(stamp)
                .as("text painted onto the bitmap is what survives here; a styled element or a background "
                        + "image would be back inside the cascade the capture path exists to escape")
                .contains("fillText")
                .doesNotContain("background-image");
    }

    /**
     * Drawing the text is not the same as leaving a mark: a transparent paint, or a zeroed
     * {@code globalAlpha}, keeps every call in place and ships an unmarked thumbnail. The server-side sibling
     * {@code NonProductionImageNoticeTest} counts pixels and so cannot be fooled that way; here the paint is
     * all there is to read, so it is what gets pinned.
     */
    @Test
    public void theStampIsPaintedInSomethingVisible() {
        String stamp = FrontEndSource.bodyOf(FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH),
                "function drawNoticeOnCanvas(", "\n}");

        assertAlphaIsVisible(stamp, FILL_STYLE_ALPHA, "fillStyle");
        assertAlphaIsVisible(stamp, STROKE_STYLE_ALPHA, "strokeStyle");
        assertThat(stamp)
                .as("globalAlpha scales every paint below it, so a zero there erases the notice without "
                        + "touching a single colour declaration")
                .doesNotContain("globalAlpha");
    }

    private static void assertAlphaIsVisible(String stamp, Pattern pattern, String property) {
        Matcher paint = pattern.matcher(stamp);
        assertThat(paint.find()).as("no rgba %s to read an alpha off", property).isTrue();
        assertThat(Double.parseDouble(paint.group(1)))
                .as("a fully transparent %s draws the notice and leaves the bitmap unmarked", property)
                .isGreaterThan(0);
    }

    @Test
    public void theStampAndTheNoticeShareOneAnswerFromTheServer() {
        String service = FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH);

        assertThat(service.split(Pattern.quote(FrontEndSource.DEVELOPMENT_PROBE_URL), -1).length - 1)
                .as("the front end must name the path the server-side tests hit, and name it once: a second "
                        + "mention of it is a second probe, and the two sides stop pinning each other")
                .isEqualTo(1);

        // Every HTTP call in the resolver, not a count of http.get: a second probe added as a post would slip
        // past that and give the two renderings of the notice separate answers to disagree over.
        assertThat(resolverBody(service).split(Pattern.quote("this.http."), -1).length - 1)
                .as("the development flag must be asked of the server in exactly one place")
                .isEqualTo(1);

        assertThat(stampBody(service))
                .as("the stamp must hang off the shared answer, not off a flag of its own")
                .contains("resolveDevelopmentMode");
    }

    /**
     * A cached failure would be worse than no cache at all: one unlucky probe would answer the question for
     * the rest of the session, and every artifact captured afterwards would carry that answer. Only a real
     * answer may be remembered.
     * <p>
     * Checked at the resolver, at the stamp and at the capture, because swallowing the error at any of the
     * three has the same effect - the capture proceeds having decided a question nobody answered.
     */
    @Test
    public void aFailedProbeIsNotRemembered() {
        String service = FrontEndSource.readWithoutComments(FrontEndSource.DEVELOPMENT_SERVICE_PATH);

        assertThat(resolverBody(service))
                .as("swallowing the error here would cache the failure and stop anything ever re-probing")
                .doesNotContain("catchError")
                .doesNotContain("shareReplay");
        assertThat(stampBody(service))
                .as("swallowing the error here would let the capture proceed on an answer nobody gave")
                .doesNotContain("catchError");
        assertThat(FrontEndSource.bodyOf(
                FrontEndSource.readWithoutComments(CAPTURE_PATH), "takeScreenShot(", "\n  }"))
                .as("swallowing the error here would do the same, one layer further out")
                .doesNotContain("catchError");
    }

    private static String stampBody(String service) {
        return FrontEndSource.bodyOf(service, "public " + STAMP_METHOD + "(", "\n  }");
    }

    private static String resolverBody(String service) {
        return FrontEndSource.bodyOf(service, "private resolveDevelopmentMode(", "\n  }");
    }

}
