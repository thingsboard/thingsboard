// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  DeviceCertificateInfo,
  DeviceCertificateRevocationRequest
} from '@shared/models/device-certificate-revocation.models';

export interface RevokeDeviceCertificateDialogData {
  certificateInfo?: DeviceCertificateInfo;
}

@Component({
  selector: 'tb-revoke-device-certificate-dialog',
  templateUrl: './revoke-device-certificate-dialog.component.html',
  styleUrls: ['./revoke-device-certificate-dialog.component.scss'],
  standalone: false
})
export class RevokeDeviceCertificateDialogComponent extends
  DialogComponent<RevokeDeviceCertificateDialogComponent, DeviceCertificateRevocationRequest> implements OnInit {

  readonly reasonMaxLength = 255;

  revokeFormGroup: UntypedFormGroup;
  certificateInfo: DeviceCertificateInfo;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: RevokeDeviceCertificateDialogData,
              public dialogRef: MatDialogRef<RevokeDeviceCertificateDialogComponent, DeviceCertificateRevocationRequest>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.certificateInfo = this.data?.certificateInfo;
    this.revokeFormGroup = this.fb.group({
      certificate: [null, this.certificateInfo ? [] : [Validators.required, Validators.pattern(/-----BEGIN CERTIFICATE-----/)]],
      certificateFile: [null],
      reason: [null, [Validators.maxLength(this.reasonMaxLength)]]
    });
    this.revokeFormGroup.get('certificateFile').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(content => {
      if (content) {
        this.revokeFormGroup.get('certificate').setValue(content);
        this.revokeFormGroup.get('certificate').markAsDirty();
        this.revokeFormGroup.get('certificate').markAsTouched();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  revoke(): void {
    const value = this.revokeFormGroup.value;
    const request: DeviceCertificateRevocationRequest = {
      reason: value.reason?.trim() || null
    };
    if (!this.certificateInfo) {
      request.certificate = value.certificate;
    }
    this.dialogRef.close(request);
  }
}
