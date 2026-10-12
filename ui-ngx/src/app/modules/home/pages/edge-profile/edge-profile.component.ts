// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EdgeProfile } from '@shared/models/edge-profile.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Operation, Resource } from '@shared/models/security.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Component({
  selector: 'tb-edge-profile',
  templateUrl: './edge-profile.component.html',
  styleUrls: [],
  standalone: false
})
export class EdgeProfileComponent extends EntityComponent<EdgeProfile> {

  @Input()
  standalone = false;

  // owned by the caller, because the root rule chain is a separate server side assignment and not part of the entity payload
  @Input()
  rootRuleChainControl: UntypedFormControl;

  edgeRuleChainType = RuleChainType.EDGE;

  resource = Resource;
  operation = Operation;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: EdgeProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<EdgeProfile>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  // set-default and re-apply are tenant-admin only on the server, whatever generic permissions a customer user holds
  isTenantAdmin(): boolean {
    return getCurrentAuthUser(this.store)?.authority === Authority.TENANT_ADMIN;
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: EdgeProfile): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : '']
    });
  }

  updateForm(entity: EdgeProfile) {
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description
    });
  }

  onEdgeProfileIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('edge-profile.id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }
}
