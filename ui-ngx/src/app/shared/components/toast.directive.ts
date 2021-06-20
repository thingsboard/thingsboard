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
  AfterViewInit, ChangeDetectorRef,
  Component, ComponentFactoryResolver, ComponentRef,
  Directive,
  ElementRef, HostBinding,
  Inject,
  Injector,
  Input,
  NgZone,
  OnDestroy, Optional,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBar, MatSnackBarConfig, MatSnackBarRef } from '@angular/material/snack-bar';
import { NotificationMessage } from '@app/core/notification/notification.models';
import { Subscription } from 'rxjs';
import { NotificationService } from '@app/core/services/notification.service';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { MatButton } from '@angular/material/button';
import Timeout = NodeJS.Timeout;

@Directive({
  selector: '[tb-toast]'
})
export class ToastDirective implements AfterViewInit, OnDestroy {

  @Input()
  toastTarget = 'root';

  private notificationSubscription: Subscription = null;
  private hideNotificationSubscription: Subscription = null;

  private snackBarRef: MatSnackBarRef<any> = null;
  private toastComponentRef: ComponentRef<TbSnackBarComponent>;
  private currentMessage: NotificationMessage = null;

  private dismissTimeout: Timeout = null;

  constructor(private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private notificationService: NotificationService,
              private componentFactoryResolver: ComponentFactoryResolver,
              private snackBar: MatSnackBar,
              private ngZone: NgZone,
              private breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef) {
  }

  ngAfterViewInit(): void {
    this.notificationSubscription = this.notificationService.getNotification().subscribe(
      (notificationMessage) => {
        if (this.shouldDisplayMessage(notificationMessage)) {
          this.currentMessage = notificationMessage;
          const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
          if (isGtSm && this.toastTarget !== 'root') {
            this.showToastPanel(notificationMessage);
          } else {
            this.showSnackBar(notificationMessage, isGtSm);
          }
        }
      }
    );

    this.hideNotificationSubscription = this.notificationService.getHideNotification().subscribe(
      (hideNotification) => {
        if (hideNotification) {
          const target = hideNotification.target || 'root';
          if (this.toastTarget === target) {
            this.ngZone.run(() => {
              if (this.snackBarRef) {
                this.snackBarRef.dismiss();
              }
              if (this.toastComponentRef) {
                this.toastComponentRef.instance.actionButton._elementRef.nativeElement.click();
              }
            });
          }
        }
      }
    );
  }

  private showToastPanel(notificationMessage: NotificationMessage) {
    this.ngZone.run(() => {
      if (this.snackBarRef) {
        this.snackBarRef.dismiss();
      }
      if (this.toastComponentRef) {
        this.viewContainerRef.detach(0);
        this.toastComponentRef.destroy();
      }
      let panelClass = ['tb-toast-panel', 'toast-panel'];
      if (notificationMessage.panelClass) {
        if (typeof notificationMessage.panelClass === 'string') {
          panelClass.push(notificationMessage.panelClass);
        } else if (notificationMessage.panelClass.length) {
          panelClass = panelClass.concat(notificationMessage.panelClass);
        }
      }
      const horizontalPosition = notificationMessage.horizontalPosition || 'left';
      const verticalPosition = notificationMessage.verticalPosition || 'top';
      if (horizontalPosition === 'start' || horizontalPosition === 'left') {
        panelClass.push('left');
      } else if (horizontalPosition === 'end' || horizontalPosition === 'right') {
        panelClass.push('right');
      } else {
        panelClass.push('h-center');
      }
      if (verticalPosition === 'top') {
        panelClass.push('top');
      } else {
        panelClass.push('bottom');
      }

      const componentFactory = this.componentFactoryResolver.resolveComponentFactory(TbSnackBarComponent);
      const data: ToastPanelData = {
        notification: notificationMessage,
        panelClass,
        destroyToastComponent: () => {
          this.viewContainerRef.detach(0);
          this.toastComponentRef.destroy();
        }
      };
      const providers: StaticProvider[] = [
        {provide: MAT_SNACK_BAR_DATA, useValue: data}
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      this.toastComponentRef = this.viewContainerRef.createComponent(componentFactory, 0, injector);
      this.cd.detectChanges();

      if (notificationMessage.duration && notificationMessage.duration > 0) {
        if (this.dismissTimeout !== null) {
          clearTimeout(this.dismissTimeout);
          this.dismissTimeout = null;
        }
        this.dismissTimeout = setTimeout(() => {
          if (this.toastComponentRef) {
            this.toastComponentRef.instance.actionButton._elementRef.nativeElement.click();
          }
          this.dismissTimeout = null;
        }, notificationMessage.duration + 500);
      }
      this.toastComponentRef.onDestroy(() => {
        if (this.dismissTimeout !== null) {
          clearTimeout(this.dismissTimeout);
          this.dismissTimeout = null;
        }
        this.toastComponentRef = null;
        this.currentMessage = null;
      });
    });
  }

  private showSnackBar(notificationMessage: NotificationMessage, isGtSm: boolean) {
    this.ngZone.run(() => {
      if (this.snackBarRef) {
        this.snackBarRef.dismiss();
      }
      const data: ToastPanelData = {
        notification: notificationMessage,
        parent: this.elementRef,
        panelClass: [],
        destroyToastComponent: () => {}
      };
      const config: MatSnackBarConfig = {
        horizontalPosition: notificationMessage.horizontalPosition || 'left',
        verticalPosition: !isGtSm ? 'bottom' : (notificationMessage.verticalPosition || 'top'),
        viewContainerRef: this.viewContainerRef,
        duration: notificationMessage.duration,
        panelClass: notificationMessage.panelClass,
        data
      };
      this.snackBarRef = this.snackBar.openFromComponent(TbSnackBarComponent, config);
      if (notificationMessage.duration && notificationMessage.duration > 0 && notificationMessage.forceDismiss) {
        if (this.dismissTimeout !== null) {
          clearTimeout(this.dismissTimeout);
          this.dismissTimeout = null;
        }
        this.dismissTimeout = setTimeout(() => {
          if (this.snackBarRef) {
            this.snackBarRef.instance.actionButton._elementRef.nativeElement.click();
          }
          this.dismissTimeout = null;
        }, notificationMessage.duration);
      }
      this.snackBarRef.afterDismissed().subscribe(() => {
        if (this.dismissTimeout !== null) {
          clearTimeout(this.dismissTimeout);
          this.dismissTimeout = null;
        }
        this.snackBarRef = null;
        this.currentMessage = null;
      });
    });
  }

  private shouldDisplayMessage(notificationMessage: NotificationMessage): boolean {
    if (notificationMessage && notificationMessage.message) {
      const target = notificationMessage.target || 'root';
      if (this.toastTarget === target) {
        if (!this.currentMessage || this.currentMessage.message !== notificationMessage.message
          || this.currentMessage.type !== notificationMessage.type) {
          return true;
        }
      }
    }
    return false;
  }

  ngOnDestroy(): void {
    if (this.toastComponentRef) {
      this.viewContainerRef.detach(0);
      this.toastComponentRef.destroy();
    }
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
    if (this.hideNotificationSubscription) {
      this.hideNotificationSubscription.unsubscribe();
    }
  }
}

interface ToastPanelData {
  notification: NotificationMessage;
  parent?: ElementRef;
  panelClass: string[];
  destroyToastComponent: () => void;
}

import {
  AnimationTriggerMetadata,
  AnimationEvent,
  trigger,
  state,
  transition,
  style,
  animate,
} from '@angular/animations';
import { onParentScrollOrWindowResize } from '@core/utils';

export const toastAnimations: {
  readonly showHideToast: AnimationTriggerMetadata;
} = {
  showHideToast: trigger('showHideAnimation', [
    state('in', style({ transform: 'scale(1)', opacity: 1 })),
    transition('void => opened', [style({ transform: 'scale(0)', opacity: 0 }), animate('{{ open }}ms')]),
    transition(
      'opened => closing',
      animate('{{ close }}ms', style({ transform: 'scale(0)', opacity: 0 })),
    ),
  ]),
};

export type ToastAnimationState = 'default' | 'opened' | 'closing';

@Component({
  selector: 'tb-snack-bar-component',
  templateUrl: 'snack-bar-component.html',
  styleUrls: ['snack-bar-component.scss'],
  animations: [toastAnimations.showHideToast]
})
export class TbSnackBarComponent implements AfterViewInit, OnDestroy {

  @ViewChild('actionButton', {static: true}) actionButton: MatButton;

  @HostBinding('class')
  get panelClass(): string[] {
    return this.data.panelClass;
  }

  private parentEl: HTMLElement;
  private snackBarContainerEl: HTMLElement;
  private parentScrollSubscription: Subscription = null;

  public notification: NotificationMessage;

  animationState: ToastAnimationState;

  animationParams = {
    open: 100,
    close: 100
  };

  constructor(@Inject(MAT_SNACK_BAR_DATA)
              private data: ToastPanelData,
              private elementRef: ElementRef,
              @Optional()
              private snackBarRef: MatSnackBarRef<TbSnackBarComponent>) {
    this.animationState = !!this.snackBarRef ? 'default' : 'opened';
    this.notification = data.notification;
  }

  ngAfterViewInit() {
    if (this.snackBarRef) {
      this.parentEl = this.data.parent.nativeElement;
      this.snackBarContainerEl = $(this.elementRef.nativeElement).closest('snack-bar-container')[0];
      this.snackBarContainerEl.style.position = 'absolute';
      this.updateContainerRect();
      this.updatePosition(this.snackBarRef.containerInstance.snackBarConfig);
      this.parentScrollSubscription = onParentScrollOrWindowResize(this.parentEl).subscribe(() => {
        this.updateContainerRect();
      });
    }
  }

  private updatePosition(config: MatSnackBarConfig) {
    const isRtl = config.direction === 'rtl';
    const isLeft = (config.horizontalPosition === 'left' ||
      (config.horizontalPosition === 'start' && !isRtl) ||
      (config.horizontalPosition === 'end' && isRtl));
    const isRight = !isLeft && config.horizontalPosition !== 'center';
    if (isLeft) {
      this.snackBarContainerEl.style.justifyContent = 'flex-start';
    } else if (isRight) {
      this.snackBarContainerEl.style.justifyContent = 'flex-end';
    } else {
      this.snackBarContainerEl.style.justifyContent = 'center';
    }
    if (config.verticalPosition === 'top') {
      this.snackBarContainerEl.style.alignItems = 'flex-start';
    } else {
      this.snackBarContainerEl.style.alignItems = 'flex-end';
    }
  }

  private updateContainerRect() {
    const viewportOffset = this.parentEl.getBoundingClientRect();
    this.snackBarContainerEl.style.top = viewportOffset.top + 'px';
    this.snackBarContainerEl.style.left = viewportOffset.left + 'px';
    this.snackBarContainerEl.style.width = viewportOffset.width + 'px';
    this.snackBarContainerEl.style.height = viewportOffset.height + 'px';
  }

  ngOnDestroy() {
    if (this.parentScrollSubscription) {
      this.parentScrollSubscription.unsubscribe();
    }
  }

  action(event: MouseEvent): void {
    event.stopPropagation();
    if (this.snackBarRef) {
      this.snackBarRef.dismissWithAction();
    } else {
      this.animationState = 'closing';
    }
  }

  onHideFinished(event: AnimationEvent) {
    const { toState } = event;
    const isFadeOut = (toState as ToastAnimationState) === 'closing';
    const itFinished = this.animationState === 'closing';
    if (isFadeOut && itFinished) {
      this.data.destroyToastComponent();
    }
  }
}
