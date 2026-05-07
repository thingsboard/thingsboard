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

import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  Type
} from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { IotHubItemLinkModule } from './iot-hub-item-link-card/iot-hub-item-link.module';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import {
  replaceItemLinkPlaceholders,
  resolveDocLinkPlaceholders
} from '@home/components/iot-hub/iot-hub-markdown.utils';
import { DevicePackageInfo } from '@shared/models/iot-hub/device-package.models';
import PhotoSwipeLightbox from 'photoswipe/lightbox';
import PhotoSwipe from 'photoswipe';

@Component({
  selector: 'tb-iot-hub-markdown',
  standalone: false,
  templateUrl: './iot-hub-markdown.component.html',
  styleUrls: ['./iot-hub-markdown.component.scss']
})
export class TbIotHubMarkdownComponent implements OnInit, OnChanges {

  @Input() data: string | undefined;

  @Input() item: MpItemVersionView | undefined;

  @Input() packageInfo: DevicePackageInfo | undefined;

  @Input() imageMap: Map<string, string> | undefined;

  @Input() onResolveVariable: (key: string) => string | undefined = () => undefined;

  @Input()
  @coerceBoolean()
  lineNumbers = false;

  @Input()
  @coerceBoolean()
  fallbackToPlainMarkdown = false;

  @Input()
  codeBlockMaxHeightPx: number;

  @Output() ready = new EventEmitter<HTMLElement>();

  additionalStyles: string[] = [];

  parsedData: string;

  readonly itemLinkCompileModules: Type<any>[] = [IotHubItemLinkModule];

  constructor(
    private iotHubApiService: IotHubApiService,
    private elementRef: ElementRef<HTMLElement>
  ) {}

  ngOnInit(): void {
    if (this.codeBlockMaxHeightPx) {
      const codeBlockMaxHeightStyle = `pre[class*="language-"] \n{
                max-height: ${this.codeBlockMaxHeightPx}px;\n
      }`;
      this.additionalStyles.push(codeBlockMaxHeightStyle);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (propName === 'data' && change.currentValue !== change.previousValue) {
        this.parsedData = this.parseData(this.data);
      }
    }
  }

  onReady() {
    const container = this.elementRef.nativeElement;
    const galleryImages = container.querySelectorAll<HTMLElement>('.tb-gallery-images');
    const lightbox = new PhotoSwipeLightbox({
      gallery: galleryImages,
      children: '.tb-gallery-image',
      pswpModule: PhotoSwipe,
      counter: false,
      bgOpacity: 0
    });
    lightbox.addFilter('domItemData', (itemData, element) => {
      const image = element.querySelector('img');
      itemData.src = image.src;
      itemData.width = image.naturalWidth;
      itemData.height = image.naturalHeight;
      itemData.thumbCropped = true;
      return itemData;
    });
    lightbox.on('change', () => {
      const item = lightbox.pswp.currSlide.content.element;// element.querySelector('img');
      item.style.display = 'block';
      item.style.maxWidth = '90vw';
      item.style.maxHeight = '78vh';
      item.style.objectFit = 'contain';
      item.style.borderRadius = '4px';
      item.style.boxShadow = '0 20px 60px #00000080';
    });
    lightbox.on('uiRegister', () => {
      lightbox.pswp.element.style.background = '#0a0a148c';
      lightbox.pswp.element.style.backdropFilter = 'blur(18px)';
      lightbox.pswp.element.style.setProperty('-webkit-backdrop-filter', 'blur(18px)');
      lightbox.pswp.ui.registerElement({
        name: 'custom-caption',
        order: 9,
        isButton: false,
        appendTo: 'root',
        html: '<span class="tb-gallery-caption"></span><span class="tb-gallery-counter"></span>',
        onInit: (el, pswp) => {
          el.style.position = 'fixed';
          el.style.bottom = '1.5rem';
          el.style.left = '50%';
          el.style.transform = 'translate(-50%)';
          el.style.zIndex = '10000';
          el.style.display = 'flex';
          el.style.flexDirection = 'column';
          el.style.alignItems = 'center';
          el.style.gap = '.25rem';
          el.style.maxWidth = '80vw';
          el.style.textAlign = 'center';
          el.style.pointerEvents = 'none';
          const caption = el.querySelector<HTMLElement>('.tb-gallery-caption');
          caption.style.color = '#fff';
          caption.style.fontSize = '1.125rem';
          caption.style.lineHeight = '1.5';
          caption.style.background = '#000000a6';
          caption.style.padding = '.5rem 1.25rem';
          caption.style.borderRadius = '8px';
          caption.style.backdropFilter = 'blur(8px)';
          caption.style.setProperty('-webkit-backdrop-filter', 'blur(8px)');
          const counter = el.querySelector<HTMLElement>('.tb-gallery-counter');
          counter.style.color = '#fff9';
          counter.style.fontSize = '.8rem';
          lightbox.pswp.on('change', () => {
            counter.innerText = pswp.currIndex + 1 + pswp.options.indexIndicatorSep + pswp.getNumItems();
            const currSlideElement = lightbox.pswp.currSlide.data.element;
            let captionHTML = '';
            if (currSlideElement) {
              const imageTooltip = currSlideElement.querySelector('.tb-image-tooltip');
              if (imageTooltip) {
                captionHTML = imageTooltip.innerHTML;
              }
            }
            caption.innerHTML = captionHTML || '';
          });
        }
      });
    });
    lightbox.init();
    this.ready.emit(container);
  }

  private parseData(content: string | undefined): string {
    let parsed = this.prefixResourceUrls(content || '');
    parsed = this.resolveDocLinks(parsed);
    parsed = replaceItemLinkPlaceholders(parsed);
    parsed = this.resolveImages(parsed);
    parsed = this.resolveVariables(parsed);
    return parsed;
  }

  private prefixResourceUrls(markdown: string): string {
    const baseUrl = this.iotHubApiService.baseUrl;
    return markdown.replace(/([("])(\/api\/resources\/[^)"]*)/g, `$1${baseUrl}$2`);
  }

  private resolveDocLinks(markdown: string): string {
    if (this.item || this.packageInfo) {
      const dd = this.item ? this.item.dataDescriptor : this.packageInfo;
      return resolveDocLinkPlaceholders(
        markdown,
        this.item?.name || dd?.name || '',
        { productURL: dd?.productURL, datasheetURL: dd?.datasheetURL },
        { productPage: 'Product page', datasheet: 'Datasheet' }
      );
    } else {
      return markdown;
    }
  }

  private resolveImages(content: string): string {
    if (this.imageMap) {
      return content.replace(/!\[([^\]]*)]\(([^)]+)\)/g, (match, alt, path) => {
        if (path.startsWith('data:') || path.startsWith('http')) {
          return match;
        }
        const dataUri = this.imageMap.get(path);
        return dataUri ? `![${alt}](${dataUri})` : match;
      });
    } else {
      return content;
    }
  }

  private resolveImage(uri: string): string {
    if (this.imageMap && uri) {
      if (uri.startsWith('data:') || uri.startsWith('http')) {
        return uri;
      }
      const dataUri = this.imageMap.get(uri);
      return dataUri ? dataUri : uri;
    } else {
      return uri;
    }
  }

  private resolveVariables(content: string): string {
    return content.replace(/\$\{([^}]+)}/g, (_match, key) => {
      // Callout boxes: ${note(...)}, ${warn(...)}, ${error(...)}
      const calloutMatch = key.match(/^(note|warn|error)\((.+)\)$/s);
      if (calloutMatch) {
        const type = calloutMatch[1];
        const text = calloutMatch[2];
        const icons: Record<string, string> = { note: 'info_outline', warn: 'warning_amber', error: 'error_outline' };
        return `<div class="tb-callout tb-callout-${type}"><i class="material-icons tb-callout-icon">${icons[type]}</i><span class="tb-callout-text">${text}</span></div>`;
      }
      // Image gallery: ${images.gallery(path1,path2,path3)}
      const galleryMatch = key.match(/^images\.gallery\((.+)\)$/);
      if (galleryMatch) {
        const paths = galleryMatch[1].split(',').map((p: string) => p.trim());
        const images = paths
        .map((p: string) => this.resolveImage(p))
        .filter((src: string | undefined) => !!src)
        .map((src: string) => `<button class="tb-gallery-image"><span class="tb-image-container">
              <img src="${src}" alt=""/>
        </span><span class="tb-image-tooltip">Placeholder!</span></button>`)
        .join('');
        return `<div class="tb-gallery-images">${images}</div>`;
      }

      // Special variables
      const res = this.onResolveVariable(key);
      if (res) {
        return res;
      }
      return '${' + key + '}';
    });
  }
}
