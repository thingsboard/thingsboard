// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ReportComponentLayoutService
} from '@home/pages/reporting/template/components/report-component-layout.service';

const PANELS_ANIMATION_OPTIONS: KeyframeAnimationOptions = {
  duration: 400,
  easing: 'cubic-bezier(0.25, 0.8, 0.25, 1)',
  fill: 'forwards'
};

const PANELS_ANIMATING_CLASS = 'tb-panels-animating';

const SETTINGS_EXTEND_PROPERTY = '--tb-report-settings-extend';

type FlipKind = 'page' | 'move' | 'stretch';

const FLIP_SELECTOR = '[data-flip]';

export interface PanelsAnimatorView {
  ready(): boolean;
  container(): HTMLElement;
  content(): HTMLElement;
  layout(): HTMLElement;
  sections(): HTMLElement;
  settings(): HTMLElement;
  library(): HTMLElement;
}

interface FlipTarget {
  element: HTMLElement;
  kind: FlipKind;
  rect: DOMRect;
}

interface PanelsAnimationRun {
  animations: Animation[];
  restore: () => void;
  done: boolean;
}

const collectFlipTargets = (root: HTMLElement): FlipTarget[] =>
  Array.from(root.querySelectorAll<HTMLElement>(FLIP_SELECTOR))
    .filter(element => !root.contains(element.parentElement.closest(FLIP_SELECTOR)))
    .map(element => ({
      element,
      kind: element.dataset.flip as FlipKind,
      rect: element.getBoundingClientRect()
    }))
    .filter(target => target.rect.width > 0);

const flipTransform = (target: FlipTarget): string => {
  const rectAfter = target.element.getBoundingClientRect();
  if (!rectAfter.width) {
    return null;
  }
  let scaleX = 1;
  let scaleY = 1;
  if (target.kind === 'page') {
    scaleX = scaleY = target.rect.width / rectAfter.width;
  } else if (target.kind === 'stretch') {
    scaleX = target.rect.width / rectAfter.width;
    scaleY = target.rect.height / (rectAfter.height || 1);
  }
  const dx = target.rect.left - rectAfter.left;
  const dy = target.rect.top - rectAfter.top;
  if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5 && Math.abs(scaleX - 1) < 0.001 && Math.abs(scaleY - 1) < 0.001) {
    return null;
  }
  return `translate(${dx}px, ${dy}px) scale(${scaleX}, ${scaleY})`;
};

export class PanelsAnimator {

  private current: PanelsAnimationRun;

  constructor(private view: PanelsAnimatorView,
              private layoutService: ReportComponentLayoutService,
              private layoutResize: () => void,
              private detectChanges: () => void) {
  }

  run(change: () => void): void {
    this.finish();
    const container = this.view.container();
    const contentEl = this.view.content();
    const layoutEl = this.view.layout();
    const sectionsEl = this.view.sections();
    const settingsEl = this.view.settings();
    if (!this.view.ready() || !container || !contentEl || !layoutEl || !sectionsEl || !settingsEl) {
      change();
      return;
    }
    const libraryEl = this.view.library();
    const settingsBefore = settingsEl.getBoundingClientRect();
    const libraryTransformBefore = libraryEl ? getComputedStyle(libraryEl).transform : null;
    const targets = collectFlipTargets(sectionsEl);

    change();
    this.detectChanges();

    const settingsAfter = settingsEl.getBoundingClientRect();
    if (Math.abs(settingsAfter.left - settingsBefore.left) < 1) {
      return;
    }
    const containerRect = container.getBoundingClientRect();
    const contentAfter = contentEl.getBoundingClientRect();
    const libraryTransformAfter = libraryEl ? getComputedStyle(libraryEl).transform : null;

    container.classList.add(PANELS_ANIMATING_CLASS);
    const columnLeft = contentEl.getBoundingClientRect().left;
    const columnRight = Math.max(settingsBefore.left, settingsAfter.left);
    const columnStyle = getComputedStyle(contentEl);
    const columnExtraWidth = contentEl.offsetWidth - contentEl.clientWidth +
      parseFloat(columnStyle.paddingLeft) + parseFloat(columnStyle.paddingRight);
    contentEl.style.flex = 'none';
    contentEl.style.width = `${columnRight - columnLeft}px`;
    layoutEl.style.marginLeft = `${contentAfter.left - columnLeft}px`;
    layoutEl.style.width = `${contentAfter.width - columnExtraWidth}px`;
    layoutEl.style.overflow = 'visible';
    const settingsExtend = containerRect.right - (settingsBefore.left + settingsAfter.width);
    if (settingsExtend > 0) {
      settingsEl.style.setProperty(SETTINGS_EXTEND_PROPERTY, `${settingsExtend}px`);
    }
    this.layoutResize();
    this.detectChanges();
    this.layoutService.flush();

    const moves = targets
      .map(target => ({element: target.element, transform: flipTransform(target)}))
      .filter(move => !!move.transform);
    const settingsLayoutLeft = settingsEl.getBoundingClientRect().left;

    const run: PanelsAnimationRun = {
      animations: [],
      done: false,
      restore: () => {
        container.classList.remove(PANELS_ANIMATING_CLASS);
        contentEl.style.removeProperty('flex');
        contentEl.style.removeProperty('width');
        layoutEl.style.removeProperty('margin-left');
        layoutEl.style.removeProperty('width');
        layoutEl.style.removeProperty('overflow');
        settingsEl.style.removeProperty(SETTINGS_EXTEND_PROPERTY);
        moves.forEach(move => move.element.style.removeProperty('transform-origin'));
      }
    };
    run.animations.push(settingsEl.animate([
      {transform: `translateX(${settingsBefore.left - settingsLayoutLeft}px)`},
      {transform: `translateX(${settingsAfter.left - settingsLayoutLeft}px)`}
    ], PANELS_ANIMATION_OPTIONS));
    if (libraryEl && libraryTransformBefore !== libraryTransformAfter) {
      run.animations.push(libraryEl.animate([
        {transform: libraryTransformBefore},
        {transform: libraryTransformAfter}
      ], PANELS_ANIMATION_OPTIONS));
    }
    moves.forEach(move => {
      move.element.style.transformOrigin = '0 0';
      run.animations.push(move.element.animate([{transform: move.transform}, {transform: 'none'}], PANELS_ANIMATION_OPTIONS));
    });
    this.current = run;
    Promise.all(run.animations.map(a => a.finished)).then(() => this.complete(run), () => {});
  }

  finish(): void {
    if (this.current) {
      this.complete(this.current);
    }
  }

  private complete(run: PanelsAnimationRun) {
    if (run.done) {
      return;
    }
    run.done = true;
    run.animations.forEach(a => a.cancel());
    run.restore();
    if (this.current === run) {
      this.current = null;
    }
  }
}
