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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import { EntityType } from '@shared/models/entity-type.models';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { ServiceType } from '@shared/models/queue.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { AssetProfile } from '@shared/models/asset.models';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Component({
    selector: 'tb-asset-profile',
    templateUrl: './asset-profile.component.html',
    styleUrls: [],
    standalone: false
})
export class AssetProfileComponent extends EntityComponent<AssetProfile> {

  @Input()
  standalone = false;

  entityType = EntityType;

  serviceType = ServiceType.TB_RULE_ENGINE;

  edgeRuleChainType = RuleChainType.EDGE;

  assetProfileId: EntityId;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: AssetProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AssetProfile>,
              protected fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: AssetProfile): UntypedFormGroup {
    this.assetProfileId = entity?.id ? entity.id : null;
    const form = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : null],
        defaultRuleChainId: [entity && entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null, []],
        defaultDashboardId: [entity && entity.defaultDashboardId ? entity.defaultDashboardId.id : null, []],
        defaultQueueName: [entity ? entity.defaultQueueName : null, []],
        defaultEdgeRuleChainId: [entity && entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null, []],
        description: [entity ? entity.description : '', []],
      }
    );
    return form;
  }

  updateForm(entity: AssetProfile) {
    this.assetProfileId = entity.id;
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({image: entity.image}, {emitEvent: false});
    this.entityForm.patchValue({defaultRuleChainId: entity.defaultRuleChainId ? entity.defaultRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultDashboardId: entity.defaultDashboardId ? entity.defaultDashboardId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({defaultQueueName: entity.defaultQueueName}, {emitEvent: false});
    this.entityForm.patchValue({defaultEdgeRuleChainId: entity.defaultEdgeRuleChainId ? entity.defaultEdgeRuleChainId.id : null}, {emitEvent: false});
    this.entityForm.patchValue({description: entity.description}, {emitEvent: false});
  }

  prepareFormValue(formValue: any): any {
    if (formValue.defaultRuleChainId) {
      formValue.defaultRuleChainId = new RuleChainId(formValue.defaultRuleChainId);
    }
    if (formValue.defaultDashboardId) {
      formValue.defaultDashboardId = new DashboardId(formValue.defaultDashboardId);
    }
    if (formValue.defaultEdgeRuleChainId) {
      formValue.defaultEdgeRuleChainId = new RuleChainId(formValue.defaultEdgeRuleChainId);
    }
    return super.prepareFormValue(formValue);
  }

  onAssetProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('asset-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
