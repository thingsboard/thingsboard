// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { IntegrationForm } from '@home/components/integration/configuration/integration-form';
import { isDefinedAndNotNull } from '@core/utils';
import {
  HandlerConfigurationType,
  HandlerConfigurationTypeTranslation,
  TcpBinaryByteOrder,
  TcpHandlerConfigurationType,
  TcpIntegration,
  TcpTextMessageSeparator,
  TcpTextMessageSeparatorTranslation
} from '@shared/models/integration.models';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'tb-tcp-integration-form',
    templateUrl: './tcp-integration-form.component.html',
    styleUrls: ['./tcp-integration-form.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TcpIntegrationFormComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => TcpIntegrationFormComponent),
            multi: true,
        }],
    standalone: false
})
export class TcpIntegrationFormComponent extends IntegrationForm implements ControlValueAccessor, Validator {

  @Input() isSetDownlink: boolean;

  tcpConfigForm: UntypedFormGroup;

  HandlerConfigurationType = TcpHandlerConfigurationType;
  HandlerConfigurationTypeTranslation = HandlerConfigurationTypeTranslation;

  TcpBinaryByteOrder = TcpBinaryByteOrder;
  tcpTextMessageSeparatorList: TcpTextMessageSeparator[] = Object.values(TcpTextMessageSeparator);
  readonly TcpTextMessageSeparator = TcpTextMessageSeparator;
  readonly TcpTextMessageSeparatorTranslation = TcpTextMessageSeparatorTranslation;

  private propagateChangePending = false;
  private propagateChange = (v: any) => { };
  constructor(private fb: UntypedFormBuilder) {
    super();
    this.tcpConfigForm = this.fb.group({
      port: [10560, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soBacklogOption: [128, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soRcvBuf: [64, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soSndBuf: [64, [Validators.required, Validators.min(1), Validators.max(65535)]],
      soKeepaliveOption: [false],
      tcpNoDelay: [true],
      cacheSize: [1000, Validators.min(0)],
      timeToLiveInMinutes: [1440, [Validators.min(0), Validators.max(525600)]],
      handlerConfiguration: this.fb.group({
        handlerType: [HandlerConfigurationType.BINARY, Validators.required],
        byteOrder: [TcpBinaryByteOrder.LITTLE_ENDIAN],
        maxFrameLength: [128, [Validators.required, Validators.min(1), Validators.max(65535)]],
        lengthFieldOffset: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        lengthFieldLength: [2, [Validators.required, Validators.min(0), Validators.max(8)]],
        lengthAdjustment: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        initialBytesToStrip: [0, [Validators.required, Validators.min(0), Validators.max(8)]],
        failFast: [false],
        stripDelimiter: [{value: true, disabled: true}],
        messageSeparator: [{value: TcpTextMessageSeparator.SYSTEM_LINE_SEPARATOR, disabled: true}],
        customSeparatorRawValue: [{value: null, disabled: true}, [Validators.required]]
      })
    });

    this.tcpConfigForm.get('handlerConfiguration.messageSeparator').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: TcpTextMessageSeparator) => {
      if (value === TcpTextMessageSeparator.CUSTOM_SEPARATOR) {
        this.tcpConfigForm.get('handlerConfiguration.customSeparatorRawValue').enable({emitEvent: false});
      } else {
        this.tcpConfigForm.get('handlerConfiguration.customSeparatorRawValue').patchValue(null, {emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.customSeparatorRawValue').disable({emitEvent: false});
      }
    });

    this.tcpConfigForm.get('handlerConfiguration.handlerType').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value: TcpHandlerConfigurationType) => {
      this.updateHandleConfigurationField(value);
    });
    this.tcpConfigForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModels(this.tcpConfigForm.getRawValue());
    });
  }

  writeValue(value: TcpIntegration) {
    if (isDefinedAndNotNull(value?.clientConfiguration)) {
      this.tcpConfigForm.reset(value.clientConfiguration, {emitEvent: false});
      if (!this.disabled) {
        this.tcpConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: true});
      }
    } else {
      this.propagateChangePending = true;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
    if (this.propagateChangePending) {
      this.propagateChangePending = false;
      setTimeout(() => {
        this.updateModels(this.tcpConfigForm.getRawValue());
      }, 0);
    }
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tcpConfigForm.disable({emitEvent: false});
    } else {
      this.tcpConfigForm.enable({emitEvent: false});
      this.tcpConfigForm.get('handlerConfiguration.handlerType').updateValueAndValidity({onlySelf: true});
    }
  }

  validate(): ValidationErrors | null {
    return this.tcpConfigForm.valid ? null : {
      updConfigForm: {valid: false}
    };
  }

  private updateModels(value) {
    this.propagateChange({clientConfiguration: value});
  }

  private updateHandleConfigurationField(type: TcpHandlerConfigurationType) {
    this.tcpConfigForm.get('handlerConfiguration').disable({emitEvent: false});
    switch (type) {
      case HandlerConfigurationType.BINARY:
        this.tcpConfigForm.get('handlerConfiguration.byteOrder').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.maxFrameLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthFieldOffset').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthFieldLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.lengthAdjustment').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.initialBytesToStrip').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.failFast').enable({emitEvent: false});
        break;
      case HandlerConfigurationType.TEXT:
        this.tcpConfigForm.get('handlerConfiguration.maxFrameLength').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.stripDelimiter').enable({emitEvent: false});
        this.tcpConfigForm.get('handlerConfiguration.messageSeparator').enable({onlySelf: true});
        break;
    }
    this.tcpConfigForm.get('handlerConfiguration.handlerType').enable({emitEvent: false});
  }
}
