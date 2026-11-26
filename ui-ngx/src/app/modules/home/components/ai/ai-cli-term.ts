// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  CLI_ART,
  CLI_TERM_HOLD,
  CLI_TERM_SESSIONS,
  CLI_TERM_WAIT,
  CLI_VERSION,
  CliTermRow,
  CliTermSession
} from '@home/components/ai/ai-cli.models';

export interface CliTerm {
  element: HTMLElement;
  destroy: () => void;
}

const MARK: Record<string, string> = {you: '❯', tool: '●', out: '└', say: '●', ok: '✔', ask: '', think: '⠿'};
const SPIN = '⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏';

let shownTab = 0;
let turning = true;
let termCount = 0;

type Kid = Node | string | null;

function el(tag: string, cls?: string, ...kids: Kid[]): HTMLElement {
  const node = document.createElement(tag);
  if (cls) {
    node.className = cls;
  }
  kids.forEach(kid => {
    if (kid != null) {
      node.append(kid);
    }
  });
  return node;
}

const jitter = (lo: number, hi: number) => lo + Math.random() * (hi - lo);

export function cliTerm(say: (key: string, params?: Record<string, unknown>) => string): CliTerm {
  const calm = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const uid = 'ai-cli-term-' + (++termCount);
  const t = (key: string) => say('ai-assistant.cli.term.' + key);
  const list = el('div', 'ai-cli-term-rows');
  list.setAttribute('role', 'img');
  const view = el('div', 'ai-cli-term-view', list);
  const typed = document.createTextNode('');
  const caret = el('span', 'caret');
  const line = el('span', 't', typed, caret);
  const input = el('div', 'ai-cli-term-input', el('i', null, '❯'), line);
  input.setAttribute('aria-hidden', 'true');
  const body = el('div', 'ai-cli-term-body', view, input);
  body.setAttribute('role', 'tabpanel');
  body.id = uid + '-panel';
  body.tabIndex = 0;
  const touch = () => {
    turning = false;
  };
  const pickTab = (k: number) => {
    touch();
    if (k !== shownTab) {
      play(k);
    }
  };
  const tabs = CLI_TERM_SESSIONS.map((session, k) => {
    const tab = el('button', null, session.tab) as HTMLButtonElement;
    tab.type = 'button';
    tab.setAttribute('role', 'tab');
    tab.id = `${uid}-tab-${k}`;
    tab.setAttribute('aria-controls', body.id);
    tab.addEventListener('click', () => pickTab(k));
    return tab;
  });
  const strip = el('div', 'tabs', ...tabs);
  strip.setAttribute('role', 'tablist');
  strip.setAttribute('aria-label', t('label'));
  strip.addEventListener('keydown', (event: KeyboardEvent) => {
    const k = tabs.indexOf(document.activeElement as HTMLButtonElement);
    const n = tabs.length;
    const to = ({ArrowRight: k + 1, ArrowLeft: k - 1 + n, Home: 0, End: n - 1} as Record<string, number>)[event.key];
    if (k < 0 || to == null) {
      return;
    }
    event.preventDefault();
    pickTab(to % n);
    tabs[to % n].focus();
  });
  const dots = el('span', 'dots', el('i'), el('i'), el('i'));
  dots.setAttribute('aria-hidden', 'true');
  const root = el('div', 'ai-cli-term', el('div', 'ai-cli-term-bar', dots, strip), body);
  root.addEventListener('click', touch);

  let run = 0;
  let spin: ReturnType<typeof setInterval> | null = null;
  let thinking: HTMLElement | null = null;
  let t0 = 0;
  let seen = true;
  let hover = false;
  let started = false;
  let told = false;
  let io: IntersectionObserver | null = null;
  let fallback: ReturnType<typeof setTimeout> | null = null;
  const timers = new Set<ReturnType<typeof setTimeout>>();

  const later = (ms: number, fn: () => void) => {
    const r = run;
    const id = setTimeout(() => {
      timers.delete(id);
      if (r === run && root.isConnected) {
        fn();
      }
    }, ms);
    timers.add(id);
  };
  const stopSpin = () => {
    if (spin) {
      clearInterval(spin);
      spin = null;
    }
  };
  const setInput = (text: string) => {
    typed.data = text;
    line.replaceChildren(typed, caret);
    input.classList.remove('menu');
    caret.classList.toggle('typing', !!text);
  };
  const stop = () => {
    run += 1;
    timers.forEach(clearTimeout);
    timers.clear();
    stopSpin();
    thinking = null;
    setInput('');
  };
  const fit = () => {
    const room = view.clientHeight;
    if (room) {
      view.classList.toggle('full', list.offsetHeight > room - 16);
    }
  };
  const row = (kind: string, ...kids: Kid[]) => {
    const r = el('span', 'r ' + kind, el('i', null, MARK[kind.split(' ')[0]]), el('span', 't', ...kids));
    list.insertBefore(r, thinking);
    fit();
    return r;
  };
  const toolRow = (item: CliTermRow) => row('tool', el('b', null, item.tool), `(${item.arg})`);
  const think = () => {
    t0 = Date.now();
    thinking = el('span', 'r think', el('i', null, SPIN[0]), el('span', 't', t('thinking')));
    list.append(thinking);
    fit();
    let f = 0;
    const glyph = thinking.firstChild as HTMLElement;
    spin = setInterval(() => {
      if (!root.isConnected) {
        stopSpin();
        return;
      }
      f += 1;
      glyph.textContent = SPIN[f % SPIN.length];
    }, 90);
  };
  const done = () => {
    if (!thinking) {
      return;
    }
    stopSpin();
    thinking.classList.add('done');
    thinking.firstChild.textContent = '⠿';
    thinking.lastChild.textContent = say('ai-assistant.cli.term.done', {seconds: Math.max(1, Math.round((Date.now() - t0) / 1000))});
    thinking = null;
  };
  const send = (text: string, next: () => void) => {
    setInput('');
    row('you', text);
    think();
    later(jitter(900, 1500), next);
  };
  const type = (text: string, next: () => void) => {
    let n = 0;
    const tick = () => {
      n += 1;
      typed.data = text.slice(0, n);
      caret.classList.add('typing');
      const ch = text[n - 1];
      if (n < text.length) {
        later(ch === ' ' ? jitter(45, 120) : /[.,:]/.test(ch) ? jitter(140, 280) : jitter(30, 75), tick);
      } else {
        later(420, () => send(text, next));
      }
    };
    tick();
  };
  const pick = (options: string[], next: () => void) => {
    input.classList.add('menu');
    line.replaceChildren(...options.map((option, j) => el('span', 'opt' + (j ? '' : ' on'), `${j + 1}. ${option}`)));
    later(1400, () => {
      (line.firstChild as HTMLElement).classList.add('press');
      later(240, () => send(options[0], next));
    });
  };
  const stream = (node: HTMLElement, text: string, next: () => void) => {
    const words = text.split(' ');
    let n = 0;
    const tick = () => {
      n = Math.min(words.length, n + 1 + Math.floor(Math.random() * 3));
      node.textContent = words.slice(0, n).join(' ');
      fit();
      if (n < words.length) {
        later(jitter(45, 100), tick);
      } else {
        next();
      }
    };
    tick();
  };
  const step = (rows: CliTermRow[], i: number) => {
    if (i >= rows.length) {
      done();
      if (turning) {
        later(CLI_TERM_HOLD, advance);
      }
      return;
    }
    const item = rows[i];
    const go = () => later(CLI_TERM_WAIT[(rows[i + 1] ?? {kind: 'you'}).kind], () => step(rows, i + 1));
    if (item.kind === 'you') {
      done();
      type(t(item.text), () => step(rows, i + 1));
      return;
    }
    if (item.kind === 'pick') {
      done();
      pick(item.options.map(t), () => step(rows, i + 1));
      return;
    }
    if (item.kind === 'tool') {
      toolRow(item);
      go();
      return;
    }
    if (item.kind === 'out') {
      const r = row('out run', t('running'));
      later(item.ms || 1200, () => {
        r.classList.remove('run');
        r.lastChild.textContent = t(item.text);
        fit();
        go();
      });
      return;
    }
    stream(row(item.kind).lastChild as HTMLElement, t(item.text), go);
  };
  const advance = () => {
    if (!turning) {
      return;
    }
    if (hover || !seen || document.hidden) {
      later(600, advance);
      return;
    }
    play((shownTab + 1) % CLI_TERM_SESSIONS.length);
  };
  const intro = (session: CliTermSession) => {
    if (session.banner) {
      list.append(el('div', 'banner', el('span', 'art', CLI_ART), el('b', null, CLI_VERSION),
        el('span', null, el('i', null, '✔'), ' ' + t('connected')), el('span', null, el('i', null, '✔'), ' ' + t('detected'))));
      fit();
      return;
    }
    (session.intro ?? []).forEach(item => {
      const r = item.kind === 'done' ? row('think done', say('ai-assistant.cli.term.done', {seconds: item.seconds}))
        : item.kind === 'tool' ? toolRow(item) : row(item.kind, t(item.text));
      r.classList.add('past');
    });
  };
  const still = (rows: CliTermRow[]) => rows.forEach(item => {
    if (item.kind === 'tool') {
      toolRow(item);
    } else if (item.kind === 'pick') {
      row('you', t(item.options[0]));
    } else {
      row(item.kind, t(item.text));
    }
  });
  const mark = (k: number) => {
    shownTab = k;
    tabs.forEach((tab, j) => {
      tab.setAttribute('aria-selected', String(j === k));
      tab.tabIndex = j === k ? 0 : -1;
    });
    if (strip.scrollWidth > strip.clientWidth) {
      strip.scrollLeft += tabs[k].getBoundingClientRect().left - strip.getBoundingClientRect().left - 10;
    }
    body.setAttribute('aria-labelledby', tabs[k].id);
    list.setAttribute('aria-label', say('ai-assistant.cli.term.picture', {about: t(CLI_TERM_SESSIONS[k].tab + '.about')}));
  };
  const opening = (k: number) => {
    list.replaceChildren();
    view.classList.remove('full');
    intro(CLI_TERM_SESSIONS[k]);
    if (calm) {
      still(CLI_TERM_SESSIONS[k].rows);
    }
  };
  const begin = (k: number) => later(CLI_TERM_WAIT.you, () => step(CLI_TERM_SESSIONS[k].rows, 0));
  function play(k: number): void {
    stop();
    mark(k);
    started = true;
    const go = () => {
      list.classList.remove('out');
      opening(k);
      if (!calm) {
        begin(k);
      }
    };
    if (list.firstChild && !calm) {
      list.classList.add('out');
      later(260, go);
    } else {
      go();
    }
  }
  const refit = () => {
    if (root.isConnected) {
      fit();
    }
  };

  shownTab = Math.min(shownTab, CLI_TERM_SESSIONS.length - 1);
  mark(shownTab);
  opening(shownTab);
  requestAnimationFrame(refit);
  const destroy = () => {
    stop();
    io?.disconnect();
    if (fallback) {
      clearTimeout(fallback);
    }
  };
  if (calm) {
    return {element: root, destroy};
  }
  root.addEventListener('pointerenter', (event: PointerEvent) => {
    if (event.pointerType === 'mouse') {
      hover = true;
    }
  });
  root.addEventListener('pointerleave', () => {
    hover = false;
  });
  const start = () => {
    if (!started) {
      started = true;
      begin(shownTab);
    }
  };
  if ('IntersectionObserver' in window) {
    io = new IntersectionObserver(entries => {
      told = true;
      seen = entries[entries.length - 1].isIntersecting;
      if (seen) {
        start();
      }
    }, {threshold: 0.2});
    io.observe(root);
  }
  fallback = setTimeout(() => {
    fallback = null;
    if (!told && root.isConnected) {
      seen = true;
      start();
    }
  }, 1500);
  return {element: root, destroy};
}
