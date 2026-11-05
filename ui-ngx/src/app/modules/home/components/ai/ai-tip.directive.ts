// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Directive, ElementRef, HostListener, inject, Input, OnDestroy, OnInit, Renderer2, TemplateRef } from '@angular/core';
import { Subscription } from 'rxjs';
import { TbPopoverDirective } from '@shared/components/popover.component';

const SHOW_DELAY = 0.35;
const HIDE_DELAY = 0.08;
const TIP_ID = 'tb-ai-tip';

const openTip: { current: AiTipDirective | null } = {current: null};

@Directive({
  selector: '[tbAiTip]',
  standalone: false
})
export class AiTipDirective implements OnInit, OnDestroy {

  private popover = inject(TbPopoverDirective, {self: true});
  private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private renderer = inject(Renderer2);

  @Input('tbAiTip') template: TemplateRef<void>;

  @Input() tipToggle = false;

  private pointerType: string;
  private visibleSubscription: Subscription;
  private unlistenKeydown: () => void;

  private get trigger(): HTMLElement {
    return this.elementRef.nativeElement;
  }

  ngOnInit(): void {
    this.popover.content = this.template;
    this.popover.trigger = 'hover';
    this.popover.placement = ['top', 'topRight', 'topLeft', 'bottom', 'bottomRight', 'bottomLeft'];
    this.popover.showCloseButton = false;
    this.popover.mouseEnterDelay = SHOW_DELAY;
    this.popover.mouseLeaveDelay = HIDE_DELAY;
    this.popover.overlayClassName = 'tb-ai-tip';
    this.visibleSubscription = this.popover.visibleChange.subscribe(visible => visible ? this.onShown() : this.onHidden());
  }

  ngOnDestroy(): void {
    this.visibleSubscription?.unsubscribe();
    this.onHidden();
  }

  @HostListener('pointerenter', ['$event'])
  onPointerEnter(event: PointerEvent): void {
    this.pointerType = event.pointerType;
    const other = openTip.current !== this ? openTip.current : null;
    this.popover.mouseEnterDelay = other ? 0 : SHOW_DELAY;
    other?.popover.hide();
  }

  @HostListener('mouseenter', ['$event'])
  onMouseEnter(event: MouseEvent): void {
    if (this.pointerType === 'touch') {
      event.stopImmediatePropagation();
    }
  }

  @HostListener('focus')
  onFocus(): void {
    if (this.trigger.matches(':focus-visible')) {
      this.popover.show();
    }
  }

  @HostListener('blur')
  onBlur(): void {
    this.popover.hide();
  }

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    if (!this.tipToggle || this.popover.component?.visible) {
      this.popover.hide();
      return;
    }
    event.preventDefault();
    this.popover.show();
  }

  private onShown(): void {
    if (openTip.current !== this) {
      openTip.current?.popover.hide();
    }
    openTip.current = this;
    this.unlistenKeydown?.();
    this.unlistenKeydown = this.renderer.listen('document', 'keydown', (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        this.popover.hide();
      }
    });
    Promise.resolve().then(() => {
      const pane = this.popover.component?.overlay?.overlayRef?.overlayElement;
      const tip = pane?.querySelector('.tb-popover-inner');
      if (openTip.current === this && tip) {
        this.renderer.setStyle(pane, 'pointer-events', 'none');
        tip.id = TIP_ID;
        this.trigger.setAttribute('aria-describedby', TIP_ID);
      }
    });
  }

  private onHidden(): void {
    if (openTip.current === this) {
      openTip.current = null;
    }
    this.unlistenKeydown?.();
    this.unlistenKeydown = null;
    this.trigger.removeAttribute('aria-describedby');
  }
}
