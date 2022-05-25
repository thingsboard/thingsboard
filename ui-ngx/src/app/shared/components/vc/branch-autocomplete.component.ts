///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  NgZone,
  OnInit,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  share,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { BranchInfo } from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { isNotEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-branch-autocomplete',
  templateUrl: './branch-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => BranchAutocompleteComponent),
    multi: true
  }]
})
export class BranchAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  branchFormGroup: FormGroup;

  modelValue: string | null;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private disabledValue: boolean;

  get disabled(): boolean {
    return this.disabledValue;
  }

  @Input()
  set disabled(value: boolean) {
    this.disabledValue = coerceBooleanProperty(value);
    if (this.disabledValue) {
      this.branchFormGroup.disable({emitEvent: false});
    } else {
      this.branchFormGroup.enable({emitEvent: false});
    }
  }

  @Input()
  selectDefaultBranch = true;

  @Input()
  selectionMode = false;

  @ViewChild('branchInput', {static: true}) branchInput: ElementRef;

  filteredBranches: Observable<Array<BranchInfo>>;

  branches: Observable<Array<BranchInfo>> = null;

  defaultBranch: BranchInfo = null;

  searchText = '';

  private dirty = false;

  private ignoreClosedPanel = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private fb: FormBuilder,
              private zone: NgZone) {
    this.branchFormGroup = this.fb.group({
      branch: [null, []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredBranches = this.branchFormGroup.get('branch').valueChanges
      .pipe(
        tap((value: BranchInfo | string) => {
          let modelValue: BranchInfo | null;
          if (typeof value === 'string' || !value) {
            if (!this.selectionMode && typeof value === 'string' && isNotEmptyStr(value)) {
              modelValue = {name: value, default: false};
            } else {
              modelValue = null;
            }
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => {
          if (value) {
            if (typeof value === 'string') {
              return value;
            } else {
              return value.name;
            }
          } else {
            return '';
          }
        }),
        debounceTime(150),
        distinctUntilChanged(),
        switchMap(name => this.fetchBranches(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  selectDefaultBranchIfNeeded(ignoreLoading = true, force = false): void {
    if ((this.selectDefaultBranch && !this.modelValue) || force) {
      this.getBranches(ignoreLoading).subscribe(
        (data) => {
          if (this.defaultBranch || force) {
            this.branchFormGroup.get('branch').patchValue(this.defaultBranch, {emitEvent: false});
            this.modelValue = this.defaultBranch?.name;
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    if (value != null) {
      this.branchFormGroup.get('branch').patchValue({name: value}, {emitEvent: false});
    } else {
      this.branchFormGroup.get('branch').patchValue(null, {emitEvent: false});
      this.selectDefaultBranchIfNeeded();
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.branchFormGroup.get('branch').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  onPanelClosed() {
    if (this.ignoreClosedPanel) {
      this.ignoreClosedPanel = false;
    } else {
      if (this.selectionMode && !this.branchFormGroup.get('branch').value && this.defaultBranch) {
        this.zone.run(() => {
          this.branchFormGroup.get('branch').patchValue(this.defaultBranch, {emitEvent: true});
        }, 0);
      }
    }
  }

  updateView(value: BranchInfo | null) {
    if (this.modelValue !== value?.name) {
      this.modelValue = value?.name;
      this.propagateChange(this.modelValue);
    }
  }

  displayBranchFn(branch?: BranchInfo): string | undefined {
    return branch ? branch.name : undefined;
  }

  fetchBranches(searchText?: string): Observable<Array<BranchInfo>> {
    this.searchText = searchText;
    return this.getBranches().pipe(
      map(branches => {
          let res = branches.filter(branch => {
            return searchText ? branch.name.toUpperCase().startsWith(searchText.toUpperCase()) : true;
          });
          if (!this.selectionMode && isNotEmptyStr(searchText) && !res.find(b => b.name === searchText)) {
            res = [{name: searchText, default: false}, ...res];
          }
          return res;
        }
      )
    );
  }

  getBranches(ignoreLoading = true): Observable<Array<BranchInfo>> {
    if (!this.branches) {
      const branchesObservable = this.entitiesVersionControlService.listBranches({ignoreLoading, ignoreErrors: true});
      this.branches = branchesObservable.pipe(
        catchError(() => of([] as Array<BranchInfo>)),
        tap((data) => {
          this.defaultBranch = data.find(branch => branch.default);
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.branches;
  }

  clear() {
    this.ignoreClosedPanel = true;
    this.branchFormGroup.get('branch').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.branchInput.nativeElement.blur();
      this.branchInput.nativeElement.focus();
    }, 0);
  }

}
