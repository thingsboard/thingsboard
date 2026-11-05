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
import { AiWorkLineComponent } from '@home/components/ai/ai-work-line.component';
import { AiActionLineComponent } from '@home/components/ai/ai-action-line.component';

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
    AiActionLineComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AiPromptInputComponent,
    AiAssistantPanelComponent,
    AiChatComponent,
    AiChatMessagesComponent,
    AiApprovalCardComponent,
    AiRenameDialogComponent,
    AiCreditsComponent,
    AiLoadingModalComponent,
    AiNoTelemetryModalComponent,
    AiAttachmentsComponent
  ]
})
export class AiModule {}
