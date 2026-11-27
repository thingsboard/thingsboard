// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const PANELS_ANIMATION_OPTIONS: KeyframeAnimationOptions = {
  duration: 400,
  easing: 'cubic-bezier(0.25, 0.8, 0.25, 1)',
  fill: 'forwards'
};

const PANELS_ANIMATING_CLASS = 'tb-panels-animating';

const SETTINGS_EXTEND_PROPERTY = '--tb-report-settings-extend';

const FLIP_KINDS = ['page', 'move', 'stretch'] as const;

type FlipKind = typeof FLIP_KINDS[number];

const FLIP_SELECTOR = '[data-flip]';

export interface PanelsAnimatorElements {
  container: HTMLElement;
  content: HTMLElement;
  layout: HTMLElement;
  sections: HTMLElement;
  settings: HTMLElement;
  library?: HTMLElement;
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
    .filter(element => (FLIP_KINDS as readonly string[]).includes(element.dataset.flip))
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

  constructor(private render: () => void,
              private relayout: () => void) {
  }

  run(elements: PanelsAnimatorElements | null, change: () => void): void {
    this.finish();
    if (!elements) {
      change();
      return;
    }
    const {container, content: contentEl, layout: layoutEl, sections: sectionsEl, settings: settingsEl, library: libraryEl} = elements;
    const settingsBefore = settingsEl.getBoundingClientRect();
    const libraryTransformBefore = libraryEl ? getComputedStyle(libraryEl).transform : null;
    const targets = collectFlipTargets(sectionsEl);

    change();
    this.render();

    const settingsAfter = settingsEl.getBoundingClientRect();
    if (Math.abs(settingsAfter.left - settingsBefore.left) < 1) {
      return;
    }
    const containerRect = container.getBoundingClientRect();
    const contentAfter = contentEl.getBoundingClientRect();
    const libraryTransformAfter = libraryEl ? getComputedStyle(libraryEl).transform : null;

    container.classList.add(PANELS_ANIMATING_CLASS);
    const animatingColumnLeft = contentEl.getBoundingClientRect().left;
    const columnRight = Math.max(settingsBefore.left, settingsAfter.left);
    const columnStyle = getComputedStyle(contentEl);
    const columnExtraWidth = contentEl.offsetWidth - contentEl.clientWidth +
      parseFloat(columnStyle.paddingLeft) + parseFloat(columnStyle.paddingRight);
    contentEl.style.flex = 'none';
    contentEl.style.width = `${columnRight - animatingColumnLeft}px`;
    layoutEl.style.marginLeft = `${contentAfter.left - animatingColumnLeft}px`;
    layoutEl.style.width = `${contentAfter.width - columnExtraWidth}px`;
    layoutEl.style.overflow = 'visible';
    const settingsExtend = containerRect.right - (settingsBefore.left + settingsAfter.width);
    if (settingsExtend > 0) {
      settingsEl.style.setProperty(SETTINGS_EXTEND_PROPERTY, `${settingsExtend}px`);
    }
    this.relayout();

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
