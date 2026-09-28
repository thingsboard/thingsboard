// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, Directive, DoCheck, OnInit, QueryList, ViewChildren } from '@angular/core';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { deepClone, isEqual } from '@core/utils';
import { AbstractControl, ControlValueAccessor, NgForm, ValidationErrors, Validator } from '@angular/forms';

@Directive()
export class CustomSchedulerEventConfigComponent
  extends PageComponent implements OnInit, AfterViewInit, DoCheck, ControlValueAccessor, Validator {

  @ViewChildren(NgForm, {read: NgForm}) forms: QueryList<NgForm>;

  configuration: SchedulerEventConfiguration;
  private configurationSnapshot: SchedulerEventConfiguration;

  disabled: boolean;

  [key: string]: any;

  private propagateChange = (_v: any) => { };

  constructor() {
    super();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit(): void {
    this.configurationSnapshot = deepClone(this.configuration);
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateFormsDisabledState();
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.configurationSnapshot = deepClone(value);
    this.configuration = value;
  }

  ngAfterViewInit() {
    if (this.forms) {
      this.updateFormsDisabledState();
      this.forms.changes.subscribe(() => {
        this.updateFormsDisabledState();
      });
    }
  }

  ngDoCheck(): void {
    const newConfiguration = this.doValidate() ? this.configuration : null;
    if (!isEqual(this.configurationSnapshot, newConfiguration)) {
      this.configurationSnapshot = deepClone(newConfiguration);
      setTimeout(() => {
        this.propagateChange(newConfiguration);
      }, 0);
    }
  }

  private updateFormsDisabledState() {
    if (this.forms) {
      setTimeout(() => {
        this.forms.toArray().forEach((form) => {
          if (this.disabled) {
            form.control.disable();
          } else {
            form.control.enable();
          }
        });
      }, 0);
    }
  }

  public validate(_control: AbstractControl): ValidationErrors | null {
    if (!this.doValidate()) {
      return {
        customSchedulerEventForm: {
          valid: false
        }
      };
    } else {
      return null;
    }
  }

  private doValidate(): boolean {
    if (this.forms) {
      const res = this.forms.toArray().filter((form) => form.valid === false);
      if (res && res.length) {
        return false;
      }
    }
    return true;
  }
}
