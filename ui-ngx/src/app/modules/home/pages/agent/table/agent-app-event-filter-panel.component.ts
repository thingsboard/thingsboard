// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, InjectionToken } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  agentAppEventActionTypeTranslationMap,
  AgentAppEventActionType,
  AgentProcessingStatus,
  agentProcessingStatusTranslationMap
} from '@shared/models/agent.models';

export interface AgentAppEventFilterValue {
  actionType: AgentAppEventActionType | null;
  processingStatus: AgentProcessingStatus | null;
}

export interface AgentAppEventFilterPanelData {
  value: AgentAppEventFilterValue;
}

export const AGENT_APP_EVENT_FILTER_PANEL_DATA =
  new InjectionToken<AgentAppEventFilterPanelData>('AgentAppEventFilterPanelData');

@Component({
  selector: 'tb-agent-app-event-filter-panel',
  templateUrl: './agent-app-event-filter-panel.component.html',
  styleUrls: ['./agent-app-filter-panel.component.scss'],
  standalone: false
})
export class AgentAppEventFilterPanelComponent {

  readonly actionOptions = Object.values(AgentAppEventActionType)
    .filter(a => a !== AgentAppEventActionType.ROLLBACK);
  readonly statusOptions = Object.values(AgentProcessingStatus);
  readonly actionTypeTranslationMap = agentAppEventActionTypeTranslationMap;
  readonly statusTranslationMap = agentProcessingStatusTranslationMap;

  readonly filterForm: UntypedFormGroup;
  result: AgentAppEventFilterValue | null = null;

  constructor(@Inject(AGENT_APP_EVENT_FILTER_PANEL_DATA) public data: AgentAppEventFilterPanelData,
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder) {
    this.filterForm = this.fb.group({
      actionType: [data.value.actionType ?? null],
      processingStatus: [data.value.processingStatus ?? null]
    });
  }

  reset(): void {
    this.filterForm.reset({ actionType: null, processingStatus: null });
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  apply(): void {
    this.result = { ...this.filterForm.value };
    this.overlayRef.dispose();
  }
}
