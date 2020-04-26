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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  Directive,
  ElementRef,
  Inject,
  Input,
  NgZone,
  OnDestroy,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { MAT_SNACK_BAR_DATA, MatSnackBar, MatSnackBarConfig, MatSnackBarRef } from '@angular/material/snack-bar';
import { NotificationMessage } from '@app/core/notification/notification.models';
import { onParentScrollOrWindowResize } from '@app/core/utils';
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

  private snackBarRef: MatSnackBarRef<TbSnackBarComponent> = null;
  private currentMessage: NotificationMessage = null;

  private dismissTimeout: Timeout = null;

  constructor(public elementRef: ElementRef,
              public viewContainerRef: ViewContainerRef,
              private notificationService: NotificationService,
              public snackBar: MatSnackBar,
              private ngZone: NgZone,
              private breakpointObserver: BreakpointObserver) {
  }

  ngAfterViewInit(): void {
    this.notificationSubscription = this.notificationService.getNotification().subscribe(
      (notificationMessage) => {
        if (this.shouldDisplayMessage(notificationMessage)) {
          this.currentMessage = notificationMessage;
          const data = {
            parent: this.elementRef,
            notification: notificationMessage
          };
          const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
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
            });
          }
        }
      }
    );
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
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
    if (this.hideNotificationSubscription) {
      this.hideNotificationSubscription.unsubscribe();
    }
  }
}

@Component({
  selector: 'tb-snack-bar-component',
  templateUrl: 'snack-bar-component.html',
  styleUrls: ['snack-bar-component.scss']
})
export class TbSnackBarComponent implements AfterViewInit, OnDestroy {

  @ViewChild('actionButton', {static: true}) actionButton: MatButton;

  private parentEl: HTMLElement;
  public snackBarContainerEl: HTMLElement;
  private parentScrollSubscription: Subscription = null;
  public notification: NotificationMessage;
  constructor(@Inject(MAT_SNACK_BAR_DATA) public data: any, private elementRef: ElementRef,
              public cd: ChangeDetectorRef,
              public snackBarRef: MatSnackBarRef<TbSnackBarComponent>) {
    this.notification = data.notification;
  }

  ngAfterViewInit() {
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

  updatePosition(config: MatSnackBarConfig) {
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

  ngOnDestroy() {
    if (this.parentScrollSubscription) {
      this.parentScrollSubscription.unsubscribe();
    }
  }

  updateContainerRect() {
    const viewportOffset = this.parentEl.getBoundingClientRect();
    this.snackBarContainerEl.style.top = viewportOffset.top + 'px';
    this.snackBarContainerEl.style.left = viewportOffset.left + 'px';
    this.snackBarContainerEl.style.width = viewportOffset.width + 'px';
    this.snackBarContainerEl.style.height = viewportOffset.height + 'px';
  }

  action(): void {
    this.snackBarRef.dismissWithAction();
  }
}
