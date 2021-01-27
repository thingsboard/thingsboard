///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { getDescendantProp, isObject } from '@core/utils';
import { SortDirection } from '@angular/material/sort';

export const MAX_SAFE_PAGE_SIZE = 2147483647;

export type PageLinkSearchFunction<T> = (entity: T, textSearch: string, searchProperty?: string) => boolean;

export function defaultPageLinkSearchFunction(searchProperty?: string): PageLinkSearchFunction<any> {
  return (entity, textSearch) => defaultPageLinkSearch(entity, textSearch, searchProperty);
}

const defaultPageLinkSearch: PageLinkSearchFunction<any> =
  (entity: any, textSearch: string, searchProperty?: string) => {
    if (textSearch === null || !textSearch.length) {
      return true;
    }
    const expected = ('' + textSearch).toLowerCase();
    if (searchProperty && searchProperty.length) {
      if (Object.prototype.hasOwnProperty.call(entity, searchProperty)) {
        const val = entity[searchProperty];
        if (val !== null) {
          if (val !== Object(val)) {
            const actual = ('' + val).toLowerCase();
            if (actual.indexOf(expected) !== -1) {
              return true;
            }
          }
        }
      }
    } else {
      for (const key of Object.keys(entity)) {
        const val = entity[key];
        if (val !== null) {
          if (val !== Object(val)) {
            const actual = ('' + val).toLowerCase();
            if (actual.indexOf(expected) !== -1) {
              return true;
            }
          } else if (isObject(val)) {
            if (defaultPageLinkSearch(val, textSearch)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  };

export function sortItems(item1: any, item2: any, property: string, asc: boolean): number {
  const item1Value = getDescendantProp(item1, property);
  const item2Value = getDescendantProp(item2, property);
  let result = 0;
  if (item1Value !== item2Value) {
    const item1Type = typeof item1Value;
    const item2Type = typeof item2Value;
    if (item1Type === 'number' && item2Type === 'number') {
      result = item1Value - item2Value;
    } else if (item1Type === 'string' && item2Type === 'string') {
      result = item1Value.localeCompare(item2Value);
    } else if ((item1Type === 'boolean' && item2Type === 'boolean') || (item1Type !== item2Type)) {
      if (item1Value && !item2Value) {
        result = 1;
      } else if (!item1Value && item2Value) {
        result = -1;
      }
    }
  }
  return asc ? result : result * -1;
}

export class PageLink {

  textSearch: string;
  pageSize: number;
  page: number;
  sortOrder: SortOrder;

  constructor(pageSize: number, page: number = 0, textSearch: string = null, sortOrder: SortOrder = null) {
    this.textSearch = textSearch;
    this.pageSize = pageSize;
    this.page = page;
    this.sortOrder = sortOrder;
  }

  public nextPageLink(): PageLink {
    return new PageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder);
  }

  public toQuery(): string {
    let query = `?pageSize=${this.pageSize}&page=${this.page}`;
    if (this.textSearch && this.textSearch.length) {
      query += `&textSearch=${this.textSearch}`;
    }
    if (this.sortOrder) {
      query += `&sortProperty=${this.sortOrder.property}&sortOrder=${this.sortOrder.direction}`;
    }
    return query;
  }

  public sort(item1: any, item2: any): number {
    if (this.sortOrder) {
      const sortProperty = this.sortOrder.property;
      const asc = this.sortOrder.direction === Direction.ASC;
      return sortItems(item1, item2, sortProperty, asc);
    }
    return 0;
  }

  public filterData<T>(data: Array<T>,
                       searchFunction: PageLinkSearchFunction<T> = defaultPageLinkSearchFunction()): PageData<T> {
    const pageData = emptyPageData<T>();
    pageData.data = [...data];
    if (this.textSearch && this.textSearch.length) {
      pageData.data = pageData.data.filter((entity) => searchFunction(entity, this.textSearch));
    }
    pageData.totalElements = pageData.data.length;
    pageData.totalPages = this.pageSize === Number.POSITIVE_INFINITY ? 1 : Math.ceil(pageData.totalElements / this.pageSize);
    if (this.sortOrder) {
      const sortProperty = this.sortOrder.property;
      const asc = this.sortOrder.direction === Direction.ASC;
      pageData.data = pageData.data.sort((a, b) => sortItems(a, b, sortProperty, asc));
    }
    if (this.pageSize !== Number.POSITIVE_INFINITY) {
      const startIndex = this.pageSize * this.page;
      pageData.data = pageData.data.slice(startIndex, startIndex + this.pageSize);
      pageData.hasNext = pageData.totalElements > startIndex + pageData.data.length;
    }
    return pageData;
  }

  public sortDirection(): SortDirection {
    if (this.sortOrder) {
      return (this.sortOrder.direction + '').toLowerCase() as SortDirection;
    } else {
      return '' as SortDirection;
    }
  }

}

export class TimePageLink extends PageLink {

  startTime: number;
  endTime: number;

  constructor(pageSize: number, page: number = 0, textSearch: string = null, sortOrder: SortOrder = null,
              startTime: number = null, endTime: number = null) {
    super(pageSize, page, textSearch, sortOrder);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public nextPageLink(): TimePageLink {
    return new TimePageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder, this.startTime, this.endTime);
  }

  public toQuery(): string {
    let query = super.toQuery();
    if (this.startTime) {
      query += `&startTime=${this.startTime}`;
    }
    if (this.endTime) {
      query += `&endTime=${this.endTime}`;
    }
    return query;
  }
}
