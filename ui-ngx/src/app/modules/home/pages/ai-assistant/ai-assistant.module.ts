// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { AiAssistantComponent } from '@home/pages/ai-assistant/ai-assistant.component';
import { AiModule } from '@home/components/ai/ai.module';
import { AiAssistantRoutingModule } from '@home/pages/ai-assistant/ai-assistant-routing.module';
import { SolutionChatModule } from '@home/components/ai/solution/solution-chat.module';

@NgModule({
  declarations: [
    AiAssistantComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    AiModule,
    SolutionChatModule,
    AiAssistantRoutingModule
  ]
})
export class AiAssistantModule { }
