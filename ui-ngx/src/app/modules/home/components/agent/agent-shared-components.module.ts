// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AgentAppInstallWizardComponent } from '@home/components/agent/wizard/agent-app-install-wizard.component';
import { AgentAppInstallFlowComponent } from '@home/components/agent/wizard/agent-app-install-flow.component';
import { AgentAppUpdateFlowComponent } from '@home/components/agent/wizard/agent-app-update-flow.component';
import { AgentAppUpgradeFlowComponent } from '@home/components/agent/wizard/agent-app-upgrade-flow.component';
import { AgentAppStepInputsComponent } from '@home/components/agent/component/agent-app-step-inputs.component';
import { AgentComposeDiffComponent } from '@home/components/agent/component/agent-compose-diff.component';
import { AgentComposeEditorComponent } from '@home/components/agent/component/agent-compose-editor.component';
import { AgentRelatedEntityAutocompleteComponent } from '@home/components/agent/agent-related-entity-autocomplete.component';
import { AgentGatewayCreateDialogComponent } from '@home/components/agent/dialog/agent-gateway-create-dialog.component';
import { AgentAutoProvisionDialogComponent } from '@home/components/agent/dialog/agent-auto-provision-dialog.component';
import { AgentAutocompleteComponent } from '@home/components/agent/agent-autocomplete.component';
import { AgentInstallInstructionsComponent } from '@home/components/agent/agent-install-instructions.component';
import { AgentDeployStatusComponent } from '@home/components/agent/agent-deploy-status.component';
import { AgentAppProfileListComponent } from '@home/components/agent/agent-app-profile-list.component';

@NgModule({
  declarations: [
    AgentAppInstallWizardComponent,
    AgentAppInstallFlowComponent,
    AgentAppUpdateFlowComponent,
    AgentAppUpgradeFlowComponent,
    AgentAppStepInputsComponent,
    AgentComposeDiffComponent,
    AgentComposeEditorComponent,
    AgentRelatedEntityAutocompleteComponent,
    AgentGatewayCreateDialogComponent,
    AgentAutoProvisionDialogComponent,
    AgentAutocompleteComponent,
    AgentInstallInstructionsComponent,
    AgentDeployStatusComponent,
    AgentAppProfileListComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AgentAppInstallWizardComponent,
    AgentAppInstallFlowComponent,
    AgentAppUpdateFlowComponent,
    AgentAppUpgradeFlowComponent,
    AgentAppStepInputsComponent,
    AgentComposeDiffComponent,
    AgentComposeEditorComponent,
    AgentRelatedEntityAutocompleteComponent,
    AgentGatewayCreateDialogComponent,
    AgentAutoProvisionDialogComponent,
    AgentAutocompleteComponent,
    AgentInstallInstructionsComponent,
    AgentDeployStatusComponent,
    AgentAppProfileListComponent
  ]
})
export class AgentSharedComponentsModule {
}
