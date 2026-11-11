// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validators,
  NG_VALIDATORS,
  Validator,
  ValidationErrors
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { attributeKeyValueValidator } from '@home/components/scheduler/config/attribute-key-value-table.component';
import { deepClone } from '@core/utils';
import { safeMerge, updateAttributesDefaults } from '@home/components/scheduler/config/config.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-update-attributes-event-config',
    templateUrl: './update-attributes.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => UpdateAttributesComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => UpdateAttributesComponent),
            multi: true
        }],
    standalone: false
})
export class UpdateAttributesComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  modelValue: SchedulerEventConfiguration | null;

  updateAttributesFormGroup: UntypedFormGroup;

  currentGroupType: EntityType;

  attributeScopes: AttributeScope[] = [];

  attributeScope = AttributeScope;

  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  entityType = EntityType;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    this.attributeScopes.push(AttributeScope.SERVER_SCOPE);
    this.attributeScopes.push(AttributeScope.SHARED_SCOPE);
    this.updateAttributesFormGroup = this.fb.group({
      originatorId: [null, [Validators.required]],
      serverAttributes: [{}, [attributeKeyValueValidator(false)]],
      sharedAttributes: [{}, [attributeKeyValueValidator(false)]],
      metadata: this.fb.group(
        {
          scope: [null, [Validators.required]]
        }
      )
    });

    this.updateAttributesFormGroup.get('originatorId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const originatorId: EntityId = this.updateAttributesFormGroup.get('originatorId').value;
      if (!originatorId || originatorId.entityType !== EntityType.DEVICE) {
        const scope: AttributeScope = this.updateAttributesFormGroup.get('metadata').get('scope').value;
        if (scope !== AttributeScope.SERVER_SCOPE) {
          this.updateAttributesFormGroup.get('metadata').get('scope').patchValue(AttributeScope.SERVER_SCOPE, {emitEvent: true});
        }
      }
    });

    this.updateAttributesFormGroup.get('metadata').get('scope').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateAttributesFormGroup.get('serverAttributes').patchValue({}, {emitEvent: false});
      this.updateAttributesFormGroup.get('sharedAttributes').patchValue({}, {emitEvent: false});
      this.updateValidators();
    });

    this.updateAttributesFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  private updateValidators() {
    const scope: AttributeScope = this.updateAttributesFormGroup.get('metadata').get('scope').value;
    this.updateAttributesFormGroup.get('serverAttributes')
      .setValidators(attributeKeyValueValidator(scope === AttributeScope.SERVER_SCOPE));
    this.updateAttributesFormGroup.get('sharedAttributes')
      .setValidators(attributeKeyValueValidator(scope === AttributeScope.SHARED_SCOPE));
    this.updateAttributesFormGroup.get('serverAttributes').updateValueAndValidity({emitEvent: false});
    this.updateAttributesFormGroup.get('sharedAttributes').updateValueAndValidity({emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    if (!this.updateAttributesFormGroup.valid) {
      setTimeout(() => {
        this.updateModel();
      }, 0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.updateAttributesFormGroup.disable({emitEvent: false});
    } else {
      this.updateAttributesFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = safeMerge<SchedulerEventConfiguration>(updateAttributesDefaults, value);
    const formValue = this.prepareInputConfig(this.modelValue);
    this.updateAttributesFormGroup.reset(formValue, { emitEvent: false });
    this.updateValidators();
  }

  validate(): ValidationErrors | null {
    if (!this.updateAttributesFormGroup.valid) {
      return {
        updateAttributesForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private prepareInputConfig(value: SchedulerEventConfiguration): SchedulerEventConfiguration | null {
    const formValue = deepClone(value);

    const attributes = formValue.msgBody;
    if (formValue.metadata.scope === AttributeScope.SERVER_SCOPE) {
      (formValue as any).serverAttributes = attributes;
    } else if (formValue.metadata.scope === AttributeScope.SHARED_SCOPE) {
      (formValue as any).sharedAttributes = attributes;
    }
    delete formValue.msgBody;

    return formValue;
  }

  private updateModel() {
    if (this.updateAttributesFormGroup.valid) {
      const value = this.updateAttributesFormGroup.getRawValue();
      const scope: AttributeScope = value.metadata.scope;
      if (scope === AttributeScope.SERVER_SCOPE) {
        value.msgBody = value.serverAttributes;
      } else if (scope === AttributeScope.SHARED_SCOPE) {
        value.msgBody = value.sharedAttributes;
      }
      delete value.serverAttributes;
      delete value.sharedAttributes;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
