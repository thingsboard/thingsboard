///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  Component,
  ElementRef,
  forwardRef,
  Input,
  NgZone,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { BranchInfo } from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { isNotEmptyStr } from '@core/utils';
import { MatAutocomplete, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';

@Component({
  selector: 'tb-branch-autocomplete',
  templateUrl: './branch-autocomplete.component.html',
  styleUrls: ['./branch-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => BranchAutocompleteComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class BranchAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  branchFormGroup: UntypedFormGroup;

  modelValue: string | null;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

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

  @Input()
  emptyPlaceholder: string;

  @Input()
  appearance: MatFormFieldAppearance = 'outline';

  @ViewChild('branchAutocomplete') matAutocomplete: MatAutocomplete;
  @ViewChild('branchInput', { read: MatAutocompleteTrigger, static: true }) autoCompleteTrigger: MatAutocompleteTrigger;
  @ViewChild('branchInput', {static: true}) branchInput: ElementRef<HTMLInputElement>;

  filteredBranches: Observable<Array<BranchInfo>>;

  defaultBranch: BranchInfo = null;

  searchText = '';

  loading = false;

  private dirty = false;

  private clearButtonClicked = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private fb: UntypedFormBuilder,
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
          if (!this.selectionMode || modelValue) {
            this.updateView(modelValue);
          }
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

  isDefaultBranchSelected(): boolean {
    return this.defaultBranch && this.defaultBranch.name === this.modelValue;
  }

  selectDefaultBranchIfNeeded(force = false): void {
    if ((this.selectDefaultBranch && !this.modelValue) || force) {
      setTimeout(() => {
        if (this.defaultBranch) {
          this.branchFormGroup.get('branch').patchValue(this.defaultBranch, {emitEvent: false});
          this.modelValue = this.defaultBranch?.name;
          this.propagateChange(this.modelValue);
        } else {
          this.loading = true;
          this.getBranches().subscribe(
            () => {
              if (this.defaultBranch || force) {
                this.branchFormGroup.get('branch').patchValue(this.defaultBranch, {emitEvent: false});
                this.modelValue = this.defaultBranch?.name;
                this.propagateChange(this.modelValue);
                this.loading = false;
              } else {
                this.loading = false;
              }
            }
          );
        }
      });
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

  onBlur() {
    if (this.clearButtonClicked) {
      this.clearButtonClicked = false;
    } else if (!this.matAutocomplete.isOpen) {
      this.selectAvailableValue();
    }
  }

  onPanelClosed() {
    this.selectAvailableValue();
  }

  selectAvailableValue() {
    if (this.selectionMode) {
      const branch = this.branchFormGroup.get('branch').value;
      this.getBranches().pipe(
        map(branches => {
          let foundBranch = branches.find(b => b.name === branch);
          if (!foundBranch && isNotEmptyStr(this.modelValue)) {
            foundBranch = branches.find(b => b.name === this.modelValue);
          }
          return foundBranch;
        })
      ).subscribe((val) => {
        if (!val && this.defaultBranch) {
          val = this.defaultBranch;
        }
        this.zone.run(() => {
          this.branchFormGroup.get('branch').patchValue(val, {emitEvent: true});
        }, 0);
      });
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

  private fetchBranches(searchText?: string): Observable<Array<BranchInfo>> {
    this.searchText = searchText;
    return this.getBranches().pipe(
      map(branches => {
          let res = branches.filter(branch => searchText ? branch.name.toUpperCase().startsWith(searchText.toUpperCase()) : true);
          if (!this.selectionMode && isNotEmptyStr(searchText) && !res.find(b => b.name === searchText)) {
            res = [{name: searchText, default: false}, ...res];
          }
          return res;
        }
      )
    );
  }

  private getBranches(): Observable<Array<BranchInfo>> {
    return this.entitiesVersionControlService.listBranches().pipe(
      tap((data) => {
        this.defaultBranch = data.find(branch => branch.default);
      })
    );
  }

  clear() {
    this.clearButtonClicked = true;
    setTimeout(() => {
      this.branchFormGroup.get('branch').patchValue(null, {emitEvent: true});
      this.branchInput.nativeElement.blur();
      this.branchInput.nativeElement.focus();
    }, 0);
  }

}
