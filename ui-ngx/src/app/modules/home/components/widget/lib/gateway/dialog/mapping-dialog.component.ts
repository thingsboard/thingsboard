///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Inject, OnDestroy, Renderer2, ViewContainerRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  Attribute,
  AttributesUpdate,
  ConnectorMapping,
  ConnectorMappingFormValue,
  ConverterMappingFormValue,
  ConvertorType,
  ConvertorTypeTranslationsMap,
  DataConversionTranslationsMap,
  DeviceConnectorMapping,
  DeviceInfoType,
  HelpLinkByMappingTypeMap,
  MappingHintTranslationsMap,
  MappingInfo,
  MappingKeysAddKeyTranslationsMap,
  MappingKeysDeleteKeyTranslationsMap,
  MappingKeysNoKeysTextTranslationsMap,
  MappingKeysPanelTitleTranslationsMap,
  MappingKeysType,
  MappingType,
  MappingTypeTranslationsMap,
  noLeadTrailSpacesRegex,
  OPCUaSourceType,
  QualityTypes,
  QualityTypeTranslationsMap,
  RequestMappingData,
  RequestMappingFormValue,
  RequestType,
  RequestTypesTranslationsMap,
  RpcMethod,
  ServerSideRPCType,
  SourceType,
  SourceTypeTranslationsMap,
  Timeseries
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
import {
  MappingDataKeysPanelComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mapping-data-keys-panel/mapping-data-keys-panel.component';

@Component({
  selector: 'tb-mapping-dialog',
  templateUrl: './mapping-dialog.component.html',
  styleUrls: ['./mapping-dialog.component.scss']
})
export class MappingDialogComponent extends DialogComponent<MappingDialogComponent, ConnectorMapping> implements OnDestroy {

  mappingForm: UntypedFormGroup;

  MappingType = MappingType;

  qualityTypes = QualityTypes;
  QualityTranslationsMap = QualityTypeTranslationsMap;

  convertorTypes: ConvertorType[] = Object.values(ConvertorType);
  ConvertorTypeEnum = ConvertorType;
  ConvertorTypeTranslationsMap = ConvertorTypeTranslationsMap;

  sourceTypes: SourceType[] = Object.values(SourceType);
  OPCUaSourceTypes = Object.values(OPCUaSourceType) as Array<OPCUaSourceType>;
  OPCUaSourceTypesEnum = OPCUaSourceType;
  sourceTypesEnum = SourceType;
  SourceTypeTranslationsMap = SourceTypeTranslationsMap;

  requestTypes: RequestType[] = Object.values(RequestType);
  RequestTypeEnum = RequestType;
  RequestTypesTranslationsMap = RequestTypesTranslationsMap;

  DeviceInfoType = DeviceInfoType;

  ServerSideRPCType = ServerSideRPCType;

  MappingKeysType = MappingKeysType;

  MappingHintTranslationsMap = MappingHintTranslationsMap;

  MappingTypeTranslationsMap = MappingTypeTranslationsMap;

  DataConversionTranslationsMap = DataConversionTranslationsMap;

  HelpLinkByMappingTypeMap = HelpLinkByMappingTypeMap;

  keysPopupClosed = true;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MappingInfo,
              public dialogRef: MatDialogRef<MappingDialogComponent, ConnectorMapping>,
              private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private translate: TranslateService) {
    super(store, router, dialogRef);

    this.createMappingForm();
  }

  get converterAttributes(): Array<string> {
    if (this.converterType) {
      return this.mappingForm.get('converter').get(this.converterType).value.attributes.map((value: Attribute) => value.key);
    }
  }

  get converterTelemetry(): Array<string> {
    if (this.converterType) {
      return this.mappingForm.get('converter').get(this.converterType).value.timeseries.map((value: Timeseries) => value.key);
    }
  }

  get opcAttributes(): Array<string> {
    return this.mappingForm.get('attributes').value?.map((value: Attribute) => value.key) || [];
  }

  get opcTelemetry(): Array<string> {
    return this.mappingForm.get('timeseries').value?.map((value: Timeseries) => value.key) || [];
  }

  get opcRpcMethods(): Array<string> {
    return this.mappingForm.get('rpc_methods').value?.map((value: RpcMethod) => value.method) || [];
  }

  get opcAttributesUpdates(): Array<string> {
    return this.mappingForm.get('attributes_updates')?.value?.map((value: AttributesUpdate) => value.key) || [];
  }

  get converterType(): ConvertorType {
    return this.mappingForm.get('converter').get('type').value;
  }

  get customKeys(): Array<string> {
    return Object.keys(this.mappingForm.get('converter').get('custom').value.extensionConfig);
  }

  get requestMappingType(): RequestType {
    return this.mappingForm.get('requestType').value;
  }

  get responseTimeoutErrorTooltip(): string {
    const control = this.mappingForm.get('requestValue.serverSideRpc.responseTimeout');
    if (control.hasError('required')) {
      return this.translate.instant('gateway.response-timeout-required');
    } else if (control.hasError('min')) {
      return this.translate.instant('gateway.response-timeout-limits-error', {min: 1});
    }
    return '';
  }


  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  private createMappingForm(): void {
    switch (this.data.mappingType) {
      case MappingType.DATA:
        this.mappingForm = this.fb.group({});
        this.createDataMappingForm();
        break;
      case MappingType.REQUESTS:
        this.mappingForm = this.fb.group({});
        this.createRequestMappingForm();
        break;
      case MappingType.OPCUA:
        this.createOPCUAMappingForm();
    }
  }

  cancel(): void {
    if (this.keysPopupClosed) {
      this.dialogRef.close(null);
    }
  }

  add(): void {
    if (this.mappingForm.valid) {
      this.dialogRef.close(this.prepareMappingData());
    }
  }

  manageKeys($event: Event, matButton: MatButton, keysType: MappingKeysType): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const group = this.data.mappingType !== MappingType.OPCUA ? this.mappingForm.get('converter').get(this.converterType)
        : this.mappingForm;

      const keysControl = group.get(keysType);
      const ctx: { [key: string]: any } = {
        keys: keysControl.value,
        keysType,
        rawData: this.mappingForm.get('converter.type')?.value === ConvertorType.BYTES,
        panelTitle: MappingKeysPanelTitleTranslationsMap.get(keysType),
        addKeyTitle: MappingKeysAddKeyTranslationsMap.get(keysType),
        deleteKeyTitle: MappingKeysDeleteKeyTranslationsMap.get(keysType),
        noKeysText: MappingKeysNoKeysTextTranslationsMap.get(keysType)
      };
      if (this.data.mappingType === MappingType.OPCUA) {
        ctx.valueTypeKeys = Object.values(OPCUaSourceType);
        ctx.valueTypeEnum = OPCUaSourceType;
        ctx.valueTypes = SourceTypeTranslationsMap;
      }
      this.keysPopupClosed = false;
      const dataKeysPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, MappingDataKeysPanelComponent, 'leftBottom', false, null,
        ctx,
        {},
        {}, {}, true);
      dataKeysPanelPopover.tbComponentRef.instance.popover = dataKeysPanelPopover;
      dataKeysPanelPopover.tbComponentRef.instance.keysDataApplied.pipe(takeUntil(this.destroy$)).subscribe((keysData) => {
        dataKeysPanelPopover.hide();
        keysControl.patchValue(keysData);
        keysControl.markAsDirty();
      });
      dataKeysPanelPopover.tbHideStart.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.keysPopupClosed = true;
      });
    }
  }

  private prepareMappingData(): ConnectorMapping {
    const formValue = this.mappingForm.value;
    switch (this.data.mappingType) {
      case MappingType.DATA:
        const {converter, topicFilter, subscriptionQos} = formValue;
        return {
          topicFilter,
          subscriptionQos,
          converter: {
            type: converter.type,
            ...converter[converter.type]
          }
        };
      case MappingType.REQUESTS:
        return {
          requestType: formValue.requestType,
          requestValue: formValue.requestValue[formValue.requestType]
        };
      default:
        return formValue;
    }
  }

  private getFormValueData(): ConnectorMappingFormValue {
    if (this.data.value && Object.keys(this.data.value).length) {
      switch (this.data.mappingType) {
        case MappingType.DATA:
          const {converter, topicFilter, subscriptionQos} = this.data.value;
          return {
            topicFilter,
            subscriptionQos,
            converter: {
              type: converter.type,
              [converter.type]: {...converter}
            }
          } as ConverterMappingFormValue;
        case MappingType.REQUESTS:
          return {
            requestType: this.data.value.requestType,
            requestValue: {
              [this.data.value.requestType]: this.data.value.requestValue
            } as Record<RequestType, RequestMappingData>
          };
        default:
          return this.data.value as DeviceConnectorMapping;
      }
    }
  }

  private createDataMappingForm(): void {
    this.mappingForm.addControl('topicFilter',
      this.fb.control('', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]));
    this.mappingForm.addControl('subscriptionQos', this.fb.control(0));
    this.mappingForm.addControl('converter', this.fb.group({
      type: [ConvertorType.JSON, []],
      json: this.fb.group({
        deviceInfo: [{}, []],
        attributes: [[], []],
        timeseries: [[], []]
      }),
      bytes: this.fb.group({
        deviceInfo: [{}, []],
        attributes: [[], []],
        timeseries: [[], []]
      }),
      custom: this.fb.group({
        extension: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        extensionConfig: [{}, []]
      }),
    }));
    this.mappingForm.patchValue(this.getFormValueData());
    this.mappingForm.get('converter.type').valueChanges.pipe(
      startWith(this.mappingForm.get('converter.type').value),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const converterGroup = this.mappingForm.get('converter');
      converterGroup.get('json').disable({emitEvent: false});
      converterGroup.get('bytes').disable({emitEvent: false});
      converterGroup.get('custom').disable({emitEvent: false});
      converterGroup.get(value).enable({emitEvent: false});
    });
  }

  private createRequestMappingForm(): void {
    this.mappingForm.addControl('requestType', this.fb.control(RequestType.CONNECT_REQUEST, []));
    this.mappingForm.addControl('requestValue', this.fb.group({
      connectRequests: this.fb.group({
        topicFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        deviceInfo: [{}, []]
      }),
      disconnectRequests: this.fb.group({
        topicFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        deviceInfo: [{}, []]
      }),
      attributeRequests: this.fb.group({
        topicFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        deviceInfo: this.fb.group({
          deviceNameExpressionSource: [SourceType.MSG, []],
          deviceNameExpression: ['', [Validators.required]],
        }),
        attributeNameExpressionSource: [SourceType.MSG, []],
        attributeNameExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        topicExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        valueExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        retain: [false, []]
      }),
      attributeUpdates: this.fb.group({
        deviceNameFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        attributeFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        topicExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        valueExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        retain: [true, []]
      }),
      serverSideRpc: this.fb.group({
        type: [ServerSideRPCType.TWO_WAY, []],
        deviceNameFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        methodFilter: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        requestTopicExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        responseTopicExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        valueExpression: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
        responseTopicQoS: [0, []],
        responseTimeout: [10000, [Validators.required, Validators.min(1)]],
      })
    }));
    this.mappingForm.get('requestType').valueChanges.pipe(
      startWith(this.mappingForm.get('requestType').value),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const requestValueGroup = this.mappingForm.get('requestValue');
      requestValueGroup.get('connectRequests').disable({emitEvent: false});
      requestValueGroup.get('disconnectRequests').disable({emitEvent: false});
      requestValueGroup.get('attributeRequests').disable({emitEvent: false});
      requestValueGroup.get('attributeUpdates').disable({emitEvent: false});
      requestValueGroup.get('serverSideRpc').disable({emitEvent: false});
      requestValueGroup.get(value).enable();
    });
    this.mappingForm.get('requestValue.serverSideRpc.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      const requestValueGroup = this.mappingForm.get('requestValue.serverSideRpc');
      if (value === ServerSideRPCType.ONE_WAY) {
        requestValueGroup.get('responseTopicExpression').disable({emitEvent: false});
        requestValueGroup.get('responseTopicQoS').disable({emitEvent: false});
        requestValueGroup.get('responseTimeout').disable({emitEvent: false});
      } else {
        requestValueGroup.get('responseTopicExpression').enable({emitEvent: false});
        requestValueGroup.get('responseTopicQoS').enable({emitEvent: false});
        requestValueGroup.get('responseTimeout').enable({emitEvent: false});
      }
    });
    this.mappingForm.patchValue(this.getFormValueData());
  }

  private createOPCUAMappingForm(): void {
    this.mappingForm = this.fb.group({
      deviceNodeSource: [OPCUaSourceType.PATH, []],
      deviceNodePattern: ['', [Validators.required]],
      deviceInfo: [{}, []],
      attributes: [[], []],
      timeseries: [[], []],
      rpc_methods: [[], []],
      attributes_updates: [[], []]
    });
    this.mappingForm.patchValue(this.getFormValueData());
  }
}
