///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  Component, ComponentRef,
  Directive,
  ElementRef,
  Inject,
  Input,
  NgZone,
  OnDestroy, Optional,
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
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';

@Directive({
  selector: '[tb-toast]'
})
export class ToastDirective implements AfterViewInit, OnDestroy {

  @Input()
  toastTarget = 'root';

  private notificationSubscription: Subscription = null;
  private hideNotificationSubscription: Subscription = null;

  private snackBarRef: MatSnackBarRef<any> = null;
  private overlayRef: OverlayRef;
  private toastComponentRef: ComponentRef<TbSnackBarComponent>;
  private currentMessage: NotificationMessage = null;

  private dismissTimeout: Timeout = null;

  constructor(private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private notificationService: NotificationService,
              private overlay: Overlay,
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

      const position = this.overlay.position();
      let panelClass = ['tb-toast-panel'];
      if (notificationMessage.panelClass) {
        if (typeof notificationMessage.panelClass === 'string') {
          panelClass.push(notificationMessage.panelClass);
        } else if (notificationMessage.panelClass.length) {
          panelClass = panelClass.concat(notificationMessage.panelClass);
        }
      }
      const overlayConfig = new OverlayConfig({
        panelClass,
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: false,
        disposeOnNavigation: true
      });
      let originX;
      let originY;
      const horizontalPosition = notificationMessage.horizontalPosition || 'left';
      const verticalPosition = notificationMessage.verticalPosition || 'top';
      if (horizontalPosition === 'start' || horizontalPosition === 'left') {
        originX = 'start';
      } else if (horizontalPosition === 'end' || horizontalPosition === 'right') {
        originX = 'end';
      } else {
        originX = 'center';
      }
      if (verticalPosition === 'top') {
        originY = 'top';
      } else {
        originY = 'bottom';
      }
      const connectedPosition: ConnectedPosition = {
        originX,
        originY,
        overlayX: originX,
        overlayY: originY
      };
      overlayConfig.positionStrategy = position.flexibleConnectedTo(this.elementRef)
        .withPositions([connectedPosition]);
      this.overlayRef = this.overlay.create(overlayConfig);
      const data: ToastPanelData = {
        notification: notificationMessage
      };
      const injectionTokens = new WeakMap<any, any>([
        [MAT_SNACK_BAR_DATA, data],
        [OverlayRef, this.overlayRef]
      ]);
      const injector = new PortalInjector(this.viewContainerRef.injector, injectionTokens);
      this.toastComponentRef = this.overlayRef.attach(new ComponentPortal(TbSnackBarComponent, this.viewContainerRef, injector));
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
        this.overlayRef = null;
        this.toastComponentRef = null;
        this.currentMessage = null;
      });
    });
  }

  private showSnackBar(notificationMessage: NotificationMessage, isGtSm: boolean) {
    const data: ToastPanelData = {
      notification: notificationMessage,
      parent: this.elementRef
    };
    const config: MatSnackBarConfig = {
      horizontalPosition: notificationMessage.horizontalPosition || 'left',
      verticalPosition: !isGtSm ? 'bottom' : (notificationMessage.verticalPosition || 'top'),
      viewContainerRef: this.viewContainerRef,
      duration: notificationMessage.duration,
      panelClass: notificationMessage.panelClass,
      data
    };
    this.ngZone.run(() => {
      if (this.snackBarRef) {
        this.snackBarRef.dismiss();
      }
      this.snackBarRef = this.snackBar.openFromComponent(TbSnackBarComponent, config);
    });
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
    if (this.overlayRef) {
      this.overlayRef.dispose();
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
              private snackBarRef: MatSnackBarRef<TbSnackBarComponent>,
              @Optional()
              private overlayRef: OverlayRef) {
    this.animationState = !!this.snackBarRef ? 'default' : 'opened';
    this.notification = data.notification;
  }

  ngAfterViewInit() {
    if (this.snackBarRef) {
      this.parentEl = this.data.parent.nativeElement;
      this.snackBarContainerEl = this.elementRef.nativeElement.parentNode;
      this.snackBarContainerEl.style.position = 'absolute';
      this.updateContainerRect();
      this.updatePosition(this.snackBarRef.containerInstance.snackBarConfig);
      const snackBarComponent = this;
      this.parentScrollSubscription = onParentScrollOrWindowResize(this.parentEl).subscribe(() => {
        snackBarComponent.updateContainerRect();
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

  action(): void {
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
      this.overlayRef.dispose();
    }
  }
}
