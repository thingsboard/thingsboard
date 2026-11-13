// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { DeviceEntityGroupInfo, EntityGroupInfo } from '@shared/models/entity-group.models';
import { Operation, publicGroupTypes, Resource, sharableGroupTypes } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { OtaUpdateType } from '@shared/models/ota-package.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';

@Component({
    selector: 'tb-entity-group',
    templateUrl: './entity-group.component.html',
    styleUrls: ['./entity-group.component.scss'],
    standalone: false
})
export class EntityGroupComponent extends EntityComponent<EntityGroupInfo> {

  isPublic = false;
  shareEnabled = false;
  makePublicEnabled = false;
  makePrivateEnabled = false;
  isGroupAll = false;
  packageTypes = OtaUpdateType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              protected userPermissionsService: UserPermissionsService,
              @Inject('entity') protected entityValue: EntityGroupInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EntityGroupInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideOpen() {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.isGroupEntitiesView;
    } else {
      return false;
    }
  }

  hideUnassign() {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.isUnassignEnabled;
    } else {
      return false;
    }
  }

  buildForm(entity: EntityGroupInfo): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
    this.updateGroupParams(entity);
    if ((this.entitiesTableConfig as EntityGroupsTableConfig).groupType === EntityType.DEVICE) {
      form.addControl('firmwareId', this.fb.control(entity ? (entity as DeviceEntityGroupInfo).firmwareId : ''));
      form.addControl('softwareId', this.fb.control(entity ? (entity as DeviceEntityGroupInfo).softwareId : ''));
    }
    return form;
  }

  updateForm(entity: EntityGroupInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    if (entity.type === EntityType.DEVICE) {
      this.entityForm.patchValue({
        firmwareId: (entity as DeviceEntityGroupInfo).firmwareId,
        softwareId: (entity as DeviceEntityGroupInfo).softwareId
      }, {emitEvent: false});
    }
    this.updateGroupParams(entity);
  }

  private updateGroupParams(entityGroup: EntityGroupInfo) {
    if (entityGroup) {
      if (entityGroup.id) {
        const isPublicGroupType = publicGroupTypes.has(entityGroup.type);
        const isSharableGroupType = sharableGroupTypes.has(entityGroup.type);
        const isPublic: boolean = entityGroup.additionalInfo?.isPublic;
        const isOwned = this.userPermissionsService.isDirectlyOwnedGroup(entityGroup);
        const isWriteAllowed = this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
        const isCreatePermissionAllowed = this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE);
        this.isPublic = isPublic;
        this.shareEnabled = !this.sharedGroup() && isSharableGroupType && isCreatePermissionAllowed && isWriteAllowed;
        this.makePublicEnabled = !this.sharedGroup() && isPublicGroupType && !isPublic && isOwned && isWriteAllowed;
        this.makePrivateEnabled = !this.sharedGroup() && isPublicGroupType && isPublic && isOwned && isWriteAllowed;
        this.isGroupAll = entityGroup.groupAll;
      } else {
        this.isPublic = false;
        this.shareEnabled = false;
        this.makePublicEnabled = false;
        this.makePrivateEnabled = false;
        this.isGroupAll = false;
      }
    }
  }

  private sharedGroup(): boolean {
    if (this.entitiesTableConfig) {
      return this.entitiesTableConfig.componentsData.shared === true;
    } else {
      return false;
    }
  }

  onEntityGroupIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('entity-group.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
