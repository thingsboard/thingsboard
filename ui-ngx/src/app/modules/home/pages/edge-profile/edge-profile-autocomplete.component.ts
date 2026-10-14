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
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { emptyPageData } from '@shared/models/page/page-data';
import { EdgeProfileId } from '@shared/models/id/edge-profile-id';
import { EdgeProfile } from '@shared/models/edge-profile.models';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { Operation, Resource } from '@shared/models/security.models';
import { getEntityDetailsPageURL } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  EdgeProfileDialogComponent,
  EdgeProfileDialogData
} from '@home/pages/edge-profile/edge-profile-dialog.component';

@Component({
  selector: 'tb-edge-profile-autocomplete',
  templateUrl: './edge-profile-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EdgeProfileAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class EdgeProfileAutocompleteComponent implements ControlValueAccessor, OnInit {

  resource = Resource;
  operation = Operation;

  selectEdgeProfileFormGroup: UntypedFormGroup;

  modelValue: EdgeProfileId | null;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  selectDefaultProfile = false;

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

  @Input()
  @coerceBoolean()
  addNewProfile = false;

  @Input()
  @coerceBoolean()
  editProfileEnabled = false;

  get edgeProfileURL(): string | null {
    return this.modelValue ? getEntityDetailsPageURL(this.modelValue.id, this.modelValue.entityType) : null;
  }

  @Output()
  edgeProfileChanged = new EventEmitter<EdgeProfile>();

  @ViewChild('edgeProfileInput', { static: true }) edgeProfileInput: ElementRef;
  @ViewChild('edgeProfileAutocomplete', { static: true }) edgeProfileAutocomplete: MatAutocomplete;

  filteredEdgeProfiles: Observable<Array<EdgeProfile>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (_: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private edgeProfileService: EdgeProfileService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    this.selectEdgeProfileFormGroup = this.fb.group({
      edgeProfile: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredEdgeProfiles = this.selectEdgeProfileFormGroup.get('edgeProfile').valueChanges.pipe(
      tap((value: EdgeProfile | string) => {
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
      switchMap(name => this.fetchEdgeProfiles(name)),
      share()
    );
  }

  selectDefaultEdgeProfileIfNeeded(): void {
    if (this.selectDefaultProfile && !this.modelValue) {
      this.edgeProfileService.getDefaultEdgeProfile().subscribe(profile => {
        if (profile) {
          this.selectEdgeProfileFormGroup.get('edgeProfile').patchValue(profile, { emitEvent: false });
          this.updateView(profile);
        }
      });
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectEdgeProfileFormGroup.disable({ emitEvent: false });
    } else {
      this.selectEdgeProfileFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: EdgeProfileId | null): void {
    this.searchText = '';
    if (value != null) {
      this.edgeProfileService.getEdgeProfile(value.id).subscribe(profile => {
        this.modelValue = new EdgeProfileId(profile.id.id);
        this.selectEdgeProfileFormGroup.get('edgeProfile').patchValue(profile, { emitEvent: false });
        this.edgeProfileChanged.emit(profile);
      });
    } else {
      this.modelValue = null;
      this.selectEdgeProfileFormGroup.get('edgeProfile').patchValue(null, { emitEvent: false });
      this.selectDefaultEdgeProfileIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectEdgeProfileFormGroup.get('edgeProfile').updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  updateView(edgeProfile: EdgeProfile | null) {
    const idValue = edgeProfile && edgeProfile.id ? new EdgeProfileId(edgeProfile.id.id) : null;
    if (!entityIdEquals(this.modelValue, idValue)) {
      this.modelValue = idValue;
      this.propagateChange(this.modelValue);
      this.edgeProfileChanged.emit(edgeProfile);
    }
  }

  displayEdgeProfileFn(profile?: EdgeProfile): string | undefined {
    return profile ? profile.name : undefined;
  }

  fetchEdgeProfiles(searchText?: string): Observable<Array<EdgeProfile>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.edgeProfileService.getEdgeProfiles(pageLink, { ignoreLoading: true }).pipe(
      catchError(() => of(emptyPageData<EdgeProfile>())),
      map(pageData => pageData.data)
    );
  }

  clear() {
    this.selectEdgeProfileFormGroup.get('edgeProfile').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.edgeProfileInput.nativeElement.blur();
      this.edgeProfileInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  createEdgeProfile($event: Event, profileName: string) {
    $event.stopPropagation();
    if (this.addNewProfile) {
      this.openEdgeProfileDialog({name: profileName} as EdgeProfile, true);
    }
  }

  editEdgeProfile($event: Event) {
    $event.stopPropagation();
    this.edgeProfileService.getEdgeProfile(this.modelValue.id).subscribe(
      (edgeProfile) => {
        this.openEdgeProfileDialog(edgeProfile, false);
      }
    );
  }

  private openEdgeProfileDialog(edgeProfile: EdgeProfile, isAdd: boolean) {
    this.dialog.open<EdgeProfileDialogComponent, EdgeProfileDialogData, EdgeProfile>(EdgeProfileDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {edgeProfile, isAdd}
    }).afterClosed().subscribe((saved) => {
      if (saved) {
        this.selectEdgeProfileFormGroup.get('edgeProfile').patchValue(saved, {emitEvent: true});
      }
    });
  }
}
