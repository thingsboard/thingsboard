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

import { Component, ElementRef, ViewChild } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of, Subject } from 'rxjs';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatChipInputEvent, MatChipGrid } from '@angular/material/chips';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { map, mergeMap, share, startWith } from 'rxjs/operators';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { buildPageStepSizeValues } from '@home/components/widget/lib/table-widget.models';

interface DisplayColumn {
  name: string;
  value: string;
}

@Component({
    selector: 'tb-persistent-table-widget-settings',
    templateUrl: './persistent-table-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class PersistentTableWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('columnsChipList') columnsChipList: MatChipGrid;
  @ViewChild('columnAutocomplete') columnAutocomplete: MatAutocomplete;
  @ViewChild('columnInput') columnInput: ElementRef<HTMLInputElement>;

  displayColumns: Array<DisplayColumn> = [
    {value: 'rpcId', name: this.translate.instant('widgets.persistent-table.rpc-id')},
    {value: 'messageType', name: this.translate.instant('widgets.persistent-table.message-type')},
    {value: 'status', name: this.translate.instant('widgets.persistent-table.status')},
    {value: 'method', name: this.translate.instant('widgets.persistent-table.method')},
    {value: 'createdTime', name: this.translate.instant('widgets.persistent-table.created-time')},
    {value: 'expirationTime', name: this.translate.instant('widgets.persistent-table.expiration-time')},
  ];

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  persistentTableWidgetSettingsForm: UntypedFormGroup;

  pageStepSizeValues = [];

  filteredDisplayColumns: Observable<Array<DisplayColumn>>;

  columnSearchText = '';

  columnInputChange = new Subject<string>();

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private fb: UntypedFormBuilder) {
    super(store);
    this.filteredDisplayColumns = this.columnInputChange
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchColumns(name) ),
        share()
      );
  }

  protected settingsForm(): UntypedFormGroup {
    return this.persistentTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      enableFilter: true,

      enableStickyHeader: true,
      enableStickyAction: true,

      displayDetails: true,
      allowSendRequest: true,
      allowDelete: true,

      displayPagination: true,
      defaultPageSize: 10,
      pageStepIncrement: null,
      pageStepCount: 3,

      defaultSortOrder: '-createdTime',
      displayColumns: ['rpcId', 'messageType', 'status', 'method', 'createdTime', 'expirationTime']
    };
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    settings.pageStepIncrement = settings.pageStepIncrement ?? settings.defaultPageSize;
    this.pageStepSizeValues = buildPageStepSizeValues(settings.pageStepCount, settings.pageStepIncrement);
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.persistentTableWidgetSettingsForm = this.fb.group({
      enableFilter: [settings.enableFilter, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      allowSendRequest: [settings.allowSendRequest, []],
      allowDelete: [settings.allowDelete, []],
      displayDetails: [settings.displayDetails, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      pageStepCount: [settings.pageStepCount ?? 3, [Validators.min(1), Validators.max(100),
        Validators.required, Validators.pattern(/^\d*$/)]],
      pageStepIncrement: [settings.pageStepIncrement, [Validators.min(1), Validators.required, Validators.pattern(/^\d*$/)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      displayColumns: [settings.displayColumns, [Validators.required]]
    });
  }

  public validateSettings(): boolean {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    this.columnsChipList.errorState = !displayColumns?.length;
    return super.validateSettings();
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination', 'pageStepCount', 'pageStepIncrement'];
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'pageStepCount' || trigger === 'pageStepIncrement') {
      this.persistentTableWidgetSettingsForm.get('defaultPageSize').reset();
      this.pageStepSizeValues = buildPageStepSizeValues(this.persistentTableWidgetSettingsForm.get('pageStepCount').value,
        this.persistentTableWidgetSettingsForm.get('pageStepIncrement').value);
      return;
    }
    const displayPagination: boolean = this.persistentTableWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.persistentTableWidgetSettingsForm.get('defaultPageSize').enable({emitEvent});
      this.persistentTableWidgetSettingsForm.get('pageStepCount').enable({emitEvent: false});
      this.persistentTableWidgetSettingsForm.get('pageStepIncrement').enable({emitEvent: false});
    } else {
      this.persistentTableWidgetSettingsForm.get('defaultPageSize').disable({emitEvent});
      this.persistentTableWidgetSettingsForm.get('pageStepCount').disable({emitEvent: false});
      this.persistentTableWidgetSettingsForm.get('pageStepIncrement').disable({emitEvent: false});
    }
  }

  private fetchColumns(searchText?: string): Observable<Array<DisplayColumn>> {
    this.columnSearchText = searchText;
    if (this.columnSearchText && this.columnSearchText.length) {
      const search = this.columnSearchText.toUpperCase();
      return of(this.displayColumns.filter(column => column.name.toUpperCase().includes(search)));
    } else {
      return of(this.displayColumns);
    }
  }

  private addColumn(existingColumn: DisplayColumn): boolean {
    if (existingColumn) {
      const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
      const index = displayColumns.indexOf(existingColumn.value);
      if (index === -1) {
        displayColumns.push(existingColumn.value);
        this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
        this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
        this.columnsChipList.errorState = false;
        return true;
      }
    }
    return false;
  }

  displayColumnFromValue(columnValue: string): DisplayColumn | undefined {
    return this.displayColumns.find((column) => column.value === columnValue);
  }

  displayColumnFn(column?: DisplayColumn): string | undefined {
    return column ? column.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  columnDrop(event: CdkDragDrop<string[]>): void {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    moveItemInArray(displayColumns, event.previousIndex, event.currentIndex);
    this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
    this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
  }

  onColumnRemoved(column: string): void {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    const index = displayColumns.indexOf(column);
    if (index > -1) {
      displayColumns.splice(index, 1);
      this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
      this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
      this.columnsChipList.errorState = !displayColumns.length;
    }
  }

  onColumnInputFocus() {
    this.columnInputChange.next(this.columnInput.nativeElement.value);
  }

  addColumnFromChipInput(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      const columnName = value.trim().toUpperCase();
      const existingColumn = this.displayColumns.find(column => column.name.toUpperCase() === columnName);
      if (this.addColumn(existingColumn)) {
        this.clearColumnInput('');
      }
    }
  }

  columnSelected(event: MatAutocompleteSelectedEvent): void {
    this.addColumn(event.option.value);
    this.clearColumnInput('');
  }

  clearColumnInput(value: string = '') {
    this.columnInput.nativeElement.value = value;
    this.columnInputChange.next(null);
    setTimeout(() => {
      this.columnInput.nativeElement.blur();
      this.columnInput.nativeElement.focus();
    }, 0);
  }
}
