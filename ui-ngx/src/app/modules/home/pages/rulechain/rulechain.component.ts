// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { RuleChain } from '@shared/models/rule-chain.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
    selector: 'tb-rulechain',
    templateUrl: './rulechain.component.html',
    styleUrls: ['./rulechain.component.scss'],
    standalone: false
})
export class RuleChainComponent extends EntityComponent<RuleChain> {

  ruleChainScope: 'tenant' | 'edges' | 'edge';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: RuleChain,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<RuleChain>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.ruleChainScope = this.entitiesTableConfig.componentsData.ruleChainScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: RuleChain): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        debugMode: [entity ? entity.debugMode : false],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: RuleChain) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({debugMode: entity.debugMode});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onRuleChainIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('rulechain.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  isEdgeRootRuleChain() {
    if (this.entitiesTableConfig && this.entityValue) {
      return this.entitiesTableConfig.componentsData.edge?.rootRuleChainId?.id == this.entityValue.id.id;
    } else {
      return false;
    }
  }

  isAutoAssignToEdgeRuleChain() {
    if (this.entitiesTableConfig && this.entityValue) {
      return !this.entityValue.root &&
        this.entitiesTableConfig.componentsData?.autoAssignToEdgeRuleChainIds?.includes(this.entityValue.id.id);
    } else {
      return false;
    }
  }

  isNotAutoAssignToEdgeRuleChain() {
    if (this.entitiesTableConfig && this.entityValue) {
      return !this.entityValue.root &&
        !this.entitiesTableConfig.componentsData?.autoAssignToEdgeRuleChainIds?.includes(this.entityValue.id.id);
    } else {
      return false;
    }
  }
}
