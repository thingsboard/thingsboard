// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { ClipboardService } from 'ngx-clipboard';
import { TooltipPosition } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { ThemePalette } from '@angular/material/core';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-copy-button',
    styleUrls: ['copy-button.component.scss'],
    templateUrl: './copy-button.component.html',
    standalone: false
})
export class CopyButtonComponent {

  private timer;

  copied = false;

  @Input()
  copyText: string;

  @Input()
  @coerceBoolean()
  disabled = false;

  // @deprecated need to use icon input
  @Input()
  mdiIcon: string;

  @Input()
  icon: string;

  @Input()
  tooltipText: string;

  @Input()
  tooltipPosition: TooltipPosition;

  @Input()
  style: {[key: string]: any} = {};

  @Input()
  color: ThemePalette;

  @Input()
  @coerceBoolean()
  miniButton = true;

  @Output()
  successCopied = new EventEmitter<string>();

  constructor(private clipboardService: ClipboardService,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  copy($event: Event): void {
    $event.stopPropagation();
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.clipboardService.copy(this.copyText);
    this.successCopied.emit(this.copyText);
    this.copied = true;
    this.timer = setTimeout(() => {
      this.copied = false;
      this.cd.detectChanges();
    }, 1500);
  }

  get matTooltipText(): string {
    return this.copied ? this.translate.instant('ota-update.copied') : this.tooltipText;
  }

  get matTooltipPosition(): TooltipPosition {
    return this.copied ? 'below' : this.tooltipPosition;
  }

}
