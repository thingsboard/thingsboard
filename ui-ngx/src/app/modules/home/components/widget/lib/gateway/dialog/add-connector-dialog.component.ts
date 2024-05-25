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

import { Component, Inject, OnDestroy } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { BaseData, HasId } from '@shared/models/base-data';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  AddConnectorConfigData,
  ConnectorType,
  CreatedConnectorConfigData,
  GatewayConnectorDefaultTypesTranslatesMap,
  GatewayLogLevel,
  getDefaultConfig,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { Subject } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';

@Component({
  selector: 'tb-add-connector-dialog',
  templateUrl: './add-connector-dialog.component.html',
  styleUrls: ['./add-connector-dialog.component.scss'],
  providers: [],
})
export class AddConnectorDialogComponent extends DialogComponent<AddConnectorDialogComponent, BaseData<HasId>> implements OnDestroy {

  connectorForm: UntypedFormGroup;

  connectorType = ConnectorType;

  gatewayConnectorDefaultTypesTranslatesMap = GatewayConnectorDefaultTypesTranslatesMap;
  gatewayLogLevel = Object.values(GatewayLogLevel);

  submitted = false;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddConnectorConfigData,
              public dialogRef: MatDialogRef<AddConnectorDialogComponent, CreatedConnectorConfigData>,
              private fb: FormBuilder,
              private resourcesService: ResourcesService) {
    super(store, router, dialogRef);
    this.connectorForm = this.fb.group({
      type: [ConnectorType.MQTT, []],
      name: ['', [Validators.required, this.uniqNameRequired(), Validators.pattern(noLeadTrailSpacesRegex)]],
      logLevel: [GatewayLogLevel.INFO, []],
      useDefaults: [true, []],
      sendDataOnlyOnChange: [false, []],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  helpLinkId(): string {
    return 'https://thingsboard.io/docs/iot-gateway/configuration/';
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    const value = this.connectorForm.getRawValue();
    if (value.useDefaults) {
      getDefaultConfig(this.resourcesService, value.type).subscribe((defaultConfig) => {
        value.configurationJson = defaultConfig;
        if (this.connectorForm.valid) {
          this.dialogRef.close(value);
        }
      });
    } else if (this.connectorForm.valid) {
      this.dialogRef.close(value);
    }
  }

  private uniqNameRequired(): ValidatorFn {
    return (c: UntypedFormControl) => {
      const newName = c.value.trim().toLowerCase();
      const found = this.data.dataSourceData.find((connectorAttr) => {
        const connectorData = connectorAttr.value;
        return connectorData.name.toLowerCase() === newName;
      });
      if (found) {
        if (c.hasError('required')) {
          return c.getError('required');
        }
        return {
          duplicateName: {
            valid: false
          }
        };
      }
      return null;
    };
  }
}
