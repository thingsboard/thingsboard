// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, effect, ElementRef, input, ViewChild } from '@angular/core';
import { AiFile } from '@home/components/ai/ai-file.models';
import { highlightCode } from '@home/components/ai/ai-code-block';

@Component({
  selector: 'tb-ai-file-body',
  template: `
    @if (file().markdown && !source()) {
      <div class="file-md art-page agent">
        <tb-markdown usePlainMarkdown [data]="file().content" [additionalStyles]="markdownStyles"></tb-markdown>
      </div>
    } @else {
      <div #code class="file-code" [class.wrap]="wrap()" [attr.data-lang]="file().lang"></div>
    }
  `,
  standalone: false,
  host: {class: 'file-body'}
})
export class AiFileBodyComponent implements AfterViewInit {

  file = input.required<AiFile>();
  wrap = input(false);
  source = input(false);

  @ViewChild('code') code: ElementRef<HTMLElement>;

  readonly markdownStyles = [
    '.tb-markdown-view {font-size: var(--chat-text); line-height: var(--chat-text-lh); color: var(--tb-ink);} ' +
    '.tb-markdown-view > :first-child {margin-top: 0 !important;} ' +
    '.tb-markdown-view h1, .tb-markdown-view h2, .tb-markdown-view h3, .tb-markdown-view h4, .tb-markdown-view h5 ' +
    '{margin: 28px 0 8px !important; padding: 0 !important; border: 0 !important; font-weight: 500 !important; letter-spacing: -0.1px; color: var(--tb-ink);} ' +
    '.tb-markdown-view h1, .tb-markdown-view h2 {font-size: var(--chat-title) !important; line-height: var(--chat-title-lh) !important; letter-spacing: -0.3px;} ' +
    '.tb-markdown-view h3 {font-size: var(--chat-head) !important; line-height: var(--chat-head-lh) !important;} ' +
    '.tb-markdown-view h4, .tb-markdown-view h5 {font-size: var(--chat-line) !important; line-height: var(--chat-line-lh) !important;} ' +
    '.tb-markdown-view p {margin: 0 0 12px !important; padding: 0 !important; font-size: inherit; line-height: inherit;} ' +
    '.tb-markdown-view ol, .tb-markdown-view ul {margin: 0 0 12px !important; padding-left: 24px !important; padding-inline-start: 24px;} ' +
    '.tb-markdown-view li {font-size: inherit; line-height: inherit;} .tb-markdown-view li + li {margin-top: 4px;} ' +
    '.tb-markdown-view div.code-wrapper {padding-left: 0 !important; margin: 0 0 16px !important;} ' +
    '.tb-markdown-view pre {border-radius: 10px !important;} ' +
    '.tb-markdown-view table, .tb-markdown-view div.code-wrapper {margin-left: 0 !important; margin-right: 0 !important;} ' +
    '.tb-markdown-view :not(pre) > code {padding: 1px 5px; border-radius: 4px; background: color-mix(in srgb, var(--tb-ink) 6%, transparent) !important; ' +
    'color: var(--tb-ink) !important; font-family: var(--tb-mono); font-size: var(--chat-code);}'
  ];

  private redraw = effect(() => {
    this.file();
    this.source();
    setTimeout(() => this.drawCode());
  });

  ngAfterViewInit(): void {
    this.drawCode();
  }

  private drawCode(): void {
    const container = this.code?.nativeElement;
    if (!container) {
      return;
    }
    const file = this.file();
    const list = document.createElement('ol');
    list.className = 'file-lines';
    highlightCode(file.lang, file.content.replace(/\n$/, '')).forEach((line, index) => {
      const item = document.createElement('li');
      const number = document.createElement('span');
      number.className = 'ln';
      number.textContent = String(index + 1);
      number.setAttribute('aria-hidden', 'true');
      const code = document.createElement('code');
      const indent = /^\s*/.exec(line.textContent ?? '')?.[0].length ?? 0;
      code.style.setProperty('--ind', indent + 'ch');
      code.append(line);
      item.append(number, code);
      list.append(item);
    });
    container.replaceChildren(list);
  }

}
