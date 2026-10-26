// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Input, OnChanges, SimpleChanges, TemplateRef } from '@angular/core';
import { AbstractControl, UntypedFormGroup } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { cleanFormFieldValue, FormFieldDefinition, FormFieldType } from '@shared/models/iot-hub/device-package.models';
import { generateSecret } from '@core/utils';

const DEFAULT_RANDOM_SIZE = 20;

/**
 * Fields shown on one line. When one of them has a help popup (outside review mode), the row shows
 * the field's hint in a box under it, next to the popup trigger, and its fields don't reserve space
 * for a hint or an error, so the box sits right under them.
 */
interface FormRow {
  fields: FormFieldDefinition[];
  hasHintBox: boolean;
}

/** Consecutive rows of one group, shown in a panel with the group as its title; no title for ungrouped fields. */
interface FormSection {
  title?: string;
  rows: FormRow[];
}

/**
 * Reusable form renderer for the device install dialog's SHOW_FORM step. Consumes
 * the FormFieldDefinition[] parsed from the package's form.json directly — both
 * device-side and integration-side fields are merged into a single combined form
 * by the package author at export time.
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
  /** Rendered under the field marked with checkConnection; the dialog supplies it. */
  @Input() checkConnectionTemplate?: TemplateRef<unknown> | null;

  passwordVisible: Record<string, boolean> = {};
  // Fields in render order: consecutive fields of a group form a section, and consecutive fields
  // with the same `row` share one line
  sections: FormSection[] = [];
  // The same for the fields of the collapsed Advanced settings panel
  advancedSections: FormSection[] = [];
  private focusedControl: AbstractControl | null = null;
  private keepFocusedError = false;

  /**
   * A new error shows only after the user leaves the field, so a value is not called wrong while it
   * is typed. An error already shown when the user enters the field stays until the value is valid.
   */
  readonly errorStateMatcher: ErrorStateMatcher = {
    isErrorState: (control, form) => {
      if (control && control === this.focusedControl) {
        this.keepFocusedError = this.keepFocusedError && control.invalid;
        return this.keepFocusedError;
      }
      return !!control?.invalid && (control.touched || !!form?.submitted);
    }
  };

  readonly FormFieldType = FormFieldType;

  /** Render this field via the Secret picker widget (tb-secret-key-input). The picker
   *  itself accepts both plaintext and Secret references, so secretSupport=true means
   *  "this field MAY hold a Secret reference and the picker UI should be shown". */
  isSecretWidget(field: FormFieldDefinition): boolean {
    return field.secretSupport === true;
  }

  ngOnChanges(changes: SimpleChanges): void {
    // The hint boxes depend on reviewMode too
    if (changes.fields || changes.reviewMode) {
      const fields = this.fields ?? [];
      this.sections = this.buildSections(fields.filter(field => field.section !== 'advanced'));
      this.advancedSections = this.buildSections(fields.filter(field => field.section === 'advanced'));
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

  private buildSections(fields: FormFieldDefinition[]): FormSection[] {
    const sections: FormSection[] = [];
    let previous: FormFieldDefinition | undefined;
    for (const field of fields) {
      if (!previous || field.group !== previous.group) {
        sections.push({ title: field.group, rows: [] });
      }
      const rows = sections[sections.length - 1].rows;
      const last = rows[rows.length - 1];
      const hasHintBox = !!field.helpPopupId && !this.reviewMode;
      if (field.row && last?.fields[0].row === field.row) {
        last.fields.push(field);
        last.hasHintBox ||= hasHintBox;
      } else {
        rows.push({ fields: [field], hasHintBox });
      }
      previous = field;
    }
    return sections;
  }

  hasCheckConnectionField(fields: FormFieldDefinition[]): boolean {
    return fields.some(field => field.checkConnection);
  }

  /** The hint of a field that a defaultValueTemplate fills describes the filled value. */
  helpTextOf(field: FormFieldDefinition): string | undefined {
    return field.defaultValueTemplate && field.defaultValueTemplateHelpText ? field.defaultValueTemplateHelpText : field.helpText;
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

  onFocus(field: FormFieldDefinition): void {
    const control = this.formGroup.controls[field.key];
    this.focusedControl = control ?? null;
    this.keepFocusedError = !!control?.invalid && control.touched;
  }

  onBlur(field: FormFieldDefinition): void {
    this.focusedControl = null;
    const control = this.formGroup.controls[field.key];
    const cleaned = cleanFormFieldValue(field, control?.value);
    if (control && cleaned !== control.value) {
      control.setValue(cleaned);
    }
  }

  getPatternErrorMessage(field: FormFieldDefinition): string {
    return field.validators?.[0]?.message || 'Invalid format';
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
