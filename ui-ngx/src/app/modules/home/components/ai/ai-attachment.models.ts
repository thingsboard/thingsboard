// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { HttpErrorResponse } from '@angular/common/http';
import { AI_IMAGE_MEDIA_TYPES, AiResourceFacts } from '@shared/models/ai-chat.models';
import { TranslateService } from '@ngx-translate/core';

const MAX_IMAGE_SIDE = 2560;
const MIN_SVG_SIDE = 1024;
const SVG_MEDIA_TYPE = 'image/svg+xml';
const SVG_UNITS: Record<string, number> = {'': 1, px: 1, pt: 4 / 3, pc: 16, in: 96, cm: 96 / 2.54, mm: 96 / 25.4};

export const AI_SVG_EXTENSION = '.svg';

export interface AiAttachmentKind {
  icon: string;
  label: string;
}

export const aiAttachmentKind = (mediaType: string, name: string): AiAttachmentKind => {
  const extension = name.split('.').pop()?.toLowerCase();
  if (mediaType?.startsWith('image/')) {
    return {icon: 'image', label: 'ai-assistant.file-kind.image'};
  }
  if (mediaType === 'application/pdf' || extension === 'pdf') {
    return {icon: 'picture_as_pdf', label: 'ai-assistant.file-kind.pdf'};
  }
  if (extension === 'xlsx' || extension === 'csv') {
    return {icon: 'table_chart', label: 'ai-assistant.file-kind.table'};
  }
  if (extension === 'docx') {
    return {icon: 'description', label: 'ai-assistant.file-kind.document'};
  }
  if (extension === 'pptx') {
    return {icon: 'slideshow', label: 'ai-assistant.file-kind.slides'};
  }
  if (extension === 'json') {
    return {icon: 'data_object', label: 'ai-assistant.file-kind.json'};
  }
  if (['txt', 'log', 'md', 'xml', 'yaml', 'yml', 'html', 'htm'].includes(extension)) {
    return {icon: 'description', label: 'ai-assistant.file-kind.text'};
  }
  return {icon: 'attach_file', label: 'ai-assistant.file-kind.file'};
};

export const formatAiFileSize = (bytes: number): string => bytes >= 1048576
  ? (bytes / 1048576).toFixed(1).replace(/\.0$/, '') + ' MB'
  : Math.max(1, Math.round(bytes / 1024)) + ' KB';

export const aiAttachmentExtension = (name: string): string => {
  const dot = name.lastIndexOf('.');
  return dot > 0 ? name.substring(dot + 1).toUpperCase() : '';
};

export const isAiSvg = (file: File): boolean => file.type === SVG_MEDIA_TYPE || file.name.toLowerCase().endsWith(AI_SVG_EXTENSION);

export const isAiAttachmentAccepted = (file: File, extensions: string[]): boolean => {
  const name = file.name.toLowerCase();
  return isAiSvg(file) || extensions.some(extension => name.endsWith(extension.toLowerCase()));
};

export const namePastedImage = (file: File): File => {
  if (!AI_IMAGE_MEDIA_TYPES.includes(file.type) || (file.name && file.name !== 'image.png')) {
    return file;
  }
  const stamp = new Date().toISOString().replace(/[-:]/g, '').replace('T', '-').substring(0, 15);
  const extension = file.type === 'image/jpeg' ? 'jpg' : file.type.split('/')[1];
  return new File([file], `screenshot-${stamp}.${extension}`, {type: file.type, lastModified: file.lastModified});
};

const loadImage = (file: File): Promise<HTMLImageElement> => new Promise((resolve, reject) => {
  const url = URL.createObjectURL(file);
  const image = new Image();
  image.onload = () => {
    URL.revokeObjectURL(url);
    resolve(image);
  };
  image.onerror = () => {
    URL.revokeObjectURL(url);
    reject(new Error('Image could not be decoded'));
  };
  image.src = url;
});

const encode = (canvas: HTMLCanvasElement, type: string, quality?: number): Promise<Blob> => new Promise((resolve, reject) =>
  canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('Image could not be encoded')), type, quality));

export const fitAiImage = async (file: File, maxSize: number): Promise<File> => {
  if (!AI_IMAGE_MEDIA_TYPES.includes(file.type)) {
    return file;
  }
  let image: HTMLImageElement;
  try {
    image = await loadImage(file);
  } catch {
    return file;
  }
  const scale = Math.min(1, MAX_IMAGE_SIDE / Math.max(image.naturalWidth, image.naturalHeight));
  if (scale === 1 && file.size <= maxSize) {
    return file;
  }
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
  canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
  const context = canvas.getContext('2d');
  if (file.type === 'image/jpeg') {
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
  }
  context.drawImage(image, 0, 0, canvas.width, canvas.height);
  let blob = await encode(canvas, file.type, 0.92);
  let type = file.type;
  if (blob.size > maxSize) {
    context.globalCompositeOperation = 'destination-over';
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    blob = await encode(canvas, 'image/jpeg', 0.85);
    type = 'image/jpeg';
  }
  if (blob.size >= file.size && scale === 1) {
    return file;
  }
  const base = file.name.replace(/\.[^.]+$/, '');
  const name = type === file.type ? file.name : `${base}.jpg`;
  return new File([blob], name, {type, lastModified: file.lastModified});
};

const svgLength = (value: string | null): number | null => {
  const match = value?.trim().match(/^([0-9]*\.?[0-9]+(?:e[-+]?[0-9]+)?)\s*(px|pt|pc|in|cm|mm)?$/i);
  const length = match ? parseFloat(match[1]) * SVG_UNITS[(match[2] ?? '').toLowerCase()] : NaN;
  return length > 0 ? length : null;
};

export const svgToAiImage = async (file: File, maxSize: number): Promise<File> => {
  const document = new DOMParser().parseFromString(await file.text(), SVG_MEDIA_TYPE);
  const svg = document.documentElement;
  if (svg.localName !== 'svg' || document.getElementsByTagName('parsererror').length) {
    throw new Error('Not an SVG image');
  }
  Array.from(document.getElementsByTagNameNS('*', 'foreignObject')).forEach(node => node.remove());
  const viewBox = (svg.getAttribute('viewBox') ?? '').trim().split(/[\s,]+/).map(Number);
  const hasViewBox = viewBox.length === 4 && viewBox[2] > 0 && viewBox[3] > 0;
  let width = svgLength(svg.getAttribute('width')) ?? (hasViewBox ? viewBox[2] : null);
  let height = svgLength(svg.getAttribute('height')) ?? (hasViewBox ? viewBox[3] : null);
  if (!width || !height) {
    width = width ?? height ?? MIN_SVG_SIDE;
    height = height ?? width;
  }
  if (!hasViewBox) {
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
  }
  const scale = Math.min(MAX_IMAGE_SIDE / Math.max(width, height), Math.max(1, MIN_SVG_SIDE / Math.max(width, height)));
  const canvas = window.document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(width * scale));
  canvas.height = Math.max(1, Math.round(height * scale));
  svg.setAttribute('width', String(canvas.width));
  svg.setAttribute('height', String(canvas.height));
  const image = await loadImage(new File([new XMLSerializer().serializeToString(document)], file.name, {type: SVG_MEDIA_TYPE}));
  const context = canvas.getContext('2d');
  context.fillStyle = '#fff';
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(image, 0, 0, canvas.width, canvas.height);
  let blob = await encode(canvas, 'image/png');
  let type = 'image/png';
  if (blob.size > maxSize) {
    blob = await encode(canvas, 'image/jpeg', 0.85);
    type = 'image/jpeg';
  }
  const base = file.name.replace(/\.[^.]+$/, '');
  return new File([blob], `${base}.${type === 'image/png' ? 'png' : 'jpg'}`, {type, lastModified: file.lastModified});
};

export function aiServerMessage(error: unknown): string | undefined {
  if (!(error instanceof HttpErrorResponse)) {
    return undefined;
  }
  let body: unknown = error.error;
  if (typeof body === 'string') {
    const text = body.trim();
    if (!text || text.startsWith('<')) {
      return undefined;
    }
    try {
      body = JSON.parse(text);
    } catch {
      return text;
    }
  }
  const message = body && typeof body === 'object' ? (body as {message?: unknown}).message : undefined;
  return typeof message === 'string' && message.trim() ? message.trim() : undefined;
}

export function aiResourceFact(facts: AiResourceFacts | undefined, translate: TranslateService): string | null {
  if (!facts) {
    return null;
  }
  if (facts.pages != null) {
    return translate.instant('ai-assistant.fact.pages', {count: facts.pages});
  }
  if (facts.rows != null) {
    return translate.instant('ai-assistant.fact.rows', {count: facts.rows}) + ' · ' + translate.instant('ai-assistant.fact.columns', {count: facts.columns ?? 0});
  }
  if (facts.lines != null) {
    return translate.instant('ai-assistant.fact.lines', {count: facts.lines});
  }
  return null;
}
