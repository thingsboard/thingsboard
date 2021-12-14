///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  GlobalSearchEntityTypes,
  GlobalSearchEntityTypesTranslation,
  GlobalSearchInfo
} from '@shared/models/global-search.models';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-global-search-table-header',
  templateUrl: './global-search-table-header.component.html',
  styleUrls: ['global-search-table-header.component.scss']
})
export class GlobalSearchTableHeaderComponent extends EntityTableHeaderComponent<GlobalSearchInfo> {

  entityFormGroup: FormGroup;

  globalSearchTypes = Object.keys(GlobalSearchEntityTypes);
  globalSearchTypesTranslationMap = GlobalSearchEntityTypesTranslation;

  focus = false;

  searchPlaceholder: string;
  searchBundle: string;
  searchText: string;

  private propagateChange = (v: any) => {};

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private translate: TranslateService) {
    super(store);
    this.entityFormGroup = this.fb.group({
      entityType: [GlobalSearchEntityTypes.DEVICE],
      searchQuery: ''
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  ngOnInit() {
    this.searchPlaceholder = this.getSearchPlaceholder();
    this.entityFormGroup.get('entityType').valueChanges.subscribe(
      value => {
        this.entitiesTableConfig.componentsData.entityType = value;
        this.searchPlaceholder = this.getSearchPlaceholder();
        this.updateTable();
      }
    );

    this.entityFormGroup.get('searchQuery').valueChanges
      .pipe(tap(value => this.searchText = value), debounceTime(150), distinctUntilChanged())
      .subscribe(value => {
        if (this.entitiesTableConfig.componentsData.searchQuery !== value) {
          this.entitiesTableConfig.componentsData.searchQuery = value;
          this.updateTable();
        }
      });
  }

  getSearchPlaceholder = () =>
    this.translate.instant('global-search.search-placeholder',
      {type: this.translate.instant(this.globalSearchTypesTranslationMap.get(this.entityFormGroup.get('entityType').value))})

  updateTable() {
    if (this.entitiesTableConfig.table.displayPagination) {
      this.entitiesTableConfig.table.paginator.pageIndex = 0;
    }
    this.entitiesTableConfig.table.updateData();
  }

  clear($event: Event): void {
    $event.preventDefault();
    $event.stopPropagation();
    this.entityFormGroup.get('searchQuery').patchValue('', {emitEvent: true});
    this.searchText = '';
  }

  toggleFocus() {
    this.focus = !this.focus;
  }
}
