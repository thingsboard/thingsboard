// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Observable } from 'rxjs';
import { AiResourceInfo } from '@shared/models/ai-chat.models';

export interface AiAttachmentItem {
  key: string;
  name: string;
  mediaType: string;
  size: number;
  image: boolean;
  url$?: Observable<string>;
  resource?: AiResourceInfo;
  status?: 'uploading' | 'ready' | 'error';
  progress?: number;
}

const MAX_IMAGE_SIDE = 2560;
const RESIZABLE_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp'];

export const aiAttachmentIcon = (mediaType: string, name: string): string => {
  const extension = name.split('.').pop()?.toLowerCase();
  if (mediaType === 'application/pdf' || extension === 'pdf') {
    return 'picture_as_pdf';
  }
  if (extension === 'xlsx' || extension === 'csv') {
    return 'table_chart';
  }
  if (extension === 'pptx') {
    return 'slideshow';
  }
  if (['json', 'xml', 'yaml', 'yml', 'html', 'htm'].includes(extension)) {
    return 'code';
  }
  return 'description';
};

export const aiAttachmentExtension = (name: string): string => {
  const dot = name.lastIndexOf('.');
  return dot > 0 ? name.substring(dot + 1).toUpperCase() : '';
};

export const isAiAttachmentAccepted = (file: File, extensions: string[]): boolean => {
  const name = file.name.toLowerCase();
  return extensions.some(extension => name.endsWith(extension.toLowerCase()));
};

export const namePastedImage = (file: File): File => {
  if (!RESIZABLE_IMAGE_TYPES.includes(file.type) || (file.name && file.name !== 'image.png')) {
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
  if (!RESIZABLE_IMAGE_TYPES.includes(file.type)) {
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
