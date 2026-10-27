// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  Inject,
  NgZone,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { AgentInfo } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { AgentService } from '@core/http/agent.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AttributeScope, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { MatDialog } from '@angular/material/dialog';
import {
  AgentUpgradeDialogComponent,
  AgentUpgradeDialogData
} from '@home/pages/agent/dialog/agent-upgrade-dialog.component';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';

@Component({
  selector: 'tb-agent',
  templateUrl: './agent.component.html',
  styleUrls: ['./agent.component.scss'],
  standalone: false
})
export class AgentComponent extends GroupEntityComponent<AgentInfo> implements OnInit, OnDestroy {

  entityType = EntityType;
  agentScope: 'tenant' | 'customer' | 'customer_user';

  agentOnline = false;
  upgradeAvailable = false;
  upgradeTargetImageRef = '';

  @ViewChild('agentHeaderStatus', { static: true }) headerExtensionTemplate: TemplateRef<unknown>;

  private activeSub: TelemetrySubscriber | null = null;
  private subscribedAgentId: string | null = null;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private agentService: AgentService,
              private router: Router,
              @Inject('entity') protected entityValue: AgentInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<AgentInfo> | GroupEntityTableConfig<AgentInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private dialog: MatDialog) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  ngOnInit() {
    this.agentScope = this.entitiesTableConfig.componentsData?.agentScope;
    super.ngOnInit();
    this.maybeSubscribeAgentActive();
    this.refreshUpgradeAvailable();
  }

  ngOnDestroy() {
    this.tearDownActiveSub();
    super.ngOnDestroy();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: AgentInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: AgentInfo): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      routingKey: this.fb.control({value: entity ? entity.routingKey : null, disabled: true}),
      secret: this.fb.control({value: entity ? entity.secret : null, disabled: true}),
      agentProfileId: [entity?.agentProfileId || null, [Validators.required]],
      description: [entity ? entity.description : '']
    });
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: AgentInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      routingKey: entity.routingKey,
      secret: entity.secret,
      agentProfileId: entity.agentProfileId || null,
      description: entity.description
    });
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
    this.maybeSubscribeAgentActive();
    this.refreshUpgradeAvailable();
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({emitEvent: false});
    this.entityForm.get('secret').disable({emitEvent: false});
  }

  onManageApplications($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.entity?.id?.id) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.entity.id.id, 'applications'));
    }
  }

  /**
   * The decision is server-side: it needs the version graph the update server publishes, and the agent
   * reports an image reference rather than a version, so a floating tag such as 'latest' is
   * deliberately answered 'no upgrade' instead of a guess. It arrives on the entity itself, so every
   * agent view answers this from the same resolved value.
   */
  private refreshUpgradeAvailable() {
    this.upgradeTargetImageRef = this.agentScope === 'customer_user'
      ? '' : (this.entity?.upgradeTargetImageRef || '');
    this.upgradeAvailable = !!this.upgradeTargetImageRef;
  }

  /**
   * After an upgrade the entity in hand still carries the pre-upgrade target, so the resolved value is
   * re-read rather than recomputed here — the agent reports its new version asynchronously, so this is
   * best-effort and simply reflects whatever the server resolves at that moment.
   */
  private reloadUpgradeTarget() {
    const agentId = this.entity?.id?.id;
    if (!agentId || this.agentScope === 'customer_user') {
      return;
    }
    this.agentService.getAgentInfoById(agentId, {ignoreErrors: true, ignoreLoading: true})
      .subscribe(info => {
        this.entity.upgradeTargetImageRef = info?.upgradeTargetImageRef;
        this.refreshUpgradeAvailable();
        this.cd.markForCheck();
      });
  }

  onUpgradeAgent($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.entity?.id?.id) {
      return;
    }
    const data: AgentUpgradeDialogData = {
      agentId: this.entity.id.id,
      agentName: this.entity.name,
      currentImageRef: this.entity.agentVersion,
      suggestedImageRef: this.upgradeTargetImageRef
    };
    this.dialog.open<AgentUpgradeDialogComponent, AgentUpgradeDialogData, any>(
      AgentUpgradeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data
      }
    ).afterClosed().subscribe(event => {
      if (event) {
        openAgentAppEventProgress(this.dialog, null, event).subscribe(() => this.reloadUpgradeTarget());
      }
    });
  }

  onAgentInfoCopied(type: string) {
    const messageMap: Record<string, string> = {
      id: 'agent.id-copied-message',
      key: 'agent.routing-key-copied-message',
      secret: 'agent.secret-copied-message'
    };
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageMap[type]),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  private generateRoutingKeyAndSecret(entity: AgentInfo, form: UntypedFormGroup) {
    if (!entity?.id?.id) {
      form.get('routingKey').patchValue(guid(), {emitEvent: false});
      form.get('secret').patchValue(generateSecret(20), {emitEvent: false});
    }
  }

  private maybeSubscribeAgentActive() {
    const id = this.entity?.id?.id;
    if (!id) {
      this.tearDownActiveSub();
      this.agentOnline = false;
      return;
    }
    if (this.subscribedAgentId === id) {
      return;
    }
    this.tearDownActiveSub();
    this.subscribedAgentId = id;
    this.agentOnline = this.entity.active === true;
    this.activeSub = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      this.entity.id,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['active']
    );
    this.activeSub.data$.subscribe(update => {
      const entries = update?.data?.['active'];
      if (!entries?.length) {
        return;
      }
      const rawValue = entries[0][1];
      const active = rawValue === true || rawValue === 'true';
      this.zone.run(() => {
        this.agentOnline = active;
        this.cd.markForCheck();
      });
    });
    this.activeSub.subscribe();
  }

  private tearDownActiveSub() {
    if (this.activeSub) {
      this.activeSub.unsubscribe();
      this.activeSub.complete();
      this.activeSub = null;
    }
    this.subscribedAgentId = null;
  }
}
