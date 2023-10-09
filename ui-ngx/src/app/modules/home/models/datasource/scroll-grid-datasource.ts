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

import { DataSource, ListRange } from '@angular/cdk/collections';
import { CdkVirtualForOf, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { BehaviorSubject, Observable, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { BreakpointObserver } from '@angular/cdk/layout';

export type GridEntitiesFetchFunction<T, F> = (pageSize: number, page: number, filter: F) => Observable<PageData<T>>;

export type GridCellType = 'emptyCell' | 'loadingCell';

export interface ScrollGridColumns {
  columns: number;
  breakpoints?: {[breakpoint: string]: number};
}

export class ScrollGridDatasource<T, F> extends DataSource<(T | GridCellType)[]> {

  public initialDataLoading = true;

  private _data: T[] = [];
  private _rows: (T | GridCellType)[][] = Array.from<T[]>({length: 100000});
  private _hasNext = true;
  private _columns: number;
  private _viewport: CdkVirtualScrollViewport;
  private _pendingRange: ListRange = null;
  private _fetchingData = false;
  private _fetchSubscription: Subscription;
  private _totalElements = 0;

  private _dataStream: BehaviorSubject<(T | GridCellType)[][]>;
  private _subscription: Subscription;

  constructor(private breakpointObserver: BreakpointObserver,
              private columns: ScrollGridColumns,
              private fetchFunction: GridEntitiesFetchFunction<T, F>,
              private filter: F) {
    super();
  }

  connect(collectionViewer: CdkVirtualForOf<(T | GridCellType)[]>): Observable<(T | GridCellType)[][]> {
    this._viewport = (collectionViewer as any)._viewport;
    this._init();

    if (this.columns.breakpoints) {
      const breakpoints = Object.keys(this.columns.breakpoints);
      this._subscription.add(this.breakpointObserver.observe(breakpoints).subscribe(
        () => {
          this._columnsChanged(this._detectColumns());
        }
      ));
    }

    this._subscription.add(
      collectionViewer.viewChange.subscribe(range => this._fetchDataFromRange(range))
    );
    return this._dataStream;
  }

  disconnect(): void {
    this._reset();
    this._subscription.unsubscribe();
  }


  get isEmpty(): boolean {
    return !this._data.length;
  }

  get active(): boolean {
    return !!this._subscription && !this._subscription.closed;
  }

  public updateFilter(filter: F) {
    this.filter = filter;
    if (this.active) {
      const prevLength = this._rows.length;
      this._reset();
      const dataLengthChanged = prevLength !== this._rows.length;

      const range = this._viewport.getRenderedRange();

      if (dataLengthChanged) {
        // Force recalculate new range
        if (range.start === 0) {
          range.start = 1;
        }
        this._viewport.appendOnly = false;
      }

      const scrollOffset = this._viewport.measureScrollOffset();
      if (scrollOffset > 0) {
        this._viewport.scrollToOffset(0);
      }

      this._dataUpdated();
      this._viewport.appendOnly = true;

      if (!dataLengthChanged) {
        this._fetchDataFromRange(range);
      }
    }
  }

  private _detectColumns(): number {
    let columns = this.columns.columns;
    if (this.columns.breakpoints) {
      for (const breakpont of Object.keys(this.columns.breakpoints)) {
        if (this.breakpointObserver.isMatched(breakpont)) {
          columns = this.columns.breakpoints[breakpont];
          break;
        }
      }
    }
    return columns;
  }

  private _init() {
    this._subscription = new Subscription();
    this._columns = this._detectColumns();
    if (this._dataStream) {
      this._dataStream.complete();
    }
    this._dataStream = new BehaviorSubject(this._rows);
  }

  private _reset() {
    this._data = [];
    this._totalElements = 0;
    this.initialDataLoading = true;
    this._rows = Array.from<T[]>({length: 100000});
    this._hasNext = true;
    this._pendingRange = null;
    this._fetchingData = false;
    if (this._fetchSubscription) {
      this._fetchSubscription.unsubscribe();
    }
  }

  private _columnsChanged(columns: number) {
    if (this._columns !== columns) {
      const fetchData = columns > this._columns;
      this._columns = columns;
      const rowsLength = this._totalElements ? Math.ceil(this._totalElements / this._columns) : 100000;
      this._rows = Array.from<T[]>({length: rowsLength});
      this._dataUpdated();
      if (fetchData && this._hasNext) {
        this._fetchDataFromRange(this._viewport.getRenderedRange());
      }
    }
  }

  private _fetchDataFromRange(range: ListRange) {
    if (this._hasNext) {
      if (this._fetchingData) {
        this._pendingRange = range;
      } else {
        const endIndex = (range.end + 1) * this._columns;
        if (endIndex > this._data.length) {
          const startIndex = this._data.length;
          const minPageSize = endIndex - startIndex;
          const maxPageSize = minPageSize * 2;
          let pageSize = minPageSize;
          let page = Math.floor(startIndex / pageSize);
          while (startIndex % pageSize !== 0 && pageSize <= maxPageSize) {
            if (((page + 1) * pageSize) > endIndex) {
              break;
            }
            pageSize++;
            page = Math.floor(startIndex / pageSize);
          }
          const offset = startIndex % pageSize;
          this._fetchData(offset, pageSize, page);
        }
      }
    }
  }

  private _fetchData(offset: number, pageSize: number, page: number) {
    this._fetchingData = true;
    this._fetchSubscription = this.fetchFunction(pageSize, page, this.filter).pipe(
      catchError(() => of(emptyPageData<T>()))
    ).subscribe(
      (data) => {
        this._hasNext = data.hasNext;
        if (data.data.length > offset) {
          for (let i = offset; i < data.data.length; i++) {
            this._data.push(data.data[i]);
          }
        }
        this._totalElements = data.totalElements;
        const rowsLength = this._totalElements ? Math.ceil(this._totalElements / this._columns) : 100000;
        this._rows = Array.from<T[]>({length: rowsLength});
        this._dataUpdated();
        this.initialDataLoading = false;
        this._fetchingData = false;
        if (this._pendingRange) {
          const range = this._pendingRange;
          this._pendingRange = null;
          this._fetchDataFromRange(range);
        }
      }
    );
  }

  private _dataUpdated() {
    for (let index = 0; index < this._data.length; index++) {
      const row = Math.floor(index / this._columns);
      const col =  index % this._columns;
      if (!this._rows[row]) {
        this._rows[row] = [];
      }
      this._rows[row][col] = this._data[index];
    }
    this._fillGridCells();
    this._dataStream.next(this._rows);
  }

  private _fillGridCells() {
    if (this._totalElements) {
      const startIndex = this._data.length;
      const endIndex = this._rows.length * this._columns;
      for (let index = startIndex; index < endIndex; index++) {
        const row = Math.floor(index / this._columns);
        const col =  index % this._columns;
        const cellType: GridCellType = index < this._totalElements ? 'loadingCell' : 'emptyCell';
        if (!this._rows[row]) {
          this._rows[row] = [];
        }
        this._rows[row][col] = cellType;
      }
    }
  }
}
