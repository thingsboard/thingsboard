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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { EdgeInfo } from '@shared/models/edge.models';
import { TranslateService } from '@ngx-translate/core';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import {EdgeService} from "@core/http/edge.service";

@Component({
  selector: 'tb-edge',
  templateUrl: './edge.component.html',
  styleUrls: ['./edge.component.scss']
})
export class EdgeComponent extends EntityComponent<EdgeInfo> {

  entityType = EntityType;

  edgeScope: 'tenant' | 'customer' | 'customer_user';
  upgradeAvailable: boolean = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private edgeService: EdgeService,
              @Inject('entity') protected entityValue: EdgeInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EdgeInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
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

  buildForm(entity: EdgeInfo): UntypedFormGroup {
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        type: [entity?.type ? entity.type : 'default', [Validators.required, Validators.maxLength(255)]],
        label: [entity ? entity.label : '', Validators.maxLength(255)],
        routingKey: this.fb.control({value: entity ? entity.routingKey : null, disabled: true}),
        secret: this.fb.control({value: entity ? entity.secret : null, disabled: true}),
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: EdgeInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      type: entity.type,
      label: entity.label,
      routingKey: entity.routingKey,
      secret: entity.secret,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
    this.edgeService.isEdgeUpgradeAvailable(this.entity.id.id)
      .subscribe(isUpgradeAvailable => {
          this.upgradeAvailable = isUpgradeAvailable;
      });
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({emitEvent: false});
    this.entityForm.get('secret').disable({emitEvent: false});
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

  private generateRoutingKeyAndSecret(entity: EdgeInfo, form: UntypedFormGroup) {
    if (entity && !entity.id) {
      form.get('routingKey').patchValue(guid(), {emitEvent: false});
      form.get('secret').patchValue(generateSecret(20), {emitEvent: false});
    }
  }
}
