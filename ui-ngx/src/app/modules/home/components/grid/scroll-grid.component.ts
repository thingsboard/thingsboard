///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  GridEntitiesFetchFunction,
  ScrollGridColumns,
  ScrollGridDatasource
} from '@home/models/datasource/scroll-grid-datasource';
import { BreakpointObserver } from '@angular/cdk/layout';
import { isObject } from '@app/core/utils';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

@Component({
  selector: 'tb-scroll-grid',
  templateUrl: './scroll-grid.component.html',
  styleUrls: ['./scroll-grid.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScrollGridComponent<T, F> implements OnInit, AfterViewInit, OnChanges {

  @ViewChild('viewport')
  viewport: CdkVirtualScrollViewport;

  @Input()
  columns: ScrollGridColumns = {columns: 1};

  @Input()
  fetchFunction: GridEntitiesFetchFunction<T, F>;

  @Input()
  filter: F;

  @Input()
  itemSize = 200;

  @Input()
  gap = 12;

  @Input()
  itemCard: TemplateRef<{item: T}>;

  @Input()
  loadingCell: TemplateRef<any>;

  @Input()
  dataLoading: TemplateRef<any>;

  @Input()
  noData: TemplateRef<any>;

  dataSource: ScrollGridDatasource<T, F>;

  constructor(private breakpointObserver: BreakpointObserver,
              private renderer: Renderer2) {
  }

  ngOnInit(): void {
    this.dataSource = new ScrollGridDatasource<T, F>(this.breakpointObserver, this.columns, this.fetchFunction, this.filter);
  }

  ngAfterViewInit() {
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'gap', this.gap + 'px');
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'padding', this.gap + 'px');
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue && propName === 'filter') {
        this.dataSource.updateFilter(this.filter);
      }
    }
  }

  isObject(value: any): boolean {
    return isObject(value);
  }
}
