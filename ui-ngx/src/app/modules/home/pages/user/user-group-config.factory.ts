// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserInfo } from '@shared/models/user.model';
import { AddUserDialogComponent, AddUserDialogData } from '@home/pages/user/add-user-dialog.component';
import { UserService } from '@core/http/user.service';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@home/pages/user/activation-link-dialog.component';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Router, UrlTree } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { mergeMap } from 'rxjs/operators';
import { UserComponent } from '@home/pages/user/user.component';

@Injectable()
export class UserGroupConfigFactory implements EntityGroupStateConfigFactory<UserInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<UserInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private userService: UserService,
              private authService: AuthService,
              private router: Router,
              private store: Store<AppState>,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<UserInfo>): Observable<GroupEntityTableConfig<UserInfo>> {
    const config = new GroupEntityTableConfig<UserInfo>(entityGroup, params);

    config.entityComponent = UserComponent;

    config.entityTitle = (user) => user ? user.email : '';

    config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    config.loadEntity = id => this.userService.getUserInfo(id.id);
    config.saveEntity = user => this.userService.saveUser(user).pipe(
      mergeMap((savedUser) => this.userService.getUserInfo(savedUser.id.id))
    );
    config.deleteEntity = id => this.userService.deleteUser(id.id);

    config.onEntityAction = action => this.onUserAction(action, config, params);
    config.addEntity = () => this.addUser(config);

    const auth = getCurrentAuthState(this.store);
    if (config.settings.enableDelete) {
      config.deleteEnabled = user => user && user.id && user.id.id !== auth.userDetails.id.id;
    }
    if (auth.userTokenAccessEnabled && this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.IMPERSONATE)) {
      const isTenantAdmins = entityGroup.ownerId.entityType === EntityType.TENANT;
      config.cellActionDescriptors.push(
        {
          name: isTenantAdmins ?
            this.translate.instant('user.login-as-tenant-admin') :
            this.translate.instant('user.login-as-customer-user'),
          icon: 'mdi:login',
          isEnabled: (entity) => config.loginAsUserEnabled(entity) && entity.id.id !== auth.authUser.userId,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  addUser(config: GroupEntityTableConfig<UserInfo>): Observable<UserInfo> {
    return this.dialog.open<AddUserDialogComponent, AddUserDialogData,
      UserInfo>(AddUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  private openUser($event: Event, user: UserInfo, config: GroupEntityTableConfig<UserInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'userGroups', params.edgeEntitiesGroupId, user.id.id]);
      } else {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId,
          params.customerId, 'userGroups', params.childEntityGroupId, user.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([user.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  loginAsUser($event: Event, user: UserInfo | ShortEntityView) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  displayActivationLink($event: Event, user: UserInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.getActivationLinkInfo(user.id.id).subscribe(
      (activationLinkInfo) => {
        this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
          void>(ActivationLinkDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            activationLinkInfo
          }
        });
      }
    );
  }

  resendActivation($event: Event, user: UserInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.sendActivationEmail(user.email).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow(
        {
          message: this.translate.instant('user.activation-email-sent-message'),
          type: 'success'
        }));
    });
  }

  setUserCredentialsEnabled($event: Event, user: UserInfo, userCredentialsEnabled: boolean) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.setUserCredentialsEnabled(user.id.id, userCredentialsEnabled).subscribe(() => {
      if (!user.additionalInfo) {
        user.additionalInfo = {};
      }
      user.additionalInfo.userCredentialsEnabled = userCredentialsEnabled;
      this.store.dispatch(new ActionNotificationShow(
        {
          message: this.translate.instant(userCredentialsEnabled ? 'user.enable-account-message' : 'user.disable-account-message'),
          type: 'success'
        }));
    });
  }

  manageOwnerAndGroups($event: Event, user: UserInfo, config: GroupEntityTableConfig<UserInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, user).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onUserAction(action: EntityAction<UserInfo>, config: GroupEntityTableConfig<UserInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openUser(action.event, action.entity, config, params);
        return true;
      case 'loginAsUser':
        this.loginAsUser(action.event, action.entity);
        return true;
      case 'displayActivationLink':
        this.displayActivationLink(action.event, action.entity);
        return true;
      case 'resendActivation':
        this.resendActivation(action.event, action.entity);
        return true;
      case 'disableAccount':
        this.setUserCredentialsEnabled(action.event, action.entity, false);
        return true;
      case 'enableAccount':
        this.setUserCredentialsEnabled(action.event, action.entity, true);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }

}
