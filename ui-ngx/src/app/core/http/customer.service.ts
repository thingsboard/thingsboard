// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { createDefaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { Customer } from '@shared/models/customer.model';
import { SaveEntityParams } from '@shared/models/entity.models';

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

  public getCustomersByIds(customerIds: Array<string>, config?: RequestConfig): Observable<Array<Customer>> {
    return this.http.get<Array<Customer>>(`/api/customers?customerIds=${customerIds.join(',')}`, defaultHttpOptionsFromConfig(config));
  }

  public saveCustomer(customer: Customer, config?: RequestConfig): Observable<Customer>;
  public saveCustomer(customer: Customer, saveParams: SaveEntityParams, config?: RequestConfig): Observable<Customer>;
  public saveCustomer(customer: Customer, saveParamsOrConfig?: SaveEntityParams | RequestConfig, config?: RequestConfig): Observable<Customer> {
    return this.http.post<Customer>('/api/customer', customer, createDefaultHttpOptions(saveParamsOrConfig, config));
  }

  public deleteCustomer(customerId: string, config?: RequestConfig) {
    return this.http.delete(`/api/customer/${customerId}`, defaultHttpOptionsFromConfig(config));
  }

}
