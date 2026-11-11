// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import { EntityId, entityIdEquals } from '@shared/models/id/entity-id';
import {
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppInstallResponse
} from '@shared/models/agent.models';

export interface UpdateSubmitContext {
  existingApplicationId: string;
  application: any;
  stepInputs: { [stepId: string]: any };
  relatedEntityId: EntityId | null;
  initialRelatedEntityId: EntityId | null;
  isProfileManagedUpdate: boolean;
  skipProfileRefetch: boolean;
}

// Owns the create/install rxjs pipelines and the related-entity assignment that
// follows a successful install/update. The flow components keep the UI state
// (the submitting flag, stepper advance, installed-app capture).
@Injectable()

export class AgentAppWizardSubmitService {

  constructor(private agentService: AgentService) {}

  upgrade(existingApplicationId: string, application: any,
          stepInputs: { [stepId: string]: any }): Observable<AgentAppEvent> {
    return this.agentService.createAgentAppEvent(existingApplicationId, {
      actionType: AgentAppEventActionType.UPGRADE,
      application,
      stepInputs
    });
  }

  update(ctx: UpdateSubmitContext): Observable<AgentAppEvent> {
    return this.assign(ctx.existingApplicationId, ctx.initialRelatedEntityId, ctx.relatedEntityId).pipe(
      mergeMap(() => this.agentService.createAgentAppEvent(ctx.existingApplicationId, {
        actionType: AgentAppEventActionType.UPDATE,
        application: ctx.application,
        stepInputs: ctx.stepInputs,
        ...(ctx.isProfileManagedUpdate ? { skipProfileRefetch: ctx.skipProfileRefetch } : {})
      }))
    );
  }

  install(application: any, stepInputs: { [stepId: string]: any },
          relatedEntityId: EntityId | null): Observable<AgentAppInstallResponse> {
    return this.agentService.installAgentApp({
      actionType: AgentAppEventActionType.INSTALL,
      application,
      stepInputs,
      ...(relatedEntityId ? { relatedEntityId } : {})
    });
  }

  private assign(appId: string | null, prev: EntityId | null,
                 current: EntityId | null): Observable<any> {
    if (!appId || entityIdEquals(prev, current)) {
      return of(null);
    }
    if (current) {
      return this.agentService.assignRelatedEntity(appId, current);
    }
    return this.agentService.unassignRelatedEntity(appId);
  }
}
