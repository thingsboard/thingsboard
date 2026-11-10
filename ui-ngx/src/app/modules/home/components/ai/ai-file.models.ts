// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { fileLanguage } from '@home/components/ai/ai-code-block';
import { AiResourceInfo } from '@shared/models/ai-chat.models';

export const WRITE_FILE_TOOL = 'write_file';
export const CREATE_FILE_TOOL = 'create_file';

export interface AiMadeFile {
  executionId: string;
  resource: AiResourceInfo;
  label: string;
  icon: string;
  size: string;
  preview: 'image' | 'pdf' | null;
}

const MADE_KINDS: Record<string, {label: string; icon: string; preview: 'image' | 'pdf' | null}> = {
  'image/png': {label: 'PNG', icon: 'image', preview: 'image'},
  'application/pdf': {label: 'PDF', icon: 'picture_as_pdf', preview: 'pdf'},
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': {label: 'Word', icon: 'description', preview: null},
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': {label: 'Excel', icon: 'table_chart', preview: null}
};

export function aiMadeFileOf(executionId: string, result: unknown): AiMadeFile | null {
  let parsed: unknown = result;
  if (typeof result === 'string') {
    try {
      parsed = JSON.parse(result);
    } catch {
      return null;
    }
  }
  const file = (parsed as {file?: Partial<AiResourceInfo>})?.file;
  if (!file || typeof file.id !== 'string' || typeof file.name !== 'string' || typeof file.mediaType !== 'string') {
    return null;
  }
  const kind = MADE_KINDS[file.mediaType] ?? {label: file.name.split('.').pop()?.toUpperCase() ?? 'File', icon: 'draft', preview: null};
  const bytes = typeof file.size === 'number' ? file.size : 0;
  return {
    executionId,
    resource: {id: file.id, name: file.name, mediaType: file.mediaType, size: bytes},
    label: kind.label,
    icon: kind.icon,
    size: formatSize(bytes),
    preview: kind.preview
  };
}

function formatSize(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

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
  image: boolean;
  previewed: boolean;
}

const PNG_SCALE = 2;
const PNG_MAX_SIDE = 4096;

const XML_ENTITIES = new Set(['amp', 'lt', 'gt', 'quot', 'apos']);

export function aiImageSrc(file: AiFile): string {
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xmlEntities(file.content));
}

function xmlEntities(svg: string): string {
  const decoder = document.createElement('textarea');
  return svg.replace(/&([a-zA-Z][a-zA-Z0-9]*);/g, (entity, name: string) => {
    if (XML_ENTITIES.has(name)) {
      return entity;
    }
    decoder.innerHTML = entity;
    const text = decoder.value;
    return text === entity ? '&amp;' + name + ';' : Array.from(text).map(char => `&#${char.codePointAt(0)};`).join('');
  });
}

export function downloadAiFile(file: AiFile): void {
  save(new Blob([file.content], {type: file.image ? 'image/svg+xml' : 'text/plain;charset=utf-8'}), file.name);
}

export function downloadAiFileAsPng(file: AiFile): Promise<void> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => {
      const size = svgSize(file.content) ?? {width: image.naturalWidth || 800, height: image.naturalHeight || 400};
      const scale = Math.min(PNG_SCALE, PNG_MAX_SIDE / Math.max(size.width, size.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(size.width * scale));
      canvas.height = Math.max(1, Math.round(size.height * scale));
      canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(blob => {
        if (blob) {
          save(blob, file.name.replace(/\.svg$/i, '') + '.png');
          resolve();
        } else {
          reject(new Error('PNG export failed'));
        }
      }, 'image/png');
    };
    image.onerror = () => reject(new Error('The image could not be drawn'));
    image.src = aiImageSrc(file);
  });
}

function svgSize(svg: string): {width: number; height: number} | null {
  const root = new DOMParser().parseFromString(svg, 'image/svg+xml').documentElement;
  if (root?.nodeName.toLowerCase() !== 'svg') {
    return null;
  }
  const width = parseFloat(root.getAttribute('width') ?? '');
  const height = parseFloat(root.getAttribute('height') ?? '');
  if (width > 0 && height > 0 && !/%/.test(root.getAttribute('width') + root.getAttribute('height'))) {
    return {width, height};
  }
  const box = (root.getAttribute('viewBox') ?? '').trim().split(/[\s,]+/).map(Number);
  return box.length === 4 && box[2] > 0 && box[3] > 0 ? {width: box[2], height: box[3]} : null;
}

function save(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url));
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
    markdown: language.label === 'Markdown',
    image: language.label === 'SVG',
    previewed: language.label === 'Markdown' || language.label === 'SVG'
  };
}
