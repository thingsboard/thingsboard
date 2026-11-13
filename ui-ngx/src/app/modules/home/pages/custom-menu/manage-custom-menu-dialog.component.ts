// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CMAssigneeType,
  cmAssigneeTypeTranslations,
  CMScope,
  cmScopeTranslations,
  CustomMenu,
  CustomMenuInfo,
  isDefaultCustomMenuConflict
} from '@shared/models/custom-menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of } from 'rxjs';
import { User } from '@shared/models/user.model';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { UserService } from '@core/http/user.service';
import { catchError, map } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';
import { parseHttpErrorMessage } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface ManageCustomMenuDialogData {
  add: boolean;
  customMenu?: CustomMenuInfo;
  assigneeList?: EntityInfoData[];
}

export interface ManageCustomMenuDialogResult {
  customMenu?: CustomMenu;
  assigneeType?: CMAssigneeType;
  assignToList?: string[];
}

@Component({
    selector: 'tb-manage-custom-menu-dialog',
    templateUrl: './manage-custom-menu-dialog.component.html',
    styleUrls: ['./manage-custom-menu-dialog.component.scss'],
    standalone: false
})
export class ManageCustomMenuDialogComponent
  extends DialogComponent<ManageCustomMenuDialogComponent, ManageCustomMenuDialogResult> implements OnInit, OnDestroy {

  Authority = Authority;

  EntityType = EntityType;

  CMAssigneeType = CMAssigneeType;

  cmScopeTranslations = cmScopeTranslations;

  cmAssigneeTypeTranslations = cmAssigneeTypeTranslations;

  authUser = getCurrentAuthUser(this.store);

  title: string;

  customMenuFormGroup: UntypedFormGroup;

  customMenuScopes: CMScope[] = this.authUser.authority === Authority.TENANT_ADMIN ?
    [CMScope.TENANT, CMScope.CUSTOMER] : [CMScope.CUSTOMER];

  assigneeTypes: CMAssigneeType[] = [];

  fetchUsersFunction = this.fetchUsers.bind(this);
  fetchGroupsFunction = this.fetchGroups.bind(this);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageCustomMenuDialogData,
              public dialogRef: MatDialogRef<ManageCustomMenuDialogComponent, ManageCustomMenuDialogResult>,
              private customMenuService: CustomMenuService,
              private userService: UserService,
              private groupService: EntityGroupService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.customMenuFormGroup = this.fb.group({
      name: [this.data.add ? null : this.data.customMenu.name, [Validators.required]],
      scope: [this.data.add ? (this.authUser.authority === Authority.TENANT_ADMIN ? CMScope.TENANT : CMScope.CUSTOMER)
        : this.data.customMenu.scope],
      assigneeType: [this.data.add ? CMAssigneeType.NO_ASSIGN: this.data.customMenu.assigneeType],
      assignToList: [this.data.add ? null : this.data.assigneeList],
      userGroupNames: [this.data.add ? null : this.data.customMenu.userGroupNames],
    });
    this.title = this.data.add ? 'custom-menu.add' : 'custom-menu.manage-assignees';
    this.updateAssigneeTypes(this.data.add);
    if (!this.data.add) {
      this.customMenuFormGroup.get('name').disable({emitEvent: false});
    }
    if (this.data.add && this.authUser.authority === Authority.TENANT_ADMIN) {
      this.customMenuFormGroup.get('scope').valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateAssigneeTypes();
      });
    } else {
      this.customMenuFormGroup.get('scope').disable({emitEvent: false});
    }
    this.customMenuFormGroup.get('assigneeType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateAssignToList();
    });
  }

  private updateAssigneeTypes(updateAssigneeList = true) {
    const scope: CMScope = this.customMenuFormGroup.get('scope').value;
    if (scope === CMScope.TENANT) {
      this.assigneeTypes = [CMAssigneeType.NO_ASSIGN, CMAssigneeType.ALL, CMAssigneeType.USERS, CMAssigneeType.USER_GROUPS];
      const assigneeType: CMAssigneeType = this.customMenuFormGroup.get('assigneeType').value;
      if (assigneeType === CMAssigneeType.CUSTOMERS) {
        this.customMenuFormGroup.get('assigneeType').patchValue(CMAssigneeType.NO_ASSIGN, {emitEvent: false});
      }
    } else {
      this.assigneeTypes = [CMAssigneeType.NO_ASSIGN, CMAssigneeType.ALL, CMAssigneeType.USERS, CMAssigneeType.USER_GROUPS, CMAssigneeType.CUSTOMERS];
    }
    if (updateAssigneeList) {
      this.updateAssignToList();
    }
  }

  private updateAssignToList() {
    const assigneeType: CMAssigneeType = this.customMenuFormGroup.get('assigneeType').value;
    this.customMenuFormGroup.get('assignToList').patchValue(null, {emitEvent: false});
    this.customMenuFormGroup.get('userGroupNames').patchValue(null, {emitEvent: false});
    switch (assigneeType) {
      case CMAssigneeType.NO_ASSIGN:
      case CMAssigneeType.ALL:
        this.customMenuFormGroup.get('assignToList').disable({emitEvent: false});
        this.customMenuFormGroup.get('userGroupNames').disable({emitEvent: false});
        break;
      case CMAssigneeType.CUSTOMERS:
      case CMAssigneeType.USERS:
        this.customMenuFormGroup.get('assignToList').enable({emitEvent: false});
        this.customMenuFormGroup.get('userGroupNames').disable({emitEvent: false});
        break;
      case CMAssigneeType.USER_GROUPS:
        this.customMenuFormGroup.get('assignToList').disable({emitEvent: false});
        this.customMenuFormGroup.get('userGroupNames').enable({emitEvent: false});
        break;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  submit() {
    if (this.data.add) {
      this.add();
    } else {
      this.updateAssignees();
    }
  }

  private add(): void {
    if (this.customMenuFormGroup.valid) {
      const customMenuInfo: CustomMenuInfo = {
        name: this.customMenuFormGroup.get('name').value,
        scope: this.customMenuFormGroup.get('scope').value,
        assigneeType: this.customMenuFormGroup.get('assigneeType').value
      };
      if (customMenuInfo.assigneeType === CMAssigneeType.USER_GROUPS) {
        customMenuInfo.userGroupNames = this.customMenuFormGroup.get('userGroupNames').value;
      }
      const assignToList: string[] = this.customMenuFormGroup.get('assignToList').value || [];
      this.handleMenuSaveOperation(
        customMenuInfo,
        this.customMenuService.saveCustomMenu(customMenuInfo, assignToList, false,
          {ignoreErrors: true}),
        customMenu => this.dialogRef.close({customMenu}),
        () => this.customMenuService.saveCustomMenu(customMenuInfo, assignToList, true)
      );
    }
  }

  private updateAssignees() {
    if (this.customMenuFormGroup.valid) {
      const customMenu = this.data.customMenu;
      const assigneeType: CMAssigneeType = this.customMenuFormGroup.get('assigneeType').value;
      let assignToList: string[] = [];
      if (assigneeType === CMAssigneeType.USER_GROUPS) {
        assignToList = this.customMenuFormGroup.get('userGroupNames').value || [];
      } else {
        assignToList = this.customMenuFormGroup.get('assignToList').value || [];
      }
      this.handleMenuSaveOperation(
        customMenu,
        this.customMenuService.assignCustomMenu(customMenu.id.id, assigneeType, assignToList, false,
          {ignoreErrors: true}),
        _res => this.dialogRef.close({assigneeType, assignToList}),
        () => this.customMenuService.assignCustomMenu(customMenu.id.id, assigneeType, assignToList, true)
      );
    }
  }

  private handleMenuSaveOperation<R>(customMenu: CustomMenuInfo,
                                     saveObservable: Observable<R>,
                                     onSuccess: (res: R) => void,
                                     onForceSave: () => Observable<R>) {
    saveObservable.pipe(
      map((res) => ({success: true, error: null, res})),
      catchError((err) => {
        if (isDefaultCustomMenuConflict(err)) {
          return of({success: false, error: null, res: null});
        } else {
          return of({success: false, error: err, res: null});
        }
      }),
    ).subscribe(
      (saveResult) => {
        if (saveResult.success) {
          onSuccess(saveResult.res);
        } else if (!saveResult.error) {
          const menuConflictMessage = this.translate.instant('custom-menu.menu-conflict-message',
            {scope: this.translate.instant(cmScopeTranslations.get(customMenu.scope))});
          this.dialogService.confirm(this.translate.instant('custom-menu.menu-conflict'), menuConflictMessage,
            this.translate.instant('action.cancel'), this.translate.instant('custom-menu.replace')).subscribe((res) => {
            if (res) {
              const forceSaveObservable = onForceSave();
              forceSaveObservable.subscribe((forceSaveRes) => {
                onSuccess(forceSaveRes);
              });
            }
          });
        } else {
          const errorMessageWithTimeout = parseHttpErrorMessage(saveResult.error, this.translate);
          setTimeout(() => {
            this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
          }, errorMessageWithTimeout.timeout);
        }
      }
    );
  }

  fetchUsers(searchText?: string): Observable<Array<User>> {
    const scope: CMScope = this.customMenuFormGroup.get('scope').value;
    let usersObservable: Observable<PageData<User>>;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'email',
      direction: Direction.ASC
    });
    if (scope === CMScope.TENANT) {
      usersObservable = this.userService.getAllUserInfos(false, pageLink, {ignoreLoading: true});
    } else if (scope === CMScope.CUSTOMER) {
      if (this.authUser.authority === Authority.TENANT_ADMIN) {
        usersObservable = this.userService.getAllCustomerUsers(pageLink, {ignoreLoading: true});
      } else {
        usersObservable = this.userService.getAllUserInfos(true, pageLink, {ignoreLoading: true});
      }
    } else {
      usersObservable = of(emptyPageData<User>());
    }
    return usersObservable.pipe(
      map(pageData => pageData.data)
    );
  }

  fetchGroups(searchText?: string): Observable<Array<StringItemsOption>> {
    const scope: CMScope = this.customMenuFormGroup.get('scope').value;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    if (scope === CMScope.TENANT || scope === CMScope.CUSTOMER && this.authUser.authority === Authority.CUSTOMER_USER) {
      return this.groupService.getEntityGroups(pageLink, EntityType.USER, false, {ignoreLoading: true}).pipe(
        map(pageData => pageData.data
          .filter(group => !group.groupAll)
          .map(group => ({value: group.name, name: group.name}))),
      );
    } else {
      const allowGroups = ["Customer Administrators", "Customer Users"];
      const normalizedSearchText = searchText.trim().toLowerCase();
      const filteredGroup = allowGroups
        .filter(group => group.toLowerCase().includes(normalizedSearchText))
        .map(group => ({value: group, name: group}));
      return of(filteredGroup);
    }
  }
}
