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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { FormFieldDefinition, FormFieldType } from '@shared/models/iot-hub/device-package.models';
import { generateSecret } from '@core/utils';

const DEFAULT_RANDOM_SIZE = 20;

/**
 * Reusable form renderer for the device install dialog's SHOW_FORM step. Consumes
 * the FormFieldDefinition[] parsed from the package's form.json directly.
 *
 * The renderer is presentation-only. The caller owns the FormGroup and supplies
 * the FormFieldDefinition[] array. Optional resolveImagePath callback maps
 * relative help-image paths to data URIs (the dialog supplies it from the parsed
 * package ZIP).
 */
@Component({
  selector: 'tb-install-form-renderer',
  templateUrl: './install-form-renderer.component.html',
  styleUrls: ['./install-form-renderer.component.scss'],
  standalone: false
})
export class InstallFormRendererComponent implements OnChanges {

  @Input() fields: FormFieldDefinition[] = [];
  @Input() formGroup!: UntypedFormGroup;
  @Input() resolveImagePath?: (path: string) => string;
  /** When true, PASSWORD inputs render unmasked by default (used by review mode). */
  @Input() reviewMode = false;

  passwordVisible: Record<string, boolean> = {};

  readonly FormFieldType = FormFieldType;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.fields || changes.reviewMode) {
      this.passwordVisible = {};
      if (this.reviewMode) {
        for (const f of this.fields) {
          if (f.type === FormFieldType.PASSWORD) {
            this.passwordVisible[f.key] = true;
          }
        }
      }
    }
  }

  togglePasswordVisible(key: string): void {
    this.passwordVisible[key] = !this.passwordVisible[key];
  }

  regenerate(field: FormFieldDefinition): void {
    const control = this.formGroup.controls[field.key];
    if (!control) return;
    control.patchValue(generateSecret(field.randomSize ?? DEFAULT_RANDOM_SIZE));
    control.markAsDirty();
  }

  getPatternErrorMessage(field: FormFieldDefinition): string {
    return field.validators?.[0]?.message || 'Invalid format';
  }

  imagePath(path: string): string {
    return this.resolveImagePath ? this.resolveImagePath(path) : path;
  }
}
