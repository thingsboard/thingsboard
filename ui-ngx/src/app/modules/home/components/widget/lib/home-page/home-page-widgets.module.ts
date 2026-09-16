// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { ClusterInfoTableComponent } from '@home/components/widget/lib/home-page/cluster-info-table.component';
import { ConfiguredFeaturesComponent } from '@home/components/widget/lib/home-page/configured-features.component';
import { VersionInfoComponent } from '@home/components/widget/lib/home-page/version-info.component';
import { DocLinksWidgetComponent } from '@home/components/widget/lib/home-page/doc-links-widget.component';
import { DocLinkComponent } from '@home/components/widget/lib/home-page/doc-link.component';
import { AddDocLinkDialogComponent } from '@home/components/widget/lib/home-page/add-doc-link-dialog.component';
import { EditLinksDialogComponent } from '@home/components/widget/lib/home-page/edit-links-dialog.component';
import { GettingStartedWidgetComponent } from '@home/components/widget/lib/home-page/getting-started-widget.component';
import {
  GettingStartedCompletedDialogComponent
} from '@home/components/widget/lib/home-page/getting-started-completed-dialog.component';
import { UsageInfoWidgetComponent } from '@home/components/widget/lib/home-page/usage-info-widget.component';
import { QuickLinksWidgetComponent } from '@home/components/widget/lib/home-page/quick-links-widget.component';
import { QuickLinkComponent } from '@home/components/widget/lib/home-page/quick-link.component';
import { AddQuickLinkDialogComponent } from '@home/components/widget/lib/home-page/add-quick-link-dialog.component';
import {
  RecentDashboardsWidgetComponent
} from '@home/components/widget/lib/home-page/recent-dashboards-widget.component';
import { IotHubWidgetComponent } from '@home/components/widget/lib/home-page/iot-hub-widget.component';
import { IotHubComponentsModule } from '@home/components/iot-hub/iot-hub-components.module';

@NgModule({
  declarations:
    [
      ClusterInfoTableComponent,
      ConfiguredFeaturesComponent,
      VersionInfoComponent,
      DocLinksWidgetComponent,
      DocLinkComponent,
      AddDocLinkDialogComponent,
      EditLinksDialogComponent,
      GettingStartedWidgetComponent,
      GettingStartedCompletedDialogComponent,
      UsageInfoWidgetComponent,
      QuickLinksWidgetComponent,
      QuickLinkComponent,
      AddQuickLinkDialogComponent,
      RecentDashboardsWidgetComponent,
      IotHubWidgetComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    IotHubComponentsModule
  ],
  exports: [
    ClusterInfoTableComponent,
    ConfiguredFeaturesComponent,
    VersionInfoComponent,
    DocLinksWidgetComponent,
    DocLinkComponent,
    AddDocLinkDialogComponent,
    EditLinksDialogComponent,
    GettingStartedWidgetComponent,
    GettingStartedCompletedDialogComponent,
    UsageInfoWidgetComponent,
    QuickLinksWidgetComponent,
    QuickLinkComponent,
    AddQuickLinkDialogComponent,
    RecentDashboardsWidgetComponent,
    IotHubWidgetComponent
  ]
})
export class HomePageWidgetsModule { }
