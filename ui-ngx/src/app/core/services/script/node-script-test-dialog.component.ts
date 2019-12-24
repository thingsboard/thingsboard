///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  Inject,
  OnInit,
  QueryList,
  SkipSelf,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { ErrorStateMatcher, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { combineLatest, Observable, of } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  toCustomAction,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetActionSource, WidgetActionType, widgetActionTypeTranslationMap } from '@shared/models/widget.models';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { DashboardService } from '@core/http/dashboard.service';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

export interface NodeScriptTestDialogData {
  script: string;
  scriptType: string;
  functionTitle: string;
  functionName: string;
  argNames: string[];
  msg?: any;
  metadata?: any;
  msgType?: string;
}

@Component({
  selector: 'tb-node-script-test-dialog',
  templateUrl: './node-script-test-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: NodeScriptTestDialogComponent}],
  styleUrls: ['./node-script-test-dialog.component.scss']
})
export class NodeScriptTestDialogComponent extends DialogComponent<NodeScriptTestDialogComponent,
  string> implements OnInit, AfterViewInit, ErrorStateMatcher {

  @ViewChildren('topPanel')
  topPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('topLeftPanel')
  topLeftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('topRightPanel')
  topRightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomPanel')
  bottomPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomLeftPanel')
  bottomLeftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('bottomLeftPanel')
  bottomRightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  nodeScriptTestFormGroup: FormGroup;

  functionTitle: string;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: NodeScriptTestDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<NodeScriptTestDialogComponent, string>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.functionTitle = this.data.functionTitle;
  }

  ngOnInit(): void {
    this.nodeScriptTestFormGroup = this.fb.group({});
  }

  ngAfterViewInit(): void {
    combineLatest(this.topPanelElmRef.changes,
                  this.topLeftPanelElmRef.changes,
                  this.topRightPanelElmRef.changes,
                  this.bottomPanelElmRef.changes,
                  this.bottomLeftPanelElmRef.changes,
                  this.bottomRightPanelElmRef.changes).subscribe(() => {
      if (this.topPanelElmRef.length && this.topLeftPanelElmRef.length &&
          this.topRightPanelElmRef.length && this.bottomPanelElmRef.length &&
          this.bottomLeftPanelElmRef.length && this.bottomRightPanelElmRef.length) {
        this.initSplitLayout(this.topPanelElmRef.first.nativeElement,
                             this.topLeftPanelElmRef.first.nativeElement,
                             this.topRightPanelElmRef.first.nativeElement,
                             this.bottomPanelElmRef.first.nativeElement,
                             this.bottomLeftPanelElmRef.first.nativeElement,
                             this.bottomRightPanelElmRef.first.nativeElement);
      }
    });
  }

  private initSplitLayout(topPanel: any,
                          topLeftPanel: any,
                          topRightPanel: any,
                          bottomPanel: any,
                          bottomLeftPanel: any,
                          bottomRightPanel: any) {

    Split([topPanel, bottomPanel], {
      sizes: [35, 65],
      gutterSize: 8,
      cursor: 'row-resize',
      direction: 'vertical'
    });

    Split([topLeftPanel, topRightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });

    Split([bottomLeftPanel, bottomRightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
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
    const script: string = this.nodeScriptTestFormGroup.get('funcBody').value;
    this.dialogRef.close(script);
  }
}
