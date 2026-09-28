// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData } from '@shared/models/base-data';
import { Injectable } from '@angular/core';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { defaultEntityTablePermissions, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { Operation } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, mergeMap } from 'rxjs/operators';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';

@Injectable()
export class AllEntitiesTableConfigService<T extends BaseData<EntityId>> {

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private store: Store<AppState>) {
  }

  prepareConfiguration(config: EntityTableConfig<T>): EntityTableConfig<T> {
    defaultEntityTablePermissions(this.userPermissionsService, config);
    if (this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, config.entityType) &&
      getCurrentAuthUser(this.store).authority !== Authority.SYS_ADMIN) {
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.change-owner'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => {
            this.changeEntitiesOwner($event, entities, config);
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

  private changeEntitiesOwner($event: MouseEvent, entities: T[],
                              config: EntityTableConfig<T>) {
    const ignoreErrors = entities.length > 1;
    const onOwnerSelected = (targetOwnerId: EntityId) => this.homeDialogs.confirm(
      this.translate.instant('entity-group.confirm-change-owner-title', {count: entities.length}),
      this.translate.instant('entity-group.confirm-change-owner-text')).pipe(
      mergeMap((res) => {
        if (res) {
          const changeOwnerObservables: Observable<any>[] = [];
          entities.forEach((entity) => {
            changeOwnerObservables.push(
              this.entityGroupService.changeEntityOwner(targetOwnerId, entity.id, null, {ignoreErrors}).pipe(
                catchError((err) => {
                  if (ignoreErrors) {
                    return of(null);
                  } else {
                    throw err;
                  }
                })
              )
            );
          });
          return forkJoin(changeOwnerObservables).pipe(
            mergeMap(() => of(true)),
            catchError((err) => {
              if (ignoreErrors) {
                return of(true);
              } else {
                throw err;
              }
            })
          );
        } else {
          return of(false);
        }
      })
    );
    let excludeOwnerIds;
    const uniqueOwnerIds = [...new Set(entities.map(e => e.ownerId?.id))];
    if (uniqueOwnerIds.length > 1) {
      excludeOwnerIds = [];
    } else {
      excludeOwnerIds = uniqueOwnerIds;
    }
    this.homeDialogs.selectOwner($event, 'entity-group.change-owner', 'entity-group.change-owner',
      'entity-group.select-target-owner',
      'entity-group.no-owners-matching',
      'entity-group.target-owner-required', onOwnerSelected,
      excludeOwnerIds).subscribe(
      (targetOwnerId) => {
        if (targetOwnerId) {
          config.updateData();
        }
      }
    );
  }
}
