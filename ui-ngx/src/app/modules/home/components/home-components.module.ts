// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DetailsPanelComponent } from '@home/components/details-panel.component';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { AuditLogDetailsDialogComponent } from '@home/components/audit-log/audit-log-details-dialog.component';
import { AuditLogTableComponent } from '@home/components/audit-log/audit-log-table.component';
import { AuditLogHeaderComponent } from '@home/components/audit-log/audit-log-header.component';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { EventFilterPanelComponent } from '@home/components/event/event-filter-panel.component';
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
import { AliasesEntitySelectPanelComponent } from '@home/components/alias/aliases-entity-select-panel.component';
import { AliasesEntitySelectComponent } from '@home/components/alias/aliases-entity-select.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { EntityAliasesDialogComponent } from '@home/components/alias/entity-aliases-dialog.component';
import { EntityFilterViewComponent } from '@home/components/entity/entity-filter-view.component';
import { EntityAliasDialogComponent } from '@home/components/alias/entity-alias-dialog.component';
import { EntityFilterComponent } from '@home/components/entity/entity-filter.component';
import { RelationFiltersComponent } from '@home/components/relation/relation-filters.component';
import { ManageWidgetActionsComponent } from '@home/components/widget/action/manage-widget-actions.component';
import { WidgetActionDialogComponent } from '@home/components/widget/action/widget-action-dialog.component';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { CustomDialogContainerComponent } from '@home/components/widget/dialog/custom-dialog-container.component';
import {
  AddWidgetToDashboardDialogComponent
} from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
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
import {
  ComplexFilterPredicateDialogComponent
} from '@home/components/filter/complex-filter-predicate-dialog.component';
import { KeyFilterDialogComponent } from '@home/components/filter/key-filter-dialog.component';
import { FiltersDialogComponent } from '@home/components/filter/filters-dialog.component';
import { FilterDialogComponent } from '@home/components/filter/filter-dialog.component';
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
import {
  DefaultDeviceProfileConfigurationComponent
} from '@home/components/profile/device/default-device-profile-configuration.component';
import {
  DeviceProfileConfigurationComponent
} from '@home/components/profile/device/device-profile-configuration.component';
import { DeviceProfileComponent } from '@home/components/profile/device-profile.component';
import {
  DefaultDeviceProfileTransportConfigurationComponent
} from '@home/components/profile/device/default-device-profile-transport-configuration.component';
import {
  DeviceProfileTransportConfigurationComponent
} from '@home/components/profile/device/device-profile-transport-configuration.component';
import { DeviceProfileDialogComponent } from '@home/components/profile/device-profile-dialog.component';
import { DeviceProfileAutocompleteComponent } from '@home/components/profile/device-profile-autocomplete.component';
import {
  MqttDeviceProfileTransportConfigurationComponent
} from '@home/components/profile/device/mqtt-device-profile-transport-configuration.component';
import {
  CoapDeviceProfileTransportConfigurationComponent
} from '@home/components/profile/device/coap-device-profile-transport-configuration.component';
import { DeviceProfileAlarmsComponent } from '@home/components/profile/alarm/device-profile-alarms.component';
import { DeviceProfileAlarmComponent } from '@home/components/profile/alarm/device-profile-alarm.component';
import { CreateAlarmRulesComponent } from '@home/components/profile/alarm/create-alarm-rules.component';
import { AlarmRuleComponent } from '@home/components/profile/alarm/alarm-rule.component';
import { AlarmRuleConditionComponent } from '@home/components/profile/alarm/alarm-rule-condition.component';
import { FilterTextComponent } from '@home/components/filter/filter-text.component';
import { AddDeviceProfileDialogComponent } from '@home/components/profile/add-device-profile-dialog.component';
import { RuleChainAutocompleteComponent } from '@home/components/rule-chain/rule-chain-autocomplete.component';
import {
  DeviceProfileProvisionConfigurationComponent
} from '@home/components/profile/device-profile-provision-configuration.component';
import { AlarmScheduleComponent } from '@home/components/profile/alarm/alarm-schedule.component';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AlarmScheduleInfoComponent } from '@home/components/profile/alarm/alarm-schedule-info.component';
import { AlarmScheduleDialogComponent } from '@home/components/profile/alarm/alarm-schedule-dialog.component';
import { EditAlarmDetailsDialogComponent } from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import {
  AlarmRuleConditionDialogComponent
} from '@home/components/profile/alarm/alarm-rule-condition-dialog.component';
import {
  DefaultTenantProfileConfigurationComponent
} from '@home/components/profile/tenant/default-tenant-profile-configuration.component';
import {
  TenantProfileConfigurationComponent
} from '@home/components/profile/tenant/tenant-profile-configuration.component';
import { SmsProviderConfigurationComponent } from '@home/components/sms/sms-provider-configuration.component';
import { AwsSnsProviderConfigurationComponent } from '@home/components/sms/aws-sns-provider-configuration.component';
import { SmppSmsProviderConfigurationComponent } from '@home/components/sms/smpp-sms-provider-configuration.component';
import {
  TwilioSmsProviderConfigurationComponent
} from '@home/components/sms/twilio-sms-provider-configuration.component';
import { Lwm2mProfileComponentsModule } from '@home/components/profile/device/lwm2m/lwm2m-profile-components.module';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { DashboardToolbarComponent } from '@home/components/dashboard-page/dashboard-toolbar.component';
import { StatesControllerModule } from '@home/components/dashboard-page/states/states-controller.module';
import { DashboardLayoutComponent } from '@home/components/dashboard-page/layout/dashboard-layout.component';
import { EditWidgetComponent } from '@home/components/dashboard-page/edit-widget.component';
import { DashboardWidgetSelectComponent } from '@home/components/dashboard-page/dashboard-widget-select.component';
import { IotHubComponentsModule } from '@home/components/iot-hub/iot-hub-components.module';
import { AddWidgetDialogComponent } from '@home/components/dashboard-page/add-widget-dialog.component';
import {
  ManageDashboardLayoutsDialogComponent
} from '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component';
import {
  AddNewBreakpointDialogComponent
} from '@home/components/dashboard-page/layout/add-new-breakpoint-dialog.component';
import { DashboardSettingsDialogComponent } from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import {
  ManageDashboardStatesDialogComponent
} from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import { DashboardStateDialogComponent } from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import { EmbedDashboardDialogComponent } from '@home/components/widget/dialog/embed-dashboard-dialog.component';
import { EMBED_DASHBOARD_DIALOG_TOKEN } from '@home/components/widget/dialog/embed-dashboard-dialog-token';
import { EdgeDownlinkTableComponent } from '@home/components/edge/edge-downlink-table.component';
import { EdgeDownlinkTableHeaderComponent } from '@home/components/edge/edge-downlink-table-header.component';
import { EntityGroupWizardDialogComponent } from '@home/components/wizard/entity-group-wizard-dialog.component';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { ConverterAutocompleteComponent } from '@home/components/converter/converter-autocomplete.component';
import { ConverterDialogComponent } from '@home/components/converter/converter-dialog.component';
import { OperationTypeListComponent } from '@home/components/role/operation-type-list.component';
import { ResourceTypeAutocompleteComponent } from '@home/components/role/resource-type-autocomplete.component';
import { PermissionListComponent } from '@home/components/role/permission-list.component';
import { ViewRoleDialogComponent } from '@home/components/role/view-role-dialog.component';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { GroupEntityTableHeaderComponent } from '@home/components/group/group-entity-table-header.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';
import { EntityGroupSettingsComponent } from '@home/components/group/entity-group-settings.component';
import { EntityGroupColumnsComponent } from '@home/components/group/entity-group-columns.component';
import { EntityGroupColumnDialogComponent } from '@home/components/group/entity-group-column-dialog.component';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { RegistrationPermissionsComponent } from '@home/components/role/registration-permissions.component';
import { UserGroupPanelComponent } from '@home/components/role/user-group-panel.component';
import { UserGroupsPanelRowComponent } from '@home/components/role/user-groups-panel-row.component';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { EntityGroupColumnComponent } from '@home/components/group/entity-group-column.component';
import {
  AlarmDurationPredicateValueComponent
} from '@home/components/profile/alarm/alarm-duration-predicate-value.component';
import { OtaUpdateEventConfigComponent } from '@home/components/scheduler/config/ota-update-event-config.component';
import { TargetSelectComponent } from '@home/components/scheduler/config/target-select.component';
import { DashboardImageDialogComponent } from '@home/components/dashboard-page/dashboard-image-dialog.component';
import {
  EditWidgetActionsTooltipComponent,
  WidgetContainerComponent
} from '@home/components/widget/widget-container.component';
import {
  SnmpDeviceProfileTransportModule
} from '@home/components/profile/device/snmp/snmp-device-profile-transport.module';
import { DeviceCredentialsModule } from '@home/components/device/device-credentials.module';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';
import {
  COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN,
  DASHBOARD_PAGE_COMPONENT_TOKEN,
  HOME_COMPONENTS_MODULE_TOKEN
} from '@home/components/tokens';
import { DashboardStateComponent } from '@home/components/dashboard-page/dashboard-state.component';
import { AlarmDynamicValue } from '@home/components/profile/alarm/alarm-dynamic-value.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { TenantProfileQueuesComponent } from '@home/components/profile/queue/tenant-profile-queues.component';
import { QueueFormComponent } from '@home/components/queue/queue-form.component';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { EntityVersionsTableComponent } from '@home/components/vc/entity-versions-table.component';
import { EntityVersionCreateComponent } from '@home/components/vc/entity-version-create.component';
import { EntityVersionRestoreComponent } from '@home/components/vc/entity-version-restore.component';
import { EntityVersionDiffComponent } from '@home/components/vc/entity-version-diff.component';
import { ComplexVersionCreateComponent } from '@home/components/vc/complex-version-create.component';
import { EntityTypesVersionCreateComponent } from '@home/components/vc/entity-types-version-create.component';
import { EntityTypesVersionLoadComponent } from '@home/components/vc/entity-types-version-load.component';
import { ComplexVersionLoadComponent } from '@home/components/vc/complex-version-load.component';
import { RemoveOtherEntitiesConfirmComponent } from '@home/components/vc/remove-other-entities-confirm.component';
import { AutoCommitSettingsComponent } from '@home/components/vc/auto-commit-settings.component';
import { OwnerEntityGroupListComponent } from '@home/components/vc/owner-entity-group-list.component';
import { RateLimitsComponent } from '@home/components/profile/tenant/rate-limits/rate-limits.component';
import { RateLimitsTextComponent } from '@home/components/profile/tenant/rate-limits/rate-limits-text.component';
import { RateLimitsListComponent } from '@home/components/profile/tenant/rate-limits/rate-limits-list.component';
import {
  RateLimitsDetailsDialogComponent
} from '@home/components/profile/tenant/rate-limits/rate-limits-details-dialog.component';
import { AssetProfileComponent } from '@home/components/profile/asset-profile.component';
import { AssetProfileDialogComponent } from '@home/components/profile/asset-profile-dialog.component';
import { AssetProfileAutocompleteComponent } from '@home/components/profile/asset-profile-autocomplete.component';
import { IntegrationWizardDialogComponent } from '@home/components/wizard/integration-wizard-dialog.component';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { ConverterTestDialogComponent } from '@home/components/converter/converter-test-dialog.component';
import { UpdatedPayloadPanelComponent } from '@home/components/converter/updated-payload-panel.component';
import { IntegrationComponentModule } from '@home/components/integration/integration-component.module';
import { MODULES_MAP } from '@shared/models/constants';
import { modulesMap } from '@modules/common/modules-map';
import { AlarmAssigneePanelComponent } from '@home/components/alarm/alarm-assignee-panel.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { SendNotificationButtonComponent } from '@home/components/notification/send-notification-button.component';
import { AiAssistantAlarmButtonComponent } from '@home/components/alarm/ai-assistant-alarm-button.component';
import { GroupEntityInfoComponent } from '@home/components/group/group-entity-info.component';
import { ManageOwnerAndGroupsDialogComponent } from '@home/components/group/manage-owner-and-groups-dialog.component';
import { OwnerAndGroupsComponent } from '@home/components/group/owner-and-groups.component';
import { AlarmAssigneeSelectPanelComponent } from '@home/components/alarm/alarm-assignee-select-panel.component';
import { DeviceInfoFilterComponent } from '@home/components/device/device-info-filter.component';
import { WidgetPreviewComponent } from '@home/components/widget/widget-preview.component';
import {
  ManageWidgetActionsDialogComponent
} from '@home/components/widget/action/manage-widget-actions-dialog.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import { BasicWidgetConfigModule } from '@home/components/widget/config/basic/basic-widget-config.module';
import { DeleteTimeseriesPanelComponent } from '@home/components/attribute/delete-timeseries-panel.component';
import { MoveWidgetsDialogComponent } from '@home/components/dashboard-page/layout/move-widgets-dialog.component';
import {
  SelectDashboardBreakpointComponent
} from '@home/components/dashboard-page/layout/select-dashboard-breakpoint.component';
import { EntityChipsComponent } from '@home/components/entity/entity-chips.component';
import { DashboardViewComponent } from '@home/components/dashboard-view/dashboard-view.component';
import { ConverterLibraryComponent } from '@home/components/converter/converter-library.component';
import {
  EntityDebugSettingsButtonComponent
} from '@home/components/entity/debug/entity-debug-settings-button.component';
import { CancelTaskDialogComponent } from '@home/components/task/cancel-task-dialog.component';
import { CheckConnectivityDialogComponent } from '@home/components/ai-model/check-connectivity-dialog.component';
import { AIModelDialogComponent } from '@home/components/ai-model/ai-model-dialog.component';
import { ResourcesDialogComponent } from "@home/components/resources/resources-dialog.component";
import { ResourcesLibraryComponent } from "@home/components/resources/resources-library.component";
import { CalculatedFieldsTableComponent } from '@home/components/calculated-fields/calculated-fields-table.component';
import { CalculatedFieldsModule } from '@home/components/calculated-fields/calculated-field.module';
import { AlarmRuleModule } from "@home/components/alarm-rules/alarm-rule.module";
import { AlarmRulesTableComponent } from "@home/components/alarm-rules/alarm-rules-table.component";
import { ApiKeysTableComponent } from '@home/components/api-key/api-keys-table.component';
import { AddApiKeyDialogComponent } from '@home/components/api-key/add-api-key-dialog.component';
import { EditApiKeyDescriptionPanelComponent } from '@home/components/api-key/edit-api-key-description-panel.component';
import { ApiKeyGeneratedDialogComponent } from '@home/components/api-key/api-key-generated-dialog.component';
import { ApiKeysTableDialogComponent } from '@home/components/api-key/api-keys-table-dialog.component';
import { AuditLogFilterComponent } from "@home/components/audit-log/audit-log-filter.component";
import { EventsDialogComponent } from '@home/dialogs/events-dialog.component';
import { NotificationBellModule } from '@home/components/notification/notification-bell.module';
import { GithubBadgeModule } from '@home/components/github-badge/github-badge.module';
import {
  ConverterLibraryVendorAutocompleteComponent
} from '@home/components/converter/converter-library-vendor-autocomplete.component';
import {
  ConverterLibraryModelAutocompleteComponent
} from '@home/components/converter/converter-library-model-autocomplete.component';
import { AiModule } from '@home/components/ai/ai.module';
import { RequestPePackComponent } from '@home/components/pe-pack/request-pe-pack.component';

@NgModule({
  declarations:
    [
      RouterTabsComponent,
      EntitiesTableComponent,
      AddEntityDialogComponent,
      DetailsPanelComponent,
      EntityDetailsPanelComponent,
      EntityDetailsPageComponent,
      AuditLogTableComponent,
      AuditLogDetailsDialogComponent,
      CalculatedFieldsTableComponent,
      AlarmRulesTableComponent,
      EventContentDialogComponent,
      EventTableHeaderComponent,
      EventTableComponent,
      EventFilterPanelComponent,
      EdgeDownlinkTableHeaderComponent,
      EdgeDownlinkTableComponent,
      RelationTableComponent,
      RelationDialogComponent,
      RelationFiltersComponent,
      AlarmTableHeaderComponent,
      AlarmTableComponent,
      AlarmAssigneePanelComponent,
      AlarmAssigneeSelectPanelComponent,
      AttributeTableComponent,
      AddAttributeDialogComponent,
      EditAttributeValuePanelComponent,
      DeleteTimeseriesPanelComponent,
      AliasesEntitySelectPanelComponent,
      AliasesEntitySelectComponent,
      AliasesEntityAutocompleteComponent,
      EntityAliasesDialogComponent,
      EntityAliasDialogComponent,
      DashboardComponent,
      WidgetContainerComponent,
      EditWidgetActionsTooltipComponent,
      WidgetComponent,
      WidgetConfigComponent,
      WidgetPreviewComponent,
      EntityFilterViewComponent,
      EntityFilterComponent,
      ManageWidgetActionsComponent,
      WidgetActionDialogComponent,
      ManageWidgetActionsDialogComponent,
      CustomDialogContainerComponent,
      SelectTargetLayoutDialogComponent,
      SelectTargetStateDialogComponent,
      AddWidgetToDashboardDialogComponent,
      ConverterAutocompleteComponent,
      ConverterDialogComponent,
      OperationTypeListComponent,
      ResourceTypeAutocompleteComponent,
      PermissionListComponent,
      ViewRoleDialogComponent,
      GroupEntitiesTableComponent,
      GroupEntityTabsComponent,
      GroupEntityTableHeaderComponent,
      EntityGroupComponent,
      EntityGroupTabsComponent,
      EntityGroupSettingsComponent,
      EntityGroupColumnComponent,
      EntityGroupColumnsComponent,
      EntityGroupColumnDialogComponent,
      AddGroupEntityDialogComponent,
      GroupEntityInfoComponent,
      OwnerAndGroupsComponent,
      ManageOwnerAndGroupsDialogComponent,
      RegistrationPermissionsComponent,
      UserGroupPanelComponent,
      UserGroupsPanelRowComponent,
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
      CoapDeviceProfileTransportConfigurationComponent,
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
      DeviceInfoFilterComponent,
      AssetProfileComponent,
      AssetProfileDialogComponent,
      AssetProfileAutocompleteComponent,
      RuleChainAutocompleteComponent,
      AlarmScheduleInfoComponent,
      DeviceProfileProvisionConfigurationComponent,
      AlarmScheduleComponent,
      AlarmDynamicValue,
      AlarmDurationPredicateValueComponent,
      DeviceWizardDialogComponent,
      AlarmScheduleDialogComponent,
      EditAlarmDetailsDialogComponent,
      SmsProviderConfigurationComponent,
      AwsSnsProviderConfigurationComponent,
      SmppSmsProviderConfigurationComponent,
      TwilioSmsProviderConfigurationComponent,
      EntityGroupWizardDialogComponent,
      DashboardToolbarComponent,
      DashboardPageComponent,
      DashboardStateComponent,
      DashboardLayoutComponent,
      SelectDashboardBreakpointComponent,
      EditWidgetComponent,
      DashboardWidgetSelectComponent,
      AddWidgetDialogComponent,
      MoveWidgetsDialogComponent,
      ManageDashboardLayoutsDialogComponent,
      AddNewBreakpointDialogComponent,
      DashboardSettingsDialogComponent,
      ManageDashboardStatesDialogComponent,
      DashboardStateDialogComponent,
      DashboardImageDialogComponent,
      EmbedDashboardDialogComponent,
      OtaUpdateEventConfigComponent,
      TargetSelectComponent,
      TenantProfileQueuesComponent,
      QueueFormComponent,
      RepositorySettingsComponent,
      VersionControlComponent,
      EntityVersionsTableComponent,
      EntityVersionCreateComponent,
      EntityVersionRestoreComponent,
      EntityVersionDiffComponent,
      ComplexVersionCreateComponent,
      EntityTypesVersionCreateComponent,
      EntityTypesVersionLoadComponent,
      ComplexVersionLoadComponent,
      RemoveOtherEntitiesConfirmComponent,
      AutoCommitSettingsComponent,
      OwnerEntityGroupListComponent,
      RateLimitsDetailsDialogComponent,
      RateLimitsComponent,
      RateLimitsListComponent,
      RateLimitsTextComponent,
      IntegrationWizardDialogComponent,
      ConverterComponent,
      ConverterTestDialogComponent,
      UpdatedPayloadPanelComponent,
      SendNotificationButtonComponent,
      AiAssistantAlarmButtonComponent,
      EntityChipsComponent,
      DashboardViewComponent,
      ConverterLibraryComponent,
      CancelTaskDialogComponent,
      CheckConnectivityDialogComponent,
      AIModelDialogComponent,
      ResourcesDialogComponent,
      ResourcesLibraryComponent,
      ApiKeysTableComponent,
      ApiKeysTableDialogComponent,
      AddApiKeyDialogComponent,
      EditApiKeyDescriptionPanelComponent,
      ApiKeyGeneratedDialogComponent,
      AuditLogHeaderComponent,
      AuditLogFilterComponent,
      EventsDialogComponent,
      ConverterLibraryVendorAutocompleteComponent,
      ConverterLibraryModelAutocompleteComponent,
      RequestPePackComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    HomeDialogsModule,
    CalculatedFieldsModule,
    AlarmRuleModule,
    IotHubComponentsModule,
    WidgetConfigComponentsModule,
    BasicWidgetConfigModule,
    Lwm2mProfileComponentsModule,
    SnmpDeviceProfileTransportModule,
    StatesControllerModule,
    DeviceCredentialsModule,
    DeviceProfileCommonModule,
    IntegrationComponentModule,
    EntityDebugSettingsButtonComponent,
    NotificationBellModule,
    GithubBadgeModule,
    AiModule,
  ],
  exports: [
    SharedHomeComponentsModule,
    RouterTabsComponent,
    EntitiesTableComponent,
    AddEntityDialogComponent,
    DetailsPanelComponent,
    EntityDetailsPanelComponent,
    EntityDetailsPageComponent,
    AuditLogTableComponent,
    CalculatedFieldsTableComponent,
    AlarmRulesTableComponent,
    EventTableComponent,
    EdgeDownlinkTableHeaderComponent,
    EdgeDownlinkTableComponent,
    RelationTableComponent,
    RelationFiltersComponent,
    AlarmTableComponent,
    AlarmAssigneePanelComponent,
    AlarmAssigneeSelectPanelComponent,
    AttributeTableComponent,
    AliasesEntitySelectComponent,
    AliasesEntityAutocompleteComponent,
    EntityAliasesDialogComponent,
    EntityAliasDialogComponent,
    DashboardComponent,
    WidgetContainerComponent,
    EditWidgetActionsTooltipComponent,
    WidgetComponent,
    WidgetConfigComponent,
    WidgetPreviewComponent,
    EntityFilterViewComponent,
    EntityFilterComponent,
    ManageWidgetActionsComponent,
    WidgetActionDialogComponent,
    ManageWidgetActionsDialogComponent,
    CustomDialogContainerComponent,
    SelectTargetLayoutDialogComponent,
    SelectTargetStateDialogComponent,
    ConverterAutocompleteComponent,
    OperationTypeListComponent,
    ResourceTypeAutocompleteComponent,
    PermissionListComponent,
    ViewRoleDialogComponent,
    GroupEntitiesTableComponent,
    GroupEntityTabsComponent,
    GroupEntityTableHeaderComponent,
    EntityGroupComponent,
    EntityGroupTabsComponent,
    EntityGroupSettingsComponent,
    EntityGroupColumnComponent,
    EntityGroupColumnsComponent,
    EntityGroupColumnDialogComponent,
    AddGroupEntityDialogComponent,
    GroupEntityInfoComponent,
    OwnerAndGroupsComponent,
    ManageOwnerAndGroupsDialogComponent,
    RegistrationPermissionsComponent,
    UserGroupPanelComponent,
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
    CoapDeviceProfileTransportConfigurationComponent,
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
    DeviceInfoFilterComponent,
    RuleChainAutocompleteComponent,
    DeviceWizardDialogComponent,
    AssetProfileComponent,
    AssetProfileDialogComponent,
    AssetProfileAutocompleteComponent,
    AlarmScheduleInfoComponent,
    AlarmScheduleComponent,
    AlarmDynamicValue,
    AlarmScheduleDialogComponent,
    AlarmDurationPredicateValueComponent,
    EditAlarmDetailsDialogComponent,
    DeviceProfileProvisionConfigurationComponent,
    SmsProviderConfigurationComponent,
    AwsSnsProviderConfigurationComponent,
    SmppSmsProviderConfigurationComponent,
    TwilioSmsProviderConfigurationComponent,
    EntityGroupWizardDialogComponent,
    DashboardToolbarComponent,
    DashboardPageComponent,
    DashboardStateComponent,
    DashboardLayoutComponent,
    SelectDashboardBreakpointComponent,
    EditWidgetComponent,
    DashboardWidgetSelectComponent,
    IotHubComponentsModule,
    AddWidgetDialogComponent,
    MoveWidgetsDialogComponent,
    ManageDashboardLayoutsDialogComponent,
    AddNewBreakpointDialogComponent,
    DashboardSettingsDialogComponent,
    ManageDashboardStatesDialogComponent,
    DashboardStateDialogComponent,
    DashboardImageDialogComponent,
    EmbedDashboardDialogComponent,
    OtaUpdateEventConfigComponent,
    TargetSelectComponent,
    TenantProfileQueuesComponent,
    QueueFormComponent,
    RepositorySettingsComponent,
    VersionControlComponent,
    EntityVersionsTableComponent,
    EntityVersionCreateComponent,
    EntityVersionRestoreComponent,
    EntityVersionDiffComponent,
    ComplexVersionCreateComponent,
    EntityTypesVersionCreateComponent,
    EntityTypesVersionLoadComponent,
    ComplexVersionLoadComponent,
    RemoveOtherEntitiesConfirmComponent,
    AutoCommitSettingsComponent,
    OwnerEntityGroupListComponent,
    RateLimitsDetailsDialogComponent,
    RateLimitsComponent,
    RateLimitsListComponent,
    RateLimitsTextComponent,
    IntegrationWizardDialogComponent,
    SendNotificationButtonComponent,
    AiAssistantAlarmButtonComponent,
    EntityChipsComponent,
    DashboardViewComponent,
    CancelTaskDialogComponent,
    CheckConnectivityDialogComponent,
    AIModelDialogComponent,
    ResourcesDialogComponent,
    ResourcesLibraryComponent,
    ApiKeysTableComponent,
    ApiKeysTableDialogComponent,
    EventsDialogComponent,
    ConverterLibraryVendorAutocompleteComponent,
    ConverterLibraryModelAutocompleteComponent,
    RequestPePackComponent
  ],
  providers: [
    WidgetComponentService,
    CustomDialogService,
    AllEntitiesTableConfigService,
    GroupConfigTableConfigService,
    EntityGroupsTableConfigResolver,
    EntityGroupConfigResolver,
    {provide: EMBED_DASHBOARD_DIALOG_TOKEN, useValue: EmbedDashboardDialogComponent},
    {provide: COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN, useValue: ComplexFilterPredicateDialogComponent},
    {provide: DASHBOARD_PAGE_COMPONENT_TOKEN, useValue: DashboardPageComponent},
    {provide: HOME_COMPONENTS_MODULE_TOKEN, useValue: HomeComponentsModule },
    {provide: MODULES_MAP, useValue: modulesMap}
  ]
})
export class HomeComponentsModule {
}
