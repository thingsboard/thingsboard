///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { AfterViewInit, Component, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import { EntityType } from '@shared/models/entity-type.models';
import { MatDrawer } from '@angular/material/sidenav';
import { NotificationService } from '@core/http/notification.service';
import {
  NotificationDeliveryMethod,
  NotificationDeliveryMethodTranslateMap,
  NotificationTemplateTypeTranslateMap
} from '@shared/models/notification.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { deepTrim } from '@core/utils';

@Component({
  selector: 'tb-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['notification-center.component.scss']
})
export class NotificationCenterComponent extends PageComponent implements AfterViewInit {

  entityType = EntityType;

  notificationSettingsForm: FormGroup;
  notificationDeliveryMethods = Object.keys(NotificationDeliveryMethod) as NotificationDeliveryMethod[];
  notificationDeliveryMethodTranslateMap = NotificationDeliveryMethodTranslateMap;

  @ViewChild('notificationSettings', {static: true}) notificationSettings!: MatDrawer;
  @ViewChild('matTabGroup', {static: true}) matTabs: MatTabGroup;
  @ViewChild('requestTab', {static: true}) requestTab: MatTab;
  @ViewChild('notificationRequest', {static: true}) notificationRequestTable: NotificationTableComponent;
  @ViewChildren(NotificationTableComponent) tableComponent: QueryList<NotificationTableComponent>;


  constructor(
    protected store: Store<AppState>,
    private notificationService: NotificationService,
    private fb: FormBuilder) {
    super(store);
    this.notificationSettingsForm = this.fb.group({
      deliveryMethodsConfigs:  this.fb.group({})
    });
    this.notificationDeliveryMethods.forEach(method => {
      (this.notificationSettingsForm.get('deliveryMethodsConfigs') as FormGroup)
        .addControl(method, this.fb.group({method, enabled: method !== NotificationDeliveryMethod.SLACK}), {emitEvent: false});
    });
    (this.notificationSettingsForm.get('deliveryMethodsConfigs.SLACK') as FormGroup)
      .addControl('botToken', this.fb.control({value: '', disabled: true}, Validators.required), {emitEvent: false});
    this.notificationSettingsForm.get('deliveryMethodsConfigs.SLACK.enabled').valueChanges.subscribe(value => {
      if (value) {
        this.notificationSettingsForm.get('deliveryMethodsConfigs.SLACK.botToken').enable({emitEvent: true});
      } else {
        this.notificationSettingsForm.get('deliveryMethodsConfigs.SLACK.botToken').disable({emitEvent: true});

      }
    });
  }

  ngAfterViewInit() {
    this.notificationSettings.openedStart.subscribe(() => {
      this.notificationService.getNotificationSettings().subscribe(
        value => {
          this.notificationSettingsForm.patchValue(value, {emitEvent: false});
          this.notificationSettingsForm.get('deliveryMethodsConfigs.SLACK.enabled').updateValueAndValidity({onlySelf: true});
        }
      );
    });
  }

  updateData() {
    this.currentTableComponent?.updateData();
  }

  openSetting() {

  }

  private get currentTableComponent(): NotificationTableComponent {
    return this.tableComponent.get(this.matTabs.selectedIndex);
  }

  sendNotification($event: Event) {
    this.notificationRequestTable.entityTableConfig.onEntityAction({event: $event, action: this.requestTab.isActive ? 'add' : 'add-without-update', entity: null});
  }

  toggleNotificationSettings() {
    this.notificationSettings.toggle().then(() => {});
  }

  saveNotificationSettigs($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const formValue = deepTrim(this.notificationSettingsForm.getRawValue());
    this.notificationService.saveNotificationSettings(formValue).subscribe(value => {
      this.notificationSettingsForm.patchValue(value, {emitEvent: false});
      this.notificationSettingsForm.markAsPristine();
    });
  }
}
