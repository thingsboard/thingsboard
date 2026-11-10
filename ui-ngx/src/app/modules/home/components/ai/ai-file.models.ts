// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { fileLanguage } from '@home/components/ai/ai-code-block';

export const WRITE_FILE_TOOL = 'write_file';

export interface AiFile {
  executionId: string;
  name: string;
  content: string;
  summary?: string;
  lang: string;
  label: string;
  icon: string;
  lines: number;
  size: string;
  markdown: boolean;
}

export function aiFileOf(executionId: string, args: unknown): AiFile | null {
  const values = (args ?? {}) as {name?: unknown; content?: unknown; summary?: unknown};
  if (typeof values.name !== 'string' || !values.name.trim() || typeof values.content !== 'string') {
    return null;
  }
  const name = values.name.trim();
  const language = fileLanguage(name);
  const content = values.content;
  const bytes = new TextEncoder().encode(content).length;
  return {
    executionId,
    name,
    content,
    summary: typeof values.summary === 'string' ? values.summary : undefined,
    lang: language.lang,
    label: language.label,
    icon: language.icon,
    lines: content.replace(/\n$/, '').split('\n').length,
    size: bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`,
    markdown: language.label === 'Markdown'
  };
}
