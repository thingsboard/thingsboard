// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { Dashboard, DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { isEqual } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-dashboard-form',
    templateUrl: './dashboard-form.component.html',
    styleUrls: ['./dashboard-form.component.scss'],
    standalone: false
})
export class DashboardFormComponent extends GroupEntityComponent<DashboardInfo> {

  // dashboardScope: 'tenant' | 'customer' | 'customer_user';
  // customerId: string;

  isPublic: boolean;
  publicLink: string;
  // assignedCustomersText: string;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dashboardService: DashboardService,
              @Inject('entity') protected entityValue: DashboardInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<DashboardInfo> | GroupEntityTableConfig<DashboardInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
    if (this.entityGroup && this.entityGroup.additionalInfo && this.entityGroup.additionalInfo.isPublic) {
      this.isPublic = true;
    } else {
      this.isPublic = false;
    }
  }

  ngOnInit() {
    // this.dashboardScope = this.entitiesTableConfig.componentsData.dashboardScope;
    // this.customerId = this.entitiesTableConfig.componentsData.customerId;
    super.ngOnInit();
  }

  /* isPublic(entity: Dashboard): boolean {
    return isPublicDashboard(entity);
  } */

  /* isCurrentPublicCustomer(entity: Dashboard): boolean {
    return isCurrentPublicDashboardCustomer(entity, this.customerId);
  } */

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: DashboardInfo): UntypedFormGroup {
    this.updateFields(entity);
    const form = this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : null],
        mobileHide: [entity ? entity.mobileHide : false],
        mobileOrder: [entity ? entity.mobileOrder : null, [Validators.pattern(/^-?[0-9]+$/)]],
        configuration: this.fb.group(
          {
            description: [entity && entity.configuration ? entity.configuration.description : ''],
          }
        )
      }
    );
    // if (this.isAdd) {
    //   form.addControl('assignedCustomerIds', this.fb.control([]));
    // }

    return form;
  }

  updateForm(entity: Dashboard) {
    this.updateFields(entity);
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({image: entity.image});
    this.entityForm.patchValue({mobileHide: entity.mobileHide});
    this.entityForm.patchValue({mobileOrder: entity.mobileOrder});
    this.entityForm.patchValue({configuration: {description: entity.configuration ? entity.configuration.description : ''}});
  }

  prepareFormValue(formValue: any): any {
    const preparedValue = super.prepareFormValue(formValue);
    preparedValue.configuration = {...(this.entity.configuration || {}), ...(preparedValue.configuration || {})};
    return preparedValue;
  }

  onPublicLinkCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('dashboard.public-link-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onDashboardIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('dashboard.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private updateFields(entity: Dashboard): void {
    if (entity && !isEqual(entity, {})) {
      // this.assignedCustomersText = getDashboardAssignedCustomersText(entity);
      if (this.isPublic) {
        this.publicLink = this.dashboardService.getPublicDashboardLink(entity, this.entityGroup);
      } else {
        this.publicLink = null;
      }
    }
  }
}
