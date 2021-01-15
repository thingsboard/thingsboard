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
  DeviceTransportType
} from '@shared/models/device.models';
import {
  Component,
  forwardRef, Inject,
  Input,
  OnInit
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  ATTR,
  OBSERVE,
  OBSERVE_ATTR,
  TELEMETRY,
  ObjectLwM2M, getDefaultProfileConfig, KEY_NAME, Instance, ProfileConfigModels, ResourceLwM2M
} from "./profile-config.models";
import { DeviceProfileService } from "@core/http/device-profile.service";
import { deepClone, isUndefined } from "@core/utils";
import { WINDOW } from "@core/services/window.service";
import { JsonObject } from '@angular/compiler-cli/ngcc/src/packages/entry_point';
import { isNotNullOrUndefined } from 'codelyzer/util/isNotNullOrUndefined';

@Component({
  selector: 'tb-profile-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-profile-transport-configuration.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class Lwm2mDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit, Validators {

  private configurationValue: ProfileConfigModels;
  private requiredValue: boolean;
  private disabled = false;

  lwm2mDeviceProfileTransportConfFormGroup: FormGroup;
  observeAttr = OBSERVE_ATTR as string;
  observe = OBSERVE as string;
  attribute = ATTR as string;
  telemetry = TELEMETRY as string;
  keyName = KEY_NAME as string;
  bootstrapServers: string;
  bootstrapServer: string;
  lwm2mServer: string;
  sortFunction = this.sortObjectKeyPathJson;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService,
              @Inject(WINDOW) private window: Window) {
    this.lwm2mDeviceProfileTransportConfFormGroup = this.fb.group({
      objectIds: [{}, Validators.required],
      observeAttrTelemetry: [{clientLwM2M: [] as ObjectLwM2M[]}, Validators.required],
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [null, Validators.required],
      notifIfDisabled: [true, []],
      binding: ['U', Validators.required],
      bootstrapServer: [null, Validators.required],
      lwm2mServer: [null, Validators.required],
      configurationJson: [null, Validators.required],
    });
    this.lwm2mDeviceProfileTransportConfFormGroup.valueChanges.subscribe(() => {
      if (!this.disabled) {
        this.updateModel();
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mDeviceProfileTransportConfFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceProfileTransportConfFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: any | null): void {
    value = (Object.keys(value).length === 0) ? getDefaultProfileConfig() : value;
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        configurationJson: value
      },
      {emitEvent: false});
    this.configurationValue = this.lwm2mDeviceProfileTransportConfFormGroup.getRawValue().configurationJson;
    this.initWriteValue();
  }

  private initWriteValue(): void {
    const modelValue = {objectIds: null, objectsList: []};
    modelValue.objectIds = this.getObjectsFromJsonAllConfig();
    if (modelValue.objectIds !== null) {
      this.deviceProfileService.getLwm2mObjects(modelValue.objectIds).subscribe(
          (objectsList) => {
            modelValue.objectsList = objectsList;
            this.updateWriteValue(modelValue);
          }
      );
    } else {
      this.updateWriteValue(modelValue);
    }
  }

  private updateWriteValue(value: any): void {
    const objectsList = deepClone(value.objectsList);
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
        objectIds: value,
        observeAttrTelemetry: {clientLwM2M: this.getObserveAttrTelemetryObjects(objectsList)},
        shortId: this.configurationValue.bootstrap.servers.shortId,
        lifetime: this.configurationValue.bootstrap.servers.lifetime,
        defaultMinPeriod: this.configurationValue.bootstrap.servers.defaultMinPeriod,
        notifIfDisabled: this.configurationValue.bootstrap.servers.notifIfDisabled,
        binding: this.configurationValue.bootstrap.servers.binding,
        bootstrapServer: this.configurationValue.bootstrap.bootstrapServer,
        lwm2mServer: this.configurationValue.bootstrap.lwm2mServer
      },
      {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.lwm2mDeviceProfileTransportConfFormGroup.valid) {
      this.upDateValueToJson();
      configuration = this.lwm2mDeviceProfileTransportConfFormGroup.getRawValue().configurationJson;
      configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }

  private updateObserveAttrTelemetryObjectFormGroup(objectsList: ObjectLwM2M[]) {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
          observeAttrTelemetry: {clientLwM2M: this.getObserveAttrTelemetryObjects(objectsList)}
        },
        {emitEvent: false});
    this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').markAsPristine({
      onlySelf: true
    });
  }

  upDateValueToJson(): void {
    this.upDateValueToJsonTab_0();
    this.upDateValueToJsonTab_1();
  }

  upDateValueToJsonTab_0(): void {
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').pristine) {
      this.upDateObserveAttrTelemetryFromGroupToJson(
          this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').value.clientLwM2M
      );
      this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
  }

  upDateValueToJsonTab_1(): void {
    this.upDateValueServersToJson();
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('bootstrapServer').pristine) {
      this.configurationValue.bootstrap.bootstrapServer = this.lwm2mDeviceProfileTransportConfFormGroup.get('bootstrapServer').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('bootstrapServer').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mServer').pristine) {
      this.configurationValue.bootstrap.lwm2mServer = this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mServer').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('lwm2mServer').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
  }

  upDateValueServersToJson(): void {
    const bootstrapServers = this.configurationValue.bootstrap.servers;
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').pristine) {
      bootstrapServers.shortId = this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('shortId').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').pristine) {
      bootstrapServers.lifetime = this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('lifetime').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').pristine) {
      bootstrapServers.defaultMinPeriod = this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('defaultMinPeriod').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').pristine) {
      bootstrapServers.notifIfDisabled = this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('notifIfDisabled').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
    if (!this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').pristine) {
      bootstrapServers.binding = this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').value;
      this.lwm2mDeviceProfileTransportConfFormGroup.get('binding').markAsPristine({
        onlySelf: true
      });
      this.upDateJsonAllConfig();
    }
  }

  getObserveAttrTelemetryObjects(listObject: ObjectLwM2M[]): ObjectLwM2M [] {
    const clientObserveAttr = deepClone(listObject);
    if (this.configurationValue[this.observeAttr]) {
      const observeArray = this.configurationValue[this.observeAttr][this.observe] as Array<string>;
      const attributeArray = this.configurationValue[this.observeAttr][this.attribute] as Array<string>;
      const telemetryArray = this.configurationValue[this.observeAttr][this.telemetry] as Array<string>;
      const keyNameJson = this.configurationValue[this.observeAttr][this.keyName] as JsonObject;
      if (this.includesInstancesNo(attributeArray, telemetryArray)) {
        this.addInstances(attributeArray, telemetryArray, clientObserveAttr);
      }
      if (observeArray) {
        this.updateObserveAttrTelemetryObjects(observeArray, clientObserveAttr, 'observe');
      }
      if (attributeArray) {
        this.updateObserveAttrTelemetryObjects(attributeArray, clientObserveAttr, 'attribute');
      }
      if (telemetryArray) {
        this.updateObserveAttrTelemetryObjects(telemetryArray, clientObserveAttr, 'telemetry');
      }
      if (keyNameJson) {
        this.updateKeyNameObjects(deepClone(keyNameJson), clientObserveAttr);
      }
    }
    clientObserveAttr.forEach(obj => {
      obj.instances.sort((a, b) => a.id - b.id);
    });
    return clientObserveAttr;
  }

  includesInstancesNo(attributeArray: Array<string>, telemetryArray: Array<string>): boolean {
    const isIdIndex = (element) => !element.includes('/0/');
    return attributeArray.findIndex(isIdIndex) >= 0 || telemetryArray.findIndex(isIdIndex) >= 0;
  }

  addInstances(attributeArray: Array<string>, telemetryArray: Array<string>, clientObserveAttr: ObjectLwM2M[]): void {
    const attr = [] as Array<string>;
    [...attributeArray].filter(x => (!x.includes('/0/'))).forEach(x => {
      attr.push(this.convertPathToInstance(x));
    });
    const telemetry = [] as Array<string>;
    [...telemetryArray].filter(x => (!x.includes('/0/'))).forEach(x => {
      telemetry.push(this.convertPathToInstance(x));
    });
    const instancesNoZero = new Set(attr.concat(telemetry).sort());
    instancesNoZero.forEach(path => {
      const pathParameter = Array.from(path.split('/'), Number);
      const objectLwM2M = clientObserveAttr.find(x => (x.id === pathParameter[0]));
      if (objectLwM2M) {
        const instance = deepClone(objectLwM2M.instances[0]) as Instance;
        instance.id = pathParameter[1];
        objectLwM2M.instances.push(instance);
      }
    });
  }

  convertPathToInstance(path: string): string {
    const newX = Array.from(path.substring(1).split('/'), Number);
    return [newX[0], newX[1]].join('/');
  }

  updateObserveAttrTelemetryObjects(isParameter: Array<string>, clientObserveAttr: ObjectLwM2M[], nameParameter: string): void {
    isParameter.forEach(attr => {
      const idKeys = Array.from(attr.substring(1).split('/'), Number);
      clientObserveAttr
          .forEach(e => {
            if (e.id === idKeys[0]) {
              const instance = e.instances.find(itrInstance => itrInstance.id === idKeys[1]);
              if (isNotNullOrUndefined(instance)) {
                instance.resources.find(resource => resource.id === idKeys[2])[nameParameter] = true;
              }
            }
          });
    });
  }

  updateKeyNameObjects(nameJson: JsonObject, clientObserveAttr: ObjectLwM2M[]): void {
    const keyName = JSON.parse(JSON.stringify(nameJson));
    Object.keys(keyName).forEach(key => {
      const idKeys = Array.from(key.substring(1).split('/'), Number);
      clientObserveAttr
          .forEach(e => {
            if (e.id === idKeys[0]) {
              e.instances
                  .find(instance => instance.id === idKeys[1]).resources
                  .find(resource => resource.id === idKeys[2]).keyName = keyName[key];
            }
          });
    });
  }

  upDateObserveAttrTelemetryFromGroupToJson(val: ObjectLwM2M[]): void {
    const observeArray: Array<string> = [];
    const attributeArray: Array<string> = [];
    const telemetryArray: Array<string> = [];
    const observeJson: ObjectLwM2M[] = JSON.parse(JSON.stringify(val));
    let pathObj;
    let pathInst;
    let pathRes;
    observeJson.forEach(obj => {
      Object.entries(obj).forEach(([key, value]) => {
        if (key === 'id') {
          pathObj = value;
        }
        if (key === 'instances') {
          const instancesJson = JSON.parse(JSON.stringify(value)) as Instance[];
          if (instancesJson.length > 0) {
            instancesJson.forEach(instance => {
              Object.entries(instance).forEach(([instanceKey, instanceValue]) => {
                if (instanceKey === 'id') {
                  pathInst = instanceValue;
                }
                if (instanceKey === 'resources') {
                  const resourcesJson = JSON.parse(JSON.stringify(instanceValue)) as ResourceLwM2M[];
                  if (resourcesJson.length > 0) {
                    resourcesJson.forEach(res => {
                      Object.entries(res).forEach(([resourceKey, resourceValue]) => {
                        if (resourceKey === 'id') {
                          // pathRes = resourceValue
                          pathRes = '/' + pathObj + '/' + pathInst + '/' + resourceValue;
                        } else if (resourceKey === 'observe' && resourceValue) {
                          observeArray.push(pathRes);
                        } else if (resourceKey === 'attribute' && resourceValue) {
                          attributeArray.push(pathRes);
                        } else if (resourceKey === 'telemetry' && resourceValue) {
                          telemetryArray.push(pathRes);
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
    if (isUndefined(this.configurationValue[this.observeAttr])) {
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
    this.updateKeyName();
  }

  sortObjectKeyPathJson(key, value) {
    if (key === 'keyName') {
      return Object.keys(value).sort((a, b) => {
        const aLC = Array.from(a.substring(1).split('/'), Number);
        const bLC = Array.from(b.substring(1).split('/'), Number);
        return aLC[0] === bLC[0] ? aLC[1] - bLC[1] : aLC[0] - bLC[0];
      }).reduce((r, k) => r[k] = value[k], {});
    } else {
      return value;
    }
  }

  updateKeyName(): void {
    const paths = new Set<string>();
    if (this.configurationValue[this.observeAttr][this.attribute]) {
      this.configurationValue[this.observeAttr][this.attribute].forEach(path => {
        paths.add(path);
      });
    }
    if (this.configurationValue[this.observeAttr][this.telemetry]) {
      this.configurationValue[this.observeAttr][this.telemetry].forEach(path => {
        paths.add(path);
      });
    }
    const keyNameNew = {};
    paths.forEach(path => {
      const pathParameter = this.findIndexsForIds(path);
      if (pathParameter.length === 3) {
        keyNameNew[path] = this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').value
            .clientLwM2M[pathParameter[0]].instances[pathParameter[1]].resources[pathParameter[2]][this.keyName];
      }
    });
    this.configurationValue[this.observeAttr][this.keyName] = this.sortObjectKeyPathJson('keyName', keyNameNew);
  }

  findIndexsForIds(path: string): number[] {
    const pathParameter = Array.from(path.substring(1).split('/'), Number);
    const pathParameterIndexes: number[] = [];
    const objectsOld = deepClone(
        this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').value.clientLwM2M) as ObjectLwM2M[];
    let isIdIndex = (element) => element.id === pathParameter[0];
    const objIndex = objectsOld.findIndex(isIdIndex);
    if (objIndex >= 0) {
      pathParameterIndexes.push(objIndex);
      isIdIndex = (element) => element.id === pathParameter[1];
      const instIndex = objectsOld[objIndex].instances.findIndex(isIdIndex);
      if (instIndex >= 0) {
        pathParameterIndexes.push(instIndex);
        isIdIndex = (element) => element.id === pathParameter[2];
        const resIndex = objectsOld[objIndex].instances[instIndex].resources.findIndex(isIdIndex);
        if (resIndex >= 0) {
          pathParameterIndexes.push(resIndex);
        }
      }
    }
    return pathParameterIndexes;
  }

  getObjectsFromJsonAllConfig(): number [] {
    const objectsIds = new Set<number>();
    if (this.configurationValue[this.observeAttr]) {
      if (this.configurationValue[this.observeAttr][this.observe]) {
        this.configurationValue[this.observeAttr][this.observe].forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), Number)[0]);
        });
      }
      if (this.configurationValue[this.observeAttr][this.attribute]) {
        this.configurationValue[this.observeAttr][this.attribute].forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), Number)[0]);
        });
      }
      if (this.configurationValue[this.observeAttr][this.telemetry]) {
        this.configurationValue[this.observeAttr][this.telemetry].forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), Number)[0]);
        });
      }
    }
    return (objectsIds.size > 0) ? Array.from(objectsIds) : null;
  }

  upDateJsonAllConfig(): void {
    this.lwm2mDeviceProfileTransportConfFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
    this.lwm2mDeviceProfileTransportConfFormGroup.markAsPristine({
      onlySelf: true
    });
  }

  addObjectsList(value: ObjectLwM2M[]): void {
    this.updateObserveAttrTelemetryObjectFormGroup(deepClone(value));
  }

  removeObjectsList(value: ObjectLwM2M): void {
    const objectsOld = deepClone(this.lwm2mDeviceProfileTransportConfFormGroup.get('observeAttrTelemetry').value.clientLwM2M);
    const isIdIndex = (element) => element.id === value.id;
    const index = objectsOld.findIndex(isIdIndex);
    if (index >= 0) {
      objectsOld.splice(index, 1);
    }
    this.updateObserveAttrTelemetryObjectFormGroup(objectsOld);
    this.removeObserveAttrTelemetryFromJson(this.observe, value.id);
    this.removeObserveAttrTelemetryFromJson(this.telemetry, value.id);
    this.removeObserveAttrTelemetryFromJson(this.attribute, value.id);
    this.removeObserveAttrTelemetryFromJson(this.attribute, value.id);
    this.removeKeyNameFromJson(value.id);
    this.upDateJsonAllConfig();
  }

  removeObserveAttrTelemetryFromJson(observeAttrTel: string, id: number): void {
    const isIdIndex = (element) => Array.from(element.substring(1).split('/'), Number)[0] === id;
    let index = this.configurationValue[this.observeAttr][observeAttrTel].findIndex(isIdIndex);
    while (index >= 0) {
      this.configurationValue[this.observeAttr][observeAttrTel].splice(index, 1);
      index = this.configurationValue[this.observeAttr][observeAttrTel].findIndex(isIdIndex);
    }
  }

  removeKeyNameFromJson(id: number): void {
    const keyNmaeJson = this.configurationValue[this.observeAttr][this.keyName];
    Object.keys(keyNmaeJson).forEach(key => {
      const idKey = Array.from(key.substring(1).split('/'), Number)[0];
      if (idKey === id) {
        delete keyNmaeJson[key];
      }
    });
  }

  isPathInJson(path: string): boolean {
    let isPath = this.findPathInJson(path, this.attribute);
    if (!isPath) {
      isPath = this.findPathInJson(path, this.telemetry);
    }
    return (isPath) ? true : false;
  }

  findPathInJson(path: string, side: string): string {
    if (this.configurationValue[this.observeAttr]) {
      if (this.configurationValue[this.observeAttr][side]) {
        return this.configurationValue[this.observeAttr][side].find(
          pathJs => pathJs === path);
      }
    }
  }
}
