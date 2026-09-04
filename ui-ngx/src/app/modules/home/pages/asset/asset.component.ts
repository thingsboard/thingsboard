// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { AssetInfo } from '@app/shared/models/asset.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
    selector: 'tb-asset',
    templateUrl: './asset.component.html',
    styleUrls: ['./asset.component.scss'],
    standalone: false
})
export class AssetComponent extends EntityComponent<AssetInfo> {

  entityType = EntityType;

  assetScope: 'tenant' | 'customer' | 'customer_user' | 'edge';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AssetInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AssetInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.assetScope = this.entitiesTableConfig.componentsData.assetScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: AssetInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: AssetInfo): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        assetProfileId: [entity ? entity.assetProfileId : null, [Validators.required]],
        label: [entity ? entity.label : '', Validators.maxLength(255)],
        customerId: [entity ? entity.customerId : ''],
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
    this.entityForm.patchValue({customerId: entity.customerId});
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
