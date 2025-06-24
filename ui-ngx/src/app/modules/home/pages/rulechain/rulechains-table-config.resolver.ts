///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { EdgeService } from '@core/http/edge.service';
import { forkJoin, Observable } from 'rxjs';
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from '@home/dialogs/add-entities-to-edge-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { isUndefined } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { Edge } from '@shared/models/edge.models';
import { mergeMap } from 'rxjs/operators';
import { PageData } from '@shared/models/page/page-data';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';

@Injectable()
export class RuleChainsTableConfigResolver  {

  private readonly config: EntityTableConfig<RuleChain> = new EntityTableConfig<RuleChain>();

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private customTranslate: CustomTranslatePipe) {
    this.config.entityType = EntityType.RULE_CHAIN;
    this.config.entityComponent = RuleChainComponent;
    this.config.entityTabsComponent = RuleChainTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.config.rowPointer = true;

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      {ruleChainName: ruleChain.name});
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.saveRuleChain(ruleChain);
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
    this.config.handleRowClick = ($event, ruleChain) => {
      if (this.config.isDetailsOpen()) {
        this.config.toggleEntityDetails($event, ruleChain);
      } else {
        this.openRuleChain($event, ruleChain);
      }
      return true;
    };
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<RuleChain> {
    const edgeId = route.params?.edgeId;
    const ruleChainScope = route.data?.ruleChainsType ? route.data?.ruleChainsType : 'tenant';
    this.config.componentsData = {
      ruleChainScope,
      edgeId
    };
    this.config.columns = this.configureEntityTableColumns(ruleChainScope);
    this.config.entitiesFetchFunction = this.configureEntityFunctions(ruleChainScope, edgeId);
    this.config.groupActionDescriptors = this.configureGroupActions(ruleChainScope);
    this.config.addActionDescriptors = this.configureAddActions(ruleChainScope);
    this.config.cellActionDescriptors = this.configureCellActions(ruleChainScope);
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edges') {
      this.config.entitySelectionEnabled = ruleChain => ruleChain && !ruleChain.root;
      this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root;
      this.config.entitiesDeleteEnabled = true;
      this.config.tableTitle = this.configureTableTitle(ruleChainScope, null);
    } else if (ruleChainScope === 'edge') {
      this.config.entitySelectionEnabled = ruleChain => this.config.componentsData.edge.rootRuleChainId.id !== ruleChain.id.id;
      this.edgeService.getEdge(edgeId).subscribe(edge => {
        this.config.componentsData.edge = edge;
        this.config.tableTitle = this.configureTableTitle(ruleChainScope, edge);
      });
      this.config.entitiesDeleteEnabled = false;
    }
    return this.config;
  }

  configureEntityTableColumns(ruleChainScope: string): Array<EntityColumn<RuleChain>> {
    const columns: Array<EntityColumn<RuleChain>> = [];
    columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '50%'),
      new EntityTableColumn<RuleChain>('description', 'rulechain.description', '50%',
        entity => this.customTranslate.transform(entity.additionalInfo?.description || ''),
        () => ({}), false)
    );
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edge') {
      columns.push(
        new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
          entity => {
            if (ruleChainScope === 'edge') {
              return checkBoxCell((this.config.componentsData.edge.rootRuleChainId.id === entity.id.id));
            } else {
              return checkBoxCell(entity.root);
            }
          })
      );
    } else if (ruleChainScope === 'edges') {
      columns.push(
        new EntityTableColumn<RuleChain>('root', 'rulechain.edge-template-root', '100px',
          entity => checkBoxCell(entity.root)),
        new EntityTableColumn<RuleChain>('assignToEdge', 'rulechain.assign-to-edge', '100px',
          entity => checkBoxCell(this.isAutoAssignToEdgeRuleChain(entity)), () => ({}), false)
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
          onAction: ($event) => this.config.getTable().addEntity($event)
        },
        {
          name: this.translate.instant('rulechain.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importRuleChain($event)
        }
      );
    }
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.assign-to-edge'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.assignRuleChainsToEdge($event)
        }
      );
    }
    return actions;
  }

  configureEntityFunctions(ruleChainScope: string, edgeId: string): (pageLink) => Observable<PageData<RuleChain>> {
    if (ruleChainScope === 'tenant') {
      return pageLink => this.fetchRuleChains(pageLink);
    } else if (ruleChainScope === 'edges') {
      return pageLink => this.fetchEdgeRuleChains(pageLink);
    } else if (ruleChainScope === 'edge') {
      return pageLink => this.ruleChainService.getEdgeRuleChains(edgeId, pageLink);
    }
  }

  configureTableTitle(ruleChainScope: string, edge: Edge): string {
    if (ruleChainScope === 'tenant') {
      return this.translate.instant('rulechain.rulechains');
    } else if (ruleChainScope === 'edges') {
      return this.translate.instant('edge.rulechain-templates');
    } else if (ruleChainScope === 'edge') {
      return this.config.tableTitle = edge.name + ': ' + this.translate.instant('rulechain.rulechains');
    }
  }

  configureGroupActions(ruleChainScope: string): Array<GroupActionDescriptor<RuleChain>> {
    const actions: Array<GroupActionDescriptor<RuleChain>> = [];
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.unassign-rulechains'),
          icon: 'assignment_return',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignRuleChainsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  configureCellActions(ruleChainScope: string): Array<CellActionDescriptor<RuleChain>> {
    const actions: Array<CellActionDescriptor<RuleChain>> = [];
    actions.push(
      {
        name: this.translate.instant('rulechain.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportRuleChain($event, entity)
      }
    );
    if (ruleChainScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        }
      );
    }
    if (ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-edge-template-root-rulechain'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setEdgeTemplateRootRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.set-auto-assign-to-edge'),
          icon: 'bookmark_outline',
          isEnabled: (entity) => this.isNotAutoAssignToEdgeRuleChain(entity),
          onAction: ($event, entity) => this.setAutoAssignToEdgeRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.unset-auto-assign-to-edge'),
          icon: 'bookmark',
          isEnabled: (entity) => this.isAutoAssignToEdgeRuleChain(entity),
          onAction: ($event, entity) => this.unsetAutoAssignToEdgeRuleChain($event, entity)
        }
      );
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
          icon: 'assignment_return',
          isEnabled: (entity) => entity.id.id !== this.config.componentsData.edge.rootRuleChainId.id,
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    actions.push(
      {
        name: this.translate.instant('rulechain.rulechain-details'),
          icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.config.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  importRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const expectedRuleChainType = this.config.componentsData.ruleChainScope === 'tenant' ? RuleChainType.CORE : RuleChainType.EDGE;
    this.importExport.importRuleChain(expectedRuleChainType).subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        if (this.config.componentsData.ruleChainScope === 'edges') {
          this.router.navigateByUrl(`edgeManagement/ruleChains/ruleChain/import`);
        } else {
          this.router.navigateByUrl(`ruleChains/ruleChain/import`);
        }
      }
    });
  }

  openRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.config.componentsData.ruleChainScope === 'edges') {
      this.router.navigateByUrl(`edgeManagement/ruleChains/${ruleChain.id.id}`);
    } else if (this.config.componentsData.ruleChainScope === 'edge') {
      this.router.navigateByUrl(`edgeInstances/${this.config.componentsData.edgeId}/ruleChains/${ruleChain.id.id}`);
    } else {
      this.router.navigateByUrl(`ruleChains/${ruleChain.id.id}`);
    }
  }

  saveRuleChain(ruleChain: RuleChain) {
    if (isUndefined(ruleChain.type)) {
      if (this.config.componentsData.ruleChainScope === 'tenant') {
        ruleChain.type = RuleChainType.CORE;
      } else if (this.config.componentsData.ruleChainScope === 'edges') {
        ruleChain.type = RuleChainType.EDGE;
      } else {
        // safe fallback to default core type
        ruleChain.type = RuleChainType.CORE;
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
                this.config.updateData();
              }
            );
          } else {
            this.ruleChainService.setRootRuleChain(ruleChain.id.id).subscribe(
              () => {
                this.config.updateData();
              }
            );
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
      case 'setEdgeTemplateRoot':
        this.setEdgeTemplateRootRuleChain(action.event, action.entity);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
      case 'setAutoAssignToEdge':
        this.setAutoAssignToEdgeRuleChain(action.event, action.entity);
        return true;
      case 'unsetAutoAssignToEdge':
        this.unsetAutoAssignToEdgeRuleChain(action.event, action.entity);
        return true;
    }
    return false;
  }

  setEdgeTemplateRootRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-edge-template-root-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-edge-template-root-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.setEdgeTemplateRootRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  private checkMissingToRelatedRuleChains() {
    this.edgeService.findMissingToRelatedRuleChains(this.config.componentsData.edgeId).subscribe(
      (missingRuleChains) => {
        if (missingRuleChains && Object.keys(missingRuleChains).length > 0) {
          const formattedMissingRuleChains: Array<string> = new Array<string>();
          for (const missingRuleChain of Object.keys(missingRuleChains)) {
            const arrayOfMissingRuleChains = missingRuleChains[missingRuleChain];
            const tmp = '- \'' + missingRuleChain + '\': \'' + arrayOfMissingRuleChains.join('\', ') + '\'';
            formattedMissingRuleChains.push(tmp);
          }
          const message = this.translate.instant('edge.missing-related-rule-chains-text',
            {missingRuleChains: formattedMissingRuleChains.join('<br>')});
          this.dialogService.alert(this.translate.instant('edge.missing-related-rule-chains-title'),
            message, this.translate.instant('action.close'), true).subscribe(
            () => {
              this.config.updateData();
            }
          );
        } else {
          this.config.updateData();
        }
      }
    );
  }

  assignRuleChainsToEdge($event: Event) {
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
            this.checkMissingToRelatedRuleChains();
          }
        }
      );
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
              this.checkMissingToRelatedRuleChains();
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
              this.checkMissingToRelatedRuleChains();
            }
          );
        }
      }
    );
  }

  setAutoAssignToEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-auto-assign-to-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.setAutoAssignToEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  unsetAutoAssignToEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unset-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.unset-auto-assign-to-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.unsetAutoAssignToEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.updateData();
            }
          );
        }
      }
    );
  }

  isNonRootRuleChain(ruleChain: RuleChain) {
    if (this.config.componentsData.ruleChainScope === 'edge') {
      return this.config.componentsData.edge.rootRuleChainId &&
        this.config.componentsData.edge.rootRuleChainId.id !== ruleChain.id.id;
    }
    return !ruleChain.root;
  }

  isAutoAssignToEdgeRuleChain(ruleChain) {
    return !ruleChain.root && this.config.componentsData.autoAssignToEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  isNotAutoAssignToEdgeRuleChain(ruleChain) {
    return !ruleChain.root && !this.config.componentsData.autoAssignToEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  fetchRuleChains(pageLink: PageLink) {
    return this.ruleChainService.getRuleChains(pageLink, RuleChainType.CORE);
  }

  fetchEdgeRuleChains(pageLink: PageLink) {
    return this.ruleChainService.getAutoAssignToEdgeRuleChains().pipe(
      mergeMap((ruleChains) => {
        this.config.componentsData.autoAssignToEdgeRuleChainIds = [];
        ruleChains.map(ruleChain => this.config.componentsData.autoAssignToEdgeRuleChainIds.push(ruleChain.id.id));
        return this.ruleChainService.getRuleChains(pageLink, RuleChainType.EDGE);
      })
    );
  }
}
