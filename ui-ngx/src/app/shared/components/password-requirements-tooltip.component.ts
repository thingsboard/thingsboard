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
