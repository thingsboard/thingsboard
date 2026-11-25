// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  signal
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ClipboardService } from 'ngx-clipboard';
import {
  codeTabGroup,
  codeTabsBlock,
  commandRow,
  labelledCodeBlock,
  onCodeTabGroup,
  valueBlock
} from '@home/components/ai/ai-code-block';
import {
  asciiGlobe,
  CLI_AGENT_GROUP,
  CLI_AGENTS,
  CLI_BUILDS,
  CLI_DOCS,
  CLI_DOCS_LINKS,
  CLI_INSTALL,
  CLI_OS_GROUP,
  CliAgentInfo,
  CliBuild,
  cliOs,
  CliOs,
  CliWay
} from '@home/components/ai/ai-cli.models';

interface CliPicks {
  way?: CliWay;
  build?: CliBuild;
  open?: boolean;
}

interface CliTreeRow {
  depth: number;
  name: string;
  last: boolean;
  icon: string;
  tone?: string;
}

@Component({
  selector: 'tb-ai-cli-guide',
  templateUrl: './ai-cli-guide.component.html',
  styleUrls: ['./ai-cli-guide.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
  host: {
    'data-ai': '',
    'class': 'tb-ai-cli-guide'
  }
})
export class AiCliGuideComponent {

  private static readonly PICKS_KEY = 'tb-ai-cli';

  copiedUntil = input<number>(0);

  closed = output<void>();

  readonly docs = CLI_DOCS;

  readonly agents = CLI_AGENTS;

  readonly builds = CLI_BUILDS;

  readonly links = CLI_DOCS_LINKS;

  readonly ways: CliWay[] = ['agent', 'terminal'];

  readonly globe = asciiGlobe(36, 210, 160, 9, 24);

  readonly url = window.location.origin;

  readonly tree: CliTreeRow[] = [
    {depth: 0, name: 'thingsboard-project', last: true, icon: 'folder'},
    {depth: 1, name: 'solutions/office', last: true, icon: 'folder'},
    {depth: 2, name: 'devices', last: false, icon: 'memory', tone: 'device'},
    {depth: 2, name: 'dashboards', last: false, icon: 'dashboard', tone: 'view'},
    {depth: 2, name: 'device-profiles', last: true, icon: 'category', tone: 'device'}
  ];

  private readonly picks = AiCliGuideComponent.readPicks();

  way = signal<CliWay>(this.picks.way === 'terminal' ? 'terminal' : 'agent');

  build = signal<CliBuild>(CLI_BUILDS.some(item => item.id === this.picks.build) ? this.picks.build : 'sensor');

  promptOpen = signal(!!this.picks.open);

  os = signal<CliOs>(AiCliGuideComponent.storedOs());

  fullscreen = signal(false);

  scrolled = signal(false);

  private translate = inject(TranslateService);
  private clipboard = inject(ClipboardService);
  private router = inject(Router);
  private injector = inject(Injector);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly hostElement = this.host.nativeElement;

  constructor() {
    const stop = onCodeTabGroup(CLI_OS_GROUP, id => this.os.set(id === 'windows' ? 'windows' : 'unix'));
    inject(DestroyRef).onDestroy(stop);
    afterRenderEffect(() => {
      this.way();
      this.build();
      this.os();
      this.fillSlots();
    });
  }

  pickWay(way: CliWay, event: Event): void {
    if (way === this.way()) {
      return;
    }
    const control = event.currentTarget as HTMLElement;
    const top = control.getBoundingClientRect().top;
    this.way.set(way);
    this.savePicks();
    afterNextRender(() => {
      const scroller = this.scroller();
      if (scroller && control.isConnected) {
        scroller.scrollTop += control.getBoundingClientRect().top - top;
        control.focus({preventScroll: true});
      }
    }, {injector: this.injector});
  }

  pickBuild(build: CliBuild): void {
    this.build.set(build);
    this.savePicks();
  }

  togglePrompt(event: Event): void {
    const control = event.currentTarget as HTMLElement;
    const top = control.getBoundingClientRect().top;
    const open = !this.promptOpen();
    this.promptOpen.set(open);
    this.savePicks();
    if (!open) {
      afterNextRender(() => {
        const scroller = this.scroller();
        if (scroller) {
          scroller.scrollTop += control.getBoundingClientRect().top - top;
        }
      }, {injector: this.injector});
    }
  }

  toSteps(): void {
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    this.hostElement.querySelector('#ai-cli-steps')?.scrollIntoView({behavior: reduce ? 'auto' : 'smooth', block: 'start'});
  }

  openApiKeys(): void {
    this.router.navigate(['/account/security']);
  }

  toggleFullscreen(): void {
    this.fullscreen.set(!this.fullscreen());
  }

  onScroll(scroller: HTMLElement): void {
    this.scrolled.set(scroller.scrollTop > 0);
  }

  buildLink(build: CliBuild): string | null {
    const link = CLI_BUILDS.find(item => item.id === build)?.link;
    return link ? CLI_DOCS + link : null;
  }

  private scroller(): HTMLElement | null {
    return this.hostElement.querySelector<HTMLElement>('.ai-cli-scroll');
  }

  private fillSlots(): void {
    this.hostElement.querySelectorAll<HTMLElement>('.ai-cli-slot').forEach(slot => {
      const key = slot.dataset.kind + ':' + (slot.dataset.key ?? '') + (slot.dataset.kind === 'prompt-hero' ? ':' + this.copiedUntil() : '');
      if (slot.dataset.built !== key) {
        slot.dataset.built = key;
        slot.replaceChildren(this.blockFor(slot.dataset.kind, slot.dataset.key));
      }
    });
  }

  private blockFor(kind: string, key: string | undefined): HTMLElement {
    const copy = (text: string) => this.clipboard.copyFromContent(text);
    const say = (word: string) => this.translate.instant('ai-assistant.cli.' + word);
    switch (kind) {
      case 'install-hero':
        return this.installBlock(true);
      case 'install':
        return this.installBlock(false);
      case 'prompt-hero':
        return labelledCodeBlock(say('setup-prompt'), say('setup-prompt-label'), 'text', this.translate, copy,
          {labelledCopy: true, classes: 'wrap', copiedFor: Math.max(0, this.copiedUntil() - Date.now())});
      case 'prompt':
        return labelledCodeBlock(say('setup-prompt'), say('setup-prompt-label'), 'text', this.translate, copy,
          {check: true, classes: 'wrap'});
      case 'url':
        return valueBlock(say('setup.url'), this.url, this.translate, copy, say('setup.copy-url'));
      case 'agent-start':
        return this.agentBlock(agent => `mkdir thingsboard-project && cd thingsboard-project && ${agent.run}`);
      case 'agent-return':
        return this.agentBlock(agent => `cd thingsboard-project && ${agent.run}`);
      case 'agent-restart':
        return this.agentBlock(agent => agent.restart);
      case 'build':
        return labelledCodeBlock(CLI_BUILDS.find(item => item.id === key)?.prompt ?? '', say('describe.prompt'), 'text', this.translate, copy,
          {check: true, classes: 'wrap'});
      case 'emulator':
        return commandRow('tb emulator run --duration 5m', key === 'windows', this.translate, copy);
      default:
        return document.createElement('span');
    }
  }

  private installBlock(hero: boolean): HTMLElement {
    return codeTabsBlock([
      {id: 'unix', label: this.translate.instant('ai-assistant.cli.mac-linux'), text: CLI_INSTALL.unix, lang: 'shell'},
      {id: 'windows', label: this.translate.instant('ai-assistant.cli.windows'), text: CLI_INSTALL.windows, lang: 'powershell'}
    ], 'shell', this.translate, text => this.clipboard.copyFromContent(text), this.os() === 'windows' ? 1 : 0,
      tab => this.os.set(tab.id === 'windows' ? 'windows' : 'unix'),
      {group: CLI_OS_GROUP, label: this.translate.instant('ai-assistant.cli.terminal'), icon: false,
        labelledCopy: hero, check: !hero});
  }

  private agentBlock(text: (agent: CliAgentInfo) => string): HTMLElement {
    return codeTabsBlock(this.agents.map(agent => ({id: agent.id, label: agent.name, text: text(agent), lang: 'shell'})),
      'shell', this.translate, value => this.clipboard.copyFromContent(value), 0, undefined,
      {group: CLI_AGENT_GROUP, label: this.translate.instant('ai-assistant.cli.terminal'), icon: false, check: true});
  }

  private savePicks(): void {
    const picks: CliPicks = {way: this.way(), build: this.build(), open: this.promptOpen()};
    try {
      localStorage.setItem(AiCliGuideComponent.PICKS_KEY, JSON.stringify(picks));
    } catch {
      return;
    }
  }

  private static readPicks(): CliPicks {
    try {
      const stored = localStorage.getItem(AiCliGuideComponent.PICKS_KEY);
      return stored ? JSON.parse(stored) ?? {} : {};
    } catch {
      return {};
    }
  }

  private static storedOs(): CliOs {
    const stored = codeTabGroup(CLI_OS_GROUP);
    return stored === 'windows' || stored === 'unix' ? stored : cliOs();
  }
}
