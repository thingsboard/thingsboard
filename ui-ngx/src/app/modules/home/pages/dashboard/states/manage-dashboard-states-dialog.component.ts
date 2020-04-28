///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { PageLink } from '@shared/models/page/page-link';
import {
  DashboardStateInfo,
  DashboardStatesDatasource
} from '@home/pages/dashboard/states/manage-dashboard-states-dialog.component.models';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { deepClone } from '@core/utils';
import {
  DashboardStateDialogComponent,
  DashboardStateDialogData
} from '@home/pages/dashboard/states/dashboard-state-dialog.component';

export interface ManageDashboardStatesDialogData {
  states: {[id: string]: DashboardState };
}

@Component({
  selector: 'tb-manage-dashboard-states-dialog',
  templateUrl: './manage-dashboard-states-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardStatesDialogComponent}],
  styleUrls: ['./manage-dashboard-states-dialog.component.scss']
})
export class ManageDashboardStatesDialogComponent extends
                  DialogComponent<ManageDashboardStatesDialogComponent, {[id: string]: DashboardState }>
  implements OnInit, ErrorStateMatcher, AfterViewInit {

  statesFormGroup: FormGroup;

  states: {[id: string]: DashboardState };

  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: DashboardStatesDatasource;

  submitted = false;

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardStatesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDashboardStatesDialogComponent, {[id: string]: DashboardState }>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private dialogs: DialogService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    this.states = this.data.states;
    this.statesFormGroup = this.fb.group({});

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
    this.dialogs.confirm(title, content, this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe(
        (res) => {
          if (res) {
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
    if (!isAdd) {
      prevStateId = state.id;
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
          this.saveState(res, prevStateId);
        }
      }
    );
  }

  saveState(state: DashboardStateInfo, prevStateId: string) {
    const newState: DashboardState = {
      name: state.name,
      root: state.root,
      layouts: state.layouts
    };
    if (prevStateId) {
      this.states[prevStateId] = newState;
    } else {
      this.states[state.id] = newState;
    }
    if (state.root) {
      for (const id of Object.keys(this.states)) {
        const otherState = this.states[id];
        if (id !== state.id) {
          otherState.root = false;
        }
      }
    }
    this.onStatesUpdated();
  }

  private onStatesUpdated() {
    this.statesFormGroup.markAsDirty();
    this.updateData(true);
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close(this.states);
  }
}
