// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Inject, Injectable, Renderer2, RendererFactory2, RendererStyleFlags2, DOCUMENT } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { defaultHttpOptions } from '@core/http/http-utils';
import { stringToBase64 } from '@core/utils';


// Scraped as source text by DevelopmentNoticeStyleGuardTest, which matches these declaration names and the closing-brace indentation of the bodies below - reformat with care.

/** Random per rebuild, so operator CSS has no fixed id to hide the notice by. Not `guid()` - Math.random() is predictable. */
function noticeElementId(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

const DEVELOPMENT_PROBE_URL = '/api/noauth/system/development';

/** Shared by the on-screen tile and the canvas stamp, so the two can never disagree. */
const NOTICE_TEXT = 'Development mode';

/** For exported FILES. Must stay character-identical to the server's DataConstants.NON_PRODUCTION_NOTICE. */
export const NON_PRODUCTION_NOTICE = 'Development Mode — non-production use only';

const NOTICE_TILE_SIZE = 140;

/** Fixed advance width, so the string tiles the same whatever font the platform gives an isolated SVG. */
const NOTICE_TEXT_LENGTH = 150;

/** The tint is deliberately faint - noticeable, but not in the way of people who legitimately develop here. */
const NOTICE_TILE_SVG =
  `<svg xmlns='http://www.w3.org/2000/svg' width='${NOTICE_TILE_SIZE}' height='${NOTICE_TILE_SIZE}' ` +
  `viewBox='0 0 ${NOTICE_TILE_SIZE} ${NOTICE_TILE_SIZE}'>` +
  `<text transform='translate(20, 130) rotate(-45)' fill='rgba(200,200,200,0.35)' ` +
  `font-family='serif' font-size='20' ` +
  `textLength='${NOTICE_TEXT_LENGTH}' lengthAdjust='spacingAndGlyphs'>` +
  `${NOTICE_TEXT}</text></svg>`;

const NOTICE_BACKGROUND_IMAGE = `url("data:image/svg+xml;base64,${stringToBase64(NOTICE_TILE_SVG)}")`;

/**
 * Marks a canvas rasterised out of the page - a capture rooted below the body never contained the notice element.
 *
 * Drawn as text into the pixels, so no stylesheet, print setting or `data-html2canvas-ignore` can drop it.
 * Heavier than the on-screen tile because it has to stay readable in a thumbnail, over either theme.
 *
 * Throws rather than returning an unmarked canvas: the caller would store it as a clean thumbnail of a
 * development deployment, which is the leak this exists to close.
 */
function drawNoticeOnCanvas(canvas: HTMLCanvasElement): void {
  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('Development notice could not be drawn: the capture canvas has no 2d drawing context');
  }
  const fontSize = Math.max(11, Math.round(canvas.width / 32));
  context.save();
  context.font = `${fontSize}px sans-serif`;
  context.textBaseline = 'middle';
  context.lineJoin = 'round';
  context.lineWidth = Math.max(2, Math.round(fontSize / 4));
  context.strokeStyle = 'rgba(255, 255, 255, 0.75)';
  context.fillStyle = 'rgba(0, 0, 0, 0.55)';
  const stepX = context.measureText(NOTICE_TEXT).width + fontSize * 3;
  const stepY = fontSize * 5;
  // After rotating about the origin the canvas lands somewhere inside a square of this half-extent, so tiling
  // the whole range covers every pixel without working out where the corners went.
  const reach = canvas.width + canvas.height;
  context.rotate(-Math.PI / 4);
  for (let y = -reach; y < reach; y += stepY) {
    for (let x = -reach; x < reach; x += stepX) {
      context.strokeText(NOTICE_TEXT, x, y);
      context.fillText(NOTICE_TEXT, x, y);
    }
  }
  context.restore();
}

/**
 * All applied with RendererStyleFlags2.Important, so operator-supplied custom CSS cannot out-rank them.
 *
 * The list is not just the properties that place the notice: it pins every property that could stop a
 * background image being seen - emptying the paint, painting over it, dropping it from another pipeline, or
 * moving the box. Do not trim it without checking which of those four a property belongs to.
 *
 * `print-color-adjust: exact` is the least obvious and the most important: it starts at `economy`, so Ctrl+P
 * would otherwise produce a clean PDF with no attacker involved.
 *
 * Out of reach here, by design: ::before/::after on this element, anything done to body/html (a `filter` or
 * `contain` there makes body the containing block), rendering contexts this element is not part of (see
 * drawNoticeOnCanvas), and pages two onwards of a multi-page print.
 */
const NOTICE_STYLES: ReadonlyArray<[string, string]> = [
  ['position', 'fixed'],
  ['top', '0'],
  ['bottom', '0'],
  ['left', '0'],
  ['right', '0'],
  ['margin', '0'],
  ['display', 'block'],
  ['visibility', 'visible'],
  ['z-index', '100000'],
  ['pointer-events', 'none'],
  ['content', 'normal'],
  ['box-shadow', 'none'],
  ['border', 'none'],
  ['border-image', 'none'],
  ['border-radius', '0'],
  ['outline', 'none'],
  ['width', '100%'],
  ['height', '100%'],
  ['max-width', 'none'],
  ['max-height', 'none'],
  ['opacity', '1'],
  ['filter', 'none'],
  ['mix-blend-mode', 'normal'],
  ['clip', 'auto'],
  ['clip-path', 'none'],
  ['-webkit-clip-path', 'none'],
  ['mask', 'none'],
  ['-webkit-mask', 'none'],
  ['transform', 'none'],
  ['scale', 'none'],
  ['translate', 'none'],
  ['rotate', 'none'],
  ['zoom', '1'],
  ['offset', 'none'],
  ['background-repeat', 'repeat'],
  ['background-size', 'auto'],
  ['background-clip', 'border-box'],
  ['background-color', 'transparent'],
  ['background-blend-mode', 'normal'],
  ['print-color-adjust', 'exact'],
  ['-webkit-print-color-adjust', 'exact'],
  ['background-image', NOTICE_BACKGROUND_IMAGE]
];

@Injectable({
  providedIn: 'root'
})
export class DevelopmentService {

  private renderer: Renderer2;
  private readonly ROOT: HTMLElement;
  private noticeElement: HTMLElement;

  /** The server's answer, once it has given one. Undefined means it has not been obtained yet. */
  private developmentMode: boolean;

  constructor(private http: HttpClient,
              private rendererFactory: RendererFactory2,
              @Inject(DOCUMENT) private document: Document) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.ROOT = this.document.body;
  }

  /**
   * The one answer every rendering acts on, so two renderings can never disagree.
   *
   * Answers are cached; FAILURES never are, so one bad moment cannot decide the question for the whole session.
   * A non-boolean response counts as a failure, not as "no" - an empty body from a proxy must not become `false`.
   * Nothing here converts a failure into an answer; each caller decides what to do when the mode is unknown.
   */
  private resolveDevelopmentMode(): Observable<boolean> {
    if (this.developmentMode !== undefined) {
      return of(this.developmentMode);
    }
    return this.http.get<boolean>(DEVELOPMENT_PROBE_URL, defaultHttpOptions(true)).pipe(
      map((developmentMode) => {
        if (typeof developmentMode !== 'boolean') {
          throw new Error('Development mode check returned no answer');
        }
        return developmentMode;
      }),
      tap((developmentMode) => this.developmentMode = developmentMode)
    );
  }

  /**
   * Renders the on-screen notice only if the server says this is a development deployment.
   *
   * The only place a failure may be swallowed: nothing is stored, the viewer is present, and a reload re-probes.
   * Anything that produces a persisted artifact must decline instead - see {@link stampDevelopmentNotice}.
   */
  public checkIsDevelopment(): void {
    this.resolveDevelopmentMode().subscribe({
      next: (developmentMode) => {
        if (developmentMode) {
          this.createDevelopmentModeComponent();
          setInterval(() => this.createDevelopmentModeComponent(), 10000);
        }
      },
      error: () => {}
    });
  }

  /**
   * Marks a canvas before whatever serialises it. Gated on the same answer the on-screen notice uses.
   *
   * If the mode cannot be established, or the notice cannot be drawn, THIS FAILS AND NOTHING IS CAPTURED.
   * Marking anyway would assert a false thing about a production deployment; not marking would leak a clean
   * thumbnail of a development one. Declining asserts neither, and an action - unlike a notice - can decline.
   */
  public stampDevelopmentNotice(canvas: HTMLCanvasElement): Observable<HTMLCanvasElement> {
    return this.resolveDevelopmentMode().pipe(
      map((developmentMode) => {
        if (developmentMode) {
          drawNoticeOnCanvas(canvas);
        }
        return canvas;
      })
    );
  }

  /**
   * For callers that must place the notice themselves, because their format is one this service knows nothing
   * about. What they must NOT duplicate is the decision of whether to stamp, so it is still taken here.
   *
   * Errors propagate and are never turned into `false`; a caller that writes its artifact anyway is making the
   * very default this service refuses to make. Cached answers emit synchronously, so repeat exports do not wait.
   */
  public isDevelopmentMode(): Observable<boolean> {
    return this.resolveDevelopmentMode();
  }

  /** Tracked by reference, not by id, so the id stays unpredictable. Do not reinstate a fixed one to find it by. */
  private createDevelopmentModeComponent(): void {
    this.noticeElement?.remove();
    const noticeElement: HTMLElement = this.renderer.createElement('div');
    this.renderer.setAttribute(noticeElement, 'id', noticeElementId());
    NOTICE_STYLES.forEach(([property, value]) =>
      this.renderer.setStyle(noticeElement, property, value, RendererStyleFlags2.Important));
    this.renderer.appendChild(this.ROOT, noticeElement);
    this.noticeElement = noticeElement;
  }

}
