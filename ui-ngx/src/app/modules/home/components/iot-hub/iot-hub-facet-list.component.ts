// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FilterParamInfo } from '@shared/models/iot-hub/iot-hub-item.models';

/**
 * One checkbox facet of a filter panel: a title, the options with their item counts, and the
 * set of keys currently selected. Presentational only - it owns no filter state and issues no
 * request; the host decides what a toggle means.
 *
 * Written for the cross-type search page, which needs three plain facets (item type, category,
 * use case). The type pages keep their own panel in TbIotHubBrowseComponent: theirs carries
 * facets this one has no notion of - grouped connectivity, vendor lists, per-type subtypes -
 * so folding the two together would mean parameterising that panel for a shape it never uses.
 */
@Component({
  selector: 'tb-iot-hub-facet-list',
  standalone: false,
  templateUrl: './iot-hub-facet-list.component.html',
  styleUrls: ['./iot-hub-facet-list.component.scss']
})
export class TbIotHubFacetListComponent {

  /** Translation key for the facet's heading. */
  @Input() label: string;

  @Input() options: FilterParamInfo[] = [];

  /** Keys the host currently has selected. Read, never written. */
  @Input() selected = new Set<string>();

  /**
   * Turns an option key into what the user reads. Defaults to the key, which is already the
   * display value for categories and use cases; item types come through as enum names and need
   * their translated plural.
   */
  @Input() labelFor: (key: string) => string = (key) => key;

  /**
   * Open on first render. The host decides: a short, always-relevant facet earns the space it
   * takes, a long one costs the reader the facets below it — on the search page all three
   * headings fit above the fold only because two of them start closed.
   */
  @Input() expanded = true;

  @Output() toggled = new EventEmitter<string>();

  /**
   * Above this many options the facet gets its own search box and a scroll cap. Matches the
   * threshold the type pages' panel uses, so the two feel the same at the same list lengths.
   */
  private static readonly SEARCH_THRESHOLD = 8;

  search = '';

  get searchable(): boolean {
    return this.options.length > TbIotHubFacetListComponent.SEARCH_THRESHOLD;
  }

  get visibleOptions(): FilterParamInfo[] {
    const needle = this.search.trim().toLowerCase();
    if (!needle) {
      return this.options;
    }
    return this.options.filter(o => this.labelFor(o.key).toLowerCase().includes(needle));
  }

  get scrollable(): boolean {
    return this.visibleOptions.length > TbIotHubFacetListComponent.SEARCH_THRESHOLD;
  }

  isSelected(key: string): boolean {
    return this.selected.has(key);
  }

  onToggle(key: string): void {
    this.toggled.emit(key);
  }

  trackByKey(_index: number, option: FilterParamInfo): string {
    return option.key;
  }
}
