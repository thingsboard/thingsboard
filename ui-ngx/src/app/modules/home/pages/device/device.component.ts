// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, DestroyRef, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DeviceInfo } from '@shared/models/device.models';
import {
  createDeviceConfiguration,
  createDeviceTransportConfiguration, DeviceCredentials,
  DeviceData,
  DeviceProfileInfo,
  DeviceProfileType,
  DeviceTransportType
} from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { ClipboardService } from 'ngx-clipboard';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { Subject } from 'rxjs';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { distinctUntilChanged } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { AiDashboardGenerationService } from '@home/components/ai/ai-dashboard-generation.service';

@Component({
    selector: 'tb-device',
    templateUrl: './device.component.html',
    styleUrls: ['./device.component.scss'],
    standalone: false
})
export class DeviceComponent extends GroupEntityComponent<DeviceInfo> {

  entityType = EntityType;

  deviceCredentials$: Subject<DeviceCredentials>;

//  deviceScope: 'tenant' | 'customer' | 'customer_user' | 'edge';

  otaUpdateType = OtaUpdateType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private deviceService: DeviceService,
              private clipboardService: ClipboardService,
              @Inject('entity') protected entityValue: DeviceInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<DeviceInfo> | GroupEntityTableConfig<DeviceInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService,
              private destroyRef: DestroyRef,
              private wl: WhiteLabelingService,
              private aiDashboardGenerationService: AiDashboardGenerationService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  ngOnInit() {
    // this.deviceScope = this.entitiesTableConfig.componentsData.deviceScope;
    this.deviceCredentials$ = this.entitiesTableConfigValue.componentsData.deviceCredentials$;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  showConnectivityDialog(): boolean {
    return !this.wl.getHideConnectivityDialog();
  }

  hideManageCredentials() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageCredentialsEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: Device): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: DeviceInfo): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        deviceProfileId: [entity ? entity.deviceProfileId : null, [Validators.required]],
        firmwareId: [entity ? entity.firmwareId : null],
        softwareId: [entity ? entity.softwareId : null],
        label: [entity ? entity.label : '', [Validators.maxLength(255)]],
        deviceData: [entity ? entity.deviceData : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            gateway: [entity && entity.additionalInfo ? entity.additionalInfo.gateway : false],
            overwriteActivityTime: [entity && entity.additionalInfo ? entity.additionalInfo.overwriteActivityTime : false],
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    form.get('deviceProfileId').valueChanges.pipe(
      distinctUntilChanged((prev, curr) => prev?.id === curr?.id),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(profileId => {
      if (profileId && this.isEdit) {
        this.entityForm.patchValue({
          firmwareId: null,
          softwareId: null
        }, {emitEvent: false});
      }
    });
    return form;
  }

  updateForm(entity: DeviceInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      deviceProfileId: entity.deviceProfileId,
      firmwareId: entity.firmwareId,
      softwareId: entity.softwareId,
      label: entity.label,
      deviceData: entity.deviceData,
      additionalInfo: {
        gateway: entity.additionalInfo ? entity.additionalInfo.gateway : false,
        overwriteActivityTime: entity.additionalInfo ? entity.additionalInfo.overwriteActivityTime : false,
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
  }


  onDeviceIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('device.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onDeviceProfileUpdated() {
    this.entitiesTableConfig.updateData(false, false);
  }

  onDeviceProfileChanged(deviceProfile: DeviceProfileInfo) {
    if (deviceProfile && this.isEdit) {
      const deviceProfileType: DeviceProfileType = deviceProfile.type;
      const deviceTransportType: DeviceTransportType = deviceProfile.transportType;
      let deviceData: DeviceData = this.entityForm.getRawValue().deviceData;
      if (!deviceData) {
        deviceData = {
          configuration: createDeviceConfiguration(deviceProfileType),
          transportConfiguration: createDeviceTransportConfiguration(deviceTransportType)
        };
        this.entityForm.patchValue({deviceData});
        this.entityForm.markAsDirty();
      } else {
        let changed = false;
        if (deviceData.configuration.type !== deviceProfileType) {
          deviceData.configuration = createDeviceConfiguration(deviceProfileType);
          changed = true;
        }
        if (deviceData.transportConfiguration.type !== deviceTransportType) {
          deviceData.transportConfiguration = createDeviceTransportConfiguration(deviceTransportType);
          changed = true;
        }
        if (changed) {
          this.entityForm.patchValue({deviceData});
          this.entityForm.markAsDirty();
        }
      }
    }
  }

  generateDashboard($event: Event) {
    $event.stopPropagation();
    this.aiDashboardGenerationService.generateWithTelemetryCheck({
      deviceId: this.entity.id.id,
      destroyRef: this.destroyRef,
      noTelemetry: { checkConnectivity: () => this.onEntityAction($event, 'checkConnectivity') }
    });
  }

  isAllowedDashboardGenerate(): boolean {
    return this.aiDashboardGenerationService.isAllowedDashboardGenerate({
      entityGroup: this.entityGroup,
      requireTelemetry: true
    });
  }
}
