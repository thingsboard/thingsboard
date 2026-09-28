// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ActivatedRouteSnapshot, Params } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { HasUUID } from '@shared/models/id/has-uuid';
import { MenuId } from '@core/services/menu.models';
import { Authority } from '@shared/models/authority.enum';

export interface BreadCrumb extends HasUUID{
  label: string;
  customTranslate: boolean;
  labelFunction?: () => string;
  link: any[] | string;
  queryParams: Params;
}

export type BreadCrumbLabelFunction<C> = (
  route: ActivatedRouteSnapshot,
  translate: TranslateService,
  component: C,
  data?: any,
  utils?: UtilsService) => string;

export interface BreadCrumbConfig<C> {
  labelFunction: BreadCrumbLabelFunction<C>;
  menuId?: MenuId;
  menuIdByAuthority?: {[authority: string]: MenuId};
  label?: string;
  icon?: string;
  skip: boolean;
  custom: boolean;
  customChild: boolean;
}
