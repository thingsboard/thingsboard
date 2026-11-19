// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { ExtensionService } from '@core/http/extension.service';
import { interval, of, Subscription } from 'rxjs';
import { catchError, filter, switchMap } from 'rxjs/operators';

export interface ExtensionLogsDialogData {
  extensionId: string;
  extensionName: string;
}

@Component({
  selector: 'tb-extension-logs-dialog',
  templateUrl: './extension-logs-dialog.component.html',
  styleUrls: ['./extension-logs-dialog.component.scss'],
  standalone: false
})
export class ExtensionLogsDialogComponent extends
  DialogComponent<ExtensionLogsDialogComponent, void> implements OnInit, OnDestroy {

  logs = '';
  loading = false;
  lines = 200;
  linesOptions = [100, 200, 500, 1000, 2000];
  autoRefresh = true;

  private readonly autoRefreshIntervalMs = 5000;
  private autoRefreshSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private extensionService: ExtensionService,
              @Inject(MAT_DIALOG_DATA) public data: ExtensionLogsDialogData,
              public dialogRef: MatDialogRef<ExtensionLogsDialogComponent, void>,
              public translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.loadLogs();
    // Silent background refresh: no progress bar flicker. switchMap cancels a still-running request when
    // the next tick fires, so requests cannot pile up on a slow pod and a late response can never
    // overwrite a newer snapshot (a plain subscribe applies responses in arrival order).
    //
    // The tick is also skipped while the tab is hidden. Every poll costs the server a pod list plus a log
    // read per pod, and a dialog left open in a background tab is nobody watching — a browser throttles
    // the timer but does not stop it. Nothing is needed on the way back: the next tick after the tab is
    // visible again refreshes it.
    this.autoRefreshSubscription = interval(this.autoRefreshIntervalMs).pipe(
      filter(() => this.autoRefresh && !document.hidden),
      // catchError INSIDE switchMap, so a failed poll ends that inner request only. Without it the first
      // error completes the outer interval and auto-refresh stops for good — which a single 429 from the
      // endpoint's rate limit, or one blip from the orchestrator, would be enough to trigger.
      switchMap(() => this.extensionService.getExtensionLogs(this.data.extensionId, this.lines, {ignoreErrors: true}).pipe(
        catchError(() => of(this.translate.instant('extension.logs-not-available')))
      ))
    ).subscribe((logs) => this.logs = logs);
  }

  ngOnDestroy(): void {
    this.autoRefreshSubscription?.unsubscribe();
    super.ngOnDestroy();
  }

  // Manual load only (initial open, Refresh button, line-count change). `loading` drives the progress
  // bar and is cleared by the handler of the load that set it — the background refresh never touches it.
  loadLogs() {
    this.loading = true;
    this.extensionService.getExtensionLogs(this.data.extensionId, this.lines, {ignoreErrors: true}).subscribe({
      next: (logs) => {
        this.logs = logs;
        this.loading = false;
      },
      error: () => {
        this.logs = this.translate.instant('extension.logs-not-available');
        this.loading = false;
      }
    });
  }

  close() {
    this.dialogRef.close();
  }
}
