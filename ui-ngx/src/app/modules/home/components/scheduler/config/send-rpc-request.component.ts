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
import { jsonRequired } from '@shared/components/json-object-edit.component';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { safeMerge, sendRPCRequestDefaults } from '@home/components/scheduler/config/config.models';

@Component({
    selector: 'tb-send-rpc-request-event-config',
    templateUrl: './send-rpc-request.component.html',
    styleUrls: ['./send-rpc-request.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SendRpcRequestComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => SendRpcRequestComponent),
            multi: true
        }],
    standalone: false
})
export class SendRpcRequestComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy, Validator {

  modelValue: SchedulerEventConfiguration | null;

  sendRpcRequestFormGroup: UntypedFormGroup;

  entityType = EntityType;

  private destroy$ = new Subject<void>();

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    this.sendRpcRequestFormGroup = this.fb.group({
      originatorId: [null, [Validators.required]],
      msgBody: this.fb.group(
        {
          method: [null, [Validators.required, Validators.pattern(/^\S+$/)]],
          params: [null, [jsonRequired]]
        }
      ),
      metadata: this.fb.group(
        {
          oneway: [null, []],
          timeout: [null, [Validators.min(0)]],
          persistent: [null, []]
        }
      )
    });

    this.sendRpcRequestFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    if (!this.sendRpcRequestFormGroup.valid) {
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
      this.sendRpcRequestFormGroup.disable({emitEvent: false});
    } else {
      this.sendRpcRequestFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SchedulerEventConfiguration | null): void {
    this.modelValue = safeMerge<SchedulerEventConfiguration>(sendRPCRequestDefaults, value);
    this.sendRpcRequestFormGroup.reset(this.modelValue, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    if (!this.sendRpcRequestFormGroup.valid) {
      return {
        rpcRequestForm: {
          valid: false
        }
      };
    }

    return null;
  }

  private updateModel() {
    if (this.sendRpcRequestFormGroup.valid) {
      const value = this.sendRpcRequestFormGroup.getRawValue();
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  public getMethodValidationText(): string {
    const methodControl = this.sendRpcRequestFormGroup.get('msgBody.method');
    if (methodControl.hasError('required')) {
      return this.translate.instant('scheduler.rpc-method-required');
    } else if (methodControl.hasError('pattern')) {
      return this.translate.instant('scheduler.rpc-method-white-space');
    }

    return '';
  }
}
