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
  Component,
  ElementRef,
  Input,
  OnDestroy,
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.service';
import { PopoverPlacement } from '@shared/components/popover.models';

@Component({
  // tslint:disable-next-line:component-selector
  selector: '[tb-help-popup]',
  templateUrl: './help-popup.component.html',
  styleUrls: ['./help-popup.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class HelpPopupComponent implements OnDestroy {

  @ViewChild('toggleHelpButton', {read: ElementRef, static: false}) toggleHelpButton: ElementRef;
  @ViewChild('toggleHelpSpan', {read: ElementRef, static: false}) toggleHelpSpan: ElementRef;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup') helpId: string;

  // tslint:disable-next-line:no-input-rename
  @Input('trigger-text') triggerText: string;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup-placement') helpPopupPlacement: PopoverPlacement;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup-style') helpPopupStyle: { [klass: string]: any } = {};

  popoverVisible = false;
  popoverReady = true;

  constructor(private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private popoverService: TbPopoverService) {}

  toggleHelp() {
    const trigger = this.triggerText ? this.toggleHelpSpan.nativeElement : this.toggleHelpButton.nativeElement;
    this.popoverService.toggleHelpPopover(trigger, this.renderer, this.viewContainerRef,
      this.helpId,
      '',
      (visible) => {
        this.popoverVisible = visible;
      }, (ready => {
        this.popoverReady = ready;
      }),
      this.helpPopupPlacement,
      {},
      this.helpPopupStyle);
  }

  ngOnDestroy(): void {
  }

}
