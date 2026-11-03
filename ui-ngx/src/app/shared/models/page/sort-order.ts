// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface SortOrder {
  property: string;
  direction: Direction;
}

export enum Direction {
  ASC = 'ASC',
  DESC = 'DESC'
}

export function sortOrderFromString(strSortOrder: string): SortOrder {
  let property: string;
  let direction = Direction.ASC;
  if (strSortOrder.startsWith('-')) {
    direction = Direction.DESC;
    property = strSortOrder.substring(1);
  } else {
    if (strSortOrder.startsWith('+')) {
      property = strSortOrder.substring(1);
    } else {
      property = strSortOrder;
    }
  }
  return {property, direction};
}
