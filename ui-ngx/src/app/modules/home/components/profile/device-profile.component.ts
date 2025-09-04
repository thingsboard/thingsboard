///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, DestroyRef, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileData,
  DeviceProfileType,
  deviceProfileTypeConfigurationInfoMap,
  deviceProfileTypeTranslationMap,
  DeviceProvisionConfiguration,
  DeviceProvisionType,
  DeviceTransportType,
  deviceTransportTypeConfigurationInfoMap,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { ServiceType } from '@shared/models/queue.models';
import { EntityId } from '@shared/models/id/entity-id';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-device-profile',
  templateUrl: './device-profile.component.html',
  styleUrls: []
})
export class DeviceProfileComponent extends EntityComponent<DeviceProfile> {

  @Input()
  standalone = false;

  entityType = EntityType;

  deviceProfileTypes = Object.values(DeviceProfileType);

  deviceProfileTypeTranslations = deviceProfileTypeTranslationMap;

  deviceTransportTypes = Object.values(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  displayProfileConfiguration: boolean;

  displayTransportConfiguration: boolean;

  isTransportTypeChanged = false;

  serviceType = ServiceType.TB_RULE_ENGINE;

  edgeRuleChainType = RuleChainType.EDGE;

  deviceProfileId: EntityId;

  otaUpdateType = OtaUpdateType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: DeviceProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<DeviceProfile>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: DeviceProfile): UntypedFormGroup {
    this.deviceProfileId = entity?.id ? entity.id : null;
    this.displayProfileConfiguration = entity && entity.type &&
      deviceProfileTypeConfigurationInfoMap.get(entity.type).hasProfileConfiguration;
    this.displayTransportConfiguration = entity && entity.transportType &&
      deviceTransportTypeConfigurationInfoMap.get(entity.transportType).hasProfileConfiguration;
    const deviceProvisionConfiguration: DeviceProvisionConfiguration = {
      type: entity?.provisionType ? entity.provisionType : DeviceProvisionType.DISABLED,
      provisionDeviceKey: entity?.provisionDeviceKey,
      provisionDeviceSecret: entity?.profileData?.provisionConfiguration?.provisionDeviceSecret,
      certificateRegExPattern: entity?.profileData?.provisionConfiguration?.certificateRegExPattern,
      allowCreateNewDevicesByX509Certificate: entity?.profileData?.provisionConfiguration?.allowCreateNewDevicesByX509Certificate
    };
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity ? entity.type : null, [Validators.required]],
        image: [entity ? entity.image : null],
        transportType: [entity ? entity.transportType : null, [Validators.required]],
        profileData: this.fb.group({
          configuration: [entity && !this.isAdd ? entity.profileData?.configuration : {}, Validators.required],
          transportConfiguration: [entity && !this.isAdd ? entity.profileData?.transportConfiguration : {}, Validators.required],
          alarms: [entity && !this.isAdd ? entity.profileData?.alarms : []],
          provisionConfiguration: [deviceProvisionConfiguration, Validators.required]
        }),
        defaultRuleChainId: [entity && entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null, []],
        defaultDashboardId: [entity && entity.defaultDashboardId ? entity.defaultDashboardId.id : null, []],
        defaultQueueName: [entity ? entity.defaultQueueName : null, []],
        defaultEdgeRuleChainId: [entity && entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null, []],
        firmwareId: [entity ? entity.firmwareId : null],
        softwareId: [entity ? entity.softwareId : null],
        description: [entity ? entity.description : '', []],
      }
    );
    form.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.deviceProfileTypeChanged(form);
    });
    form.get('transportType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.deviceProfileTransportTypeChanged(form);
    });
    this.checkIsNewDeviceProfile(entity, form);
    return form;
  }

  private checkIsNewDeviceProfile(entity: DeviceProfile, form: UntypedFormGroup) {
    if (entity && !entity.id) {
      form.get('type').patchValue(DeviceProfileType.DEFAULT, {emitEvent: true});
      form.get('transportType').patchValue(DeviceTransportType.DEFAULT, {emitEvent: true});
      form.get('provisionType').patchValue(DeviceProvisionType.DISABLED, {emitEvent: true});
    }
  }

  private deviceProfileTypeChanged(form: UntypedFormGroup) {
    const deviceProfileType: DeviceProfileType = form.get('type').value;
    this.displayProfileConfiguration = deviceProfileType &&
      deviceProfileTypeConfigurationInfoMap.get(deviceProfileType).hasProfileConfiguration;
    let profileData: DeviceProfileData = form.getRawValue().profileData;
    if (!profileData) {
      profileData = {
        configuration: null,
        transportConfiguration: null
      };
    }
    profileData.configuration = createDeviceProfileConfiguration(deviceProfileType);
    form.patchValue({profileData});
  }

  private deviceProfileTransportTypeChanged(form: UntypedFormGroup) {
    const deviceTransportType: DeviceTransportType = form.get('transportType').value;
    this.displayTransportConfiguration = deviceTransportType &&
      deviceTransportTypeConfigurationInfoMap.get(deviceTransportType).hasProfileConfiguration;
    this.isTransportTypeChanged = true;
    let profileData: DeviceProfileData = form.getRawValue().profileData;
    if (!profileData) {
      profileData = {
        configuration: null,
        transportConfiguration: null
      };
    }
    profileData.transportConfiguration = createDeviceProfileTransportConfiguration(deviceTransportType);
    form.patchValue({profileData});
  }

  updateForm(entity: DeviceProfile) {
    this.deviceProfileId = entity.id;
    this.displayProfileConfiguration = entity.type &&
      deviceProfileTypeConfigurationInfoMap.get(entity.type).hasProfileConfiguration;
    this.displayTransportConfiguration = entity.transportType &&
      deviceTransportTypeConfigurationInfoMap.get(entity.transportType).hasProfileConfiguration;
    const deviceProvisionConfiguration: DeviceProvisionConfiguration = {
      type: entity?.provisionType ? entity.provisionType : DeviceProvisionType.DISABLED,
      provisionDeviceKey: entity?.provisionDeviceKey,
      provisionDeviceSecret: entity?.profileData?.provisionConfiguration?.provisionDeviceSecret,
      certificateRegExPattern: entity?.profileData?.provisionConfiguration?.certificateRegExPattern,
      allowCreateNewDevicesByX509Certificate: entity?.profileData?.provisionConfiguration?.allowCreateNewDevicesByX509Certificate
    };
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type}, {emitEvent: false});
    this.entityForm.patchValue({image: entity.image}, {emitEvent: false});
    this.entityForm.patchValue({transportType: entity.transportType}, {emitEvent: false});
    this.entityForm.patchValue({provisionType: entity.provisionType}, {emitEvent: false});
    this.entityForm.patchValue({provisionDeviceKey: entity.provisionDeviceKey}, {emitEvent: false});
    this.entityForm.patchValue({profileData: {
      configuration: entity.profileData?.configuration,
      transportConfiguration: entity.profileData?.transportConfiguration,
      alarms: entity.profileData?.alarms,
      provisionConfiguration: deviceProvisionConfiguration
    }}, {emitEvent: false});
    this.entityForm.patchValue({defaultRuleChainId: entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultDashboardId: entity.defaultDashboardId ? entity.defaultDashboardId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultQueueName: entity.defaultQueueName}, {emitEvent: false});
    this.entityForm.patchValue({defaultEdgeRuleChainId: entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({firmwareId: entity.firmwareId}, {emitEvent: false});
    this.entityForm.patchValue({softwareId: entity.softwareId}, {emitEvent: false});
    this.entityForm.patchValue({description: entity.description}, {emitEvent: false});
  }

  prepareFormValue(formValue: any): any {
    if (formValue.defaultRuleChainId) {
      formValue.defaultRuleChainId = new RuleChainId(formValue.defaultRuleChainId);
    }
    if (formValue.defaultDashboardId) {
      formValue.defaultDashboardId = new DashboardId(formValue.defaultDashboardId);
    }
    if (formValue.defaultEdgeRuleChainId) {
      formValue.defaultEdgeRuleChainId = new RuleChainId(formValue.defaultEdgeRuleChainId);
    }
    const deviceProvisionConfiguration: DeviceProvisionConfiguration = formValue.profileData.provisionConfiguration;
    formValue.provisionType = deviceProvisionConfiguration.type;
    formValue.provisionDeviceKey = deviceProvisionConfiguration.provisionDeviceKey;
    delete deviceProvisionConfiguration.provisionDeviceKey;
    return super.prepareFormValue(formValue);
  }

  onDeviceProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('device-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
