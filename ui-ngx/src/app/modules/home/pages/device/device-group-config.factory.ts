// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { deviceAiAssistantConfig, DeviceCredentials, DeviceInfo } from '@shared/models/device.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { Observable, Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { DestroyRef, Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceComponent } from '@home/pages/device/device.component';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  DeviceCredentialsDialogComponent,
  DeviceCredentialsDialogData
} from '@home/pages/device/device-credentials-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { Operation, Resource } from '@shared/models/security.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId } from '@shared/models/id/customer-id';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import {
  DeviceWizardDialogComponent,
  DeviceWizardDialogData
} from '@home/components/wizard/device-wizard-dialog.component';
import { isDefinedAndNotNull } from '@core/utils';
import { Router, UrlTree } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  DeviceCheckConnectivityDialogComponent,
  DeviceCheckConnectivityDialogData
} from '@home/pages/device/device-check-connectivity-dialog.component';
import { select, Store } from '@ngrx/store';
import { getCurrentAuthUser, selectUserSettingsProperty } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { AiDashboardGenerationService } from '@home/components/ai/ai-dashboard-generation.service';
import { Authority } from '@shared/models/authority.enum';

@Injectable()
export class DeviceGroupConfigFactory implements EntityGroupStateConfigFactory<DeviceInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<DeviceInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private deviceService: DeviceService,
              private router: Router,
              private broadcast: BroadcastService,
              private store: Store<AppState>,
              private wl: WhiteLabelingService,
              private iotHubActions: IotHubActionsService,
              @Inject(WINDOW) private window: Window,
              private destroyRef: DestroyRef,
              private aiDashboardGenerationService: AiDashboardGenerationService) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<DeviceInfo>): Observable<GroupEntityTableConfig<DeviceInfo>> {
    const config = new GroupEntityTableConfig<DeviceInfo>(entityGroup, params);

    config.entityComponent = DeviceComponent;

    config.componentsData = {
      deviceCredentials$: new Subject<DeviceCredentials>()
    };

    config.entityTitle = (device) => device ?
      this.utils.customTranslation(device.name, device.name) : '';

    config.deleteEntityTitle = device => this.translate.instant('device.delete-device-title', { deviceName: device.name });
    config.deleteEntityContent = () => this.translate.instant('device.delete-device-text');
    config.deleteEntitiesTitle = count => this.translate.instant('device.delete-devices-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('device.delete-devices-text');

    config.loadEntity = id => this.deviceService.getDeviceInfo(id.id);
    config.saveEntity = device => this.deviceService.saveDevice(device).pipe(
      tap(() => {
        this.broadcast.broadcast('deviceSaved');
      }),
      mergeMap((savedDevice) => this.deviceService.getDeviceInfo(savedDevice.id.id)
      ));
    config.deleteEntity = id => this.deviceService.deleteDevice(id.id);

    config.onEntityAction = action => this.onDeviceAction(action, config, params);
    config.addEntity = () => this.deviceWizard(config);

    if (config.settings.enableCredentialsManagement) {
      if (this.userPermissionsService.hasGroupEntityPermission(Operation.READ_CREDENTIALS, config.entityGroup) &&
        !this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.view-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, true, config)
          }
        );
      }

      if (this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE_CREDENTIALS, config.entityGroup)) {
        config.cellActionDescriptors.push(
          {
            name: this.translate.instant('device.manage-credentials'),
            icon: 'security',
            isEnabled: config.manageCredentialsEnabled,
            onAction: ($event, entity) => this.manageCredentials($event, entity, false, config)
          }
        );
      }
    }

    const authUser = getCurrentAuthUser(this.store);

    if (this.aiDashboardGenerationService.isAllowedDashboardGenerate({ entityGroup: config.entityGroup, requireTelemetry: true })) {
      config.cellActionDescriptors.push({
        name: this.translate.instant('solution-creator.generate-dashboard-with-ai'),
        icon: 'mdi:creation',
        isEnabled: () => true,
        onAction: ($event, entity) => this.generateDashboard($event, entity)
      });
    }

    if (this.userPermissionsService.hasGroupEntityPermission(Operation.CREATE, config.entityGroup)) {
      config.headerActionDescriptors.push(
        {
          name: this.translate.instant('device.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importDevices($event, config)
        }
      );
      if (authUser.authority === Authority.TENANT_ADMIN && this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
        config.headerActionDescriptors.push(
          {
            name: this.translate.instant('iot-hub.add-from-iot-hub'),
            icon: 'hub',
            isEnabled: () => true,
            onAction: (_$event) => this.addDeviceFromIotHub(config)
          }
        );
      }
    }
    config.aiAssistantConfig = deviceAiAssistantConfig(this.store, this.userPermissionsService, this.translate,
      this.userPermissionsService.hasGroupEntityPermission(Operation.WRITE, config.entityGroup),
      AiAssistantViewType.DEVICE, AiAssistantViewType.DEVICE_GROUP, config.entityGroup.id);
    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  deviceWizard(config: GroupEntityTableConfig<DeviceInfo>): Observable<DeviceInfo> {
    return this.dialog.open<DeviceWizardDialogComponent, DeviceWizardDialogData,
      DeviceInfo>(DeviceWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityGroup: config.entityGroup
      }
    }).afterClosed().pipe(
      map(device => {
        if (device) {
          if (this.wl.getHideConnectivityDialog()) {
            config.updateData();
          } else {
            this.store.pipe(select(selectUserSettingsProperty('notDisplayConnectivityAfterAddDevice'))).pipe(
              take(1)
            ).subscribe((settings: boolean) => {
              if (!settings) {
                this.checkConnectivity(null, device.id, true, config);
              } else {
                config.updateData();
              }
            });
          }
        }
        return null;
      })
    );
  }

  importDevices($event: Event, config: GroupEntityTableConfig<DeviceInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    let customerId: CustomerId = null;
    if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
      customerId = entityGroup.ownerId as CustomerId;
    }
    this.homeDialogs.importEntities(customerId, EntityType.DEVICE, entityGroupId).subscribe((res) => {
      if (res) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  addDeviceFromIotHub(config: GroupEntityTableConfig<DeviceInfo>) {
    const entityGroup = config.entityGroup;
    const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
    const customerId = entityGroup.ownerId.entityType === EntityType.CUSTOMER ? entityGroup.ownerId.id : undefined;
    this.iotHubActions.addItem(ItemType.DEVICE, { entityGroupId, customerId }).subscribe(result => {
      if (result?.descriptor) {
        this.broadcast.broadcast('deviceSaved');
        config.updateData();
      }
    });
  }

  private openDevice($event: Event, device: DeviceInfo, config: GroupEntityTableConfig<DeviceInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'deviceGroups', params.edgeEntitiesGroupId, device.id.id]);
      } else {
        url = this.router.createUrlTree(['customers', 'groups', params.entityGroupId,
          params.customerId, 'entities', 'devices', 'groups', params.childEntityGroupId, device.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([device.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  manageCredentials($event: Event, device: DeviceInfo | ShortEntityView, isReadOnly: boolean, config: GroupEntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCredentialsDialogComponent, DeviceCredentialsDialogData,
      DeviceCredentials>(DeviceCredentialsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId: device.id.id,
        deviceProfileId: device.deviceProfileId?.id,
        isReadOnly
      }
    }).afterClosed().subscribe(deviceCredentials => {
      if (isDefinedAndNotNull(deviceCredentials)) {
        config.componentsData.deviceCredentials$.next(deviceCredentials);
      }
    });
  }

  manageOwnerAndGroups($event: Event, device: DeviceInfo, config: GroupEntityTableConfig<DeviceInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, device).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  checkConnectivity($event: Event, deviceId: EntityId, afterAdd = false, config?: GroupEntityTableConfig<DeviceInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<DeviceCheckConnectivityDialogComponent, DeviceCheckConnectivityDialogData>
    (DeviceCheckConnectivityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        deviceId,
        afterAdd
      }
    })
      .afterClosed()
      .subscribe(() => {
      if (afterAdd ) {
        config.updateData();
      }
    });
  }

  onDeviceAction(action: EntityAction<DeviceInfo>, config: GroupEntityTableConfig<DeviceInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openDevice(action.event, action.entity, config, params);
        return true;
      case 'manageCredentials':
        this.manageCredentials(action.event, action.entity, false, config);
        return true;
      case 'viewCredentials':
        this.manageCredentials(action.event, action.entity, true, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
      case 'checkConnectivity':
        this.checkConnectivity(action.event, action.entity.id);
        return true;
    }
    return false;
  }

  generateDashboard($event: Event, device: ShortEntityView) {
    $event.stopPropagation();
    this.aiDashboardGenerationService.generateWithTelemetryCheck({
      deviceId: device.id.id,
      destroyRef: this.destroyRef,
      noTelemetry: { checkConnectivity: () => this.checkConnectivity($event, device.id) }
    });
  }
}
