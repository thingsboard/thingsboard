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
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    this.edgeScope = this.entitiesTableConfig.componentsData.edgeScope;
    this.entityForm.patchValue({
      cloudEndpoint:this.window.location.origin
    });
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
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        type: [entity?.type ? entity.type : 'default', [Validators.required]],
        label: [entity ? entity.label : ''],
        cloudEndpoint: [null, [Validators.required]],
        edgeLicenseKey: ['', [Validators.required]],
        routingKey: this.fb.control({ value: entity ? entity.routingKey : null, disabled: true }),
        secret: this.fb.control({ value: entity ? entity.secret : null, disabled: true }),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
    this.checkIsNewEdge(entity, form);
    return form;
  }

  updateForm(entity: EdgeInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type,
      label: entity.label,
      cloudEndpoint: entity.cloudEndpoint ? entity.cloudEndpoint : this.window.location.origin,
      edgeLicenseKey: entity.edgeLicenseKey,
      routingKey: entity.routingKey,
      secret: entity.secret,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
    this.checkIsNewEdge(entity, this.entityForm);
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({ emitEvent: false });
    this.entityForm.get('secret').disable({ emitEvent: false });
  }

  private checkIsNewEdge(entity: EdgeInfo, form: FormGroup) {
    if (entity && !entity.id) {
      form.get('routingKey').patchValue(guid(), { emitEvent: false });
      form.get('secret').patchValue(this.generateSecret(20), { emitEvent: false });
    }
  }

  onEdgeIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('edge.id-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  generateSecret(length): string {
    if (isUndefined(length) || length == null) {
      length = 1;
    }
    var l = length > 10 ? 10 : length;
    var str = Math.random().toString(36).substr(2, l);
    if (str.length >= length) {
      return str;
    }
    return str.concat(this.generateSecret(length - str.length));
  }

  onEdgeInfoCopied(type: string) {
    const message = type === 'key' ? 'edge.edge-key-copied-message'
      : 'edge.edge-secret-copied-message';
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant(message),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
