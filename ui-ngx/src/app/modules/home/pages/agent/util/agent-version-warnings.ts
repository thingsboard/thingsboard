// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { AgentApplicationInfo } from '@shared/models/agent.models';
import {
  AgentAppVersionWarningDialogComponent,
  AgentAppVersionWarningDialogData,
  AgentAppVersionWarningResult
} from '@home/pages/agent/dialog/agent-app-version-warning-dialog.component';

export function confirmAppVersionWarning(dialog: MatDialog,
                                         data: AgentAppVersionWarningDialogData): Observable<AgentAppVersionWarningResult> {
  return dialog.open<AgentAppVersionWarningDialogComponent, AgentAppVersionWarningDialogData, AgentAppVersionWarningResult>(
    AgentAppVersionWarningDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data
    }).afterClosed().pipe(map(res => res ?? false));
}

export function confirmUpdateDrift(dialog: MatDialog, app: AgentApplicationInfo): Observable<boolean> {
  const appVersion = app?.currentVersion || app?.templateVersion;
  const profileVersion = app?.profileTemplateVersion;
  if (!app?.applicationProfileId || !profileVersion || !appVersion || profileVersion === appVersion) {
    return of(true);
  }
  return confirmAppVersionWarning(dialog, {
    context: 'update',
    appName: app.name,
    profileName: app.profileName || '',
    appVersion,
    profileVersion,
    nextVersion: app.nextVersion
  }).pipe(map(res => res === true));
}
