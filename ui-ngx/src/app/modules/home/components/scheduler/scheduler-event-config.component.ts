// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { SchedulerEventConfiguration } from '@shared/models/scheduler-event.models';
import { deepClone, isDefined } from '@core/utils';
import { SchedulerEventConfigType } from '@home/components/scheduler/scheduler-event-config.models';
import { jsonRequired } from '@shared/components/json-object-edit.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-scheduler-event-config',
    templateUrl: './scheduler-event-config.component.html',
    styleUrls: ['./scheduler-event-config.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SchedulerEventConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SchedulerEventConfigComponent),
            multi: true
        }],
    standalone: false
})
export class SchedulerEventConfigComponent implements ControlValueAccessor, OnInit, OnChanges, Validator {

  schedulerEventConfigFormGroup: UntypedFormGroup;

  modelValue: SchedulerEventConfiguration | null;

  @Input()
  disabled: boolean;

  @Input()
  schedulerEventConfigTypes: {[eventType: string]: SchedulerEventConfigType};

  @Input()
  schedulerEventType: string;

  useDefinedTemplate = false;
  showOriginator = true;
  showMsgType = true;
  showMetadata = true;

  private clearMsgBody = false;
  private clearMetadata = false;
  private clearMsgType = false;
  private clearOriginator = false;
  private propagateChange: (value: SchedulerEventConfiguration) => void = () => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.schedulerEventConfigFormGroup = this.fb.group({
      originatorId: [null],
      msgType: [null],
      configuration: [null, Validators.required],
      msgBody: [null, jsonRequired],
      metadata: [null]
    });
    this.schedulerEventConfigFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateView();
    });
    this.buildSchedulerEventConfigForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'schedulerEventType' && change.currentValue) {
          this.buildSchedulerEventConfigForm();
        }
      }
    }
  }

  private buildSchedulerEventConfigForm() {
    this.useDefinedTemplate = false;
    this.showOriginator = true;
    this.showMsgType = true;
    this.showMetadata = true;
    this.clearMsgBody = false;
    this.clearMetadata = false;
    this.clearMsgType = false;
    this.clearOriginator = false;
    if (this.schedulerEventType) {
      const configType = this.schedulerEventConfigTypes[this.schedulerEventType];
      if (configType) {
        this.useDefinedTemplate = isDefined(configType.template) || isDefined(configType.componentType);
        this.showOriginator = configType.originator;
        this.showMsgType = configType.msgType;
        this.showMetadata = configType.metadata;
        this.clearMsgBody = configType.clearMsgBody ?? false;
        this.clearMetadata = configType.clearMetadata ?? false;
        this.clearMsgType = configType.clearMsgType ?? false;
        this.clearOriginator = configType.clearOriginator ?? false;
      }
    }
    this.updateEnabledState();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.schedulerEventConfigFormGroup.disable({emitEvent: false});
    } else {
      this.updateEnabledState();
    }
  }

  private updateEnabledState() {
    if (!this.disabled) {
      if (this.showOriginator) {
        this.schedulerEventConfigFormGroup.get('originatorId').enable({emitEvent: false});
      } else {
        this.schedulerEventConfigFormGroup.get('originatorId').disable({emitEvent: false});
      }
      if (this.showMsgType) {
        this.schedulerEventConfigFormGroup.get('msgType').enable({emitEvent: false});
      } else {
        this.schedulerEventConfigFormGroup.get('msgType').disable({emitEvent: false});
      }
      if (this.useDefinedTemplate) {
        this.schedulerEventConfigFormGroup.get('configuration').enable({emitEvent: false});
        this.schedulerEventConfigFormGroup.get('msgBody').disable({emitEvent: false});
      } else {
        this.schedulerEventConfigFormGroup.get('msgBody').enable({emitEvent: false});
        this.schedulerEventConfigFormGroup.get('configuration').disable({emitEvent: false});
      }
      if (this.showMetadata) {
        this.schedulerEventConfigFormGroup.get('metadata').enable({emitEvent: false});
      } else {
        this.schedulerEventConfigFormGroup.get('metadata').disable({emitEvent: false});
      }
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = value;
    const model = deepClone(this.modelValue) || undefined;
    if (model) {
      (model as any).configuration = deepClone(model);
    }
    this.schedulerEventConfigFormGroup.reset(model,{emitEvent: false});
  }

  updateView() {
    if (this.schedulerEventConfigFormGroup.valid) {
      let schedulerEventConfig = this.schedulerEventConfigFormGroup.value;
      if (schedulerEventConfig) {
        if (this.useDefinedTemplate) {
          const configuration = schedulerEventConfig.configuration;
          if (this.clearMsgBody) {
            delete configuration.msgBody;
          }
          if (this.clearOriginator) {
            delete configuration.originatorId;
          }
          if (this.clearMsgType) {
            delete configuration.msgType;
          }
          if (this.clearMetadata) {
            delete configuration.metadata;
          }
          delete schedulerEventConfig.configuration;
          if (configuration) {
            schedulerEventConfig = {...configuration, ...schedulerEventConfig};
          }
        }
      }
      this.modelValue = schedulerEventConfig;
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  validate(_control: AbstractControl): ValidationErrors | null {
    if (!this.schedulerEventConfigFormGroup.valid) {
      return {
        schedulerEventConfigForm: {
          valid: false
        }
      };
    }

    return null;
  }
}
