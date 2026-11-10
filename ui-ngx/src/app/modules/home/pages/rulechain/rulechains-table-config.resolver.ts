// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { inject, Injectable } from '@angular/core';

import { AiAssistantConfigService, isEdgeScope } from '@core/services/ai-assistant-config.service';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { RuleChain, RuleChainParams } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { EdgeService } from '@core/http/edge.service';
import { MatDialog } from '@angular/material/dialog';
import { UtilsService } from '@core/services/utils.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { RuleChainsTableConfig } from '@home/pages/rulechain/rulechains-table-config';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';

@Injectable()
export class RuleChainsTableConfigResolver  {

  private aiAssistantConfigService = inject(AiAssistantConfigService);

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private customTranslate: CustomTranslatePipe,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService,
              private iotHubActions: IotHubActionsService) {
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<RuleChain> {
    const config = this.resolveRuleChainsTableConfig(route);
    config.aiAssistantConfig = isEdgeScope(config.componentsData.ruleChainScope) ? null :
      this.aiAssistantConfigService.tableConfig(AiAssistantViewType.RULE_CHAIN_LIST, AiAssistantViewType.RULE_CHAIN);
    return config;
  }

  resolveRuleChainsTableConfig(params: ActivatedRouteSnapshot | RuleChainParams): EntityTableConfig<RuleChain> {
    return new RuleChainsTableConfig (
      this.ruleChainService,
      this.dialogService,
      this.dialog,
      this.importExport,
      this.itembuffer,
      this.edgeService,
      this.translate,
      this.datePipe,
      this.router,
      this.customTranslate,
      this.utils,
      this.userPermissionsService,
      this.iotHubActions,
      params
    );
  }
}
