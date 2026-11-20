// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

export interface SceneContext {
  t: (key: string, params?: Record<string, unknown>) => string;
  locale: string;
  still: boolean;
}

type Child = Node | string | null | undefined | false | Child[];

type Attrs = Record<string, string | null | undefined> & { style?: string };

const ENTITY: Record<string, [string, string]> = {
  device: ['memory', 'device'],
  dashboard: ['dashboard', 'view'],
  rule: ['rule', 'rule'],
  notification: ['notifications', 'people']
};

const TONES = [null, 'sky', 'aqua', 'teal', 'amber', 'lilac'];

const POINTER = '<svg viewBox="0 0 24 24"><path d="M5 3l14 8-6.2 1.6L16 19l-2.6 1.2-3.1-6.3L5 18z"/></svg>';

const CHART = '<svg viewBox="0 0 240 80" preserveAspectRatio="none"><line class="lim" x1="0" y1="30" x2="240" y2="30"/>'
  + '<g class="plot"><path class="ln" d="M0 60 C 20 58, 30 62, 50 59 S 80 56, 100 60 S 130 62, 150 58 S 172 60, 184 52 S 198 14, 210 12 '
  + 'S 228 17, 240 14"/></g></svg>';

function append(el: HTMLElement, kids: Child[]): void {
  for (const kid of kids) {
    if (kid == null || kid === false) {
      continue;
    }
    if (Array.isArray(kid)) {
      append(el, kid);
    } else {
      el.append(kid);
    }
  }
}

function h(tag: string, attrs: Attrs | null, ...kids: Child[]): HTMLElement {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs ?? {})) {
    if (value != null) {
      el.setAttribute(key, value);
    }
  }
  append(el, kids);
  return el;
}

const q = (cls: string, ...kids: Child[]) => h('div', {class: cls}, ...kids);

const icon = (name: string, cls?: string) => h('span', {class: 'mat-icon material-icons-round' + (cls ? ' ' + cls : ''), 'aria-hidden': 'true'}, name);

const svg = (cls: string, markup: string) => {
  const el = h('span', {class: cls});
  el.innerHTML = markup;
  return el;
};

const aiMark = () => h('span', {class: 'tb-ai-mark'}, icon('auto_awesome'));

const delay = (ms: number) => new Promise<void>(resolve => setTimeout(resolve, ms));

const wait = <T extends HTMLElement>(el: T): T => {
  el.classList.add('rq-wait');
  return el;
};

const come = (...els: HTMLElement[]) => els.forEach(el => {
  el.classList.remove('rq-wait');
  el.classList.add('rq-in');
});

type Step = [number, (still: boolean) => void | Promise<void>];

function story(root: HTMLElement, ctx: SceneContext, steps: Step[]): void {
  (async () => {
    for (let n = 0; n < 40 && !root.isConnected; n++) {
      await delay(50);
    }
    if (!root.isConnected) {
      return;
    }
    for (const [ms, step] of steps) {
      if (!ctx.still) {
        await delay(ms);
      }
      if (!root.isConnected) {
        return;
      }
      await step(ctx.still);
    }
  })();
}

async function typeText(el: HTMLElement, text: string, still: boolean): Promise<void> {
  if (still) {
    el.textContent = text;
    return;
  }
  for (let i = 1; i <= text.length && el.isConnected; i++) {
    el.textContent = text.slice(0, i);
    await delay(/[\s,.?]/.test(text[i - 1]) ? 70 + Math.random() * 70 : 24 + Math.random() * 40);
  }
}

const pointer = () => svg('rq-cursor', POINTER);

async function pointAt(cursor: HTMLElement, el: HTMLElement, still: boolean): Promise<void> {
  if (still || !cursor.isConnected || !cursor.offsetParent || !el) {
    return;
  }
  const host = cursor.offsetParent.getBoundingClientRect();
  const rect = el.getBoundingClientRect();
  cursor.classList.add('on');
  cursor.style.transform = `translate(${Math.round(rect.left - host.left + rect.width / 2)}px, ${Math.round(rect.top - host.top + rect.height / 2)}px)`;
  await delay(560);
  cursor.classList.add('press');
  el.classList.add('rq-pressed');
  await delay(170);
  cursor.classList.remove('press');
  el.classList.remove('rq-pressed');
}

function composer(words: HTMLElement, o: { focus?: boolean; compact?: boolean; docked?: boolean; empty?: boolean } = {}): HTMLElement {
  return q('rq-term' + (o.focus ? ' focus' : '') + (o.compact ? ' compact' : '') + (o.docked ? ' docked' : ''),
    q('rq-term-in', icon('chevron_right', 'rq-pre'), h('span', {class: 'rq-term-words'}, words),
      h('span', {class: 'rq-send' + (o.empty ? ' off' : '')}, icon('keyboard_return'))),
    q('rq-term-foot', h('span', {class: 'rq-tool'}, icon('attach_file')), h('span', {class: 'rq-sp'}),
      h('span', {class: 'rq-tool'}, icon('keyboard'))));
}

const userMsg = (text: string | null, files?: HTMLElement[]) =>
  q('rq-user', files ? q('rq-files', files) : null, text ? q('rq-bubble', text) : null);

const fileChip = (ic: string, name: string, meta: string) =>
  q('rq-file', h('span', {class: 'rq-file-ic', 'data-tone': 'rule'}, icon(ic)), h('span', null, h('b', null, name), h('small', null, meta)));

function rec(kind: string, name: string, meta: string, o: { planned?: boolean; done?: boolean } = {}): HTMLElement {
  const [ic, palette] = ENTITY[kind] ?? ENTITY.device;
  return q('rq-rec' + (o.planned ? ' planned' : ''),
    h('span', {class: 'rq-rec-tile', style: `background: var(--entity-${palette}-bg); color: var(--entity-${palette}-fg)`}, icon(ic)),
    h('b', null, name), h('small', null, meta), o.done ? icon('check_circle', 'rq-rec-ok') : null);
}

interface Ask {
  text: string;
  files?: () => HTMLElement[];
  reply: () => HTMLElement[];
}

function heroChat(ctx: SceneContext, asks: Ask[]): { words: HTMLElement; log: HTMLElement } {
  const text = h('span', {class: 'rq-typed'});
  const caret = h('i', {class: 'rq-caret', 'aria-hidden': 'true'});
  const box = h('span', {class: 'rq-typing'}, text, caret);
  const log = q('rq-log');
  const mine = (ask: Ask) => userMsg(ask.text, ask.files ? ask.files() : undefined);
  (async () => {
    for (let n = 0; n < 40 && !box.isConnected; n++) {
      await delay(50);
    }
    if (!box.isConnected) {
      return;
    }
    const send = box.closest('.rq-term')?.querySelector('.rq-send');
    if (ctx.still) {
      caret.remove();
      text.className = 'rq-ph';
      text.textContent = ctx.t('tour.scene.cover.placeholder');
      send?.classList.add('off');
      log.classList.add('open');
      log.replaceChildren(mine(asks[1]), q('rq-agent', asks[1].reply()));
      return;
    }
    const live = () => box.isConnected && !!box.closest('.iv-scene.on');
    for (let k = 0; live(); k = (k + 1) % asks.length) {
      const ask = asks[k];
      text.textContent = '';
      box.classList.remove('sent');
      send?.classList.add('off');
      await delay(k ? 500 : 900);
      send?.classList.remove('off');
      await typeText(text, ask.text, false);
      await delay(550);
      if (!live()) {
        return;
      }
      send?.classList.add('press');
      await delay(140);
      box.classList.add('sent');
      log.classList.add('out');
      await delay(210);
      send?.classList.remove('press');
      send?.classList.add('off');
      text.textContent = '';
      box.classList.remove('sent');
      log.classList.add('open');
      log.replaceChildren(mine(ask));
      log.classList.remove('out');
      await delay(910);
      if (!live()) {
        return;
      }
      const agent = q('rq-agent');
      log.append(agent);
      for (const node of ask.reply()) {
        if (!live()) {
          return;
        }
        agent.append(node);
        await delay(550);
      }
      await delay(1500);
    }
  })();
  return {words: box, log};
}

function sceneCover(ctx: SceneContext, words: CoverWords): HTMLElement[] {
  const t = (key: string) => ctx.t('tour.scene.cover.' + key);
  const chat = heroChat(ctx, [
    {text: t('ask-1'), reply: () => [h('p', null, t('reply-1')), rec('device', t('record-1-name'), t('record-1-meta'))]},
    {text: t('ask-2'), reply: () => [h('p', null, t('reply-2')), rec('dashboard', t('record-2-name'), t('record-2-meta'), {done: true})]},
    {text: t('ask-3'), files: () => [fileChip('picture_as_pdf', t('file-3-name'), t('file-3-meta'))], reply: () => [h('p', null, t('reply-3'))]}
  ]);
  return [q('rq-hero',
    h('span', {class: 'iv-version'}, icon('auto_awesome'), words.pill),
    h('b', {class: 'rq-hero-t'}, h('span', null, words.news), h('span', {class: 'iv-grad'}, words.title)),
    chat.log, composer(chat.words, {focus: true}))];
}

function sceneHome(ctx: SceneContext): HTMLElement[] {
  const t = (key: string) => ctx.t('tour.scene.home.' + key);
  const hour = new Date().getHours();
  const day = new Date().toLocaleDateString(ctx.locale, {weekday: 'long', day: 'numeric', month: 'long'});
  const pill = (...kids: Child[]) => wait(h('span', {class: 'rq-hpill'}, ...kids));
  const critical = pill(icon('warning'), t('critical'));
  const quiet = pill(t('inactive'));
  const placeholder = h('span', {class: 'rq-ph'}, t('placeholder'));
  const text = h('span', {class: 'rq-typed'});
  const caret = h('i', {class: 'rq-caret', 'aria-hidden': 'true'});
  const box = h('span', {class: 'rq-typing'}, placeholder);
  const row = q('rq-hrow',
    q('rq-hfield', icon('chevron_right', 'rq-pre'), box, h('kbd', {class: 'rq-hkey'}, '/'), h('span', {class: 'rq-send'}, icon('keyboard_return'))),
    h('span', {class: 'rq-hchats'}, icon('forum'), h('span', null, t('chats')), icon('chevron_right')));
  const chat = (title: string, meta: string) => h('span', {class: 'rq-hchat'}, h('i', null), h('b', null, title), h('small', null, meta));
  const waits = wait(q('rq-hwait', h('small', null, t('waiting')), q('rq-hchatrow', chat(t('chat-1'), '12m'), chat(t('chat-2'), '2h'))));
  const card = (ic: string, name: string, count: string, widths: number[]) => q('rq-hcard', h('b', null, icon(ic), name, h('small', null, count)),
    widths.map(width => h('i', {style: `width: ${width}%`})));
  const cards = wait(q('rq-hcards', card('warning', t('alarms'), '26', [92, 70, 84]), card('notifications', t('notifications'), '7', [88, 62, 80])));
  const page = q('rq-home', q('rq-hdate', day),
    q('rq-hgreet', aiMark(), t(hour < 12 ? 'morning' : hour < 18 ? 'afternoon' : 'evening')),
    q('rq-hpills', critical, quiet), row, waits, cards);
  story(page, ctx, [
    [600, () => come(critical)],
    [160, () => come(quiet)],
    [450, () => come(waits)],
    [300, () => come(cards)],
    [900, async still => {
      if (still) {
        return;
      }
      row.classList.add('typing');
      box.replaceChildren(text, caret);
      await typeText(text, t('ask'), false);
    }]
  ]);
  return [page];
}

function sceneAnyPage(ctx: SceneContext): HTMLElement[] {
  const t = (key: string) => ctx.t('tour.scene.page.' + key);
  const tile = (key: string, value: string, unit: string) => q('rq-tile', h('small', null, key),
    h('b', null, h('span', {class: 'v'}, value), h('span', {class: 'u'}, unit)));
  const prompt = (ic: string, text: string) => q('rq-prompt', icon(ic), h('span', null, text));
  const load = tile(t('load'), '4.1', 'kW');
  const alarm = q('rq-alarm', icon('warning'), t('alarm'));
  const chart = svg('rq-chart', CHART);
  const ask = prompt('help_outline', t('ask'));
  const body = q('rq-cbody', q('rq-empty', aiMark(), h('b', null, t('title')), q('rq-prompts', ask, prompt('add_chart', t('prompt-2')))));
  const said = wait(h('p', null, t('answer')));
  const about = wait(rec('device', t('record-name'), t('record-meta')));
  const cursor = pointer();
  const win = q('iv-window rq-dock',
    q('iv-sk-page', q('rq-ptop', h('b', null, t('page')), h('small', null, t('range'))),
      q('rq-dash', q('iv-sk-row', load, tile(t('today'), '96', 'kWh')), chart, alarm)),
    q('rq-companion', q('rq-chead', h('b', null, t('new-chat')), h('span', {class: 'rq-sp'}), icon('close')), body,
      composer(h('span', {class: 'rq-ph'}, t('title')), {compact: true, empty: true, docked: true})));
  story(win, ctx, [
    [800, () => chart.classList.add('drawn')],
    [640, () => {
      load.classList.add('hot');
      load.querySelector('.v').textContent = '14.2';
    }],
    [380, () => alarm.classList.add('on')],
    [900, still => pointAt(cursor, ask, still)],
    [0, () => ask.classList.add('picked')],
    [260, () => {
      cursor.classList.remove('on');
      body.classList.add('said');
      const mine = userMsg(t('ask'));
      body.replaceChildren(mine, q('rq-agent', said, about));
      come(mine);
    }],
    [650, () => come(said)],
    [550, () => come(about)]
  ]);
  return [win, cursor];
}

function scenePlan(ctx: SceneContext): HTMLElement[] {
  const t = (key: string, params?: Record<string, unknown>) => ctx.t('tour.scene.plan.' + key, params);
  const rows = ([['dashboard', 'row-1', 'dashboard'], ['rule', 'row-2', 'alarm-rule'], ['notification', 'row-3', 'notification-rule']] as const)
    .map(([kind, name, type]) => {
      const el = wait(rec(kind, t(name), t('planned', {type: t(type)}), {planned: true}));
      const made = () => {
        el.classList.remove('planned');
        el.classList.add('made');
        el.querySelector('small').textContent = t('created', {type: t(type)});
        el.append(icon('check_circle', 'rq-rec-ok'));
      };
      return {el, made};
    });
  const run = h('span', {class: 'rq-btn fill sm'}, icon('play_arrow'), t('run'));
  const cursor = pointer();
  const mine = userMsg(t('request'));
  const head = wait(q('rq-plan-h', h('b', null, t('plan')), h('small', null, t('steps')), h('span', {class: 'rq-sp'}), run));
  const win = q('iv-window rq-col', mine, q('rq-agent rq-plan', head, rows.map(row => row.el)));
  story(win, ctx, [
    [1200, () => come(head)],
    [180, () => come(rows[0].el)],
    [180, () => come(rows[1].el)],
    [180, () => come(rows[2].el)],
    [900, still => pointAt(cursor, run, still)],
    [0, () => {
      run.classList.add('busy');
      run.replaceChildren(t('running'));
    }],
    [260, () => cursor.classList.remove('on')],
    [420, () => rows[0].made()],
    [520, () => rows[1].made()],
    [520, () => {
      rows[2].made();
      run.classList.remove('busy');
      run.classList.add('done');
      run.replaceChildren(icon('check'), t('done'));
    }]
  ]);
  return [win, cursor];
}

function sceneAsk(ctx: SceneContext): HTMLElement[] {
  const t = (key: string) => ctx.t('tour.scene.ask.' + key);
  const chip = (text: string, on?: boolean) => h('span', {class: 'rq-chip' + (on ? ' on' : '')}, text);
  const count = h('small', null, t('count-1'));
  const next = h('span', {class: 'rq-btn fill'}, icon('arrow_forward'), t('next'));
  const first = [chip(t('option-1-1')), chip(t('option-1-2')), chip(t('option-1-3')), chip(t('other'))];
  const said = q('rq-said');
  const ask = q('rq-ask-q', h('b', null, t('question-1')), h('span', null, t('why-1')));
  const chips = q('rq-chips', first);
  const hint = h('p', {class: 'rq-ask-hint'}, t('hint-1'));
  const cursor = pointer();
  const card = q('rq-review rq-ask', q('rq-rev-head', icon('help_outline'), t('needs-answer'), count), said, ask, chips, hint,
    q('rq-actions', next, h('span', {class: 'rq-sp'}), h('span', {class: 'rq-btn text'}, t('skip'))));
  story(card, ctx, [
    [1300, still => pointAt(cursor, first[1], still)],
    [0, () => first[1].classList.add('on')],
    [550, still => pointAt(cursor, next, still)],
    [0, () => {
      cursor.classList.remove('on');
      count.textContent = t('count-2');
      said.replaceChildren(icon('check'), h('span', null, t('said')), h('b', null, t('option-1-2')));
      said.classList.add('on');
      ask.replaceChildren(h('b', null, t('question-2')), h('span', null, t('why-2')));
      chips.replaceChildren(chip(t('option-2-1')), chip(t('option-2-2'), true), chip(t('option-2-3')), chip(t('other')));
      hint.textContent = t('hint-2');
      next.replaceChildren(icon('check'), t('done'));
      come(ask, chips, hint);
    }]
  ]);
  return [card, cursor];
}

function sceneFiles(ctx: SceneContext): HTMLElement[] {
  const t = (key: string) => ctx.t('tour.scene.files.' + key);
  const span = (cls: string, text: string) => h('span', {class: 'hl-' + cls}, text);
  const code: Child[][] = [
    [span('flag', '#include'), ' ', span('str', '"SHT31.h"')],
    [span('key', 'SHT31'), ' sensor;'],
    [''],
    [span('key', 'void'), ' ', span('cmd', 'loop'), '() {'],
    ['  ', span('key', 'float'), ' t = sensor.', span('cmd', 'getTemperature'), '();'],
    ['  ', span('cmd', 'publish'), '(', span('str', '"temperature"'), ', t);'],
    ['  ', span('cmd', 'delay'), '(', span('num', '30000'), ');'],
    ['}']
  ];
  const lines = code.map((parts, k) => h('div', {class: 'rq-ln'}, h('i', null, String(k + 1)), h('span', null, ...parts)));
  const tile = ([name, ic, palette]: string[]) => wait(q('rq-ft',
    h('span', {class: 'rq-file-ic', style: `background: var(--entity-${palette}-bg); color: var(--entity-${palette}-fg)`}, icon(ic)), h('b', null, name)));
  const ins = [['SHT31.pdf', 'picture_as_pdf', 'rule'], ['wiring.jpg', 'image', 'path'], ['stores.xlsx', 'table_chart', 'view'],
    ['registers.csv', 'grid_on', 'view'], ['serial.log', 'subject', 'people']].map(tile);
  const outs = [['sht31.ino', 'code', 'chain'], ['decoder.js', 'integration_instructions', 'chain'], ['config.json', 'data_object', 'device'],
    ['compose.yml', 'tune', 'device'], ['setup.md', 'description', 'people']].map(tile);
  const group = (label: string, tiles: HTMLElement[], cls: string) => q('rq-fgroup ' + cls, h('small', null, label), q('rq-ftiles', tiles));
  const cursor = pointer();
  const win = q('iv-window rq-files-w',
    q('rq-fgrid', group(t('in'), ins, 'in'), group(t('out'), outs, 'out')),
    q('rq-code', q('rq-code-head', icon('code'), h('b', null, 'sht31.ino'), h('span', {class: 'rq-sp'}), icon('search'),
      h('span', {class: 'rq-code-edit'}, t('edit'))), q('rq-code-body', lines)));
  story(win, ctx, [
    [450, async still => {
      for (const el of [...ins, ...outs]) {
        if (!el.isConnected) {
          return;
        }
        come(el);
        if (!still) {
          await delay(55);
        }
      }
    }],
    [650, still => pointAt(cursor, outs[0], still)],
    [0, () => outs[0].classList.add('on')],
    [220, () => win.classList.add('open')],
    [380, async still => {
      cursor.classList.remove('on');
      for (const line of lines) {
        if (!line.isConnected) {
          return;
        }
        line.classList.add('w');
        if (!still) {
          await delay(70);
        }
      }
    }]
  ]);
  return [win, cursor];
}

export interface CoverWords {
  pill: string;
  news: string;
  title: string;
}

export const WELCOME_CARDS = 5;

export function buildScene(n: number, ctx: SceneContext, words: CoverWords): HTMLElement {
  const tone = TONES[n];
  const frame = h('div', {
    class: 'iv-canvas iv-scene ' + (!n ? 'ai-field' : tone === 'sky' ? 'ai-field iv-sky' : 'iv-tone'),
    'data-level': n ? null : 'tile',
    'data-motion': n ? null : 'live',
    'data-tone': n && tone !== 'sky' ? tone : null,
    'data-scene': String(n),
    'aria-hidden': 'true'
  });
  const parts = [() => sceneCover(ctx, words), () => sceneHome(ctx), () => sceneAnyPage(ctx), () => scenePlan(ctx),
    () => sceneAsk(ctx), () => sceneFiles(ctx)][n]();
  append(frame, parts);
  return frame;
}
