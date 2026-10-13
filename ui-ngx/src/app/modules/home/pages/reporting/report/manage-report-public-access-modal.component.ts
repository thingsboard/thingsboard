// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ReportService } from '@core/http/report.service';
import { ReportInfo } from '@shared/models/report.models';
import { FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, switchMap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

export interface ReportPublicModalData {
  report: ReportInfo;
}

@Component({
  selector: 'tb-manage-report-public-access-modal',
  templateUrl: './manage-report-public-access-modal.component.html',
  styleUrl: './manage-report-public-access-modal.component.scss',
  standalone: false
})
export class ManageReportPublicAccessModalComponent extends DialogComponent<ManageReportPublicAccessModalComponent> implements OnInit {

  report = this.data.report;

  publicStatusControl = new FormControl(this.report.public);

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    public dialogRef: MatDialogRef<ManageReportPublicAccessModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReportPublicModalData,
    private reportService: ReportService,
    private destroyRef: DestroyRef
  ) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.publicStatusControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(isPublic => this.reportService.updateReportPublicStatus(this.report.id.id, isPublic).pipe(
        catchError(() => {
          this.publicStatusControl.setValue(!isPublic, { emitEvent: false });
          return EMPTY;
        })
      ))
    ).subscribe(updatedReport => {
      this.report.public = updatedReport.public;
      this.report.publicKey = updatedReport.publicKey;
    });
  }

  get publicReportLink(): string {
    return this.reportService.getPublicReportDownloadUrl(this.report.publicKey);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
