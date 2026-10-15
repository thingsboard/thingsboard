// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ElementRef, ViewChild } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatChipInputEvent, MatChipGrid } from '@angular/material/chips';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { Observable, of, Subject } from 'rxjs';
import { map, mergeMap, share, startWith } from 'rxjs/operators';

@Component({
    selector: 'tb-navigation-cards-widget-settings',
    templateUrl: './navigation-cards-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class NavigationCardsWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('filterItemsChipList') filterItemsChipList: MatChipGrid;
  @ViewChild('filterItemAutocomplete') filterItemAutocomplete: MatAutocomplete;
  @ViewChild('filterItemInput') filterItemInput: ElementRef<HTMLInputElement>;

  filterItems: Array<string> = ['/devices', '/assets', '/profiles/deviceProfiles'];

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  navigationCardsWidgetSettingsForm: UntypedFormGroup;

  filteredFilterItems: Observable<Array<string>>;

  filterItemSearchText = '';

  filterItemInputChange = new Subject<string>();

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
    this.filteredFilterItems = this.filterItemInputChange
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchFilterItems(name) ),
        share()
      );
  }

  protected settingsForm(): UntypedFormGroup {
    return this.navigationCardsWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      filterType: 'all',
      filter: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.navigationCardsWidgetSettingsForm = this.fb.group({
      filterType: [settings.filterType, []],
      filter: [settings.filter, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['filterType'];
  }

  protected updateValidators(emitEvent: boolean) {
    const filterType: string = this.navigationCardsWidgetSettingsForm.get('filterType').value;
    if (filterType === 'all') {
      this.navigationCardsWidgetSettingsForm.get('filter').disable();
    } else {
      this.navigationCardsWidgetSettingsForm.get('filter').enable();
    }
    this.navigationCardsWidgetSettingsForm.get('filter').updateValueAndValidity({emitEvent});
  }

  private fetchFilterItems(searchText?: string): Observable<Array<string>> {
    this.filterItemSearchText = searchText;
    let result = [...this.filterItems];
    if (this.filterItemSearchText && this.filterItemSearchText.length) {
      result.unshift(this.filterItemSearchText);
      result = result.filter(item => item.includes(this.filterItemSearchText));
    }
    return of(result);
  }

  private addFilterItem(filterItem: string): boolean {
    if (filterItem) {
      const filterItems: string[] = this.navigationCardsWidgetSettingsForm.get('filter').value;
      const index = filterItems.indexOf(filterItem);
      if (index === -1) {
        filterItems.push(filterItem);
        this.navigationCardsWidgetSettingsForm.get('filter').setValue(filterItems);
        this.navigationCardsWidgetSettingsForm.get('filter').markAsDirty();
        return true;
      }
    }
    return false;
  }

  onFilterItemRemoved(filterItem: string): void {
    const filterItems: string[] = this.navigationCardsWidgetSettingsForm.get('filter').value;
    const index = filterItems.indexOf(filterItem);
    if (index > -1) {
      filterItems.splice(index, 1);
      this.navigationCardsWidgetSettingsForm.get('filter').setValue(filterItems);
      this.navigationCardsWidgetSettingsForm.get('filter').markAsDirty();
    }
  }

  onFilterItemInputFocus() {
    this.filterItemInputChange.next(this.filterItemInput.nativeElement.value);
  }

  addFilterItemFromChipInput(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      const filterItem = value.trim();
      if (this.addFilterItem(filterItem)) {
        this.clearFilterItemInput('');
      }
    }
  }

  filterItemSelected(event: MatAutocompleteSelectedEvent): void {
    this.addFilterItem(event.option.value);
    this.clearFilterItemInput('');
  }

  clearFilterItemInput(value: string = '') {
    this.filterItemInput.nativeElement.value = value;
    this.filterItemInputChange.next(null);
    setTimeout(() => {
      this.filterItemInput.nativeElement.blur();
      this.filterItemInput.nativeElement.focus();
    }, 0);
  }
}
