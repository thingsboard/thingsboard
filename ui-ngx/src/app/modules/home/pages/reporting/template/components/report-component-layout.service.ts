// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, OnDestroy } from '@angular/core';

export interface ReportComponentLayoutTarget {
  measureWidth(): number;
  applyWidth(width: number): void;
  measureHeight(): number;
  applyHeight(height: number): void;
}

@Injectable({ providedIn: 'root' })
export class ReportComponentLayoutService implements OnDestroy {

  private readonly pendingTargets = new Set<ReportComponentLayoutTarget>();
  private flushQueued = false;

  private resizeObserver: ResizeObserver;
  private readonly observedTargets = new WeakMap<Element, ReportComponentLayoutTarget>();

  observe(target: ReportComponentLayoutTarget, element: HTMLElement): void {
    if (!this.resizeObserver) {
      this.resizeObserver = new ResizeObserver(entries => {
        for (const entry of entries) {
          const observedTarget = this.observedTargets.get(entry.target);
          if (observedTarget) {
            this.schedule(observedTarget);
          }
        }
      });
    }
    this.observedTargets.set(element, target);
    this.resizeObserver.observe(element);
  }

  unobserve(target: ReportComponentLayoutTarget, element: HTMLElement): void {
    this.pendingTargets.delete(target);
    if (this.resizeObserver && this.observedTargets.get(element) === target) {
      this.observedTargets.delete(element);
      this.resizeObserver.unobserve(element);
    }
  }

  schedule(target: ReportComponentLayoutTarget): void {
    this.pendingTargets.add(target);
    if (!this.flushQueued) {
      this.flushQueued = true;
      queueMicrotask(() => {
        this.flushQueued = false;
        this.flush();
      });
    }
  }

  flush(): void {
    if (!this.pendingTargets.size) {
      return;
    }
    const targets = Array.from(this.pendingTargets);
    this.pendingTargets.clear();
    const widths = targets.map(target => target.measureWidth());
    const sizedTargets = targets.filter((_target, i) => widths[i] > 0);
    const sizedWidths = widths.filter(width => width > 0);
    sizedTargets.forEach((target, i) => target.applyWidth(sizedWidths[i]));
    const heights = sizedTargets.map(target => target.measureHeight());
    sizedTargets.forEach((target, i) => target.applyHeight(heights[i]));
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.pendingTargets.clear();
  }
}
