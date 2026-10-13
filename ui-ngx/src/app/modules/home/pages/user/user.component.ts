// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject, Optional } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { UserInfo } from '@shared/models/user.model';
import { getCurrentAuthUser, selectAuth, selectAuthUser } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { isDefinedAndNotNull, validateEmail } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CMAssigneeType, CMScope } from '@shared/models/custom-menu.models';
import { environment as env } from '@env/environment';
import { UnitSystems } from '@shared/models/unit.models';

@Component({
    selector: 'tb-user',
    templateUrl: './user.component.html',
    styleUrls: ['./user.component.scss'],
    standalone: false
})
export class UserComponent extends GroupEntityComponent<UserInfo>{

  CMScope = CMScope;

  CMAssigneeType = CMAssigneeType;

  authority = Authority;
  languageList = env.supportedLangs;
  UnitSystems = UnitSystems;

  loginAsUserEnabled$ = this.store.pipe(
    select(selectAuth),
    map((auth) => auth.userTokenAccessEnabled)
  );

  whiteLabelingAllowed$ = this.store.pipe(
    select(selectAuth),
    map((auth) => auth.whiteLabelingAllowed)
  );

  isSysAdmin$ = this.store.pipe(
    select(selectAuthUser),
    map((auth) => auth?.authority === Authority.SYS_ADMIN)
  );

  private authUser = getCurrentAuthUser(this.store);

  constructor(protected store: Store<AppState>,
              @Optional() @Inject('entity') protected entityValue: UserInfo,
              @Optional() @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<UserInfo> | GroupEntityTableConfig<UserInfo>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected translate: TranslateService,
              protected userPermissionsService: UserPermissionsService) {
       super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isCurrentUser(): boolean {
    return this.authUser.userId === this.entity?.id?.id;
  }

  isUserTenantAdmin(): boolean {
    return this.entity?.authority === Authority.TENANT_ADMIN;
  }

  isUserCredentialsEnabled(): boolean {
      return this.entity?.additionalInfo?.userCredentialsEnabled === true;
  }

  isUserActivated(): boolean {
    return this.entity?.additionalInfo?.userActivated === true;
  }

  buildForm(entity: UserInfo): UntypedFormGroup {
    return this.fb.group(
      {
        email: [entity ? entity.email : '', [Validators.required, validateEmail]],
        firstName: [entity ? entity.firstName : ''],
        lastName: [entity ? entity.lastName : ''],
        phone: [entity ? entity.phone : ''],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            lang: [entity && entity.additionalInfo ? entity.additionalInfo.lang : null],
            unitSystem: [entity && entity.additionalInfo ? entity.additionalInfo.unitSystem : null],
            defaultDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null],
            defaultDashboardFullscreen: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        ),
        customMenuId: [entity?.customMenuId]
      }
    );
  }

  updateForm(entity: UserInfo) {
    this.entityForm.patchValue({email: entity.email});
    this.entityForm.patchValue({firstName: entity.firstName});
    this.entityForm.patchValue({lastName: entity.lastName});
    this.entityForm.patchValue({phone: entity.phone});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({additionalInfo:
        {lang: entity.additionalInfo ? entity.additionalInfo.lang : null}});
    this.entityForm.patchValue({additionalInfo:
        {unitSystem: entity.additionalInfo ? entity.additionalInfo.unitSystem : null}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardId: entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardFullscreen: entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardHideToolbar: entity.additionalInfo &&
          isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true}});
    this.entityForm.patchValue({customMenuId: entity.customMenuId});
  }

  onUserIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('user.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }
    ));
  }

}
