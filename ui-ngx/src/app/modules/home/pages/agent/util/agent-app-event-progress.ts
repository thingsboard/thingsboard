// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AgentAppEvent, AgentApplication } from '@shared/models/agent.models';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';

/**
 * Opens the event-progress dialog for an action. All dispatches (install, update, upgrade,
 * restart, delete) route through here so the "monitor progress" UX is consistent. An
 * agent-scoped event, such as the agent upgrading itself, has no application and passes null.
 *
 * Returns the dialog's afterClosed observable so the caller can trigger a
 * list refresh (or navigate away) once the user dismisses the dialog.
 */
export function openAgentAppEventProgress(
  dialog: MatDialog,
  application: AgentApplication | null,
  event: AgentAppEvent
): Observable<boolean> {
  return dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
    AgentAppEventProgressDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data: { application, event }
    }
  ).afterClosed();
}
