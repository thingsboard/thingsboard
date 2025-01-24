///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
// [TODO]: [Calculated fields] - implement when BE ready
export class CalculatedFieldsService {

  fieldsMock = [
    {
      name: 'Calculated Field 1',
      type: 'Simple',
      expression: '1 + 2',
      id: {
        id: '1',
      }
    },
    {
      name: 'Calculated Field 2',
      type: 'Script',
      expression: '${power}',
      id: {
        id: '2',
      }
    }
  ];

  constructor(
    private http: HttpClient
  ) { }

  public getCalculatedField(calculatedFieldId: string, config?: RequestConfig): Observable<any> {
    return of(this.fieldsMock[0]);
    // return this.http.get<any>(`/api/calculated-field/${calculatedFieldId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveCalculatedField(calculatedField: any, config?: RequestConfig): Observable<any> {
    return of(this.fieldsMock[1]);
    // return this.http.post<any>('/api/calculated-field', calculatedField, defaultHttpOptionsFromConfig(config));
  }

  public deleteCalculatedField(calculatedFieldId: string, config?: RequestConfig): Observable<boolean> {
    return of(true);
    // return this.http.delete<boolean>(`/api/calculated-field/${calculatedFieldId}`, defaultHttpOptionsFromConfig(config));
  }

  public getCalculatedFields(query: any,
                   config?: RequestConfig): Observable<PageData<any>> {
    return of({
      data: this.fieldsMock,
      totalPages: 1,
      totalElements: 2,
      hasNext: false,
    });
    // return this.http.get<PageData<any>>(`/api/calculated-field${query.toQuery()}`,
    //   defaultHttpOptionsFromConfig(config));
  }
}
