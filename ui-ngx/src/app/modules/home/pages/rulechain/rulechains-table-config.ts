// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityColumn, EntityTableColumn,
  EntityTableConfig, GroupActionDescriptor, HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { RuleChain, RuleChainParams, RuleChainType } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { RuleChainComponent } from '@home/pages/rulechain/rulechain.component';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Operation, Resource } from '@shared/models/security.models';
import { isUndefined } from '@core/utils';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { PageLink } from '@shared/models/page/page-link';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { Authority } from '@shared/models/authority.enum';

export class RuleChainsTableConfig extends EntityTableConfig<RuleChain> {

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private customTranslate: CustomTranslatePipe,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService,
              private iotHubActions: IotHubActionsService,
              private params: ActivatedRouteSnapshot | RuleChainParams) {
    super((params as any)?.hierarchyView ? undefined : resolveGroupParams(params as any));

    this.entityType = EntityType.RULE_CHAIN;
    this.entityComponent = RuleChainComponent;
    this.entityTabsComponent = RuleChainTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.rowPointer = true;

    this.componentsData = this.setComponentsData(this.params);
    this.columns = this.configureEntityTableColumns(this.componentsData.ruleChainScope);
    this.groupActionDescriptors = this.configureGroupActions(this.componentsData.ruleChainScope);
    this.addActionDescriptors = this.configureAddActions(this.componentsData.ruleChainScope);
    this.cellActionDescriptors = this.configureCellActions(this.componentsData);
    this.entitiesFetchFunction = this.configureEntityFunctions(this.componentsData.ruleChainScope);

    this.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title', {ruleChainName: ruleChain.name});
    this.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.saveEntity = ruleChain => this.saveRuleChain(ruleChain);
    this.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.onEntityAction = action => this.onRuleChainAction(action, this.componentsData);
    this.handleRowClick = ($event, ruleChain) => {
      if (this.isDetailsOpen()) {
        this.toggleEntityDetails($event, ruleChain);
      } else {
        this.openRuleChain($event, ruleChain, this.componentsData);
      }
      return true;
    };

    this.configureRuleChainScope();
    defaultEntityTablePermissions(this.userPermissionsService, this);
  }

  private setComponentsData(params: any): RuleChainParams {
    let ruleChainScope: 'tenant' | 'edges';
    if (params?.hierarchyView) {
      ruleChainScope = params.ruleChainScope;
    } else {
      ruleChainScope = params.data.ruleChainsType ? params.data.ruleChainsType : 'tenant';
    }
    return {
      ruleChainScope,
      hierarchyView: params?.hierarchyView,
      entityGroupId: params?.entityGroupId,
      childEntityGroupId: params?.childEntityGroupId,
      customerId: params?.customerId
    };
  }

  private configureEntityTableColumns(ruleChainScope: string): Array<EntityColumn<RuleChain>> {
    const columns: Array<EntityColumn<RuleChain>> = [];

    this.entityTitle = (ruleChain) => ruleChain ?
      this.utils.customTranslation(ruleChain.name, ruleChain.name) : '';

    columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '50%', this.entityTitle),
      new EntityTableColumn<RuleChain>('description', 'rulechain.description', '50%',
        entity => this.customTranslate.transform(entity.additionalInfo?.description || ''),
        () => ({}), false)
    );
    if (ruleChainScope === 'tenant') {
      columns.push(new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px', entity => checkBoxCell(entity.root)));
    }
    return columns;
  }

  private configureGroupActions(ruleChainScope: string): Array<GroupActionDescriptor<RuleChain>> {
    const actions: Array<GroupActionDescriptor<RuleChain>> = [];
    return actions;
  }

  private configureAddActions(ruleChainScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.create-new-rulechain'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.getTable().addEntity($event)
        },
        {
          name: this.translate.instant('rulechain.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importRuleChain($event)
        }
      );
      if (this.userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)) {
        actions.push(
          {
            name: this.translate.instant('iot-hub.add-from-iot-hub'),
            icon: 'hub',
            isEnabled: () => true,
            onAction: (_$event) => this.addRuleChainFromIotHub()
          }
        );
      }
    }
    return actions;
  }

  private addRuleChainFromIotHub(): void {
    const ruleChainScope = this.componentsData.ruleChainScope;
    const ruleChainType = ruleChainScope === 'edges' ? 'EDGE' : 'CORE';
    this.iotHubActions.addItem(ItemType.RULE_CHAIN, { itemSubType: ruleChainType }).subscribe(result => {
      if (result?.descriptor?.type === 'RULE_CHAIN' && result.descriptor.ruleChainId?.id) {
        const ruleChainId = result.descriptor.ruleChainId.id;
        if (ruleChainScope === 'edges') {
          this.router.navigateByUrl(`edgeManagement/templates/ruleChains/${ruleChainId}`);
        } else {
          this.router.navigateByUrl(`ruleChains/${ruleChainId}`);
        }
      }
    });
  }

  private importRuleChain($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    const expectedRuleChainType = this.componentsData.ruleChainScope === 'tenant' ? RuleChainType.CORE : RuleChainType.EDGE;
    this.importExport.importRuleChain(expectedRuleChainType).subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        if (this.componentsData.ruleChainScope === 'edges') {
          this.router.navigateByUrl(`edgeManagement/templates/ruleChains/ruleChain/import`);
        } else {
          this.router.navigateByUrl(`ruleChains/ruleChain/import`);
        }
      }
    });
  }

  private configureCellActions(params: RuleChainParams): Array<CellActionDescriptor<RuleChain>> {
    const actions: Array<CellActionDescriptor<RuleChain>> = [];
    actions.push(
      {
        name: this.translate.instant('rulechain.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportRuleChain($event, entity)
      }
    );
    if (params.ruleChainScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        }
      );
    }
    actions.push(
      {
        name: this.translate.instant('rulechain.rulechain-details'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.toggleEntityDetails($event, entity)
      }
    );
    return actions;
  }

  private configureEntityFunctions(ruleChainScope: string): (pageLink) => Observable<PageData<RuleChain>> {
    if (ruleChainScope === 'tenant') {
      return pageLink => this.fetchRuleChains(pageLink);
    } else if (ruleChainScope === 'edges') {
      return pageLink => this.fetchEdgeRuleChains(pageLink);
    }
  }

  private fetchRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, RuleChainType.CORE);
  }

  private fetchEdgeRuleChains(pageLink: PageLink): Observable<PageData<RuleChain>> {
    return this.ruleChainService.getRuleChains(pageLink, RuleChainType.EDGE);
  }

  private saveRuleChain(ruleChain: RuleChain): Observable<RuleChain> {
    if (isUndefined(ruleChain.type)) {
      if (this.componentsData.ruleChainScope === 'tenant') {
        ruleChain.type = RuleChainType.CORE;
      } else if (this.componentsData.ruleChainScope === 'edges') {
        ruleChain.type = RuleChainType.EDGE;
      } else {
        // safe fallback to default core type
        ruleChain.type = RuleChainType.CORE;
      }
    }
    return this.ruleChainService.saveRuleChain(ruleChain);
  }

  private onRuleChainAction(action: EntityAction<RuleChain>, params: RuleChainParams): boolean {
    switch (action.action) {
      case 'open':
        this.openRuleChain(action.event, action.entity, params);
        return true;
      case 'export':
        this.exportRuleChain(action.event, action.entity);
        return true;
      case 'setRoot':
        this.setRootRuleChain(action.event, action.entity);
        return true;
    }
    return false;
  }

  private openRuleChain($event: Event, ruleChain: RuleChain, params?: RuleChainParams): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.componentsData.ruleChainScope === 'edges') {
      this.router.navigateByUrl(`edgeManagement/templates/ruleChains/${ruleChain.id.id}`);
    } else {
      this.router.navigateByUrl(`ruleChains/${ruleChain.id.id}`);
    }
  }

  private exportRuleChain($event: Event, ruleChain: RuleChain): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportRuleChain(ruleChain.id.id);
  }

  private setRootRuleChain($event: Event, ruleChain: RuleChain): void {
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
          this.ruleChainService.setRootRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.updateData();
            }
          );
        }
      }
    );
  }

  private isNonRootRuleChain(ruleChain: RuleChain): boolean {
    return !ruleChain.root;
  }

  private configureRuleChainScope(): void {
    if (this.componentsData.ruleChainScope === 'tenant') {
      this.entitySelectionEnabled = ruleChain => ruleChain && !ruleChain.root &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.tableTitle = this.configureTableTitle(this.componentsData.ruleChainScope);
    } else if (this.componentsData.ruleChainScope === 'edges') {
      // an edge template's root now lives on the edge profile; the legacy 'root' flag is not shown and the server
      // rejects deleting a chain that is the root of a profile or of an edge
      this.entitySelectionEnabled = ruleChain => ruleChain &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.deleteEnabled = (ruleChain) => ruleChain &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.tableTitle = this.configureTableTitle(this.componentsData.ruleChainScope);
    }
  }

  private configureTableTitle(ruleChainScope: string): string {
    if (ruleChainScope === 'tenant') {
      return this.translate.instant('rulechain.rulechains');
    } else if (ruleChainScope === 'edges') {
      return this.translate.instant('edge.rulechain-templates');
    }
  }
}
