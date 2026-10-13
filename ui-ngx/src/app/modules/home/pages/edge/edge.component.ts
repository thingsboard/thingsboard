// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { EdgeInfo } from '@shared/models/edge.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid } from '@core/utils';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import {EdgeService} from "@core/http/edge.service";
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AgentService } from '@core/http/agent.service';
import { AgentApplication, AgentAppConfigType, AgentAppEvent } from '@shared/models/agent.models';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult
} from '@home/components/agent/wizard/agent-app-install-wizard.component';

@Component({
    selector: 'tb-edge',
    templateUrl: './edge.component.html',
    styleUrls: ['./edge.component.scss'],
    standalone: false
})
export class EdgeComponent extends GroupEntityComponent<EdgeInfo> {

  entityType = EntityType;

  // edgeScope: 'tenant' | 'customer' | 'customer_user';
  upgradeAvailable: boolean = false;

  managedApp: AgentApplication | null = null;
  managedAppUpgradeAvailable = false;

  licenseVersion: number;
  legacyLicenseFields: boolean;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private edgeService: EdgeService,
              private agentService: AgentService,
              private router: Router,
              private dialog: MatDialog,
              @Inject('entity') protected entityValue: EdgeInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<EdgeInfo> | GroupEntityTableConfig<EdgeInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  ngOnInit() {
    // this.edgeScope = this.entitiesTableConfig.componentsData.edgeScope;
    if (this.entityForm.get('cloudEndpoint')) {
      this.entityForm.patchValue({
        cloudEndpoint: window.location.origin
      });
    }
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageUsers() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageUsersEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageAssets() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageAssetsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDevices() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDevicesEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageEntityViews() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageEntityViewsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageDashboards() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageDashboardsEnabled(this.entity);
    } else {
      return false;
    }
  }

  hideManageSchedulerEvents() {
    if (this.isGroupMode()) {
      return !this.groupEntitiesTableConfig.manageSchedulerEventsEnabled(this.entity);
    } else {
      return false;
    }
  }

  /* isAssignedToCustomer(entity: EdgeInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  } */

  buildForm(entity: EdgeInfo): UntypedFormGroup {
    this.licenseVersion = getCurrentAuthState(this.store).licenseVersion;
    this.legacyLicenseFields = this.licenseVersion < 2;

    const form = this.fb.group({
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
    });
    if (this.legacyLicenseFields) {
      form.addControl('cloudEndpoint', this.fb.control(null, [Validators.required, Validators.maxLength(255)]));
      form.addControl('edgeLicenseKey', this.fb.control('', [Validators.required]));
    }
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: EdgeInfo) {
    const patch: any = {
      name: entity.name,
      type: entity.type,
      label: entity.label,
      routingKey: entity.routingKey,
      secret: entity.secret,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    };
    if (this.legacyLicenseFields) {
      patch.cloudEndpoint = entity.cloudEndpoint ? entity.cloudEndpoint : window.location.origin;
      patch.edgeLicenseKey = entity.edgeLicenseKey;
    }
    this.entityForm.patchValue(patch);
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
    if (this.isTenantAdmin()) {
      this.edgeService.isEdgeUpgradeAvailable(this.entity.id.id)
        .subscribe(isUpgradeAvailable => {
          this.upgradeAvailable = isUpgradeAvailable;
        });
      if (this.entity?.id?.id) {
        this.loadManagedApp(this.entity.id.id);
      }
    }
  }

  private loadManagedApp(edgeId: string) {
    this.managedApp = null;
    this.managedAppUpgradeAvailable = false;
    this.agentService.getAgentApplicationByRelatedEntity(EntityType.EDGE, edgeId,
      { ignoreErrors: true, ignoreLoading: true }).subscribe({
      next: app => {
        this.managedApp = app || null;
        if (app?.templateVersion) {
          this.agentService.getAgentAppTemplateByVersion(app.appType, AgentAppConfigType.DOCKER_COMPOSE, app.templateVersion, { ignoreLoading: true })
            .subscribe(tpl => {
              this.managedAppUpgradeAvailable = !!tpl?.nextVersion;
              this.cd.markForCheck();
            });
        }
        this.cd.markForCheck();
      },
      error: () => {
        this.managedApp = null;
        this.managedAppUpgradeAvailable = false;
      }
    });
  }

  openManagedApp($event: Event) {
    if ($event) { $event.stopPropagation(); }
    if (!this.managedApp) { return; }
    const agentId = this.managedApp.agentId?.id;
    this.router.navigateByUrl(`/edgeManagement/agents/all/${agentId}/applications/${this.managedApp.id.id}`);
  }

  onUpgradeManagedApp($event: Event) {
    if ($event) { $event.stopPropagation(); }
    if (!this.managedApp) { return; }
    this.agentService.getAgentApplicationInfoById(this.managedApp.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
          data: {
            agentId: full.agentId?.id,
            agent: null,
            mode: 'upgrade',
            application: full
          }
        }).afterClosed().subscribe(event => {
          if (event && this.entity?.id?.id) {
            this.loadManagedApp(this.entity.id.id);
          }
        });
    });
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({ emitEvent: false });
    this.entityForm.get('secret').disable({ emitEvent: false });
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

  isTenantAdmin(): boolean {
    const authUser: AuthUser = getCurrentAuthUser(this.store);
    return authUser?.authority === Authority.TENANT_ADMIN;
  }

  private generateRoutingKeyAndSecret(entity: EdgeInfo, form: UntypedFormGroup) {
    if (entity && (!entity.id || (entity.id && !entity.id.id))) {
      if (this.legacyLicenseFields) {
        form.get('edgeLicenseKey').patchValue('6qcGys6gz4M2ZuIqZ6hRDjWT', { emitEvent: false });
      }
      form.get('routingKey').patchValue(guid(), { emitEvent: false });
      form.get('secret').patchValue(generateSecret(20), { emitEvent: false });
    }
  }
}
