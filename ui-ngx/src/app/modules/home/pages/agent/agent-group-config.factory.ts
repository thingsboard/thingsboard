// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, inject } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AgentService } from '@core/http/agent.service';
import { AgentInfo } from '@shared/models/agent.models';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AiAssistantConfigService } from '@core/services/ai-assistant-config.service';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';

@Injectable()
export class AgentGroupConfigFactory implements EntityGroupStateConfigFactory<AgentInfo> {

  private aiAssistantConfigService = inject(AiAssistantConfigService);

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<AgentInfo>,
              private translate: TranslateService,
              private homeDialogs: HomeDialogsService,
              private agentService: AgentService,
              private router: Router) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<AgentInfo>): Observable<GroupEntityTableConfig<AgentInfo>> {
    const config = new GroupEntityTableConfig<AgentInfo>(entityGroup, params);

    config.entityComponent = AgentComponent;
    config.aiAssistantConfig = this.aiAssistantConfigService.tableConfig(AiAssistantViewType.AGENT_GROUP, AiAssistantViewType.AGENT, config.entityGroup.id);
    config.entityTitle = (agent) => agent ? agent.name : '';

    config.deleteEntityTitle = agent => this.translate.instant('agent.delete-agent-title', { agentName: agent.name });
    config.deleteEntityContent = () => this.translate.instant('agent.delete-agent-text');
    config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-agents-title', { count });
    config.deleteEntitiesContent = () => this.translate.instant('agent.delete-agents-text');

    config.loadEntity = id => this.agentService.getAgentInfoById(id.id);
    config.saveEntity = agent => this.agentService.saveAgent(agent).pipe(
      mergeMap(saved => this.agentService.getAgentInfoById(saved.id.id))
    );
    config.deleteEntity = id => this.agentService.deleteAgent(id.id);

    config.onEntityAction = action => this.onAgentAction(action, config);

    return this.groupConfigTableConfigService.prepareConfiguration(params, config);
  }

  private openAgent(_event: Event, agent: AgentInfo, config: GroupEntityTableConfig<AgentInfo>) {
    if (_event) {
      _event.stopPropagation();
    }
    const url: UrlTree = this.router.createUrlTree([agent.id.id], { relativeTo: config.getActivatedRoute() });
    this.router.navigateByUrl(url);
  }

  private manageOwnerAndGroups(event: Event, agent: AgentInfo, config: GroupEntityTableConfig<AgentInfo>) {
    this.homeDialogs.manageOwnerAndGroups(event, agent).subscribe(res => {
      if (res) {
        config.updateData();
      }
    });
  }

  private onAgentAction(action: EntityAction<AgentInfo>, config: GroupEntityTableConfig<AgentInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAgent(action.event, action.entity, config);
        return true;
      case 'manageOwnerAndGroups':
        this.manageOwnerAndGroups(action.event, action.entity, config);
        return true;
    }
    return false;
  }
}

