// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';

export interface ReportComponentLayoutTarget {
  element: HTMLElement;
  measureWidth(): number;
  applyWidth(width: number): void;
  measureHeight(): number;
  applyHeight(height: number): void;
}

const REPORT_COMPONENT_SELECTOR = 'tb-report-component';

const nestingDepth = (element: HTMLElement): number => {
  let depth = 0;
  let parent = element.parentElement?.closest(REPORT_COMPONENT_SELECTOR);
  while (parent) {
    depth++;
    parent = parent.parentElement?.closest(REPORT_COMPONENT_SELECTOR);
  }
  return depth;
};

const groupByNestingDepth = (targets: ReportComponentLayoutTarget[]): ReportComponentLayoutTarget[][] => {
  const levels: ReportComponentLayoutTarget[][] = [];
  for (const target of targets) {
    const depth = nestingDepth(target.element);
    (levels[depth] ??= []).push(target);
  }
  return levels.filter(Boolean);
};

@Injectable({ providedIn: 'root' })
export class ReportComponentLayoutService {

  private readonly pendingTargets = new Set<ReportComponentLayoutTarget>();
  private flushQueued = false;

  private resizeObserver: ResizeObserver;
  private readonly observedTargets = new WeakMap<Element, ReportComponentLayoutTarget>();

  observe(target: ReportComponentLayoutTarget): void {
    this.resizeObserver ??= new ResizeObserver(entries => {
      for (const entry of entries) {
        const observedTarget = this.observedTargets.get(entry.target);
        if (observedTarget) {
          this.schedule(observedTarget);
        }
      }
    });
    this.observedTargets.set(target.element, target);
    this.resizeObserver.observe(target.element);
  }

  unobserve(target: ReportComponentLayoutTarget): void {
    this.pendingTargets.delete(target);
    this.observedTargets.delete(target.element);
    this.resizeObserver?.unobserve(target.element);
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
    const levels = groupByNestingDepth(Array.from(this.pendingTargets));
    this.pendingTargets.clear();
    const sizedLevels = levels.map(level => {
      const sized = level
        .map(target => ({target, width: target.measureWidth()}))
        .filter(entry => entry.width > 0);
      sized.forEach(entry => entry.target.applyWidth(entry.width));
      return sized.map(entry => entry.target);
    });
    sizedLevels.reverse().forEach(level => {
      const measured = level.map(target => ({target, height: target.measureHeight()}));
      measured.forEach(entry => entry.target.applyHeight(entry.height));
    });
  }
}
