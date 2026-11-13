// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { Directive, ElementRef, EventEmitter, Input, NgZone, OnDestroy, OnInit, Output } from '@angular/core';
import PhotoSwipeLightbox from 'photoswipe/lightbox';
import PhotoSwipe from 'photoswipe';
import cssjs from '@core/css/css';

const PHOTO_GALLERY_STYLE_ID = 'photoswipe-gallery-style';
// Share of the available area an opened image takes, leaving backdrop to click and room for close
const SHRINK_FILL = 0.86;
const GROW_FILL = 0.95;
/** Upper bound for enlarging a small image, relative to its own size. */
const MAX_UPSCALE = 2;
/** How far a click-to-zoom steps past the level the image opened at. */
const ZOOM_STEP = 2;
// Symmetric, so the image stays centred; the caption floats over the bottom padding
const VIEWPORT_PADDING = { top: 64, bottom: 64, left: 24, right: 24 };
const PHOTO_GALLERY_CLASS = 'tb-photoswipe-gallery';
const PHOTO_GALLERY_STYLE =
  // PhotoSwipe's own transform/will-change create a layer backdrop-filter cannot see through
  '{\n' +
  '    transform: none;\n' +
  '    will-change: auto;\n' +
  '}\n' +
  '\n' +
  // On .pswp__bg, the layer PhotoSwipe fades in with the zoom
  '.pswp__bg {\n' +
  '    background: rgba(10, 10, 20, 0.55);\n' +
  '    backdrop-filter: blur(18px);\n' +
  '    -webkit-backdrop-filter: blur(18px);\n' +
  '    transform: none;\n' +
  '    will-change: backdrop-filter;\n' +
  '}\n' +
  '\n' +
  '.pswp__tb-photoswipe-caption {\n' +
  '    position: fixed;\n' +
  '    bottom: 1.5rem;\n' +
  '    left: 50%;\n' +
  '    transform: translate(-50%);\n' +
  '    z-index: 10000;\n' +
  '    display: flex;\n' +
  '    flex-direction: column;\n' +
  '    align-items: center;\n' +
  '    gap: .25rem;\n' +
  '    max-width: 80vw;\n' +
  '    text-align: center;\n' +
  '    pointer-events: none;\n' +
  '    line-height: 1.75;\n' +
  '}\n' +
  '\n' +
  '.pswp__tb-photoswipe-caption .tb-gallery-caption {\n' +
  '    color: #fff;\n' +
  '    font-size: 1.125rem;\n' +
  '    line-height: 1.5;\n' +
  '    background: #000000a6;\n' +
  '    padding: .5rem 1.25rem;\n' +
  '    border-radius: 8px;\n' +
  '    backdrop-filter: blur(8px);\n' +
  '    -webkit-backdrop-filter: blur(8px);\n' +
  '}\n' +
  '\n' +
  '.pswp__tb-photoswipe-caption .tb-gallery-counter {\n' +
  '    color: #fff9;\n' +
  '    font-size: .8rem;\n' +
  '}\n'+
  '\n' +
  '.pswp__item img.pswp__img {\n' +
  '    display: block;\n' +
  '    object-fit: contain;\n' +
  '    border-radius: 4px;\n' +
  '}\n' +
  '\n' +
  // drop-shadow follows transparency; on the wrap so it survives the placeholder swap
  '.pswp__zoom-wrap {\n' +
  '    filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.5));\n' +
  '}\n' +
  '\n' +
  // PhotoSwipe's #222 placeholder background flashes against the translucent backdrop
  '.pswp__item .pswp__img--placeholder {\n' +
  '    background: transparent;\n' +
  '    border-radius: 4px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button {\n' +
  '    border-radius: 50%;\n' +
  '    border: 1px solid rgba(255, 255, 255, .2);\n' +
  '    background: #1e1e2899;\n' +
  '    backdrop-filter: blur(8px);\n' +
  '    -webkit-backdrop-filter: blur(8px);\n' +
  '    color: #fff;\n' +
  '    transition: background .18s ease, transform .18s ease;\n' +
  '    outline: none;\n' +
  '}\n' +
  '\n' +
  '.pswp__button:hover {\n' +
  '    background: #3c3c50d9;\n' +
  '    transform: scale(1.08);\n' +
  '    outline: none;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--close, .pswp__button.pswp__button--zoom {\n' +
  '    width: 40px;\n' +
  '    height: 40px;\n' +
  '    margin-top: 16px;\n' +
  '    margin-right: 16px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--close .pswp__icn, .pswp__button.pswp__button--zoom .pswp__icn {\n' +
  '    width: 24px;\n' +
  '    height: 24px;\n' +
  '    top: 7px;\n' +
  '    left: 7px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow {\n' +
  '    width: 48px;\n' +
  '    height: 48px;\n' +
  '    margin-top: -24px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow .pswp__icn {\n' +
  '    width: 32px;\n' +
  '    height: 32px;\n' +
  '    margin-top: 0;\n' +
  '    top: 7px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow.pswp__button--arrow--prev {\n' +
  '    left: 16px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow.pswp__button--arrow--prev .pswp__icn {\n' +
  '    left: 12px;\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow.pswp__button--arrow--next {\n' +
  '    right: 16px\n' +
  '}\n' +
  '\n' +
  '.pswp__button.pswp__button--arrow.pswp__button--arrow--next .pswp__icn {  \n' +
  '    right: 12px;\n' +
  '}';

/** The parts of PhotoSwipe's ZoomLevel this directive needs; `panAreaSize` already excludes padding. */
interface ZoomLevelSizes {
  fit: number;
  panAreaSize: { x: number; y: number } | null;
  elementSize: { x: number; y: number } | null;
}

/** PhotoSwipe's `fit` is capped at 1; use the raw ratio so a small image grows, up to MAX_UPSCALE. */
function initialZoom(zoomLevel: ZoomLevelSizes): number {
  const { panAreaSize, elementSize } = zoomLevel;
  if (!panAreaSize || !elementSize?.x || !elementSize?.y) {
    return zoomLevel.fit;
  }
  const fitRatio = Math.min(panAreaSize.x / elementSize.x, panAreaSize.y / elementSize.y);
  if (fitRatio <= 1) {
    return fitRatio * SHRINK_FILL;
  }
  return Math.min(fitRatio * GROW_FILL, MAX_UPSCALE);
}

/** Click-to-zoom level; always above the opening level, so the click visibly zooms. */
function secondaryZoom(zoomLevel: ZoomLevelSizes): number {
  return Math.max(initialZoom(zoomLevel) * ZOOM_STEP, Math.min(1, zoomLevel.fit * 3));
}

// A gallery child may be a container with no image in it
function thumbnailImage(element: Element): HTMLImageElement | null {
  return element instanceof HTMLImageElement ? element : element.querySelector('img');
}

/** Bounds of the pixels an `object-fit: contain` image paints, centred inside its box. */
function containedImageBounds(image: HTMLImageElement): { x: number; y: number; w: number } {
  const rect = image.getBoundingClientRect();
  const scale = Math.min(rect.width / image.naturalWidth, rect.height / image.naturalHeight);
  const width = image.naturalWidth * scale;
  const height = image.naturalHeight * scale;
  return {
    x: rect.left + (rect.width - width) / 2,
    y: rect.top + (rect.height - height) / 2,
    w: width
  };
}

@Directive({
  selector: '[tbPhotoSwipeGallery]',
  standalone: false
})
export class PhotoSwipeGalleryDirective implements OnInit, OnDestroy {

  @Input() galleryChildrenSelector = '.tb-image';
  @Input() imageCaptionSelector = '.tb-image-tooltip';

  @Output() readonly lightboxOpened = new EventEmitter<void>();
  @Output() readonly lightboxClosed = new EventEmitter<void>();
  /** The slide the lightbox moved to, emitted while it is still open. */
  @Output() readonly slideChanged = new EventEmitter<number>();

  private lightbox: PhotoSwipeLightbox;
  private closeOnOpened = false;

  constructor(
    private elementRef: ElementRef<HTMLElement>,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.initPhotoSwipeGalleryStyle();
    this.lightbox = new PhotoSwipeLightbox({
      gallery: this.elementRef.nativeElement,
      children: this.galleryChildrenSelector,
      pswpModule: PhotoSwipe,
      counter: false,
      // No toolbar magnifier: zoom stays on scroll and click
      zoom: false,
      // The backdrop look lives in PHOTO_GALLERY_STYLE; PhotoSwipe fades it in up to this opacity
      bgOpacity: 1,
      mainClass: PHOTO_GALLERY_CLASS,
      padding: VIEWPORT_PADDING,
      // The scroll lock only preventDefaults wheel, so PhotoSwipe still receives it
      wheelToZoom: true,
      initialZoomLevel: zoomLevel => initialZoom(zoomLevel),
      secondaryZoomLevel: zoomLevel => secondaryZoom(zoomLevel)
    });
    this.lightbox.addFilter('domItemData', (itemData, element) => {
      const image = thumbnailImage(element);
      if (!image) {
        return itemData;
      }
      itemData.src = image.src;
      itemData.width = image.naturalWidth;
      itemData.height = image.naturalHeight;
      // Only a cover-fitted thumbnail is cropped; otherwise the zoom starts from a clipped frame
      itemData.thumbCropped = getComputedStyle(image).objectFit === 'cover';
      return itemData;
    });
    // PhotoSwipe measures the thumbnail's box, but `contain` paints a smaller image inside it,
    // so the zoom would start off the visible pixels
    this.lightbox.addFilter('thumbBounds', (thumbBounds, itemData) => {
      const image = itemData.element ? thumbnailImage(itemData.element) : null;
      if (!image?.naturalWidth || getComputedStyle(image).objectFit !== 'contain') {
        return thumbBounds;
      }
      return containedImageBounds(image);
    });
    this.lightbox.on('uiRegister', () => {
      this.lightbox.pswp.ui.registerElement({
        name: 'tb-photoswipe-caption',
        order: 9,
        isButton: false,
        appendTo: 'root',
        html: '<span class="tb-gallery-caption"></span><span class="tb-gallery-counter"></span>',
        onInit: (el, pswp) => {
          const caption = el.querySelector<HTMLElement>('.tb-gallery-caption');
          const counter = el.querySelector<HTMLElement>('.tb-gallery-counter');
          this.lightbox.pswp.on('change', () => {
            counter.innerText = pswp.currIndex + 1 + pswp.options.indexIndicatorSep + pswp.getNumItems();
            const currSlideElement = this.lightbox.pswp.currSlide.data.element;
            let imageTooltip: Element;
            if (currSlideElement) {
              imageTooltip = currSlideElement.querySelector(this.imageCaptionSelector);
            }
            if (imageTooltip) {
              caption.style.display = 'block';
              caption.innerHTML = imageTooltip.innerHTML || '';
            } else {
              caption.style.display = 'none';
            }
          });
        }
      });
    });
    // Emitted while still open: PhotoSwipe reads the thumbnail position when closing starts, so a
    // host carousel must already show the matching slide by then
    this.lightbox.on('change', () => {
      this.ngZone.run(() => this.slideChanged.emit(this.lightbox.pswp.currIndex));
    });
    this.lightbox.on('beforeOpen', () => {
      this.attachDocumentListeners();
      this.ngZone.run(() => this.lightboxOpened.emit());
    });
    this.lightbox.on('destroy', () => {
      this.detachDocumentListeners();
      this.ngZone.run(() => this.lightboxClosed.emit());
    });
    this.lightbox.init();
  }

  ngOnDestroy(): void {
    this.detachDocumentListeners();
    if (this.lightbox) {
      this.lightbox.destroy();
    }
  }

  /**
   * Escape closes the image only, not a mat-dialog behind it. Runs in the capture phase, ahead of
   * the CDK overlay, and closes PhotoSwipe itself: stopping propagation also keeps the key from
   * PhotoSwipe's own bubble-phase handler on document.
   */
  private readonly onKeydownCapture = (e: KeyboardEvent): void => {
    const pswp = this.lightbox?.pswp;
    if (e.key !== 'Escape' || !pswp) {
      return;
    }
    e.stopPropagation();
    e.preventDefault();
    if (pswp.opener?.isOpen) {
      pswp.close();
      return;
    }
    // During the opening zoom close() is a no-op, so close once it ends (only once)
    if (this.closeOnOpened) {
      return;
    }
    this.closeOnOpened = true;
    pswp.on('openingAnimationEnd', () => pswp.close());
  };

  private readonly onScrollEvent = (e: Event): void => {
    // Touch gestures inside the lightbox belong to PhotoSwipe (pan, pinch, swipe)
    if (e.type === 'touchmove' && this.lightbox?.pswp?.element?.contains(e.target as Node)) {
      return;
    }
    e.preventDefault();
  };

  /**
   * Escape interception plus the page scroll lock. PhotoSwipe does not lock page scroll, and the
   * scroller may be the page or a dialog pane, so the wheel and touch events are blocked instead.
   * Outside the zone, since touchmove fires at ~60 Hz during a pinch or pan.
   */
  private attachDocumentListeners(): void {
    this.ngZone.runOutsideAngular(() => {
      document.addEventListener('keydown', this.onKeydownCapture, true);
      document.addEventListener('wheel', this.onScrollEvent, { passive: false, capture: true });
      document.addEventListener('touchmove', this.onScrollEvent, { passive: false, capture: true });
    });
  }

  private detachDocumentListeners(): void {
    this.closeOnOpened = false;
    document.removeEventListener('keydown', this.onKeydownCapture, true);
    document.removeEventListener('wheel', this.onScrollEvent, true);
    document.removeEventListener('touchmove', this.onScrollEvent, true);
  }

  private initPhotoSwipeGalleryStyle(): void {
    const existingElement = document.getElementById(PHOTO_GALLERY_STYLE_ID);
    if (!existingElement) {
      const cssParser = new cssjs();
      cssParser.testMode = false;
      cssParser.cssPreviewNamespace = PHOTO_GALLERY_CLASS;
      cssParser.createStyleElement(PHOTO_GALLERY_STYLE_ID, PHOTO_GALLERY_STYLE);
    }
  }
}
