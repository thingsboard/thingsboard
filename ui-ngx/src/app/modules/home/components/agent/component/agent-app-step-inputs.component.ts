// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { StepBinding, VolumeChoice } from '@home/pages/agent/util/agent-app-step-inputs';

// Renders the user-input blocks (backup volumes, pull images) the active
// template exposes for the current action. Bindings are mutated in place, so
// the host's buildStepInputs(bindings) reads back the user's choices.
@Component({
  selector: 'tb-agent-app-step-inputs',
  templateUrl: './agent-app-step-inputs.component.html',
  styleUrls: ['./agent-app-step-inputs.component.scss'],
  standalone: false
})
export class AgentAppStepInputsComponent {

  @Input() bindings: StepBinding[] = [];

  toggleBackupVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }
}
