// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { AssetInfo } from '@shared/models/asset.models';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';


@Component({
    selector: 'tb-asset',
    templateUrl: './asset.component.html',
    styleUrls: ['./asset.component.scss'],
    standalone: false
})
export class AssetComponent extends GroupEntityComponent<AssetInfo> {

  entityType = EntityType;

  readonly isTenantAdmin: boolean;

  // assetScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AssetInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<AssetInfo> | GroupEntityTableConfig<AssetInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
    this.isTenantAdmin = getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN;
  }

  ngOnInit() {
    // this.assetScope = this.entitiesTableConfig.componentsData.assetScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: Asset): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: AssetInfo): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        assetProfileId: [entity ? entity.assetProfileId : null, [Validators.required]],
        label: [entity ? entity.label : '', Validators.maxLength(255)],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: AssetInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({assetProfileId: entity.assetProfileId});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onAssetIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('asset.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onAssetProfileUpdated() {
    this.entitiesTableConfig.updateData(false, false);
  }
}
