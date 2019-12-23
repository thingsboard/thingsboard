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

// tslint:disable-next-line:no-reference
/// <reference path="../../../../../../../src/typings/split.js.typings.d.ts" />

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild, ViewEncapsulation, ViewChildren, QueryList, ComponentFactoryResolver
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { combineLatest, fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import {
  WidgetActionDescriptorInfo,
  WidgetActionsData,
  WidgetActionsDatasource,
  WidgetActionCallbacks, toWidgetActionDescriptor
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityRelation, EntitySearchDirection, RelationTypeGroup } from '@shared/models/relation.models';
import { RelationDialogComponent, RelationDialogData } from '@home/components/relation/relation-dialog.component';
import { CustomActionDescriptor, WidgetActionDescriptor, WidgetActionSource } from '@shared/models/widget.models';
import {
  WidgetActionDialogComponent,
  WidgetActionDialogData
} from '@home/components/widget/action/widget-action-dialog.component';
import { deepClone } from '@core/utils';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { CustomActionPrettyResourcesTabsComponent } from '@home/components/widget/action/custom-action-pretty-resources-tabs.component';
import { MatTab, MatTabGroup } from '@angular/material/tabs';

@Component({
  selector: 'tb-custom-action-pretty-editor',
  templateUrl: './custom-action-pretty-editor.component.html',
  styleUrls: ['./custom-action-pretty-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomActionPrettyEditorComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class CustomActionPrettyEditorComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  action: CustomActionDescriptor;

  fullscreen = false;

  @ViewChildren('leftPanel')
  leftPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  @ViewChildren('rightPanel')
  rightPanelElmRef: QueryList<ElementRef<HTMLElement>>;

  private propagateChange = (_: any) => {};

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    combineLatest(this.leftPanelElmRef.changes, this.rightPanelElmRef.changes).subscribe(() => {
      if (this.leftPanelElmRef.length && this.rightPanelElmRef.length) {
        this.initSplitLayout(this.leftPanelElmRef.first.nativeElement,
          this.rightPanelElmRef.first.nativeElement);
      }
    });
  }

  private initSplitLayout(leftPanel: any, rightPanel: any) {
    Split([leftPanel, rightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });
  }

  ngOnDestroy(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: CustomActionDescriptor): void {
    this.action = obj;
  }

  public onActionUpdated(valid: boolean = true) {
    if (!valid) {
      this.propagateChange(null);
    } else {
      this.propagateChange(this.action);
    }
  }
}
