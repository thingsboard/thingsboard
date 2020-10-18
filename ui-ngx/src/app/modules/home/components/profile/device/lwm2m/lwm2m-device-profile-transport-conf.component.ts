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
  Component,
  forwardRef,
  Input,
  OnInit
} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {
  ATTR,
  OBSERVE,
  OBSERVE_ATTR,
  TELEMETRY,
  ObjectLwM2M
} from "./profile-config.models";
import {DeviceProfileService} from "../../../../../../core/http/device-profile.service";
import {deepClone} from "../../../../../../core/utils";

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
  observeAttr: string;
  observe: string;
  attribute: string;
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
              private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.initConstants();
    this.lwm2mDeviceProfileTransportConfFormGroup = this.fb.group({
      objectIds: [{}, Validators.required],
      observeAttrTelemetry: [this.fb.group({clientLwM2M: this.fb.control('')}), Validators.required],
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [null, Validators.required],
      notifIfDisabled: [true, []],
      binding: ["U", Validators.required],
      configurationJson: [null, Validators.required]
    });
    this.lwm2mDeviceProfileTransportConfFormGroup.valueChanges.subscribe(() => {
      if (!this.disabled) {
        this.updateModel();
      }
    });
  }

  initConstants(): void {
    this.observeAttr = OBSERVE_ATTR;
    this.observe = OBSERVE;
    this.attribute = ATTR;
    this.telemetry = TELEMETRY;
  }

  /**
   initChildesFormGroup
   */
  get bootstrapFormGroup(): FormGroup {
    return this.lwm2mDeviceProfileTransportConfFormGroup.get('bootstrapFormGroup') as FormGroup;
  }

  get lwm2mServerFormGroup(): FormGroup {
    return this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mServerFormGroup') as FormGroup;
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
    this.initWriteValue ();
  }

  private initWriteValue (): void {
    let modelValue = this.getObjectsFromJsonAllConfig();
    this.deviceProfileService.getLwm2mObjects(modelValue).subscribe(
      (objectsList) => {
        this.updateWriteValue ({"objectIds": modelValue, "objectsList": objectsList});
      }
    );
  }

  private updateWriteValue (value: any): void {
    let objectsList = deepClone(value.objectsList);
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        objectIds: value,
        observeAttrTelemetry: this.initObserveAttrTelemetryFormGroup(this.getObserveAttrTelemetryObjects(objectsList)),
        shortId: this.configurationValue['bootstrap'].servers.shortId,
        lifetime: this.configurationValue['bootstrap'].servers.lifetime,
        defaultMinPeriod: this.configurationValue['bootstrap'].servers.defaultMinPeriod,
        notifIfDisabled: this.configurationValue['bootstrap'].servers.notifIfDisabled,
        binding: this.configurationValue['bootstrap'].servers.binding
      },
      {emitEvent: false});
  }

  private initObserveAttrTelemetryFormGroup (listObject: ObjectLwM2M[]): FormGroup {
    return this.fb.group({
      clientLwM2M: this.fb.array(listObject)
    });
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

  private updateObserveAttrTelemetryObjectFormGroup(objectsList: ObjectLwM2M[]) {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        observeAttrTelemetry: this.initObserveAttrTelemetryFormGroup(this.getObserveAttrTelemetryObjects(objectsList))
      },
      {emitEvent: false});
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
        this.upDateValueToJsonTab_1();
        break;
      case 2:
        // this.upDateValueToJsonTab_2();
        break;
    }
  }

  upDateValueToJsonTab_0(): void {
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").pristine && this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").valid) {
      this.upDateObserveAttrFromGroup(this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").value);
      this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
  }


  upDateValueToJsonTab_1(): void {
    if (this.lwm2mDeviceProfileTransportConfFormGroup !== null) {
      if (this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').dirty && this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').valid) {
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

  getObserveAttrTelemetryObjects(listObject: ObjectLwM2M[]): ObjectLwM2M [] {
    let clientObserveAttr = deepClone(listObject);
    if (this.configurationValue[this.observeAttr]) {
      let observeArray = this.configurationValue[this.observeAttr][this.observe] as Array<string>;
      let attributeArray = this.configurationValue[this.observeAttr][this.attribute] as Array<string>;
      let telemetryArray = this.configurationValue[this.observeAttr][this.telemetry] as Array<string>;
      if (observeArray) clientObserveAttr = this.updateObserveAttrTelemetryObjects(observeArray, clientObserveAttr, "observe");
      if (attributeArray) clientObserveAttr = this.updateObserveAttrTelemetryObjects(attributeArray, clientObserveAttr, "attribute");
      if (telemetryArray) clientObserveAttr = this.updateObserveAttrTelemetryObjects(telemetryArray, clientObserveAttr, "telemetry");
    }
    return clientObserveAttr;
  }

  updateObserveAttrTelemetryObjects(isParameter: Array<string>, clientObserveAttr: ObjectLwM2M[], nameParameter: string): ObjectLwM2M [] {
    isParameter.forEach(attr => {
      let pathParameter = Array.from(attr.substring(1).split('/'), Number);
      clientObserveAttr.forEach(obj => {
        if (obj.id === pathParameter[0]) {
          obj.instances.forEach(inst => {
            if (inst.id === pathParameter[1]) {
              inst.resources.forEach(res => {
                if (res.id === pathParameter[2]) res[nameParameter] = true;
              })
            }
          })
        }
      });
    });
    return clientObserveAttr;
  }

  upDateObserveAttrFromGroup(val: any): void {
    let observeArray = [] as Array<string>;
    let attributeArray = [] as Array<string>;
    let telemetryArray = [] as Array<string>;
    let observeJson = JSON.parse(JSON.stringify(val['clientLwM2M'])) as [];
    let pathObj;
    let pathInst;
    let pathRes
    observeJson.forEach(obj => {
      Object.entries(obj).forEach(([key, value]) => {
        if (key === 'id') {
          pathObj = value;
        }
        if (key === 'instances') {
          let instancesJson = JSON.parse(JSON.stringify(value)) as [];
          if (instancesJson.length > 0) {
            instancesJson.forEach(instance => {
              Object.entries(instance).forEach(([key, value]) => {
                if (key === 'id') {
                  pathInst = value;
                }
                let pathInstObserve;
                if (key === 'observe' && value) {
                  pathInstObserve = '/' + pathObj + '/' + pathInst;
                  observeArray.push(pathInstObserve)
                }
                if (key === 'resources') {
                  let resourcesJson = JSON.parse(JSON.stringify(value)) as [];
                  if (resourcesJson.length > 0) {
                    resourcesJson.forEach(res => {
                      Object.entries(res).forEach(([key, value]) => {
                        if (key === 'id') {
                          // pathRes = value
                          pathRes = '/' + pathObj + '/' + pathInst + '/' + value;
                        } else if (key === 'observe' && value) {
                          observeArray.push(pathRes)
                        } else if (key === 'attribute' && value) {
                          attributeArray.push(pathRes)
                        } else if (key === 'telemetry' && value) {
                          telemetryArray.push(pathRes)
                        }
                      });
                    });
                  }
                }
              });
            });
          }
        }
      });
    });
    if (this.configurationValue[this.observeAttr] === undefined
    ) {
      this.configurationValue[this.observeAttr] = {
        [this.observe]: observeArray,
        [this.attribute]: attributeArray,
        [this.telemetry]: telemetryArray
      };
    } else {
      this.configurationValue[this.observeAttr][this.observe] = observeArray;
      this.configurationValue[this.observeAttr][this.attribute] = attributeArray;
      this.configurationValue[this.observeAttr][this.telemetry] = telemetryArray;
    }
  }

  getObjectsFromJsonAllConfig(): number [] {
    let observe = this.configurationValue[this.observeAttr][this.observe];
    let attribute = this.configurationValue[this.observeAttr][this.attribute];
    let telemetry = this.configurationValue[this.observeAttr][this.telemetry];
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
    return Array.from(objectsIds);
  }

  upDateJsonAllConfig(): void {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
    this.lwm2mDeviceProfileTransportConfFormGroup.markAsPristine({
      onlySelf: true
    });
  }

  addList(value: ObjectLwM2M[]): void {
    this.updateObserveAttrTelemetryObjectFormGroup(deepClone(value));
  }

  removeList(value: ObjectLwM2M): void {
    console.warn(this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").value);
    let objectOld = (!this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").value['clientLwM2M']) ?
      deepClone(this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").value.get('clientLwM2M').value as Array<ObjectLwM2M>) :
      deepClone(this.lwm2mDeviceProfileTransportConfFormGroup.get("observeAttrTelemetry").value['clientLwM2M']);
    const isIdIndex = (element) => element.id === value.id;
    let index = objectOld.findIndex(isIdIndex);
    if (index >= 0) {
      objectOld.splice(index, 1);
    }
    this.updateObserveAttrTelemetryObjectFormGroup(objectOld);
    this.removeObserveAttrTelemetryFromJson(this.observe, value.id);
    this.removeObserveAttrTelemetryFromJson(this.telemetry, value.id);
    this.removeObserveAttrTelemetryFromJson(this.attribute, value.id);
    this.upDateJsonAllConfig();
  }

  removeObserveAttrTelemetryFromJson(observeAttrTel: string, id: number) {
    let isIdIndex = (element) => Array.from(element.substring(1).split('/'), Number)[0] === id;
    let index = this.configurationValue[this.observeAttr][observeAttrTel].findIndex(isIdIndex);
    while (index >= 0) {
      this.configurationValue[this.observeAttr][observeAttrTel].splice(index, 1);
      index = this.configurationValue[this.observeAttr][observeAttrTel].findIndex(isIdIndex);
    }
  }
}
