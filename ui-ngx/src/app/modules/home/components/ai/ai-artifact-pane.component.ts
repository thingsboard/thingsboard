// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, EventEmitter, inject, input, Output, signal, ViewChild } from '@angular/core';

const PANE_MIN = 360;
const CHAT_MIN = 420;
const WIDTH_KEY = 'tbSolutionPaneWidth';

@Component({
  selector: 'tb-ai-artifact-pane',
  templateUrl: './ai-artifact-pane.component.html',
  standalone: false,
  host: {
    class: 'art-pane',
    '[class.scrolled]': 'scrolled()',
    '[class.more]': 'more()',
    '[style.flex-basis]': 'width() ? width() + "px" : null',
    '(keydown.escape)': 'onEscape($event)'
  }
})
export class AiArtifactPaneComponent implements AfterViewInit {

  dark = input(false);

  @Output() closed = new EventEmitter<void>();

  @ViewChild('body', {static: true}) body: ElementRef<HTMLElement>;

  width = signal<number | null>(AiArtifactPaneComponent.storedWidth());
  scrolled = signal(false);
  more = signal(false);

  private host = inject(ElementRef<HTMLElement>);

  ngAfterViewInit(): void {
    setTimeout(() => this.refreshEdges(), 50);
  }

  refreshEdges(): void {
    const body = this.body?.nativeElement;
    if (body) {
      this.scrolled.set(body.scrollTop > 0);
      this.more.set(body.scrollTop + body.clientHeight < body.scrollHeight - 1);
    }
  }

  onEscape(event: Event): void {
    if (event.defaultPrevented) {
      return;
    }
    event.stopPropagation();
    this.closed.emit();
  }

  startResize(event: PointerEvent): void {
    event.preventDefault();
    const edge = event.target as HTMLElement;
    const pane = this.host.nativeElement as HTMLElement;
    const cols = pane.parentElement;
    if (!cols) {
      return;
    }
    const startX = event.clientX;
    const startWidth = pane.getBoundingClientRect().width;
    const max = cols.getBoundingClientRect().width - CHAT_MIN;
    edge.setPointerCapture(event.pointerId);
    edge.classList.add('held');
    let closing = false;
    const move = (e: PointerEvent) => {
      const width = startWidth + (startX - e.clientX);
      closing = width < PANE_MIN - 72;
      pane.classList.toggle('closing', closing);
      this.width.set(Math.round(Math.min(max, Math.max(PANE_MIN, width))));
    };
    const up = () => {
      edge.removeEventListener('pointermove', move);
      edge.removeEventListener('pointerup', up);
      edge.removeEventListener('pointercancel', up);
      edge.classList.remove('held');
      pane.classList.remove('closing');
      if (closing) {
        this.width.set(startWidth);
        this.closed.emit();
      } else {
        this.storeWidth();
      }
    };
    edge.addEventListener('pointermove', move);
    edge.addEventListener('pointerup', up);
    edge.addEventListener('pointercancel', up);
  }

  resetWidth(): void {
    this.width.set(null);
    this.storeWidth();
  }

  onEdgeKey(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.resetWidth();
      return;
    }
    const pane = this.host.nativeElement as HTMLElement;
    const cols = pane.parentElement;
    if (!cols) {
      return;
    }
    const max = cols.getBoundingClientRect().width - CHAT_MIN;
    let width: number;
    if (event.key === 'Home') {
      width = max;
    } else if (event.key === 'End') {
      width = PANE_MIN;
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      const step = event.shiftKey ? 64 : 16;
      width = pane.getBoundingClientRect().width + (event.key === 'ArrowLeft' ? step : -step);
    } else {
      return;
    }
    event.preventDefault();
    this.width.set(Math.round(Math.min(max, Math.max(PANE_MIN, width))));
    this.storeWidth();
  }

  private static storedWidth(): number | null {
    try {
      return Number(localStorage.getItem(WIDTH_KEY)) || null;
    } catch {
      return null;
    }
  }

  private storeWidth(): void {
    try {
      const width = this.width();
      if (width) {
        localStorage.setItem(WIDTH_KEY, String(width));
      } else {
        localStorage.removeItem(WIDTH_KEY);
      }
    } catch {
      return;
    }
  }

}
