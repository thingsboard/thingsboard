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

import {
  AfterViewInit, ChangeDetectorRef,
  Component,
  Input, NgZone,
  OnChanges, OnDestroy,
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
} from '@shared/components/grid/scroll-grid-datasource';
import { BreakpointObserver } from '@angular/cdk/layout';
import { isObject } from '@app/core/utils';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

export type ItemSizeFunction = (itemWidth: number) => number;

export interface ItemSizeStrategy {
  defaultItemSize: number;
  itemSizeFunction: ItemSizeFunction;
}

@Component({
    selector: 'tb-scroll-grid',
    templateUrl: './scroll-grid.component.html',
    styleUrls: ['./scroll-grid.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ScrollGridComponent<T, F> implements OnInit, AfterViewInit, OnChanges, OnDestroy {

  @ViewChild('viewport')
  viewport: CdkVirtualScrollViewport;

  @Input()
  columns: ScrollGridColumns | number = 1;

  @Input()
  fetchFunction: GridEntitiesFetchFunction<T, F>;

  @Input()
  filter: F;

  @Input()
  itemSize: number | ItemSizeStrategy = 200;

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

  calculatedItemSize: number;
  minBuffer: number;
  maxBuffer: number;

  private contentResize$: ResizeObserver;

  constructor(private breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef,
              private renderer: Renderer2,
              private zone: NgZone) {
  }

  ngOnInit(): void {
    if (typeof this.itemSize === 'number') {
      this.calculatedItemSize = this.itemSize;
    } else {
      this.calculatedItemSize = this.itemSize.defaultItemSize;
    }
    this.minBuffer = this.calculatedItemSize;
    this.maxBuffer = this.calculatedItemSize * 2;
    this.dataSource = new ScrollGridDatasource<T, F>(this.breakpointObserver, this.columns, this.fetchFunction, this.filter);
  }

  ngAfterViewInit() {
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'gap', this.gap + 'px');
    this.renderer.setStyle(this.viewport._contentWrapper.nativeElement, 'padding', this.gap + 'px');
    if (!(typeof this.itemSize === 'number')) {
      this.contentResize$ = new ResizeObserver(() => {
        this.zone.run(() => {
          this.onContentResize();
        });
      });
      this.contentResize$.observe(this.viewport._contentWrapper.nativeElement);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue && propName === 'filter') {
        this.dataSource.updateFilter(this.filter);
      }
    }
  }

  ngOnDestroy() {
    if (this.contentResize$) {
      this.contentResize$.disconnect();
    }
  }

  isObject(value: any): boolean {
    return isObject(value);
  }

  trackByItemsRow(index: number, itemsRow: T[]): number {
    return index;
  }

  trackByItem(index: number, item: T): T {
    return item;
  }

  public update() {
    this.dataSource.update();
  }

  public updateItem(index: number, item: T) {
    this.dataSource.updateItem(index, item);
  }

  public deleteItem(index: number) {
    this.dataSource.deleteItem(index);
  }

  private onContentResize() {
    const contentWidth = this.viewport._contentWrapper.nativeElement.getBoundingClientRect().width;
    const columns = this.dataSource.currentColumns;
    const itemWidth = (contentWidth - this.gap * (columns + 1)) / columns;
    this.calculatedItemSize = (this.itemSize as ItemSizeStrategy).itemSizeFunction(itemWidth);
    this.minBuffer = this.calculatedItemSize;
    this.maxBuffer = this.calculatedItemSize * 2;
    this.cd.markForCheck();
  }
}
