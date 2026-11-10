// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, computed, effect, inject, ElementRef, EventEmitter, input, Output, signal, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AiFile, aiImageSrc, downloadAiFile, downloadAiFileAsPng } from '@home/components/ai/ai-file.models';
import {
  AiAttachmentPreviewDialogComponent,
  AiAttachmentPreviewDialogData
} from '@home/components/ai/ai-attachment-preview-dialog.component';
import { highlightCode } from '@home/components/ai/ai-code-block';

export interface AiFileMarks {
  from: number;
  to: number;
}

@Component({
  selector: 'tb-ai-file-body',
  template: `
    @if (file().markdown && !source()) {
      <div class="file-md art-page agent">
        <tb-markdown usePlainMarkdown [data]="file().content" [additionalStyles]="markdownStyles"></tb-markdown>
      </div>
    } @else if (file().image && !source()) {
      <div class="file-img art-page">
        <button type="button" class="file-img-open" (click)="expandImage()"
                [matTooltip]="'ai-assistant.file.expand' | translate" matTooltipPosition="below" [attr.aria-label]="'ai-assistant.file.expand' | translate">
          <img [src]="imageSrc()" [alt]="file().name">
        </button>
      </div>
    } @else {
      @if (finding()) {
        <div class="file-find" role="search">
          <mat-icon class="tb-mat-18 file-find-ic" aria-hidden="true">search</mat-icon>
          <input #findInput type="search" class="file-find-in" spellcheck="false"
                 [placeholder]="'ai-assistant.file.find' | translate" [attr.aria-label]="'ai-assistant.file.find' | translate"
                 [value]="query()" (input)="setQuery(findInput.value)" (keydown)="onFindKey($event)">
          @if (query()) {
            <span class="file-find-n" aria-live="polite">{{ matches() ? ('ai-assistant.file.match-of' | translate: {at: current() + 1, count: matches()}) : ('ai-assistant.file.no-matches' | translate) }}</span>
          }
          <button type="button" class="file-find-b" (click)="step(-1)" [matTooltip]="'ai-assistant.file.previous-match' | translate" matTooltipPosition="below"
                  [attr.aria-label]="'ai-assistant.file.previous-match' | translate"><mat-icon class="tb-mat-18" aria-hidden="true">keyboard_arrow_up</mat-icon></button>
          <button type="button" class="file-find-b" (click)="step(1)" [matTooltip]="'ai-assistant.file.next-match' | translate" matTooltipPosition="below"
                  [attr.aria-label]="'ai-assistant.file.next-match' | translate"><mat-icon class="tb-mat-18" aria-hidden="true">keyboard_arrow_down</mat-icon></button>
          <button type="button" class="file-find-b" (click)="closeFind()" [matTooltip]="'ai-assistant.file.close-find' | translate" matTooltipPosition="below"
                  [attr.aria-label]="'ai-assistant.file.close-find' | translate"><mat-icon class="tb-mat-18" aria-hidden="true">close</mat-icon></button>
        </div>
      }
      <div #code class="file-code" [class.wrap]="wrap()" [class.ws]="whitespace()" [attr.data-lang]="file().lang"
           (click)="onCodeClick($event)"></div>
    }
  `,
  standalone: false,
  host: {class: 'file-body', '(keydown)': 'onKey($event)'}
})
export class AiFileBodyComponent implements AfterViewInit {

  file = input.required<AiFile>();
  wrap = input(false);
  source = input(false);
  whitespace = input(false);
  marks = input<AiFileMarks | null>(null);

  @Output() marksChange = new EventEmitter<AiFileMarks | null>();

  @ViewChild('code') code: ElementRef<HTMLElement>;
  @ViewChild('findInput') findInput: ElementRef<HTMLInputElement>;

  private dialog = inject(MatDialog);

  imageSrc = computed(() => aiImageSrc(this.file()));

  finding = signal(false);
  query = signal('');
  matches = signal(0);
  current = signal(0);

  private anchor = 0;

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
    this.whitespace();
    setTimeout(() => this.drawCode());
  });

  private remark = effect(() => {
    const marks = this.marks();
    setTimeout(() => this.paintMarks(marks));
  });

  ngAfterViewInit(): void {
    this.drawCode();
  }

  expandImage(): void {
    const file = this.file();
    this.dialog.open<AiAttachmentPreviewDialogComponent, AiAttachmentPreviewDialogData>(AiAttachmentPreviewDialogComponent, {
      panelClass: ['tb-dialog', 'tb-ai-attachment-preview-dialog'],
      maxWidth: '92vw',
      data: {name: file.name, url: this.imageSrc(), vector: true, download: () => downloadAiFile(file), downloadPng: () => downloadAiFileAsPng(file)}
    });
  }

  openFind(): void {
    this.finding.set(true);
    setTimeout(() => {
      this.findInput?.nativeElement.focus();
      this.findInput?.nativeElement.select();
    });
  }

  closeFind(): void {
    this.finding.set(false);
    this.query.set('');
    this.paintFind();
  }

  setQuery(value: string): void {
    this.query.set(value);
    this.current.set(0);
    this.paintFind();
  }

  step(by: number): void {
    const count = this.matches();
    if (!count) {
      return;
    }
    this.current.set((this.current() + by + count) % count);
    this.focusMatch();
  }

  onFindKey(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.step(event.shiftKey ? -1 : 1);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.closeFind();
    }
  }

  onKey(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'f' && !(this.file().previewed && !this.source())) {
      event.preventDefault();
      this.openFind();
    }
  }

  onCodeClick(event: MouseEvent): void {
    const number = (event.target as HTMLElement).closest('.ln') as HTMLElement | null;
    if (!number) {
      return;
    }
    const line = Number(number.dataset.line);
    const marks = this.marks();
    if (event.shiftKey && marks) {
      this.marksChange.emit({from: Math.min(this.anchor, line), to: Math.max(this.anchor, line)});
      return;
    }
    if (marks && marks.from === line && marks.to === line) {
      this.marksChange.emit(null);
      return;
    }
    this.anchor = line;
    this.marksChange.emit({from: line, to: line});
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
      const number = document.createElement('button');
      number.type = 'button';
      number.className = 'ln';
      number.tabIndex = -1;
      number.dataset.line = String(index + 1);
      number.textContent = String(index + 1);
      const code = document.createElement('code');
      const indent = /^\s*/.exec(line.textContent ?? '')?.[0].length ?? 0;
      code.style.setProperty('--ind', indent + 'ch');
      if (this.whitespace()) {
        AiFileBodyComponent.markWhitespace(line);
      }
      code.append(line);
      item.append(number, code);
      list.append(item);
    });
    container.replaceChildren(list);
    this.paintMarks(this.marks());
    this.paintFind();
  }

  private paintMarks(marks: AiFileMarks | null): void {
    this.code?.nativeElement.querySelectorAll<HTMLElement>('.file-lines li').forEach((item, index) => {
      item.classList.toggle('marked', !!marks && index + 1 >= marks.from && index + 1 <= marks.to);
    });
  }

  private paintFind(): void {
    const container = this.code?.nativeElement;
    if (!container) {
      return;
    }
    container.querySelectorAll('mark.fm').forEach(mark => mark.replaceWith(...Array.from(mark.childNodes)));
    container.normalize();
    const query = this.query();
    let count = 0;
    if (query) {
      container.querySelectorAll<HTMLElement>('.file-lines li > code').forEach(code => {
        count += AiFileBodyComponent.wrapMatches(code, query.toLowerCase());
      });
    }
    this.matches.set(count);
    if (this.current() >= count) {
      this.current.set(0);
    }
    this.focusMatch();
  }

  private focusMatch(): void {
    const marks = this.code?.nativeElement.querySelectorAll<HTMLElement>('mark.fm') ?? [];
    marks.forEach((mark, index) => mark.classList.toggle('now', index === this.current()));
    marks[this.current()]?.scrollIntoView({block: 'nearest', inline: 'nearest'});
  }

  private static wrapMatches(root: HTMLElement, query: string): number {
    const text = (root.textContent ?? '').toLowerCase();
    const starts: number[] = [];
    for (let at = text.indexOf(query); at >= 0; at = text.indexOf(query, at + query.length)) {
      starts.push(at);
    }
    for (let i = starts.length - 1; i >= 0; i--) {
      AiFileBodyComponent.wrapRange(root, starts[i], starts[i] + query.length);
    }
    return starts.length;
  }

  private static wrapRange(root: HTMLElement, from: number, to: number): void {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const pieces: {node: Text; start: number; end: number}[] = [];
    let offset = 0;
    for (let node = walker.nextNode() as Text | null; node; node = walker.nextNode() as Text | null) {
      const length = node.data.length;
      const start = Math.max(from, offset);
      const end = Math.min(to, offset + length);
      if (start < end) {
        pieces.push({node, start: start - offset, end: end - offset});
      }
      offset += length;
    }
    pieces.forEach(({node, start, end}) => {
      const middle = node.splitText(start);
      middle.splitText(end - start);
      const mark = document.createElement('mark');
      mark.className = 'fm';
      middle.replaceWith(mark);
      mark.append(middle);
    });
  }

  private static markWhitespace(line: HTMLElement): void {
    const full = line.textContent ?? '';
    const trailFrom = full.replace(/[ \t]+$/, '').length;
    const walker = document.createTreeWalker(line, NodeFilter.SHOW_TEXT);
    const nodes: Text[] = [];
    for (let node = walker.nextNode() as Text | null; node; node = walker.nextNode() as Text | null) {
      nodes.push(node);
    }
    let offset = 0;
    nodes.forEach(node => {
      const data = node.data;
      const fragment = document.createDocumentFragment();
      let plain = '';
      for (let i = 0; i < data.length; i++) {
        const char = data[i];
        const at = offset + i;
        if (char !== ' ' && char !== '\t') {
          plain += char;
          continue;
        }
        if (plain) {
          fragment.append(plain);
          plain = '';
        }
        const span = document.createElement('span');
        span.textContent = char;
        const run = (full[at - 1] === ' ' || full[at + 1] === ' ') && char === ' ';
        span.className = char === '\t' ? 'ws tab' : at >= trailFrom ? 'ws trail' : run || at < (/^\s*/.exec(full)?.[0].length ?? 0) ? 'ws' : 'ws quiet';
        fragment.append(span);
      }
      if (plain) {
        fragment.append(plain);
      }
      offset += data.length;
      node.replaceWith(fragment);
    });
  }

}
