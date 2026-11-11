// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
export interface BlobEntitiesWidgetSettings {
  title: string;
  displayCreatedTime: boolean;
  displayType: boolean;
  displayCustomer: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  pageStepIncrement: number;
  pageStepCount: number;
  defaultSortOrder: string;
  noDataDisplayMessage: string;
  forceDefaultType: string;
}
