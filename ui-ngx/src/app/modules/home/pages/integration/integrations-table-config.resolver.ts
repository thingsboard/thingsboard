// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DestroyRef, Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  Integration,
  IntegrationInfo,
  IntegrationParams,
  resolveIntegrationParams
} from '@shared/models/integration.models';
import { IntegrationService } from '@core/http/integration.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EdgeService } from '@core/http/edge.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { IntegrationsTableConfig } from '@home/pages/integration/integrations-table-config';
import { PageLink } from '@shared/models/page/page-link';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';

@Injectable()
export class IntegrationsTableConfigResolver  {

  constructor(private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private dialogService: DialogService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private destroyRef: DestroyRef,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<Integration, PageLink, IntegrationInfo> {
    return this.resolveIntegrationsTableConfig(resolveIntegrationParams(route));
  }

  resolveIntegrationsTableConfig(params: IntegrationParams): EntityTableConfig<Integration, PageLink, IntegrationInfo> {
    return new IntegrationsTableConfig(
      this.integrationService,
      this.userPermissionsService,
      this.edgeService,
      this.translate,
      this.datePipe,
      this.router,
      this.utils,
      this.dialogService,
      this.dialog,
      this.entityDebugSettingsService,
      this.destroyRef,
      params
    );
  }
}
