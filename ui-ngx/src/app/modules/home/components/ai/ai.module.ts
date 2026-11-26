// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';
import { AiAssistantPanelComponent } from '@home/components/ai/ai-assistant-panel.component';
import { AiChatMessagesComponent } from '@home/components/ai/ai-chat-messages.component';
import { AiChatComponent } from '@home/components/ai/ai-chat.component';
import { AiApprovalCardComponent } from '@home/components/ai/ai-approval-card.component';
import { AiRenameDialogComponent } from '@home/components/ai/ai-rename-dialog.component';
import { AiCreditsComponent } from '@home/components/ai/ai-credits.component';
import { AiCreditsOutComponent } from '@home/components/ai/ai-credits-out.component';
import { AiLimitOutComponent } from '@home/components/ai/ai-limit-out.component';
import { AiCreditsPopupComponent } from '@home/components/ai/ai-credits-popup.component';
import { AiLoadingModalComponent } from '@home/components/ai/ai-loading-modal.component';
import { AiNoTelemetryModalComponent } from '@home/components/ai/ai-no-telemetry-modal.component';
import { AiAttachmentsComponent } from '@home/components/ai/ai-attachments.component';
import { AiAttachmentPreviewDialogComponent } from '@home/components/ai/ai-attachment-preview-dialog.component';
import { AiTipDirective } from '@home/components/ai/ai-tip.directive';
import { AiSpinComponent } from '@home/components/ai/ai-spin.component';
import { AiSevMarkComponent } from '@home/components/ai/ai-sev-mark.component';
import { AiShortcutsDialogComponent } from '@home/components/ai/ai-shortcuts-dialog.component';
import { AiLoadLinesComponent } from '@home/components/ai/ai-load-lines.component';
import { AiChatTitleComponent } from '@home/components/ai/ai-chat-title.component';
import { AiWorkLineComponent } from '@home/components/ai/ai-work-line.component';
import { AiActionLineComponent } from '@home/components/ai/ai-action-line.component';
import { AiActionDetailComponent } from '@home/components/ai/ai-action-detail.component';
import { AiRunSummaryComponent } from '@home/components/ai/ai-run-summary.component';
import { AiStartedFromComponent } from '@home/components/ai/ai-started-from.component';
import { AiAssistantButtonComponent } from '@home/components/ai/ai-assistant-button.component';
import { AiRecordComponent } from '@home/components/ai/ai-record.component';
import { AiRecordCardComponent } from '@home/components/ai/ai-record-card.component';
import { AiRecordsComponent } from '@home/components/ai/ai-records.component';
import { AiCardActionsComponent } from '@home/components/ai/ai-card-actions.component';
import { AiDisclosureComponent } from '@home/components/ai/ai-disclosure.component';
import { AiQuestionComponent } from '@home/components/ai/ai-question.component';
import { AiPlanComponent } from '@home/components/ai/ai-plan.component';
import { AiHandOverComponent } from '@home/components/ai/ai-hand-over.component';
import { AiArtifactPaneComponent } from '@home/components/ai/ai-artifact-pane.component';
import { AiFileBodyComponent } from '@home/components/ai/ai-file-body.component';
import { AiSparkComponent } from '@home/components/ai/ai-spark.component';
import { AiMarkComponent } from '@home/components/ai/ai-mark.component';
import { AiCliGuideComponent } from '@home/components/ai/ai-cli-guide.component';
import { AiFadeInImageDirective } from '@home/components/ai/ai-fade-in-image.directive';

@NgModule({
  declarations: [
    AiFadeInImageDirective,
    AiPromptInputComponent,
    AiAssistantPanelComponent,
    AiChatComponent,
    AiChatMessagesComponent,
    AiApprovalCardComponent,
    AiRenameDialogComponent,
    AiCreditsComponent,
    AiCreditsOutComponent,
    AiLimitOutComponent,
    AiCreditsPopupComponent,
    AiLoadingModalComponent,
    AiNoTelemetryModalComponent,
    AiAttachmentsComponent,
    AiAttachmentPreviewDialogComponent,
    AiTipDirective,
    AiSpinComponent,
    AiWorkLineComponent,
    AiActionLineComponent,
    AiActionDetailComponent,
    AiRunSummaryComponent,
    AiStartedFromComponent,
    AiAssistantButtonComponent,
    AiRecordComponent,
    AiRecordCardComponent,
    AiRecordsComponent,
    AiCardActionsComponent,
    AiDisclosureComponent,
    AiSparkComponent,
    AiMarkComponent,
    AiQuestionComponent,
    AiPlanComponent,
    AiHandOverComponent,
    AiArtifactPaneComponent,
    AiFileBodyComponent,
    AiSevMarkComponent,
    AiLoadLinesComponent,
    AiShortcutsDialogComponent,
    AiChatTitleComponent,
    AiCliGuideComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AiAssistantButtonComponent,
    AiSpinComponent,
    AiSevMarkComponent,
    AiLoadLinesComponent,
    AiChatTitleComponent,
    AiWorkLineComponent,
    AiRunSummaryComponent,
    AiSparkComponent,
    AiMarkComponent,
    AiPromptInputComponent,
    AiAssistantPanelComponent,
    AiChatComponent,
    AiChatMessagesComponent,
    AiApprovalCardComponent,
    AiRenameDialogComponent,
    AiCreditsComponent,
    AiCreditsOutComponent,
    AiLimitOutComponent,
    AiLoadingModalComponent,
    AiNoTelemetryModalComponent,
    AiAttachmentsComponent,
    AiQuestionComponent,
    AiPlanComponent,
    AiHandOverComponent,
    AiArtifactPaneComponent,
    AiFileBodyComponent,
    AiRecordComponent,
    AiRecordCardComponent,
    AiRecordsComponent,
    AiCardActionsComponent,
    AiDisclosureComponent,
    AiCliGuideComponent
  ]
})
export class AiModule {}
