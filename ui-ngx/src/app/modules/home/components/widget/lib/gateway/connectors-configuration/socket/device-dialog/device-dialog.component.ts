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
  DeviceAttributesRequests,
  DeviceAttributesUpdate,
  DeviceDataKey,
  DeviceRpcMethod,
  MappingInfo,
  MappingValue,
  noLeadTrailSpacesRegex,
  SocketKeysAddKeyTranslationsMap,
  SocketKeysDeleteKeyTranslationsMap,
  SocketKeysNoKeysTextTranslationsMap,
  SocketKeysPanelTitleTranslationsMap,
  SocketValueKey,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  DeviceDataKeysPanelComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/socket/device-data-keys-pannel/device-data-keys-panel.component';

@Component({
  selector: 'tb-device-dialog',
  templateUrl: './device-dialog.component.html',
  styleUrls: ['./device-dialog.component.scss']
})
export class DeviceDialogComponent extends DialogComponent<DeviceDialogComponent, MappingValue> implements OnDestroy {
  mappingForm: UntypedFormGroup;

  SocketValueKey = SocketValueKey;

  keysPopupClosed = true;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: MappingInfo,
              public dialogRef: MatDialogRef<DeviceDialogComponent, MappingValue>,
              private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
    super(store, router, dialogRef);
    this.createMappingForm();
  }

  get socketAttributes(): Array<string> {
    return this.mappingForm.get('attributes').value?.map((value: DeviceDataKey) => value.key) || [];
  }

  get socketTelemetry(): Array<string> {
    return this.mappingForm.get('timeseries').value?.map((value: DeviceDataKey) => value.key) || [];
  }

  get socketRpcMethods(): Array<string> {
    return this.mappingForm.get('rpc_methods').value?.map((value: DeviceRpcMethod) => value.methodRPC) || [];
  }

  get socketAttributesUpdates(): Array<string> {
    return this.mappingForm.get('attributes_updates')?.value?.map((value: DeviceAttributesUpdate) => value.attributeOnThingsBoard) || [];
  }

  get socketAttributesRequests(): Array<string> {
    return this.mappingForm.get('attributes_requests')?.value?.map((value: DeviceAttributesRequests) => value.requestExpression) || [];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  private createMappingForm(): void {
    this.mappingForm = this.fb.group({});
    this.createDataMappingForm();
  }

  helpLinkId(): string {
    return 'https://thingsboard.io/docs/iot-gateway/config/socket/#device-subsection';
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

  manageKeys($event: Event, matButton: MatButton, keysType: SocketValueKey): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const group = this.mappingForm;

      const keysControl = group.get(keysType);
      const ctx: { [key: string]: any } = {
        keys: keysControl.value,
        keysType,
        panelTitle: SocketKeysPanelTitleTranslationsMap.get(keysType),
        addKeyTitle: SocketKeysAddKeyTranslationsMap.get(keysType),
        deleteKeyTitle: SocketKeysDeleteKeyTranslationsMap.get(keysType),
        noKeysText: SocketKeysNoKeysTextTranslationsMap.get(keysType)
      };
      this.keysPopupClosed = false;
      const dataKeysPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DeviceDataKeysPanelComponent, 'leftBottom', false, null,
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

  private prepareMappingData(): { [key: string]: unknown } {
    const formValue = this.mappingForm.value;
    const {address, deviceName, deviceType} = formValue;
    return {
      address,
      deviceName,
      deviceType
    };
  }

  private prepareFormValueData(): { [key: string]: unknown } {
    if (this.data.value && Object.keys(this.data.value).length) {
      const {address, deviceName, deviceType} = this.data.value;
      return {
        address,
        deviceName,
        deviceType
      };
    }
  }

  private createDataMappingForm(): void {
    this.mappingForm = this.fb.group({
      address: ['127.0.0.1:50001', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceName: ['Device Example', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      deviceType: ['Default', [Validators.pattern(noLeadTrailSpacesRegex)]],
      timeseries: [[], []],
      attributes: [[], []],
      attributes_requests: [[], []],
      attributes_updates: [[], []],
      rpc_methods: [[], []],
    });
    this.mappingForm.patchValue(this.prepareFormValueData());
  }
}
