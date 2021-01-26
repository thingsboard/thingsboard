///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DetailsPanelComponent } from '@home/components/details-panel.component';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { AuditLogDetailsDialogComponent } from '@home/components/audit-log/audit-log-details-dialog.component';
import { AuditLogTableComponent } from '@home/components/audit-log/audit-log-table.component';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { RelationTableComponent } from '@home/components/relation/relation-table.component';
import { RelationDialogComponent } from '@home/components/relation/relation-dialog.component';
import { AlarmTableHeaderComponent } from '@home/components/alarm/alarm-table-header.component';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AttributeTableComponent } from '@home/components/attribute/attribute-table.component';
import { AddAttributeDialogComponent } from '@home/components/attribute/add-attribute-dialog.component';
import { EditAttributeValuePanelComponent } from '@home/components/attribute/edit-attribute-value-panel.component';
import { DashboardComponent } from '@home/components/dashboard/dashboard.component';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { LegendComponent } from '@home/components/widget/legend.component';
import { AliasesEntitySelectPanelComponent } from '@home/components/alias/aliases-entity-select-panel.component';
import { AliasesEntitySelectComponent } from '@home/components/alias/aliases-entity-select.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { EntityAliasesDialogComponent } from '@home/components/alias/entity-aliases-dialog.component';
import { EntityFilterViewComponent } from '@home/components/entity/entity-filter-view.component';
import { EntityAliasDialogComponent } from '@home/components/alias/entity-alias-dialog.component';
import { EntityFilterComponent } from '@home/components/entity/entity-filter.component';
import { RelationFiltersComponent } from '@home/components/relation/relation-filters.component';
import { EntityAliasSelectComponent } from '@home/components/alias/entity-alias-select.component';
import { DataKeysComponent } from '@home/components/widget/data-keys.component';
import { DataKeyConfigDialogComponent } from '@home/components/widget/data-key-config-dialog.component';
import { DataKeyConfigComponent } from '@home/components/widget/data-key-config.component';
import { LegendConfigPanelComponent } from '@home/components/widget/legend-config-panel.component';
import { LegendConfigComponent } from '@home/components/widget/legend-config.component';
import { ManageWidgetActionsComponent } from '@home/components/widget/action/manage-widget-actions.component';
import { WidgetActionDialogComponent } from '@home/components/widget/action/widget-action-dialog.component';
import { CustomActionPrettyResourcesTabsComponent } from '@home/components/widget/action/custom-action-pretty-resources-tabs.component';
import { CustomActionPrettyEditorComponent } from '@home/components/widget/action/custom-action-pretty-editor.component';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { CustomDialogContainerComponent } from '@home/components/widget/dialog/custom-dialog-container.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ImportDialogComponent } from '@home/components/import-export/import-dialog.component';
import { AddWidgetToDashboardDialogComponent } from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
import { ImportDialogCsvComponent } from '@home/components/import-export/import-dialog-csv.component';
import { TableColumnsAssignmentComponent } from '@home/components/import-export/table-columns-assignment.component';
import { EventContentDialogComponent } from '@home/components/event/event-content-dialog.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { SelectTargetLayoutDialogComponent } from '@home/components/dashboard/select-target-layout-dialog.component';
import { SelectTargetStateDialogComponent } from '@home/components/dashboard/select-target-state-dialog.component';
import { AliasesEntityAutocompleteComponent } from '@home/components/alias/aliases-entity-autocomplete.component';
import { BooleanFilterPredicateComponent } from '@home/components/filter/boolean-filter-predicate.component';
import { StringFilterPredicateComponent } from '@home/components/filter/string-filter-predicate.component';
import { NumericFilterPredicateComponent } from '@home/components/filter/numeric-filter-predicate.component';
import { ComplexFilterPredicateComponent } from '@home/components/filter/complex-filter-predicate.component';
import { FilterPredicateComponent } from '@home/components/filter/filter-predicate.component';
import { FilterPredicateListComponent } from '@home/components/filter/filter-predicate-list.component';
import { KeyFilterListComponent } from '@home/components/filter/key-filter-list.component';
import { ComplexFilterPredicateDialogComponent } from '@home/components/filter/complex-filter-predicate-dialog.component';
import { KeyFilterDialogComponent } from '@home/components/filter/key-filter-dialog.component';
import { FiltersDialogComponent } from '@home/components/filter/filters-dialog.component';
import { FilterDialogComponent } from '@home/components/filter/filter-dialog.component';
import { FilterSelectComponent } from '@home/components/filter/filter-select.component';
import { FiltersEditComponent } from '@home/components/filter/filters-edit.component';
import { FiltersEditPanelComponent } from '@home/components/filter/filters-edit-panel.component';
import { UserFilterDialogComponent } from '@home/components/filter/user-filter-dialog.component';
import { FilterUserInfoComponent } from '@home/components/filter/filter-user-info.component';
import { FilterUserInfoDialogComponent } from '@home/components/filter/filter-user-info-dialog.component';
import { FilterPredicateValueComponent } from '@home/components/filter/filter-predicate-value.component';
import { TenantProfileAutocompleteComponent } from '@home/components/profile/tenant-profile-autocomplete.component';
import { TenantProfileComponent } from '@home/components/profile/tenant-profile.component';
import { TenantProfileDialogComponent } from '@home/components/profile/tenant-profile-dialog.component';
import { TenantProfileDataComponent } from '@home/components/profile/tenant-profile-data.component';
import { DefaultDeviceProfileConfigurationComponent } from '@home/components/profile/device/default-device-profile-configuration.component';
import { DeviceProfileConfigurationComponent } from '@home/components/profile/device/device-profile-configuration.component';
import { DeviceProfileComponent } from '@home/components/profile/device-profile.component';
import { DefaultDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/default-device-profile-transport-configuration.component';
import { DeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/device-profile-transport-configuration.component';
import { DeviceProfileDialogComponent } from '@home/components/profile/device-profile-dialog.component';
import { DeviceProfileAutocompleteComponent } from '@home/components/profile/device-profile-autocomplete.component';
import { MqttDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/mqtt-device-profile-transport-configuration.component';
import { Lwm2mDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/lwm2m-device-profile-transport-configuration.component';
import { DeviceProfileAlarmsComponent } from '@home/components/profile/alarm/device-profile-alarms.component';
import { DeviceProfileAlarmComponent } from '@home/components/profile/alarm/device-profile-alarm.component';
import { CreateAlarmRulesComponent } from '@home/components/profile/alarm/create-alarm-rules.component';
import { AlarmRuleComponent } from '@home/components/profile/alarm/alarm-rule.component';
import { AlarmRuleConditionComponent } from '@home/components/profile/alarm/alarm-rule-condition.component';
import { FilterTextComponent } from '@home/components/filter/filter-text.component';
import { AddDeviceProfileDialogComponent } from '@home/components/profile/add-device-profile-dialog.component';
import { RuleChainAutocompleteComponent } from '@home/components/rule-chain/rule-chain-autocomplete.component';
import { DeviceProfileProvisionConfigurationComponent } from '@home/components/profile/device-profile-provision-configuration.component';
import { AlarmScheduleComponent } from '@home/components/profile/alarm/alarm-schedule.component';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { DeviceCredentialsComponent } from '@home/components/device/device-credentials.component';
import { AlarmScheduleInfoComponent } from '@home/components/profile/alarm/alarm-schedule-info.component';
import { AlarmScheduleDialogComponent } from '@home/components/profile/alarm/alarm-schedule-dialog.component';
import { EditAlarmDetailsDialogComponent } from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import { AlarmRuleConditionDialogComponent } from '@home/components/profile/alarm/alarm-rule-condition-dialog.component';
import { DefaultTenantProfileConfigurationComponent } from '@home/components/profile/tenant/default-tenant-profile-configuration.component';
import { TenantProfileConfigurationComponent } from '@home/components/profile/tenant/tenant-profile-configuration.component';
import { SmsProviderConfigurationComponent } from '@home/components/sms/sms-provider-configuration.component';
import { AwsSnsProviderConfigurationComponent } from '@home/components/sms/aws-sns-provider-configuration.component';
import { TwilioSmsProviderConfigurationComponent } from '@home/components/sms/twilio-sms-provider-configuration.component';
import { CopyDeviceCredentialsComponent } from '@home/components/device/copy-device-credentials.component';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { DashboardToolbarComponent } from '@home/components/dashboard-page/dashboard-toolbar.component';
import { StatesControllerModule } from '@home/components/dashboard-page/states/states-controller.module';
import { DashboardLayoutComponent } from '@home/components/dashboard-page/layout/dashboard-layout.component';
import { EditWidgetComponent } from '@home/components/dashboard-page/edit-widget.component';
import { DashboardWidgetSelectComponent } from '@home/components/dashboard-page/dashboard-widget-select.component';
import { AddWidgetDialogComponent } from '@home/components/dashboard-page/add-widget-dialog.component';
import { ManageDashboardLayoutsDialogComponent } from '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component';
import { DashboardSettingsDialogComponent } from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import { ManageDashboardStatesDialogComponent } from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import { DashboardStateDialogComponent } from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import { EmbedDashboardDialogComponent } from '@home/components/widget/dialog/embed-dashboard-dialog.component';
import { EMBED_DASHBOARD_DIALOG_TOKEN } from '@home/components/widget/dialog/embed-dashboard-dialog-token';

@NgModule({
  declarations:
    [
      EntitiesTableComponent,
      AddEntityDialogComponent,
      DetailsPanelComponent,
      EntityDetailsPanelComponent,
      AuditLogTableComponent,
      AuditLogDetailsDialogComponent,
      EventContentDialogComponent,
      EventTableHeaderComponent,
      EventTableComponent,
      RelationTableComponent,
      RelationDialogComponent,
      RelationFiltersComponent,
      AlarmTableHeaderComponent,
      AlarmTableComponent,
      AttributeTableComponent,
      AddAttributeDialogComponent,
      EditAttributeValuePanelComponent,
      AliasesEntitySelectPanelComponent,
      AliasesEntitySelectComponent,
      AliasesEntityAutocompleteComponent,
      EntityAliasesDialogComponent,
      EntityAliasDialogComponent,
      DashboardComponent,
      WidgetComponent,
      LegendComponent,
      WidgetConfigComponent,
      EntityFilterViewComponent,
      EntityFilterComponent,
      EntityAliasSelectComponent,
      DataKeysComponent,
      DataKeyConfigComponent,
      DataKeyConfigDialogComponent,
      LegendConfigPanelComponent,
      LegendConfigComponent,
      ManageWidgetActionsComponent,
      WidgetActionDialogComponent,
      CustomActionPrettyResourcesTabsComponent,
      CustomActionPrettyEditorComponent,
      CustomDialogContainerComponent,
      ImportDialogComponent,
      ImportDialogCsvComponent,
      SelectTargetLayoutDialogComponent,
      SelectTargetStateDialogComponent,
      AddWidgetToDashboardDialogComponent,
      TableColumnsAssignmentComponent,
      BooleanFilterPredicateComponent,
      StringFilterPredicateComponent,
      NumericFilterPredicateComponent,
      ComplexFilterPredicateComponent,
      ComplexFilterPredicateDialogComponent,
      FilterPredicateComponent,
      FilterPredicateListComponent,
      KeyFilterListComponent,
      KeyFilterDialogComponent,
      FilterDialogComponent,
      FiltersDialogComponent,
      FilterSelectComponent,
      FilterTextComponent,
      FiltersEditComponent,
      FiltersEditPanelComponent,
      UserFilterDialogComponent,
      FilterUserInfoComponent,
      FilterUserInfoDialogComponent,
      FilterPredicateValueComponent,
      TenantProfileAutocompleteComponent,
      DefaultTenantProfileConfigurationComponent,
      TenantProfileConfigurationComponent,
      TenantProfileDataComponent,
      TenantProfileComponent,
      TenantProfileDialogComponent,
      DeviceProfileAutocompleteComponent,
      DefaultDeviceProfileConfigurationComponent,
      DeviceProfileConfigurationComponent,
      DefaultDeviceProfileTransportConfigurationComponent,
      MqttDeviceProfileTransportConfigurationComponent,
      Lwm2mDeviceProfileTransportConfigurationComponent,
      DeviceProfileTransportConfigurationComponent,
      CreateAlarmRulesComponent,
      AlarmRuleComponent,
      AlarmRuleConditionDialogComponent,
      AlarmRuleConditionComponent,
      DeviceProfileAlarmComponent,
      DeviceProfileAlarmsComponent,
      DeviceProfileComponent,
      DeviceProfileDialogComponent,
      AddDeviceProfileDialogComponent,
      RuleChainAutocompleteComponent,
      AlarmScheduleInfoComponent,
      DeviceProfileProvisionConfigurationComponent,
      AlarmScheduleComponent,
      DeviceWizardDialogComponent,
      DeviceCredentialsComponent,
      CopyDeviceCredentialsComponent,
      AlarmScheduleDialogComponent,
      EditAlarmDetailsDialogComponent,
      SmsProviderConfigurationComponent,
      AwsSnsProviderConfigurationComponent,
      TwilioSmsProviderConfigurationComponent,
      DashboardToolbarComponent,
      DashboardPageComponent,
      DashboardLayoutComponent,
      EditWidgetComponent,
      DashboardWidgetSelectComponent,
      AddWidgetDialogComponent,
      ManageDashboardLayoutsDialogComponent,
      DashboardSettingsDialogComponent,
      ManageDashboardStatesDialogComponent,
      DashboardStateDialogComponent,
      EmbedDashboardDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    StatesControllerModule
  ],
  exports: [
    EntitiesTableComponent,
    AddEntityDialogComponent,
    DetailsPanelComponent,
    EntityDetailsPanelComponent,
    AuditLogTableComponent,
    EventTableComponent,
    RelationTableComponent,
    RelationFiltersComponent,
    AlarmTableComponent,
    AttributeTableComponent,
    AliasesEntitySelectComponent,
    AliasesEntityAutocompleteComponent,
    EntityAliasesDialogComponent,
    EntityAliasDialogComponent,
    DashboardComponent,
    WidgetComponent,
    LegendComponent,
    WidgetConfigComponent,
    EntityFilterViewComponent,
    EntityFilterComponent,
    EntityAliasSelectComponent,
    DataKeysComponent,
    DataKeyConfigComponent,
    DataKeyConfigDialogComponent,
    LegendConfigComponent,
    ManageWidgetActionsComponent,
    WidgetActionDialogComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    CustomDialogContainerComponent,
    ImportDialogComponent,
    ImportDialogCsvComponent,
    TableColumnsAssignmentComponent,
    SelectTargetLayoutDialogComponent,
    SelectTargetStateDialogComponent,
    BooleanFilterPredicateComponent,
    StringFilterPredicateComponent,
    NumericFilterPredicateComponent,
    ComplexFilterPredicateComponent,
    ComplexFilterPredicateDialogComponent,
    FilterPredicateComponent,
    FilterPredicateListComponent,
    KeyFilterListComponent,
    KeyFilterDialogComponent,
    FilterDialogComponent,
    FiltersDialogComponent,
    FilterSelectComponent,
    FilterTextComponent,
    FiltersEditComponent,
    UserFilterDialogComponent,
    TenantProfileAutocompleteComponent,
    TenantProfileDataComponent,
    TenantProfileComponent,
    TenantProfileDialogComponent,
    DeviceProfileAutocompleteComponent,
    DefaultDeviceProfileConfigurationComponent,
    DeviceProfileConfigurationComponent,
    DefaultDeviceProfileTransportConfigurationComponent,
    MqttDeviceProfileTransportConfigurationComponent,
    Lwm2mDeviceProfileTransportConfigurationComponent,
    DeviceProfileTransportConfigurationComponent,
    CreateAlarmRulesComponent,
    AlarmRuleComponent,
    AlarmRuleConditionDialogComponent,
    AlarmRuleConditionComponent,
    DeviceProfileAlarmComponent,
    DeviceProfileAlarmsComponent,
    DeviceProfileComponent,
    DeviceProfileDialogComponent,
    AddDeviceProfileDialogComponent,
    RuleChainAutocompleteComponent,
    DeviceWizardDialogComponent,
    DeviceCredentialsComponent,
    CopyDeviceCredentialsComponent,
    AlarmScheduleInfoComponent,
    AlarmScheduleComponent,
    AlarmScheduleDialogComponent,
    EditAlarmDetailsDialogComponent,
    DeviceProfileProvisionConfigurationComponent,
    SmsProviderConfigurationComponent,
    AwsSnsProviderConfigurationComponent,
    TwilioSmsProviderConfigurationComponent,
    DashboardToolbarComponent,
    DashboardPageComponent,
    DashboardLayoutComponent,
    EditWidgetComponent,
    DashboardWidgetSelectComponent,
    AddWidgetDialogComponent,
    ManageDashboardLayoutsDialogComponent,
    DashboardSettingsDialogComponent,
    ManageDashboardStatesDialogComponent,
    DashboardStateDialogComponent,
    EmbedDashboardDialogComponent
  ],
  providers: [
    WidgetComponentService,
    CustomDialogService,
    ImportExportService,
    {provide: EMBED_DASHBOARD_DIALOG_TOKEN, useValue: EmbedDashboardDialogComponent}
  ]
})
export class HomeComponentsModule { }
