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

import { AfterViewInit, Component, ElementRef, Inject, OnInit, SecurityContext, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { PageLink } from '@shared/models/page/page-link';
import {
  DashboardStateInfo,
  DashboardStatesDatasource
} from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component.models';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { deepClone, isDefined, isEqual } from '@core/utils';
import {
  DashboardStateDialogComponent,
  DashboardStateDialogData
} from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { Widget } from '@shared/models/widget.models';
import { DomSanitizer } from '@angular/platform-browser';

export interface ManageDashboardStatesDialogData {
  states: {[id: string]: DashboardState };
  widgets: {[id: string]: Widget };
}

export interface ManageDashboardStatesDialogResult {
  states: {[id: string]: DashboardState };
  addWidgets?: {[id: string]: Widget };
}

@Component({
    selector: 'tb-manage-dashboard-states-dialog',
    templateUrl: './manage-dashboard-states-dialog.component.html',
    styleUrls: ['./manage-dashboard-states-dialog.component.scss'],
    standalone: false
})
export class ManageDashboardStatesDialogComponent
  extends DialogComponent<ManageDashboardStatesDialogComponent, ManageDashboardStatesDialogResult>
  implements OnInit, AfterViewInit {

  @ViewChild('searchInput', {static: false}) searchInputField: ElementRef;

  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;

  isDirty = false;

  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: DashboardStatesDatasource;

  private states: {[id: string]: DashboardState };
  private widgets: {[id: string]: Widget};

  private stateNames: Set<string> = new Set<string>();
  private addWidgets: {[id: string]: Widget} = {};

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardStatesDialogData,
              public dialogRef: MatDialogRef<ManageDashboardStatesDialogComponent, ManageDashboardStatesDialogResult>,
              private translate: TranslateService,
              private dialogs: DialogService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private sanitizer: DomSanitizer) {
    super(store, router, dialogRef);

    this.states = this.data.states;
    this.widgets = this.data.widgets;
    Object.values(this.states).forEach(value => this.stateNames.add(value.name));

    const sortOrder: SortOrder = { property: 'name', direction: Direction.ASC };
    this.pageLink = new PageLink(5, 0, null, sortOrder);
    this.displayedColumns = ['name', 'id', 'root', 'actions'];
    this.dataSource = new DashboardStatesDatasource(this.states);
  }

  ngOnInit(): void {
    this.dataSource.loadStates(this.pageLink);
  }

  ngAfterViewInit() {
    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadStates(this.pageLink, reload);
  }

  addState($event: Event) {
    this.openStateDialog($event);
  }

  editState($event: Event, state: DashboardStateInfo) {
    this.openStateDialog($event, state);
  }

  deleteState($event: Event, state: DashboardStateInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('dashboard.delete-state-title');
    const content = this.translate.instant('dashboard.delete-state-text', {stateName: state.name});
    const safeContent = this.sanitizer.sanitize(SecurityContext.HTML, content);
    this.dialogs.confirm(title, safeContent, this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe(
        (res) => {
          if (res) {
            this.stateNames.delete(state.name);
            delete this.states[state.id];
            this.onStatesUpdated();
          }
        }
    );
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  openStateDialog($event: Event, state: DashboardStateInfo = null) {
    if ($event) {
      $event.stopPropagation();
    }
    const isAdd = state === null;
    let prevStateId = null;
    let prevStateName = '';
    if (!isAdd) {
      prevStateId = state.id;
      prevStateName = state.name;
    }
    this.dialog.open<DashboardStateDialogComponent, DashboardStateDialogData,
      DashboardStateInfo>(DashboardStateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        states: this.states,
        state: deepClone(state)
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.saveState(res, prevStateId, prevStateName);
        }
      }
    );
  }

  saveState(state: DashboardStateInfo, prevStateId: string, prevStateName: string) {
    const newState: DashboardState = {
      name: state.name,
      root: state.root,
      layouts: state.layouts
    };
    if (prevStateId && prevStateId !== state.id) {
      delete this.states[prevStateId];
      this.states[state.id] = newState;
    } else {
      this.states[state.id] = newState;
    }
    if (prevStateName && prevStateName !== state.name) {
      this.stateNames.delete(prevStateName);
      this.stateNames.add(state.name);
    }
    if (state.root) {
      for (const id of Object.keys(this.states)) {
        const otherState = this.states[id];
        if (id !== state.id) {
          otherState.root = false;
        }
      }
    } else {
      let rootFound = false;
      for (const id of Object.keys(this.states)) {
        const otherState = this.states[id];
        if (otherState.root) {
          rootFound = true;
          break;
        }
      }
      if (!rootFound) {
        const firstStateId = Object.keys(this.states)[0];
        this.states[firstStateId].root = true;
      }
    }
    this.onStatesUpdated();
  }

  duplicateState($event: Event, state: DashboardStateInfo) {
    const suffix = ` - ${this.translate.instant('action.copy')} `;
    let counter = 0;
    const maxAttempts = 1000;

    while (counter++ < maxAttempts) {
      const candidateName = `${state.name}${suffix}${counter}`;
      if (this.stateNames.has(candidateName)) continue;

      const candidateId = candidateName.toLowerCase().replace(/\W/g, '_');
      if (this.states[candidateId]) {
        continue;
      }

      const duplicatedState = deepClone(state);
      const mainWidgets = {};
      const rightWidgets = {};
      duplicatedState.id = candidateId;
      duplicatedState.name = candidateName;
      duplicatedState.root = false;
      this.stateNames.add(duplicatedState.name);

      for (const [key, value] of Object.entries(duplicatedState.layouts.main.widgets)) {
        const guid = this.utils.guid();
        mainWidgets[guid] = value;
        this.addWidgets[guid] = deepClone(this.widgets[key] ?? this.addWidgets[key]);
        this.addWidgets[guid].id = guid;
      }
      duplicatedState.layouts.main.widgets = mainWidgets;

      if (isDefined(duplicatedState.layouts?.right)) {
        for (const [key, value] of Object.entries(duplicatedState.layouts.right.widgets)) {
          const guid = this.utils.guid();
          rightWidgets[guid] = value;
          this.addWidgets[guid] = deepClone(this.widgets[key] ?? this.addWidgets[key]);
          this.addWidgets[guid].id = guid;
        }
        duplicatedState.layouts.right.widgets = rightWidgets;
      }

      this.states[duplicatedState.id] = duplicatedState;
      this.onStatesUpdated();
      return;
    }
  }

  private onStatesUpdated() {
    this.isDirty = true;
    this.updateData(true);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    const result: ManageDashboardStatesDialogResult = {states: this.states};
    if (!isEqual(this.addWidgets, {})) {
      result.addWidgets = this.addWidgets;
    }
    this.dialogRef.close(result);
  }
}
