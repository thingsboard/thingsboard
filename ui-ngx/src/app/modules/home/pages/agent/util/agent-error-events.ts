// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import {
  AgentAppEventInfo,
  agentAppEventActionTypeTranslationMap,
  isAgentScopedAppEventActionType
} from '@shared/models/agent.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

export const RECENT_AGENT_ERRORS_LIMIT = 5;

export function recentAgentErrorsPageLink(): PageLink {
  return new PageLink(RECENT_AGENT_ERRORS_LIMIT, 0, null, { property: 'createdTime', direction: Direction.DESC });
}

export function buildAgentErrorEventsTooltip(events: AgentAppEventInfo[],
                                             translate: TranslateService,
                                             datePipe: DatePipe): string {
  const header = translate.instant('agent.app-errors-recent', { count: events.length });
  const lines = events.map(e => {
    const app = e.applicationName || (isAgentScopedAppEventActionType(e.actionType) ? '' : '—');
    const actionKey = agentAppEventActionTypeTranslationMap.get(e.actionType);
    const action = actionKey ? translate.instant(actionKey) : e.actionType;
    const time = datePipe.transform(e.createdTime, 'yyyy-MM-dd HH:mm:ss');
    return `• ${app ? app + ' — ' : ''}${action} — ${time}`;
  });
  return [header, ...lines].join('\n');
}
