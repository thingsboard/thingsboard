// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { GroupEntityInfo } from '@shared/models/base-data';
import { Injectable } from '@angular/core';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { defaultEntityTablePermissions, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { Observable } from 'rxjs';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';

@Injectable()
export class AllEntitiesTableConfigService<T extends GroupEntityInfo<EntityId>> {

  constructor(private userPermissionsService: UserPermissionsService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private store: Store<AppState>) {
  }

  prepareConfiguration(config: EntityTableConfig<T>): EntityTableConfig<T> {
    defaultEntityTablePermissions(this.userPermissionsService, config);
    if (this.userPermissionsService.hasManageOwnerAndGroupsPermission(config.entityType) &&
      getCurrentAuthUser(this.store).authority !== Authority.SYS_ADMIN) {
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.manage-owner-and-groups'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => {
            this.manageOwnerAndGroups($event, entities, config);
          }
        }
      );
    }
    if (!config.addEntity) {
      config.addEntity = () => this.addGroupEntity(config);
    }
    return config;
  }

  private addGroupEntity(config: EntityTableConfig<T>): Observable<T> {
    return this.dialog.open<AddGroupEntityDialogComponent, AddGroupEntityDialogData<T>,
      T>(AddGroupEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  private manageOwnerAndGroups($event: MouseEvent, entities: T[],
                               config: EntityTableConfig<T>) {
    this.homeDialogs.manageOwnerAndGroups($event, entities).subscribe(
      (res) => {
        if (res) {
          config.updateData();
        }
      }
    );
  }
}
