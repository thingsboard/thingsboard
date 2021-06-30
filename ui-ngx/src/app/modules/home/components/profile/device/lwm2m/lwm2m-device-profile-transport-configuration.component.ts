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
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  ATTRIBUTE,
  BingingMode,
  BingingModeTranslationsMap,
  DEFAULT_BINDING,
  DEFAULT_FW_UPDATE_RESOURCE,
  DEFAULT_ID_SERVER,
  DEFAULT_LIFE_TIME,
  DEFAULT_MIN_PERIOD,
  DEFAULT_NOTIF_IF_DESIBLED,
  DEFAULT_SW_UPDATE_RESOURCE,
  getDefaultBootstrapServerSecurityConfig,
  getDefaultBootstrapServersSecurityConfig,
  getDefaultLwM2MServerSecurityConfig,
  getDefaultProfileClientLwM2mSettingsConfig,
  getDefaultProfileObserveAttrConfig,
  Instance,
  INSTANCES,
  KEY_NAME,
  Lwm2mProfileConfigModels,
  ObjectLwM2M,
  OBSERVE,
  OBSERVE_ATTR_TELEMETRY,
  PowerMode,
  PowerModeTranslationMap,
  RESOURCES,
  ServerSecurityConfig,
  TELEMETRY
} from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { deepClone, isDefinedAndNotNull, isEmpty } from '@core/utils';
import { JsonArray, JsonObject } from '@angular/compiler-cli/ngcc/src/packages/entry_point';
import { Direction } from '@shared/models/page/sort-order';
import _ from 'lodash';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-profile-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-profile-transport-configuration.component.html',
  styleUrls: ['./lwm2m-device-profile-transport-configuration.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class Lwm2mDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, Validators, OnDestroy {

  private configurationValue: Lwm2mProfileConfigModels;
  private requiredValue: boolean;
  private disabled = false;
  private destroy$ = new Subject();

  bindingModeTypes = Object.values(BingingMode);
  bindingModeTypeNamesMap = BingingModeTranslationsMap;
  lwm2mDeviceProfileFormGroup: FormGroup;
  lwm2mDeviceConfigFormGroup: FormGroup;
  sortFunction: (key: string, value: object) => object;
  isFwUpdateStrategy: boolean;
  isSwUpdateStrategy: boolean;
  powerMods = Object.values(PowerMode);
  powerModeTranslationMap = PowerModeTranslationMap;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private propagateChange = (v: any) => {
  }

  constructor(private fb: FormBuilder,
              private deviceProfileService: DeviceProfileService) {
    this.lwm2mDeviceProfileFormGroup = this.fb.group({
      objectIds: [null, Validators.required],
      observeAttrTelemetry: [null, Validators.required],
      bootstrap: this.fb.group({
        servers: this.fb.group({
          binding: [DEFAULT_BINDING],
          shortId: [DEFAULT_ID_SERVER, [Validators.required, Validators.min(1), Validators.max(65534), Validators.pattern('[0-9]*')]],
          lifetime: [DEFAULT_LIFE_TIME, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
          notifIfDisabled: [DEFAULT_NOTIF_IF_DESIBLED, []],
          defaultMinPeriod: [DEFAULT_MIN_PERIOD, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
        }),
        bootstrapServer: [null, Validators.required],
        lwm2mServer: [null, Validators.required]
      }),
      clientLwM2mSettings: this.fb.group({
        clientOnlyObserveAfterConnect: [1, []],
        fwUpdateStrategy: [1, []],
        swUpdateStrategy: [1, []],
        fwUpdateResource: [{value: '', disabled: true}, []],
        swUpdateResource: [{value: '', disabled: true}, []],
        powerMode: [PowerMode.DRX, Validators.required],
        compositeOperationsSupport: [false]
      })
    });
    this.lwm2mDeviceConfigFormGroup = this.fb.group({
      configurationJson: [null, Validators.required]
    });
    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateStrategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((fwStrategy) => {
      if (fwStrategy === 2) {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').enable({emitEvent: false});
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource')
          .patchValue(DEFAULT_FW_UPDATE_RESOURCE, {emitEvent: false});
        this.isFwUpdateStrategy = true;
      } else {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').disable({emitEvent: false});
        this.isFwUpdateStrategy = false;
      }
      this.otaUpdateFwStrategyValidate(true);
    });
    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateStrategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((swStrategy) => {
      if (swStrategy === 2) {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').enable({emitEvent: false});
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource')
          .patchValue(DEFAULT_SW_UPDATE_RESOURCE, {emitEvent: false});
        this.isSwUpdateStrategy = true;
      } else {
        this.isSwUpdateStrategy = false;
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').disable({emitEvent: false});
      }
      this.otaUpdateSwStrategyValidate(true);
    });
    this.lwm2mDeviceProfileFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateDeviceProfileValue(value);
    });
    this.lwm2mDeviceConfigFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
    this.sortFunction = this.sortObjectKeyPathJson;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
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

  async writeValue(value: Lwm2mProfileConfigModels | null) {
    if (isDefinedAndNotNull(value)) {
      if (value?.clientLwM2mSettings || value?.observeAttr || value?.bootstrap) {
        this.configurationValue = value;
      } else {
        this.configurationValue = await this.defaultProfileConfig();
      }
      this.lwm2mDeviceConfigFormGroup.patchValue({
        configurationJson: this.configurationValue
      }, {emitEvent: false});
      this.initWriteValue();
    }
  }

  private async defaultProfileConfig(): Promise<Lwm2mProfileConfigModels> {
    let bootstrap: ServerSecurityConfig;
    let lwm2m: ServerSecurityConfig;
    try {
      [bootstrap, lwm2m] = await Promise.all([
        this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(true).toPromise(),
        this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(false).toPromise()
      ]);
    } catch (e) {
      bootstrap = getDefaultBootstrapServerSecurityConfig();
      lwm2m = getDefaultLwM2MServerSecurityConfig();
    }
    return {
      observeAttr: getDefaultProfileObserveAttrConfig(),
      bootstrap: {
        servers: getDefaultBootstrapServersSecurityConfig(),
        bootstrapServer: bootstrap,
        lwm2mServer: lwm2m
      },
      clientLwM2mSettings: getDefaultProfileClientLwM2mSettingsConfig()
    };
  }

  private initWriteValue = (): void => {
    const objectIds = this.getObjectsFromJsonAllConfig();
    if (objectIds.length > 0) {
      const sortOrder = {
        property: 'id',
        direction: Direction.ASC
      };
      this.deviceProfileService.getLwm2mObjects(sortOrder, objectIds, null).subscribe(
        (objectsList) => {
          this.updateWriteValue(objectsList);
        }
      );
    } else {
      this.updateWriteValue([]);
    }
  }

  private updateWriteValue = (value: ObjectLwM2M[]): void => {
    const fwResource = isDefinedAndNotNull(this.configurationValue.clientLwM2mSettings.fwUpdateResource) ?
      this.configurationValue.clientLwM2mSettings.fwUpdateResource : '';
    const swResource = isDefinedAndNotNull(this.configurationValue.clientLwM2mSettings.swUpdateResource) ?
      this.configurationValue.clientLwM2mSettings.swUpdateResource : '';
    this.lwm2mDeviceProfileFormGroup.patchValue({
        objectIds: value,
        observeAttrTelemetry: this.getObserveAttrTelemetryObjects(value),
        bootstrap: this.configurationValue.bootstrap,
        clientLwM2mSettings: {
          clientOnlyObserveAfterConnect: this.configurationValue.clientLwM2mSettings.clientOnlyObserveAfterConnect,
          fwUpdateStrategy: this.configurationValue.clientLwM2mSettings.fwUpdateStrategy || 1,
          swUpdateStrategy: this.configurationValue.clientLwM2mSettings.swUpdateStrategy || 1,
          fwUpdateResource: fwResource,
          swUpdateResource: swResource,
          powerMode: this.configurationValue.clientLwM2mSettings.powerMode || PowerMode.DRX,
          compositeOperationsSupport: this.configurationValue.clientLwM2mSettings.compositeOperationsSupport || false
        }
      },
      {emitEvent: false});
    this.configurationValue.clientLwM2mSettings.fwUpdateResource = fwResource;
    this.configurationValue.clientLwM2mSettings.swUpdateResource = swResource;
    this.isFwUpdateStrategy = this.configurationValue.clientLwM2mSettings.fwUpdateStrategy === 2;
    this.isSwUpdateStrategy = this.configurationValue.clientLwM2mSettings.swUpdateStrategy === 2;
    this.otaUpdateSwStrategyValidate();
    this.otaUpdateFwStrategyValidate();
  }

  private updateModel = (): void => {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.lwm2mDeviceConfigFormGroup.valid && this.lwm2mDeviceProfileFormGroup.valid) {
      configuration = this.lwm2mDeviceConfigFormGroup.value.configurationJson;
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
      this.updateObserveAttrTelemetryFromGroupToJson(config.observeAttrTelemetry);
    }
    this.configurationValue.bootstrap.bootstrapServer = config.bootstrap.bootstrapServer;
    this.configurationValue.bootstrap.lwm2mServer = config.bootstrap.lwm2mServer;
    this.configurationValue.bootstrap.servers = config.bootstrap.servers;
    this.configurationValue.clientLwM2mSettings = config.clientLwM2mSettings;
    this.upDateJsonAllConfig();
    this.updateModel();
  }

  private getObserveAttrTelemetryObjects = (objectList: ObjectLwM2M[]): ObjectLwM2M[] => {
    const objectLwM2MS = deepClone(objectList);
    if (this.configurationValue.observeAttr && objectLwM2MS.length > 0) {
      const attributeArray = this.configurationValue.observeAttr.attribute;
      const telemetryArray = this.configurationValue.observeAttr.telemetry;
      const keyNameJson = this.configurationValue.observeAttr.keyName;
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
        this.updateAttributes(objectLwM2MS);
      }
      if (isDefinedAndNotNull(keyNameJson)) {
        this.configurationValue.observeAttr.keyName = this.validateKeyNameObjects(keyNameJson, attributeArray, telemetryArray);
        this.upDateJsonAllConfig();
        this.updateKeyNameObjects(objectLwM2MS);
      }
    }
    return objectLwM2MS;
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
        const instance = this.updateInInstanceKeyName(objectLwM2M.instances[0], +pathParameter[1]);
        objectLwM2M.instances.push(instance);
      }
    });
  }

  private updateInInstanceKeyName = (instance: Instance, instanceId: number): Instance => {
    const instanceUpdate = deepClone(instance);
    instanceUpdate.id = instanceId;
    instanceUpdate.resources.forEach(resource => {
      resource.keyName = _.camelCase(resource.name + instanceUpdate.id);
    });
    return instanceUpdate;
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

  private updateAttributes = (objectLwM2MS: ObjectLwM2M[]): void => {
    Object.keys(this.configurationValue.observeAttr.attributeLwm2m).forEach(key => {
      const [objectKeyId, instanceId, resourceId] = Array.from(key.substring(1).split('/'), String);
      const objectLwM2M = objectLwM2MS.find(objectLwm2m => objectLwm2m.keyId === objectKeyId);
      if (objectLwM2M && instanceId) {
        const instance = objectLwM2M.instances.find(obj => obj.id === +instanceId);
        if (instance && resourceId) {
          instance.resources.find(resource => resource.id === +resourceId)
            .attributes = this.configurationValue.observeAttr.attributeLwm2m[key];
        } else if (instance) {
          instance.attributes = this.configurationValue.observeAttr.attributeLwm2m[key];
        }
      } else if (objectLwM2M) {
        objectLwM2M.attributes = this.configurationValue.observeAttr.attributeLwm2m[key];
      }
    });
  }

  private updateKeyNameObjects = (objectLwM2MS: ObjectLwM2M[]): void => {
    Object.keys(this.configurationValue.observeAttr.keyName).forEach(key => {
      const [objectKeyId, instanceId, resourceId] = Array.from(key.substring(1).split('/'), String);
      const objectLwM2M = objectLwM2MS.find(objectLwm2m => objectLwm2m.keyId === objectKeyId);
      if (objectLwM2M) {
        objectLwM2M.instances.find(instance => instance.id === +instanceId)
          .resources.find(resource => resource.id === +resourceId)
          .keyName = this.configurationValue.observeAttr.keyName[key];
      }
    });
  }

  private validateKeyNameObjects = (nameJson: JsonObject, attributeArray: JsonArray, telemetryArray: JsonArray): {} => {
    const keyName = JSON.parse(JSON.stringify(nameJson));
    const keyNameValidate = {};
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
    const attributes: any = {};
    const keyNameNew = {};
    const observeJson: ObjectLwM2M[] = JSON.parse(JSON.stringify(val));
    observeJson.forEach(obj => {
      if (isDefinedAndNotNull(obj.attributes) && !isEmpty(obj.attributes)) {
        const pathObject = `/${obj.keyId}`;
        attributes[pathObject] = obj.attributes;
      }
      if (obj.hasOwnProperty(INSTANCES) && Array.isArray(obj.instances)) {
        obj.instances.forEach(instance => {
          if (isDefinedAndNotNull(instance.attributes) && !isEmpty(instance.attributes)) {
            const pathInstance = `/${obj.keyId}/${instance.id}`;
            attributes[pathInstance] = instance.attributes;
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
                if (isDefinedAndNotNull(resource.attributes) && !isEmpty(resource.attributes)) {
                  attributes[pathRes] = resource.attributes;
                }
              }
            });
          }
        });
      }
    });
    this.configurationValue.observeAttr = {
      observe: observeArray,
      attribute: attributeArray,
      telemetry: telemetryArray,
      keyName: this.sortObjectKeyPathJson(KEY_NAME, keyNameNew),
      attributeLwm2m: attributes
    };
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
    this.removeAttributesFromJson(value.keyId);
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

  private removeAttributesFromJson = (keyId: string): void => {
    const keyNameJson = this.configurationValue.observeAttr.attributeLwm2m;
    Object.keys(keyNameJson).forEach(key => {
      if (key.startsWith(`/${keyId}`)) {
        delete keyNameJson[key];
      }
    });
  }

  private otaUpdateFwStrategyValidate(updated = false): void {
    if (this.isFwUpdateStrategy) {
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').setValidators([Validators.required]);
    } else {
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').clearValidators();
    }
    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').updateValueAndValidity({emitEvent: updated});
  }

  private otaUpdateSwStrategyValidate(updated = false): void {
    if (this.isSwUpdateStrategy) {
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').setValidators([Validators.required]);
    } else {
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').clearValidators();
    }
    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').updateValueAndValidity({emitEvent: updated});
  }

}
