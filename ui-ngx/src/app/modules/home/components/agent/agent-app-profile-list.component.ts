// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { BehaviorSubject, combineLatest, forkJoin, Observable, of } from 'rxjs';
import { catchError, filter, map, share, startWith, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { PageLink } from '@shared/models/page/page-link';
import {
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  isVirtualAppProfile,
  VirtualAgentAppProfile
} from '@shared/models/agent.models';
import {
  appProfileSearchMatches,
  appProfileTemplateLabel,
  appProfileTrackKey,
  buildVirtualAppProfiles,
  VIRTUAL_APP_TYPES
} from '@home/pages/agent/util/virtual-app-profiles';
import { EdgeTemplateCompatibilityService } from '@home/pages/agent/util/edge-template-compatibility.service';

// Chips multi-select over the tenant's agent app profiles (value: array of profile id strings).
// Alongside real profiles, offers a virtual "Predefined" entry per template version that has no
// profile yet; selecting one materializes a real profile via the materialize endpoint first.
@Component({
  selector: 'tb-agent-app-profile-list',
  templateUrl: './agent-app-profile-list.component.html',
  styleUrls: ['./agent-app-profile-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AgentAppProfileListComponent),
      multi: true
    }
  ],
  standalone: false
})
export class AgentAppProfileListComponent implements ControlValueAccessor, OnInit {

  // The picker loads a single page and filters client-side (needed to overlay virtual entries);
  // tenants with more profiles than this see a silently truncated list.
  private static readonly PROFILES_PAGE_SIZE = 1024;

  @Input() labelText: string;
  @Input() placeholderText = '';
  @Input() appearance: MatFormFieldAppearance = 'outline';

  disabled = false;

  readonly canCreateAppProfile: boolean;

  listFormGroup: UntypedFormGroup;
  selected: AgentAppProfile[] = [];
  filteredEntries: Observable<AgentAppProfile[]>;
  materializing = false;
  errorText = '';

  readonly trackKey = appProfileTrackKey;

  @ViewChild('profileInput') profileInput: ElementRef<HTMLInputElement>;

  private modelValue: string[] | null = null;
  private realProfiles: AgentAppProfile[] = [];
  private templatesByType = new Map<AgentApplicationType, AgentAppTemplate[]>();
  private allEntries$ = new BehaviorSubject<AgentAppProfile[]>([]);
  private entriesLoaded = false;
  private pendingSelectionIds: string[] | null = null;

  private propagateChange = (_v: any) => {};

  constructor(private agentService: AgentService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private edgeTemplateCompatibility: EdgeTemplateCompatibilityService,
              private fb: UntypedFormBuilder) {
    this.canCreateAppProfile = this.userPermissionsService.hasGenericPermission(Resource.AGENT_APP_PROFILE, Operation.CREATE);
    this.listFormGroup = this.fb.group({ entities: [this.selected], entity: [null] });
  }

  ngOnInit() {
    this.loadEntries();
    const text$ = this.listFormGroup.get('entity').valueChanges.pipe(
      startWith(''),
      tap(value => {
        if (value && typeof value !== 'string') {
          this.onEntrySelected(value as AgentAppProfile);
        }
      }),
      filter(value => typeof value === 'string' || value === null),
      map(value => typeof value === 'string' ? value : '')
    );
    this.filteredEntries = combineLatest([this.allEntries$, text$]).pipe(
      map(([all, text]) => this.filterEntries(all, text)),
      share()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.listFormGroup.disable({ emitEvent: false });
    } else {
      this.listFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: string[] | null): void {
    this.modelValue = value?.length ? [...value] : null;
    if (value?.length) {
      if (this.entriesLoaded) {
        this.resolveSelection(value);
      } else {
        this.pendingSelectionIds = [...value];
      }
    } else {
      this.pendingSelectionIds = null;
      this.selected = [];
      this.listFormGroup.get('entities').setValue(this.selected);
    }
    if (this.profileInput) {
      this.profileInput.nativeElement.value = '';
    }
  }

  // Resolves selected ids against the already-loaded profile page; only ids not in the page are
  // fetched individually, and each failed fetch (e.g. a concurrently deleted profile) is dropped
  // from the selection instead of blanking it entirely.
  private resolveSelection(ids: string[]) {
    const byId = new Map(this.realProfiles.map(p => [p.id.id, p]));
    const missing = ids.filter(id => !byId.has(id));
    if (!missing.length) {
      this.applySelection(ids.map(id => byId.get(id)));
      return;
    }
    forkJoin(missing.map(id =>
      this.agentService.getAgentAppProfileById(id).pipe(catchError(() => of<AgentAppProfile | null>(null)))
    )).subscribe(fetched => {
      const found = fetched.filter((p): p is AgentAppProfile => !!p);
      found.forEach(p => byId.set(p.id.id, p));
      this.mergeRealProfiles(found);
      this.applySelection(ids.map(id => byId.get(id)).filter(p => !!p));
    });
  }

  private applySelection(profiles: AgentAppProfile[]) {
    this.selected = profiles;
    this.listFormGroup.get('entities').setValue(this.selected);
    const resolvedIds = profiles.map(p => p.id.id);
    // Deliberate deviation from the "writeValue must not emit" CVA rule: stale ids are pruned
    // and propagated so they can't be submitted. The length-diff guard below is what prevents
    // an emit-loop with the parent form — don't weaken it.
    if ((this.modelValue?.length || 0) !== resolvedIds.length) {
      this.modelValue = resolvedIds.length ? resolvedIds : null;
      this.propagateChange(this.modelValue);
    }
  }

  isVirtual(profile: AgentAppProfile): boolean {
    return isVirtualAppProfile(profile);
  }

  templateLabel(profile: AgentAppProfile): string {
    return appProfileTemplateLabel(profile, this.translate);
  }

  displayFn(profile?: AgentAppProfile): string | undefined {
    return profile ? profile.name : undefined;
  }

  remove(profile: AgentAppProfile) {
    const index = this.selected.indexOf(profile);
    if (index >= 0) {
      this.selected.splice(index, 1);
      this.listFormGroup.get('entities').setValue(this.selected);
      this.modelValue = this.selected.length ? this.selected.map(p => p.id.id) : null;
      this.propagateChange(this.modelValue);
      this.clearInput();
    }
  }

  private onEntrySelected(entry: AgentAppProfile) {
    if (isVirtualAppProfile(entry)) {
      this.materialize(entry);
    } else {
      this.addProfile(entry);
    }
  }

  private materialize(entry: VirtualAgentAppProfile) {
    this.materializing = true;
    this.errorText = '';
    this.agentService.materializeAgentAppProfile(entry.appType, entry.templateVersion, entry.defaultComposeType).subscribe({
      next: profile => {
        this.materializing = false;
        this.mergeRealProfiles([profile]);
        this.addProfile(profile);
      },
      error: () => {
        this.materializing = false;
        this.errorText = this.translate.instant('agent.app-install-profile-materialize-failed');
        this.clearInput();
      }
    });
  }

  private addProfile(profile: AgentAppProfile) {
    if (!this.modelValue) {
      this.modelValue = [];
    }
    if (this.modelValue.indexOf(profile.id.id) === -1) {
      this.modelValue.push(profile.id.id);
      this.selected.push(profile);
      this.listFormGroup.get('entities').setValue(this.selected);
    }
    this.propagateChange(this.modelValue);
    this.clearInput();
  }

  private filterEntries(all: AgentAppProfile[], text: string): AgentAppProfile[] {
    const selectedIds = new Set((this.modelValue || []));
    let entries = all.filter(p => isVirtualAppProfile(p) || !selectedIds.has(p.id.id));
    if (text?.length) {
      entries = entries.filter(p => appProfileSearchMatches(p, text));
    }
    return entries;
  }

  private loadEntries() {
    forkJoin([
      this.agentService.getTenantAgentAppProfiles(new PageLink(AgentAppProfileListComponent.PROFILES_PAGE_SIZE))
        .pipe(catchError(() => of(null))),
      ...VIRTUAL_APP_TYPES.map(type => this.canCreateAppProfile
        ? this.agentService.getAgentAppTemplatesByAppType(type).pipe(catchError(() => of([] as AgentAppTemplate[])))
        : of([] as AgentAppTemplate[]))
    ]).subscribe(([profilesPage, ...templateLists]) => {
      this.realProfiles = profilesPage?.data || [];
      VIRTUAL_APP_TYPES.forEach((type, i) =>
        this.templatesByType.set(type, templateLists[i] as AgentAppTemplate[]));
      this.refreshEntries();
      this.entriesLoaded = true;
      if (this.pendingSelectionIds?.length) {
        const pending = this.pendingSelectionIds;
        this.pendingSelectionIds = null;
        this.resolveSelection(pending);
      }
    });
  }

  private mergeRealProfiles(profiles: AgentAppProfile[]) {
    const known = new Set(this.realProfiles.map(p => p.id.id));
    const added = profiles.filter(p => p?.id && !known.has(p.id.id));
    if (added.length) {
      this.realProfiles = [...this.realProfiles, ...added];
      this.refreshEntries();
    }
  }

  private refreshEntries() {
    const addOnEdge = this.edgeTemplateCompatibility.isAddOnEdgeByDefault();
    const entries: AgentAppProfile[] = this.edgeTemplateCompatibility.filterProfiles(this.realProfiles, addOnEdge);
    VIRTUAL_APP_TYPES.forEach(type =>
      entries.push(...buildVirtualAppProfiles(
        this.edgeTemplateCompatibility.filterTemplates(this.templatesByType.get(type) || [], addOnEdge),
        this.realProfiles.filter(p => p.appType === type))));
    this.allEntries$.next(entries);
  }

  private clearInput(value: string = '') {
    if (this.profileInput) {
      this.profileInput.nativeElement.value = value;
    }
    this.listFormGroup.get('entity').patchValue(value, { emitEvent: true });
  }
}
