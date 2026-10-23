// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { FormFieldDefinition, FormFieldType } from '@shared/models/iot-hub/device-package.models';
import { generateSecret } from '@core/utils';

const DEFAULT_RANDOM_SIZE = 20;

/** Fields shown on one line. */
interface FormRow {
  fields: FormFieldDefinition[];
}

/** Consecutive rows of one group, shown in a panel with the group as its title; no title for ungrouped fields. */
interface FormSection {
  title?: string;
  rows: FormRow[];
}

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
  // Fields in render order: consecutive fields of a group form a section, and consecutive fields
  // with the same `row` share one line
  sections: FormSection[] = [];
  private rowOf = new Map<string, FormRow>();

  readonly FormFieldType = FormFieldType;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.fields) {
      this.sections = [];
      this.rowOf.clear();
      let previous: FormFieldDefinition | undefined;
      for (const field of this.fields ?? []) {
        if (!previous || field.group !== previous.group) {
          this.sections.push({ title: field.group, rows: [] });
        }
        const rows = this.sections[this.sections.length - 1].rows;
        const last = rows[rows.length - 1];
        if (field.row && last?.fields[0].row === field.row) {
          last.fields.push(field);
        } else {
          rows.push({ fields: [field] });
        }
        this.rowOf.set(field.key, rows[rows.length - 1]);
        previous = field;
      }
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

  /** Fields with a help popup show their hint in a box under the row, with the popup trigger. */
  hasHintBox(field: FormFieldDefinition): boolean {
    return !!field.helpPopupId && !this.reviewMode;
  }

  /**
   * True for the fields of a row that has a hint box. They don't reserve space for a hint or an
   * error, so the box sits right under them.
   */
  isInHintBoxRow(field: FormFieldDefinition): boolean {
    return !!this.rowOf.get(field.key)?.fields.some(f => this.hasHintBox(f));
  }

  /** True when the field fails a validator other than required and pattern (one the dialog added). */
  hasFieldError(field: FormFieldDefinition): boolean {
    const errors = this.formGroup.controls[field.key]?.errors;
    return !!errors && Object.keys(errors).some(key => key !== 'required' && key !== 'pattern');
  }

  imagePath(path: string): string {
    return this.resolveImagePath ? this.resolveImagePath(path) : path;
  }
}
