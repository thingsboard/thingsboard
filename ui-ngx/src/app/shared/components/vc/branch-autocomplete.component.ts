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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { BranchInfo } from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';

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

  @Input()
  disabled: boolean;

  @Input()
  selectDefaultBranch = true;

  @ViewChild('branchInput', {static: true}) branchInput: ElementRef;

  filteredBranches: Observable<Array<string>>;

  branches: Observable<Array<BranchInfo>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private fb: FormBuilder) {
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

    this.branches = null;
    this.filteredBranches = this.branchFormGroup.get('branch').valueChanges
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(value => {
          this.updateView(value);
        }),
        map(value => value ? value : ''),
        switchMap(branch => this.fetchBranches(branch))
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.branchFormGroup.disable({emitEvent: false});
    } else {
      this.branchFormGroup.enable({emitEvent: false});
    }
  }

  selectDefaultBranchIfNeeded(): void {
    if (this.selectDefaultBranch && !this.modelValue) {
      this.getBranches().subscribe(
        (data) => {
          if (data && data.length) {
            const defaultBranch = data.find(branch => branch.default);
            if (defaultBranch) {
              this.modelValue = defaultBranch.name;
              this.branchFormGroup.get('branch').patchValue(this.modelValue, {emitEvent: false});
              this.propagateChange(this.modelValue);
            }
          }
        }
      );
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    if (value != null) {
      this.branchFormGroup.get('branch').patchValue(value, {emitEvent: false});
    } else {
      this.branchFormGroup.get('branch').patchValue('', {emitEvent: false});
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

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayBranchFn(branch?: string): string | undefined {
    return branch ? branch : undefined;
  }

  fetchBranches(searchText?: string): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getBranches().pipe(
      map(branches => branches.map(branch => branch.name).filter(branchName => {
        return searchText ? branchName.toUpperCase().startsWith(searchText.toUpperCase()) : true;
      }))
    );
  }

  getBranches(): Observable<Array<BranchInfo>> {
    if (!this.branches) {
      const branchesObservable = this.entitiesVersionControlService.listBranches({ignoreLoading: true, ignoreErrors: true});
      this.branches = branchesObservable.pipe(
        catchError(() => of([] as Array<BranchInfo>)),
        publishReplay(1),
        refCount()
      );
    }
    return this.branches;
  }

  clear() {
    this.branchFormGroup.get('branch').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.branchInput.nativeElement.blur();
      this.branchInput.nativeElement.focus();
    }, 0);
  }

}
