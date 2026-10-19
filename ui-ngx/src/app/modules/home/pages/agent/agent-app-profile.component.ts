// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentAppArgument,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  agentApplicationTypeTranslationMap,
  AgentAppConfigType,
  dockerComposeConfig
} from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentService } from '@core/http/agent.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { getAce } from '@shared/models/ace/ace.models';
import { Ace } from 'ace-builds';
import * as YAML from 'yaml';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import { dumpYaml } from '@home/pages/agent/util/agent-compose-yaml';
import {
  clearSettingsCardFullscreen,
  markSettingsCardFullscreen
} from '@home/pages/agent/util/agent-settings-card';
import {
  AgentAppProfileUpgradeDialogComponent,
  AgentAppProfileUpgradeDialogData
} from '@home/pages/agent/dialog/agent-app-profile-upgrade-dialog.component';

@Component({
  selector: 'tb-agent-app-profile',
  templateUrl: './agent-app-profile.component.html',
  styleUrls: ['./agent-app-profile.component.scss'],
  standalone: false
})
export class AgentAppProfileComponent extends EntityComponent<AgentAppProfile>
  implements AfterViewInit, OnDestroy {

  entityType = EntityType;
  agentApplicationTypes = Object.values(AgentApplicationType);
  agentApplicationTypeTranslationMap = agentApplicationTypeTranslationMap;

  templateVersion = '';
  nextVersion: string | null = null;
  private fullscreenHost: HTMLElement | null = null;

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
              @Inject('entity') protected entityValue: AgentAppProfile,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentAppProfile>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private agentService: AgentService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private hostElementRef: ElementRef<HTMLElement>) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngAfterViewInit() {
    this.loadTemplateVersion();
    Promise.resolve().then(() => {
      this.fullscreenHost =
        markSettingsCardFullscreen(this.hostElementRef.nativeElement, 'tb-agent-app-profile-fullscreen');
    });
  }

  ngOnDestroy() {
    if (this.fullscreenHost) {
      clearSettingsCardFullscreen(this.fullscreenHost, 'tb-agent-app-profile-fullscreen');
      this.fullscreenHost = null;
    }
    if (this.composeEditor) {
      try { this.composeEditor.destroy(); } catch (_) { /* no-op */ }
      this.composeEditor = null;
    }
  }

  buildForm(entity: AgentAppProfile): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      appType: [entity ? entity.appType : AgentApplicationType.EDGE, Validators.required],
      composeYaml: [this.dumpCompose(entity)],
      arguments: [this.extractArguments(entity)]
    });
  }

  updateForm(entity: AgentAppProfile) {
    const yaml = this.dumpCompose(entity);
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description,
      appType: entity.appType,
      composeYaml: yaml,
      arguments: this.extractArguments(entity)
    });
    const appTypeCtrl = this.entityForm.get('appType');
    if (entity?.id) {
      appTypeCtrl.disable({ emitEvent: false });
    } else {
      appTypeCtrl.enable({ emitEvent: false });
    }
    this.pushComposeToEditor(yaml);
    this.loadTemplateVersion();
  }

  prepareFormValue(formValue: any): any {
    const prepared = super.prepareFormValue(formValue);
    const yaml = prepared.composeYaml;
    const argumentsValue: AgentAppArgument[] = prepared.arguments ?? [];
    delete prepared.composeYaml;
    delete prepared.arguments;
    const config: any = {
      ...(this.entity?.config || { type: AgentAppConfigType.DOCKER_COMPOSE }),
      arguments: argumentsValue
    };
    if (yaml?.trim()) {
      try {
        config.compose = YAML.parse(yaml);
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
    } else {
      config.compose = null;
    }
    prepared.config = config;
    if (prepared.appType == null && this.entity?.appType) {
      prepared.appType = this.entity.appType;
    }
    return prepared;
  }

  updateFormState() {
    super.updateFormState();
    this.applyComposeEditorReadOnly();
  }

  canUpgrade(): boolean {
    if (this.entity?.appType === AgentApplicationType.GENERIC) { return false; }
    return !!this.nextVersion;
  }

  onUpgrade($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.dialog.open<AgentAppProfileUpgradeDialogComponent, AgentAppProfileUpgradeDialogData, AgentAppProfile | null>(
      AgentAppProfileUpgradeDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { profile: this.entity }
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        // Assign into the entity setter so updateForm() + loadTemplateVersion()
        // run. The table side-panel ignores 'reload' actions, so we can't rely
        // on reloadEntity() alone to refresh templateVersion / nextVersion.
        this.entity = saved;
        this.reloadEntity();
        // Also refresh the parent list — the row's template-version cell is
        // rendered from the profile entity that was fetched before this upgrade
        // and would otherwise keep showing the old version until the next page
        // navigation.
        this.entitiesTableConfig?.updateData();
        this.showAssignedAppsPropagationHint();
      }
    });
  }

  private showAssignedAppsPropagationHint() {
    this.dialogService.alert(
      this.translate.instant('agent.app-profile-saved-title'),
      this.translate.instant('agent.app-profile-saved-propagation-text')
    );
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

  private loadTemplateVersion() {
    // Clear cached state up front. Otherwise a reload after a successful
    // profile upgrade leaves the stale `nextVersion` active until the async
    // template fetch resolves, keeping the Upgrade button on the header
    // enabled for a frame (and "forever" if the fetch is swallowed).
    this.templateVersion = '';
    this.nextVersion = null;
    this.cd.markForCheck();
    if (this.entity?.templateVersion) {
      this.agentService.getAgentAppTemplateByVersion(this.entity.appType, AgentAppConfigType.DOCKER_COMPOSE, this.entity.templateVersion).subscribe({
        next: tpl => {
          this.templateVersion = tpl.currentVersion || '';
          this.nextVersion = tpl.nextVersion || null;
          this.cd.markForCheck();
        },
        error: () => {}
      });
    }
  }

  private reloadEntity() {
    this.entityAction.emit({ event: null, action: 'reload', entity: this.entity });
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
        if (this.composeEditorSettingValue) { return; }
        // Propagate every user-visible change (typing, paste, programmatic
        // edits) to the form control. Our own writes are already guarded by
        // composeEditorSettingValue above, so we don't gate on focus — that
        // would silently drop paste/programmatic edits made while unfocused.
        const ctrl = this.entityForm?.get('composeYaml');
        if (ctrl && ctrl.value !== editor.getValue()) {
          ctrl.setValue(editor.getValue());
          ctrl.markAsDirty();
        }
      });
      this.composeEditor = editor;
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
    this.composeEditorSettingValue = true;
    this.composeEditor.setValue(yaml || '', -1);
    this.composeEditorSettingValue = false;
  }

  private applyComposeEditorReadOnly() {
    if (!this.composeEditor) { return; }
    const readOnly = !this.isEdit;
    this.composeEditor.setReadOnly(readOnly);
    const cursorLayer = (this.composeEditor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = readOnly ? 'none' : '';
    }
  }

  private extractArguments(entity: AgentAppProfile): AgentAppArgument[] {
    return entity?.config?.arguments ?? [];
  }

  private dumpCompose(entity: AgentAppProfile): string {
    const compose: any = dockerComposeConfig(entity)?.compose;
    if (!compose) { return ''; }
    return dumpYaml(compose, 0).trimEnd() + '\n';
  }
}
