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

import {ActivatedRouteSnapshot, Resolve, Route, Router} from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn, EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor, HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleChain, ruleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { EdgeService } from "@core/http/edge.service";
import {map, mergeMap} from "rxjs/operators";
import { forkJoin, Observable } from "rxjs";
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from "@home/dialogs/add-entities-to-edge-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { isDefined, isUndefined } from "@core/utils";
import { PageLink } from "@shared/models/page/page-link";
import { Edge } from "@shared/models/edge.models";

@Injectable()
export class RuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

  private readonly config: EntityTableConfig<RuleChain> = new EntityTableConfig<RuleChain>();
  private edge: Edge;

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {
    this.config.entityType = EntityType.RULE_CHAIN;
    this.config.entityComponent = RuleChainComponent;
    this.config.entityTabsComponent = RuleChainTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      { ruleChainName: ruleChain.name });
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.saveRuleChain(ruleChain);
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<RuleChain> {
    const routeParams = route.params;
    this.config.componentsData = {
      ruleChainScope: route.data.ruleChainsType,
      edgeId: routeParams.edgeId
    };
    this.config.columns = this.configureEntityTableColumns(this.config.componentsData.ruleChainScope);
    this.configureEntityFunctions(this.config.componentsData.ruleChainScope);
    this.config.groupActionDescriptors = this.configureGroupActions(this.config.componentsData.ruleChainScope);
    this.config.addActionDescriptors = this.configureAddActions(this.config.componentsData.ruleChainScope);
    this.config.cellActionDescriptors = this.configureCellActions(this.config.componentsData.ruleChainScope);
    if (this.config.componentsData.ruleChainScope === 'tenant' || this.config.componentsData.ruleChainScope === 'edges') {
      this.config.entitySelectionEnabled = ruleChain => ruleChain && !ruleChain.root;
      this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root;
      this.config.entitiesDeleteEnabled = true;
    }
    else if (this.config.componentsData.ruleChainScope === 'edge') {
      this.config.entitySelectionEnabled = ruleChain => this.config.componentsData.edge.rootRuleChainId.id != ruleChain.id.id;
      this.edgeService.getEdge(this.config.componentsData.edgeId).subscribe(edge => {
        this.config.componentsData.edge = edge;
        this.config.tableTitle = edge.name + ': ' + this.translate.instant('rulechain.edge-rulechains');
      });
      this.config.entitiesDeleteEnabled = false;
    }
    return this.config;
  }

  configureEntityTableColumns(ruleChainScope: string): Array<EntityColumn<RuleChain>> {
    const columns: Array<EntityColumn<RuleChain>> = [];
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edge') {
      columns.push(
        new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
        new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%'),
        new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
          entity => {
          if (ruleChainScope === 'edge') {
            return checkBoxCell((this.config.componentsData.edge.rootRuleChainId.id == entity.id.id));
          } else {
            return checkBoxCell(entity.root);
          }
          })
      );
    }
    if (ruleChainScope === 'edges') {
      columns.push(
        new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
        new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%'),
        new EntityTableColumn<RuleChain>('root', 'rulechain.default-root', '60px',
          entity => {
            return checkBoxCell(entity.root);
          })
      );
    }
    return columns;
  }

  configureAddActions(ruleChainScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.create-new-rulechain'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        },
        {
          name: this.translate.instant('rulechain.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importRuleChain($event)
        }
      )
    }
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.assign-new-rulechain'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addRuleChainsToEdge($event)
        }
      )
    }
    return actions;
  }

  configureEntityFunctions(ruleChainScope: string): void {
    if (ruleChainScope === 'tenant') {
      this.config.tableTitle = this.translate.instant('rulechain.core-rulechains');
      this.config.entitiesFetchFunction = pageLink => this.fetchRuleChains(pageLink);
    } else if (ruleChainScope === 'edges') {
      this.config.tableTitle = this.translate.instant('rulechain.edge-rulechains');
      this.config.entitiesFetchFunction = pageLink => this.fetchEdgeRuleChains(pageLink);
    } else if (ruleChainScope === 'edge') {
      this.config.entitiesFetchFunction = pageLink => this.ruleChainService.getEdgeRuleChains(this.config.componentsData.edgeId, pageLink);
    }
  }

  configureGroupActions(ruleChainScope: string): Array<GroupActionDescriptor<RuleChain>> {
    const actions: Array<GroupActionDescriptor<RuleChain>> = [];
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.unassign-rulechains'),
          icon: 'portable_wifi_off',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignRuleChainsFromEdge($event, entities)
        }
      )
    }
    return actions;
  }

  configureCellActions(ruleChainScope: string): Array<CellActionDescriptor<RuleChain>> {
    const actions: Array<CellActionDescriptor<RuleChain>> = [];
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.open-rulechain'),
          icon: 'settings_ethernet',
          isEnabled: () => true,
          onAction: ($event, entity) => this.openRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.export'),
          icon: 'file_download',
          isEnabled: () => true,
          onAction: ($event, entity) => this.exportRuleChain($event, entity)
        }
      )
    }
    if (ruleChainScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        }
      )
    }
    if (ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-default-root-edge'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setDefaultRootEdgeRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.set-default-edge'),
          icon: 'bookmark_outline',
          isEnabled: (entity) => this.isNonDefaultEdgeRuleChain(entity),
          onAction: ($event, entity) => this.setDefaultEdgeRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.remove-default-edge'),
          icon: 'bookmark',
          isEnabled: (entity) => this.isDefaultEdgeRuleChain(entity),
          onAction: ($event, entity) => this.removeDefaultEdgeRuleChain($event, entity)
        }
      )
    }
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        },
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'portable_wifi_off',
          isEnabled: (entity) => entity.id.id != this.config.componentsData.edge.rootRuleChainId.id,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      )
    }
    return actions;
  }

  importRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importRuleChain().subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        this.router.navigateByUrl(`${this.router.routerState.snapshot.url}/ruleChain/import`);
      }
    });
  }

  openRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`${this.router.routerState.snapshot.url}/${ruleChain.id.id}`);
  }

  saveRuleChain(ruleChain: RuleChain) {
    if (isUndefined(ruleChain.type)) {
      if (this.config.componentsData.ruleChainScope == 'tenant') {
        ruleChain.type = ruleChainType.core;
      } else if (this.config.componentsData.ruleChainScope == 'edges') {
        ruleChain.type = ruleChainType.edge;
      }
    }
    return this.ruleChainService.saveRuleChain(ruleChain);
  }

  exportRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportRuleChain(ruleChain.id.id);
  }

  setRootRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-root-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-root-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          if (this.config.componentsData.ruleChainScope === 'edge') {
            this.ruleChainService.setEdgeRootRuleChain(this.config.componentsData.edgeId, ruleChain.id.id).subscribe(
              (edge) => {
                this.config.componentsData.edge = edge;
                this.config.table.updateData();
              }
            )
          } else {
            this.ruleChainService.setRootRuleChain(ruleChain.id.id).subscribe(
              () => {
                this.config.table.updateData();
              }
            )
          }
        }
      }
    );
  }

  onRuleChainAction(action: EntityAction<RuleChain>): boolean {
    switch (action.action) {
      case 'open':
        this.openRuleChain(action.event, action.entity);
        return true;
      case 'export':
        this.exportRuleChain(action.event, action.entity);
        return true;
      case 'setRoot':
        this.setRootRuleChain(action.event, action.entity);
        return true;
      case 'setDefaultRoot':
        this.setDefaultRootEdgeRuleChain(action.event, action.entity);
        return true;
    }
    return false;
  }

  setDefaultRootEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-default-root-edge-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-default-root-edge-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.setDefaultRootEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  addRuleChainsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.config.componentsData.edgeId,
        entityType: EntityType.RULE_CHAIN
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      }
    )
  }

  unassignFromEdge($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unassign-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.unassign-rulechain-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.unassignRuleChainFromEdge(this.config.componentsData.edgeId, ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignRuleChainsFromEdge($event: Event, ruleChains: Array<RuleChain>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unassign-rulechains-from-edge-title', {count: ruleChains.length}),
      this.translate.instant('rulechain.unassign-rulechains-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          ruleChains.forEach(
            (ruleChain) => {
              tasks.push(this.ruleChainService.unassignRuleChainFromEdge(this.config.componentsData.edgeId, ruleChain.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  setDefaultEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-default-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-default-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.ruleChainService.addDefaultEdgeRuleChain(ruleChain.id.id).subscribe(
          () => {
            this.config.table.updateData();
          }
        )
      }
      }
    );
  }

  removeDefaultEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.remove-default-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.remove-default-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.removeDefaultEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          )
        }
      }
    );
  }

  isNonRootRuleChain(ruleChain: RuleChain) {
    if (this.config.componentsData.ruleChainScope === 'edge') {
      return (isDefined(this.config.componentsData.edge.rootRuleChainId) && this.config.componentsData.edge.rootRuleChainId != null && this.config.componentsData.edge.rootRuleChainId.id != ruleChain.id.id);
    }
    return (isDefined(ruleChain)) && !ruleChain.root;
  }

  isDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && this.config.componentsData.defaultEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  isNonDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && !this.config.componentsData.defaultEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  fetchRuleChains(pageLink: PageLink) {
    return this.ruleChainService.getRuleChains(pageLink, ruleChainType.core);
  }

  fetchEdgeRuleChains(pageLink: PageLink) {
    this.config.componentsData.defaultEdgeRuleChainIds = [];
    this.ruleChainService.getDefaultEdgeRuleChains().subscribe(ruleChains => {
        ruleChains.map(ruleChain => this.config.componentsData.defaultEdgeRuleChainIds.push(ruleChain.id.id));
    });
    return this.ruleChainService.getRuleChains(pageLink, ruleChainType.edge);
  }
}
