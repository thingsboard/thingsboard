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
import { Customer } from '@shared/models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  constructor(
    private http: HttpClient
  ) { }

  public getCustomers(tenantId: string, pageLink: PageLink, ignoreErrors: boolean = false,
                      ignoreLoading: boolean = false): Observable<PageData<Customer>> {
    return this.http.get<PageData<Customer>>(`/api/tenant/${tenantId}/customers${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomer(customerId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Customer> {
    return this.http.get<Customer>(`/api/customer/${customerId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveCustomer(customer: Customer, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<Customer> {
    return this.http.post<Customer>('/api/customer', customer, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteCustomer(customerId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/customer/${customerId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}
