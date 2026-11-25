// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

export type CliOs = 'unix' | 'windows';

export type CliAgent = 'claude' | 'antigravity';

export type CliWay = 'terminal' | 'agent';

export type CliBuild = 'sensor' | 'solution' | 'change';

export const CLI_OS_GROUP = 'cli-os';

export const CLI_AGENT_GROUP = 'cli-agent';

export const CLI_COPIED_MS = 4000;

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
}

export const CLI_AGENTS: CliAgentInfo[] = [
  {id: 'claude', name: 'Claude Code', run: 'claude', restart: 'claude --continue', get: 'https://claude.com/claude-code'},
  {id: 'antigravity', name: 'Antigravity', run: 'agy', restart: 'agy', get: 'https://antigravity.google/'}
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
      + 'Create the device and a dashboard for it. Alert me when CO2 goes above 1000.'
  },
  {
    id: 'solution',
    prompt: 'Create a smart retail solution for my supermarket chain: three stores, each with refrigeration, ambient and door sensors, '
      + 'an admin for the chain, a manager for each store, and alarms for the freezers.',
    link: 'guides/example-smart-retail/'
  },
  {
    id: 'change',
    prompt: 'Each freezer in my stores needs its own alarm limit. Let me set it from the dashboard.',
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

export function cliOs(): CliOs {
  const nav = navigator as Navigator & {userAgentData?: {platform?: string}};
  return /win/i.test(nav.userAgentData?.platform || navigator.platform || '') ? 'windows' : 'unix';
}

export function cliInstallCommand(): string {
  return CLI_INSTALL[cliOs()];
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
