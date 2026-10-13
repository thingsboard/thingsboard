// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Ace } from 'ace-builds';

export function forceAceFontSize(editor: Ace.Editor, px: number): void {
  const container = editor.container;
  if (container?.style) {
    container.style.setProperty('font-size', `${px}px`, 'important');
  }
  editor.setFontSize(px);
  const renderer: any = editor.renderer;
  if (typeof renderer.updateFontSize === 'function') {
    renderer.updateFontSize();
  }
  if (typeof renderer.onResize === 'function') {
    renderer.onResize(true);
  }
}
