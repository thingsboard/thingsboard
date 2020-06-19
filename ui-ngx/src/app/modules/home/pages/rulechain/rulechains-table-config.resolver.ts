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

import { Resolve, Router } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleChain } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';

@Injectable()
export class RuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

  private readonly config: EntityTableConfig<RuleChain> = new EntityTableConfig<RuleChain>();

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router) {

    this.config.entityType = EntityType.RULE_CHAIN;
    this.config.entityComponent = RuleChainComponent;
    this.config.entityTabsComponent = RuleChainTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.config.columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%'),
      new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
        entity => {
          return checkBoxCell(entity.root);
        }),
    );

    this.config.addActionDescriptors.push(
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
    );

    this.config.cellActionDescriptors.push(
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
      },
      {
        name: this.translate.instant('rulechain.set-root'),
        icon: 'flag',
        isEnabled: (ruleChain) => !ruleChain.root,
        onAction: ($event, entity) => this.setRootRuleChain($event, entity)
      }
    );

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      { ruleChainName: ruleChain.name });
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');

    this.config.entitiesFetchFunction = pageLink => this.ruleChainService.getRuleChains(pageLink);
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.ruleChainService.saveRuleChain(ruleChain);
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
    this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root;
    this.config.entitySelectionEnabled = (ruleChain) => ruleChain && !ruleChain.root;
  }

  resolve(): EntityTableConfig<RuleChain> {
    this.config.tableTitle = this.translate.instant('rulechain.rulechains');

    return this.config;
  }

  importRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importRuleChain().subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        this.router.navigateByUrl(`ruleChains/ruleChain/import`);
      }
    });
  }

  openRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`ruleChains/${ruleChain.id.id}`);
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
          this.ruleChainService.setRootRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
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
    }
    return false;
  }

}
