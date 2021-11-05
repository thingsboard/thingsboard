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
  Input, OnChanges,
  OnDestroy,
  Renderer2, SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.service';
import { PopoverPlacement } from '@shared/components/popover.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  // tslint:disable-next-line:component-selector
  selector: '[tb-help-popup], [tb-help-popup-content]',
  templateUrl: './help-popup.component.html',
  styleUrls: ['./help-popup.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class HelpPopupComponent implements OnChanges, OnDestroy {

  @ViewChild('toggleHelpButton', {read: ElementRef, static: false}) toggleHelpButton: ElementRef;
  @ViewChild('toggleHelpTextButton', {read: ElementRef, static: false}) toggleHelpTextButton: ElementRef;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup') helpId: string;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup-content') helpContent: string;

  // tslint:disable-next-line:no-input-rename
  @Input('trigger-text') triggerText: string;

  // tslint:disable-next-line:no-input-rename
  @Input('trigger-style') triggerStyle: string;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup-placement') helpPopupPlacement: PopoverPlacement;

  // tslint:disable-next-line:no-input-rename
  @Input('tb-help-popup-style') helpPopupStyle: { [klass: string]: any } = {};

  popoverVisible = false;
  popoverReady = true;

  triggerSafeHtml: SafeHtml = null;
  textMode = false;

  constructor(private viewContainerRef: ViewContainerRef,
              private element: ElementRef<HTMLElement>,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private popoverService: TbPopoverService) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (isDefinedAndNotNull(this.triggerText)) {
      this.triggerSafeHtml = this.sanitizer.bypassSecurityTrustHtml(this.triggerText);
    } else {
      this.triggerSafeHtml = null;
    }
    this.textMode = this.triggerSafeHtml != null;
  }

  toggleHelp() {
    const trigger = this.textMode ? this.toggleHelpTextButton.nativeElement : this.toggleHelpButton.nativeElement;
    this.popoverService.toggleHelpPopover(trigger, this.renderer, this.viewContainerRef,
      this.helpId,
      this.helpContent,
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
