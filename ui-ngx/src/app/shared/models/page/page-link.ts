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

import { Direction, SortOrder } from '@shared/models/page/sort-order';

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
      const property = this.sortOrder.property;
      const item1Value = item1[property];
      const item2Value = item2[property];
      let result = 0;
      if (item1Value !== item2Value) {
        if (typeof item1Value === 'number' && typeof item2Value === 'number') {
          result = item1Value - item2Value;
        } else if (typeof item1Value === 'string' && typeof item2Value === 'string') {
          result = item1Value.localeCompare(item2Value);
        } else if (typeof item1Value !== typeof item2Value) {
          result = 1;
        }
      }
      return this.sortOrder.direction === Direction.ASC ? result : result * -1;
    }
    return 0;
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
