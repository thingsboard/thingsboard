// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { createDefaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Customer, CustomerInfo, ShortCustomerInfo } from '@shared/models/customer.model';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { SaveEntityWithGroupParams, toSaveParams } from '@shared/models/entity.models';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  constructor(
    private http: HttpClient
  ) { }

  public getCustomers(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Customer>> {
    return this.http.get<PageData<Customer>>(`/api/customers${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomer(customerId: string, config?: RequestConfig): Observable<Customer> {
    return this.http.get<Customer>(`/api/customer/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomerInfo(customerId: string, config?: RequestConfig): Observable<CustomerInfo> {
    return this.http.get<CustomerInfo>(`/api/customer/info/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveCustomer(customer: Customer, entityGroupIds?: string | string[], config?: RequestConfig): Observable<Customer>;
  public saveCustomer(customer: Customer, saveParams?: SaveEntityWithGroupParams, config?: RequestConfig): Observable<Customer>;
  public saveCustomer(customer: Customer, saveParams?: string | string[] | SaveEntityWithGroupParams, config?: RequestConfig): Observable<Customer> {
    const params = toSaveParams(saveParams);
    return this.http.post<Customer>('/api/customer', customer, createDefaultHttpOptions(params, config));
  }

  public deleteCustomer(customerId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

  public getCustomersByIds(customerIds: Array<string>, config?: RequestConfig): Observable<Array<Customer>> {
    return this.http.get<Array<Customer>>(`/api/customers?customerIds=${customerIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((customers) => sortEntitiesByIds(customers, customerIds))
    );
  }

  public getUserCustomers(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Customer>> {
    return this.http.get<PageData<Customer>>(`/api/user/customers${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllCustomerInfos(includeCustomers: boolean,
                             pageLink: PageLink, config?: RequestConfig): Observable<PageData<CustomerInfo>> {
    let url = `/api/customerInfos/all${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<CustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerCustomerInfos(includeCustomers: boolean, customerId: string,
                                  pageLink: PageLink, config?: RequestConfig): Observable<PageData<CustomerInfo>> {
    let url = `/api/customer/${customerId}/customerInfos${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<CustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getShortCustomerInfo(customerId: string, config?: RequestConfig): Observable<ShortCustomerInfo> {
    return this.http.get<ShortCustomerInfo>(`/api/customer/${customerId}/shortInfo`, defaultHttpOptionsFromConfig(config));
  }

}
