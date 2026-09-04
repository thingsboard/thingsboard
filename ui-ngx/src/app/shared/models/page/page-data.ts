// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface PageData<T> {
  data: Array<T>;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
}

export function emptyPageData<T>(): PageData<T> {
  return {
    data: [],
    totalPages: 0,
    totalElements: 0,
    hasNext: false
  } as PageData<T>;
}
