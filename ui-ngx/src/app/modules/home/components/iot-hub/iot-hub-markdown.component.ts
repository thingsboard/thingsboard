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
  escapeHtmlAttr,
  replaceItemLinkPlaceholders,
  resolveDocLinkPlaceholders,
  sanitizeInlineHtml
} from '@home/components/iot-hub/iot-hub-markdown.utils';
import { DevicePackageInfo } from '@shared/models/iot-hub/device-package.models';
import { isNotEmptyStr } from '@core/utils';

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
    this.ready.emit(container);
  }

  private parseData(content: string | undefined): string {
    let parsed = this.prefixResourceUrls(content || '');
    parsed = this.resolveDocLinks(parsed);
    parsed = replaceItemLinkPlaceholders(parsed);
    parsed = this.resolveImages(parsed);
    parsed = this.resolveVariables(parsed);
    parsed = this.forceLinksOpenInNewTab(parsed);
    return parsed;
  }

  // Ensure every link rendered through tb-markdown opens in a new tab.
  //  1) For markdown links `[text](url)` — append the
  //     `{:target="_blank"}` suffix to the link TEXT (not the URL)
  //     so MarkedOptionsService.renderer.link, which checks
  //     `token.text.endsWith(targetBlankBlock)`, emits
  //     `target="_blank"` on the rendered <a>. Skip links whose
  //     text already ends with the suffix and skip image syntax
  //     (`![alt](url)`).
  //  2) For raw <a ...> HTML anchors authored in the markdown — add
  //     `target="_blank"` when no `target=` attribute is already
  //     present.
  private forceLinksOpenInNewTab(content: string): string {
    const TARGET_BLANK_BLOCK = '{:target="_blank"}';
    content = content.replace(
      /(?<!!)\[([^\]]*?)]\(([^)]+)\)/g,
      (match, text: string, url: string) =>
        text.endsWith(TARGET_BLANK_BLOCK) ? match : `[${text}${TARGET_BLANK_BLOCK}](${url})`
    );
    content = content.replace(
      /<a\b([^>]*)>/gi,
      (match, attrs: string) =>
        /\btarget\s*=/i.test(attrs) ? match : `<a${attrs} target="_blank">`
    );
    return content;
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
    // Image gallery is handled first because its inner ${...} contents
    // may include nested braces and span multiple lines, which the
    // generic ${key} regex below cannot parse.
    //
    // Format: ${images.gallery({src: 'p1', alt: 'a1', caption: 'c1'}, {src: 'p2'}, ...)}
    // Each image entry is a JS object literal with a required `src`
    // and optional `alt` / `caption` string fields.
    content = content.replace(/\$\{\s*images\.gallery\(([\s\S]*?)\)\s*}/g, (_match, inner: string) => {
      const objects: string[] = inner.match(/\{[\s\S]*?}/g) || [];
      const items = objects
        .map((obj: string) => {
          const src = (obj.match(/src\s*:\s*(['"])((?:(?!\1).)*)\1/) || [])[2] || '';
          const alt = (obj.match(/alt\s*:\s*(['"])((?:(?!\1).)*)\1/) || [])[2] || '';
          const caption = (obj.match(/caption\s*:\s*(['"])((?:(?!\1).)*)\1/) || [])[2] || '';
          return { src: this.resolveImage(src), alt, caption };
        })
        .filter((item: { src?: string }) => !!item.src);
      const images = items
        .map(item => {
          let galleryImageHtml =  `<button class="tb-gallery-image">
                                            <span class="tb-image-container">
                                               <img src="${item.src}" alt="${escapeHtmlAttr(item.alt)}"/>
                                            </span>`;
          if (isNotEmptyStr(item.caption)) {
            galleryImageHtml += `<span class="tb-image-tooltip">${sanitizeInlineHtml(item.caption)}</span>`;
          }
          galleryImageHtml += `</button>`;
          return galleryImageHtml;
        })
        .join('');
      return `<div class="tb-gallery-images" tbPhotoSwipeGallery galleryChildrenSelector=".tb-gallery-image" imageCaptionSelector=".tb-image-tooltip">${images}</div>`;
    });

    return content.replace(/\$\{([^}]+)}/g, (_match, key) => {
      // Callout boxes: ${note(...)}, ${warn(...)}, ${error(...)}
      const calloutMatch = key.match(/^(note|warn|error)\((.+)\)$/s);
      if (calloutMatch) {
        const type = calloutMatch[1];
        const text = calloutMatch[2];
        const icons: Record<string, string> = { note: 'info_outline', warn: 'warning_amber', error: 'error_outline' };
        return `<div class="tb-callout tb-callout-${type}"><i class="material-icons tb-callout-icon">${icons[type]}</i><span class="tb-callout-text">${text}</span></div>`;
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
