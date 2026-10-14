// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import {
  checkBoxCell,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DialogService } from '@core/services/dialog.service';
import { EdgeProfile } from '@shared/models/edge-profile.models';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { EdgeProfileComponent } from '@home/pages/edge-profile/edge-profile.component';
import { EdgeProfileTabsComponent } from '@home/pages/edge-profile/edge-profile-tabs.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Direction } from '@shared/models/page/sort-order';
import { ImportExportService } from '@shared/import-export/import-export.service';

@Injectable()
export class EdgeProfilesTableConfigResolver {

  private readonly config: EntityTableConfig<EdgeProfile> = new EntityTableConfig<EdgeProfile>();

  constructor(private store: Store<AppState>,
              private edgeProfileService: EdgeProfileService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialogService: DialogService,
              private importExportService: ImportExportService) {
    this.config.entityType = EntityType.EDGE_PROFILE;
    this.config.entityComponent = EdgeProfileComponent;
    this.config.entityTabsComponent = EdgeProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.EDGE_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.EDGE_PROFILE);
    this.config.rowPointer = true;
    this.config.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.config.columns.push(
      new DateEntityTableColumn<EdgeProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<EdgeProfile>('name', 'edge-profile.name', '60%'),
      new EntityTableColumn<EdgeProfile>('description', 'edge-profile.description', '40%', entity => entity.description || '', () => ({}), false),
      // keyed on the entity attribute ('isDefault'), which doubles as the sort property sent to the server
      new EntityTableColumn<EdgeProfile>('isDefault', 'edge-profile.default', '60px', entity => checkBoxCell(entity.default))
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('edge-profile.set-default'),
        icon: 'flag',
        // a profile with no root rule chain is rejected by setDefaultEdgeProfile, and one created from this UI starts
        // without one, so without this the action is offered and always fails
        isEnabled: (profile) => this.isTenantAdmin() && !profile.default && !!profile.rootRuleChainId &&
          this.userPermissionsService.hasGenericPermission(Resource.EDGE_PROFILE, Operation.WRITE),
        onAction: ($event, entity) => this.setDefaultEdgeProfile($event, entity)
      },
      {
        name: this.translate.instant('edge-profile.reapply'),
        icon: 'autorenew',
        isEnabled: () => this.isTenantAdmin() && this.userPermissionsService.hasGenericPermission(Resource.EDGE_PROFILE, Operation.WRITE),
        onAction: ($event, entity) => this.reapply($event, entity)
      }
    );
    this.config.deleteEnabled = (profile) => this.isTenantAdmin() && profile && !profile.default &&
      this.userPermissionsService.hasGenericPermission(Resource.EDGE_PROFILE, Operation.DELETE);
    this.config.entitySelectionEnabled = (profile) => this.isTenantAdmin() && profile && !profile.default &&
      this.userPermissionsService.hasGenericPermission(Resource.EDGE_PROFILE, Operation.DELETE);

    this.config.deleteEntityTitle = profile => this.translate.instant('edge-profile.delete-edge-profile-title', {profileName: profile.name});
    this.config.deleteEntityContent = () => this.translate.instant('edge-profile.delete-edge-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('edge-profile.delete-edge-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('edge-profile.delete-edge-profiles-text');

    this.config.entitiesFetchFunction = pageLink => this.edgeProfileService.getEdgeProfiles(pageLink);
    this.config.loadEntity = id => this.edgeProfileService.getEdgeProfile(id.id);
    this.config.saveEntity = profile => this.edgeProfileService.saveEdgeProfile(profile);
    this.config.deleteEntity = id => this.edgeProfileService.deleteEdgeProfile(id.id);
    this.config.onEntityAction = action => this.onEdgeProfileAction(action);
  }

  resolve(): EntityTableConfig<EdgeProfile> {
    this.config.tableTitle = this.translate.instant('edge-profile.edge-profiles');
    // add / delete / readonly follow the user's generic EDGE_PROFILE permissions
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    // but every write endpoint of EdgeProfileController is tenant-admin only, so a customer user reaching this page
    // through its own edge would be offered add, edit and delete and get a 403 from each of them
    if (!this.isTenantAdmin()) {
      this.config.addEnabled = false;
      this.config.entitiesDeleteEnabled = false;
      this.config.detailsReadonly = () => true;
    }
    return this.config;
  }

  private isTenantAdmin(): boolean {
    return getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN;
  }

  setDefaultEdgeProfile($event: Event, profile: EdgeProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('edge-profile.set-default-title', {profileName: profile.name}),
      this.translate.instant('edge-profile.set-default-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.edgeProfileService.setDefaultEdgeProfile(profile.id.id).subscribe(() => this.config.updateData());
      }
    });
  }

  reapply($event: Event, profile: EdgeProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('edge-profile.reapply-title', {profileName: profile.name}),
      this.translate.instant('edge-profile.reapply-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.edgeProfileService.applyEdgeProfile(profile.id.id).subscribe(() => {
          this.store.dispatch(new ActionNotificationShow({
            message: this.translate.instant('edge-profile.reapply-started'),
            type: 'success', duration: 1500, verticalPosition: 'bottom', horizontalPosition: 'right'
          }));
        });
      }
    });
  }

  exportEdgeProfile($event: Event, profile: EdgeProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    // the shared helper is what device and asset profiles use; hand-rolling the strip list here means every field
    // later added to it (externalId already) is dropped for them and kept for edge profiles
    const copy = this.importExportService.prepareProfileExport<EdgeProfile>({...profile});
    this.importExportService.exportJson(copy, profile.name, true);
  }

  onEdgeProfileAction(action: EntityAction<EdgeProfile>): boolean {
    switch (action.action) {
      case 'open':
        this.openEdgeProfile(action.event, action.entity);
        return true;
      case 'setDefault':
        this.setDefaultEdgeProfile(action.event, action.entity);
        return true;
      case 'reapply':
        this.reapply(action.event, action.entity);
        return true;
      case 'export':
        this.exportEdgeProfile(action.event, action.entity);
        return true;
    }
    return false;
  }

  private openEdgeProfile($event: Event, profile: EdgeProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([profile.id.id], {relativeTo: this.config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }
}
