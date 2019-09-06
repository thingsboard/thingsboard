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

import { Injectable } from '@angular/core';
import { defaultHttpOptions } from './http-utils';
import { Observable } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetType } from '@shared/models/widget.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '../services/resources.service';

@Injectable({
  providedIn: 'root'
})
export class WidgetService {

  constructor(
    private http: HttpClient,
    private utils: UtilsService,
    private resources: ResourcesService,
    private translate: TranslateService
  ) {
  }

  public getWidgetBundles(pageLink: PageLink, ignoreErrors: boolean = false,
                          ignoreLoading: boolean = false): Observable<PageData<WidgetsBundle>> {
    return this.http.get<PageData<WidgetsBundle>>(`/api/widgetsBundles${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getWidgetsBundle(widgetsBundleId: string,
                          ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetsBundle> {
    return this.http.get<WidgetsBundle>(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveWidgetsBundle(widgetsBundle: WidgetsBundle,
                           ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetsBundle> {
    return this.http.post<WidgetsBundle>('/api/widgetsBundle', widgetsBundle, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteWidgetsBundle(widgetsBundleId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/widgetsBundle/${widgetsBundleId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getBundleWidgetTypes(bundleAlias: string, isSystem: boolean,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Array<WidgetType>> {
    return this.http.get<Array<WidgetType>>(`/api/widgetTypes?isSystem=${isSystem}&bundleAlias=${bundleAlias}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getWidgetType(bundleAlias: string, widgetTypeAlias: string, isSystem: boolean,
                       ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<WidgetType> {
    return this.http.get<WidgetType>(`/api/widgetType?isSystem=${isSystem}&bundleAlias=${bundleAlias}&alias=${widgetTypeAlias}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }
}
