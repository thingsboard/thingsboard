// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Input, OnInit, output } from '@angular/core';
import { FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { TranslateService } from '@ngx-translate/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  AgentAppArgument,
  AgentAppArgumentFormat,
  agentAppArgumentFormatTranslationMap,
  AgentAppArgumentSource,
  agentAppArgumentSourceEntityTypeMap,
  agentAppArgumentSourceTranslationMap,
  AgentAppArgumentValueType,
  agentAppArgumentValueTypeTranslationMap
} from '@shared/models/agent.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-agent-app-argument-panel',
  templateUrl: './agent-app-argument-panel.component.html',
  styleUrls: ['./agent-app-argument-panel.component.scss'],
  standalone: false
})
export class AgentAppArgumentPanelComponent implements OnInit {

  @Input() argument: AgentAppArgument;
  @Input() buttonTitle: string;
  @Input() usedNames: string[] = [];
  @Input() readonly = false;

  argumentApplied = output<AgentAppArgument>();

  argumentSources = Object.values(AgentAppArgumentSource);
  argumentSourceTranslationMap = agentAppArgumentSourceTranslationMap;
  argumentSourceEntityTypeMap = agentAppArgumentSourceEntityTypeMap;
  argumentValueTypes = Object.values(AgentAppArgumentValueType);
  argumentValueTypeTranslationMap = agentAppArgumentValueTypeTranslationMap;
  attributeScopes = [AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE, AttributeScope.CLIENT_SCOPE];
  attributeScopeTranslationMap = telemetryTypeTranslations;
  argumentFormats = Object.values(AgentAppArgumentFormat);
  argumentFormatTranslationMap = agentAppArgumentFormatTranslationMap;

  readonly AgentAppArgumentValueType = AgentAppArgumentValueType;
  readonly DataKeyType = DataKeyType;

  argumentFormGroup: FormGroup;
  keyEntityFilter: EntityFilter;

  private readonly tenantId: string;

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              private store: Store<AppState>,
              private destroyRef: DestroyRef,
              private popover: TbPopoverComponent<AgentAppArgumentPanelComponent>) {
    this.tenantId = getCurrentAuthUser(this.store).tenantId;
  }

  get isAttribute(): boolean {
    return this.argumentFormGroup.get('valueType').value === AgentAppArgumentValueType.ATTRIBUTE;
  }

  get sourceType(): AgentAppArgumentSource {
    return this.argumentFormGroup.get('sourceType').value;
  }

  get isEntitySource(): boolean {
    return this.argumentSourceEntityTypeMap.has(this.sourceType);
  }

  get sourceEntityType(): EntityType {
    return this.argumentSourceEntityTypeMap.get(this.sourceType)?.entityType;
  }

  get keyDataType(): DataKeyType {
    return this.isAttribute ? DataKeyType.attribute : DataKeyType.timeseries;
  }

  get keyAutocompleteEnabled(): boolean {
    if (this.sourceType === AgentAppArgumentSource.TENANT) {
      return true;
    }
    return this.isEntitySource && !!(this.argumentFormGroup.get('sourceEntityId').value as EntityId)?.id;
  }

  private updateKeyEntityFilter(): void {
    const sourceEntityId = this.argumentFormGroup.get('sourceEntityId').value as EntityId;
    const singleEntity = this.isEntitySource && sourceEntityId?.id
      ? sourceEntityId
      : { id: this.tenantId, entityType: EntityType.TENANT };
    this.keyEntityFilter = { type: AliasFilterType.singleEntity, singleEntity };
  }

  get formatHint(): string {
    return this.translate.instant('agent.argument-format-hint-json')
      + '\ne.g. ports: ${tb.edge_ports}\n\n'
      + this.translate.instant('agent.argument-format-hint-embedded')
      + '\ne.g. volumes: - "${tb.host_data_dir}:/data"\n\n'
      + this.translate.instant('agent.argument-format-hint-text');
  }

  ngOnInit(): void {
    this.argumentFormGroup = this.fb.group({
      sourceType: [this.argument?.sourceType ?? AgentAppArgumentSource.AGENT, [Validators.required]],
      sourceEntityId: [this.argument?.sourceEntityId ?? null],
      valueType: [this.argument?.valueType ?? AgentAppArgumentValueType.ATTRIBUTE, [Validators.required]],
      scope: [this.argument?.scope ?? AttributeScope.SERVER_SCOPE],
      key: [this.argument?.key ?? '', [Validators.required]],
      name: [this.argument?.name ?? '',
        [Validators.required, Validators.pattern(/^[a-zA-Z0-9_]+$/), this.uniqueNameValidator()]],
      defaultValue: [this.argument?.defaultValue ?? ''],
      format: [this.argument?.format ?? AgentAppArgumentFormat.STRING, [Validators.required]]
    });
    this.updateSourceEntityValidators();
    this.updateKeyEntityFilter();
    this.argumentFormGroup.get('sourceType').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.argumentFormGroup.get('sourceEntityId').reset(null, { emitEvent: false });
        this.argumentFormGroup.get('key').reset('', { emitEvent: false });
        this.updateSourceEntityValidators();
        this.updateKeyEntityFilter();
      });
    this.argumentFormGroup.get('sourceEntityId').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.argumentFormGroup.get('key').reset('', { emitEvent: false });
        this.updateKeyEntityFilter();
      });
    if (this.readonly) {
      this.argumentFormGroup.disable({ emitEvent: false });
    }
  }

  private updateSourceEntityValidators(): void {
    const control = this.argumentFormGroup.get('sourceEntityId');
    control.setValidators(this.isEntitySource ? [Validators.required] : []);
    control.updateValueAndValidity({ emitEvent: false });
  }

  get nameReference(): string {
    const name = this.argumentFormGroup?.get('name').value;
    return '${tb.' + (name && name.length ? name : '<name>') + '}';
  }

  apply(): void {
    const raw = this.argumentFormGroup.getRawValue();
    const argument: AgentAppArgument = {
      name: raw.name,
      sourceType: raw.sourceType,
      valueType: raw.valueType,
      key: (raw.key ?? '').trim(),
      defaultValue: raw.defaultValue || undefined
    };
    if (this.isEntitySource) {
      argument.sourceEntityId = raw.sourceEntityId;
    }
    if (raw.valueType === AgentAppArgumentValueType.ATTRIBUTE) {
      argument.scope = raw.scope;
    }
    argument.format = raw.format;
    this.argumentApplied.emit(argument);
  }

  cancel(): void {
    this.popover.hide();
  }

  private uniqueNameValidator(): ValidatorFn {
    return control => {
      const value = control.value;
      return value && this.usedNames.includes(value) ? { duplicateName: true } : null;
    };
  }

}
