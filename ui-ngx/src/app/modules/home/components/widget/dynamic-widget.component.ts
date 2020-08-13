///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { PageComponent } from '@shared/components/page.component';
import { Inject, Injector, OnDestroy, OnInit, Directive } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { IDynamicWidgetComponent, WidgetContext } from '@home/models/widget-component.models';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import {
  NotificationHorizontalPosition,
  NotificationType,
  NotificationVerticalPosition
} from '@core/notification/notification.models';
import { FormBuilder, Validators } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';

@Directive()
export class DynamicWidgetComponent extends PageComponent implements IDynamicWidgetComponent, OnInit, OnDestroy {

  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse;

  [key: string]: any;

  validators = Validators;

  constructor(@Inject(RafService) public raf: RafService,
              @Inject(Store) protected store: Store<AppState>,
              @Inject(FormBuilder) public fb: FormBuilder,
              @Inject(Injector) public readonly $injector: Injector,
              @Inject('widgetContext') public readonly ctx: WidgetContext,
              @Inject('errorMessages') public readonly errorMessages: string[]) {
    super(store);
    this.ctx.$injector = $injector;
    this.ctx.deviceService = $injector.get(DeviceService);
    this.ctx.assetService = $injector.get(AssetService);
    this.ctx.entityViewService = $injector.get(EntityViewService);
    this.ctx.customerService = $injector.get(CustomerService);
    this.ctx.dashboardService = $injector.get(DashboardService);
    this.ctx.userService = $injector.get(UserService);
    this.ctx.attributeService = $injector.get(AttributeService);
    this.ctx.entityRelationService = $injector.get(EntityRelationService);
    this.ctx.entityService = $injector.get(EntityService);
    this.ctx.dialogs = $injector.get(DialogService);
    this.ctx.customDialog = $injector.get(CustomDialogService);
    this.ctx.date = $injector.get(DatePipe);
    this.ctx.translate = $injector.get(TranslateService);
    this.ctx.http = $injector.get(HttpClient);
    this.ctx.sanitizer = $injector.get(DomSanitizer);

    this.ctx.$scope = this;
    if (this.ctx.defaultSubscription) {
      this.executingRpcRequest = this.ctx.defaultSubscription.executingRpcRequest;
      this.rpcEnabled = this.ctx.defaultSubscription.rpcEnabled;
      this.rpcErrorText = this.ctx.defaultSubscription.rpcErrorText;
      this.rpcRejection = this.ctx.defaultSubscription.rpcRejection;
    }
  }

  ngOnInit() {

  }

  ngOnDestroy(): void {

  }

  clearRpcError() {
    if (this.widgetContext.defaultSubscription) {
      this.widgetContext.defaultSubscription.clearRpcError();
    }
  }

  showSuccessToast(message: string, duration: number = 1000,
                   verticalPosition: NotificationVerticalPosition = 'bottom',
                   horizontalPosition: NotificationHorizontalPosition = 'left',
                   target?: string) {
    this.ctx.showSuccessToast(message, duration, verticalPosition, horizontalPosition, target);
  }

  showErrorToast(message: string,
                 verticalPosition: NotificationVerticalPosition = 'bottom',
                 horizontalPosition: NotificationHorizontalPosition = 'left',
                 target?: string) {
    this.ctx.showErrorToast(message, verticalPosition, horizontalPosition, target);
  }

  showToast(type: NotificationType, message: string, duration: number,
            verticalPosition: NotificationVerticalPosition = 'bottom',
            horizontalPosition: NotificationHorizontalPosition = 'left',
            target?: string) {
    this.ctx.showToast(type, message, duration, verticalPosition, horizontalPosition, target);
  }

}
