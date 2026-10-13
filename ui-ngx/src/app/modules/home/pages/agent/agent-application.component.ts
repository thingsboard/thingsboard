// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentApplication,
  AgentApplicationSaveRequest,
  AgentAppArgument,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppProfileInfo,
  AgentApplicationInfo,
  agentApplicationOriginTranslationMap,
  AgentApplicationType,
  agentApplicationTypeTranslationMap,
  AgentAppConfigType,
  dockerComposeConfig
} from '@shared/models/agent.models';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
import * as YAML from 'yaml';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityService } from '@core/http/entity.service';
import { baseDetailsPageByEntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AgentAppProfileId } from '@shared/models/id/agent-app-profile-id';
import {
  AgentAppDeleteDialogComponent,
  AgentAppDeleteDialogData
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData,
  AgentAppInstallWizardResult
} from '@home/components/agent/wizard/agent-app-install-wizard.component';
import { catchError, map, mergeMap, share, startWith } from 'rxjs/operators';
import { findComposeDownStep } from '@home/pages/agent/util/agent-app-steps';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { dumpYaml } from '@home/pages/agent/util/agent-compose-yaml';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { confirmAppVersionWarning, confirmUpdateDrift } from '@home/pages/agent/util/agent-version-warnings';
import {
  clearSettingsCardFullscreen,
  markSettingsCardFullscreen
} from '@home/pages/agent/util/agent-settings-card';
import {
  AgentAppAssignCompareDialogComponent,
  AgentAppAssignCompareDialogData
} from '@home/pages/agent/dialog/agent-app-assign-compare-dialog.component';

@Component({
  selector: 'tb-agent-application',
  templateUrl: './agent-application.component.html',
  styleUrls: ['./agent-application.component.scss'],
  standalone: false
})
export class AgentApplicationComponent extends EntityComponent<AgentApplicationInfo>
  implements AfterViewInit, OnDestroy {

  entityType = EntityType;
  private fullscreenHost: HTMLElement | null = null;

  agentApplicationTypes = Object.values(AgentApplicationType);
  agentApplicationTypeTranslationMap = agentApplicationTypeTranslationMap;
  agentApplicationOriginTranslationMap = agentApplicationOriginTranslationMap;

  templateVersion = '';

  relatedEntity: { name: string; url: string; typeLabel: string } | null = null;

  profileSearchCtrl: UntypedFormControl;
  filteredProfiles: Observable<AgentAppProfileInfo[]>;

  private availableProfiles$ = new BehaviorSubject<AgentAppProfileInfo[]>([]);
  private availableProfilesValue: AgentAppProfileInfo[] = [];

  get availableProfiles(): AgentAppProfileInfo[] {
    return this.availableProfilesValue;
  }

  set availableProfiles(profiles: AgentAppProfileInfo[]) {
    this.availableProfilesValue = profiles;
    this.availableProfiles$.next(profiles);
    this.syncProfileDisplay();
  }
  // Cached compose YAML of the previously non-profile state so the user can
  // restore it by clearing the profile selector. Captured on first switch to
  // a profile and cleared once they detach it.
  private detachedComposeYaml: string | null = null;
  // Same restore semantics as detachedComposeYaml, for the custom arguments list.
  private detachedArguments: AgentAppArgument[] | null = null;

  private composeEditor: Ace.Editor | null = null;
  private composeEditorSettingValue = false;
  private pendingComposeValue: string | null = null;
  @ViewChild('composeAceEditor', { static: false })
  set composeAceRef(ref: ElementRef<HTMLElement> | undefined) {
    if (ref && !this.composeEditor) {
      this.initComposeEditor(ref.nativeElement);
    }
  }

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentApplicationInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentApplicationInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private agentService: AgentService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private router: Router,
              private destroyRef: DestroyRef,
              private hostElementRef: ElementRef<HTMLElement>) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
    this.profileSearchCtrl = this.fb.control(null);
    this.profileSearchCtrl.disable({ emitEvent: false });
    const text$ = this.profileSearchCtrl.valueChanges.pipe(
      startWith(this.profileSearchCtrl.value),
      map(value => !value ? '' : (typeof value === 'string' ? value : (value as AgentAppProfileInfo).name))
    );
    this.filteredProfiles = combineLatest([this.availableProfiles$, text$]).pipe(
      map(([all, text]) => {
        if (!text || !text.length) {
          return all;
        }
        const lc = text.toLowerCase();
        return all.filter(p => p.name.toLowerCase().includes(lc));
      }),
      share()
    );
    this.profileSearchCtrl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      const selected = (value && typeof value !== 'string') ? value as AgentAppProfileInfo : null;
      if (selected?.id?.id && selected.id.id !== this.selectedProfileId) {
        this.onProfileChange(selected.id.id);
      }
    });
  }

  ngAfterViewInit(): void {
    // Deferred to a microtask so the lookup runs after the tab body finishes
    // attaching the host element to the DOM.
    Promise.resolve().then(() => {
      this.fullscreenHost =
        markSettingsCardFullscreen(this.hostElementRef.nativeElement, 'tb-agent-app-fullscreen');
    });
  }

  ngOnDestroy(): void {
    if (this.fullscreenHost) {
      clearSettingsCardFullscreen(this.fullscreenHost, 'tb-agent-app-fullscreen');
      this.fullscreenHost = null;
    }
    if (this.composeEditor) {
      try { this.composeEditor.destroy(); } catch (_) { /* no-op */ }
      this.composeEditor = null;
    }
  }

  updateFormState() {
    super.updateFormState();
    if (this.profileSearchCtrl) {
      if (this.isEdit) {
        this.profileSearchCtrl.enable({ emitEvent: false });
      } else {
        this.profileSearchCtrl.disable({ emitEvent: false });
      }
    }
    this.applyComposeEditorReadOnly();
  }

  private initComposeEditor(host: HTMLElement) {
    getAce().subscribe((ace) => {
      const editor: Ace.Editor = ace.edit(host);
      editor.setTheme('ace/theme/textmate');
      editor.session.setMode('ace/mode/yaml');
      editor.session.setUseWrapMode(false);
      editor.setShowPrintMargin(false);
      editor.setOption('scrollPastEnd', 0);
      editor.renderer.setScrollMargin(0, 0, 0, 0);
      editor.setFontSize(12);
      // Override the global .ace_editor { font-size: 16px !important } from
      // styles.scss by setting font-size inline with !important.
      const container = editor.container;
      container?.style?.setProperty('font-size', '12px', 'important');
      editor.setOption('tabSize', 2);
      editor.setOption('useSoftTabs', true);
      editor.setOption('showLineNumbers', true);
      editor.setOption('highlightActiveLine', false);
      const initial = this.pendingComposeValue
        ?? (this.entityForm?.get('composeYaml')?.value as string)
        ?? '';
      editor.setValue(initial, -1);
      this.pendingComposeValue = null;
      editor.getSession().on('change', () => {
        if (this.composeEditorSettingValue || editor.getReadOnly()) {
          return;
        }
        const ctrl = this.entityForm?.get('composeYaml');
        if (ctrl && ctrl.value !== editor.getValue()) {
          ctrl.setValue(editor.getValue());
          ctrl.markAsDirty();
        }
      });
      this.composeEditor = editor;
      // Confine wheel input to this editor. stopPropagation alone won't
      // work — ace uses virtual scrolling and doesn't preventDefault on
      // deltas it can't consume, so those spill into the browser's default
      // scroll chain and scroll the surrounding details page. Instead we
      // preventDefault on the container and manually forward the delta
      // into ace's scrollTop/scrollLeft so the editor still scrolls while
      // it has room to move.
      confineWheelToAceEditor(container, editor, () => !!this.isEdit && editor.isFocused());
      this.applyComposeEditorReadOnly();
      setTimeout(() => editor.resize(true), 0);
    });
  }

  private pushComposeToEditor(yaml: string) {
    if (!this.composeEditor) {
      this.pendingComposeValue = yaml;
      return;
    }
    if (this.composeEditor.getValue() === yaml) {
      return;
    }
    this.composeEditorSettingValue = true;
    this.composeEditor.setValue(yaml || '', -1);
    this.composeEditorSettingValue = false;
  }

  private applyComposeEditorReadOnly() {
    if (!this.composeEditor) {
      return;
    }
    // Profile-managed apps are always read-only — credential edits go through
    // the credentials form below and are merged into the compose on save.
    const readOnly = !this.isEdit || this.isProfileManaged;
    this.composeEditor.setReadOnly(readOnly);
    const cursorLayer = (this.composeEditor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = readOnly ? 'none' : '';
    }
  }

  get isProfileManaged(): boolean {
    return !!this.selectedProfileId;
  }

  get selectedProfileId(): string | null {
    // Once the form exists it is the source of truth — `??` would have fallen
    // back to the persisted entity when the user explicitly cleared the
    // selector (value === null), making the credentials form linger.
    const ctrl = this.entityForm?.get('applicationProfileId');
    if (ctrl) {
      return ctrl.value ?? null;
    }
    return this.entity?.applicationProfileId?.id ?? null;
  }

  displayProfileFn(profile?: AgentAppProfileInfo): string {
    return profile ? profile.name : '';
  }

  clearProfileField() {
    // Emit so the autocomplete filter stream resets — patching silently leaves
    // the dropdown filtered by the previously selected profile's name.
    this.profileSearchCtrl.patchValue(null);
    this.onProfileChange(null);
  }

  syncProfileDisplay() {
    if (!this.profileSearchCtrl) {
      return;
    }
    const id = this.selectedProfileId;
    if (!id) {
      this.profileSearchCtrl.patchValue(null);
      return;
    }
    const match = this.availableProfiles.find(p => p.id.id === id);
    this.profileSearchCtrl.patchValue(match ?? { id: new AgentAppProfileId(id), name: this.selectedProfileName } as AgentAppProfileInfo);
  }

  onProfileChange(profileId: string | null) {
    // Called from the mat-autocomplete selection (via profileSearchCtrl
    // valueChanges) AND from the clear button. Neither path writes the
    // applicationProfileId control directly, so applyProfileSelection sets it —
    // both paths converge there.
    if (!profileId) {
      this.applyProfileSelection(null);
      return;
    }
    this.resolveProfile(profileId).subscribe(profile => {
      if (!profile) {
        this.applyProfileSelection(profileId);
        return;
      }
      this.confirmProfileAssignment(profile).pipe(
        mergeMap(result => {
          if (result === 'upgrade') {
            this.syncProfileDisplay();
            this.onUpgrade(null);
            return of(null);
          }
          return result ? this.confirmComposeCompare(profile) : of(false);
        })
      ).subscribe(confirmed => {
        if (confirmed === null) {
          return;
        }
        if (confirmed) {
          this.applyProfileSelection(profileId, profile);
        } else {
          this.syncProfileDisplay();
        }
      });
    });
  }

  private resolveProfile(profileId: string): Observable<AgentAppProfileInfo | null> {
    const profile = this.availableProfiles.find(p => p.id.id === profileId);
    if (profile) {
      return of(profile);
    }
    if (!this.entity?.appType) {
      return of(null);
    }
    return this.agentService.getAgentAppProfilesByAppType(this.entity.appType).pipe(
      map(list => {
        this.availableProfiles = list;
        return list.find(p => p.id.id === profileId) ?? null;
      })
    );
  }

  private confirmProfileAssignment(profile: AgentAppProfileInfo): Observable<boolean | 'upgrade'> {
    const appVersion = this.entity?.currentVersion || this.templateVersion || this.entity?.templateVersion;
    const profileVersion = profile.templateCurrentVersion;
    if (this.entity?.appType === AgentApplicationType.GENERIC
        || !appVersion || !profileVersion || profileVersion === appVersion) {
      return of(true);
    }
    return confirmAppVersionWarning(this.dialog, {
      context: 'assign',
      appName: this.entity?.name,
      profileName: profile.name,
      appVersion,
      profileVersion,
      nextVersion: this.entity?.nextVersion
    });
  }

  private confirmComposeCompare(profile: AgentAppProfileInfo): Observable<boolean> {
    const profileCompose: any = dockerComposeConfig(profile)?.compose;
    if (!profileCompose) {
      return of(true);
    }
    const currentYaml = (this.entityForm?.get('composeYaml')?.value as string) || this.dumpCompose(this.entity);
    // Same preview merge as the update flow's future-config compare: fills
    // credentials/host values from the related entity so the future compose
    // shows real values instead of the profile's placeholders.
    const draft = { ...this.entity, templateVersion: profile.templateVersion, config: profile.config } as AgentApplication;
    const relatedEntityId = this.entity?.relatedEntityId;
    return this.agentService.mergeForPreview(
      profile.templateVersion, this.entity.appType, draft, undefined, relatedEntityId || undefined
    ).pipe(
      map(merged => this.dumpCompose(merged as AgentApplicationInfo)),
      catchError(() => of(dumpYaml(profileCompose, 0).trimEnd() + '\n')),
      mergeMap(futureYaml => this.dialog.open<AgentAppAssignCompareDialogComponent, AgentAppAssignCompareDialogData, boolean>(
        AgentAppAssignCompareDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            appName: this.entity?.name,
            profileName: profile.name,
            currentYaml,
            futureYaml
          }
        }).afterClosed().pipe(map(res => !!res)))
    );
  }

  private applyProfileSelection(profileId: string | null, profile?: AgentAppProfileInfo) {
    const ctrl = this.entityForm.get('applicationProfileId');
    if (ctrl.value !== (profileId ?? null)) {
      ctrl.setValue(profileId ?? null);
    }
    ctrl.markAsDirty();

    if (profileId) {
      if (this.detachedComposeYaml == null) {
        this.detachedComposeYaml = this.entityForm.get('composeYaml')?.value ?? this.dumpCompose(this.entity);
      }
      if (this.detachedArguments == null) {
        this.detachedArguments = this.entityForm.get('arguments')?.value ?? this.extractArguments(this.entity);
      }
      if (profile) {
        this.applyProfileConfig(profile);
      }
    } else {
      // Detached — restore the previous compose so the editor becomes editable again.
      const restored = this.detachedComposeYaml ?? this.dumpCompose(this.entity);
      this.entityForm.get('composeYaml').setValue(restored);
      this.entityForm.get('composeYaml').markAsDirty();
      this.pushComposeToEditor(restored);
      const restoredArgs = this.detachedArguments ?? this.extractArguments(this.entity);
      this.entityForm.get('arguments').setValue(restoredArgs);
      this.entityForm.get('arguments').markAsDirty();
      this.detachedComposeYaml = null;
      this.detachedArguments = null;
    }
    this.syncProfileDisplay();
    this.applyComposeEditorReadOnly();
  }

  private applyProfileConfig(profile: AgentAppProfile) {
    const profileCompose: any = dockerComposeConfig(profile)?.compose;
    if (!profileCompose) { return; }
    const yaml = dumpYaml(profileCompose, 0).trimEnd() + '\n';
    this.entityForm.get('composeYaml').setValue(yaml);
    this.entityForm.get('composeYaml').markAsDirty();
    this.pushComposeToEditor(yaml);
    this.entityForm.get('arguments').setValue(profile?.config?.arguments ?? []);
    this.entityForm.get('arguments').markAsDirty();
  }

  private loadProfilesForType(appType: AgentApplicationType | undefined) {
    if (!appType) { return; }
    this.agentService.getAgentAppProfilesByAppType(appType).subscribe(profiles => {
      this.availableProfiles = profiles;
    });
  }

  hideDelete() {
    return true;
  }

  canUpgrade(): boolean {
    if (this.entity?.appType === AgentApplicationType.GENERIC) { return false; }
    return !!this.entity?.nextVersion;
  }

  get profileTemplateDrifted(): boolean {
    return !!this.entity?.profileTemplateVersion && !!this.entity?.templateVersion
      && this.entity.profileTemplateVersion !== this.entity.templateVersion;
  }

  canHaveRelatedEntity(): boolean {
    return this.entity?.appType === AgentApplicationType.EDGE
        || this.entity?.appType === AgentApplicationType.GATEWAY;
  }

  buildForm(entity: AgentApplicationInfo): UntypedFormGroup {
    const relatedEntityRequired = entity?.appType === AgentApplicationType.EDGE
      || entity?.appType === AgentApplicationType.GATEWAY;
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      applicationProfileId: [entity?.applicationProfileId?.id ?? null],
      composeYaml: [this.dumpCompose(entity)],
      arguments: [this.extractArguments(entity)],
      relatedEntityId: [entity?.relatedEntityId ?? null, relatedEntityRequired ? [Validators.required] : []]
    });
  }

  updateForm(entity: AgentApplicationInfo) {
    const yaml = this.dumpCompose(entity);
    const args = this.extractArguments(entity);
    this.entityForm.patchValue({
      name: entity.name,
      applicationProfileId: entity?.applicationProfileId?.id ?? null,
      composeYaml: yaml,
      arguments: args,
      relatedEntityId: entity?.relatedEntityId ?? null
    });
    this.pushComposeToEditor(yaml);
    this.detachedComposeYaml = entity?.applicationProfileId ? null : yaml;
    this.detachedArguments = entity?.applicationProfileId ? null : args;
    this.syncProfileDisplay();
    this.loadProfilesForType(entity?.appType);
    this.applyComposeEditorReadOnly();
    this.templateVersion = entity?.currentVersion || '';
    this.resolveRelatedEntity(entity?.relatedEntityId);
    // The detail GET returns AgentApplication (no currentVersion / nextVersion
    // — those are only joined on the list endpoint). Resolve them from the
    // linked template so templateVersion renders the actual version AND the
    // upgrade button / hint can read entity.nextVersion.
    if (entity?.templateVersion && (!entity.currentVersion || !entity.nextVersion)) {
      this.agentService.getAgentAppTemplateByVersion(entity.appType, AgentAppConfigType.DOCKER_COMPOSE, entity.templateVersion).subscribe(tpl => {
        if (tpl?.currentVersion) {
          this.templateVersion = tpl.currentVersion;
          if (!entity.currentVersion) {
            entity.currentVersion = tpl.currentVersion;
          }
        }
        if (tpl?.nextVersion && !entity.nextVersion) {
          entity.nextVersion = tpl.nextVersion;
          this.cd.markForCheck();
        }
      });
    }
  }

  private resolveRelatedEntity(relatedEntityId: EntityId | undefined | null) {
    this.relatedEntity = null;
    if (!relatedEntityId?.id || !relatedEntityId?.entityType) {
      return;
    }
    const basePath = baseDetailsPageByEntityType.get(relatedEntityId.entityType as EntityType);
    if (!basePath) {
      return;
    }
    const url = `${basePath}/${relatedEntityId.id}`;
    const typeLabel = this.translate.instant(`entity.type-${relatedEntityId.entityType.toLowerCase()}`);
    // Show the id as a fallback so the field isn't blank while the name loads
    // (or if the lookup fails — e.g. the related entity was deleted).
    this.relatedEntity = { name: relatedEntityId.id, url, typeLabel };
    this.entityService.getEntity(
      relatedEntityId.entityType as EntityType,
      relatedEntityId.id,
      { ignoreLoading: true, ignoreErrors: true }
    ).subscribe({
      next: (e: any) => {
        if (this.relatedEntity) {
          this.relatedEntity = { ...this.relatedEntity, name: e?.name || relatedEntityId.id };
          this.cd.markForCheck();
        }
      }
    });
  }

  get selectedProfileName(): string {
    const id = this.selectedProfileId;
    if (!id) { return ''; }
    const match = this.availableProfiles.find(p => p.id.id === id);
    // Fall back to the profileName the info endpoint joined onto the entity —
    // covers the window before `availableProfiles` resolves on first render.
    return match?.name || this.entity?.profileName || '';
  }

  prepareFormValue(formValue: any): AgentApplicationSaveRequest {
    const prepared = super.prepareFormValue(formValue);
    let compose = dockerComposeConfig(this.entity)?.compose;
    const yamlText: string = formValue?.composeYaml;
    if (yamlText && yamlText.trim().length > 0) {
      try {
        compose = YAML.parse(yamlText);
        this.entityForm.get('composeYaml')?.setErrors(null);
      } catch (e) {
        const composeCtrl = this.entityForm.get('composeYaml');
        composeCtrl?.setErrors({ invalidYaml: true });
        composeCtrl?.markAsTouched();
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('agent.app-compose-invalid-yaml'),
          type: 'error',
          duration: 3000,
          verticalPosition: 'bottom',
          horizontalPosition: 'left'
        }));
        throw e;
      }
    }
    prepared.config = {
      ...(this.entity?.config || { type: AgentAppConfigType.DOCKER_COMPOSE }),
      compose,
      arguments: formValue?.arguments ?? []
    };
    const profileId: string | null = formValue?.applicationProfileId ?? null;
    prepared.applicationProfileId = profileId
        ? { id: profileId, entityType: 'AGENT_APP_PROFILE' }
        : null;
    delete prepared.composeYaml;
    delete prepared.arguments;
    delete prepared.appType;
    delete prepared.currentVersion;
    delete prepared.relatedEntityId;
    const request: AgentApplicationSaveRequest = {
      ...prepared,
      relatedEntityIdNext: formValue?.relatedEntityId ?? null,
      relatedEntityIdPrev: this.entity?.relatedEntityId ?? null
    };
    return request;
  }

  onUpdate($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationInfoById(this.entity.id.id).pipe(
      mergeMap(full => confirmUpdateDrift(this.dialog, full).pipe(
        map(confirmed => ({ full, confirmed }))
      )),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(({ full, confirmed }) => {
      if (!confirmed) { return; }
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
          data: {
            agentId: full.agentId.id,
            agent: null,
            mode: 'update',
            application: full
          }
        }
      ).afterClosed().subscribe(event => {
        if (event) {
          this.reloadEntity();
        }
      });
    });
  }

  onRestart($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.app-restart-title', { name: this.entity.name }),
      this.translate.instant('agent.app-restart-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (!res) { return; }
      // Need the full application for the progress dialog.
      this.agentService.getAgentApplicationInfoById(this.entity.id.id).pipe(
        mergeMap(full =>
          this.agentService.createAgentAppEvent(this.entity.id.id, { actionType: AgentAppEventActionType.RESTART })
            .pipe(mergeMap(event => {
              this.reloadEntity();
              if (event) {
                return openAgentAppEventProgress(this.dialog, full as AgentApplication, event);
              }
              return of(null);
            }))
        ),
        takeUntilDestroyed(this.destroyRef)
      ).subscribe();
    });
  }

  onUpgrade($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationInfoById(this.entity.id.id).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppInstallWizardResult>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-agent-wizard-dialog'],
          data: {
            agentId: full.agentId.id,
            agent: null,
            mode: 'upgrade',
            application: full
          }
        }
      ).afterClosed().subscribe(event => {
        if (event) {
          this.reloadEntity();
        }
      });
    });
  }

  onDelete($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationInfoById(this.entity.id.id).pipe(
      mergeMap(full => {
        const composeDownStep$ = full?.templateVersion
          ? this.agentService.getAgentAppTemplateByVersion(full.appType, AgentAppConfigType.DOCKER_COMPOSE, full.templateVersion).pipe(
              map(findComposeDownStep),
              catchError(() => of(null))
            )
          : of(null);
        return composeDownStep$.pipe(
          mergeMap(composeDownStep => this.dialog.open<AgentAppDeleteDialogComponent, AgentAppDeleteDialogData, AgentAppEvent | null>(
            AgentAppDeleteDialogComponent, {
              disableClose: false,
              panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
              data: { application: full, composeDownStep }
            }
          ).afterClosed().pipe(map(event => ({ full, event }))))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(({ full, event }) => {
      if (!event) { return; }
      const agentId = full.agentId?.id;
      openAgentAppEventProgress(this.dialog, full, event).subscribe(() => {
        if (agentId) {
          this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId, 'applications'));
        }
      });
    });
  }

  onAppIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.app-id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'left'
    }));
  }

  private reloadEntity() {
    // Ask the parent details page to refetch + reinject the entity. This is
    // critical because EntityDetailsPageComponent caches `this.entity` and
    // re-clones it on every edit-mode toggle — patching only our local copy
    // would be silently overwritten the next time the user clicks the pencil.
    this.entityAction.emit({ event: null, action: 'reload', entity: this.entity });
    return of(null);
  }

  private extractArguments(entity: AgentApplicationInfo): AgentAppArgument[] {
    return entity?.config?.arguments ?? [];
  }

  private dumpCompose(entity: AgentApplicationInfo): string {
    const compose: any = dockerComposeConfig(entity)?.compose;
    if (!compose) {
      return '';
    }
    return dumpYaml(compose, 0).trimEnd() + '\n';
  }
}
