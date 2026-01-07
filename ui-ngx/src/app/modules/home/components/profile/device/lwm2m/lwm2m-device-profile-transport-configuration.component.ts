///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  ATTRIBUTE,
  DEFAULT_EDRX_CYCLE,
  DEFAULT_FW_UPDATE_RESOURCE,
  DEFAULT_PAGING_TRANSMISSION_WINDOW,
  DEFAULT_PSM_ACTIVITY_TIMER,
  DEFAULT_SW_UPDATE_RESOURCE,
  Instance,
  INSTANCES,
  KEY_NAME,
  Lwm2mProfileConfigModels,
  ObjectLwM2M,
  OBSERVE,
  PowerMode,
  ObjectIDVer,
  RESOURCES,
  ServerSecurityConfig,
  TELEMETRY,
  ObjectIDVerTranslationMap,
  ObserveStrategy,
  ObserveStrategyMap
} from './lwm2m-profile-config.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { deepClone, isDefinedAndNotNull, isEmpty } from '@core/utils';
import { Direction } from '@shared/models/page/sort-order';
import _ from 'lodash';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Lwm2mSecurityType } from '@shared/models/lwm2m-security-config.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-profile-lwm2m-device-transport-configuration',
  templateUrl: './lwm2m-device-profile-transport-configuration.component.html',
  styleUrls: ['./lwm2m-device-profile-transport-configuration.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mDeviceProfileTransportConfigurationComponent),
      multi: true
    }]
})
export class Lwm2mDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, Validator, OnDestroy {

  public disabled = false;
  public isTransportWasRunWithBootstrap = true;
  public isBootstrapServerUpdateEnable: boolean;
  private destroy$ = new Subject<void>();

  lwm2mDeviceProfileFormGroup: UntypedFormGroup;
  configurationValue: Lwm2mProfileConfigModels;

  objectIDVers = Object.values(ObjectIDVer) as ObjectIDVer[];
  objectIDVerTranslationMap = ObjectIDVerTranslationMap;

  observeStrategyList = Object.values(ObserveStrategy) as ObserveStrategy[];
  observeStrategyMap = ObserveStrategyMap;

  sortFunction: (key: string, value: object) => object;

  @Input()
  isAdd: boolean;

  private propagateChange = (v: any) => {
  }

  constructor(public translate: TranslateService,
              private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private dialogService: DialogService,
              private deviceProfileService: DeviceProfileService) {
    this.lwm2mDeviceProfileFormGroup = this.fb.group({
      objectIds: [null],
      observeAttrTelemetry: [null],
      bootstrapServerUpdateEnable: [false],
      bootstrap: [[]],
      observeStrategy: [null, []],
      clientLwM2mSettings: this.fb.group({
        clientOnlyObserveAfterConnect: [1, []],
        useObject19ForOtaInfo: [false],
        fwUpdateStrategy: [1, []],
        swUpdateStrategy: [1, []],
        fwUpdateResource: [{value: '', disabled: true}, []],
        swUpdateResource: [{value: '', disabled: true}, []],
        powerMode: [PowerMode.DRX, Validators.required],
        edrxCycle: [{disabled: true, value: 0}, Validators.required],
        psmActivityTimer: [{disabled: true, value: 0}, Validators.required],
        pagingTransmissionWindow: [{disabled: true, value: 0}, Validators.required],
        defaultObjectIDVer: [ObjectIDVer.V1_0, Validators.required]
      })
    });

    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateStrategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((fwStrategy) => {
      if (fwStrategy === 2) {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').enable({emitEvent: false});
      } else {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource').disable({emitEvent: false});
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateResource')
          .reset(DEFAULT_FW_UPDATE_RESOURCE, {emitEvent: false});
      }
    });

    this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateStrategy').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((swStrategy) => {
      if (swStrategy === 2) {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').enable({emitEvent: false});
      } else {
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource').disable({emitEvent: false});
        this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateResource')
          .reset(DEFAULT_SW_UPDATE_RESOURCE, {emitEvent: false});
      }
    });

    this.lwm2mDeviceProfileFormGroup.get('bootstrapServerUpdateEnable').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      if (!value) {
        const bootstrap = this.lwm2mDeviceProfileFormGroup.get('bootstrap').value;
        const bootstrapResultArray = bootstrap.filter(server => server.bootstrapServerIs === true);
        if (bootstrapResultArray.length) {
          this.dialogService.confirm(
            this.translate.instant('device-profile.lwm2m.bootstrap-update-title'),
            this.translate.instant('device-profile.lwm2m.bootstrap-update-text'),
            this.translate.instant('action.no'),
            this.translate.instant('action.yes'),
          ).pipe(
            takeUntil(this.destroy$)
          ).subscribe((result) => {
            if (result) {
              this.isBootstrapServerUpdateEnable = value;
            } else {
              this.lwm2mDeviceProfileFormGroup.patchValue({
                bootstrapServerUpdateEnable: true
              }, {emitEvent: true});
            }
            this.cd.markForCheck();
          });
        } else {
          this.isBootstrapServerUpdateEnable = value;
        }
      } else {
        this.isBootstrapServerUpdateEnable = value;
      }
    });

    this.lwm2mDeviceProfileFormGroup.get('objectIds').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateObserveStrategy(value));

    this.lwm2mDeviceProfileFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateDeviceProfileValue(value);
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
    } else {
      this.lwm2mDeviceProfileFormGroup.enable({emitEvent: false});
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.powerMode').updateValueAndValidity({onlySelf: true});
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateStrategy').updateValueAndValidity({onlySelf: true});
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateStrategy').updateValueAndValidity({onlySelf: true});
    }
  }

  async writeValue(value: Lwm2mProfileConfigModels | null) {
    if (isDefinedAndNotNull(value) && (value?.clientLwM2mSettings || value?.observeAttr || value?.bootstrap)) {
      this.configurationValue = value;
      if (this.isAdd) {
        await this.defaultProfileConfig();
      }
      this.initWriteValue();
    }
  }

  validate(): ValidationErrors | null {
    return this.lwm2mDeviceProfileFormGroup.valid ? null : {
      lwm2mDeviceProfile: false
    };
  }

  private async defaultProfileConfig(): Promise<void> {
    let lwm2m: ServerSecurityConfig;
    let bootstrap: ServerSecurityConfig;
    [bootstrap, lwm2m] = await Promise.all([
      this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(true).toPromise(),
      this.deviceProfileService.getLwm2mBootstrapSecurityInfoBySecurityType(false).toPromise(),
    ]);
    if (lwm2m) {
      lwm2m.securityMode = Lwm2mSecurityType.NO_SEC;
    }
    this.isTransportWasRunWithBootstrap = !!bootstrap;
    this.configurationValue.bootstrap = [lwm2m];
    this.lwm2mDeviceProfileFormGroup.patchValue({
      bootstrap: this.configurationValue.bootstrap
    }, {emitEvent: true});
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
    this.lwm2mDeviceProfileFormGroup.patchValue({
        objectIds: value,
        observeAttrTelemetry: this.getObserveAttrTelemetryObjects(value),
        bootstrap: this.configurationValue.bootstrap,
        bootstrapServerUpdateEnable: this.configurationValue.bootstrapServerUpdateEnable || false,
        observeStrategy: this.configurationValue.observeAttr.observeStrategy || ObserveStrategy.SINGLE,
        clientLwM2mSettings: {
          clientOnlyObserveAfterConnect: this.configurationValue.clientLwM2mSettings.clientOnlyObserveAfterConnect,
          useObject19ForOtaInfo: this.configurationValue.clientLwM2mSettings.useObject19ForOtaInfo ?? false,
          fwUpdateStrategy: this.configurationValue.clientLwM2mSettings.fwUpdateStrategy || 1,
          swUpdateStrategy: this.configurationValue.clientLwM2mSettings.swUpdateStrategy || 1,
          fwUpdateResource: this.configurationValue.clientLwM2mSettings.fwUpdateResource || '',
          swUpdateResource: this.configurationValue.clientLwM2mSettings.swUpdateResource || '',
          powerMode: this.configurationValue.clientLwM2mSettings.powerMode || PowerMode.DRX,
          edrxCycle: this.configurationValue.clientLwM2mSettings.edrxCycle || DEFAULT_EDRX_CYCLE,
          pagingTransmissionWindow:
            this.configurationValue.clientLwM2mSettings.pagingTransmissionWindow || DEFAULT_PAGING_TRANSMISSION_WINDOW,
          psmActivityTimer: this.configurationValue.clientLwM2mSettings.psmActivityTimer || DEFAULT_PSM_ACTIVITY_TIMER,
          defaultObjectIDVer: this.configurationValue.clientLwM2mSettings.defaultObjectIDVer || ObjectIDVer.V1_0
        }
      },
      {emitEvent: false});
    this.isBootstrapServerUpdateEnable = this.configurationValue.bootstrapServerUpdateEnable || false;
    if (!this.disabled) {
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.powerMode').updateValueAndValidity({onlySelf: true});
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.fwUpdateStrategy').updateValueAndValidity({onlySelf: true});
      this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings.swUpdateStrategy').updateValueAndValidity({onlySelf: true});
    }
    this.updateObserveStrategy(value);
    this.cd.markForCheck();
  }

  private updateModel = (): void => {
    this.propagateChange(this.configurationValue);
  }

  private updateObserveAttrTelemetryObjectFormGroup = (objectsList: ObjectLwM2M[]): void => {
    this.lwm2mDeviceProfileFormGroup.patchValue({
        observeAttrTelemetry: deepClone(this.getObserveAttrTelemetryObjects(objectsList))
      },
      {emitEvent: false});
  }

  private updateDeviceProfileValue(config): void {
    if (this.lwm2mDeviceProfileFormGroup.valid && config.observeAttrTelemetry) {
      this.updateObserveAttrTelemetryFromGroupToJson(config.observeAttrTelemetry);
    }
    this.configurationValue.bootstrap = config.bootstrap;
    this.configurationValue.clientLwM2mSettings = config.clientLwM2mSettings;
    this.configurationValue.bootstrapServerUpdateEnable = config.bootstrapServerUpdateEnable;
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

  private validateKeyNameObjects = (nameJson: object, attributeArray: string[], telemetryArray: string[]): object => {
    const keyName = JSON.parse(JSON.stringify(nameJson));
    const keyNameValidate = {};
    const keyAttrTelemetry = attributeArray.concat(telemetryArray);
    Object.keys(keyName).forEach(key => {
      if (keyAttrTelemetry.includes(key)) {
        keyNameValidate[key] = keyName[key];
      }
    });
    return keyNameValidate;
  };

  private updateObserveAttrTelemetryFromGroupToJson = (val: ObjectLwM2M[]): void => {
    const observeArray: Array<string> = [];
    const attributeArray: Array<string> = [];
    const telemetryArray: Array<string> = [];
    const attributes: any = {};
    const keyNameNew = {};
    const observeStrategyValue = this.lwm2mDeviceProfileFormGroup.get('observeStrategy').value;
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
      attributeLwm2m: attributes,
      observeStrategy: observeStrategyValue
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

  addObjectsList = (value: ObjectLwM2M[]): void => {
    this.updateObserveAttrTelemetryObjectFormGroup(value);
  }

  removeObjectsList = (value: ObjectLwM2M): void => {
    const objectsOld = this.lwm2mDeviceProfileFormGroup.get('observeAttrTelemetry').value;
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
    this.lwm2mDeviceProfileFormGroup.patchValue({
      observeAttrTelemetry: deepClone(objectsOld)
    }, {emitEvent: false});
  };

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

  get clientSettingsFormGroup(): UntypedFormGroup {
    return this.lwm2mDeviceProfileFormGroup.get('clientLwM2mSettings') as UntypedFormGroup;
  }

  private updateObserveStrategy(value: ObjectLwM2M[]) {
    if (value.length && !this.disabled) {
      this.lwm2mDeviceProfileFormGroup.get('observeStrategy').enable({onlySelf: true});
    } else {
      this.lwm2mDeviceProfileFormGroup.get('observeStrategy').disable({onlySelf: true});
    }
  }

}
