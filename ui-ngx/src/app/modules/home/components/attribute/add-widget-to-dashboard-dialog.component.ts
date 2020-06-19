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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { Dashboard, DashboardLayoutId } from '@app/shared/models/dashboard.models';
import { objToBase64 } from '@core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { EntityId } from '@app/shared/models/id/entity-id';
import { Widget } from '@app/shared/models/widget.models';
import { DashboardService } from '@core/http/dashboard.service';
import { forkJoin, Observable, of } from 'rxjs';
import { SelectTargetLayoutDialogComponent } from '@home/components/dashboard/select-target-layout-dialog.component';
import {
  SelectTargetStateDialogComponent,
  SelectTargetStateDialogData
} from '@home/components/dashboard/select-target-state-dialog.component';
import { mergeMap } from 'rxjs/operators';
import { AliasesInfo } from '@shared/models/alias.models';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { StateObject } from '@core/api/widget-api.models';

export interface AddWidgetToDashboardDialogData {
  entityId: EntityId;
  entityName: string;
  widget: Widget;
}

@Component({
  selector: 'tb-add-widget-to-dashboard-dialog',
  templateUrl: './add-widget-to-dashboard-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddWidgetToDashboardDialogComponent}],
  styleUrls: ['./add-widget-to-dashboard-dialog.component.scss']
})
export class AddWidgetToDashboardDialogComponent extends
  DialogComponent<AddWidgetToDashboardDialogComponent, void>
  implements OnInit, ErrorStateMatcher {

  addWidgetFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddWidgetToDashboardDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AddWidgetToDashboardDialogComponent, void>,
              private fb: FormBuilder,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private dashboardService: DashboardService,
              private itembuffer: ItemBufferService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    this.addWidgetFormGroup = this.fb.group(
      {
        addToDashboardType: [0, []],
        dashboardId: [null, [Validators.required]],
        newDashboardTitle: [{value: null, disabled: true}, []],
        openDashboard: [false, []]
      }
    );

    this.addWidgetFormGroup.get('addToDashboardType').valueChanges.subscribe(
      (addToDashboardType: number) => {
        if (addToDashboardType === 0) {
          this.addWidgetFormGroup.get('dashboardId').setValidators([Validators.required]);
          this.addWidgetFormGroup.get('dashboardId').enable();
          this.addWidgetFormGroup.get('newDashboardTitle').setValidators([]);
          this.addWidgetFormGroup.get('newDashboardTitle').disable();
          this.addWidgetFormGroup.get('dashboardId').updateValueAndValidity();
          this.addWidgetFormGroup.get('newDashboardTitle').updateValueAndValidity();
        } else {
          this.addWidgetFormGroup.get('dashboardId').setValidators([]);
          this.addWidgetFormGroup.get('dashboardId').disable();
          this.addWidgetFormGroup.get('newDashboardTitle').setValidators([Validators.required]);
          this.addWidgetFormGroup.get('newDashboardTitle').enable();
          this.addWidgetFormGroup.get('dashboardId').updateValueAndValidity();
          this.addWidgetFormGroup.get('newDashboardTitle').updateValueAndValidity();
        }
      }
    );
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    const addToDashboardType: number = this.addWidgetFormGroup.get('addToDashboardType').value;
    if (addToDashboardType === 0) {
      const dashboardId: string = this.addWidgetFormGroup.get('dashboardId').value;
      this.dashboardService.getDashboard(dashboardId).pipe(
        mergeMap((dashboard) => {
          dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
          return this.selectTargetState(dashboard).pipe(
            mergeMap((targetState) => {
              return forkJoin([of(dashboard), of(targetState), this.selectTargetLayout(dashboard, targetState)]);
            })
          );
        })
      ).subscribe((res) => {
        this.addWidgetToDashboard(res[0], res[1], res[2]);
      });
    } else {
      const dashboardTitle: string = this.addWidgetFormGroup.get('newDashboardTitle').value;
      const newDashboard: Dashboard = {
        title: dashboardTitle
      };
      this.addWidgetToDashboard(newDashboard, 'default', 'main');
    }
  }

  private selectTargetState(dashboard: Dashboard): Observable<string> {
    const states = dashboard.configuration.states;
    const stateIds = Object.keys(states);
    if (stateIds.length > 1) {
      return this.dialog.open<SelectTargetStateDialogComponent, SelectTargetStateDialogData,
        string>(SelectTargetStateDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          states
        }
      }).afterClosed();
    } else {
      return of(stateIds[0]);
    }
  }

  private selectTargetLayout(dashboard: Dashboard, targetState: string): Observable<DashboardLayoutId> {
    const layouts = dashboard.configuration.states[targetState].layouts;
    const layoutIds = Object.keys(layouts);
    if (layoutIds.length > 1) {
      return this.dialog.open<SelectTargetLayoutDialogComponent, any,
        DashboardLayoutId>(SelectTargetLayoutDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed();
    } else {
      return of(layoutIds[0] as DashboardLayoutId);
    }
  }

  private addWidgetToDashboard(dashboard: Dashboard, targetState: string, targetLayout: DashboardLayoutId) {
    const aliasesInfo: AliasesInfo = {
      datasourceAliases: {},
      targetDeviceAliases: {}
    };
    aliasesInfo.datasourceAliases[0] = {
      alias: this.data.entityName,
      filter: this.dashboardUtils.createSingleEntityFilter(this.data.entityId)
    };
    this.itembuffer.addWidgetToDashboard(dashboard, targetState,
      targetLayout, this.data.widget, aliasesInfo, null,
      48, null, -1, -1).pipe(
      mergeMap((theDashboard) => {
        return this.dashboardService.saveDashboard(theDashboard);
      })
    ).subscribe(
      (theDashboard) => {
        const openDashboard: boolean = this.addWidgetFormGroup.get('openDashboard').value;
        this.dialogRef.close();
        if (openDashboard) {
          let url;
          const stateIds = Object.keys(dashboard.configuration.states);
          const stateIndex = stateIds.indexOf(targetState);
          if (stateIndex > 0) {
            const stateObject: StateObject = {
              id: targetState,
              params: {}
            };
            const state = objToBase64([ stateObject ]);
            url = `/dashboards/${theDashboard.id.id}?state=${state}`;
          } else {
            url = `/dashboards/${theDashboard.id.id}`;
          }
          const urlTree = this.router.parseUrl(url);
          this.router.navigateByUrl(url);
        }
      }
    );
  }
}
