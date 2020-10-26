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

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from "@core/core.state";
import { EntityComponent } from "@home/components/entity/entity.component";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { EntityType } from "@shared/models/entity-type.models";
import { EdgeInfo } from "@shared/models/edge.models";
import { TranslateService } from "@ngx-translate/core";
import { NULL_UUID } from "@shared/models/id/has-uuid";
import { ActionNotificationShow } from "@core/notification/notification.actions";
import { guid, isUndefined } from "@core/utils";
import { EntityTableConfig } from "@home/models/entity/entities-table-config.models";
import { WINDOW } from "@core/services/window.service";

@Component({
  selector: 'tb-edge',
  templateUrl: './edge.component.html',
  styleUrls: ['./edge.component.scss']
})
export class EdgeComponent extends EntityComponent<EdgeInfo> {

  entityType = EntityType;

  edgeScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: EdgeInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EdgeInfo>,
              public fb: FormBuilder,
              @Inject(WINDOW) protected window: Window) {
    super(store, fb, entityValue, entitiesTableConfigValue, window);
  }

  ngOnInit() {
    this.edgeScope = this.entitiesTableConfig.componentsData.edgeScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: EdgeInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: EdgeInfo): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        label: [entity ? entity.label : ''],
        cloudEndpoint: [this.window.location.origin, [Validators.required]],
        edgeLicenseKey: ['', [Validators.required]],
        routingKey: guid(),
        secret: this.generateSecret(20),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
  }

  updateForm(entity: EdgeInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({cloudEndpoint: entity.cloudEndpoint});
    this.entityForm.patchValue({edgeLicenseKey: entity.edgeLicenseKey});
    this.entityForm.patchValue({routingKey: entity.routingKey});
    this.entityForm.patchValue({secret: entity.secret});
    this.entityForm.patchValue({additionalInfo: {
      description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }

  onEdgeIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.id-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  onEdgeKeyCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.edge-key-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  onEdgeSecretCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.edge-secret-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
  }

  generateSecret(length): string {
    if (isUndefined(length) || length == null) {
      length = 1;
    }
    var l = length > 10 ? 10 : length;
    var str =  Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }
}
