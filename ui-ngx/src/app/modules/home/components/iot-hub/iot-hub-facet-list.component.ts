// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { filterByLabel, IOT_HUB_FILTER_SCROLL_THRESHOLD } from '@home/components/iot-hub/iot-hub-utils';

/**
 * One checkbox facet of a filter panel: a title, the option keys, and the set of keys currently
 * selected. Presentational only - the host decides what a toggle means.
 */
@Component({
  selector: 'tb-iot-hub-facet-list',
  standalone: false,
  templateUrl: './iot-hub-facet-list.component.html',
  styleUrls: ['./iot-hub-facet-list.component.scss']
})
export class TbIotHubFacetListComponent implements OnChanges {

  /** Translation key for the facet's heading. */
  @Input() label: string;

  @Input() options: string[] = [];

  /** Keys the host has selected. The host hands over a new Set on every toggle. */
  @Input() selected = new Set<string>();

  /** Turns an option key into what the user reads. */
  @Input() labelFor: (key: string) => string = (key) => key;

  @Input() expanded = true;

  @Output() toggled = new EventEmitter<string>();

  search = '';
  searchable = false;
  visibleOptions: string[] = [];
  scrollable = false;

  ngOnChanges(): void {
    this.searchable = this.options.length > IOT_HUB_FILTER_SCROLL_THRESHOLD;
    this.applySearch();
  }

  onSearchChange(search: string): void {
    this.search = search;
    this.applySearch();
  }

  isSelected(key: string): boolean {
    return this.selected.has(key);
  }

  onToggle(key: string): void {
    this.toggled.emit(key);
  }

  private applySearch(): void {
    this.visibleOptions = filterByLabel(this.options, this.search, this.labelFor);
    this.scrollable = this.visibleOptions.length > IOT_HUB_FILTER_SCROLL_THRESHOLD;
  }
}
