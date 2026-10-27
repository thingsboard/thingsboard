// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import {
  CreateProductRequest,
  ProductDetail,
  ProductItem,
  ProductResponse,
  ProductValidationResult,
  ThingModelView,
  UpdateThingModelRequest
} from '@shared/models/product.models';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private http: HttpClient) {
  }

  listProducts(keyword?: string, config?: RequestConfig): Observable<ProductItem[]> {
    let url = '/api/thing-model/products';
    if (keyword && keyword.trim().length) {
      url += `?keyword=${encodeURIComponent(keyword.trim())}`;
    }
    return this.http.get<ProductItem[]>(url, defaultHttpOptionsFromConfig(config));
  }

  getProduct(productId: string, config?: RequestConfig): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(
      `/api/thing-model/products/${encodeURIComponent(productId)}`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  getThingModel(productId: string, config?: RequestConfig): Observable<ThingModelView> {
    return this.http.get<ThingModelView>(
      `/api/thing-model/products/${encodeURIComponent(productId)}/thing-model`,
      defaultHttpOptionsFromConfig(config)
    );
  }

  createProduct(request: CreateProductRequest, config?: RequestConfig): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(
      '/api/thing-model/products',
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  updateThingModel(productId: string, request: UpdateThingModelRequest,
                   config?: RequestConfig): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(
      `/api/thing-model/products/${encodeURIComponent(productId)}/thing-model`,
      request,
      defaultHttpOptionsFromConfig(config)
    );
  }

  validateThingModel(productId: string, tsl: any,
                     config?: RequestConfig): Observable<ProductValidationResult> {
    return this.http.post<ProductValidationResult>(
      `/api/thing-model/products/${encodeURIComponent(productId)}/thing-model/validate`,
      {tsl},
      defaultHttpOptionsFromConfig(config)
    );
  }

  syncProduct(productId: string, config?: RequestConfig): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(
      `/api/thing-model/products/${encodeURIComponent(productId)}/sync`,
      null,
      defaultHttpOptionsFromConfig(config)
    );
  }
}
