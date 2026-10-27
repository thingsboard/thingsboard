// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export type ProductStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED';

export interface ProductItem {
  tenantId: string;
  productId: string;
  name: string;
  model: string;
  categoryId: string;
  tslVersion: string;
  status: ProductStatus;
  propertyCount: number;
  eventCount: number;
  actionCount: number;
  tbProfileId: string;
  tbAssetId: string;
  tbRuleChainId: string;
  updatedAt: string;
}

export interface ProductDetail {
  product: ProductItem;
  versions: ProductVersion[];
  catalog: ProductCatalog;
}

export interface ProductVersion {
  version: string;
  status: ProductStatus;
  checksum: string;
  publishedAt: string;
  createdAt: string;
}

export interface ProductCatalog {
  productId: string;
  version: string;
  properties: ProductProperty[];
  events: ProductEvent[];
  actions: ProductAction[];
}

export interface ProductProperty {
  id: string;
  name: string;
  dataType: string;
  mode: string;
  unit: string;
  min: number;
  max: number;
  step: number;
  required: boolean;
  mapping: any;
}

export interface ProductEvent {
  id: string;
  name: string;
  type: string;
  params: any;
}

export interface ProductAction {
  id: string;
  name: string;
  input: any;
  output: any;
}

export interface ProductResponse {
  productId: string;
  tslVersion: string;
  tbProfileId: string;
  tbAssetId: string;
  tbRuleChainId: string;
  propertyCount: number;
  eventCount: number;
  actionCount: number;
  schemaBytes: number;
  status: ProductStatus;
  warnings: ProductIssue[];
}

export interface ProductValidationResult {
  valid: boolean;
  errors: ProductIssue[];
  warnings: ProductIssue[];
}

export interface ProductIssue {
  code: string;
  path: string;
  message: string;
}

export interface ThingModelView {
  productId: string;
  version: string;
  status: ProductStatus;
  strictUnknown: boolean;
  tsl: any;
}

export interface CreateProductRequest {
  tenantId?: string;
  productId?: string;
  name?: string;
  model?: string;
  categoryId?: string;
  tslVersion?: string;
  tsl: any;
  strictUnknown?: boolean;
}

export interface UpdateThingModelRequest {
  tslVersion?: string;
  tsl: any;
  strictUnknown?: boolean;
}

export interface ProductIssueDialogData {
  title: string;
  issues: ProductIssue[];
}
