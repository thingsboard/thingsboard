// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminRoutingModule } from './admin-routing.module';
import { SharedModule } from '@app/shared/shared.module';
import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { GeneralSettingsComponent } from '@modules/home/pages/admin/general-settings.component';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { MailTemplatesComponent } from '@home/pages/admin/mail-templates.component';
import { WhiteLabelingComponent } from '@home/pages/admin/white-labeling.component';
import { PaletteComponent } from '@home/pages/admin/palette.component';
import { PaletteDialogComponent } from '@home/pages/admin/palette-dialog.component';
import { CustomCssDialogComponent } from '@home/pages/admin/custom-css-dialog.component';
import { SelfRegistrationComponent } from '@home/pages/admin/self-registration.component';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { SendTestSmsDialogComponent } from '@home/pages/admin/send-test-sms-dialog.component';
import { HomeSettingsComponent } from '@home/pages/admin/home-settings.component';
import { ResourceTabsComponent } from '@home/pages/admin/resource/resource-tabs.component';
import { ResourcesTableHeaderComponent } from '@home/pages/admin/resource/resources-table-header.component';
import { QueueComponent } from '@home/pages/admin/queue/queue.component';
import { RepositoryAdminSettingsComponent } from '@home/pages/admin/repository-admin-settings.component';
import { AutoCommitAdminSettingsComponent } from '@home/pages/admin/auto-commit-admin-settings.component';
import { TwoFactorAuthSettingsComponent } from '@home/pages/admin/two-factor-auth-settings.component';
import { OAuth2Module } from '@home/pages/admin/oauth2/oauth2.module';
import { JsLibraryTableHeaderComponent } from '@home/pages/admin/resource/js-library-table-header.component';
import { JsResourceComponent } from '@home/pages/admin/resource/js-resource.component';
import { NgxFlowModule } from '@flowjs/ngx-flow';
import { ResourceLibraryTabsComponent } from '@home/pages/admin/resource/resource-library-tabs.component';
import { LicenseManagementComponent } from '@home/pages/admin/license-management.component';
import { AddLicenseItemDialogComponent } from '@home/pages/admin/add-license-item-dialog.component';
import { LicenseHandOffDialogComponent } from '@home/pages/admin/license-hand-off-dialog.component';

@NgModule({
  declarations:
    [
      GeneralSettingsComponent,
      MailServerComponent,
      MailTemplatesComponent,
      SmsProviderComponent,
      SendTestSmsDialogComponent,
      WhiteLabelingComponent,
      SecuritySettingsComponent,
      PaletteComponent,
      PaletteDialogComponent,
      CustomCssDialogComponent,
      SelfRegistrationComponent,
      SecuritySettingsComponent,
      HomeSettingsComponent,
      ResourceTabsComponent,
      ResourceLibraryTabsComponent,
      ResourcesTableHeaderComponent,
      JsResourceComponent,
      JsLibraryTableHeaderComponent,
      QueueComponent,
      RepositoryAdminSettingsComponent,
      AutoCommitAdminSettingsComponent,
      TwoFactorAuthSettingsComponent,
      LicenseManagementComponent,
      AddLicenseItemDialogComponent,
      LicenseHandOffDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AdminRoutingModule,
    OAuth2Module,
    NgxFlowModule
  ]
})
export class AdminModule { }
