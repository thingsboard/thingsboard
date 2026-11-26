// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

export type CliOs = 'unix' | 'windows';

export type CliAgent = 'claude' | 'antigravity';

export type CliWay = 'terminal' | 'agent';

export type CliBuild = 'sensor' | 'solution' | 'change';

export const CLI_OS_GROUP = 'cli-os';

export const CLI_AGENT_GROUP = 'cli-agent';

export const CLI_DOCS = 'https://thingsboard.io/docs/cli/';

export const CLI_INSTALL: Record<CliOs, string> = {
  unix: 'curl -LsSf https://thingsboard.io/cli/install.sh | sh',
  windows: 'powershell -ExecutionPolicy ByPass -c "irm https://thingsboard.io/cli/install.ps1 | iex"'
};

export interface CliAgentInfo {
  id: CliAgent;
  name: string;
  run: string;
  restart: string;
  get: string;
  glow: string;
}

export const CLI_AGENTS: CliAgentInfo[] = [
  {id: 'claude', name: 'Claude Code', run: 'claude', restart: 'claude --continue', get: 'https://claude.com/claude-code', glow: '#D97757'},
  {id: 'antigravity', name: 'Antigravity', run: 'agy', restart: 'agy', get: 'https://antigravity.google/', glow: '#3186FF'}
];

export interface CliBuildInfo {
  id: CliBuild;
  prompt: string;
  link?: string;
}

export const CLI_BUILDS: CliBuildInfo[] = [
  {
    id: 'sensor',
    prompt: 'My office has a sensor that sends {"temperature": 22.5, "humidity": 41, "co2": 650} every minute. '
      + 'Create the device and a dashboard. Alert me when CO2 goes above 1000.'
  },
  {
    id: 'solution',
    prompt: 'Create a smart retail solution for my supermarket chain: three stores, each with refrigeration, ambient and door sensors, '
      + 'an admin for the chain, a manager for each store, and alarms for the freezers.',
    link: 'guides/example-smart-retail/'
  },
  {
    id: 'change',
    prompt: 'Each freezer in my stores needs its own alarm limit. Let me set the limit from the dashboard.',
    link: 'guides/alarms/'
  }
];

export interface CliDocsLink {
  key: string;
  icon: string;
  path: string;
}

export const CLI_DOCS_LINKS: CliDocsLink[] = [
  {key: 'device', icon: 'sensors', path: 'guides/connect-real-device/'},
  {key: 'troubleshooting', icon: 'build', path: 'reference/troubleshooting/'},
  {key: 'all', icon: 'menu_book', path: ''}
];

export type CliTermKind = 'you' | 'pick' | 'tool' | 'out' | 'say' | 'ok' | 'ask' | 'done';

export interface CliTermRow {
  kind: CliTermKind;
  text?: string;
  tool?: string;
  arg?: string;
  ms?: number;
  options?: string[];
  seconds?: number;
}

export interface CliTermSession {
  tab: string;
  banner?: boolean;
  intro?: CliTermRow[];
  rows: CliTermRow[];
}

export const CLI_VERSION = 'thingsboard-cli 4.4.0.2.post1';

export const CLI_ART = [
  ' _____ _     _                 ____                      _ ',
  '|_   _| |__ (_)_ __   __ _ ___| __ )  ___   __ _ _ __ __| |',
  '  | | | \'_ \\| | \'_ \\ / _` / __|  _ \\ / _ \\ / _` | \'__/ _` |',
  '  | | | | | | | | | | (_| \\__ \\ |_) | (_) | (_| | | | (_| |',
  '  |_| |_| |_|_|_| |_|\\__, |___/____/ \\___/ \\__,_|_|  \\__,_|',
  '                     |___/                                 '
].join('\n');

export const CLI_TERM_WAIT: Record<string, number> = {you: 1200, pick: 300, tool: 380, out: 60, say: 520, ok: 110, ask: 260};

export const CLI_TERM_HOLD = 6000;

export const CLI_TERM_SESSIONS: CliTermSession[] = [
  {
    tab: 'office-sensor',
    banner: true,
    rows: [
      {kind: 'you', text: 'office-sensor.you'},
      {kind: 'tool', tool: 'Skill', arg: 'create-solution'},
      {kind: 'out', text: 'office-sensor.written', ms: 2600},
      {kind: 'say', text: 'office-sensor.ready'},
      {kind: 'ok', text: 'office-sensor.device'},
      {kind: 'ok', text: 'office-sensor.alarm'},
      {kind: 'ok', text: 'office-sensor.dashboard'},
      {kind: 'ask', text: 'office-sensor.ask'},
      {kind: 'pick', options: ['office-sensor.stream', 'office-sensor.deploy-only', 'office-sensor.not-now']},
      {kind: 'tool', tool: 'Bash', arg: 'tb push office'},
      {kind: 'out', text: 'office-sensor.deployed', ms: 1500},
      {kind: 'tool', tool: 'Bash', arg: 'tb emulator run --duration 5m'},
      {kind: 'out', text: 'office-sensor.streaming', ms: 1100}
    ]
  },
  {
    tab: 'smart-retail',
    intro: [
      {kind: 'tool', tool: 'Bash', arg: 'tb device list'},
      {kind: 'out', text: 'smart-retail.devices'},
      {kind: 'say', text: 'smart-retail.connected'},
      {kind: 'done', seconds: 6}
    ],
    rows: [
      {kind: 'you', text: 'smart-retail.you'},
      {kind: 'tool', tool: 'Skill', arg: 'design-solution-architecture'},
      {kind: 'out', text: 'smart-retail.designed', ms: 2800},
      {kind: 'say', text: 'smart-retail.approve'},
      {kind: 'pick', options: ['smart-retail.step', 'smart-retail.whole']},
      {kind: 'say', text: 'smart-retail.first'},
      {kind: 'ok', text: 'smart-retail.overview'},
      {kind: 'ok', text: 'smart-retail.store'},
      {kind: 'ask', text: 'smart-retail.build'},
      {kind: 'pick', options: ['smart-retail.yes', 'smart-retail.not-yet']},
      {kind: 'tool', tool: 'Skill', arg: 'create-solution'},
      {kind: 'out', text: 'smart-retail.built', ms: 3000},
      {kind: 'say', text: 'smart-retail.next'}
    ]
  },
  {
    tab: 'freezer-limits',
    intro: [
      {kind: 'tool', tool: 'Skill', arg: 'create-solution'},
      {kind: 'out', text: 'smart-retail.built'},
      {kind: 'say', text: 'smart-retail.next'},
      {kind: 'done', seconds: 41}
    ],
    rows: [
      {kind: 'you', text: 'freezer-limits.you'},
      {kind: 'tool', tool: 'Skill', arg: 'update-solution'},
      {kind: 'out', text: 'freezer-limits.updated', ms: 2400},
      {kind: 'say', text: 'freezer-limits.done'},
      {kind: 'ok', text: 'freezer-limits.attribute'},
      {kind: 'ok', text: 'freezer-limits.alarm'},
      {kind: 'ok', text: 'freezer-limits.dashboard'},
      {kind: 'ask', text: 'freezer-limits.ask'},
      {kind: 'pick', options: ['freezer-limits.push', 'freezer-limits.not-now']},
      {kind: 'tool', tool: 'Bash', arg: 'tb push smart-retail --profile dev'},
      {kind: 'out', text: 'freezer-limits.deployed', ms: 1600}
    ]
  }
];

export function cliOs(): CliOs {
  const nav = navigator as Navigator & {userAgentData?: {platform?: string}};
  return /win/i.test(nav.userAgentData?.platform || navigator.platform || '') ? 'windows' : 'unix';
}

export function asciiGlobe(rows: number, cols: number, cx: number, cy: number, radius: number): string {
  const ramp = ' .:-+*';
  const light = [-0.55, -0.62, 0.56];
  const lines: string[] = [];
  for (let r = 0; r < rows; r++) {
    let line = '';
    for (let c = 0; c < cols; c++) {
      const x = (c - cx) * 0.62 / radius;
      const y = (r - cy) / radius;
      const d = x * x + y * y;
      if (d > 1) {
        line += (r % 4 === 1 && c % 11 === (r * 3) % 11) ? '.' : ' ';
        continue;
      }
      const z = Math.sqrt(1 - d);
      const b = Math.max(0, x * light[0] + y * light[1] + z * light[2]);
      const lat = Math.asin(-y);
      const lon = Math.atan2(x, z) + 0.35;
      const onGrid = Math.abs(Math.sin(lat * 5)) < 0.09 || Math.abs(Math.sin(lon * 6)) < 0.07;
      line += onGrid ? ramp[Math.min(ramp.length - 1, 1 + Math.round(b * 4))] : b > 0.82 && (r * 7 + c) % 9 === 0 ? '.' : ' ';
    }
    lines.push(line);
  }
  return lines.join('\n');
}
