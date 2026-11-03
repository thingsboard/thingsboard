// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData, HasId } from './base-data';

export interface ContactBased<T extends HasId> extends BaseData<T> {
  country: string;
  state: string;
  city: string;
  address: string;
  address2: string;
  zip: string;
  phone: string;
  email: string;
}
