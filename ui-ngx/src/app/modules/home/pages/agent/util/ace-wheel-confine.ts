// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Ace } from 'ace-builds';

export function confineWheelToAceEditor(
  host: HTMLElement | null | undefined,
  editor: Ace.Editor | null,
  shouldCaptureVertical: () => boolean = () => true,
  capture = true
): void {
  if (!host || !editor) { return; }
  host.addEventListener('wheel', (ev: WheelEvent) => {
    const session = editor.getSession();
    const renderer: any = editor.renderer;
    const lineHeight = renderer.lineHeight || 16;
    let deltaX = ev.deltaX;
    let deltaY = ev.deltaY;
    if (ev.deltaMode === 1) {            // DOM_DELTA_LINE
      deltaX *= lineHeight;
      deltaY *= lineHeight;
    } else if (ev.deltaMode === 2) {     // DOM_DELTA_PAGE
      deltaX *= (renderer.$size?.scrollerWidth || 0);
      deltaY *= (renderer.$size?.scrollerHeight || 0);
    }
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      const maxLeft = Math.max(0,
        (renderer.layerConfig?.width || 0) - (renderer.$size?.scrollerWidth || 0));
      const curLeft = session.getScrollLeft();
      const nextLeft = Math.max(0, Math.min(maxLeft, curLeft + deltaX));
      if (nextLeft !== curLeft) { session.setScrollLeft(nextLeft); }
      ev.preventDefault();
      ev.stopPropagation();
      return;
    }
    if (!shouldCaptureVertical()) { return; }
    const maxTop = Math.max(0,
      (renderer.layerConfig?.maxHeight || 0) - (renderer.$size?.scrollerHeight || 0));
    const curTop = session.getScrollTop();
    const nextTop = Math.max(0, Math.min(maxTop, curTop + deltaY));
    ev.stopPropagation();
    if (nextTop !== curTop) {
      session.setScrollTop(nextTop);
      ev.preventDefault();
    }
  }, { passive: false, capture });
}
