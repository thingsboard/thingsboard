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

import {
  CustomActionDescriptor,
  WidgetActionDescriptor,
  WidgetActionSource,
  widgetActionTypeTranslationMap
} from '@app/shared/models/widget.models';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, isDefined, isUndefined } from '@core/utils';

import customSampleJs from '!raw-loader!./custom-sample-js.raw';
import customSampleCss from '!raw-loader!./custom-sample-css.raw';
import customSampleHtml from '!raw-loader!./custom-sample-html.raw';

export interface WidgetActionCallbacks {
  fetchDashboardStates: (query: string) => Array<string>;
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

export function toWidgetActionDescriptor(action: WidgetActionDescriptorInfo): WidgetActionDescriptor {
  const copy = deepClone(action);
  delete copy.actionSourceId;
  delete copy.actionSourceName;
  delete copy.typeName;
  return copy;
}

export function toCustomAction(action: WidgetActionDescriptorInfo): CustomActionDescriptor {
  let result: CustomActionDescriptor;
  if (!action || (isUndefined(action.customFunction) && isUndefined(action.customHtml) && isUndefined(action.customCss))) {
    result = {
      customHtml: customSampleHtml,
      customCss: customSampleCss,
      customFunction: customSampleJs
    };
  } else {
    result = {
      customHtml: action.customHtml,
      customCss: action.customCss,
      customFunction: action.customFunction
    };
  }
  result.customResources = action && isDefined(action.customResources) ? deepClone(action.customResources) : [];
  return result;
}

export class WidgetActionsDatasource implements DataSource<WidgetActionDescriptorInfo> {

  private actionsSubject = new BehaviorSubject<WidgetActionDescriptorInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<WidgetActionDescriptorInfo>>(emptyPageData<WidgetActionDescriptorInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  private allActions: Observable<Array<WidgetActionDescriptorInfo>>;

  private actionsMap: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  private actionSources: {[actionSourceId: string]: WidgetActionSource};

  constructor(private translate: TranslateService,
              private utils: UtilsService) {}

  connect(collectionViewer: CollectionViewer): Observable<WidgetActionDescriptorInfo[] | ReadonlyArray<WidgetActionDescriptorInfo>> {
    return this.actionsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
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
        publishReplay(1),
        refCount()
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
