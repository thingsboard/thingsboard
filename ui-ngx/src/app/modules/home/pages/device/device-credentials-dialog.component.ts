// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { DeviceCredentials, DeviceCredentialsType, DeviceProfileInfo, DeviceTransportType } from '@shared/models/device.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { forkJoin, of, throwError, Observable } from 'rxjs';
import { isDefinedAndNotNull } from '@core/utils';
import { catchError, filter, map, mergeMap } from 'rxjs/operators';
import { HttpStatusCode } from '@angular/common/http';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { DeviceCertificateInfo, DeviceCertificateRevocationRequest } from '@shared/models/device-certificate-revocation.models';
import { Lwm2mSecurityConfigModels, Lwm2mSecurityType } from '@shared/models/lwm2m-security-config.models';
import {
  RevokeDeviceCertificateDialogComponent,
  RevokeDeviceCertificateDialogData
} from '@home/components/device/revoke-device-certificate-dialog.component';

export interface DeviceCredentialsDialogData {
  isReadOnly: boolean;
  deviceId: string;
  deviceProfileId: string;
}

@Component({
    selector: 'tb-device-credentials-dialog',
    templateUrl: './device-credentials-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: DeviceCredentialsDialogComponent }],
    styleUrls: ['./device-credentials-dialog.component.scss'],
    standalone: false
})
export class DeviceCredentialsDialogComponent extends
  DialogComponent<DeviceCredentialsDialogComponent, DeviceCredentials> implements OnInit, ErrorStateMatcher {

  deviceCredentialsFormGroup: UntypedFormGroup;
  deviceTransportType: DeviceTransportType;
  isReadOnly: boolean;
  loadingCredentials = true;
  certificateInfo: DeviceCertificateInfo;
  // LwM2M trust mode: the certificate is not stored with the credentials
  certificateNotStored = false;
  // Revocations apply to the whole tenant: customer users can see them, but not revoke certificates
  readonly canManageRevocations: boolean;

  private deviceCredentials: DeviceCredentials;
  private submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogData,
              private deviceService: DeviceService,
              private deviceProfileService: DeviceProfileService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DeviceCredentialsDialogComponent, DeviceCredentials>,
              public fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private translate: TranslateService) {
    super(store, router, dialogRef);

    this.isReadOnly = data.isReadOnly;
    this.canManageRevocations = !this.isReadOnly && getCurrentAuthUser(store).authority === Authority.TENANT_ADMIN;
  }

  ngOnInit(): void {
    this.deviceCredentialsFormGroup = this.fb.group({
      credential: [null]
    });
    if (this.isReadOnly) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    }
    this.loadDeviceCredentials();
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  loadDeviceCredentials() {
    const task = [
      this.deviceService.getDeviceCredentials(this.data.deviceId),
      this.deviceProfileInfo(this.data.deviceProfileId, this.data.deviceId)
    ];
    forkJoin(task).pipe(
      mergeMap(([deviceCredentials, deviceProfile]: [DeviceCredentials, DeviceProfileInfo]) =>
        this.loadCertificateInfo(deviceCredentials).pipe(
          map(certificateInfo => ({deviceCredentials, deviceProfile, certificateInfo}))
        )
      )
    ).subscribe(({deviceCredentials, deviceProfile, certificateInfo}) => {
      this.deviceTransportType = deviceProfile.transportType;
      this.deviceCredentials = deviceCredentials;
      this.certificateInfo = certificateInfo;
      this.certificateNotStored = !certificateInfo && this.isLwm2mCertificateNotStored(deviceCredentials);
      this.deviceCredentialsFormGroup.patchValue({
        credential: deviceCredentials
      }, {emitEvent: false});
      this.loadingCredentials = false;
    });
  }

  get canRevokeCertificate(): boolean {
    return this.canManageRevocations && !!this.certificateInfo && !this.certificateInfo.revocation && !this.deviceCredentialsFormGroup.dirty;
  }

  revokeCertificate(): void {
    this.dialog.open<RevokeDeviceCertificateDialogComponent, RevokeDeviceCertificateDialogData,
      DeviceCertificateRevocationRequest>(RevokeDeviceCertificateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        certificateInfo: this.certificateInfo
      }
    }).afterClosed().pipe(
      filter(request => !!request),
      mergeMap(request => this.deviceService.revokeDeviceCertificate(this.data.deviceId, request))
    ).subscribe(revocation => {
      this.certificateInfo = {...this.certificateInfo, revocation};
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('device.certificate-revocation.certificate-revoked'),
        type: 'success'
      }));
    });
  }

  private loadCertificateInfo(deviceCredentials: DeviceCredentials): Observable<DeviceCertificateInfo> {
    if (deviceCredentials.credentialsType !== DeviceCredentialsType.X509_CERTIFICATE &&
      deviceCredentials.credentialsType !== DeviceCredentialsType.LWM2M_CREDENTIALS) {
      return of(null);
    }
    return this.deviceService.getDeviceCertificateInfo(this.data.deviceId, {ignoreErrors: true}).pipe(
      catchError(() => of(null))
    );
  }

  private isLwm2mCertificateNotStored(deviceCredentials: DeviceCredentials): boolean {
    if (deviceCredentials.credentialsType !== DeviceCredentialsType.LWM2M_CREDENTIALS) {
      return false;
    }
    try {
      const credentials: Lwm2mSecurityConfigModels = JSON.parse(deviceCredentials.credentialsValue);
      return credentials?.client?.securityConfigClientMode === Lwm2mSecurityType.X509 && !credentials.client.cert;
    } catch (e) {
      return false;
    }
  }

  private deviceProfileInfo(deviceProfileId, deviceId): Observable<DeviceProfileInfo> {
    if (isDefinedAndNotNull(deviceProfileId)) {
      return this.deviceProfileService.getDeviceProfileInfo(deviceProfileId);
    } else {
      return this.deviceService.getDevice(deviceId).pipe(
        mergeMap(device => this.deviceProfileService.getDeviceProfileInfo(device.deviceProfileId.id))
      );
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value.credential;
    this.deviceCredentials = {...this.deviceCredentials, ...deviceCredentialsValue};
    this.deviceService.saveDeviceCredentials(this.deviceCredentials)
      .pipe(
        catchError((err) => {
          if (err.status === HttpStatusCode.Conflict) {
            return this.deviceService.getDeviceCredentials(this.deviceCredentials.deviceId.id);
          }
          return throwError(() => err);
        })
      )
      .subscribe(
      (deviceCredentials) => {
        this.dialogRef.close(deviceCredentials);
      }
    );
  }
}
