///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import PhotoSwipeLightbox from 'photoswipe/lightbox';
import PhotoSwipe from 'photoswipe';
import cssjs from '@core/css/css';

const PHOTO_GALLERY_STYLE_ID = 'photoswipe-gallery-style';
const PHOTO_GALLERY_CLASS = 'tb-photoswipe-gallery';
const PHOTO_GALLERY_STYLE =
  '{\n'+
  ' background: rgba(10, 10, 20, 0.55);\n' +
  '    backdrop-filter: blur(18px);\n' +
  '    opacity: 1;\n' +
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
  '    border-radius: 4px;    \n' +
  '    box-shadow: 0 20px 60px #00000080;\n' +
  '}\n' +
  '\n' +
  '.pswp__item .pswp__img--placeholder {\n' +
  '    border-radius: 4px; \n' +
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

@Directive({
  selector: '[tbPhotoSwipeGallery]',
  standalone: false
})
export class PhotoSwipeGalleryDirective implements OnInit, OnDestroy {

  @Input() galleryChildrenSelector = '.tb-image';
  @Input() imageCaptionSelector = '.tb-image-tooltip';

  private lightbox: PhotoSwipeLightbox;

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
      bgOpacity: 0,
      mainClass: PHOTO_GALLERY_CLASS
    });
    this.lightbox.addFilter('domItemData', (itemData, element) => {
      let image: HTMLImageElement;
      if (element instanceof HTMLImageElement) {
        image = element;
      } else {
        image = element.querySelector('img');
      }
      itemData.src = image.src;
      itemData.width = image.naturalWidth;
      itemData.height = image.naturalHeight;
      itemData.thumbCropped = true;
      return itemData;
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
    this.lightbox.init();
  }

  ngOnDestroy(): void {
    if (this.lightbox) {
      this.lightbox.destroy();
    }
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
