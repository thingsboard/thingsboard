// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface ISearchableComponent {
  onSearchTextUpdated(searchText: string);
}

export function instanceOfSearchableComponent(object: any): object is ISearchableComponent {
  return 'onSearchTextUpdated' in object;
}
