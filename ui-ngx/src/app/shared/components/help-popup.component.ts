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

import { Component, ElementRef, Input, OnDestroy, Renderer2, ViewContainerRef, ViewEncapsulation } from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.component';

@Component({
  // tslint:disable-next-line:component-selector
  selector: '[tb-help-popup]',
  templateUrl: './help-popup.component.html',
  styleUrls: ['./help-popup.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class HelpPopupComponent implements OnDestroy {

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup') helpId: string;

  popoverVisible = false;
  popoverReady = true;

  constructor(private elementRef: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private popoverService: TbPopoverService) {}

  toggleHelp() {
    this.popoverService.toggleHelpPopover(this.elementRef.nativeElement, this.renderer, this.viewContainerRef,
      this.helpId,
      (visible) => {
        this.popoverVisible = visible;
      }, (ready => {
        this.popoverReady = ready;
      }));
  }

  ngOnDestroy(): void {
  }

}
