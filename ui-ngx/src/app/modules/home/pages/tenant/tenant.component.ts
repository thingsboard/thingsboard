// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Tenant, TenantInfo } from '@app/shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { ContactBasedComponent } from '../../components/entity/contact-based.component';
import { isDefined } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { isDefinedAndNotNull } from '@core/utils';
import { CountryData } from '@shared/models/country.models';

@Component({
    selector: 'tb-tenant',
    templateUrl: './tenant.component.html',
    styleUrls: ['./tenant.component.scss'],
    standalone: false
})
export class TenantComponent extends ContactBasedComponent<TenantInfo> {

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: TenantInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected countryData: CountryData) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, countryData);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildEntityForm(entity: TenantInfo): UntypedFormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        tenantProfileId: [entity ? entity.tenantProfileId : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            allowWhiteLabeling: [entity && entity.additionalInfo
                     && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true],
            allowCustomerWhiteLabeling: [entity && entity.additionalInfo
                    && isDefined(entity.additionalInfo.allowCustomerWhiteLabeling) ?
                        entity.additionalInfo.allowCustomerWhiteLabeling : true],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        )
      }
    );
  }

  updateEntityForm(entity: Tenant) {
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({tenantProfileId: entity.tenantProfileId});
    this.entityForm.patchValue({additionalInfo: {
      description: entity.additionalInfo ? entity.additionalInfo.description : '',
      allowWhiteLabeling: entity.additionalInfo
        && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true,
      allowCustomerWhiteLabeling: entity.additionalInfo
        && isDefined(entity.additionalInfo.allowCustomerWhiteLabeling) ?
          entity.additionalInfo.allowCustomerWhiteLabeling : true,
      homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null,
      homeDashboardHideToolbar: entity.additionalInfo &&
        isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true
    }});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onTenantProfileUpdated() {
    this.entitiesTableConfig.updateData(false, false);
  }
}
