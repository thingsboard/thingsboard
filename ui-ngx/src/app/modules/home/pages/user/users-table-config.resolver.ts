// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AuthUser, UserInfo } from '@shared/models/user.model';
import { UserService } from '@core/http/user.service';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { CustomerService } from '@core/http/customer.service';
import { map, mergeMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomerId } from '@shared/models/id/customer-id';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { AuthState } from '@core/auth/auth.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthService } from '@core/auth/auth.service';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TenantService } from '@app/core/http/tenant.service';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';
import { UserTableHeaderComponent } from '@home/pages/user/user-table-header.component';
import { Customer } from '@shared/models/customer.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AddUserDialogComponent, AddUserDialogData } from '@home/pages/user/add-user-dialog.component';

export interface UsersTableRouteData {
  authority: Authority;
}

@Injectable()
export class UsersTableConfigResolver  {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<UserInfo>,
              private store: Store<AppState>,
              private userService: UserService,
              private authService: AuthService,
              private tenantService: TenantService,
              private customerService: CustomerService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private homeDialogs: HomeDialogsService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<UserInfo>> {
    const groupParams = resolveGroupParams(route);
    const tenantId = route.params.tenantId;
    const config = new EntityTableConfig<UserInfo>(groupParams);
    const authState = getCurrentAuthState(this.store);
    const authUser = authState.authUser;
    this.configDefaults(config, authUser, tenantId);
    config.componentsData = {
      includeCustomers: true,
      displayIncludeCustomers: authUser.authority !== Authority.SYS_ADMIN,
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    let titleObservable: Observable<string>;
    if (tenantId && authUser.authority === Authority.SYS_ADMIN) {
      titleObservable = this.tenantService.getTenant(tenantId).pipe(
        map((tenant) => tenant.title + ': ' + this.translate.instant('user.tenant-admins'))
      );
    } else {
      titleObservable = (config.customerId ?
        this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
          map((parentCustomer) => {
            if (parentCustomer) {
              return parentCustomer.title + ': ' + this.translate.instant('user.users');
            } else {
              return this.translate.instant('user.users');
            }
          }
        ));
    }
    return titleObservable.pipe(
      map((title) => {
        config.tableTitle = title;
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(authUser, config, tenantId);
        config.cellActionDescriptors = this.configureCellActions(authState, config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<UserInfo>, authUser: AuthUser, tenantId?: string) {
    config.entityType = EntityType.USER;
    config.entityComponent = UserComponent;
    config.entityTabsComponent = authUser.authority === Authority.SYS_ADMIN ? UserTabsComponent : GroupEntityTabsComponent<UserInfo>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.USER);
    config.entityResources = entityTypeResources.get(EntityType.USER);

    config.entityTitle = (user) => user ? user.email : '';

    config.rowPointer = true;

    config.deleteEnabled = user => user && user.id && user.id.id !== authUser.userId;

    config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    config.loadEntity = id => this.userService.getUserInfo(id.id);
    config.saveEntity = user => this.saveUser(authUser, config, user, tenantId);
    config.onEntityAction = action => this.onUserAction(action, config);
    config.addEntity = () => this.addUser(config, tenantId);
    config.headerComponent = UserTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<UserInfo>): Array<EntityColumn<UserInfo>> {
    const columns: Array<EntityColumn<UserInfo>> = [
      new DateEntityTableColumn<UserInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<UserInfo>('firstName', 'user.first-name', '33%'),
      new EntityTableColumn<UserInfo>('lastName', 'user.last-name', '33%'),
      new EntityTableColumn<UserInfo>('email', 'user.email', '33%')
    ];
    if (authUser.authority !== Authority.SYS_ADMIN) {
      let groupsColumnSize = '35%';
      if (config.componentsData.includeCustomers) {
        columns[1].width = '15%';
        columns[2].width = '15%';
        columns[3].width = '25%';
        const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
          ? 'entity.sub-customer-name' : 'entity.customer-name';
        columns.push(new EntityTableColumn<UserInfo>('ownerName', title, '20%'));
        groupsColumnSize = '25%';
      } else {
        columns[1].width = '15%';
        columns[2].width = '15%';
        columns[3].width = '35%';
      }
      columns.push(
        new EntityChipsEntityTableColumn<UserInfo>('groups', 'entity.groups', groupsColumnSize)
      );
    }
    return columns;
  }

  configureEntityFunctions(authUser: AuthUser, config: EntityTableConfig<UserInfo>, tenantId?: string): void {
    if (tenantId && authUser.authority === Authority.SYS_ADMIN) {
      config.entitiesFetchFunction = pageLink =>
        this.userService.getTenantAdmins(tenantId, pageLink);
    } else {
      if (config.customerId) {
        config.entitiesFetchFunction = pageLink =>
          this.userService.getCustomerUserInfos(config.componentsData.includeCustomers,
            config.customerId, pageLink);
      } else {
        config.entitiesFetchFunction = pageLink =>
          this.userService.getAllUserInfos(config.componentsData.includeCustomers, pageLink);
      }
    }
    config.deleteEntity = id => this.userService.deleteUser(id.id);
  }

  configureCellActions(auth: AuthState, config: EntityTableConfig<UserInfo>): Array<CellActionDescriptor<UserInfo>> {
    const actions: Array<CellActionDescriptor<UserInfo>> = [];
    if (auth.userTokenAccessEnabled && this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.IMPERSONATE)) {
      actions.push(
        {
          name: '',
          nameFunction: (user) => user.authority === Authority.TENANT_ADMIN ?
            this.translate.instant('user.login-as-tenant-admin') :
            this.translate.instant('user.login-as-customer-user'),
          icon: 'mdi:login',
          isEnabled: (user) => user.id.id !== auth.authUser.userId,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<UserInfo>): Array<GroupActionDescriptor<UserInfo>> {
    const actions: Array<GroupActionDescriptor<UserInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<UserInfo>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    return actions;
  }

  private saveUser(authUser: AuthUser, config: EntityTableConfig<UserInfo>, user: UserInfo, tenantId?: string): Observable<UserInfo> {
    if (authUser.authority === Authority.SYS_ADMIN && tenantId) {
      user.tenantId = new TenantId(tenantId);
      user.customerId = new CustomerId(NULL_UUID);
      user.authority = Authority.TENANT_ADMIN;
    } else {
      user.tenantId = new TenantId(authUser.tenantId);
    }
    if (!user.additionalInfo.lang) {
      delete user.additionalInfo.lang;
    }
    if (!user.additionalInfo.unitSystem) {
      delete user.additionalInfo.unitSystem;
    }
    return this.userService.saveUser(user).pipe(
      mergeMap((savedUser) => this.userService.getUserInfo(savedUser.id.id))
    );
  }

  private addUser(config: EntityTableConfig<UserInfo>, tenantId?: string): Observable<UserInfo> {
    return this.dialog.open<AddUserDialogComponent, AddUserDialogData,
      UserInfo>(AddUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config,
        tenantId
      }
    }).afterClosed();
  }

  private openUser($event: Event, user: UserInfo, config: EntityTableConfig<UserInfo>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([user.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  private loginAsUser($event: Event, user: UserInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  private displayActivationLink($event: Event, user: UserInfo) {
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

  private resendActivation($event: Event, user: UserInfo) {
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

  private setUserCredentialsEnabled($event: Event, user: UserInfo, userCredentialsEnabled: boolean) {
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

  manageOwnerAndGroups($event: Event, user: UserInfo, config: EntityTableConfig<UserInfo>) {
    this.homeDialogs.manageOwnerAndGroups($event, user).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }

  onUserAction(action: EntityAction<UserInfo>, config: EntityTableConfig<UserInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openUser(action.event, action.entity, config);
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
