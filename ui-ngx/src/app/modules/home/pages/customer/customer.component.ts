// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { CustomerInfo } from '@shared/models/customer.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { isDefined, isDefinedAndNotNull } from '@core/utils';
import { GroupContactBasedComponent } from '@home/components/group/group-contact-based.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { CountryData } from '@shared/models/country.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CMAssigneeType, CMScope } from '@shared/models/custom-menu.models';

@Component({
    selector: 'tb-customer',
    templateUrl: './customer.component.html',
    styleUrls: ['./customer.component.scss'],
    standalone: false
})
export class CustomerComponent extends GroupContactBasedComponent<CustomerInfo> {

  CMScope = CMScope;

  CMAssigneeType = CMAssigneeType;

  isPublic = false;

  allowCustomerWhiteLabeling = getCurrentAuthState(this.store).customerWhiteLabelingAllowed;
  whiteLabelingAllowed = getCurrentAuthState(this.store).whiteLabelingAllowed;
  edgesSupportEnabled = getCurrentAuthState(this.store).edgesSupportEnabled;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: CustomerInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<CustomerInfo> | GroupEntityTableConfig<CustomerInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected countryData: CountryData,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, countryData, userPermissionsService);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageUsers() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageUsersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageCustomers() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageCustomersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageAssets() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageAssetsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDevices() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDevicesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEntityViews() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageEntityViewsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEdges() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageEdgesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDashboards() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDashboardsEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildEntityForm(entity: CustomerInfo): UntypedFormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            allowWhiteLabeling: [entity && entity.additionalInfo
            && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        ),
        customMenuId: [entity?.customMenuId]
      }
    );
  }

  updateEntityForm(entity: CustomerInfo) {
    this.isPublic = entity.additionalInfo && entity.additionalInfo.isPublic;
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : '',
        allowWhiteLabeling: entity.additionalInfo
        && isDefined(entity.additionalInfo.allowWhiteLabeling) ? entity.additionalInfo.allowWhiteLabeling : true,
        homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null,
        homeDashboardHideToolbar: entity.additionalInfo &&
        isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true
      }});
    this.entityForm.patchValue({customMenuId: entity.customMenuId});
  }

  onCustomerIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('customer.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
