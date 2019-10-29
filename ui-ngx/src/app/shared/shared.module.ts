///
/// Copyright © 2016-2019 The Thingsboard Authors
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
import { CommonModule, DatePipe } from '@angular/common';
import { FooterComponent } from './components/footer.component';
import { LogoComponent } from './components/logo.component';
import { TbSnackBarComponent, ToastDirective } from './components/toast.directive';
import { BreadcrumbComponent } from '@app/shared/components/breadcrumb.component';

import {
  MatAutocompleteModule,
  MatButtonModule,
  MatCardModule,
  MatCheckboxModule,
  MatChipsModule,
  MatDatepickerModule,
  MatDialogModule,
  MatDividerModule,
  MatExpansionModule,
  MatGridListModule,
  MatIconModule,
  MatInputModule,
  MatMenuModule,
  MatPaginatorModule,
  MatProgressBarModule,
  MatProgressSpinnerModule,
  MatRadioModule,
  MatSelectModule,
  MatSidenavModule,
  MatSliderModule,
  MatSlideToggleModule,
  MatSnackBarModule,
  MatSortModule,
  MatStepperModule,
  MatTableModule,
  MatTabsModule,
  MatToolbarModule,
  MatTooltipModule
} from '@angular/material';
import { MatDatetimepickerModule, MatNativeDatetimeModule } from '@mat-datetimepicker/core';
import { GridsterModule } from 'angular-gridster2';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ShareModule as ShareButtonsModule } from '@ngx-share/core';
import { HotkeyModule } from 'angular2-hotkeys';
import { ColorPickerModule } from 'ngx-color-picker';
import { UserMenuComponent } from '@shared/components/user-menu.component';
import { NospacePipe } from './pipe/nospace.pipe';
import { TranslateModule } from '@ngx-translate/core';
import { TbCheckboxComponent } from '@shared/components/tb-checkbox.component';
import { HelpComponent } from '@shared/components/help.component';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { TimewindowComponent } from '@shared/components/time/timewindow.component';
import { OverlayModule } from '@angular/cdk/overlay';
import { TimewindowPanelComponent } from '@shared/components/time/timewindow-panel.component';
import { TimeintervalComponent } from '@shared/components/time/timeinterval.component';
import { DatetimePeriodComponent } from '@shared/components/time/datetime-period.component';
import { EnumToArrayPipe } from '@shared/pipe/enum-to-array.pipe';
import { ClipboardModule } from 'ngx-clipboard';
import { ValueInputComponent } from '@shared/components/value-input.component';
import { FullscreenDirective } from '@shared/components/fullscreen.directive';
import { HighlightPipe } from '@shared/pipe/highlight.pipe';
import { DashboardAutocompleteComponent } from '@shared/components/dashboard-autocomplete.component';
import { EntitySubTypeAutocompleteComponent } from '@shared/components/entity/entity-subtype-autocomplete.component';
import { EntitySubTypeSelectComponent } from './components/entity/entity-subtype-select.component';
import { EntityAutocompleteComponent } from './components/entity/entity-autocomplete.component';
import { EntityListComponent } from '@shared/components/entity/entity-list.component';
import { EntityTypeSelectComponent } from './components/entity/entity-type-select.component';
import { EntitySelectComponent } from './components/entity/entity-select.component';
import { DatetimeComponent } from '@shared/components/time/datetime.component';
import { EntityKeysListComponent } from './components/entity/entity-keys-list.component';
import { SocialSharePanelComponent } from './components/socialshare-panel.component';
import { RelationTypeAutocompleteComponent } from '@shared/components/relation/relation-type-autocomplete.component';
import { EntityListSelectComponent } from './components/entity/entity-list-select.component';
import { JsonObjectEditComponent } from './components/json-object-edit.component';
import { FooterFabButtonsComponent } from '@shared/components/footer-fab-buttons.component';
import { CircularProgressDirective } from './components/circular-progress.directive';
import { MatSpinner } from '@angular/material/progress-spinner';
import { FabToolbarComponent, FabActionsDirective, FabTriggerDirective } from './components/fab-toolbar.component';
import { DashboardSelectPanelComponent } from '@shared/components/dashboard-select-panel.component';
import { DashboardSelectComponent } from '@shared/components/dashboard-select.component';
import { WidgetsBundleSelectComponent } from './components/widgets-bundle-select.component';
import { KeyboardShortcutPipe } from './pipe/keyboard-shortcut.pipe';
import { TbErrorComponent } from './components/tb-error.component';
import { EntityTypeListComponent } from './components/entity/entity-type-list.component';
import { EntitySubTypeListComponent } from './components/entity/entity-subtype-list.component';
import { TruncatePipe } from './pipe/truncate.pipe';
import { ColorPickerDialogComponent } from './components/dialog/color-picker-dialog.component';
import { MatChipDraggableDirective } from './components/mat-chip-draggable.directive';
import { ColorInputComponent } from './components/color-input.component';
import { JsFuncComponent } from './components/js-func.component';
import { JsonFormComponent } from './components/json-form/json-form.component';
import { MaterialIconsDialogComponent } from '@shared/components/dialog/material-icons-dialog.component';
import { MaterialIconSelectComponent } from '@shared/components/material-icon-select.component';

@NgModule({
  providers: [
    DatePipe,
    MillisecondsToTimeStringPipe,
    EnumToArrayPipe,
    HighlightPipe,
    TruncatePipe
  ],
  entryComponents: [
    TbSnackBarComponent,
    TbAnchorComponent,
    TimewindowPanelComponent,
    DashboardSelectPanelComponent,
    MatSpinner,
    ColorPickerDialogComponent,
    MaterialIconsDialogComponent
  ],
  declarations: [
    FooterComponent,
    LogoComponent,
    FooterFabButtonsComponent,
    ToastDirective,
    FullscreenDirective,
    CircularProgressDirective,
    MatChipDraggableDirective,
    TbAnchorComponent,
    HelpComponent,
    TbCheckboxComponent,
    TbSnackBarComponent,
    TbErrorComponent,
    BreadcrumbComponent,
    UserMenuComponent,
    TimewindowComponent,
    TimewindowPanelComponent,
    TimeintervalComponent,
    DashboardSelectComponent,
    DashboardSelectPanelComponent,
    DatetimePeriodComponent,
    DatetimeComponent,
    ValueInputComponent,
    DashboardAutocompleteComponent,
    EntitySubTypeAutocompleteComponent,
    EntitySubTypeSelectComponent,
    EntitySubTypeListComponent,
    EntityAutocompleteComponent,
    EntityListComponent,
    EntityTypeSelectComponent,
    EntitySelectComponent,
    EntityKeysListComponent,
    EntityListSelectComponent,
    EntityTypeListComponent,
    RelationTypeAutocompleteComponent,
    SocialSharePanelComponent,
    JsonObjectEditComponent,
    JsFuncComponent,
    FabTriggerDirective,
    FabActionsDirective,
    FabToolbarComponent,
    WidgetsBundleSelectComponent,
    ColorPickerDialogComponent,
    MaterialIconsDialogComponent,
    ColorInputComponent,
    MaterialIconSelectComponent,
    JsonFormComponent,
    NospacePipe,
    MillisecondsToTimeStringPipe,
    EnumToArrayPipe,
    HighlightPipe,
    TruncatePipe,
    KeyboardShortcutPipe
  ],
  imports: [
    CommonModule,
    RouterModule,
    TranslateModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule,
    MatInputModule,
    MatSnackBarModule,
    MatSidenavModule,
    MatToolbarModule,
    MatMenuModule,
    MatGridListModule,
    MatDialogModule,
    MatSelectModule,
    MatTooltipModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTabsModule,
    MatRadioModule,
    MatSlideToggleModule,
    MatDatepickerModule,
    MatNativeDatetimeModule,
    MatDatetimepickerModule,
    MatSliderModule,
    MatExpansionModule,
    MatStepperModule,
    MatAutocompleteModule,
    MatChipsModule,
    GridsterModule,
    ClipboardModule,
    FlexLayoutModule.withConfig({addFlexToParent: false}),
    FormsModule,
    ReactiveFormsModule,
    OverlayModule,
    ShareButtonsModule,
    HotkeyModule,
    ColorPickerModule
  ],
  exports: [
    FooterComponent,
    LogoComponent,
    FooterFabButtonsComponent,
    ToastDirective,
    FullscreenDirective,
    CircularProgressDirective,
    MatChipDraggableDirective,
    TbAnchorComponent,
    HelpComponent,
    TbCheckboxComponent,
    TbErrorComponent,
    BreadcrumbComponent,
    UserMenuComponent,
    TimewindowComponent,
    TimewindowPanelComponent,
    TimeintervalComponent,
    DashboardSelectComponent,
    DatetimePeriodComponent,
    DatetimeComponent,
    DashboardAutocompleteComponent,
    EntitySubTypeAutocompleteComponent,
    EntitySubTypeSelectComponent,
    EntitySubTypeListComponent,
    EntityAutocompleteComponent,
    EntityListComponent,
    EntityTypeSelectComponent,
    EntitySelectComponent,
    EntityKeysListComponent,
    EntityListSelectComponent,
    EntityTypeListComponent,
    RelationTypeAutocompleteComponent,
    SocialSharePanelComponent,
    JsonObjectEditComponent,
    JsFuncComponent,
    FabTriggerDirective,
    FabActionsDirective,
    FabToolbarComponent,
    WidgetsBundleSelectComponent,
    ValueInputComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule,
    MatInputModule,
    MatSnackBarModule,
    MatSidenavModule,
    MatToolbarModule,
    MatMenuModule,
    MatGridListModule,
    MatDialogModule,
    MatSelectModule,
    MatTooltipModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTabsModule,
    MatRadioModule,
    MatSlideToggleModule,
    MatDatepickerModule,
    MatNativeDatetimeModule,
    MatDatetimepickerModule,
    MatSliderModule,
    MatExpansionModule,
    MatStepperModule,
    MatAutocompleteModule,
    MatChipsModule,
    GridsterModule,
    ClipboardModule,
    FlexLayoutModule,
    FormsModule,
    ReactiveFormsModule,
    OverlayModule,
    ShareButtonsModule,
    HotkeyModule,
    ColorPickerModule,
    ColorPickerDialogComponent,
    MaterialIconsDialogComponent,
    ColorInputComponent,
    MaterialIconSelectComponent,
    JsonFormComponent,
    NospacePipe,
    MillisecondsToTimeStringPipe,
    EnumToArrayPipe,
    HighlightPipe,
    TruncatePipe,
    KeyboardShortcutPipe,
    TranslateModule
  ]
})
export class SharedModule { }
