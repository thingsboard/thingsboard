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

import { Component, Inject, OnDestroy, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { BaseData, HasId } from '@shared/models/base-data';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  ConvertorTypes,
  ConvertorTypeTranslationsMap,
  MappingHintTranslationsMap,
  MappingKeysAddKeyTranslationsMap,
  MappingKeysDeleteKeyTranslationsMap,
  MappingKeysNoKeysTextTranslationsMap,
  MappingKeysPanelTitleTranslationsMap,
  MappingKeysType,
  MappingTypes,
  MappingTypeTranslationsMap,
  QualityTypes,
  QualityTypeTranslationsMap,
  RequestTypes,
  RequestTypesTranslationsMap,
  ServerSideRPCType,
  SourceTypes,
  SourceTypeTranslationsMap
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { startWith, takeUntil } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { MappingDataKeysPanelComponent } from '@home/components/widget/lib/gateway/connectors-configuration/mapping-data-keys-panel.component';

@Component({
  selector: 'tb-mapping-dialog',
  templateUrl: './mapping-dialog.component.html',
  styleUrls: ['./mapping-dialog.component.scss'],
  providers: [],
})
export class MappingDialogComponent extends DialogComponent<MappingDialogComponent, BaseData<HasId>> implements OnInit, OnDestroy {

  mappingForm: UntypedFormGroup;

  MappingTypes = MappingTypes;

  qualityTypes = QualityTypes;
  QualityTranslationsMap = QualityTypeTranslationsMap;

  convertorTypes = Object.values(ConvertorTypes);
  ConvertorTypes = ConvertorTypes;
  ConvertorTypeTranslationsMap = ConvertorTypeTranslationsMap;

  sourceTypes = Object.values(SourceTypes);
  sourceTypesEnum = SourceTypes;
  SourceTypeTranslationsMap = SourceTypeTranslationsMap;

  requestTypes = Object.values(RequestTypes);
  RequestTypes = RequestTypes;
  RequestTypesTranslationsMap = RequestTypesTranslationsMap;

  ServerSideRPCType = ServerSideRPCType;

  MappingKeysType = MappingKeysType;

  MappingHintTranslationsMap = MappingHintTranslationsMap;

  MappingTypeTranslationsMap = MappingTypeTranslationsMap;

  submitted = false;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: any,
              public dialogRef: MatDialogRef<MappingDialogComponent, any>,
              private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
    super(store, router, dialogRef);

    this.createMappingForm();
  }

  get converterAttributes(): Array<any> {
    if (this.converterType) {
      return this.mappingForm.get('converter').get(this.converterType).value.attributes;
    }
  }

  get converterTelemetry(): Array<any> {
    if (this.converterType) {
      return this.mappingForm.get('converter').get(this.converterType).value.timeseries;
    }
  }

  get converterType(): ConvertorTypes {
    return this.mappingForm.get('converter').get('type').value;
  }

  get customKeys(): any {
    return this.mappingForm.get('converter').get('custom').value.extensionConfig;
  }

  get requestMappingType(): any {
    return this.mappingForm.get('requestType').value;
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  private createMappingForm(): void {
    this.mappingForm = this.fb.group({});
    if (this.data.mappingType === MappingTypes.DATA) {
      this.mappingForm.addControl('topicFilter', this.fb.control('', [Validators.required]));
      this.mappingForm.addControl('subscriptionQos', this.fb.control(0));
      this.mappingForm.addControl('converter', this.fb.group({
        type: [ConvertorTypes.JSON, []],
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
          extension: ['', []],
          extensionConfig: [{}, []]
        }),
      }));
      this.mappingForm.patchValue(this.prepareFormValueData());
      this.mappingForm.get('converter.type').valueChanges.pipe(
        startWith(this.mappingForm.get('converter.type').value),
        takeUntil(this.destroy$)
      ).subscribe((value) => {
        const converterGroup = this.mappingForm.get('converter');
        converterGroup.get('json').disable({emitEvent: false});
        converterGroup.get('bytes').disable({emitEvent: false});
        converterGroup.get('custom').disable({emitEvent: false});
        converterGroup.get(value).enable({emitEvent: false});
      })
    }

    if (this.data.mappingType === MappingTypes.REQUESTS) {
      this.mappingForm.addControl('requestType', this.fb.control(RequestTypes.CONNECT_REQUEST, []));
      this.mappingForm.addControl('requestValue', this.fb.group({
        connectRequests: this.fb.group({
          topicFilter: ['', [Validators.required]],
          deviceInfo: [{}, []]
        }),
        disconnectRequests: this.fb.group({
          topicFilter: ['', [Validators.required]],
          deviceInfo: [{}, []]
        }),
        attributeRequests: this.fb.group({
          topicFilter: ['', [Validators.required]],
          deviceInfo: [{}, [Validators.required]],
          attributeNameExpressionSource: [SourceTypes.MSG],
          attributeNameExpression: ['', [Validators.required]],
          topicExpression: ['', [Validators.required]],
          valueExpression: ['', [Validators.required]],
          retain: [false, []]
        }),
        attributeUpdates: this.fb.group({
          deviceNameFilter: ['', [Validators.required]],
          attributeFilter: ['', [Validators.required]],
          topicExpression: ['', [Validators.required]],
          valueExpression: ['', [Validators.required]],
          retain: [true, []]
        }),
        serverSideRpc: this.fb.group({
          type: [ServerSideRPCType.TWO_WAY, []],
          deviceNameFilter: ['', [Validators.required]],
          methodFilter: ['', [Validators.required]],
          requestTopicExpression: ['', [Validators.required]],
          responseTopicExpression: ['', [Validators.required]],
          valueExpression: ['', [Validators.required]],
          responseTopicQoS: [0, []],
          responseTimeout: [10000, [Validators.required]],
        })
      }));
      this.mappingForm.patchValue(this.prepareFormValueData());
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
        requestValueGroup.get(value).enable({emitEvent: false});
      });
      this.mappingForm.get('requestValue.serverSideRpc.type').valueChanges.pipe(
        startWith(this.mappingForm.get('requestValue.serverSideRpc.type').value),
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
    }
  }

  helpLinkId(): string {
    return "";
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    if (this.mappingForm.valid) {
      this.dialogRef.close(this.prepareMappingData());
    }
  }

  manageKeys($event: Event, matButton: MatButton, keysType: MappingKeysType) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const keysControl = this.mappingForm.get('converter').get(this.converterType).get(keysType);
      const ctx: any = {
        keys: keysControl.value,
        keysType: keysType,
        rawData: this.mappingForm.get('converter.type').value === ConvertorTypes.BYTES,
        panelTitle: MappingKeysPanelTitleTranslationsMap.get(keysType),
        addKeyTitle: MappingKeysAddKeyTranslationsMap.get(keysType),
        deleteKeyTitle: MappingKeysDeleteKeyTranslationsMap.get(keysType),
        noKeysText: MappingKeysNoKeysTextTranslationsMap.get(keysType)
      };
      const dataKeysPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, MappingDataKeysPanelComponent, 'leftBottom', false, null,
        ctx,
        {},
        {}, {}, true);
      dataKeysPanelPopover.tbComponentRef.instance.popover = dataKeysPanelPopover;
      dataKeysPanelPopover.tbComponentRef.instance.keysDataApplied.subscribe((keysData) => {
        dataKeysPanelPopover.hide();
        keysControl.patchValue(keysData);
      });
    }
  }

  private prepareMappingData(): any {
    const formValue = this.mappingForm.value;
    if (this.data.mappingType === MappingTypes.DATA) {
      const { converter, topicFilter, subscriptionQos } = formValue;
      return {
        topicFilter,
        subscriptionQos,
        converter: {
          type: converter.type,
          ...converter[converter.type]
        }
      };
    } else {
      return {
        requestType: formValue.requestType,
        requestValue: formValue.requestValue[formValue.requestType]
      };
    }
  }

  private prepareFormValueData(): any {
    if (this.data.value && Object.keys(this.data.value).length) {
      if (this.data.mappingType === MappingTypes.DATA) {
        const {converter, topicFilter, subscriptionQos} = this.data.value;
        return {
          topicFilter,
          subscriptionQos,
          converter: {
            type: converter.type,
            [converter.type]: { ...converter }
          }
        };
      } else {
        return {
          requestType: this.data.value.requestType,
          requestValue: {
            [this.data.value.requestType]: this.data.value.requestValue
          }
        };
      }
    }
    return this.data.value;
  }
}
