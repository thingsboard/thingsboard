// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, EventEmitter,
  Inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityGroupStateInfo, GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import {
  EntityGroupInfo,
  EntityGroupParams,
  resolveGroupParams,
  ShortEntityView
} from '@shared/models/entity-group.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DialogService } from '@core/services/dialog.service';
import { WINDOW } from '@core/services/window.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroupsTableConfig } from '@home/components/group/entity-groups-table-config';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';

// @dynamic
@Component({
    selector: 'tb-group-entities-table',
    templateUrl: './group-entities-table.component.html',
    styleUrls: ['./group-entities-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class GroupEntitiesTableComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy, OnChanges {

  isGroupDetailsOpen = false;

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  @ViewChild('entityGroupDetailsPanel') entityGroupDetailsPanel: EntityDetailsPanelComponent;

  @Input()
  groupParams: EntityGroupParams;

  @Input()
  entityGroup: EntityGroupStateInfo<BaseData<HasId>>;

  entityGroupConfig: GroupEntityTableConfig<BaseData<HasId>>;

  entityGroupDetailsConfig: EntityGroupsTableConfig;

  private rxSubscriptions = new Array<Subscription>();

  updateBreadcrumbs = new EventEmitter();

  constructor(protected store: Store<AppState>,
              @Inject(WINDOW) private window: Window,
              private route: ActivatedRoute,
              private router: Router,
              private entityGroupsTableConfigResolver: EntityGroupsTableConfigResolver,
              private entityGroupConfigResolver: EntityGroupConfigResolver,
              private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private dialogService: DialogService,
              private cd: ChangeDetectorRef) {
    super(store);
  }


  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entityGroup' && change.currentValue) {
          this.init(change.currentValue, this.groupParams);
        }
      }
    }
  }

  ngOnInit(): void {
    if (this.entityGroup) {
      this.init(this.entityGroup, this.groupParams);
    } else {
      this.rxSubscriptions.push(this.route.data.subscribe(
        (data) => {
          const groupParams = resolveGroupParams(this.route.snapshot);
          this.init(data.entityGroup, groupParams);
        }
      ));
    }
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  onToggleEntityGroupDetails() {
    this.isGroupDetailsOpen = !this.isGroupDetailsOpen;
    if (this.isGroupDetailsOpen) {
      this.entitiesTable.isDetailsOpen = false;
    }
  }

  onToggleEntityDetails($event: Event, entity: ShortEntityView) {
    this.entitiesTable.toggleEntityDetails($event, entity);
  }

  onEntityGroupUpdated(entity: BaseData<HasId>) {
    const entityGroup = entity as EntityGroupInfo;
    if (this.groupParams.hierarchyView) {
      this.groupParams.hierarchyCallbacks.groupUpdated(entityGroup);
    }
    this.reloadEntityGroupConfig(entityGroup);
  }

  onEntityGroupAction(action: EntityAction<BaseData<HasId>>) {
    if (action.action === 'delete') {
      this.deleteEntityGroup(action.event, action.entity as EntityGroupInfo);
    }
  }

  private reloadEntityGroup() {
    this.entityGroupService.getEntityGroup(this.entityGroup.id.id).subscribe(
      (entityGroup) => {
        if (this.groupParams.hierarchyView) {
          this.groupParams.hierarchyCallbacks.groupUpdated(entityGroup);
        }
        this.reloadEntityGroupConfig(entityGroup, true);
      }
    );
  }

  private reloadEntityGroupConfig(entityGroup: EntityGroupInfo, reloadGroupDetails = false) {
    this.entityGroupConfigResolver.constructGroupConfig<BaseData<HasId>>(this.groupParams, entityGroup).subscribe(
      (entityGroupConfig) => {
        this.init(entityGroupConfig, this.groupParams, false, false, reloadGroupDetails);
      }
    );
  }

  private deleteEntityGroup($event: Event, entity: EntityGroupInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.entityGroupDetailsConfig.deleteEntityTitle(entity),
      this.entityGroupDetailsConfig.deleteEntityContent(entity),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.entityGroupDetailsConfig.deleteEntity(entity.id).subscribe(
          () => {
            if (this.groupParams.hierarchyView) {
              this.groupParams.hierarchyCallbacks.groupDeleted(this.groupParams.nodeId, entity.id.id);
            } else {
              this.window.history.back();
            }
          }
        );
      }
    });
  }

  private init(entityGroup: EntityGroupStateInfo<BaseData<HasId>>, groupParams: EntityGroupParams, closeGroupDetails = true,
               reloadGroupDetailsConfig = true, reloadGroupDetails = false) {
    if (closeGroupDetails) {
      this.isGroupDetailsOpen = false;
    }
    this.entityGroup = entityGroup;
    this.groupParams = groupParams || resolveGroupParams(this.route.snapshot);
    this.entityGroupConfig = entityGroup.entityGroupConfig;
    if (groupParams.hierarchyView) {
      this.entityGroupConfig.pageMode = false;
    }
    this.entityGroupConfig.onToggleEntityGroupDetails = this.onToggleEntityGroupDetails.bind(this);
    this.entityGroupConfig.onToggleEntityDetails = this.onToggleEntityDetails.bind(this);
    this.entitiesTable.detailsPanelOpened.subscribe((isDetailsOpened: boolean) => {
      if (isDetailsOpened) {
        this.isGroupDetailsOpen = false;
      }
    });
    if (reloadGroupDetailsConfig) {
      this.entityGroupDetailsConfig =
        this.entityGroupsTableConfigResolver.resolveEntityGroupTableConfig(this.groupParams, false) as EntityGroupsTableConfig;
      this.entityGroupDetailsConfig.componentsData = {
        isGroupEntitiesView: true,
        reloadEntityGroup: this.reloadEntityGroup.bind(this)
      };
    }
    if (reloadGroupDetails && this.entityGroupDetailsPanel) {
      this.entityGroupDetailsPanel.reloadEntity();
    }
    this.cd.detectChanges();
    this.updateBreadcrumbs.emit();
  }

  ngAfterViewInit(): void {
  }

}
