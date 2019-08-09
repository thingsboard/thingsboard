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
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewContainerRef
} from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';

@Directive({
  selector: '[tb-fullscreen]'
})
export class FullscreenDirective {

  fullscreenValue = false;

  private overlayRef: OverlayRef;
  private parentElement: HTMLElement;

  @Input()
  set fullscreen(fullscreen: boolean) {
    if (this.fullscreenValue !== fullscreen) {
      this.fullscreenValue = fullscreen;
      if (this.fullscreenValue) {
        this.enterFullscreen();
      } else {
        this.exitFullscreen();
      }
    }
  }

  @Output()
  fullscreenChanged = new EventEmitter<boolean>();

  constructor(public elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private overlay: Overlay) {

  }

  enterFullscreen() {
    this.parentElement = this.elementRef.nativeElement.parentElement;
    this.parentElement.removeChild(this.elementRef.nativeElement);
    this.elementRef.nativeElement.classList.add('tb-fullscreen');
    const position = this.overlay.position();
    const config = new OverlayConfig({
      hasBackdrop: false,
      panelClass: 'tb-fullscreen-parent'
    });
    config.minWidth = '100%';
    config.minHeight = '100%';
    config.positionStrategy = position.global().top('0%').left('0%')
      .right('0%').bottom('0%');

    this.overlayRef = this.overlay.create(config);
    this.overlayRef.attach(new EmptyPortal());
    this.overlayRef.overlayElement.append( this.elementRef.nativeElement );
    this.fullscreenChanged.emit(true);
  }

  exitFullscreen() {
    if (this.parentElement) {
      this.overlayRef.overlayElement.removeChild( this.elementRef.nativeElement );
      this.parentElement.append( this.elementRef.nativeElement );
      this.parentElement = null;
    }
    this.elementRef.nativeElement.classList.remove('tb-fullscreen');
    this.overlayRef.dispose();
    this.fullscreenChanged.emit(false);
  }

}

class EmptyPortal extends ComponentPortal<TbAnchorComponent> {

  constructor() {
    super(TbAnchorComponent);
  }

}
