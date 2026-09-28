// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, InjectionToken } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { AgentAppUnitType, agentAppUnitTypeTranslationMap } from '@shared/models/agent.models';

export interface AgentAppUnitFilterValue {
  type: AgentAppUnitType | null;
}

export interface AgentAppUnitFilterPanelData {
  value: AgentAppUnitFilterValue;
}

export const AGENT_APP_UNIT_FILTER_PANEL_DATA =
  new InjectionToken<AgentAppUnitFilterPanelData>('AgentAppUnitFilterPanelData');

@Component({
  selector: 'tb-agent-app-unit-filter-panel',
  templateUrl: './agent-app-unit-filter-panel.component.html',
  styleUrls: ['./agent-app-filter-panel.component.scss'],
  standalone: false
})
export class AgentAppUnitFilterPanelComponent {

  readonly typeOptions = Object.values(AgentAppUnitType);
  readonly typeTranslationMap = agentAppUnitTypeTranslationMap;

  readonly filterForm: UntypedFormGroup;
  result: AgentAppUnitFilterValue | null = null;

  constructor(@Inject(AGENT_APP_UNIT_FILTER_PANEL_DATA) public data: AgentAppUnitFilterPanelData,
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder) {
    this.filterForm = this.fb.group({
      type: [data.value.type ?? null]
    });
  }

  reset(): void {
    this.filterForm.reset({ type: null });
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  apply(): void {
    this.result = { ...this.filterForm.value };
    this.overlayRef.dispose();
  }
}
