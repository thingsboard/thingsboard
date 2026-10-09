// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { AgentInfo } from '@shared/models/agent.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { AgentComponent } from '@home/pages/agent/agent.component';

@Injectable({ providedIn: 'root' })
export class AgentCreateDialogService {

  constructor(private dialog: MatDialog,
              private agentService: AgentService) {}

  create(): Observable<AgentInfo> {
    const config = new EntityTableConfig<AgentInfo>();
    config.entityType = EntityType.AGENT;
    config.entityComponent = AgentComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT);
    config.entityResources = entityTypeResources.get(EntityType.AGENT);
    config.entityTitle = (agent) => agent ? agent.name : '';
    config.componentsData = { agentScope: 'tenant', customerId: null };
    config.saveEntity = (agent) => this.agentService.saveAgent(agent).pipe(
      mergeMap((saved) => this.agentService.getAgentInfoById(saved.id.id))
    );
    return this.dialog.open<AddEntityDialogComponent, AddEntityDialogData<AgentInfo>, AgentInfo>(
      AddEntityDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { entitiesTableConfig: config }
      }
    ).afterClosed();
  }
}
