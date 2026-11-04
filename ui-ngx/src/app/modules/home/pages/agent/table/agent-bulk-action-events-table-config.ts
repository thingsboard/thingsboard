// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DatePipe } from '@angular/common';
import { ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { EntityLinkTableColumn } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AgentAppEventInfo, AgentApplication, AgentBulkActionEventStats } from '@shared/models/agent.models';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { AbstractAgentAppEventTableConfig } from './abstract-agent-app-event-table-config';
import { AgentEventsStatsHeaderComponent, EventsStatsFetcher } from './agent-events-stats-header.component';

export class AgentBulkActionEventsTableConfig
  extends AbstractAgentAppEventTableConfig<AgentAppEventInfo>
  implements EventsStatsFetcher {

  constructor(public readonly bulkActionId: string,
              agentService: AgentService,
              dialogService: DialogService,
              dialog: MatDialog,
              translate: TranslateService,
              datePipe: DatePipe,
              overlay: Overlay,
              viewContainerRef: ViewContainerRef,
              private readonly router: Router) {
    super(agentService, dialogService, dialog, translate, datePipe, overlay, viewContainerRef, 'agent.bulk-no-events');

    this.tableTitle = this.translate.instant('agent.executions');
    this.headerComponent = AgentEventsStatsHeaderComponent;
    this.pageMode = false;

    this.columns.push(
      new EntityLinkTableColumn<AgentAppEventInfo>('agentName',
        'agent.app-event-agent-name', '20%',
        (e) => e.agentName || '',
        (e) => e.agentId?.id ? this.agentUrl(e.agentId.id) : '',
        false),
      new EntityLinkTableColumn<AgentAppEventInfo>('applicationName',
        'agent.app-event-app-name', '25%',
        (e) => this.applicationCell(e),
        (e) => e.agentId?.id && e.applicationId?.id
          ? this.agentUrl(e.agentId.id, 'applications', e.applicationId.id)
          : '',
        false),
      ...this.eventColumns()
    );
  }

  fetchEventStats(): Observable<AgentBulkActionEventStats> {
    return this.agentService.getAgentBulkActionEventStats(this.bulkActionId);
  }

  protected fetchEvents(pageLink: PageLink): Observable<PageData<AgentAppEventInfo>> {
    return this.agentService.getAgentBulkActionEvents(
      this.bulkActionId,
      pageLink,
      this.filter.actionType || undefined,
      this.filter.processingStatus || undefined
    );
  }

  protected resolveApplication(e: AgentAppEventInfo): Observable<AgentApplication | null> {
    return this.applicationById(e);
  }

  private agentUrl(agentId: string, ...tail: string[]): string {
    return agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, ...tail);
  }
}
