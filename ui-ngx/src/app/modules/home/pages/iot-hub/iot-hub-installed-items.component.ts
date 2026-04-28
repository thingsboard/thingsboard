///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { TbIotHubInstalledItemsTableComponent } from '@home/components/iot-hub/iot-hub-installed-items-table.component';

@Component({
  selector: 'tb-iot-hub-installed-items',
  standalone: false,
  templateUrl: './iot-hub-installed-items.component.html',
  styleUrls: ['./iot-hub-installed-items.component.scss']
})
export class TbIotHubInstalledItemsComponent implements OnInit, OnDestroy {

  textSearch = '';
  appliedTextSearch = '';
  typeFilters: string[] = [];

  activeTypeFilters = new Set<string>();
  allItemTypes: string[] = ['WIDGET', 'DASHBOARD', 'SOLUTION_TEMPLATE', 'CALCULATED_FIELD', 'RULE_CHAIN', 'DEVICE'];

  @ViewChild(TbIotHubInstalledItemsTableComponent) tableComponent: TbIotHubInstalledItemsTableComponent;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private translate: TranslateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const itemType = this.route.snapshot.queryParamMap.get('itemType');
    if (itemType && this.allItemTypes.includes(itemType)) {
      this.activeTypeFilters.add(itemType);
      this.typeFilters = Array.from(this.activeTypeFilters);
    }
    this.searchSubject.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.appliedTextSearch = value;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  navigateToMarketplace(): void {
    void this.router.navigate(['/iot-hub']);
  }

  onSearchChange(value: string): void {
    this.textSearch = value;
    this.searchSubject.next(value);
  }

  isTypeFilterActive(type: string): boolean {
    return this.activeTypeFilters.has(type);
  }

  toggleTypeFilter(type: string): void {
    if (this.activeTypeFilters.has(type)) {
      this.activeTypeFilters.delete(type);
    } else {
      this.activeTypeFilters.add(type);
    }
    this.typeFilters = Array.from(this.activeTypeFilters);
  }

  removeTypeFilter(type: string): void {
    this.activeTypeFilters.delete(type);
    this.typeFilters = Array.from(this.activeTypeFilters);
  }

  clearAllFilters(): void {
    this.activeTypeFilters.clear();
    this.typeFilters = [];
  }

  hasActiveFilters(): boolean {
    return this.activeTypeFilters.size > 0;
  }

  getItemTypeLabel(itemType: string): string {
    const key = itemTypeTranslations.get(itemType as ItemType);
    return key ? this.translate.instant(key) : itemType;
  }

  checkForUpdates(): void {
    this.tableComponent?.checkForUpdates();
  }

  get isCheckingUpdates(): boolean {
    return this.tableComponent?.isCheckingUpdates || false;
  }

  get updatesChecked(): boolean {
    return this.tableComponent?.updatesChecked || false;
  }
}
