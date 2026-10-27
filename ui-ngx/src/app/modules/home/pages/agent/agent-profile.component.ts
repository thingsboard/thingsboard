// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentProfileInfo,
  AgentProvisionType,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeSupportsAppAutoInstall,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentService } from '@core/http/agent.service';

@Component({
  selector: 'tb-agent-profile',
  templateUrl: './agent-profile.component.html',
  styleUrls: ['./agent-profile.component.scss'],
  standalone: false
})
export class AgentProfileComponent extends EntityComponent<AgentProfileInfo> {

  @Input()
  standalone = false;

  entityType = EntityType;
  agentProvisionTypes = Object.values(AgentProvisionType);
  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;
  readonly AgentProvisionType = AgentProvisionType;

  dockerCommand = '';

  get autoInstallEnabled(): boolean {
    return agentProvisionTypeSupportsAppAutoInstall(this.entity?.provisionType);
  }

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Optional() @Inject('entity') protected entityValue: AgentProfileInfo,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentProfileInfo>,
              public fb: UntypedFormBuilder,
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

  buildForm(entity: AgentProfileInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      provisionType: [entity?.provisionType || AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE],
    });
  }

  updateForm(entity: AgentProfileInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description,
      provisionType: entity.provisionType || AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE,
    });
    if (entity?.id?.id) {
      this.agentService.getAgentProvisionInstructions(entity.id.id).subscribe({
        next: res => { this.dockerCommand = res?.instructions || ''; },
        error: () => { this.dockerCommand = ''; }
      });
    } else {
      this.dockerCommand = '';
    }
  }

  onProfileIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.profile-id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  onProvisioningCopied(): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.install-command-copied-message'),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

}
