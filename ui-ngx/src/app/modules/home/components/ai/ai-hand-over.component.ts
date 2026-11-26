// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewChecked, Component, computed, DestroyRef, effect, ElementRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { TranslateService } from '@ngx-translate/core';
import { AiConnectionCheck, AiHandOver } from '@home/components/ai/ai-action.models';
import { aiFenceMeta, announceCopied, codeTabGroup, codeTabsBlock, decorateCodeBlocks } from '@home/components/ai/ai-code-block';

export interface AiHandOverWait {
  running: boolean;
  startedAt: number;
  progress?: string;
  endedAt?: number;
  check?: AiConnectionCheck | null;
}

const MIN_SILENCE_MS = 120000;
const TEST_MARKER = 'aiTestValue';
const OS_LABELS: Record<string, string> = {macos: 'macOS', windows: 'Windows', linux: 'Linux'};

function localOs(): string {
  const platform = navigator.userAgent;
  return /Windows/i.test(platform) ? 'windows' : /Mac/i.test(platform) ? 'macos' : 'linux';
}
const COPIED_MS = 1500;

@Component({
  selector: 'tb-ai-hand-over',
  templateUrl: './ai-hand-over.component.html',
  standalone: false
})
export class AiHandOverComponent implements AfterViewChecked {

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
  readonly testState = signal<'idle' | 'sending' | 'sent' | 'failed'>('idle');
  readonly testAt = signal(0);
  readonly testError = signal('');

  readonly choiceValue = computed(() => this.picked() ?? this.handOver().choice?.value ?? null);
  readonly steps = computed(() => {
    const picked = this.handOver().choice?.options.find(option => option.id === this.choiceValue());
    return picked?.steps?.length ? picked.steps : this.handOver().steps;
  });
  readonly choiceText = computed(() => this.handOver().choice?.options.find(option => option.id === this.choiceValue())?.text ?? '');

  readonly arrived = computed(() => this.wait()?.check?.dataArriving === true);
  readonly folded = computed(() => this.arrived() && !this.open());
  readonly arrivedLine = computed(() => {
    const wait = this.wait();
    const at = wait?.endedAt ? new Date(wait.endedAt).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit', hourCycle: 'h23'}) : '';
    return this.translate.instant('ai-assistant.hand-over.arrived-' + (this.handOver().waitFor ?? 'message'), {time: at});
  });

  readonly waiting = computed(() => !!this.wait()?.running);
  readonly canSendTest = computed(() => !!this.handOver().test?.sample && this.handOver().waitFor === 'message');
  readonly testNote = computed(() => {
    const time = new Date(this.testAt()).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit', hourCycle: 'h23'});
    switch (this.testState()) {
      case 'sent':
        return this.translate.instant('ai-assistant.hand-over.test-sent', {time});
      case 'failed':
        return this.translate.instant('ai-assistant.hand-over.test-failed', {error: this.testError()});
      default:
        return '';
    }
  });
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
      this.steps();
      this.handOver();
      this.folded();
      setTimeout(() => this.decorate());
    });
  }

  md(text: string): string {
    return aiFenceMeta(text);
  }

  ngAfterViewChecked(): void {
    if ((this.host.nativeElement as HTMLElement).querySelector('.k-test-slot:empty')) {
      this.decorate();
    }
  }

  decorate(): void {
    const root = this.host.nativeElement as HTMLElement;
    const test = this.handOver().test;
    root.querySelectorAll<HTMLElement>('.k-test-slot:empty').forEach(slot => {
      if (!test) {
        return;
      }
      const order = ['macos', 'windows', 'linux'];
      const tabs = [...test.commands].sort((a, b) => order.indexOf(a.os) - order.indexOf(b.os)).map(command => ({id: command.os, label: OS_LABELS[command.os], text: command.command,
        lang: command.os === 'windows' ? 'powershell' : 'shell'}));
      const preferred = codeTabGroup('os') ?? localOs();
      const initial = Math.max(0, tabs.findIndex(tab => tab.id === preferred));
      slot.append(codeTabsBlock(tabs, 'shell', this.translate, text => this.clipboard.copy(text), initial, undefined, {group: 'os'}));
    });
    decorateCodeBlocks(root, this.translate, text => this.clipboard.copy(text));
  }

  waitLabel(): string {
    return this.translate.instant('ai-assistant.hand-over.wait-' + (this.handOver().waitFor ?? 'message'));
  }

  pick(id: string): void {
    this.picked.set(id);
    this.ticked.set(new Set());
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
    return this.ticked().has(index) !== (this.steps()[index]?.done === true);
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

  async sendTest(): Promise<void> {
    const test = this.handOver().test;
    if (!test?.sample || !test.accessToken || this.testState() === 'sending') {
      return;
    }
    this.testState.set('sending');
    const ts = Date.now();
    try {
      const response = await fetch(`/api/v1/${encodeURIComponent(test.accessToken)}/telemetry`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ts, values: {...test.sample, [TEST_MARKER]: true}})
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      this.testAt.set(ts);
      this.testState.set('sent');
    } catch (error) {
      this.testError.set(error instanceof Error ? error.message : String(error));
      this.testState.set('failed');
    }
  }

  say(key: string): void {
    this.prompt.emit(this.translate.instant(key));
  }
}
