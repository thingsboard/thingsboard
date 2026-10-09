// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  cmAssigneeTypeTranslations,
  cmScopeTranslations,
  CustomMenu,
  CustomMenuDeleteResult,
  CustomMenuInfo,
  toCustomMenuDeleteResult
} from '@shared/models/custom-menu.models';
import { CustomMenuTableHeaderComponent } from '@home/pages/custom-menu/custom-menu-table-header.component';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Authority } from '@shared/models/authority.enum';
import { Observable, of, switchMap } from 'rxjs';
import {
  ManageCustomMenuDialogComponent,
  ManageCustomMenuDialogData,
  ManageCustomMenuDialogResult
} from '@home/pages/custom-menu/manage-custom-menu-dialog.component';
import { TbPopoverService } from '@shared/components/popover.service';
import { EditCustomMenuNamePanelComponent } from '@home/pages/custom-menu/edit-custom-menu-name-panel.component';
import { catchError, map } from 'rxjs/operators';
import {
  CustomMenuIsAssignedDialogComponent,
  CustomMenuIsAssignedDialogData
} from '@home/pages/custom-menu/custom-menu-is-assigned.dialog.component';
import { parseHttpErrorMessage } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
    selector: 'tb-custom-menu-table',
    templateUrl: './custom-menu-table.component.html',
    styleUrls: ['./custom-menu-table.component.scss'],
    standalone: false
})
export class CustomMenuTableComponent implements OnInit {

  readonly customMenuTableConfig = new EntityTableConfig<CustomMenuInfo>();

  constructor(private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private router: Router,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.customMenuTableConfig.tableTitle = '';
    this.customMenuTableConfig.headerComponent = CustomMenuTableHeaderComponent;
    this.customMenuTableConfig.detailsPanelEnabled = false;
    this.customMenuTableConfig.searchEnabled = true;
    this.customMenuTableConfig.selectionEnabled = false;
    this.customMenuTableConfig.entityTranslations = {
      add: 'custom-menu.add',
      noEntities: 'custom-menu.no-custom-menus-prompt',
      search: 'custom-menu.search'
    };
    this.customMenuTableConfig.entityResources = {
    } as EntityTypeResource<CustomMenuInfo>;
    this.customMenuTableConfig.entitiesFetchFunction = pageLink => this.customMenuService.getCustomMenuInfos(pageLink);
    this.customMenuTableConfig.defaultSortOrder = {property: 'createdTime', direction: Direction.ASC};

    this.customMenuTableConfig.deleteEntityTitle = customMenu => this.translate.instant('custom-menu.delete-custom-menu-title',
      { customMenuName: customMenu.name });
    this.customMenuTableConfig.deleteEntityContent = () => this.translate.instant('custom-menu.delete-custom-menu-text');

    this.customMenuTableConfig.saveEntity = customMenu => this.customMenuService.saveCustomMenu(customMenu);
    this.customMenuTableConfig.deleteEntity = id => this.deleteCustomMenu(id.id);

    const authUser = getCurrentAuthUser(this.store);
    const readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
    this.customMenuTableConfig.addEnabled = !readonly && authUser.authority !== Authority.SYS_ADMIN;
    this.customMenuTableConfig.entitiesDeleteEnabled = !readonly && authUser.authority !== Authority.SYS_ADMIN;
    this.customMenuTableConfig.addEntity = () => this.addCustomMenu();

    let mainColumnsWidth: string;
    switch (authUser.authority) {
      case Authority.SYS_ADMIN:
        mainColumnsWidth = '50%';
        break;
      case Authority.TENANT_ADMIN:
        mainColumnsWidth = '33%';
        break;
      case Authority.CUSTOMER_USER:
        mainColumnsWidth = '50%';
        break;
    }

    this.customMenuTableConfig.columns.push(
      new DateEntityTableColumn<CustomMenuInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<CustomMenuInfo>('name', 'custom-menu.name', mainColumnsWidth,
          entity => entity.name, _entity => ({}), true, () => ({}), () => undefined, false,
        {
          name: this.translate.instant('custom-menu.edit-name'),
          icon: 'edit',
          isEnabled: () => !readonly && authUser.authority !== Authority.SYS_ADMIN,
          onAction: ($event, entity) => this.updateCustomMenuName($event, entity)
        })
    );
    if (authUser.authority !== Authority.CUSTOMER_USER) {
      this.customMenuTableConfig.columns.push(new EntityTableColumn<CustomMenuInfo>('scope', 'custom-menu.scope', mainColumnsWidth,
        (menu) => this.translate.instant(cmScopeTranslations.get(menu.scope))));
    }
    if (authUser.authority !== Authority.SYS_ADMIN) {
      this.customMenuTableConfig.columns.push(new EntityTableColumn<CustomMenuInfo>('assigneeType',
        'custom-menu.assignee-type', mainColumnsWidth,
        (menu) => this.translate.instant(cmAssigneeTypeTranslations(menu.assigneeType, menu.scope))));
    }
    if (authUser.authority !== Authority.SYS_ADMIN) {
      if (!readonly) {
        this.customMenuTableConfig.cellActionDescriptors.push(
          {
            name: this.translate.instant('custom-menu.manage-assignees'),
            icon: 'mdi:account-group-outline',
            isEnabled: () => true,
            onAction: ($event, entity) => this.manageCustomMenuAssignees($event, entity)
          }
        );
      }
    }
    this.customMenuTableConfig.cellActionDescriptors.push(
      {
        name: this.translate.instant('custom-menu.manage-config'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.openCustomMenuConfig($event, entity)
      }
    );
  }

  private addCustomMenu(): Observable<CustomMenu> {
    return this.dialog.open<ManageCustomMenuDialogComponent, ManageCustomMenuDialogData,
      ManageCustomMenuDialogResult>(ManageCustomMenuDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        add: true
      }
    }).afterClosed().pipe(map(res => res?.customMenu));
  }

  private updateCustomMenuName($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
      const trigger = ($event.target || $event.srcElement || $event.currentTarget) as Element;
      if (this.popoverService.hasPopover(trigger)) {
        this.popoverService.hidePopover(trigger);
      } else {
        const editCustomMenuNamePanelPopover = this.popoverService.displayPopover({
          trigger,
          renderer: this.renderer,
          componentType: EditCustomMenuNamePanelComponent,
          hostView: this.viewContainerRef,
          preferredPlacement: ['right', 'bottom', 'top'],
          context: {
            customMenuId: customMenu.id.id,
            name: customMenu.name
          },
          isModal: true
        });
        editCustomMenuNamePanelPopover.tbComponentRef.instance.popover = editCustomMenuNamePanelPopover;
        editCustomMenuNamePanelPopover.tbComponentRef.instance.nameApplied.subscribe(() => {
          editCustomMenuNamePanelPopover.hide();
          this.customMenuTableConfig.updateData();
        });
      }
    }
  }

  private manageCustomMenuAssignees($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.customMenuService.getCustomMenuAssigneeList(customMenu.id.id).subscribe((assigneeList) => {
      this.dialog.open<ManageCustomMenuDialogComponent, ManageCustomMenuDialogData,
        ManageCustomMenuDialogResult>(ManageCustomMenuDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          add: false,
          customMenu,
          assigneeList
        }
      }).afterClosed().subscribe(res => {
        if (res?.assigneeType) {
          this.customMenuTableConfig.updateData();
        }
      });
    });
  }

  private openCustomMenuConfig($event: Event, customMenu: CustomMenuInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`white-labeling/customMenu/${customMenu.id.id}`);
  }

  private deleteCustomMenu(customMenuId: string): Observable<any> {
    return this.customMenuService.deleteCustomMenu(customMenuId, false, {ignoreErrors: true}).pipe(
      map(() => ({success: true} as CustomMenuDeleteResult)),
      catchError((err) => of(toCustomMenuDeleteResult(err))),
      switchMap((deleteResult) => {
        if (deleteResult.success) {
          return of(null);
        } else if (!deleteResult.error) {
          return this.dialog.open<CustomMenuIsAssignedDialogComponent, CustomMenuIsAssignedDialogData,
            boolean>(CustomMenuIsAssignedDialogComponent, {
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
            data: {
              assigneeType: deleteResult.assigneeType,
              assigneeList: deleteResult.assigneeList
            }
          }).afterClosed().pipe(
            switchMap((res) => {
              if (res) {
                return this.customMenuService.deleteCustomMenu(customMenuId, true);
              } else {
                return of(null);
              }
            })
          );
        } else {
          const errorMessageWithTimeout = parseHttpErrorMessage(deleteResult.error, this.translate);
          setTimeout(() => {
            this.store.dispatch(new ActionNotificationShow({message: errorMessageWithTimeout.message, type: 'error'}));
          }, errorMessageWithTimeout.timeout);
          return of(null);
        }
      }
    ));
  }
}
