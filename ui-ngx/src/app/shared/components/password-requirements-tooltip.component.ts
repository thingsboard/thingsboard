// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { CdkOverlayOrigin, ConnectionPositionPair } from '@angular/cdk/overlay';
import { passwordErrorRules } from '@shared/models/password.models';
import { AbstractControl } from '@angular/forms';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import { POSITION_MAP } from '@shared/models/overlay.models';

@Component({
    selector: 'tb-password-requirements-tooltip',
    templateUrl: './password-requirements-tooltip.component.html',
    styleUrl: './password-requirements-tooltip.component.scss',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PasswordRequirementsTooltipComponent {
  @Input() passwordControl: AbstractControl;
  @Input() passwordPolicy: UserPasswordPolicy;
  @Input() trigger: CdkOverlayOrigin;

  passwordErrorRules = passwordErrorRules;
  isTooltipOpen = false;

  overlayPositions: ConnectionPositionPair[] = [
    {...POSITION_MAP.top, offsetY: -20}
  ];

  checkForError(errorName: string): boolean {
    return this.passwordControl?.hasError(errorName) ?? false;
  }

  onFocus(): void {
    this.isTooltipOpen = true;
  }

  onBlur(): void {
    this.isTooltipOpen = false;
  }
}
