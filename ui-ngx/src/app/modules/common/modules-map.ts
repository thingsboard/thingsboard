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

/* eslint-disable max-len */

import * as AngularAnimations from '@angular/animations';
import * as AngularCore from '@angular/core';
import * as AngularCommon from '@angular/common';
import * as AngularForms from '@angular/forms';
import * as AngularPlatformBrowser from '@angular/platform-browser';
import * as AngularPlatformBrowserAnimations from '@angular/platform-browser/animations';
import * as AngularRouter from '@angular/router';
import * as AngularCdkCoercion from '@angular/cdk/coercion';
import * as AngularCdkCollections from '@angular/cdk/collections';
import * as AngularCdkKeycodes from '@angular/cdk/keycodes';
import * as AngularCdkLayout from '@angular/cdk/layout';
import * as AngularCdkOverlay from '@angular/cdk/overlay';
import * as AngularCdkPortal from '@angular/cdk/portal';
import * as AngularCdkBidi from '@angular/cdk/bidi';
import * as AngularCdkPlatform from '@angular/cdk/platform';
import * as AngularMaterialAutocomplete from '@angular/material/autocomplete';
import * as AngularMaterialBadge from '@angular/material/badge';
import * as AngularMaterialBottomSheet from '@angular/material/bottom-sheet';
import * as AngularMaterialButton from '@angular/material/button';
import * as AngularMaterialButtonToggle from '@angular/material/button-toggle';
import * as AngularMaterialCard from '@angular/material/card';
import * as AngularMaterialCheckbox from '@angular/material/checkbox';
import * as AngularMaterialChips from '@angular/material/chips';
import * as AngularMaterialCore from '@angular/material/core';
import * as AngularMaterialDatepicker from '@angular/material/datepicker';
import * as AngularMaterialDialog from '@angular/material/dialog';
import * as AngularMaterialDivider from '@angular/material/divider';
import * as AngularMaterialExpansion from '@angular/material/expansion';
import * as AngularMaterialFormField from '@angular/material/form-field';
import * as AngularMaterialGridList from '@angular/material/grid-list';
import * as AngularMaterialIcon from '@angular/material/icon';
import * as AngularMaterialInput from '@angular/material/input';
import * as AngularMaterialList from '@angular/material/list';
import * as AngularMaterialMenu from '@angular/material/menu';
import * as AngularMaterialPaginator from '@angular/material/paginator';
import * as AngularMaterialProgressBar from '@angular/material/progress-bar';
import * as AngularMaterialProgressSpinner from '@angular/material/progress-spinner';
import * as AngularMaterialRadio from '@angular/material/radio';
import * as AngularMaterialSelect from '@angular/material/select';
import * as AngularMaterialSidenav from '@angular/material/sidenav';
import * as AngularMaterialSlideToggle from '@angular/material/slide-toggle';
import * as AngularMaterialSlider from '@angular/material/slider';
import * as AngularMaterialSnackBar from '@angular/material/snack-bar';
import * as AngularMaterialSort from '@angular/material/sort';
import * as AngularMaterialStepper from '@angular/material/stepper';
import * as AngularMaterialTable from '@angular/material/table';
import * as AngularMaterialTabs from '@angular/material/tabs';
import * as AngularMaterialToolbar from '@angular/material/toolbar';
import * as AngularMaterialTooltip from '@angular/material/tooltip';
import * as AngularMaterialTree from '@angular/material/tree';
import * as DragDropModule from '@angular/cdk/drag-drop';
import * as HttpClientModule from '@angular/common/http';

import * as NgrxStore from '@ngrx/store';
import * as RxJs from 'rxjs';
import * as RxJsOperators from 'rxjs/operators';
import * as TranslateCore from '@ngx-translate/core';
import * as MatDateTimePicker from '@mat-datetimepicker/core';
import _moment from 'moment';
import * as tslib from 'tslib';

import * as TbCore from '@core/public-api';
import * as TbShared from '@shared/public-api';
import * as TbHomeComponents from '@home/components/public-api';

import * as DateAgoPipe from '@shared/pipe/date-ago.pipe';
import * as EnumToArrayPipe from '@shared/pipe/enum-to-array.pipe';
import * as FileSizePipe from '@shared/pipe/file-size.pipe';
import * as HighlightPipe from '@shared/pipe/highlight.pipe';
import * as KeyboardShortcutPipe from '@shared/pipe/keyboard-shortcut.pipe';
import * as MillisecondsToTimeStringPipe from '@shared/pipe/milliseconds-to-time-string.pipe';
import * as NospacePipe from '@shared/pipe/nospace.pipe';
import * as SafePipe from '@shared/pipe/safe.pipe';
import * as SelectableColumnsPipe from '@shared/pipe/selectable-columns.pipe';
import * as ShortNumberPipe from '@shared/pipe/short-number.pipe';
import * as TbJsonPipe from '@shared/pipe/tbJson.pipe';
import * as TruncatePipe from '@shared/pipe/truncate.pipe';
import * as ImagePipe from '@shared/pipe/image.pipe';

import * as EllipsisChipListDirective from '@shared/directives/ellipsis-chip-list.directive';
import * as TruncateWithTooltipDirective from '@shared/directives/truncate-with-tooltip.directive';

import * as coercion from '@shared/decorators/coercion';
import * as enumerable from '@shared/decorators/enumerable';

import * as FooterComponent from '@shared/components/footer.component';
import * as LogoComponent from '@shared/components/logo.component';
import * as FooterFabButtonsComponent from '@shared/components/footer-fab-buttons.component';
import * as FullscreenDirective from '@shared/components/fullscreen.directive';
import * as CircularProgressDirective from '@shared/components/circular-progress.directive';
import * as TbHotkeysDirective from '@shared/components/hotkeys.directive';
import * as TbAnchorComponent from '@shared/components/tb-anchor.component';
import * as TbPopoverComponent from '@shared/components/popover.component';
import * as TbStringTemplateOutletDirective from '@shared/components/directives/sring-template-outlet.directive';
import * as TbComponentOutletDirective from '@shared/components/directives/component-outlet.directive';
import * as TbMarkdownComponent from '@shared/components/markdown.component';
import * as HelpComponent from '@shared/components/help.component';
import * as HelpMarkdownComponent from '@shared/components/help-markdown.component';
import * as HelpPopupComponent from '@shared/components/help-popup.component';
import * as TbCheckboxComponent from '@shared/components/tb-checkbox.component';
import * as TbToast from '@shared/components/toast.directive';
import * as TbErrorComponent from '@shared/components/tb-error.component';
import * as TbCheatSheetComponent from '@shared/components/cheatsheet.component';
import * as BreadcrumbComponent from '@shared/components/breadcrumb.component';
import * as UserMenuComponent from '@shared/components/user-menu.component';
import * as TimewindowComponent from '@shared/components/time/timewindow.component';
import * as TimewindowPanelComponent from '@shared/components/time/timewindow-panel.component';
import * as TimeintervalComponent from '@shared/components/time/timeinterval.component';
import * as QuickTimeIntervalComponent from '@shared/components/time/quick-time-interval.component';
import * as DashboardSelectComponent from '@shared/components/dashboard-select.component';
import * as DashboardSelectPanelComponent from '@shared/components/dashboard-select-panel.component';
import * as DatetimePeriodComponent from '@shared/components/time/datetime-period.component';
import * as DatetimeComponent from '@shared/components/time/datetime.component';
import * as TimezoneSelectComponent from '@shared/components/time/timezone-select.component';
import * as ValueInputComponent from '@shared/components/value-input.component';
import * as DashboardAutocompleteComponent from '@shared/components/dashboard-autocomplete.component';
import * as EntitySubTypeAutocompleteComponent from '@shared/components/entity/entity-subtype-autocomplete.component';
import * as EntitySubTypeSelectComponent from '@shared/components/entity/entity-subtype-select.component';
import * as EntitySubTypeListComponent from '@shared/components/entity/entity-subtype-list.component';
import * as EntityAutocompleteComponent from '@shared/components/entity/entity-autocomplete.component';
import * as EntityListComponent from '@shared/components/entity/entity-list.component';
import * as EntityTypeSelectComponent from '@shared/components/entity/entity-type-select.component';
import * as EntitySelectComponent from '@shared/components/entity/entity-select.component';
import * as EntityKeysListComponent from '@shared/components/entity/entity-keys-list.component';
import * as EntityListSelectComponent from '@shared/components/entity/entity-list-select.component';
import * as EntityTypeListComponent from '@shared/components/entity/entity-type-list.component';
import * as QueueAutocompleteComponent from '@shared/components/queue/queue-autocomplete.component';
import * as RelationTypeAutocompleteComponent from '@shared/components/relation/relation-type-autocomplete.component';
import * as SocialSharePanelComponent from '@shared/components/socialshare-panel.component';
import * as JsonObjectEditComponent from '@shared/components/json-object-edit.component';
import * as JsonObjectViewComponent from '@shared/components/json-object-view.component';
import * as JsonContentComponent from '@shared/components/json-content.component';
import * as JsFuncComponent from '@shared/components/js-func.component';
import * as TbScriptLangComponent from '@shared/components/script-lang.component';
import * as FabToolbarComponent from '@shared/components/fab-toolbar.component';
import * as WidgetsBundleSelectComponent from '@shared/components/widgets-bundle-select.component';
import * as ConfirmDialogComponent from '@shared/components/dialog/confirm-dialog.component';
import * as AlertDialogComponent from '@shared/components/dialog/alert-dialog.component';
import * as TodoDialogComponent from '@shared/components/dialog/todo-dialog.component';
import * as ColorPickerDialogComponent from '@shared/components/dialog/color-picker-dialog.component';
import * as MaterialIconsDialogComponent from '@shared/components/dialog/material-icons-dialog.component';
import * as ColorInputComponent from '@shared/components/color-input.component';
import * as MaterialIconSelectComponent from '@shared/components/material-icon-select.component';
import * as NodeScriptTestDialogComponent from '@shared/components/dialog/node-script-test-dialog.component';
import * as NotificationComponent from '@shared/components/notification/notification.component';
import * as TemplateAutocompleteComponent from '@shared/components/notification/template-autocomplete.component';
import * as ImageInputComponent from '@shared/components/image-input.component';
import * as FileInputComponent from '@shared/components/file-input.component';
import * as MessageTypeAutocompleteComponent from '@shared/components/message-type-autocomplete.component';
import * as KeyValMapComponent from '@shared/components/kv-map.component';
import * as MultipleImageInputComponent from '@shared/components/multiple-image-input.component';
import * as NavTreeComponent from '@shared/components/nav-tree.component';
import * as LedLightComponent from '@shared/components/led-light.component';
import * as TbJsonToStringDirective from '@shared/components/directives/tb-json-to-string.directive';
import * as JsonObjectEditDialogComponent from '@shared/components/dialog/json-object-edit-dialog.component';
import * as HistorySelectorComponent from '@shared/components/time/history-selector/history-selector.component';
import * as EntityGatewaySelectComponent from '@shared/components/entity/entity-gateway-select.component';
import * as ContactComponent from '@shared/components/contact.component';
import * as OtaPackageAutocompleteComponent from '@shared/components/ota-package/ota-package-autocomplete.component';
import * as WidgetsBundleSearchComponent from '@shared/components/widgets-bundle-search.component';
import * as CopyButtonComponent from '@shared/components/button/copy-button.component';
import * as TogglePasswordComponent from '@shared/components/button/toggle-password.component';
import * as ProtobufContentComponent from '@shared/components/protobuf-content.component';
import * as SlackConversationAutocompleteComponent from '@shared/components/slack-conversation-autocomplete.component';
import * as StringItemsListComponent from '@shared/components/string-items-list.component';
import * as ToggleHeaderComponent from '@shared/components/toggle-header.component';
import * as ToggleSelectComponent from '@shared/components/toggle-select.component';
import * as UnitInputComponent from '@shared/components/unit-input.component';
import * as MaterialIconsComponent from '@shared/components/material-icons.component';
import * as TbIconComponent from '@shared/components/icon.component';
import * as HintTooltipIconComponent from '@shared/components/hint-tooltip-icon.component';
import * as ScrollGridComponent from '@shared/components/grid/scroll-grid.component';
import * as GalleryImageInputComponent from '@shared/components/image/gallery-image-input.component';
import * as MultipleGalleryImageInputComponent from '@shared/components/image/multiple-gallery-image-input.component';
import * as TbPopoverService from '@shared/components/popover.service';


import * as CssUnitSelectComponent from '@home/components/widget/lib/settings/common/css-unit-select.component';
import * as WidgetActionsPanelComponent from '@home/components/widget/config/basic/common/widget-actions-panel.component';
import * as FontSettingsComponent from '@home/components/widget/lib/settings/common/font-settings.component';
import * as ColorSettingsComponent from '@home/components/widget/lib/settings/common/color-settings.component';
import * as DisplayColumnsPanelComponent from '@home/components/widget/lib/display-columns-panel.component';
import * as AlarmDetailsDialogComponent from '@home/components/alarm/alarm-details-dialog.component';
import * as AlarmAssigneePanelComponent from '@home/components/alarm/alarm-assignee-panel.component';
import * as AlarmCommentDialogComponent from '@home/components/alarm/alarm-comment-dialog.component';
import * as AlarmFilterConfigComponent from '@home/components/alarm/alarm-filter-config.component';
import * as DatasourceComponent from '@home/components/widget/config/datasources.component';
import * as DataKeysPanelComponent from '@home/components/widget/config/basic/common/data-keys-panel.component';
import * as AddEntityDialogComponent from '@home/components/entity/add-entity-dialog.component';
import * as EntitiesTableComponent from '@home/components/entity/entities-table.component';
import * as DetailsPanelComponent from '@home/components/details-panel.component';
import * as EntityDetailsPanelComponent from '@home/components/entity/entity-details-panel.component';
import * as AuditLogDetailsDialogComponent from '@home/components/audit-log/audit-log-details-dialog.component';
import * as AuditLogTableComponent from '@home/components/audit-log/audit-log-table.component';
import * as EventTableHeaderComponent from '@home/components/event/event-table-header.component';
import * as EventTableComponent from '@home/components/event/event-table.component';
import * as EventFilterPanelComponent from '@home/components/event/event-filter-panel.component';
import * as RelationTableComponent from '@home/components/relation/relation-table.component';
import * as RelationDialogComponent from '@home/components/relation/relation-dialog.component';
import * as AlarmTableHeaderComponent from '@home/components/alarm/alarm-table-header.component';
import * as AlarmTableComponent from '@home/components/alarm/alarm-table.component';
import * as AttributeTableComponent from '@home/components/attribute/attribute-table.component';
import * as AddAttributeDialogComponent from '@home/components/attribute/add-attribute-dialog.component';
import * as EditAttributeValuePanelComponent from '@home/components/attribute/edit-attribute-value-panel.component';
import * as DashboardComponent from '@home/components/dashboard/dashboard.component';
import * as WidgetComponent from '@home/components/widget/widget.component';
import * as LegendComponent from '@home/components/widget/lib/legend.component';
import * as AliasesEntitySelectPanelComponent from '@home/components/alias/aliases-entity-select-panel.component';
import * as AliasesEntitySelectComponent from '@home/components/alias/aliases-entity-select.component';
import * as WidgetConfigComponent from '@home/components/widget/widget-config.component';
import * as EntityAliasesDialogComponent from '@home/components/alias/entity-aliases-dialog.component';
import * as EntityFilterViewComponent from '@home/components/entity/entity-filter-view.component';
import * as EntityAliasDialogComponent from '@home/components/alias/entity-alias-dialog.component';
import * as EntityFilterComponent from '@home/components/entity/entity-filter.component';
import * as RelationFiltersComponent from '@home/components/relation/relation-filters.component';
import * as EntityAliasSelectComponent from '@home/components/widget/lib/settings/common/alias/entity-alias-select.component';
import * as DataKeysComponent from '@home/components/widget/lib/settings/common/key/data-keys.component';
import * as DataKeyConfigDialogComponent from '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component';
import * as DataKeyConfigComponent from '@home/components/widget/lib/settings/common/key/data-key-config.component';
import * as LegendConfigComponent from '@home/components/widget/lib/settings/common/legend-config.component';
import * as ManageWidgetActionsComponent from '@home/components/widget/action/manage-widget-actions.component';
import * as WidgetActionDialogComponent from '@home/components/widget/action/widget-action-dialog.component';
import * as CustomActionPrettyResourcesTabsComponent from '@home/components/widget/lib/settings/common/action/custom-action-pretty-resources-tabs.component';
import * as CustomActionPrettyEditorComponent from '@home/components/widget/lib/settings/common/action/custom-action-pretty-editor.component';
import * as MobileActionEditorComponent from '@home/components/widget/lib/settings/common/action/mobile-action-editor.component';
import * as CustomDialogService from '@home/components/widget/dialog/custom-dialog.service';
import * as CustomDialogContainerComponent from '@home/components/widget/dialog/custom-dialog-container.component';
import * as ImportExportService from '@shared/import-export/import-export.service';
import * as ImportDialogComponent from '@shared/import-export/import-dialog.component';
import * as AddWidgetToDashboardDialogComponent from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
import * as ImportDialogCsvComponent from '@shared/import-export/import-dialog-csv.component';
import * as TableColumnsAssignmentComponent from '@shared/import-export/table-columns-assignment.component';
import * as EventContentDialogComponent from '@home/components/event/event-content-dialog.component';
import * as SharedHomeComponentsModule from '@home/components/shared-home-components.module';
import * as WidgetConfigComponentsModule from '@home/components/widget/config/widget-config-components.module';
import * as BasicWidgetConfigModule from '@home/components/widget/config/basic/basic-widget-config.module';
import * as WidgetSettingsCommonModule from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import * as WidgetComponentsModule from '@home/components/widget/widget-components.module';
import * as SelectTargetLayoutDialogComponent from '@home/components/dashboard/select-target-layout-dialog.component';
import * as SelectTargetStateDialogComponent from '@home/components/dashboard/select-target-state-dialog.component';
import * as AliasesEntityAutocompleteComponent from '@home/components/alias/aliases-entity-autocomplete.component';
import * as BooleanFilterPredicateComponent from '@home/components/filter/boolean-filter-predicate.component';
import * as StringFilterPredicateComponent from '@home/components/filter/string-filter-predicate.component';
import * as NumericFilterPredicateComponent from '@home/components/filter/numeric-filter-predicate.component';
import * as ComplexFilterPredicateComponent from '@home/components/filter/complex-filter-predicate.component';
import * as FilterPredicateComponent from '@home/components/filter/filter-predicate.component';
import * as FilterPredicateListComponent from '@home/components/filter/filter-predicate-list.component';
import * as KeyFilterListComponent from '@home/components/filter/key-filter-list.component';
import * as ComplexFilterPredicateDialogComponent from '@home/components/filter/complex-filter-predicate-dialog.component';
import * as KeyFilterDialogComponent from '@home/components/filter/key-filter-dialog.component';
import * as FiltersDialogComponent from '@home/components/filter/filters-dialog.component';
import * as FilterDialogComponent from '@home/components/filter/filter-dialog.component';
import * as FilterSelectComponent from '@home/components/widget/lib/settings/common/filter/filter-select.component';
import * as FiltersEditComponent from '@home/components/filter/filters-edit.component';
import * as FiltersEditPanelComponent from '@home/components/filter/filters-edit-panel.component';
import * as UserFilterDialogComponent from '@home/components/filter/user-filter-dialog.component';
import * as FilterUserInfoComponent from '@home/components/filter/filter-user-info.component';
import * as FilterUserInfoDialogComponent from '@home/components/filter/filter-user-info-dialog.component';
import * as FilterPredicateValueComponent from '@home/components/filter/filter-predicate-value.component';
import * as TenantProfileComponent from '@home/components/profile/tenant-profile.component';
import * as TenantProfileDialogComponent from '@home/components/profile/tenant-profile-dialog.component';
import * as TenantProfileDataComponent from '@home/components/profile/tenant-profile-data.component';
import * as DefaultDeviceProfileConfigurationComponent from '@home/components/profile/device/default-device-profile-configuration.component';
import * as DeviceProfileConfigurationComponent from '@home/components/profile/device/device-profile-configuration.component';
import * as DeviceProfileComponent from '@home/components/profile/device-profile.component';
import * as DefaultDeviceProfileTransportConfigurationComponent from '@home/components/profile/device/default-device-profile-transport-configuration.component';
import * as DeviceProfileTransportConfigurationComponent from '@home/components/profile/device/device-profile-transport-configuration.component';
import * as DeviceProfileDialogComponent from '@home/components/profile/device-profile-dialog.component';
import * as DeviceProfileAutocompleteComponent from '@home/components/profile/device-profile-autocomplete.component';
import * as MqttDeviceProfileTransportConfigurationComponent from '@home/components/profile/device/mqtt-device-profile-transport-configuration.component';
import * as CoapDeviceProfileTransportConfigurationComponent from '@home/components/profile/device/coap-device-profile-transport-configuration.component';
import * as DeviceProfileAlarmsComponent from '@home/components/profile/alarm/device-profile-alarms.component';
import * as DeviceProfileAlarmComponent from '@home/components/profile/alarm/device-profile-alarm.component';
import * as CreateAlarmRulesComponent from '@home/components/profile/alarm/create-alarm-rules.component';
import * as AlarmRuleComponent from '@home/components/profile/alarm/alarm-rule.component';
import * as AlarmRuleConditionComponent from '@home/components/profile/alarm/alarm-rule-condition.component';
import * as FilterTextComponent from '@home/components/filter/filter-text.component';
import * as AddDeviceProfileDialogComponent from '@home/components/profile/add-device-profile-dialog.component';
import * as RuleChainAutocompleteComponent from '@home/components/rule-chain/rule-chain-autocomplete.component';
import * as DeviceProfileProvisionConfigurationComponent from '@home/components/profile/device-profile-provision-configuration.component';
import * as AlarmScheduleComponent from '@home/components/profile/alarm/alarm-schedule.component';
import * as DeviceWizardDialogComponent from '@home/components/wizard/device-wizard-dialog.component';
import * as AlarmScheduleInfoComponent from '@home/components/profile/alarm/alarm-schedule-info.component';
import * as AlarmScheduleDialogComponent from '@home/components/profile/alarm/alarm-schedule-dialog.component';
import * as EditAlarmDetailsDialogComponent from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import * as AlarmRuleConditionDialogComponent from '@home/components/profile/alarm/alarm-rule-condition-dialog.component';
import * as DefaultTenantProfileConfigurationComponent from '@home/components/profile/tenant/default-tenant-profile-configuration.component';
import * as TenantProfileConfigurationComponent from '@home/components/profile/tenant/tenant-profile-configuration.component';
import * as SmsProviderConfigurationComponent from '@home/components/sms/sms-provider-configuration.component';
import * as AwsSnsProviderConfigurationComponent from '@home/components/sms/aws-sns-provider-configuration.component';
import * as TwilioSmsProviderConfigurationComponent from '@home/components/sms/twilio-sms-provider-configuration.component';
import * as DashboardPageComponent from '@home/components/dashboard-page/dashboard-page.component';
import * as DashboardToolbarComponent from '@home/components/dashboard-page/dashboard-toolbar.component';
import * as DashboardLayoutComponent from '@home/components/dashboard-page/layout/dashboard-layout.component';
import * as EditWidgetComponent from '@home/components/dashboard-page/edit-widget.component';
import * as DashboardWidgetSelectComponent from '@home/components/dashboard-page/dashboard-widget-select.component';
import * as AddWidgetDialogComponent from '@home/components/dashboard-page/add-widget-dialog.component';
import * as ManageDashboardLayoutsDialogComponent from '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component';
import * as DashboardSettingsDialogComponent from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import * as ManageDashboardStatesDialogComponent from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import * as DashboardStateDialogComponent from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import * as EmbedDashboardDialogComponent from '@home/components/widget/dialog/embed-dashboard-dialog.component';
import * as EdgeDownlinkTableComponent from '@home/components/edge/edge-downlink-table.component';
import * as EdgeDownlinkTableHeaderComponent from '@home/components/edge/edge-downlink-table-header.component';
import * as DisplayWidgetTypesPanelComponent from '@home/components/dashboard-page/widget-types-panel.component';
import * as AlarmDurationPredicateValueComponent from '@home/components/profile/alarm/alarm-duration-predicate-value.component';
import * as DashboardImageDialogComponent from '@home/components/dashboard-page/dashboard-image-dialog.component';
import * as WidgetContainerComponent from '@home/components/widget/widget-container.component';
import * as TenantProfileQueuesComponent from '@home/components/profile/queue/tenant-profile-queues.component';
import * as QueueFormComponent from '@home/components/queue/queue-form.component';
import * as AssetProfileComponent from '@home/components/profile/asset-profile.component';
import * as AssetProfileDialogComponent from '@home/components/profile/asset-profile-dialog.component';
import * as AssetProfileAutocompleteComponent from '@home/components/profile/asset-profile-autocomplete.component';
import * as RuleChainSelectComponent from '@shared/components/rule-chain/rule-chain-select.component';
import * as TimezoneComponent from '@shared/components/time/timezone.component';
import * as TimezonePanelComponent from '@shared/components/time/timezone-panel.component';
import * as DatapointsLimitComponent from '@shared/components/time/datapoints-limit.component';
import * as AggregationTypeSelectComponent from '@shared/components/time/aggregation/aggregation-type-select.component';
import * as AggregationOptionsConfigComponent from '@shared/components/time/aggregation/aggregation-options-config-panel.component';
import * as IntervalOptionsConfigPanelComponent from '@shared/components/time/interval-options-config-panel.component';
import * as AIModelDialogComponent from '@home/components/ai-model/ai-model-dialog.component';

import { IModulesMap } from '@modules/common/modules-map.models';
import { Observable, of } from 'rxjs';
import { isJSResourceUrl } from '@shared/public-api';

class ModulesMap implements IModulesMap {

  private initialized = false;

  private modulesMap: {[key: string]: any} = {
    '@angular/animations': AngularAnimations,
    '@angular/core': {...AngularCore, ɵɵStandaloneFeature: () => {}, ɵɵInputTransformsFeature: () => {}, ɵɵpropertyInterpolate: (...args) => {
        // @ts-ignore
        return AngularCore.ɵɵproperty(...args);
      }},
    '@angular/common': AngularCommon,
    '@angular/common/http': HttpClientModule,
    '@angular/forms': AngularForms,
    '@angular/platform-browser': AngularPlatformBrowser,
    '@angular/platform-browser/animations': AngularPlatformBrowserAnimations,
    '@angular/router': AngularRouter,
    '@angular/cdk/coercion': AngularCdkCoercion,
    '@angular/cdk/collections': AngularCdkCollections,
    '@angular/cdk/keycodes': AngularCdkKeycodes,
    '@angular/cdk/layout': AngularCdkLayout,
    '@angular/cdk/overlay': AngularCdkOverlay,
    '@angular/cdk/portal': AngularCdkPortal,
    '@angular/cdk/bidi': AngularCdkBidi,
    '@angular/cdk/platform': AngularCdkPlatform,
    '@angular/cdk/drag-drop': DragDropModule,
    '@angular/material/autocomplete': AngularMaterialAutocomplete,
    '@angular/material/badge': AngularMaterialBadge,
    '@angular/material/bottom-sheet': AngularMaterialBottomSheet,
    '@angular/material/button': AngularMaterialButton,
    '@angular/material/button-toggle': AngularMaterialButtonToggle,
    '@angular/material/card': AngularMaterialCard,
    '@angular/material/checkbox': AngularMaterialCheckbox,
    '@angular/material/chips': AngularMaterialChips,
    '@angular/material/core': AngularMaterialCore,
    '@angular/material/datepicker': AngularMaterialDatepicker,
    '@angular/material/dialog': AngularMaterialDialog,
    '@angular/material/divider': AngularMaterialDivider,
    '@angular/material/expansion': AngularMaterialExpansion,
    '@angular/material/form-field': AngularMaterialFormField,
    '@angular/material/grid-list': AngularMaterialGridList,
    '@angular/material/icon': AngularMaterialIcon,
    '@angular/material/input': AngularMaterialInput,
    '@angular/material/list': AngularMaterialList,
    '@angular/material/menu': AngularMaterialMenu,
    '@angular/material/paginator': AngularMaterialPaginator,
    '@angular/material/progress-bar': AngularMaterialProgressBar,
    '@angular/material/progress-spinner': AngularMaterialProgressSpinner,
    '@angular/material/radio': AngularMaterialRadio,
    '@angular/material/select': AngularMaterialSelect,
    '@angular/material/sidenav': AngularMaterialSidenav,
    '@angular/material/slide-toggle': AngularMaterialSlideToggle,
    '@angular/material/slider': AngularMaterialSlider,
    '@angular/material/snack-bar': AngularMaterialSnackBar,
    '@angular/material/sort': AngularMaterialSort,
    '@angular/material/stepper': AngularMaterialStepper,
    '@angular/material/table': AngularMaterialTable,
    '@angular/material/tabs': AngularMaterialTabs,
    '@angular/material/toolbar': AngularMaterialToolbar,
    '@angular/material/tooltip': AngularMaterialTooltip,
    '@angular/material/tree': AngularMaterialTree,
    '@ngrx/store': NgrxStore,
    rxjs: RxJs,
    'rxjs/operators': RxJsOperators,
    '@ngx-translate/core': TranslateCore,
    '@mat-datetimepicker/core': MatDateTimePicker,
    moment: _moment,
    tslib,

    '@core/public-api': TbCore,
    '@shared/public-api': TbShared,
    '@home/components/public-api': TbHomeComponents,

    '@shared/pipe/date-ago.pipe': DateAgoPipe,
    '@shared/pipe/enum-to-array.pipe': EnumToArrayPipe,
    '@shared/pipe/file-size.pipe': FileSizePipe,
    '@shared/pipe/highlight.pipe': HighlightPipe,
    '@shared/pipe/keyboard-shortcut.pipe': KeyboardShortcutPipe,
    '@shared/pipe/milliseconds-to-time-string.pipe': MillisecondsToTimeStringPipe,
    '@shared/pipe/nospace.pipe': NospacePipe,
    '@shared/pipe/safe.pipe': SafePipe,
    '@shared/pipe/selectable-columns.pipe': SelectableColumnsPipe,
    '@shared/pipe/short-number.pipe': ShortNumberPipe,
    '@shared/pipe/tbJson.pipe': TbJsonPipe,
    '@shared/pipe/truncate.pipe': TruncatePipe,
    '@shared/pipe/image.pipe': ImagePipe,

    '@shared/directives/ellipsis-chip-list.directive': EllipsisChipListDirective,
    '@shared/directives/truncate-with-tooltip.directive': TruncateWithTooltipDirective,

    '@shared/decorators/coercion': coercion,
    '@shared/decorators/enumerable': enumerable,

    '@shared/import-export/import-export.service': ImportExportService,
    '@shared/import-export/import-dialog.component': ImportDialogComponent,
    '@shared/import-export/import-dialog-csv.component': ImportDialogCsvComponent,
    '@shared/import-export/table-columns-assignment.component': TableColumnsAssignmentComponent,

    '@shared/components/footer.component': FooterComponent,
    '@shared/components/logo.component': LogoComponent,
    '@shared/components/footer-fab-buttons.component': FooterFabButtonsComponent,
    '@shared/components/fullscreen.directive': FullscreenDirective,
    '@shared/components/circular-progress.directive': CircularProgressDirective,
    '@shared/components/hotkeys.directive': TbHotkeysDirective,
    '@shared/components/tb-anchor.component': TbAnchorComponent,
    '@shared/components/popover.component': TbPopoverComponent,
    '@shared/components/directives/sring-template-outlet.directive': TbStringTemplateOutletDirective,
    '@shared/components/directives/component-outlet.directive': TbComponentOutletDirective,
    '@shared/components/markdown.component': TbMarkdownComponent,
    '@shared/components/help.component': HelpComponent,
    '@shared/components/help-markdown.component': HelpMarkdownComponent,
    '@shared/components/help-popup.component': HelpPopupComponent,
    '@shared/components/tb-checkbox.component': TbCheckboxComponent,
    '@shared/components/toast.directive': TbToast,
    '@shared/components/tb-error.component': TbErrorComponent,
    '@shared/components/cheatsheet.component': TbCheatSheetComponent,
    '@shared/components/breadcrumb.component': BreadcrumbComponent,
    '@shared/components/user-menu.component': UserMenuComponent,
    '@shared/components/time/timewindow.component': TimewindowComponent,
    '@shared/components/time/timewindow-panel.component': TimewindowPanelComponent,
    '@shared/components/time/timeinterval.component': TimeintervalComponent,
    '@shared/components/time/quick-time-interval.component': QuickTimeIntervalComponent,
    '@shared/components/dashboard-select.component': DashboardSelectComponent,
    '@shared/components/dashboard-select-panel.component': DashboardSelectPanelComponent,
    '@shared/components/rule-chain/rule-chain-select.component': RuleChainSelectComponent,
    '@shared/components/time/datetime-period.component': DatetimePeriodComponent,
    '@shared/components/time/datetime.component': DatetimeComponent,
    '@shared/components/time/timezone-select.component': TimezoneSelectComponent,
    '@shared/components/time/timezone.component': TimezoneComponent,
    '@shared/components/time/timezone-panel.component': TimezonePanelComponent,
    '@shared/components/time/datapoints-limit.component': DatapointsLimitComponent,
    '@shared/components/time/aggregation/aggregation-type-select.component': AggregationTypeSelectComponent,
    '@shared/components/time/aggregation/aggregation-options-config-panel.component': AggregationOptionsConfigComponent,
    '@shared/components/time/interval-options-config-panel.component': IntervalOptionsConfigPanelComponent,
    '@shared/components/value-input.component': ValueInputComponent,
    '@shared/components/dashboard-autocomplete.component': DashboardAutocompleteComponent,
    '@shared/components/entity/entity-subtype-autocomplete.component': EntitySubTypeAutocompleteComponent,
    '@shared/components/entity/entity-subtype-select.component': EntitySubTypeSelectComponent,
    '@shared/components/entity/entity-subtype-list.component': EntitySubTypeListComponent,
    '@shared/components/entity/entity-autocomplete.component': EntityAutocompleteComponent,
    '@shared/components/entity/entity-list.component': EntityListComponent,
    '@shared/components/entity/entity-type-select.component': EntityTypeSelectComponent,
    '@shared/components/entity/entity-select.component': EntitySelectComponent,
    '@shared/components/entity/entity-keys-list.component': EntityKeysListComponent,
    '@shared/components/entity/entity-list-select.component': EntityListSelectComponent,
    '@shared/components/entity/entity-type-list.component': EntityTypeListComponent,
    '@shared/components/queue/queue-autocomplete.component': QueueAutocompleteComponent,
    '@shared/components/relation/relation-type-autocomplete.component': RelationTypeAutocompleteComponent,
    '@shared/components/socialshare-panel.component': SocialSharePanelComponent,
    '@shared/components/json-object-edit.component': JsonObjectEditComponent,
    '@shared/components/json-object-view.component': JsonObjectViewComponent,
    '@shared/components/json-content.component': JsonContentComponent,
    '@shared/components/js-func.component': JsFuncComponent,
    '@shared/components/script-lang.component': TbScriptLangComponent,
    '@shared/components/fab-toolbar.component': FabToolbarComponent,
    '@shared/components/widgets-bundle-select.component': WidgetsBundleSelectComponent,
    '@shared/components/dialog/confirm-dialog.component': ConfirmDialogComponent,
    '@shared/components/dialog/alert-dialog.component': AlertDialogComponent,
    '@shared/components/dialog/todo-dialog.component': TodoDialogComponent,
    '@shared/components/dialog/color-picker-dialog.component': ColorPickerDialogComponent,
    '@shared/components/dialog/material-icons-dialog.component': MaterialIconsDialogComponent,
    '@shared/components/color-input.component': ColorInputComponent,
    '@shared/components/material-icon-select.component': MaterialIconSelectComponent,
    '@shared/components/dialog/node-script-test-dialog.component': NodeScriptTestDialogComponent,
    '@shared/components/notification/notification.component': NotificationComponent,
    '@shared/components/notification/template-autocomplete.component': TemplateAutocompleteComponent,
    '@shared/components/image-input.component': ImageInputComponent,
    '@shared/components/file-input.component': FileInputComponent,
    '@shared/components/message-type-autocomplete.component': MessageTypeAutocompleteComponent,
    '@shared/components/kv-map.component': KeyValMapComponent,
    '@shared/components/multiple-image-input.component': MultipleImageInputComponent,
    '@shared/components/nav-tree.component': NavTreeComponent,
    '@shared/components/led-light.component': LedLightComponent,
    '@shared/components/directives/tb-json-to-string.directive': TbJsonToStringDirective,
    '@shared/components/dialog/json-object-edit-dialog.component': JsonObjectEditDialogComponent,
    '@shared/components/time/history-selector/history-selector.component': HistorySelectorComponent,
    '@shared/components/entity/entity-gateway-select.component': EntityGatewaySelectComponent,
    '@shared/components/contact.component': ContactComponent,
    '@shared/components/ota-package/ota-package-autocomplete.component': OtaPackageAutocompleteComponent,
    '@shared/components/widgets-bundle-search.component': WidgetsBundleSearchComponent,
    '@shared/components/button/copy-button.component': CopyButtonComponent,
    '@shared/components/button/toggle-password.component': TogglePasswordComponent,
    '@shared/components/protobuf-content.component': ProtobufContentComponent,
    '@shared/components/slack-conversation-autocomplete.component': SlackConversationAutocompleteComponent,
    '@shared/components/string-items-list.component': StringItemsListComponent,
    '@shared/components/toggle-header.component': ToggleHeaderComponent,
    '@shared/components/toggle-select.component': ToggleSelectComponent,
    '@shared/components/unit-input.component': UnitInputComponent,
    '@shared/components/material-icons.component': MaterialIconsComponent,
    '@shared/components/icon.component': TbIconComponent,
    '@shared/components/hint-tooltip-icon.component': HintTooltipIconComponent,
    '@shared/components/grid/scroll-grid.component': ScrollGridComponent,
    '@shared/components/image/gallery-image-input.component': GalleryImageInputComponent,
    '@shared/components/image/multiple-gallery-image-input.component': MultipleGalleryImageInputComponent,
    '@shared/components/popover.service': TbPopoverService,


    '@home/components/alarm/alarm-filter-config.component': AlarmFilterConfigComponent,
    '@home/components/alarm/alarm-comment-dialog.component': AlarmCommentDialogComponent,
    '@home/components/alarm/alarm-assignee-panel.component': AlarmAssigneePanelComponent,
    '@home/components/alarm/alarm-details-dialog.component': AlarmDetailsDialogComponent,
    '@home/components/widget/lib/display-columns-panel.component': DisplayColumnsPanelComponent,
    '@home/components/widget/config/datasources.component': DatasourceComponent,
    '@home/components/widget/config/basic/common/data-keys-panel.component': DataKeysPanelComponent,
    '@home/components/widget/lib/settings/common/color-settings.component': ColorSettingsComponent,
    '@home/components/widget/lib/settings/common/font-settings.component': FontSettingsComponent,
    '@home/components/widget/config/basic/common/widget-actions-panel.component': WidgetActionsPanelComponent,
    '@home/components/widget/lib/settings/common/css-unit-select.component': CssUnitSelectComponent,
    '@home/components/entity/add-entity-dialog.component': AddEntityDialogComponent,
    '@home/components/entity/entities-table.component': EntitiesTableComponent,
    '@home/components/details-panel.component': DetailsPanelComponent,
    '@home/components/entity/entity-details-panel.component': EntityDetailsPanelComponent,
    '@home/components/audit-log/audit-log-details-dialog.component': AuditLogDetailsDialogComponent,
    '@home/components/audit-log/audit-log-table.component': AuditLogTableComponent,
    '@home/components/event/event-table-header.component': EventTableHeaderComponent,
    '@home/components/event/event-table.component': EventTableComponent,
    '@home/components/event/event-filter-panel.component': EventFilterPanelComponent,
    '@home/components/relation/relation-table.component': RelationTableComponent,
    '@home/components/relation/relation-dialog.component': RelationDialogComponent,
    '@home/components/alarm/alarm-table-header.component': AlarmTableHeaderComponent,
    '@home/components/alarm/alarm-table.component': AlarmTableComponent,
    '@home/components/attribute/attribute-table.component': AttributeTableComponent,
    '@home/components/attribute/add-attribute-dialog.component': AddAttributeDialogComponent,
    '@home/components/attribute/edit-attribute-value-panel.component': EditAttributeValuePanelComponent,
    '@home/components/dashboard/dashboard.component': DashboardComponent,
    '@home/components/widget/widget.component': WidgetComponent,
    '@home/components/widget/lib/legend.component': LegendComponent,
    '@home/components/alias/aliases-entity-select-panel.component': AliasesEntitySelectPanelComponent,
    '@home/components/alias/aliases-entity-select.component': AliasesEntitySelectComponent,
    '@home/components/widget/widget-config.component': WidgetConfigComponent,
    '@home/components/alias/entity-aliases-dialog.component': EntityAliasesDialogComponent,
    '@home/components/entity/entity-filter-view.component': EntityFilterViewComponent,
    '@home/components/alias/entity-alias-dialog.component': EntityAliasDialogComponent,
    '@home/components/entity/entity-filter.component': EntityFilterComponent,
    '@home/components/relation/relation-filters.component': RelationFiltersComponent,
    '@home/components/widget/lib/settings/common/alias/entity-alias-select.component': EntityAliasSelectComponent,
    '@home/components/widget/lib/settings/common/key/data-keys.component': DataKeysComponent,
    '@home/components/widget/lib/settings/common/key/data-key-config-dialog.component': DataKeyConfigDialogComponent,
    '@home/components/widget/lib/settings/common/key/data-key-config.component': DataKeyConfigComponent,
    '@home/components/widget/lib/settings/common/legend-config.component': LegendConfigComponent,
    '@home/components/widget/action/manage-widget-actions.component': ManageWidgetActionsComponent,
    '@home/components/widget/action/widget-action-dialog.component': WidgetActionDialogComponent,
    '@home/components/widget/lib/settings/common/action/custom-action-pretty-resources-tabs.component': CustomActionPrettyResourcesTabsComponent,
    '@home/components/widget/lib/settings/common/action/custom-action-pretty-editor.component': CustomActionPrettyEditorComponent,
    '@home/components/widget/lib/settings/common/action/mobile-action-editor.component': MobileActionEditorComponent,
    '@home/components/widget/dialog/custom-dialog.service': CustomDialogService,
    '@home/components/widget/dialog/custom-dialog-container.component': CustomDialogContainerComponent,
    '@home/components/attribute/add-widget-to-dashboard-dialog.component': AddWidgetToDashboardDialogComponent,
    '@home/components/event/event-content-dialog.component': EventContentDialogComponent,
    '@home/components/shared-home-components.module': SharedHomeComponentsModule,
    '@home/components/widget/config/widget-config-components.module': WidgetConfigComponentsModule,
    '@home/components/widget/config/basic/basic-widget-config.module': BasicWidgetConfigModule,
    '@home/components/widget/lib/settings/common/widget-settings-common.module': WidgetSettingsCommonModule,
    '@home/components/widget/widget-components.module': WidgetComponentsModule,
    '@home/components/dashboard/select-target-layout-dialog.component': SelectTargetLayoutDialogComponent,
    '@home/components/dashboard/select-target-state-dialog.component': SelectTargetStateDialogComponent,
    '@home/components/alias/aliases-entity-autocomplete.component': AliasesEntityAutocompleteComponent,
    '@home/components/filter/boolean-filter-predicate.component': BooleanFilterPredicateComponent,
    '@home/components/filter/string-filter-predicate.component': StringFilterPredicateComponent,
    '@home/components/filter/numeric-filter-predicate.component': NumericFilterPredicateComponent,
    '@home/components/filter/complex-filter-predicate.component': ComplexFilterPredicateComponent,
    '@home/components/filter/filter-predicate.component': FilterPredicateComponent,
    '@home/components/filter/filter-predicate-list.component': FilterPredicateListComponent,
    '@home/components/filter/key-filter-list.component': KeyFilterListComponent,
    '@home/components/filter/complex-filter-predicate-dialog.component': ComplexFilterPredicateDialogComponent,
    '@home/components/filter/key-filter-dialog.component': KeyFilterDialogComponent,
    '@home/components/filter/filters-dialog.component': FiltersDialogComponent,
    '@home/components/filter/filter-dialog.component': FilterDialogComponent,
    '@home/components/widget/lib/settings/common/filter/filter-select.component': FilterSelectComponent,
    '@home/components/filter/filters-edit.component': FiltersEditComponent,
    '@home/components/filter/filters-edit-panel.component': FiltersEditPanelComponent,
    '@home/components/filter/user-filter-dialog.component': UserFilterDialogComponent,
    '@home/components/filter/filter-user-info.component': FilterUserInfoComponent,
    '@home/components/filter/filter-user-info-dialog.component': FilterUserInfoDialogComponent,
    '@home/components/filter/filter-predicate-value.component': FilterPredicateValueComponent,
    '@home/components/profile/tenant-profile.component': TenantProfileComponent,
    '@home/components/profile/tenant-profile-dialog.component': TenantProfileDialogComponent,
    '@home/components/profile/tenant-profile-data.component': TenantProfileDataComponent,
    '@home/components/profile/device/default-device-profile-configuration.component': DefaultDeviceProfileConfigurationComponent,
    '@home/components/profile/device/device-profile-configuration.component': DeviceProfileConfigurationComponent,
    '@home/components/profile/device-profile.component': DeviceProfileComponent,
    '@home/components/profile/device/default-device-profile-transport-configuration.component':
    DefaultDeviceProfileTransportConfigurationComponent,
    '@home/components/profile/device/device-profile-transport-configuration.component': DeviceProfileTransportConfigurationComponent,
    '@home/components/profile/device-profile-dialog.component': DeviceProfileDialogComponent,
    '@home/components/profile/device-profile-autocomplete.component': DeviceProfileAutocompleteComponent,
    '@home/components/profile/device/mqtt-device-profile-transport-configuration.component':
    MqttDeviceProfileTransportConfigurationComponent,
    '@home/components/profile/device/coap-device-profile-transport-configuration.component':
    CoapDeviceProfileTransportConfigurationComponent,
    '@home/components/profile/asset-profile.component': AssetProfileComponent,
    '@home/components/profile/asset-profile-dialog.component': AssetProfileDialogComponent,
    '@home/components/profile/asset-profile-autocomplete.component': AssetProfileAutocompleteComponent,
    '@home/components/profile/alarm/device-profile-alarms.component': DeviceProfileAlarmsComponent,
    '@home/components/profile/alarm/device-profile-alarm.component': DeviceProfileAlarmComponent,
    '@home/components/profile/alarm/create-alarm-rules.component': CreateAlarmRulesComponent,
    '@home/components/profile/alarm/alarm-rule.component': AlarmRuleComponent,
    '@home/components/profile/alarm/alarm-rule-condition.component': AlarmRuleConditionComponent,
    '@home/components/filter/filter-text.component': FilterTextComponent,
    '@home/components/profile/add-device-profile-dialog.component': AddDeviceProfileDialogComponent,
    '@home/components/rule-chain/rule-chain-autocomplete.component': RuleChainAutocompleteComponent,
    '@home/components/profile/device-profile-provision-configuration.component': DeviceProfileProvisionConfigurationComponent,
    '@home/components/profile/alarm/alarm-schedule.component': AlarmScheduleComponent,
    '@home/components/wizard/device-wizard-dialog.component': DeviceWizardDialogComponent,
    '@home/components/profile/alarm/alarm-schedule-info.component': AlarmScheduleInfoComponent,
    '@home/components/profile/alarm/alarm-schedule-dialog.component': AlarmScheduleDialogComponent,
    '@home/components/profile/alarm/edit-alarm-details-dialog.component': EditAlarmDetailsDialogComponent,
    '@home/components/profile/alarm/alarm-rule-condition-dialog.component': AlarmRuleConditionDialogComponent,
    '@home/components/profile/tenant/default-tenant-profile-configuration.component': DefaultTenantProfileConfigurationComponent,
    '@home/components/profile/tenant/tenant-profile-configuration.component': TenantProfileConfigurationComponent,
    '@home/components/sms/sms-provider-configuration.component': SmsProviderConfigurationComponent,
    '@home/components/sms/aws-sns-provider-configuration.component': AwsSnsProviderConfigurationComponent,
    '@home/components/sms/twilio-sms-provider-configuration.component': TwilioSmsProviderConfigurationComponent,
    '@home/components/dashboard-page/dashboard-page.component': DashboardPageComponent,
    '@home/components/dashboard-page/dashboard-toolbar.component': DashboardToolbarComponent,
    '@home/components/dashboard-page/layout/dashboard-layout.component': DashboardLayoutComponent,
    '@home/components/dashboard-page/edit-widget.component': EditWidgetComponent,
    '@home/components/dashboard-page/dashboard-widget-select.component': DashboardWidgetSelectComponent,
    '@home/components/dashboard-page/add-widget-dialog.component': AddWidgetDialogComponent,
    '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component': ManageDashboardLayoutsDialogComponent,
    '@home/components/dashboard-page/dashboard-settings-dialog.component': DashboardSettingsDialogComponent,
    '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component': ManageDashboardStatesDialogComponent,
    '@home/components/dashboard-page/states/dashboard-state-dialog.component': DashboardStateDialogComponent,
    '@home/components/widget/dialog/embed-dashboard-dialog.component': EmbedDashboardDialogComponent,
    '@home/components/edge/edge-downlink-table.component': EdgeDownlinkTableComponent,
    '@home/components/edge/edge-downlink-table-header.component': EdgeDownlinkTableHeaderComponent,
    '@home/components/dashboard-page/widget-types-panel.component': DisplayWidgetTypesPanelComponent,
    '@home/components/profile/alarm/alarm-duration-predicate-value.component': AlarmDurationPredicateValueComponent,
    '@home/components/dashboard-page/dashboard-image-dialog.component': DashboardImageDialogComponent,
    '@home/components/widget/widget-container.component': WidgetContainerComponent,
    '@home/components/profile/queue/tenant-profile-queues.component': TenantProfileQueuesComponent,
    '@home/components/queue/queue-form.component': QueueFormComponent,
    '@home/components/ai-model/ai-model-dialog.component': AIModelDialogComponent,
  };

  init(): Observable<any> {
    if (!this.initialized) {
      System.constructor.prototype.resolve = (id: string) => {
        try {
          if (this.modulesMap[id]) {
            return 'app:' + id;
          } else {
            return id;
          }
        } catch (err) {
          return id;
        }
      };
      for (const moduleId of Object.keys(this.modulesMap)) {
        System.set('app:' + moduleId, this.modulesMap[moduleId]);
      }
      System.constructor.prototype.shouldFetch = (url: string) => url.endsWith('/download') || isJSResourceUrl(url);
      System.constructor.prototype.fetch = (url: string, options: RequestInit & {meta?: any}) => {
        if (options?.meta?.additionalHeaders) {
          options.headers = { ...options.headers, ...options.meta.additionalHeaders };
        }
        return fetch(url, options);
      };
      this.initialized = true;
    }
    return of(null);
  }
}

export const modulesMap = new ModulesMap();
