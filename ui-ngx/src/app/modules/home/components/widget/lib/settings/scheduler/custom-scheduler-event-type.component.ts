// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { CustomSchedulerEventType } from '@home/components/scheduler/scheduler-events.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export function customSchedulerEventTypeValidator(control: AbstractControl) {
    const schedulerEventType: CustomSchedulerEventType = control.value;
    if (!schedulerEventType
      || !schedulerEventType.name
      || !schedulerEventType.value
      || !schedulerEventType.template
    ) {
      return {
        customSchedulerEventType: true
      };
    }
    return null;
}

@Component({
    selector: 'tb-custom-scheduler-event-type',
    templateUrl: './custom-scheduler-event-type.component.html',
    styleUrls: ['./custom-scheduler-event-type.component.scss', './../widget-settings.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomSchedulerEventTypeComponent),
            multi: true
        }
    ],
    standalone: false
})
export class CustomSchedulerEventTypeComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeCustomSchedulerEventType = new EventEmitter();

  private modelValue: CustomSchedulerEventType;

  private propagateChange = null;

  public customSchedulerEventTypeFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.customSchedulerEventTypeFormGroup = this.fb.group({
      name: [null, [Validators.required]],
      value: [null, [Validators.required]],
      originator: [null, []],
      msgType: [null, []],
      metadata: [null, []],
      template: [null, [Validators.required]]
    });
    this.customSchedulerEventTypeFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.customSchedulerEventTypeFormGroup.disable({emitEvent: false});
    } else {
      this.customSchedulerEventTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CustomSchedulerEventType): void {
    this.modelValue = value;
    this.customSchedulerEventTypeFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  customSchedulerEventTypeHtml(): SafeHtml {
    const value: CustomSchedulerEventType = this.customSchedulerEventTypeFormGroup.value;
    const name = value.name || 'Undefined';
    const typeName = value.value || 'Undefined';
    return this.domSanitizer.bypassSecurityTrustHtml(`${name} (<small>${typeName}</small>)`);
  }

  private updateModel() {
    const value: CustomSchedulerEventType = this.customSchedulerEventTypeFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
