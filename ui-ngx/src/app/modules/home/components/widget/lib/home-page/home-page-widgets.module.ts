///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
      RecentDashboardsWidgetComponent
    ],
  imports: [
    CommonModule,
    SharedModule
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
    RecentDashboardsWidgetComponent
  ]
})
export class HomePageWidgetsModule { }
