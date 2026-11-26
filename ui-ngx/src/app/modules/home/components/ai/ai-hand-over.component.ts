// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, DestroyRef, effect, ElementRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { TranslateService } from '@ngx-translate/core';
import { AiConnectionCheck, AiHandOver } from '@home/components/ai/ai-action.models';
import { aiFileFences, announceCopied, decorateCodeBlocks, labelFileBlocks } from '@home/components/ai/ai-code-block';

export interface AiHandOverWait {
  running: boolean;
  startedAt: number;
  progress?: string;
  endedAt?: number;
  check?: AiConnectionCheck | null;
}

const MIN_SILENCE_MS = 120000;
const COPIED_MS = 1500;

@Component({
  selector: 'tb-ai-hand-over',
  templateUrl: './ai-hand-over.component.html',
  standalone: false
})
export class AiHandOverComponent {

  handOver = input.required<AiHandOver>();
  wait = input<AiHandOverWait | null>(null);
  time = input<number>(0);

  @Output() prompt = new EventEmitter<string>();

  @Output() stop = new EventEmitter<void>();

  private host = inject(ElementRef<HTMLElement>);
  private translate = inject(TranslateService);
  private clipboard = inject(Clipboard);

  readonly picked = signal<string | null>(null);
  readonly ticked = signal<ReadonlySet<number>>(new Set());
  readonly shown = signal<ReadonlySet<string>>(new Set());
  readonly copied = signal<string | null>(null);
  readonly now = signal(Date.now());
  readonly open = signal(false);

  readonly choiceValue = computed(() => this.picked() ?? this.handOver().choice?.value ?? null);
  readonly choiceText = computed(() => this.handOver().choice?.options.find(option => option.id === this.choiceValue())?.text ?? '');

  readonly arrived = computed(() => this.wait()?.check?.dataArriving === true);
  readonly folded = computed(() => this.arrived() && !this.open());
  readonly arrivedLine = computed(() => {
    const wait = this.wait();
    const at = wait?.endedAt ? new Date(wait.endedAt).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit', hourCycle: 'h23'}) : '';
    return this.translate.instant('ai-assistant.hand-over.arrived-' + (this.handOver().waitFor ?? 'message'), {time: at});
  });

  readonly waiting = computed(() => !!this.wait()?.running);
  readonly elapsed = computed(() => Math.max(0, this.now() - (this.wait()?.startedAt ?? this.now())));
  readonly clock = computed(() => {
    const seconds = Math.floor(this.elapsed() / 1000);
    return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
  });
  readonly silenceMs = computed(() => Math.max(MIN_SILENCE_MS, 2 * 1000 * (this.handOver().waitFor === 'gateway' ? 0 : this.handOver().intervalSeconds ?? 0)));
  readonly quiet = computed(() => this.waiting() && this.elapsed() >= this.silenceMs());
  readonly quietMinutes = computed(() => Math.max(2, Math.floor(this.elapsed() / 60000)));
  readonly stoppedQuiet = computed(() => {
    const wait = this.wait();
    return !!wait && !wait.running && !!wait.check && !wait.check.dataArriving;
  });

  constructor() {
    const timer = setInterval(() => {
      if (this.waiting()) {
        this.now.set(Date.now());
      }
    }, 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
    effect(() => {
      this.choiceText();
      this.handOver();
      this.folded();
      setTimeout(() => this.decorate());
    });
  }

  md(text: string): string {
    return aiFileFences(text).text;
  }

  files(text: string): string {
    return aiFileFences(text).files.join('|');
  }

  decorate(): void {
    const root = this.host.nativeElement as HTMLElement;
    root.querySelectorAll<HTMLElement>('tb-markdown[data-files]').forEach(markdown =>
      labelFileBlocks(markdown, (markdown.dataset.files ?? '').split('|')));
    decorateCodeBlocks(root, this.translate, text => this.clipboard.copy(text));
  }

  waitLabel(): string {
    return this.translate.instant('ai-assistant.hand-over.wait-' + (this.handOver().waitFor ?? 'message'));
  }

  pick(id: string): void {
    this.picked.set(id);
  }

  tick(index: number): void {
    const next = new Set(this.ticked());
    if (next.has(index)) {
      next.delete(index);
    } else {
      next.add(index);
    }
    this.ticked.set(next);
  }

  done(index: number): boolean {
    return this.ticked().has(index) !== (this.handOver().steps[index]?.done === true);
  }

  reveal(key: string): void {
    const next = new Set(this.shown());
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    this.shown.set(next);
  }

  masked(value: string): string {
    return value.length > 8 ? value.slice(0, 4) + '••••••••' + value.slice(-4) : '••••••••';
  }

  copy(key: string, value: string): void {
    if (this.clipboard.copy(value)) {
      this.copied.set(key);
      announceCopied(this.translate.instant('ai-assistant.copied'));
      setTimeout(() => this.copied.set(null), COPIED_MS);
    }
  }

  say(key: string): void {
    this.prompt.emit(this.translate.instant(key));
  }
}
