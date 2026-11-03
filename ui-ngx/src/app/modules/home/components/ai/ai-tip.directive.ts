// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Directive,
  ElementRef,
  EmbeddedViewRef,
  HostListener,
  inject,
  Injector,
  Input,
  NgZone,
  OnDestroy,
  TemplateRef
} from '@angular/core';

const SHOW_DELAY = 350;
const HIDE_DELAY = 80;
const MARGIN = 8;
const GAP = 10;
const TIP_ID = 'tb-ai-tip';

// One floating tip for the composer's controls: it lives on the page, so a scrolling log or a panel's edge never clips it.
// A pointer opens it after a short pause and moving to the next control swaps it at once; keyboard focus opens it at once;
// blur, Esc, a scroll or a resize close it. It sits above its control, kept 8px inside the window, below when there is no room.
class AiTipHost {

  private element: HTMLElement;
  private owner: AiTipDirective;
  private view: EmbeddedViewRef<unknown>;
  private showTimer: ReturnType<typeof setTimeout>;
  private hideTimer: ReturnType<typeof setTimeout>;
  private frame = 0;
  private listening = false;

  get open(): boolean {
    return !!this.owner;
  }

  isOwner(tip: AiTipDirective): boolean {
    return this.owner === tip;
  }

  scheduleShow(tip: AiTipDirective): void {
    this.clearTimers();
    this.showTimer = setTimeout(() => this.show(tip), this.owner ? 0 : SHOW_DELAY);
  }

  scheduleHide(tip: AiTipDirective): void {
    clearTimeout(this.showTimer);
    this.hideTimer = setTimeout(() => {
      if (this.owner === tip) {
        this.hide();
      }
    }, HIDE_DELAY);
  }

  show(tip: AiTipDirective): void {
    this.clearTimers();
    this.listen();
    const element = this.ensure();
    if (this.owner && this.owner !== tip) {
      this.owner.trigger.removeAttribute('aria-describedby');
    }
    this.view?.destroy();
    this.view = tip.render();
    element.className = 'tb-ai-tip ' + (tip.tipClass || '');
    element.replaceChildren(...this.view.rootNodes);
    this.owner = tip;
    tip.trigger.setAttribute('aria-describedby', TIP_ID);
    cancelAnimationFrame(this.frame);
    this.place();
    element.classList.add('on');
  }

  hide(): void {
    clearTimeout(this.showTimer);
    cancelAnimationFrame(this.frame);
    if (!this.owner) {
      return;
    }
    this.owner.trigger.removeAttribute('aria-describedby');
    this.owner = null;
    this.element?.classList.remove('on');
    this.view?.destroy();
    this.view = null;
  }

  private place(): void {
    if (!this.owner) {
      return;
    }
    const trigger = this.owner.trigger;
    if (!trigger.isConnected) {
      this.hide();
      return;
    }
    this.view?.detectChanges();
    const target = trigger.getBoundingClientRect();
    const tip = this.element.getBoundingClientRect();
    const center = target.left + target.width / 2;
    const left = Math.round(Math.min(Math.max(MARGIN, center - tip.width / 2), innerWidth - tip.width - MARGIN));
    const below = target.top - tip.height - GAP < MARGIN;
    const top = Math.round(below ? target.bottom + GAP : target.top - tip.height - GAP);
    this.element.style.left = left + 'px';
    this.element.style.top = top + 'px';
    this.element.classList.toggle('below', below);
    this.element.style.setProperty('--arrow-x', Math.round(Math.min(Math.max(12, center - left), tip.width - 12)) + 'px');
    this.frame = requestAnimationFrame(() => this.place());
  }

  private ensure(): HTMLElement {
    if (!this.element) {
      this.element = document.createElement('div');
      this.element.id = TIP_ID;
      this.element.setAttribute('role', 'tooltip');
      document.body.appendChild(this.element);
    }
    return this.element;
  }

  private listen(): void {
    if (this.listening) {
      return;
    }
    this.listening = true;
    addEventListener('scroll', () => this.hide(), true);
    addEventListener('resize', () => this.hide());
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && this.owner) {
        this.hide();
      }
    });
  }

  private clearTimers(): void {
    clearTimeout(this.showTimer);
    clearTimeout(this.hideTimer);
  }
}

const tipHost = new AiTipHost();

@Directive({
  selector: '[tbAiTip]',
  standalone: false
})
export class AiTipDirective implements OnDestroy {

  private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private injector = inject(Injector);
  private zone = inject(NgZone);

  @Input('tbAiTip') template: TemplateRef<unknown>;

  @Input() tipClass: string;

  @Input() tipToggle = false;

  get trigger(): HTMLElement {
    return this.elementRef.nativeElement;
  }

  render(): EmbeddedViewRef<unknown> {
    const view = this.template.createEmbeddedView(null, this.injector);
    view.detectChanges();
    return view;
  }

  @HostListener('pointerenter', ['$event'])
  onPointerEnter(event: PointerEvent): void {
    if (event.pointerType !== 'touch') {
      this.zone.runOutsideAngular(() => tipHost.scheduleShow(this));
    }
  }

  @HostListener('pointerleave', ['$event'])
  onPointerLeave(event: PointerEvent): void {
    if (event.pointerType !== 'touch') {
      tipHost.scheduleHide(this);
    }
  }

  @HostListener('focus')
  onFocus(): void {
    if (this.trigger.matches(':focus-visible')) {
      this.zone.runOutsideAngular(() => tipHost.show(this));
    }
  }

  @HostListener('blur')
  onBlur(): void {
    if (tipHost.isOwner(this)) {
      tipHost.hide();
    }
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    if (!this.tipToggle) {
      tipHost.hide();
      return;
    }
    event.preventDefault();
    if (tipHost.isOwner(this)) {
      tipHost.hide();
    } else {
      this.zone.runOutsideAngular(() => tipHost.show(this));
    }
  }

  ngOnDestroy(): void {
    if (tipHost.isOwner(this)) {
      tipHost.hide();
    }
  }
}
