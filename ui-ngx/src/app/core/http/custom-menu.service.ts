// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  CMAssigneeType, CMScope,
  CustomMenu,
  CustomMenuConfig,
  CustomMenuDeleteResult,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
import { mergeMap, Observable, of, Subject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageData } from '@shared/models/page/page-data';
import { EntityInfoData } from '@shared/models/entity.models';

@Injectable({
  providedIn: 'root'
})
export class CustomMenuService {

  private customMenuConfig: CustomMenuConfig = null;

  private customMenuConfigChanged: Subject<CustomMenuConfig> = new Subject<CustomMenuConfig>();

  public customMenuConfigChanged$: Observable<CustomMenuConfig> = this.customMenuConfigChanged.asObservable();

  constructor(
    private http: HttpClient
  ) {}

  public getCustomMenu(): CustomMenuConfig {
    return this.customMenuConfig;
  }

  public loadCustomMenu(notify = false): Observable<CustomMenuConfig> {
    return this.http.get<CustomMenuConfig>('/api/customMenu')
    .pipe(
      tap((customMenuConfig) => {
        this.customMenuConfig = customMenuConfig;
        if (notify) {
          this.customMenuConfigChanged.next(customMenuConfig);
        }
      })
    );
  }

  public getCustomMenuInfos(pageLink: PageLink, scope?: CMScope, assigneeType?: CMAssigneeType,
                            config?: RequestConfig): Observable<PageData<CustomMenuInfo>> {
    let url = `/api/customMenu/infos${pageLink.toQuery()}`;
    if (scope) {
      url += `&scope=${scope}`;
    }
    if (assigneeType) {
      url += `&assigneeType=${assigneeType}`;
    }
    return this.http.get<PageData<CustomMenuInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuInfo(customMenuId: string, config?: RequestConfig): Observable<CustomMenuInfo> {
    return this.http.get<CustomMenuInfo>(`/api/customMenu/${customMenuId}/info`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuConfig(customMenuId: string, config?: RequestConfig): Observable<CustomMenuConfig> {
    return this.http.get<CustomMenuConfig>(`/api/customMenu/${customMenuId}/config`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomMenuAssigneeList(customMenuId: string, config?: RequestConfig): Observable<Array<EntityInfoData>> {
    return this.http.get<Array<EntityInfoData>>(`/api/customMenu/${customMenuId}/assigneeList`, defaultHttpOptionsFromConfig(config));
  }

  public updateCustomMenuConfig(customMenuId: string, customMenuConfig: CustomMenuConfig, config?: RequestConfig): Observable<CustomMenu> {
    return this.http.put<CustomMenu>(`/api/customMenu/${customMenuId}/config`, customMenuConfig,
      defaultHttpOptionsFromConfig(config)).pipe(
        mergeMap((res) => this.loadCustomMenu(true).pipe( map(() => res) ))
    );
  }

  public updateCustomMenuName(customMenuId: string, name: string, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/customMenu/${customMenuId}/name`, name,
      defaultHttpOptionsFromConfig(config));
  }

  public assignCustomMenu(customMenuId: string, assigneeType: CMAssigneeType,
                          entityIds: string[], force = false, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/customMenu/${customMenuId}/assign/${assigneeType}?force=${force}`, entityIds,
      defaultHttpOptionsFromConfig(config)).pipe(
        mergeMap((res) => this.loadCustomMenu(true).pipe( map(() => res) ))
    );
  }

  public saveCustomMenu(customMenuInfo: CustomMenuInfo, assignToList?: string[],
                        force = false, config?: RequestConfig): Observable<CustomMenu> {
    let url = `/api/customMenu?force=${force}`;
    if (assignToList && Array.isArray(assignToList)) {
        url += `&assignToList=${assignToList.join(',')}`;
    }
    return this.http.post<CustomMenu>(url, customMenuInfo,
      defaultHttpOptionsFromConfig(config)).pipe(
        mergeMap((res) => this.loadCustomMenu(true).pipe( map(() => res) ))
    );
  }

  public deleteCustomMenu(customMenuId: string, force = false, config?: RequestConfig): Observable<CustomMenuDeleteResult> {
    return this.http.delete<CustomMenuDeleteResult>(`/api/customMenu/${customMenuId}?force=${force}`,
      defaultHttpOptionsFromConfig(config)).pipe(
        mergeMap((res) => (res.success ? this.loadCustomMenu(true) : of(null)).pipe( map(() => res) ))
    );
  }

}
