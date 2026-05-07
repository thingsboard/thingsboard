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
    // Gallery image click-to-expand
    const galleryImages = container.querySelectorAll('.tb-gallery-img');
    galleryImages.forEach(img => {
      img.addEventListener('click', () => {
        img.classList.toggle('tb-gallery-img-expanded');
      });
    });
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
        .map((src: string) => `<img src="${src}" alt="" class="tb-gallery-img" />`)
        .join('');
        return `<div class="tb-gallery">${images}</div>`;
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
