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
import { AiCreditsPopupComponent } from '@home/components/ai/ai-credits-popup.component';
import { AiLoadingModalComponent } from '@home/components/ai/ai-loading-modal.component';
import { AiNoTelemetryModalComponent } from '@home/components/ai/ai-no-telemetry-modal.component';
import { AiAttachmentsComponent } from '@home/components/ai/ai-attachments.component';
import { AiAttachmentPreviewDialogComponent } from '@home/components/ai/ai-attachment-preview-dialog.component';
import { AiTipDirective } from '@home/components/ai/ai-tip.directive';
import { AiSpinComponent } from '@home/components/ai/ai-spin.component';
import { AiSevMarkComponent } from '@home/components/ai/ai-sev-mark.component';
import { AiLoadLinesComponent } from '@home/components/ai/ai-load-lines.component';
import { AiWorkLineComponent } from '@home/components/ai/ai-work-line.component';
import { AiActionLineComponent } from '@home/components/ai/ai-action-line.component';
import { AiRunSummaryComponent } from '@home/components/ai/ai-run-summary.component';
import { AiStartedFromComponent } from '@home/components/ai/ai-started-from.component';
import { AiAssistantButtonComponent } from '@home/components/ai/ai-assistant-button.component';
import { AiRecordComponent } from '@home/components/ai/ai-record.component';
import { AiQuestionComponent } from '@home/components/ai/ai-question.component';
import { AiSparkComponent } from '@home/components/ai/ai-spark.component';

@NgModule({
  declarations: [
    AiPromptInputComponent,
    AiAssistantPanelComponent,
    AiChatComponent,
    AiChatMessagesComponent,
    AiApprovalCardComponent,
    AiRenameDialogComponent,
    AiCreditsComponent,
    AiCreditsPopupComponent,
    AiLoadingModalComponent,
    AiNoTelemetryModalComponent,
    AiAttachmentsComponent,
    AiAttachmentPreviewDialogComponent,
    AiTipDirective,
    AiSpinComponent,
    AiWorkLineComponent,
    AiActionLineComponent,
    AiRunSummaryComponent,
    AiStartedFromComponent,
    AiAssistantButtonComponent,
    AiRecordComponent,
    AiSparkComponent,
    AiQuestionComponent,
    AiSevMarkComponent,
    AiLoadLinesComponent
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
    AiWorkLineComponent,
    AiRunSummaryComponent,
    AiSparkComponent,
    AiPromptInputComponent,
    AiAssistantPanelComponent,
    AiChatComponent,
    AiChatMessagesComponent,
    AiApprovalCardComponent,
    AiRenameDialogComponent,
    AiCreditsComponent,
    AiLoadingModalComponent,
    AiNoTelemetryModalComponent,
    AiAttachmentsComponent,
    AiQuestionComponent
  ]
})
export class AiModule {}
