///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  OnChanges,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.service';
import { PopoverPlacement } from '@shared/components/popover.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: '[tb-help-popup], [tb-help-popup-content], [tb-help-popup-content-base64], [tb-help-popup-async-content]',
    templateUrl: './help-popup.component.html',
    styleUrls: ['./help-popup.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class HelpPopupComponent implements OnChanges {

  @ViewChild('toggleHelpButton', {read: ElementRef, static: false}) toggleHelpButton: ElementRef;
  @ViewChild('toggleHelpTextButton', {read: ElementRef, static: false}) toggleHelpTextButton: ElementRef;

  @Input('tb-help-popup') helpId: string;

  @Input('tb-help-popup-content') helpContent: string;

  @Input('tb-help-popup-content-base64') helpContentBase64: string;

  @Input('tb-help-popup-async-content') asyncHelpContent: () => Observable<string> | null;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('help-icon') helpIcon = 'help_outline';

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('help-opened-icon') helpOpenedIcon = 'help';

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('help-icon-tooltip') helpIconTooltip = this.translate.instant('help.show-help');

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('help-icon-button-class') helpIconButtonClass = 'tb-mat-32';

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('trigger-text') triggerText: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('trigger-style') triggerStyle: string;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup-placement') helpPopupPlacement: PopoverPlacement;

  // eslint-disable-next-line @angular-eslint/no-input-rename
  @Input('tb-help-popup-style') helpPopupStyle: { [klass: string]: any } = {};

  popoverVisible = false;
  popoverReady = true;


  @Input()
  @coerceBoolean()
  hintMode = false;

  triggerSafeHtml: SafeHtml = null;
  textMode = false;

  constructor(private viewContainerRef: ViewContainerRef,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private popoverService: TbPopoverService,
              private translate: TranslateService) {
  }

  ngOnChanges(_changes: SimpleChanges): void {
    if (isDefinedAndNotNull(this.triggerText)) {
      this.triggerSafeHtml = this.sanitizer.bypassSecurityTrustHtml(this.triggerText);
    } else {
      this.triggerSafeHtml = null;
    }
    this.textMode = this.triggerSafeHtml != null;
  }

  disabled(): boolean {
    return !this.helpId && !this.helpContent && !this.helpContentBase64 && !this.asyncHelpContent;
  }

  toggleHelp() {
    if (!this.disabled()) {
      const trigger = this.textMode ? this.toggleHelpTextButton.nativeElement : this.toggleHelpButton.nativeElement;
      this.popoverService.toggleHelpPopover(trigger, this.renderer, this.viewContainerRef,
        this.helpId,
        this.helpContent,
        this.helpContentBase64,
        this.asyncHelpContent ? this.asyncHelpContent() : null,
        (visible) => {
          this.popoverVisible = visible;
        }, (ready => {
          this.popoverReady = ready;
        }),
        this.helpPopupPlacement,
        {},
        this.helpPopupStyle);
    }
  }
}
