// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppConfigType,
  AgentAppTemplate,
  AgentApplicationType,
  agentApplicationTypeTranslationMap,
  AgentProfile,
  AgentProvisionType
} from '@shared/models/agent.models';
import { AgentProfileWizardData } from '@home/pages/agent/wizard/agent-profile-wizard.component';
import { pickComposeType } from '@home/pages/agent/util/agent-compose-yaml';

interface PresetTile {
  appTypes: AgentApplicationType[];
  icon: string;
  labelKey: string;
  descriptionKey: string;
  defaultNameKey: string;
}

const PRESET_TILES: PresetTile[] = [
  {
    appTypes: [AgentApplicationType.EDGE],
    icon: 'router',
    labelKey: 'agent.app-type-edge',
    descriptionKey: 'agent.preset-edge-description',
    defaultNameKey: 'agent.preset-name-edge'
  },
  {
    appTypes: [AgentApplicationType.GATEWAY],
    icon: 'dns',
    labelKey: 'agent.app-type-gateway',
    descriptionKey: 'agent.preset-gateway-description',
    defaultNameKey: 'agent.preset-name-gateway'
  },
  {
    appTypes: [AgentApplicationType.EDGE, AgentApplicationType.GATEWAY],
    icon: 'device_hub',
    labelKey: 'agent.preset-edge-gateway',
    descriptionKey: 'agent.preset-edge-gateway-description',
    defaultNameKey: 'agent.preset-name-edge-gateway'
  }
];

@Component({
  selector: 'tb-agent-profile-preset-dialog',
  templateUrl: './agent-profile-preset-dialog.component.html',
  styleUrls: ['./agent-profile-preset-dialog.component.scss'],
  standalone: false
})
export class AgentProfilePresetDialogComponent
  extends DialogComponent<AgentProfilePresetDialogComponent, AgentProfile> {

  tiles: PresetTile[];
  selectedTile: PresetTile | null = null;
  form: UntypedFormGroup;
  mode: 'preset' | 'advanced' = 'preset';
  advancedDefaults: Partial<AgentProfile> | null = null;
  nameEditedByUser = false;
  submitting = false;
  templatesState: 'loading' | 'loaded' = 'loading';
  latestTemplates: Partial<Record<AgentApplicationType, AgentAppTemplate>> = {};
  missingTypes: AgentApplicationType[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileWizardData,
              public dialogRef: MatDialogRef<AgentProfilePresetDialogComponent, AgentProfile>) {
    super(store, router, dialogRef);
    const locked = data?.lockedAppType;
    this.tiles = locked ? PRESET_TILES.filter(t => t.appTypes.includes(locked)) : [...PRESET_TILES];
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]]
    });
    if (data?.defaults?.name) {
      this.form.get('name').setValue(data.defaults.name);
      this.nameEditedByUser = true;
    }
    const singleTypeMatch = locked ? this.tiles.find(t => t.appTypes.length === 1) : null;
    this.selectTile(singleTypeMatch ?? this.tiles.find(t => t.appTypes.length === 2) ?? this.tiles[0] ?? null);
    this.loadTemplates();
  }

  private neededTypes(): AgentApplicationType[] {
    return [...new Set(this.tiles.flatMap(t => t.appTypes))];
  }

  private loadTemplates() {
    this.templatesState = 'loading';
    const types = this.neededTypes();
    forkJoin(types.map(type =>
      this.agentService.getLatestAgentAppTemplate(type, AgentAppConfigType.DOCKER_COMPOSE,
        { ignoreLoading: true, ignoreErrors: true }).pipe(catchError(() => of(null)))
    )).subscribe(results => {
      this.latestTemplates = {};
      this.missingTypes = [];
      results.forEach((tpl, i) => {
        if (tpl?.currentVersion) {
          this.latestTemplates[types[i]] = tpl;
        } else {
          this.missingTypes.push(types[i]);
        }
      });
      this.templatesState = 'loaded';
      if (this.selectedTile && this.tileDisabled(this.selectedTile)) {
        this.selectTile(this.tiles.find(t => !this.tileDisabled(t)) ?? null);
      }
    });
  }

  retryTemplates() {
    this.loadTemplates();
  }

  tileDisabled(tile: PresetTile): boolean {
    return this.templatesState === 'loaded' && tile.appTypes.some(t => this.missingTypes.includes(t));
  }

  selectTile(tile: PresetTile | null) {
    if (tile && this.tileDisabled(tile)) {
      return;
    }
    this.selectedTile = tile;
    if (!this.nameEditedByUser || !(this.form.get('name').value || '').trim()) {
      this.form.get('name').setValue(tile ? this.translate.instant(tile.defaultNameKey) : '');
      this.nameEditedByUser = false;
    }
  }

  onNameInput() {
    this.nameEditedByUser = !!(this.form.get('name').value || '').trim();
  }

  appLabelWithVersion(type: AgentApplicationType): string {
    const label = this.translate.instant(agentApplicationTypeTranslationMap.get(type));
    const version = this.latestTemplates[type]?.currentVersion;
    return version ? `${label} ${version}` : label;
  }

  versionChips(tile: PresetTile): string[] {
    return tile.appTypes.map(t => this.appLabelWithVersion(t));
  }

  get missingTypesLabel(): string {
    return this.missingTypes
      .map(t => this.translate.instant(agentApplicationTypeTranslationMap.get(t)))
      .join(', ');
  }

  get summaryBody(): string {
    if (!this.selectedTile) {
      return '';
    }
    const apps = this.selectedTile.appTypes.map(t => this.appLabelWithVersion(t));
    const appsText = apps.length === 2
      ? this.translate.instant('agent.preset-summary-apps-two', { first: apps[0], second: apps[1] })
      : apps[0];
    return this.translate.instant('agent.preset-summary-body', { apps: appsText });
  }

  get canSubmit(): boolean {
    return this.form.valid
      && !!this.selectedTile
      && this.templatesState === 'loaded'
      && !this.tileDisabled(this.selectedTile)
      && this.selectedTile.appTypes.every(t => !!this.latestTemplates[t])
      && !this.submitting;
  }

  advanced() {
    const name = (this.form.get('name').value || '').trim();
    this.advancedDefaults = {
      ...this.data?.defaults,
      ...(this.nameEditedByUser && name ? { name } : {})
    };
    this.mode = 'advanced';
  }

  onAdvancedFinished(profile: AgentProfile) {
    this.dialogRef.close(profile);
  }

  onAdvancedBack() {
    this.mode = 'preset';
  }

  cancel() {
    this.dialogRef.close(undefined);
  }

  submit() {
    if (!this.canSubmit) {
      return;
    }
    this.submitting = true;
    forkJoin(this.selectedTile.appTypes.map(type => {
      const tpl = this.latestTemplates[type];
      return this.agentService.materializeAgentAppProfile(type, tpl.currentVersion, pickComposeType(tpl));
    })).pipe(
      switchMap(appProfiles => this.agentService.saveAgentProfile(
        {
          name: (this.form.get('name').value || '').trim(),
          provisionType: AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE
        } as AgentProfile,
        undefined,
        appProfiles.map(p => p.id.id)))
    ).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }
}
