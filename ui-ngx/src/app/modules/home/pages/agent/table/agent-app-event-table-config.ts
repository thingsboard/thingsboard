// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DatePipe } from '@angular/common';
import { ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { AgentAppEvent, AgentApplication, AgentApplicationInfo } from '@shared/models/agent.models';
import { AbstractAgentAppEventTableConfig } from './abstract-agent-app-event-table-config';

export class AgentAppEventTableConfig extends AbstractAgentAppEventTableConfig<AgentAppEvent> {

  constructor(private readonly application: AgentApplicationInfo,
              agentService: AgentService,
              dialogService: DialogService,
              dialog: MatDialog,
              translate: TranslateService,
              datePipe: DatePipe,
              overlay: Overlay,
              viewContainerRef: ViewContainerRef) {
    super(agentService, dialogService, dialog, translate, datePipe, overlay, viewContainerRef, 'agent.app-no-events');

    this.tableTitle = this.translate.instant('agent.app-events');
    // Per-tab table, not a page-level table — don't let router query params
    // drive our paginator/sort (the parent Applications list shares the URL).
    this.pageMode = false;

    this.columns.push(...this.eventColumns());
  }

  protected fetchEvents(pageLink: PageLink): Observable<PageData<AgentAppEvent>> {
    return this.agentService.getAgentAppEvents(
      this.application.id.id,
      pageLink,
      this.filter.actionType || undefined,
      this.filter.processingStatus || undefined
    );
  }

  protected resolveApplication(): Observable<AgentApplication | null> {
    return of(this.application);
  }
}
