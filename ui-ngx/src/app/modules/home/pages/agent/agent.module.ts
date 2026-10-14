// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { AgentSharedComponentsModule } from '@home/components/agent/agent-shared-components.module';
import { AgentRoutingModule } from '@home/pages/agent/agent-routing.module';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import { AgentProfileComponent } from '@home/pages/agent/agent-profile.component';
import { AgentProfileTabsComponent } from '@home/pages/agent/agent-profile-tabs.component';
import { AgentProfileAutocompleteComponent } from '@home/pages/agent/agent-profile-autocomplete.component';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import {
  AgentInstallInstructionsDialogComponent
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AgentProfileCreatedDialogComponent
} from '@home/pages/agent/agent-profile-created-dialog.component';
import {
  AgentProfileMergedProfilesComponent
} from '@home/pages/agent/agent-profile-merged-profiles.component';
import {
  AgentExecutionsSidePanelComponent
} from '@home/pages/agent/agent-executions-side-panel.component';
import {
  AgentProfileAssignProfileDialogComponent
} from '@home/pages/agent/dialog/agent-profile-assign-profile-dialog.component';
import {
  AgentProfileBulkActionDialogComponent
} from '@home/pages/agent/dialog/agent-profile-bulk-action-dialog.component';
import {
  AgentAppDeleteDialogComponent
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppVersionWarningDialogComponent
} from '@home/pages/agent/dialog/agent-app-version-warning-dialog.component';
import {
  AgentProfilePresetDialogComponent
} from '@home/pages/agent/dialog/agent-profile-preset-dialog.component';
import {
  AgentAppEventProgressDialogComponent
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';
import {
  AgentUpgradeDialogComponent
} from '@home/pages/agent/dialog/agent-upgrade-dialog.component';
import {
  AgentAppProfileUpgradeDialogComponent
} from '@home/pages/agent/dialog/agent-app-profile-upgrade-dialog.component';
import {
  AgentAppAssignCompareDialogComponent
} from '@home/pages/agent/dialog/agent-app-assign-compare-dialog.component';
import {
  AgentAppProfileWizardComponent
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';
import {
  AgentProfileWizardComponent
} from '@home/pages/agent/wizard/agent-profile-wizard.component';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';
import {
  AgentAppEventTableComponent
} from '@home/pages/agent/table/agent-app-event-table.component';
import {
  AgentBulkActionEventTableComponent
} from '@home/pages/agent/table/agent-bulk-action-event-table.component';
import {
  AgentEventsStatsHeaderComponent
} from '@home/pages/agent/table/agent-events-stats-header.component';
import {
  AgentAppEventFilterPanelComponent
} from '@home/pages/agent/table/agent-app-event-filter-panel.component';
import {
  AgentAppUnitTableComponent
} from '@home/pages/agent/table/agent-app-unit-table.component';
import {
  AgentAppUnitFilterPanelComponent
} from '@home/pages/agent/table/agent-app-unit-filter-panel.component';
import { AgentEventsPageComponent } from '@home/pages/agent/agent-events-page.component';
import { AgentBulkActionEventsPageComponent } from '@home/pages/agent/agent-bulk-action-events-page.component';
import {
  LogHighlightPipe,
  LogViewerComponent
} from '@home/pages/agent/log-viewer/log-viewer.component';
import {
  AgentAppUnitLogViewerPageComponent
} from '@home/pages/agent/log-viewer/agent-app-unit-log-viewer-page.component';
import {
  AgentMetricsStripComponent
} from '@home/pages/agent/metrics/agent-metrics-strip.component';
import {
  AgentApplicationsPageComponent
} from '@home/pages/agent/metrics/agent-applications-page.component';
import {
  AgentApplicationDetailsPageComponent
} from '@home/pages/agent/metrics/agent-application-details-page.component';
import {
  AgentMultiEntityMetricsPanelComponent
} from '@home/pages/agent/metrics/agent-multi-entity-metrics-panel.component';
import { AGENT_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { AgentGroupConfigFactory } from '@home/pages/agent/agent-group-config.factory';
import { AgentAppArgumentsComponent } from '@home/pages/agent/component/agent-app-arguments.component';
import { AgentAppArgumentPanelComponent } from '@home/pages/agent/component/agent-app-argument-panel.component';
import { AgentAppProfilesPageComponent } from '@home/pages/agent/agent-app-profiles-page.component';
import {
  AgentAppProfileQuickStartComponent
} from '@home/pages/agent/agent-app-profile-quick-start.component';

@NgModule({
  declarations: [
    AgentComponent,
    AgentTabsComponent,
    AgentProfileComponent,
    AgentProfileTabsComponent,
    AgentProfileAutocompleteComponent,
    AgentAppProfileComponent,
    AgentInstallInstructionsDialogComponent,
    AgentProfileCreatedDialogComponent,
    AgentProfileMergedProfilesComponent,
    AgentExecutionsSidePanelComponent,
    AgentProfileAssignProfileDialogComponent,
    AgentProfileBulkActionDialogComponent,
    AgentAppDeleteDialogComponent,
    AgentAppVersionWarningDialogComponent,
    AgentProfilePresetDialogComponent,
    AgentAppEventProgressDialogComponent,
    AgentUpgradeDialogComponent,
    AgentAppProfileUpgradeDialogComponent,
    AgentAppAssignCompareDialogComponent,
    AgentAppProfileWizardComponent,
    AgentProfileWizardComponent,
    AgentApplicationComponent,
    AgentApplicationTabsComponent,
    AgentAppEventTableComponent,
    AgentBulkActionEventTableComponent,
    AgentEventsStatsHeaderComponent,
    AgentAppEventFilterPanelComponent,
    AgentAppUnitTableComponent,
    AgentAppUnitFilterPanelComponent,
    AgentEventsPageComponent,
    AgentBulkActionEventsPageComponent,
    LogViewerComponent,
    LogHighlightPipe,
    AgentAppUnitLogViewerPageComponent,
    AgentMetricsStripComponent,
    AgentApplicationsPageComponent,
    AgentApplicationDetailsPageComponent,
    AgentMultiEntityMetricsPanelComponent,
    AgentAppArgumentsComponent,
    AgentAppArgumentPanelComponent,
    AgentAppProfilesPageComponent,
    AgentAppProfileQuickStartComponent,
  ],
  imports: [
    CommonModule,
    ScrollingModule,
    SharedModule,
    HomeDialogsModule,
    HomeComponentsModule,
    AgentSharedComponentsModule,
    AgentRoutingModule,
  ],
  providers: [
    DatePipe,
    {
      provide: AGENT_GROUP_CONFIG_FACTORY,
      useClass: AgentGroupConfigFactory
    }
  ]
})
export class AgentModule { }
