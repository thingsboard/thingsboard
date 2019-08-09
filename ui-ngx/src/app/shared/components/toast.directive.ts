///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  Component,
  Directive,
  ElementRef,
  Inject, Input,
  OnDestroy,
  ViewContainerRef
} from '@angular/core';
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBar,
  MatSnackBarConfig,
  MatSnackBarRef
} from '@angular/material';
import { NotificationMessage } from '@app/core/notification/notification.models';
import { onParentScrollOrWindowResize } from '@app/core/utils';
import { Subscription } from 'rxjs';
import { NotificationService } from '@app/core/services/notification.service';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';

@Directive({
  selector: '[tb-toast]'
})
export class ToastDirective implements AfterViewInit, OnDestroy {

  @Input()
  toastTarget = 'root';

  private notificationSubscription: Subscription = null;

  constructor(public elementRef: ElementRef,
              public viewContainerRef: ViewContainerRef,
              private notificationService: NotificationService,
              public snackBar: MatSnackBar,
              private breakpointObserver: BreakpointObserver) {
  }

  ngAfterViewInit(): void {
    const toastComponent = this;

    this.notificationSubscription = this.notificationService.getNotification().subscribe(
      (notificationMessage) => {
        if (notificationMessage && notificationMessage.message) {
          const target = notificationMessage.target || 'root';
          if (this.toastTarget === target) {
            const data = {
              parent: this.elementRef,
              notification: notificationMessage
            };
            const isGtSm = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']);
            const config: MatSnackBarConfig = {
              horizontalPosition: notificationMessage.horizontalPosition || 'left',
              verticalPosition: !isGtSm ? 'bottom' : (notificationMessage.verticalPosition || 'top'),
              viewContainerRef: toastComponent.viewContainerRef,
              duration: notificationMessage.duration,
              data
            };
            this.snackBar.openFromComponent(TbSnackBarComponent, config);
          }
        }
      }
    );
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }
}

@Component({
  selector: 'tb-snack-bar-component',
  templateUrl: 'snack-bar-component.html',
  styleUrls: ['snack-bar-component.scss']
})
export class TbSnackBarComponent implements AfterViewInit, OnDestroy {
  private parentEl: HTMLElement;
  private snackBarContainerEl: HTMLElement;
  private parentScrollSubscription: Subscription = null;
  public notification: NotificationMessage;
  constructor(@Inject(MAT_SNACK_BAR_DATA) public data: any, private elementRef: ElementRef,
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
