// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, ElementRef, EventEmitter, forwardRef, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap } from 'rxjs/operators';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { EntityService } from '@core/http/entity.service';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { Edge } from '@shared/models/edge.models';
import { Device } from '@shared/models/device.models';
import { AgentApplicationType } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EdgeCreateDialogService } from '@home/pages/edge/edge-create-dialog.service';
import { AgentGatewayCreateDialogComponent } from '@home/components/agent/dialog/agent-gateway-create-dialog.component';
import { PageLink } from '@shared/models/page/page-link';
import { emptyPageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';

interface RelatedOption {
  id: string;
  name: string;
  entityType: EntityType;
}

interface RelatedSearch {
  text: string;
  targetType: EntityType | null;
}

@Component({
  selector: 'tb-agent-related-entity-autocomplete',
  templateUrl: './agent-related-entity-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AgentRelatedEntityAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class AgentRelatedEntityAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  @Input() appType: AgentApplicationType;

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input() labelKey: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  @coerceBoolean()
  allowCreate = false;

  // Emits the display name of the selected entity (null when cleared) so callers
  // can derive defaults (e.g. an app name) — the value accessor only carries the id.
  @Output() relatedEntityNameChange = new EventEmitter<string | null>();

  selectFormGroup: UntypedFormGroup;
  filteredOptions: Observable<RelatedOption[]>;

  @ViewChild('relatedEntityInput', { static: true }) relatedEntityInput: ElementRef<HTMLInputElement>;

  private dirty = false;
  private propagateChange: (value: EntityId | null) => void = () => {};
  private modelValue: EntityId | null = null;

  constructor(private entityService: EntityService,
              private agentService: AgentService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private edgeCreateDialogService: EdgeCreateDialogService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.selectFormGroup = this.fb.group({ relatedEntity: [null] });
  }

  ngOnInit(): void {
    this.selectFormGroup.get('relatedEntity').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const selected = (value && typeof value !== 'string') ? value as RelatedOption : null;
      const next: EntityId | null = selected ? { id: selected.id, entityType: selected.entityType } : null;
      if (!entityIdEquals(this.modelValue, next)) {
        this.modelValue = next;
        this.propagateChange(next);
        this.relatedEntityNameChange.emit(selected ? selected.name : null);
      }
    });
    this.filteredOptions = this.selectFormGroup.get('relatedEntity').valueChanges.pipe(
      map(value => !value ? '' : (typeof value === 'string' ? value : (value as RelatedOption).name)),
      debounceTime(150),
      map((text): RelatedSearch => ({ text, targetType: this.targetEntityType() })),
      distinctUntilChanged((a, b) => a.text === b.text && a.targetType === b.targetType),
      switchMap(search => this.fetchOptions(search)),
      share()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.appType && !changes.appType.firstChange) {
      this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: true });
      this.dirty = true;
    }
    if (changes.required) {
      this.updateRequiredValidator();
    }
  }

  private updateRequiredValidator(): void {
    const ctrl = this.selectFormGroup.get('relatedEntity');
    if (this.required) {
      ctrl.setValidators([Validators.required]);
    } else {
      ctrl.clearValidators();
    }
    ctrl.updateValueAndValidity({ emitEvent: false });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.selectFormGroup.disable({ emitEvent: false });
    } else {
      this.selectFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: EntityId | null): void {
    this.dirty = true;
    if (!value?.id || !value?.entityType) {
      this.modelValue = null;
      this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: false });
      return;
    }
    this.modelValue = { id: value.id, entityType: value.entityType };
    this.entityService.getEntity(value.entityType as EntityType, value.id,
      { ignoreLoading: true, ignoreErrors: true }).subscribe({
      next: (e: any) => {
        const opt: RelatedOption = {
          id: value.id,
          entityType: value.entityType as EntityType,
          name: e?.name || value.id
        };
        this.selectFormGroup.get('relatedEntity').patchValue(opt, { emitEvent: false });
        this.relatedEntityNameChange.emit(opt.name);
      }
    });
  }

  onFocus(): void {
    if (this.dirty) {
      this.selectFormGroup.get('relatedEntity').updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  displayOptionFn(option?: RelatedOption): string {
    return option ? option.name : '';
  }

  clear() {
    this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.relatedEntityInput.nativeElement.blur();
      this.relatedEntityInput.nativeElement.focus();
    }, 0);
  }

  createEntity($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.relatedEntityInput.nativeElement.blur();
    if (this.appType === AgentApplicationType.EDGE) {
      this.edgeCreateDialogService.create().subscribe(edge => this.onEntityCreated(edge));
    } else if (this.appType === AgentApplicationType.GATEWAY) {
      this.dialog.open<AgentGatewayCreateDialogComponent, any, Device>(
        AgentGatewayCreateDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog']
        }).afterClosed().subscribe(device => this.onEntityCreated(device));
    }
  }

  private onEntityCreated(entity: Edge | Device | undefined) {
    const targetType = this.targetEntityType();
    if (!entity || !targetType) {
      return;
    }
    const option: RelatedOption = { id: entity.id.id, entityType: targetType, name: entity.name };
    this.selectFormGroup.get('relatedEntity').patchValue(option, { emitEvent: true });
  }

  private fetchOptions(search: RelatedSearch): Observable<RelatedOption[]> {
    if (!search.targetType) {
      return of([]);
    }
    const pageLink = new PageLink(10, 0, search.text);
    return this.agentService.getRelatedEntityCandidates(search.targetType, pageLink, this.modelValue?.id,
      { ignoreLoading: true }).pipe(
      catchError(() => of(emptyPageData<EntityInfoData>())),
      map(pageData => pageData.data.map(info => ({ id: info.id.id, entityType: search.targetType, name: info.name })))
    );
  }

  private targetEntityType(): EntityType | null {
    if (this.appType === AgentApplicationType.EDGE) { return EntityType.EDGE; }
    if (this.appType === AgentApplicationType.GATEWAY) { return EntityType.DEVICE; }
    return null;
  }
}
