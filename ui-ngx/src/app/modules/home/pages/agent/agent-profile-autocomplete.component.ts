// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { MatDialog } from '@angular/material/dialog';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { emptyPageData } from '@shared/models/page/page-data';
import { AgentProfileId } from '@shared/models/id/agent-profile-id';
import { AgentProfile, AgentProfileInfo } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import {
  AgentProfileWizardComponent,
  AgentProfileWizardData
} from '@home/pages/agent/wizard/agent-profile-wizard.component';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { Operation, Resource } from '@shared/models/security.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-agent-profile-autocomplete',
  templateUrl: './agent-profile-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AgentProfileAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class AgentProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  resource = Resource;
  operation = Operation;

  selectAgentProfileFormGroup: UntypedFormGroup;

  modelValue: AgentProfileId | null;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  selectDefaultProfile = false;

  @Input()
  editProfileEnabled = true;

  @Input()
  addNewProfile = true;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  @Input()
  hint: string;

  @Input()
  appearance: MatFormFieldAppearance = 'outline';

  @Input()
  @coerceBoolean()
  showDetailsPageLink = false;

  get agentProfileURL(): string | null {
    return this.modelValue ? getEntityDetailsPageURL(this.modelValue.id, this.modelValue.entityType) : null;
  }

  @Output()
  agentProfileUpdated = new EventEmitter<AgentProfileId>();

  @Output()
  agentProfileChanged = new EventEmitter<AgentProfileInfo>();

  @ViewChild('agentProfileInput', { static: true }) agentProfileInput: ElementRef;
  @ViewChild('agentProfileAutocomplete', { static: true }) agentProfileAutocomplete: MatAutocomplete;

  filteredAgentProfiles: Observable<Array<AgentProfileInfo>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    this.selectAgentProfileFormGroup = this.fb.group({
      agentProfile: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredAgentProfiles = this.selectAgentProfileFormGroup.get('agentProfile').valueChanges.pipe(
      tap((value: AgentProfileInfo | string) => {
        const modelValue = (typeof value === 'string' || !value) ? null : value;
        this.updateView(modelValue);
      }),
      map(value => {
        if (!value) {
          return '';
        }
        return typeof value === 'string' ? value : value.name;
      }),
      debounceTime(150),
      distinctUntilChanged(),
      switchMap(name => this.fetchAgentProfiles(name)),
      share()
    );
  }

  selectDefaultAgentProfileIfNeeded(): void {
    if (this.selectDefaultProfile && !this.modelValue) {
      this.agentService.getDefaultAgentProfileInfo().subscribe(profile => {
        if (profile) {
          this.selectAgentProfileFormGroup.get('agentProfile').patchValue(profile, { emitEvent: false });
          this.updateView(profile);
        }
      });
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectAgentProfileFormGroup.disable({ emitEvent: false });
    } else {
      this.selectAgentProfileFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: AgentProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.agentService.getAgentProfileInfoById(value.id).subscribe(profile => {
        this.modelValue = new AgentProfileId(profile.id.id);
        this.selectAgentProfileFormGroup.get('agentProfile').patchValue(profile, { emitEvent: false });
        this.agentProfileChanged.emit(profile);
      });
    } else {
      this.modelValue = null;
      this.selectAgentProfileFormGroup.get('agentProfile').patchValue(null, { emitEvent: false });
      this.selectDefaultAgentProfileIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectAgentProfileFormGroup.get('agentProfile').updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  updateView(agentProfile: AgentProfileInfo | null) {
    const idValue = agentProfile && agentProfile.id ? new AgentProfileId(agentProfile.id.id) : null;
    if (!entityIdEquals(this.modelValue, idValue)) {
      this.modelValue = idValue;
      this.propagateChange(this.modelValue);
      this.agentProfileChanged.emit(agentProfile);
    }
  }

  displayAgentProfileFn(profile?: AgentProfileInfo): string | undefined {
    return profile ? profile.name : undefined;
  }

  fetchAgentProfiles(searchText?: string): Observable<Array<AgentProfileInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.agentService.getTenantAgentProfileInfos(pageLink, { ignoreLoading: true }).pipe(
      catchError(() => of(emptyPageData<AgentProfileInfo>())),
      map(pageData => pageData.data)
    );
  }

  clear() {
    this.selectAgentProfileFormGroup.get('agentProfile').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.agentProfileInput.nativeElement.blur();
      this.agentProfileInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  agentProfileEnter($event: KeyboardEvent) {
    if (this.editProfileEnabled && $event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createAgentProfile($event, this.searchText);
      }
    }
  }

  createAgentProfile($event: Event, profileName: string) {
    $event.stopPropagation();
    const agentProfile: AgentProfile = {
      name: profileName
    } as AgentProfile;
    if (this.addNewProfile) {
      this.openAgentProfileDialog(agentProfile, true);
    }
  }

  editAgentProfile($event: Event) {
    $event.stopPropagation();
    this.agentService.getAgentProfileById(this.modelValue.id).subscribe(agentProfile => {
      this.openAgentProfileDialog(agentProfile, false);
    });
  }

  openAgentProfileDialog(agentProfile: AgentProfile, isAdd: boolean) {
    const data: AgentProfileWizardData = isAdd
      ? { defaults: { name: agentProfile?.name } }
      : { profile: agentProfile };
    const afterClosed$ = this.dialog.open<AgentProfileWizardComponent, AgentProfileWizardData, AgentProfile>(
      AgentProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data
      }
    ).afterClosed();
    afterClosed$.subscribe(savedAgentProfile => {
      if (!savedAgentProfile) {
        setTimeout(() => {
          this.agentProfileInput.nativeElement.blur();
          this.agentProfileInput.nativeElement.focus();
        }, 0);
      } else {
        this.agentService.getAgentProfileInfoById(savedAgentProfile.id.id).subscribe(profile => {
          this.modelValue = new AgentProfileId(profile.id.id);
          this.selectAgentProfileFormGroup.get('agentProfile').patchValue(profile, { emitEvent: true });
          if (isAdd) {
            this.propagateChange(this.modelValue);
          } else {
            this.agentProfileUpdated.next(savedAgentProfile.id);
          }
          this.agentProfileChanged.emit(profile);
        });
      }
    });
  }
}
