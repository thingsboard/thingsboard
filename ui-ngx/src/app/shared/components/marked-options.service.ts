///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { MarkedOptions, MarkedRenderer } from 'ngx-markdown';
import { Inject, Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DOCUMENT } from '@angular/common';
import { WINDOW } from '@core/services/window.service';

const copyCodeBlock = '{:copy-code}';
const autoBlock = '{:auto}';
const targetBlankBlock = '{:target=&quot;_blank&quot;}';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class MarkedOptionsService extends MarkedOptions {

  renderer = new MarkedRenderer();
  headerIds = true;
  gfm = true;
  breaks = false;
  pedantic = false;
  smartLists = true;
  smartypants = false;

  private renderer2 = new MarkedRenderer();

  private id = 1;

  constructor(private translate: TranslateService,
              @Inject(WINDOW) private readonly window: Window,
              @Inject(DOCUMENT) private readonly document: Document) {
    super();
    this.renderer.code = (code: string, language: string | undefined, isEscaped: boolean) => {
      if (code.endsWith(copyCodeBlock)) {
        code = code.substring(0, code.length - copyCodeBlock.length);
        const content = postProcessCodeContent(this.renderer2.code(code, language, isEscaped), code);
        this.id++;
        return this.wrapCopyCode(this.id, content, code);
      } else {
        return this.wrapDiv(postProcessCodeContent(this.renderer2.code(code, language, isEscaped), code));
      }
    };
    this.renderer.table = (header: string, body: string) => {
      let autoLayout = false;
      if (header.includes(autoBlock)) {
        autoLayout = true;
        header = header.replace(autoBlock, '');
      }
      let table = this.renderer2.table(header, body);
      if (autoLayout) {
        table = table.replace('<table', '<table class="auto"');
      }
      return table;
    };
    this.renderer.tablecell = (content: string, flags: {
      header: boolean;
      align: 'center' | 'left' | 'right' | null;
    }) => {
      if (content.endsWith(copyCodeBlock)) {
        content = content.substring(0, content.length - copyCodeBlock.length);
        this.id++;
        content = this.wrapCopyCode(this.id, content, content);
      }
      return this.renderer2.tablecell(content, flags);
    };
    this.renderer.link = (href: string | null, title: string | null, text: string) => {
      if (text.endsWith(targetBlankBlock)) {
        text = text.substring(0, text.length - targetBlankBlock.length);
        const content = this.renderer2.link(href, title, text);
        return content.replace('<a href=', '<a target="_blank" href=');
      } else {
        return this.renderer2.link(href, title, text);
      }
    };
    this.document.addEventListener('selectionchange', this.onSelectionChange.bind(this));
    (this.window as any).markdownCopyCode = this.markdownCopyCode.bind(this);
  }

  private wrapDiv(content: string): string {
    return `<div>${content}</div>`;
  }

  private wrapCopyCode(id: number, content: string, code: string): string {
    return `<div class="code-wrapper noChars" id="codeWrapper${id}" onClick="markdownCopyCode(${id})">${content}` +
      `<span id="copyCodeId${id}" style="display: none;">${encodeURIComponent(code)}</span>` +
      `<button class="clipboard-btn">\n` +
      `    <p>${this.translate.instant('markdown.copy-code')}</p>\n` +
      `    <div>\n` +
      `       <img src="/assets/copy-code-icon.svg" alt="${this.translate.instant('markdown.copy-code')}">\n` +
      `    </div>\n` +
      `</button>` +
      `</div>`;
  }

  private onSelectionChange() {
    const codeWrappers = $('.code-wrapper');
    codeWrappers.removeClass('noChars');
    const selectedChars = this.getSelectedText();
    if (!selectedChars) {
      codeWrappers.addClass('noChars');
    }
  }

  private getSelectedText(): string {
    let text;
    if (this.window.getSelection) {
      text = this.window.getSelection().toString();
    } else if (this.document.getSelection) {
      text = this.document.getSelection();
    } else if ((this.document as any).selection) {
      text = (this.document as any).selection.createRange().text;
    }
    return text;
  }

  private markdownCopyCode(id: number) {
    const copyWrapper = $('#codeWrapper' + id);
    if (copyWrapper.hasClass('noChars')) {
      const text = decodeURIComponent($('#copyCodeId' + id).text());
      this.window.navigator.clipboard.writeText(text).then(() => {
        import('tooltipster').then(
          () => {
            if (!copyWrapper.hasClass('tooltipstered')) {
              copyWrapper.tooltipster(
                {
                  content: this.translate.instant('markdown.copied'),
//              theme: 'tooltipster-shadow',
                  delay: 0,
                  trigger: 'custom',
                  triggerClose: {
                    click: true,
                    tap: true,
                    scroll: true,
                    mouseleave: true
                  },
                  side: 'top',
                  distance: 12,
                  trackOrigin: true
                }
              );
            }
            const tooltip = copyWrapper.tooltipster('instance');
            tooltip.open();
          }
        );
      });
    }
  }
}

function postProcessCodeContent(content: string, code: string): string {
  const lineCount = code.trim().split('\n').length;
  let replacement;
  if (lineCount < 2) {
    replacement = '<pre ngNonBindable class="no-line-numbers">';
  } else {
    replacement = '<pre ngNonBindable>';
  }
  return content.replace('<pre>', replacement);
}
