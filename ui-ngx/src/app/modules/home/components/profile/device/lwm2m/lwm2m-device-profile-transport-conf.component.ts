///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType, Lwm2mDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import {MatTabChangeEvent} from "@angular/material/tabs";
import {
  ATTR,
  BOOTSTRAP_SERVER,
  BOOTSTRAP_SERVERS, JSON_OBSERVE,
  LWM2M_SERVER, OBSERVE,
  OBSERVE_ATTR, TELEMETRY
} from "../../../../pages/device/lwm2m/security-config.models";
import {
  AfterViewInit,
  Component,
  ElementRef, EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnInit, Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { MatChipList } from '@angular/material/chips';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {LWM2M_MODEL} from "./profile-config.models";
import {DeviceProfileId} from "../../../../../../shared/models/id/device-profile-id";

@Component({
  selector: 'tb-lwm2m-device-profile-transport-conf',
  templateUrl: './lwm2m-device-profile-transport-conf.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfComponent),
    multi: true
  }]
})
export class Lwm2mDeviceProfileTransportConfComponent implements ControlValueAccessor, OnInit {

  lwm2mDeviceProfileTransportConfFormGroup: FormGroup;
  observe: string;
  attr: string;
  telemetry: string;
  bootstrapServers: string;
  bootstrapServer: string;
  lwm2mServer: string;
  private configurationValue: {};
  private requiredValue: boolean;
  private tabIndexPrevious = 0 as number;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => {
  };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.lwm2mDeviceProfileTransportConfFormGroup = this.fb.group({
      objectIds:  [null, Validators.required],
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [null, Validators.required],
      notifIfDisabled: [true, []],
      binding: ["U", Validators.required],
      configurationJson: [null, Validators.required]
    });
    this.lwm2mDeviceProfileTransportConfFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  // initConstants(): void {
  //   this.observe = OBSERVE;
  //   this.attr = ATTR;
  //   this.telemetry = TELEMETRY;
  // }

  /**
   initChildesFormGroup
   */

  get bootstrapFormGroup(): FormGroup {
    return this.lwm2mDeviceProfileTransportConfFormGroup.get('bootstrapFormGroup') as FormGroup;
  }

  get lwm2mServerFormGroup(): FormGroup {
    return this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mServerFormGroup') as FormGroup;
  }

  get observeAttrTelemetryFormGroup(): FormGroup {
    return this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mDeviceProfileTransportConfModelFormGroup') as FormGroup;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.lwm2mDeviceProfileTransportConfFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceProfileTransportConfFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Lwm2mDeviceProfileTransportConfiguration | null): void {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        configurationJson: value
      },
      {emitEvent: false});
    this.configurationValue = this.lwm2mDeviceProfileTransportConfFormGroup.getRawValue().configurationJson;
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        objectIds: this.getObjectsFromJsonAllConfig(),
        shortId: this.configurationValue['bootstrap'].servers.shortId,
        lifetime: this.configurationValue['bootstrap'].servers.lifetime,
        defaultMinPeriod: this.configurationValue['bootstrap'].servers.defaultMinPeriod,
        notifIfDisabled: this.configurationValue['bootstrap'].servers.notifIfDisabled,
        binding: this.configurationValue['bootstrap'].servers.binding
      },
      {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    this.upDateValueToJson();
    if (this.lwm2mDeviceProfileTransportConfFormGroup.valid) {
      configuration = this.lwm2mDeviceProfileTransportConfFormGroup.getRawValue().configurationJson;
      configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }

  tabChanged = (tabChangeEvent: MatTabChangeEvent): void => {
    if (this.tabIndexPrevious !== tabChangeEvent.index) this.upDateValueToJson();
    this.tabIndexPrevious = tabChangeEvent.index;
  }

  upDateValueToJson(): void {
    switch (this.tabIndexPrevious) {
      case 0:
        this.upDateValueToJsonTab_0();
        break;
      case 1:
        // this.upDateValueToJsonTab_1();
        break;
      case 2:
        // this.upDateValueToJsonTab_2();
        break;
    }
  }

  upDateValueToJsonTab_0(): void {
    if (this.lwm2mDeviceProfileTransportConfFormGroup !== null) {
      if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').valid) {
        this.configurationValue['bootstrap'].servers.shortId = this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').value;
        this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').valid) {
        this.configurationValue['bootstrap'].servers.lifetime = this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').value;
        this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').valid) {
        this.configurationValue['bootstrap'].servers.defaultMinPeriod = this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').value;
        this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').valid) {
        this.configurationValue['bootstrap'].servers.notifIfDisabled = this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').value;
        this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').valid) {
        this.configurationValue['bootstrap'].servers.binding = this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').value;
        this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').markAsPristine({
          onlySelf: true
        });
        this.upDateJsonAllConfig();
      }

      // if (this.bootstrapFormGroup !== null && !this.bootstrapFormGroup.pristine && this.bootstrapFormGroup.valid) {
      //   this.configurationValue['bootstrap'].bootstrapServer = this.bootstrapFormGroup.value;
      //   this.bootstrapFormGroup.markAsPristine({
      //     onlySelf: true
      //   });
      //   this.upDateJsonAllConfig();
      // }   debugger
      //
      // if (this.lwm2mServerFormGroup !== null && !this.lwm2mServerFormGroup.pristine && this.lwm2mServerFormGroup.valid) {
      //   this.configurationValue['bootstrap'].lwm2mServer = this.lwm2mServerFormGroup.value;
      //   // this.configurationValue['bootstrap'].lwm2mServer.bootstrapServerIs = false;
      //   this.lwm2mServerFormGroup.markAsPristine({
      //     onlySelf: true
      //   });
      //   this.upDateJsonAllConfig();
      // }
    }
  }

  getObjectsFromJsonAllConfig(): number [] {
     let observe = this.configurationValue['observeAttr'].observe;
    let attribute = this.configurationValue['observeAttr'].attribute;
    let telemetry = this.configurationValue['observeAttr'].telemetry;
    let objectsIds = new Set<number>();

    observe.forEach(obj => {
      objectsIds.add(+obj.split("/")[1]);
    });
    attribute.forEach(obj => {
      objectsIds.add(+obj.split("/")[1]);
    });
    telemetry.forEach(obj => {
      objectsIds.add(+obj.split("/")[1]);
    });
    return Array.from( objectsIds );
  }

  upDateJsonAllConfig(): void {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
    this.lwm2mDeviceProfileTransportConfFormGroup.markAsDirty();
  }

  changeListAdd(value){
    console.warn("add:" + value)
  }

  changeListRemove(value){
    console.warn("remove:" + value)
  }

}
