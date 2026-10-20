// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { AgentApplication, AgentApplicationType, AgentAppEvent } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult
} from '@home/components/agent/wizard/agent-app-install-wizard.component';

@Injectable({ providedIn: 'root' })
export class AgentDeployDialogService {

  constructor(private dialog: MatDialog,
              private agentService: AgentService) {}

  getManagedApp(relatedEntity: EntityId): Observable<AgentApplication | null> {
    return this.agentService.getAgentApplicationByRelatedEntity(
      relatedEntity.entityType as string, relatedEntity.id,
      { ignoreErrors: true, ignoreLoading: true }).pipe(
      catchError(() => of(null))
    );
  }

  open(appType: AgentApplicationType, relatedEntity: EntityId,
       options?: { showBack?: boolean }): Observable<AgentAppInstallWizardResult> {
    return this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
      AgentAppInstallWizardComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
        data: {
          mode: 'install',
          selectAgent: true,
          lockedType: appType,
          lockedRelatedEntity: relatedEntity,
          navigateToAgentOnFinish: true,
          showBack: !!options?.showBack
        }
      }
    ).afterClosed();
  }
}
