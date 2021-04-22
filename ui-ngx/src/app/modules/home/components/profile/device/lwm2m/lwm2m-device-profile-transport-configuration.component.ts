///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { DeviceProfileTransportConfiguration } from '@shared/models/device.models';
import { Component, forwardRef, Inject, Input } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  ATTRIBUTE,
  BINDING_MODE,
  BINDING_MODE_NAMES,
  getDefaultProfileConfig,
  INSTANCES,
  KEY_NAME,
  Lwm2mProfileConfigModels,
  ModelValue,
  ObjectLwM2M,
  OBSERVE,
  OBSERVE_ATTR_TELEMETRY,
  RESOURCES,
  TELEMETRY
} from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { deepClone, isDefinedAndNotNull, isEmpty, isUndefined } from '@core/utils';
import { WINDOW } from '@core/services/window.service';
import { JsonArray, JsonObject } from '@angular/compiler-cli/ngcc/src/packages/entry_point';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-profile-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-profile-transport-configuration.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class Lwm2mDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, Validators {

  private configurationValue: Lwm2mProfileConfigModels;
  private requiredValue: boolean;
  private disabled = false;

  bindingModeType = BINDING_MODE;
  bindingModeTypes = Object.keys(BINDING_MODE);
  bindingModeTypeNamesMap = BINDING_MODE_NAMES;
  lwm2mDeviceProfileFormGroup: FormGroup;
  lwm2mDeviceConfigFormGroup: FormGroup;
  bootstrapServers: string;
  bootstrapServer: string;
  lwm2mServer: string;
  sortFunction: (key: string, value: object) => object;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => {
  }

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService,
              @Inject(WINDOW) private window: Window) {
    this.lwm2mDeviceProfileFormGroup = this.fb.group({
      clientOnlyObserveAfterConnect: [1, []],
      objectIds: [null, Validators.required],
      observeAttrTelemetry: [null, Validators.required],
      shortId: [null, Validators.required],
      lifetime: [null, Validators.required],
      defaultMinPeriod: [null, Validators.required],
      notifIfDisabled: [true, []],
      binding:[],
      bootstrapServer: [null, Validators.required],
      lwm2mServer: [null, Validators.required],
    });
    this.lwm2mDeviceConfigFormGroup = this.fb.group({
      configurationJson: [null, Validators.required]
    });
    this.lwm2mDeviceProfileFormGroup.valueChanges.subscribe((value) => {
      this.updateDeviceProfileValue(value);
    });
    this.lwm2mDeviceConfigFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.sortFunction = this.sortObjectKeyPathJson;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.lwm2mDeviceProfileFormGroup.disable({emitEvent: false});
      this.lwm2mDeviceConfigFormGroup.disable({emitEvent: false});
    } else {
      this.lwm2mDeviceProfileFormGroup.enable({emitEvent: false});
      this.lwm2mDeviceConfigFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Lwm2mProfileConfigModels | null): void {
    this.configurationValue = (Object.keys(value).length === 0) ? getDefaultProfileConfig() : value;
    this.lwm2mDeviceConfigFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
    this.initWriteValue();
  }

  private initWriteValue = (): void => {
    const modelValue = {objectIds: [], objectsList: []} as ModelValue;
    modelValue.objectIds = this.getObjectsFromJsonAllConfig();
    if (modelValue.objectIds.length > 0) {
      const sortOrder = {
        property: 'id',
        direction: Direction.ASC
      };
      this.deviceProfileService.getLwm2mObjects(sortOrder, modelValue.objectIds, null).subscribe(
        (objectsList) => {
          modelValue.objectsList = objectsList;
          this.updateWriteValue(modelValue);
        }
      );
    } else {
      this.updateWriteValue(modelValue);
    }
  }

  private updateWriteValue = (value: ModelValue): void => {
    this.lwm2mDeviceProfileFormGroup.patchValue({
        clientOnlyObserveAfterConnect: this.configurationValue.clientLwM2mSettings.clientOnlyObserveAfterConnect,
        objectIds: value,
        observeAttrTelemetry: this.getObserveAttrTelemetryObjects(value['objectsList']),
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

  private updateModel = (): void => {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.lwm2mDeviceConfigFormGroup.valid && this.lwm2mDeviceProfileFormGroup.valid) {
      configuration = this.lwm2mDeviceConfigFormGroup.value.configurationJson;
      // configuration.type = DeviceTransportType.LWM2M;
    }
    this.propagateChange(configuration);
  }

  private updateObserveAttrTelemetryObjectFormGroup = (objectsList: ObjectLwM2M[]): void => {
    this.lwm2mDeviceProfileFormGroup.patchValue({
        observeAttrTelemetry: deepClone(this.getObserveAttrTelemetryObjects(objectsList))
      },
      {emitEvent: false});
  }

  private updateDeviceProfileValue(config): void {
    if (this.lwm2mDeviceProfileFormGroup.valid) {
      this.configurationValue.clientLwM2mSettings.clientOnlyObserveAfterConnect =
        config.clientOnlyObserveAfterConnect;
      this.updateObserveAttrTelemetryFromGroupToJson(config.observeAttrTelemetry.clientLwM2M);
      this.configurationValue.bootstrap.bootstrapServer = config.bootstrapServer;
      this.configurationValue.bootstrap.lwm2mServer = config.lwm2mServer;
      const bootstrapServers = this.configurationValue.bootstrap.servers;
      bootstrapServers.shortId = config.shortId;
      bootstrapServers.lifetime = config.lifetime;
      bootstrapServers.defaultMinPeriod = config.defaultMinPeriod;
      bootstrapServers.notifIfDisabled = config.notifIfDisabled;
      bootstrapServers.binding = config.binding;
      this.upDateJsonAllConfig();
      this.updateModel();
    }
  }

  private getObserveAttrTelemetryObjects = (objectList: ObjectLwM2M[]): object => {
    const objectLwM2MS = deepClone(objectList);
    if (this.configurationValue.observeAttr && objectLwM2MS.length > 0) {
      const attributeArray = this.configurationValue.observeAttr.attribute;
      const telemetryArray = this.configurationValue.observeAttr.telemetry;
      let keyNameJson = this.configurationValue.observeAttr.keyName;
      if (this.includesNotZeroInstance(attributeArray, telemetryArray)) {
        this.addInstances(attributeArray, telemetryArray, objectLwM2MS);
      }
      if (isDefinedAndNotNull(this.configurationValue.observeAttr.observe) &&
        this.configurationValue.observeAttr.observe.length > 0) {
        this.updateObserveAttrTelemetryObjects(this.configurationValue.observeAttr.observe, objectLwM2MS, OBSERVE);
      }
      if (isDefinedAndNotNull(attributeArray) && attributeArray.length > 0) {
        this.updateObserveAttrTelemetryObjects(attributeArray, objectLwM2MS, ATTRIBUTE);
      }
      if (isDefinedAndNotNull(telemetryArray) && telemetryArray.length > 0) {
        this.updateObserveAttrTelemetryObjects(telemetryArray, objectLwM2MS, TELEMETRY);
      }
      if (isDefinedAndNotNull(this.configurationValue.observeAttr.attributeLwm2m)) {
        this.updateAttributeLwm2m(objectLwM2MS);
      }
      if (isDefinedAndNotNull(keyNameJson)) {
        this.configurationValue.observeAttr.keyName = this.validateKeyNameObjects(keyNameJson, attributeArray, telemetryArray);
        this.upDateJsonAllConfig();
        this.updateKeyNameObjects(objectLwM2MS);
      }
    }
    return {clientLwM2M: objectLwM2MS};
  }

  private includesNotZeroInstance = (attribute: string[], telemetry: string[]): boolean => {
    const isNotZeroInstanceId = (instance) => !instance.includes('/0/');
    return attribute.some(isNotZeroInstanceId) || telemetry.some(isNotZeroInstanceId);
  }

  private addInstances = (attribute: string[], telemetry: string[], clientObserveAttrTelemetry: ObjectLwM2M[]): void => {
    const instancesPath = attribute.concat(telemetry)
      .filter(instance => !instance.includes('/0/'))
      .map(instance => instance.slice(1, instance.lastIndexOf('/')))
      .sort(this.sortPath);

    new Set(instancesPath).forEach(path => {
      const pathParameter = Array.from(path.split('/'), String);
      const objectLwM2M = clientObserveAttrTelemetry.find(x => x.keyId === pathParameter[0]);
      if (objectLwM2M) {
        const instance = deepClone(objectLwM2M.instances[0]);
        instance.id = +pathParameter[1];
        objectLwM2M.instances.push(instance);
      }
    });
  }

  private updateObserveAttrTelemetryObjects = (parameters: string[], objectLwM2MS: ObjectLwM2M[],
                                               nameParameter: string): void => {
    parameters.forEach(parameter => {
      const [objectKeyId, instanceId, resourceId] = Array.from(parameter.substring(1).split('/'), String);
      const objectLwM2M = objectLwM2MS.find(objectLwm2m => objectLwm2m.keyId === objectKeyId);
      if (objectLwM2M) {
        objectLwM2M.instances.find(itrInstance => itrInstance.id === +instanceId)
          .resources.find(resource => resource.id === +resourceId)
          [nameParameter] = true;
      }
    });
  }

  private updateAttributeLwm2m = (objectLwM2MS: ObjectLwM2M[]): void => {
    Object.keys(this.configurationValue.observeAttr.attributeLwm2m).forEach(key => {
      const [objectKeyId, instanceId, resourceId] = Array.from(key.substring(1).split('/'), String);
      let objectLwM2M = objectLwM2MS.find(objectLwm2m => objectLwm2m.keyId === objectKeyId);
      if (objectLwM2M && instanceId) {
        let instance = objectLwM2M.instances.find(instance => instance.id === +instanceId)
        if (instance && resourceId) {
          instance.resources.find(resource => resource.id === +resourceId)
            .attributeLwm2m = this.configurationValue.observeAttr.attributeLwm2m[key];
        } else if (instance) {
          instance.attributeLwm2m = this.configurationValue.observeAttr.attributeLwm2m[key];
        }
      } else if (objectLwM2M) {
        objectLwM2M.attributeLwm2m = this.configurationValue.observeAttr.attributeLwm2m[key];
      }
    });
  }

  private updateKeyNameObjects = (objectLwM2MS: ObjectLwM2M[]): void => {
    Object.keys(this.configurationValue.observeAttr.keyName).forEach(key => {
      const [objectKeyId, instanceId, resourceId] = Array.from(key.substring(1).split('/'), String);
      const objectLwM2M = objectLwM2MS.find(objectLwm2m => objectLwm2m.keyId === objectKeyId)
      if (objectLwM2M) {
        objectLwM2M.instances.find(instance => instance.id === +instanceId)
          .resources.find(resource => resource.id === +resourceId)
          .keyName = this.configurationValue.observeAttr.keyName[key];
      }
    });
  }

  private validateKeyNameObjects = (nameJson: JsonObject, attributeArray: JsonArray, telemetryArray: JsonArray): {} => {
    const keyName = JSON.parse(JSON.stringify(nameJson));
    let keyNameValidate = {};
    const keyAttrTelemetry = attributeArray.concat(telemetryArray);
    Object.keys(keyName).forEach(key => {
      if (keyAttrTelemetry.includes(key)) {
        keyNameValidate[key] = keyName[key];
      }
    });
    return keyNameValidate;
  }

  private updateObserveAttrTelemetryFromGroupToJson = (val: ObjectLwM2M[]): void => {
    const observeArray: Array<string> = [];
    const attributeArray: Array<string> = [];
    const telemetryArray: Array<string> = [];
    const attributeLwm2m: any = {};
    const keyNameNew = {};
    const observeJson: ObjectLwM2M[] = JSON.parse(JSON.stringify(val));
    observeJson.forEach(obj => {
      if (isDefinedAndNotNull(obj.attributeLwm2m) && !isEmpty(obj.attributeLwm2m)) {
        const pathObject = `/${obj.keyId}`;
        attributeLwm2m[pathObject] = obj.attributeLwm2m;
      }
      if (obj.hasOwnProperty(INSTANCES) && Array.isArray(obj.instances)) {
        obj.instances.forEach(instance => {
          if (isDefinedAndNotNull(instance.attributeLwm2m) && !isEmpty(instance.attributeLwm2m)) {
            const pathInstance = `/${obj.keyId}/${instance.id}`;
            attributeLwm2m[pathInstance] = instance.attributeLwm2m;
          }
          if (instance.hasOwnProperty(RESOURCES) && Array.isArray(instance.resources)) {
            instance.resources.forEach(resource => {
              if (resource.attribute || resource.telemetry) {
                const pathRes = `/${obj.keyId}/${instance.id}/${resource.id}`;
                if (resource.observe) {
                  observeArray.push(pathRes);
                }
                if (resource.attribute) {
                  attributeArray.push(pathRes);
                }
                if (resource.telemetry) {
                  telemetryArray.push(pathRes);
                }
                keyNameNew[pathRes] = resource.keyName;
                if (isDefinedAndNotNull(resource.attributeLwm2m) && !isEmpty(resource.attributeLwm2m)) {
                  attributeLwm2m[pathRes] = resource.attributeLwm2m;
                }
              }
            })
          }
        })
      }
    });
    if (isUndefined(this.configurationValue.observeAttr)) {
      this.configurationValue.observeAttr = {
        observe: observeArray,
        attribute: attributeArray,
        telemetry: telemetryArray,
        keyName: this.sortObjectKeyPathJson(KEY_NAME, keyNameNew),
        attributeLwm2m: attributeLwm2m
      };
    } else {
      this.configurationValue.observeAttr.observe = observeArray;
      this.configurationValue.observeAttr.attribute = attributeArray;
      this.configurationValue.observeAttr.telemetry = telemetryArray;
      this.configurationValue.observeAttr.keyName = this.sortObjectKeyPathJson(KEY_NAME, keyNameNew);
      this.configurationValue.observeAttr.attributeLwm2m = attributeLwm2m;
    }
  }

  sortObjectKeyPathJson = (key: string, value: object): object => {
    if (key === KEY_NAME) {
      return Object.keys(value).sort(this.sortPath).reduce((obj, keySort) => {
        obj[keySort] = value[keySort];
        return obj;
      }, {});
    } else if (key === OBSERVE || key === ATTRIBUTE || key === TELEMETRY) {
      return Object.values(value).sort(this.sortPath).reduce((arr, arrValue) => {
        arr.push(arrValue);
        return arr;
      }, []);
    } else {
      return value;
    }
  }

  private sortPath = (a, b): number => {
    return a.localeCompare(b, undefined, {
      numeric: true,
      sensitivity: 'base'
    });
  }

  private getObjectsFromJsonAllConfig = (): string[] => {
    const objectsIds = new Set<string>();
    if (this.configurationValue.observeAttr) {
      if (this.configurationValue.observeAttr.observe) {
        this.configurationValue.observeAttr.observe.forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), String)[0]);
        });
      }
      if (this.configurationValue.observeAttr.attribute) {
        this.configurationValue.observeAttr.attribute.forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), String)[0]);
        });
      }
      if (this.configurationValue.observeAttr.telemetry) {
        this.configurationValue.observeAttr.telemetry.forEach(obj => {
          objectsIds.add(Array.from(obj.substring(1).split('/'), String)[0]);
        });
      }
    }
    return (objectsIds.size > 0) ? Array.from(objectsIds) : [];
  }

  private upDateJsonAllConfig = (): void => {
    this.lwm2mDeviceConfigFormGroup.patchValue({
      configurationJson: this.configurationValue
    }, {emitEvent: false});
  }

  addObjectsList = (value: ObjectLwM2M[]): void => {
    this.updateObserveAttrTelemetryObjectFormGroup(value);
  }

  removeObjectsList = (value: ObjectLwM2M): void => {
    const objectsOld = this.lwm2mDeviceProfileFormGroup.get(OBSERVE_ATTR_TELEMETRY).value.clientLwM2M;
    const isIdIndex = (element) => element.keyId === value.keyId;
    const index = objectsOld.findIndex(isIdIndex);
    if (index >= 0) {
      objectsOld.splice(index, 1);
    }
    this.removeObserveAttrTelemetryFromJson(OBSERVE, value.keyId);
    this.removeObserveAttrTelemetryFromJson(TELEMETRY, value.keyId);
    this.removeObserveAttrTelemetryFromJson(ATTRIBUTE, value.keyId);
    this.removeKeyNameFromJson(value.keyId);
    this.removeAttributeLwm2mFromJson(value.keyId);
    this.updateObserveAttrTelemetryObjectFormGroup(objectsOld);
    this.upDateJsonAllConfig();
  }

  private removeObserveAttrTelemetryFromJson = (observeAttrTel: string, keyId: string): void => {
    const isIdIndex = (element) => element.startsWith(`/${keyId}`);
    let index = this.configurationValue.observeAttr[observeAttrTel].findIndex(isIdIndex);
    while (index >= 0) {
      this.configurationValue.observeAttr[observeAttrTel].splice(index, 1);
      index = this.configurationValue.observeAttr[observeAttrTel].findIndex(isIdIndex, index);
    }
  }

  private removeKeyNameFromJson = (keyId: string): void => {
    const keyNameJson = this.configurationValue.observeAttr.keyName;
    Object.keys(keyNameJson).forEach(key => {
      if (key.startsWith(`/${keyId}`)) {
        delete keyNameJson[key];
      }
    });
  }

  private removeAttributeLwm2mFromJson = (keyId: string): void => {
    debugger
    const keyNameJson = this.configurationValue.observeAttr.attributeLwm2m;
    Object.keys(keyNameJson).forEach(key => {
      if (key.startsWith(`/${keyId}`)) {
        delete keyNameJson[key];
      }
    });
  }
}
