// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { selectAuthUser } from '@core/auth/auth.selectors';
import { catchError, map, mergeMap, switchMap, take } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { MatDialog } from '@angular/material/dialog';
import { AgentProfileInfo, AgentProvisionType, agentProvisionTypeTranslationMap } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentProfileComponent } from '@home/pages/agent/agent-profile.component';
import { AgentProfileTabsComponent } from '@home/pages/agent/agent-profile-tabs.component';
import {
  AgentProfileCreatedDialogComponent,
  AgentProfileCreatedDialogData
} from '@home/pages/agent/agent-profile-created-dialog.component';
import { AgentProfileCreateDialogService } from '@home/pages/agent/agent-profile-create-dialog.service';
import { DialogService } from '@core/services/dialog.service';
import { of } from 'rxjs';

@Injectable()
export class AgentProfilesTableConfigResolver {

  private readonly config: EntityTableConfig<AgentProfileInfo> = new EntityTableConfig<AgentProfileInfo>();

  constructor(private store: Store<AppState>,
              private agentService: AgentService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private agentProfileCreateDialogService: AgentProfileCreateDialogService) {

    this.config.entityType = EntityType.AGENT_PROFILE;
    this.config.entityComponent = AgentProfileComponent;
    this.config.entityTabsComponent = AgentProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AGENT_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.AGENT_PROFILE);
    this.config.rowPointer = true;

    this.config.deleteEntityTitle = profile => this.translate.instant('agent.delete-profile-title', {profileName: profile.name});
    this.config.deleteEntityContent = () => this.translate.instant('agent.delete-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('agent.delete-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('agent.delete-profiles-text');

    this.config.loadEntity = id => this.agentService.getAgentProfileInfoById(id.id);
    this.config.saveEntity = profile => {
      return this.agentService.saveAgentProfile(profile).pipe(
        mergeMap((saved) => this.agentService.getAgentProfileInfoById(saved.id.id))
      );
    };
    this.config.onEntityAction = action => this.onProfileAction(action);
    this.config.entityAdded = (profile) => {
      if (profile.provisionType && profile.provisionType !== AgentProvisionType.DISABLED) {
        this.openProfileCreatedInstructions(profile);
      }
    };

    this.config.cellActionDescriptors.push({
      name: this.translate.instant('agent.set-default-profile'),
      icon: 'flag',
      isEnabled: (profile) => !profile.default,
      onAction: ($event, entity) => this.setDefaultAgentProfile($event, entity)
    });

    this.config.addEntity = () => this.openAddWizard();
  }

  private openAddWizard() {
    return this.agentProfileCreateDialogService.open({}).pipe(
      switchMap(saved => saved ? this.agentService.getAgentProfileInfoById(saved.id.id) : of(null))
    );
  }

  private openProfileCreatedInstructions(agentProfile: AgentProfileInfo) {
    this.agentService.getAgentProvisionInstructions(agentProfile.id.id).pipe(
      catchError(() => of({ instructions: '' }))
    ).subscribe(res => {
      this.dialog.open<AgentProfileCreatedDialogComponent, AgentProfileCreatedDialogData>(
        AgentProfileCreatedDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: { agentProfile, dockerCommand: res?.instructions || '' }
        });
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<AgentProfileInfo>> {
    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map((authUser) => {
        this.config.tableTitle = this.translate.instant('agent.agent-profiles');
        this.config.columns = this.configureColumns();
        this.config.entitiesFetchFunction = pageLink =>
          this.agentService.getTenantAgentProfileInfos(pageLink);
        this.config.deleteEntity = id => this.agentService.deleteAgentProfile(id.id);
        this.config.addEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.entitiesDeleteEnabled = authUser.authority === Authority.TENANT_ADMIN;
        this.config.deleteEnabled = (profile) => authUser.authority === Authority.TENANT_ADMIN && profile && !profile.default;
        this.config.entitySelectionEnabled = (profile) => profile && !profile.default;
        this.config.detailsReadonly = () => authUser.authority !== Authority.TENANT_ADMIN;
        return this.config;
      })
    );
  }

  configureColumns(): Array<EntityTableColumn<AgentProfileInfo>> {
    return [
      new DateEntityTableColumn<AgentProfileInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AgentProfileInfo>('name', 'agent.profile-name', '40%'),
      new EntityTableColumn<AgentProfileInfo>('provisionType', 'agent.provisioning-strategy', '40%',
        entity => this.provisionTypeLabel(entity.provisionType), () => ({}), false),
      new EntityTableColumn<AgentProfileInfo>('isDefault', 'agent.default-profile', '60px',
        entity => checkBoxCell(entity.default))
    ];
  }

  private setDefaultAgentProfile($event: Event, agentProfile: AgentProfileInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('agent.set-default-profile-title', {profileName: agentProfile.name}),
      this.translate.instant('agent.set-default-profile-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
      if (res) {
        this.agentService.setDefaultAgentProfile(agentProfile.id.id).subscribe(
          () => this.config.updateData()
        );
      }
    });
  }

  private provisionTypeLabel(type: AgentProvisionType | undefined): string {
    const key = agentProvisionTypeTranslationMap.get(type ?? AgentProvisionType.DISABLED);
    return key ? this.translate.instant(key) : '';
  }

  onProfileAction(action: EntityAction<AgentProfileInfo>): boolean {
    switch (action.action) {
      case 'open':
        this.openAgentProfile(action.event, action.entity);
        return true;
      case 'setDefault':
        this.setDefaultAgentProfile(action.event, action.entity);
        return true;
    }
    return false;
  }

  private openAgentProfile($event: Event, agentProfile: AgentProfileInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([agentProfile.id.id], {relativeTo: this.config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }
}
