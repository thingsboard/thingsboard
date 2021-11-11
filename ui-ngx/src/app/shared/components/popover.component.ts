///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactory,
  ComponentFactoryResolver,
  ComponentRef,
  Directive,
  ElementRef,
  EventEmitter,
  Injector,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Optional,
  Output,
  Renderer2,
  SimpleChanges,
  TemplateRef,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { Direction, Directionality } from '@angular/cdk/bidi';
import {
  CdkConnectedOverlay,
  CdkOverlayOrigin,
  ConnectedOverlayPositionChange,
  ConnectionPositionPair
} from '@angular/cdk/overlay';
import { Subject, Subscription } from 'rxjs';
import {
  DEFAULT_POPOVER_POSITIONS,
  getPlacementName,
  popoverMotion,
  PopoverPlacement,
  POSITION_MAP,
  PropertyMapping
} from '@shared/components/popover.models';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { isNotEmptyStr, onParentScrollOrWindowResize } from '@core/utils';

export type TbPopoverTrigger = 'click' | 'focus' | 'hover' | null;

@Directive({
  selector: '[tb-popover]',
  exportAs: 'tbPopover',
  host: {
    '[class.tb-popover-open]': 'visible'
  }
})
export class TbPopoverDirective implements OnChanges, OnDestroy, AfterViewInit {

  // tslint:disable:no-input-rename
  @Input('tbPopoverContent') content?: string | TemplateRef<void>;
  @Input('tbPopoverTrigger') trigger?: TbPopoverTrigger = 'hover';
  @Input('tbPopoverPlacement') placement?: string | string[] = 'top';
  @Input('tbPopoverOrigin') origin?: ElementRef<HTMLElement>;
  @Input('tbPopoverVisible') visible?: boolean;
  @Input('tbPopoverMouseEnterDelay') mouseEnterDelay?: number;
  @Input('tbPopoverMouseLeaveDelay') mouseLeaveDelay?: number;
  @Input('tbPopoverOverlayClassName') overlayClassName?: string;
  @Input('tbPopoverOverlayStyle') overlayStyle?: { [klass: string]: any };
  @Input() tbPopoverBackdrop = false;

  // tslint:disable-next-line:no-output-rename
  @Output('tbPopoverVisibleChange') readonly visibleChange = new EventEmitter<boolean>();

  componentFactory: ComponentFactory<TbPopoverComponent> = this.resolver.resolveComponentFactory(TbPopoverComponent);
  component?: TbPopoverComponent;

  private readonly destroy$ = new Subject<void>();
  private readonly triggerDisposables: Array<() => void> = [];
  private delayTimer?;
  private internalVisible = false;

  constructor(
    private elementRef: ElementRef,
    private hostView: ViewContainerRef,
    private resolver: ComponentFactoryResolver,
    private renderer: Renderer2
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    const { trigger } = changes;

    if (trigger && !trigger.isFirstChange()) {
      this.registerTriggers();
    }

    if (this.component) {
      this.updatePropertiesByChanges(changes);
    }
  }

  ngAfterViewInit(): void {
    this.registerTriggers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.clearTogglingTimer();
    this.removeTriggerListeners();
  }

  show(): void {
    if (!this.component) {
      this.createComponent();
    }
    this.component?.show();
  }

  hide(): void {
    this.component?.hide();
  }

  updatePosition(): void {
    if (this.component) {
      this.component.updatePosition();
    }
  }

  private createComponent(): void {
    const componentRef = this.hostView.createComponent(this.componentFactory);

    this.component = componentRef.instance;

    this.renderer.removeChild(
      this.renderer.parentNode(this.elementRef.nativeElement),
      componentRef.location.nativeElement
    );
    this.component.setOverlayOrigin({ elementRef: this.origin || this.elementRef });

    this.initProperties();

    this.component.tbVisibleChange
      .pipe(distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((visible: boolean) => {
        this.internalVisible = visible;
        this.visibleChange.emit(visible);
      });
  }

  private registerTriggers(): void {
    // When the method gets invoked, all properties has been synced to the dynamic component.
    // After removing the old API, we can just check the directive's own `nzTrigger`.
    const el = this.elementRef.nativeElement;
    const trigger = this.trigger;

    this.removeTriggerListeners();

    if (trigger === 'hover') {
      let overlayElement: HTMLElement;
      this.triggerDisposables.push(
        this.renderer.listen(el, 'mouseenter', () => {
          this.delayEnterLeave(true, true, this.mouseEnterDelay);
        })
      );
      this.triggerDisposables.push(
        this.renderer.listen(el, 'mouseleave', () => {
          this.delayEnterLeave(true, false, this.mouseLeaveDelay);
          if (this.component?.overlay.overlayRef && !overlayElement) {
            overlayElement = this.component.overlay.overlayRef.overlayElement;
            this.triggerDisposables.push(
              this.renderer.listen(overlayElement, 'mouseenter', () => {
                this.delayEnterLeave(false, true, this.mouseEnterDelay);
              })
            );
            this.triggerDisposables.push(
              this.renderer.listen(overlayElement, 'mouseleave', () => {
                this.delayEnterLeave(false, false, this.mouseLeaveDelay);
              })
            );
          }
        })
      );
    } else if (trigger === 'focus') {
      this.triggerDisposables.push(this.renderer.listen(el, 'focusin', () => this.show()));
      this.triggerDisposables.push(this.renderer.listen(el, 'focusout', () => this.hide()));
    } else if (trigger === 'click') {
      this.triggerDisposables.push(
        this.renderer.listen(el, 'click', (e: MouseEvent) => {
          e.preventDefault();
          if (this.component?.visible) {
            this.hide();
          } else {
            this.show();
          }
        })
      );
    }
    // Else do nothing because user wants to control the visibility programmatically.
  }

  private updatePropertiesByChanges(changes: SimpleChanges): void {
    this.updatePropertiesByKeys(Object.keys(changes));
  }

  private updatePropertiesByKeys(keys?: string[]): void {
    const mappingProperties: PropertyMapping = {
      // common mappings
      content: ['tbContent', () => this.content],
      trigger: ['tbTrigger', () => this.trigger],
      placement: ['tbPlacement', () => this.placement],
      visible: ['tbVisible', () => this.visible],
      mouseEnterDelay: ['tbMouseEnterDelay', () => this.mouseEnterDelay],
      mouseLeaveDelay: ['tbMouseLeaveDelay', () => this.mouseLeaveDelay],
      overlayClassName: ['tbOverlayClassName', () => this.overlayClassName],
      overlayStyle: ['tbOverlayStyle', () => this.overlayStyle],
      tbPopoverBackdrop: ['tbBackdrop', () => this.tbPopoverBackdrop]
    };

    (keys || Object.keys(mappingProperties).filter(key => !key.startsWith('directive'))).forEach(
      (property: any) => {
        if (mappingProperties[property]) {
          const [name, valueFn] = mappingProperties[property];
          this.updateComponentValue(name, valueFn());
        }
      }
    );

    this.component?.updateByDirective();
  }


  private initProperties(): void {
    this.updatePropertiesByKeys();
  }

  private updateComponentValue(key: string, value: any): void {
    if (typeof value !== 'undefined') {
      // @ts-ignore
      this.component[key] = value;
    }
  }

  private delayEnterLeave(isOrigin: boolean, isEnter: boolean, delay: number = -1): void {
    if (this.delayTimer) {
      this.clearTogglingTimer();
    } else if (delay > 0) {
      this.delayTimer = setTimeout(() => {
        this.delayTimer = undefined;
        isEnter ? this.show() : this.hide();
      }, delay * 1000);
    } else {
      // `isOrigin` is used due to the tooltip will not hide immediately
      // (may caused by the fade-out animation).
      isEnter && isOrigin ? this.show() : this.hide();
    }
  }

  private removeTriggerListeners(): void {
    this.triggerDisposables.forEach(dispose => dispose());
    this.triggerDisposables.length = 0;
  }

  private clearTogglingTimer(): void {
    if (this.delayTimer) {
      clearTimeout(this.delayTimer);
      this.delayTimer = undefined;
    }
  }
}

@Component({
  selector: 'tb-popover',
  exportAs: 'tbPopoverComponent',
  animations: [popoverMotion],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  styleUrls: ['./popover.component.scss'],
  template: `
    <ng-template
      #overlay="cdkConnectedOverlay"
      cdkConnectedOverlay
      [cdkConnectedOverlayHasBackdrop]="hasBackdrop"
      [cdkConnectedOverlayOrigin]="origin"
      [cdkConnectedOverlayPositions]="positions"
      [cdkConnectedOverlayOpen]="visible"
      [cdkConnectedOverlayPush]="true"
      (overlayOutsideClick)="onClickOutside($event)"
      (detach)="hide()"
      (positionChange)="onPositionChange($event)"
    >
      <div #popoverRoot [@popoverMotion]="tbAnimationState"
           (@popoverMotion.done)="animationDone()">
        <div
          class="tb-popover"
          [class.tb-popover-rtl]="dir === 'rtl'"
          [ngClass]="classMap"
          [ngStyle]="tbOverlayStyle"
        >
          <div class="tb-popover-content">
            <div class="tb-popover-arrow">
              <span class="tb-popover-arrow-content"></span>
            </div>
            <div class="tb-popover-inner" [ngStyle]="tbPopoverInnerStyle" role="tooltip">
              <div class="tb-popover-close-button" (click)="closeButtonClick($event)">×</div>
              <div style="width: 100%; height: 100%;">
                <div class="tb-popover-inner-content">
                  <ng-container *ngIf="tbContent">
                    <ng-container *tbStringTemplateOutlet="tbContent">{{ tbContent }}</ng-container>
                  </ng-container>
                  <ng-container *ngIf="tbComponentFactory"
                                [tbComponentOutlet]="tbComponentFactory"
                                [tbComponentInjector]="tbComponentInjector"
                                [tbComponentOutletContext]="tbComponentContext"
                                (componentChange)="onComponentChange($event)"
                                [tbComponentStyle]="tbComponentStyle">
                  </ng-container>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ng-template>
  `
})
export class TbPopoverComponent implements OnDestroy, OnInit {

  @ViewChild('overlay', { static: false }) overlay!: CdkConnectedOverlay;
  @ViewChild('popoverRoot', { static: false }) popoverRoot!: ElementRef<HTMLElement>;

  tbContent: string | TemplateRef<void> | null = null;
  tbComponentFactory: ComponentFactory<any> | null = null;
  tbComponentRef: ComponentRef<any> | null = null;
  tbComponentContext: any;
  tbComponentInjector: Injector | null = null;
  tbComponentStyle: { [klass: string]: any }  = {};
  tbOverlayClassName!: string;
  tbOverlayStyle: { [klass: string]: any } = {};
  tbPopoverInnerStyle: { [klass: string]: any } = {};
  tbBackdrop = false;
  tbMouseEnterDelay?: number;
  tbMouseLeaveDelay?: number;
  tbHideOnClickOutside = true;

  tbAnimationState = 'active';

  tbVisibleChange = new Subject<boolean>();
  tbAnimationDone = new Subject<void>();
  tbComponentChange = new Subject<ComponentRef<any>>();
  tbDestroy = new Subject<void>();

  set tbVisible(value: boolean) {
    const visible = value;
    if (this.visible !== visible) {
      this.visible = visible;
      this.tbVisibleChange.next(visible);
    }
  }

  get tbVisible(): boolean {
    return this.visible;
  }

  visible = false;

  set tbHidden(value: boolean) {
    const hidden = value;
    if (this.hidden !== hidden) {
      this.hidden = hidden;
      if (this.hidden) {
        this.renderer.setStyle(this.popoverRoot.nativeElement, 'width', this.popoverRoot.nativeElement.offsetWidth + 'px');
        this.renderer.setStyle(this.popoverRoot.nativeElement, 'height', this.popoverRoot.nativeElement.offsetHeight + 'px');
      } else {
        setTimeout(() => {
          this.renderer.removeStyle(this.popoverRoot.nativeElement, 'width');
          this.renderer.removeStyle(this.popoverRoot.nativeElement, 'height');
        });
      }
      this.updateStyles();
      this.cdr.markForCheck();
    }
  }

  get tbHidden(): boolean {
    return this.hidden;
  }

  hidden = false;
  lastIsIntersecting = true;

  set tbTrigger(value: TbPopoverTrigger) {
    this.trigger = value;
  }

  get tbTrigger(): TbPopoverTrigger {
    return this.trigger;
  }

  protected trigger: TbPopoverTrigger = 'hover';

  set tbPlacement(value: PopoverPlacement | PopoverPlacement[]) {
    if (typeof value === 'string') {
      this.positions = [POSITION_MAP[value], ...DEFAULT_POPOVER_POSITIONS];
    } else {
      const preferredPosition = value.map(placement => POSITION_MAP[placement]);
      this.positions = [...preferredPosition, ...DEFAULT_POPOVER_POSITIONS];
    }
  }

  get hasBackdrop(): boolean {
    return this.tbTrigger === 'click' ? this.tbBackdrop : false;
  }

  preferredPlacement: PopoverPlacement = 'top';
  origin!: CdkOverlayOrigin;
  public dir: Direction = 'ltr';
  classMap: { [klass: string]: any } = {};
  positions: ConnectionPositionPair[] = [...DEFAULT_POPOVER_POSITIONS];
  private parentScrollSubscription: Subscription = null;
  private intersectionObserver = new IntersectionObserver((entries) => {
    if (this.lastIsIntersecting !== entries[0].isIntersecting) {
      this.lastIsIntersecting = entries[0].isIntersecting;
      this.updateStyles();
      this.cdr.markForCheck();
    }
  }, {threshold: [0.5]});

  constructor(
    public cdr: ChangeDetectorRef,
    private renderer: Renderer2,
    @Optional() private directionality: Directionality
  ) {}

  ngOnInit(): void {
    this.directionality.change?.pipe(takeUntil(this.tbDestroy)).subscribe((direction: Direction) => {
      this.dir = direction;
      this.cdr.detectChanges();
    });

    this.dir = this.directionality.value;
  }

  ngOnDestroy(): void {
    if (this.parentScrollSubscription) {
      this.parentScrollSubscription.unsubscribe();
      this.parentScrollSubscription = null;
    }
    if (this.origin) {
      const el = this.origin.elementRef.nativeElement;
      this.intersectionObserver.unobserve(el);
    }
    this.intersectionObserver.disconnect();
    this.intersectionObserver = null;
    this.tbVisibleChange.complete();
    this.tbAnimationDone.complete();
    this.tbDestroy.next();
    this.tbDestroy.complete();
  }

  closeButtonClick($event: Event) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    this.hide();
  }

  show(): void {
    if (this.tbVisible) {
      return;
    }

    if (!this.isEmpty()) {
      this.tbVisible = true;
      this.tbVisibleChange.next(true);
      this.cdr.detectChanges();
    }

    if (this.origin && this.overlay && this.overlay.overlayRef) {
      if (this.overlay.overlayRef.getDirection() === 'rtl') {
        this.overlay.overlayRef.setDirection('ltr');
      }
      const el = this.origin.elementRef.nativeElement;
      this.parentScrollSubscription = onParentScrollOrWindowResize(el).subscribe(() => {
        this.overlay.overlayRef.updatePosition();
      });
      this.intersectionObserver.observe(el);
    }
  }

  hide(): void {
    if (!this.tbVisible) {
      return;
    }
    if (this.parentScrollSubscription) {
      this.parentScrollSubscription.unsubscribe();
      this.parentScrollSubscription = null;
    }
    if (this.origin) {
      const el = this.origin.elementRef.nativeElement;
      this.intersectionObserver.unobserve(el);
    }

    this.tbVisible = false;
    this.tbVisibleChange.next(false);
    this.cdr.detectChanges();
  }

  updateByDirective(): void {
    this.updateStyles();
    this.cdr.detectChanges();

    Promise.resolve().then(() => {
      this.updatePosition();
      this.updateVisibilityByContent();
    });
  }

  updatePosition(): void {
    if (this.origin && this.overlay && this.overlay.overlayRef) {
      this.overlay.overlayRef.updatePosition();
    }
  }

  onPositionChange(position: ConnectedOverlayPositionChange): void {
    this.preferredPlacement = getPlacementName(position);
    this.updateStyles();
    this.cdr.detectChanges();
  }

  updateStyles(): void {
    this.classMap = {
      [`tb-popover-placement-${this.preferredPlacement}`]: true,
      ['tb-popover-hidden']: this.tbHidden || !this.lastIsIntersecting
    };
    if (this.tbOverlayClassName) {
      this.classMap[this.tbOverlayClassName] = true;
    }
  }

  setOverlayOrigin(origin: CdkOverlayOrigin): void {
    this.origin = origin;
    this.cdr.markForCheck();
  }

  onClickOutside(event: MouseEvent): void {
    if (this.tbHideOnClickOutside && !this.origin.elementRef.nativeElement.contains(event.target) && this.tbTrigger !== null) {
      this.hide();
    }
  }

  onComponentChange(component: ComponentRef<any>) {
    this.tbComponentRef = component;
    this.tbComponentChange.next(component);
  }

  animationDone() {
    this.tbAnimationDone.next();
  }

  private updateVisibilityByContent(): void {
    if (this.isEmpty()) {
      this.hide();
    }
  }

  private isEmpty(): boolean {
    return (this.tbComponentFactory instanceof ComponentFactory || this.tbContent instanceof TemplateRef)
      ? false : !isNotEmptyStr(this.tbContent);
  }
}
