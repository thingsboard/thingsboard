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
  CellClickColumnInfo,
  WidgetActionDescriptor,
  WidgetActionSource,
  widgetActionTypeTranslationMap
} from '@app/shared/models/widget.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject, shareReplay } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { deepClone } from '@core/utils';

export interface WidgetActionCallbacks {
  fetchDashboardStates: (query: string) => Array<string>;
  fetchCellClickColumns: () => Array<CellClickColumnInfo>;
}

export interface WidgetActionsData {
  actionsMap: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  actionSources: {[actionSourceId: string]: WidgetActionSource};
}

export interface WidgetActionDescriptorInfo extends WidgetActionDescriptor {
  actionSourceId?: string;
  actionSourceName?: string;
  typeName?: string;
}

export const toWidgetActionDescriptor = (action: WidgetActionDescriptorInfo): WidgetActionDescriptor => {
  const copy = deepClone(action);
  delete copy.actionSourceId;
  delete copy.actionSourceName;
  delete copy.typeName;
  return copy;
};

export class WidgetActionsDatasource implements DataSource<WidgetActionDescriptorInfo> {

  private actionsSubject = new BehaviorSubject<WidgetActionDescriptorInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<WidgetActionDescriptorInfo>>(emptyPageData<WidgetActionDescriptorInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allActions: Observable<Array<WidgetActionDescriptorInfo>>;

  private actionsMap: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  private actionSources: {[actionSourceId: string]: WidgetActionSource};

  constructor(private translate: TranslateService,
              private utils: UtilsService) {}

  connect(_collectionViewer: CollectionViewer): Observable<WidgetActionDescriptorInfo[] | ReadonlyArray<WidgetActionDescriptorInfo>> {
    return this.actionsSubject.asObservable();
  }

  disconnect(_collectionViewer: CollectionViewer): void {
    this.actionsSubject.complete();
    this.pageDataSubject.complete();
  }

  setActions(actionsData: WidgetActionsData) {
    this.actionsMap = actionsData.actionsMap;
    this.actionSources = actionsData.actionSources;
  }

  loadActions(pageLink: PageLink, reload: boolean = false): Observable<PageData<WidgetActionDescriptorInfo>> {
    if (reload) {
      this.allActions = null;
    }
    const result = new ReplaySubject<PageData<WidgetActionDescriptorInfo>>();
    this.fetchActions(pageLink).pipe(
      catchError(() => of(emptyPageData<WidgetActionDescriptorInfo>())),
    ).subscribe(
      (pageData) => {
        this.actionsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchActions(pageLink: PageLink): Observable<PageData<WidgetActionDescriptorInfo>> {
    return this.getAllActions().pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllActions(): Observable<Array<WidgetActionDescriptorInfo>> {
    if (!this.allActions) {
      const actions: WidgetActionDescriptorInfo[] = [];
      for (const actionSourceId of Object.keys(this.actionsMap)) {
        const descriptors = this.actionsMap[actionSourceId];
        descriptors.forEach((descriptor) => {
          actions.push(this.toWidgetActionDescriptorInfo(actionSourceId, descriptor));
        });
      }
      this.allActions = of(actions).pipe(
        shareReplay(1)
      );
    }
    return this.allActions;
  }

  private toWidgetActionDescriptorInfo(actionSourceId: string, action: WidgetActionDescriptor): WidgetActionDescriptorInfo {
    const actionSource = this.actionSources[actionSourceId];
    const actionSourceName = actionSource ? this.utils.customTranslation(actionSource.name, actionSource.name) : actionSourceId;
    const typeName = this.translate.instant(widgetActionTypeTranslationMap.get(action.type));
    return { actionSourceId, actionSourceName, typeName, ...action};
  }

  isEmpty(): Observable<boolean> {
    return this.actionsSubject.pipe(
      map((actions) => !actions.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

}
