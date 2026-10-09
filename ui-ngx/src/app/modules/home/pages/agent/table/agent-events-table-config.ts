// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DatePipe } from '@angular/common';
import { ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityLinkTableColumn } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AgentAppEventInfo, AgentApplication, AgentInfo } from '@shared/models/agent.models';
import { AbstractAgentAppEventTableConfig } from './abstract-agent-app-event-table-config';

export class AgentEventsTableConfig extends AbstractAgentAppEventTableConfig<AgentAppEventInfo> {

  constructor(private readonly agentId: string,
              agent: AgentInfo | null,
              agentService: AgentService,
              dialogService: DialogService,
              dialog: MatDialog,
              translate: TranslateService,
              datePipe: DatePipe,
              overlay: Overlay,
              viewContainerRef: ViewContainerRef,
              private readonly agentScopeBasePath: string) {
    super(agentService, dialogService, dialog, translate, datePipe, overlay, viewContainerRef, 'agent.app-no-events');

    this.tableTitle = (agent?.name ? agent.name + ': ' : '') + this.translate.instant('agent.app-events');

    this.columns.push(
      new EntityLinkTableColumn<AgentAppEventInfo>('applicationName',
        'agent.app-event-app-name', '25%',
        (e) => this.applicationCell(e),
        (e) => e.applicationId?.id
          ? `${this.agentScopeBasePath}/applications/${e.applicationId.id}`
          : '',
        false),
      ...this.eventColumns()
    );
  }

  protected fetchEvents(pageLink: PageLink): Observable<PageData<AgentAppEventInfo>> {
    return this.agentService.getAgentAppEventInfosByAgentId(
      this.agentId,
      pageLink,
      this.filter.actionType || undefined,
      this.filter.processingStatus || undefined
    );
  }

  protected resolveApplication(e: AgentAppEventInfo): Observable<AgentApplication | null> {
    return this.applicationById(e);
  }
}
