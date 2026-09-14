// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Directive, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import PhotoSwipeLightbox from 'photoswipe/lightbox';
import PhotoSwipe from 'photoswipe';
import cssjs from '@core/css/css';

const PHOTO_GALLERY_STYLE_ID = 'photoswipe-gallery-style';
/** Share of the viewport the opened image is aimed at, leaving the platform visible around it. */
const VIEWPORT_FILL = 0.8;
/** A small screenshot is enlarged to fill that share, but never past this much of its own size. */
const MAX_UPSCALE = 1.5;
const PHOTO_GALLERY_CLASS = 'tb-photoswipe-gallery';
const PHOTO_GALLERY_STYLE =
  // The root only needs its compositing layer neutralised. PhotoSwipe ships
  // `transform: translateZ(0)` and `will-change: opacity` on .pswp and .pswp__bg, and together
  // those promote a layer that backdrop-filter cannot sample the page through — the blur
  // silently does nothing and the backdrop reads as flat black.
  '{\n' +
  '    transform: none;\n' +
  '    will-change: auto;\n' +
  '}\n' +
  '\n' +
  // On .pswp__bg rather than the root element: that is the layer PhotoSwipe fades in along with the
  // zoom, so its opacity is left to PhotoSwipe (see bgOpacity below) instead of being pinned here.
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
  // drop-shadow, not box-shadow, and on the wrap rather than the image. box-shadow traces the
  // element's rectangle, so anything with transparency — an SVG, a PNG with an alpha background —
  // got a shadow drawn around empty space. drop-shadow follows the alpha channel and outlines the
  // picture itself. On the wrap because it is always present, so the shadow does not flicker when
  // the low-res placeholder is swapped for the full image.
  '.pswp__zoom-wrap {\n' +
  '    filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.5));\n' +
  '}\n' +
  '\n' +
  // PhotoSwipe paints a #222 block behind the low-res placeholder while the zoom-from-thumbnail
  // animation runs. Against a translucent backdrop that reads as a black box flashing in and out.
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

/**
 * PhotoSwipe's own `fit` is capped at 1, so a screenshot smaller than the viewport opens at its
 * original size and looks lost on screen. This fills the pan area in both directions instead,
 * capped so a tiny image is not blown up into mush.
 */
function initialZoom(zoomLevel: ZoomLevelSizes): number {
  const { panAreaSize, elementSize } = zoomLevel;
  if (!panAreaSize || !elementSize?.x || !elementSize?.y) {
    return zoomLevel.fit;
  }
  return Math.min(panAreaSize.x / elementSize.x, panAreaSize.y / elementSize.y, MAX_UPSCALE);
}

function thumbnailImage(element: Element): HTMLImageElement {
  return element instanceof HTMLImageElement ? element : element.querySelector('img');
}

/**
 * Bounds of the pixels an `object-fit` image actually paints, as opposed to the bounds of its box.
 * `fill` stretches to the box, so the box is already the right answer.
 */
function renderedImageBounds(image: HTMLImageElement): { x: number; y: number; w: number } {
  const rect = image.getBoundingClientRect();
  const style = getComputedStyle(image);
  const { naturalWidth, naturalHeight } = image;
  let width = rect.width;
  let height = rect.height;
  switch (style.objectFit) {
    case 'contain':
    case 'scale-down': {
      let scale = Math.min(rect.width / naturalWidth, rect.height / naturalHeight);
      if (style.objectFit === 'scale-down') {
        scale = Math.min(scale, 1);
      }
      width = naturalWidth * scale;
      height = naturalHeight * scale;
      break;
    }
    case 'none':
      width = naturalWidth;
      height = naturalHeight;
      break;
  }
  const [positionX, positionY] = style.objectPosition.split(' ');
  return {
    x: rect.left + objectPositionOffset(positionX, rect.width - width),
    y: rect.top + objectPositionOffset(positionY, rect.height - height),
    w: width
  };
}

/** Resolves one axis of a computed `object-position`, which is either a percentage or a length. */
function objectPositionOffset(position: string, freeSpace: number): number {
  const value = parseFloat(position);
  if (isNaN(value)) {
    return freeSpace / 2;
  }
  return position.endsWith('%') ? (value / 100) * freeSpace : value;
}

@Directive({
  selector: '[tbPhotoSwipeGallery]',
  standalone: false
})
export class PhotoSwipeGalleryDirective implements OnInit, OnDestroy {

  @Input() galleryChildrenSelector = '.tb-image';
  @Input() imageCaptionSelector = '.tb-image-tooltip';

  /** Raised when the lightbox opens, and on close with the slide it was left on. */
  @Output() readonly lightboxOpened = new EventEmitter<void>();
  @Output() readonly lightboxClosed = new EventEmitter<number>();

  private lightbox: PhotoSwipeLightbox;
  private lastIndex = 0;

  constructor(
    private elementRef: ElementRef<HTMLElement>
  ) {}

  ngOnInit(): void {
    this.initPhotoSwipeGalleryStyle();
    this.lightbox = new PhotoSwipeLightbox({
      gallery: this.elementRef.nativeElement,
      children: this.galleryChildrenSelector,
      pswpModule: PhotoSwipe,
      counter: false,
      // The backdrop's colour and blur live in PHOTO_GALLERY_STYLE; a full target opacity here is
      // what makes PhotoSwipe fade it in with the zoom rather than flash it on at once.
      bgOpacity: 1,
      mainClass: PHOTO_GALLERY_CLASS,
      paddingFn: viewportSize => {
        const horizontal = viewportSize.x * (1 - VIEWPORT_FILL) / 2;
        const vertical = viewportSize.y * (1 - VIEWPORT_FILL) / 2;
        return { top: vertical, bottom: vertical, left: horizontal, right: horizontal };
      },
      initialZoomLevel: zoomLevel => initialZoom(zoomLevel),
      // Keeps the click-to-zoom step from going backwards for an image opened above its own size.
      secondaryZoomLevel: zoomLevel => Math.max(initialZoom(zoomLevel), Math.min(1, zoomLevel.fit * 3))
    });
    this.lightbox.addFilter('domItemData', (itemData, element) => {
      const image = thumbnailImage(element);
      itemData.src = image.src;
      itemData.width = image.naturalWidth;
      itemData.height = image.naturalHeight;
      // Only a cover-fitted thumbnail is really cropped. Claiming it for the rest makes PhotoSwipe
      // start the zoom from a clipped, slightly oversized frame, which reads as a jump.
      itemData.thumbCropped = getComputedStyle(image).objectFit === 'cover';
      return itemData;
    });
    // PhotoSwipe measures the thumbnail element, but object-fit leaves the rendered image smaller
    // than its box. Hand it the bounds of the pixels actually on screen so the zoom starts exactly
    // where the thumbnail ends. Cover is left alone: PhotoSwipe's own cropped path already fits it.
    this.lightbox.addFilter('thumbBounds', (thumbBounds, itemData) => {
      const element = itemData.element;
      if (!element || itemData.thumbCropped) {
        return thumbBounds;
      }
      const image = thumbnailImage(element);
      return image?.naturalWidth ? renderedImageBounds(image) : thumbBounds;
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
    // PhotoSwipe does not lock page scroll — it only manages overflow inside its own container —
    // so without this the page keeps scrolling behind the open image. Blocking the events rather
    // than setting overflow:hidden on a scroller keeps this working wherever the gallery is used:
    // here the dialog's own content pane scrolls, elsewhere the page does, and the directive
    // cannot know which.
    this.lightbox.on('change', () => {
      this.lastIndex = this.lightbox.pswp?.currIndex ?? this.lastIndex;
    });
    this.lightbox.on('beforeOpen', () => {
      this.lockScroll();
      this.lightboxOpened.emit();
    });
    this.lightbox.on('destroy', () => {
      this.unlockScroll();
      this.lightboxClosed.emit(this.lastIndex);
    });
    this.lightbox.init();
  }

  ngOnDestroy(): void {
    this.unlockScroll();
    if (this.lightbox) {
      this.lightbox.destroy();
    }
  }

  /**
   * Make Escape close the image and nothing else.
   *
   * Without this, one press closes both the image and the dialog behind it: PhotoSwipe listens for
   * Escape on document, and so does the CDK overlay dispatcher that serves mat-dialog, so both
   * react to the same key.
   *
   * The handler runs in the capture phase to get in before CDK, and closes PhotoSwipe itself
   * rather than leaving that to PhotoSwipe's own handler — that one is bound on document in the
   * bubble phase (see pswp.events.add(document, 'keydown', ...)), which stopPropagation from a
   * capture listener on the same node never reaches, so Escape would stop working entirely.
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
    // Escape landed during the opening zoom, where close() early-returns because
    // opener.isOpen is still false. The key is swallowed either way, so dropping it
    // here would leave both the image and the dialog open; close once the animation
    // lets go instead.
    pswp.on('openingAnimationEnd', () => pswp.close());
  };

  private readonly onScrollEvent = (e: Event): void => {
    e.preventDefault();
  };

  private lockScroll(): void {
    document.addEventListener('keydown', this.onKeydownCapture, true);
    document.addEventListener('wheel', this.onScrollEvent, { passive: false, capture: true });
    document.addEventListener('touchmove', this.onScrollEvent, { passive: false, capture: true });
  }

  private unlockScroll(): void {
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
