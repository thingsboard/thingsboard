///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, DestroyRef, inject, Inject, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import {
  CalculatedFieldArgument,
  CalculatedFieldConfiguration,
  CalculatedFieldInfo,
  CalculatedFieldType
} from '@shared/models/calculated-field.models';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { Observable } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import {
  CalculatedFieldsTableConfig,
  CalculatedFieldsTableEntity
} from '@home/components/calculated-fields/calculated-fields-table-config';
import { TenantId } from '@shared/models/id/tenant-id';
import { StringItemsOption } from '@shared/components/string-items-list.component';
import { RelationTypes } from '@shared/models/relation.models';
import {
  AlarmRule,
  AlarmRuleConditionType,
  alarmRuleEntityTypeList,
  AlarmRuleExpressionType
} from '@shared/models/alarm-rule.models';
import { CalculatedFieldFormService } from '@core/services/calculated-field-form.service';
import { AssetInfo } from '@shared/models/asset.models';
import { DeviceInfo } from '@shared/models/device.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityService } from '@core/http/entity.service';

@Component({
  selector: 'tb-alarm-rules',
  templateUrl: './alarm-rules.component.html',
  styleUrls: ['./alarm-rule-dialog.component.scss']
})
export class AlarmRulesComponent extends EntityComponent<CalculatedFieldsTableEntity> {

  @Input()
  standalone = false;

  @Input()
  entityName: string;

  ownerId = new TenantId(getCurrentAuthUser(this.store).tenantId);
  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  readonly EntityType = EntityType;
  readonly alarmRuleEntityTypeList = alarmRuleEntityTypeList;
  readonly CalculatedFieldType = CalculatedFieldType;

  private cfFormService = inject(CalculatedFieldFormService);
  private destroyRef = inject(DestroyRef);

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: CalculatedFieldInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: CalculatedFieldsTableConfig,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef,
              private entityService: EntityService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  additionalDebugActionConfig = {
    ...this.entitiesTableConfig.additionalDebugActionConfig,
    action: () => this.entitiesTableConfig.additionalDebugActionConfig.action(
      { id: this.entity.id, ...this.entityFormValue() }, false,
      (expression) => {
        if (expression) {
          this.entityForm.get('configuration').setValue({...this.entityFormValue().configuration, expression});
          this.entityForm.get('configuration').markAsDirty();
        }
      }),
  };

  get entityId(): EntityId {
    return this.entityForm.get('entityId').value;
  }

  get entitiesTableConfig(): CalculatedFieldsTableConfig {
    return this.entitiesTableConfigValue;
  }

  changeEntity(entity: BaseData<EntityId>): void {
    this.entityName = entity?.name;
  }

  buildForm(_entity?: CalculatedFieldInfo): FormGroup {
    return inject(CalculatedFieldFormService).buildAlarmRuleForm();
  }

  updateForm(entity: CalculatedFieldInfo) {
    const { configuration = {} as CalculatedFieldConfiguration, type = CalculatedFieldType.ALARM, debugSettings = { failuresEnabled: true, allEnabled: true }, entityId = this.entityId, ...value } = entity ?? {};
    this.entityForm.patchValue({ configuration, debugSettings, entityId, ...value }, {emitEvent: false});
    if (!entityId) {
      this.entityForm.get('configuration').disable({emitEvent: false});
    }
  }

  onTestScript(expression?: string): Observable<string> {
    return this.cfFormService.testScript(
      this.entity?.id?.id,
      this.entityFormValue(),
      this.entitiesTableConfig.getTestScriptDialog.bind(this.entitiesTableConfig),
      this.destroyRef,
      expression
    );
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        this.entityForm.get('entityId').disable({emitEvent: false});
        this.getOwnerId(this.entityId);
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  get arguments(): Record<string, CalculatedFieldArgument> {
    return this.entityForm.get('configuration.arguments').value;
  }

  get predefinedTypeValues(): StringItemsOption[] {
    return RelationTypes.map(type => ({
      name: type,
      value: type
    }));
  }

  get configFormGroup(): FormGroup {
    return this.entityForm.get('configuration') as FormGroup;
  }

  public removeClearAlarmRule() {
    this.configFormGroup.patchValue({clearRule: null});
    this.entityForm.markAsDirty();
  }

  public addClearAlarmRule() {
    const clearAlarmRule: AlarmRule = {
      condition: {
        type: AlarmRuleConditionType.SIMPLE,
        expression: {
          type: AlarmRuleExpressionType.SIMPLE
        }
      },
      alarmDetails: null
    };
    this.configFormGroup.patchValue({clearRule: clearAlarmRule});
  }

  getOwnerId(entityId: EntityId) {
    if (entityId?.entityType === EntityType.DEVICE || entityId?.entityType === EntityType.ASSET) {
      this.entityService.getEntity(entityId.entityType, entityId.id, { ignoreLoading: true, ignoreErrors: true }).subscribe(
        (entity: AssetInfo | DeviceInfo) => {
          if (this.isAssignedToCustomer(entity)) {
            this.ownerId = entity.customerId;
          }
        }
      );
    }
  }

  private isAssignedToCustomer(entity: AssetInfo | DeviceInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

}
