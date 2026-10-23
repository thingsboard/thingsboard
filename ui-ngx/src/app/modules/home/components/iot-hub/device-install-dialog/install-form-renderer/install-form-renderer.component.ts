// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
  // Fields in render order; consecutive fields with the same `row` share one line
  rows: { field: FormFieldDefinition; index: number }[][] = [];

  readonly FormFieldType = FormFieldType;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.fields) {
      this.rows = [];
      (this.fields ?? []).forEach((field, index) => {
        const last = this.rows[this.rows.length - 1];
        if (field.row && last?.[0].field.row === field.row) {
          last.push({ field, index });
        } else {
          this.rows.push([{ field, index }]);
        }
      });
    }
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

  /** True when the previous field belonged to a different group (for header rendering). */
  shouldRenderGroupHeader(field: FormFieldDefinition, index: number): boolean {
    if (!field.group) return false;
    if (index === 0) return true;
    return this.fields[index - 1].group !== field.group;
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
