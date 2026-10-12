// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { RevokedCertificatesTableComponent } from '@home/pages/device-certificate-revocation/revoked-certificates-table.component';

@NgModule({
  declarations: [
    RevokedCertificatesTableComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ]
})
export class DeviceCertificateRevocationModule { }
