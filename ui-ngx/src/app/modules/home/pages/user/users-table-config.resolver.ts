///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { User } from '@shared/models/user.model';
import { UserService } from '@core/http/user.service';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { CustomerService } from '@core/http/customer.service';
import { map, mergeMap, take, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomerId } from '@shared/models/id/customer-id';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { AddUserDialogComponent, AddUserDialogData } from '@modules/home/pages/user/add-user-dialog.component';
import { AuthState } from '@core/auth/auth.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuth } from '@core/auth/auth.selectors';
import { AuthService } from '@core/auth/auth.service';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { TenantService } from '@app/core/http/tenant.service';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';

export interface UsersTableRouteData {
  authority: Authority;
}

@Injectable()
export class UsersTableConfigResolver implements Resolve<EntityTableConfig<User>> {

  private readonly config: EntityTableConfig<User> = new EntityTableConfig<User>();

  private tenantId: string;
  private customerId: string;
  private authority: Authority;
  private authUser: User;

  constructor(private store: Store<AppState>,
              private userService: UserService,
              private authService: AuthService,
              private tenantService: TenantService,
              private customerService: CustomerService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.USER;
    this.config.entityComponent = UserComponent;
    this.config.entityTabsComponent = UserTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.USER);
    this.config.entityResources = entityTypeResources.get(EntityType.USER);

    this.config.columns.push(
      new DateEntityTableColumn<User>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<User>('firstName', 'user.first-name', '33%'),
      new EntityTableColumn<User>('lastName', 'user.last-name', '33%'),
      new EntityTableColumn<User>('email', 'user.email', '33%')
    );

    this.config.deleteEnabled = user => user && user.id && user.id.id !== this.authUser.id.id;
    this.config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    this.config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    this.config.loadEntity = id => this.userService.getUser(id.id);
    this.config.saveEntity = user => this.saveUser(user);
    this.config.deleteEntity = id => this.userService.deleteUser(id.id);
    this.config.onEntityAction = action => this.onUserAction(action);
    this.config.addEntity = () => this.addUser();
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<User>> {
    const routeParams = route.params;
    return this.store.pipe(select(selectAuth), take(1)).pipe(
      tap((auth) => {
        this.authUser = auth.userDetails;
        this.authority = routeParams.tenantId ? Authority.TENANT_ADMIN : Authority.CUSTOMER_USER;
        if (this.authority === Authority.TENANT_ADMIN) {
          this.tenantId = routeParams.tenantId;
          this.customerId = NULL_UUID;
          this.config.entitiesFetchFunction = pageLink => this.userService.getTenantAdmins(this.tenantId, pageLink);
        } else {
          this.tenantId = this.authUser.tenantId.id;
          this.customerId = routeParams.customerId;
          this.config.entitiesFetchFunction = pageLink => this.userService.getCustomerUsers(this.customerId, pageLink);
        }
        this.updateActionCellDescriptors(auth);
      }),
      mergeMap(() => this.authority === Authority.TENANT_ADMIN ?
        this.tenantService.getTenant(this.tenantId) :
        this.customerService.getCustomer(this.customerId)),
      map((parentEntity) => {
        if (this.authority === Authority.TENANT_ADMIN) {
          this.config.tableTitle = parentEntity.title + ': ' + this.translate.instant('user.tenant-admins');
        } else {
          this.config.tableTitle = parentEntity.title + ': ' + this.translate.instant('user.customer-users');
        }
        return this.config;
      })
    );
  }

  updateActionCellDescriptors(auth: AuthState) {
    this.config.cellActionDescriptors.splice(0);
    if (auth.userTokenAccessEnabled) {
      this.config.cellActionDescriptors.push(
        {
          name: this.authority === Authority.TENANT_ADMIN ?
            this.translate.instant('user.login-as-tenant-admin') :
            this.translate.instant('user.login-as-customer-user'),
          mdiIcon: 'mdi:login',
          isEnabled: () => true,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
  }

  saveUser(user: User): Observable<User> {
    user.tenantId = new TenantId(this.tenantId);
    user.customerId = new CustomerId(this.customerId);
    user.authority = this.authority;
    return this.userService.saveUser(user);
  }

  addUser(): Observable<User> {
    return this.dialog.open<AddUserDialogComponent, AddUserDialogData,
      User>(AddUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId: this.tenantId,
        customerId: this.customerId,
        authority: this.authority
      }
    }).afterClosed();
  }

  loginAsUser($event: Event, user: User) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  displayActivationLink($event: Event, user: User) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.getActivationLink(user.id.id).subscribe(
      (activationLink) => {
        this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
          void>(ActivationLinkDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            activationLink
          }
        });
      }
    );
  }

  resendActivation($event: Event, user: User) {
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

  setUserCredentialsEnabled($event: Event, user: User, userCredentialsEnabled: boolean) {
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

  onUserAction(action: EntityAction<User>): boolean {
    switch (action.action) {
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
    }
    return false;
  }

}
