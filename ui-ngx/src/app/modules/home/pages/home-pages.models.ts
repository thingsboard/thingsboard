// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { ResourceInfo } from '@shared/models/resource.models';
import { OtaPackage } from '@shared/models/ota-package.models';

export const entityDetailsPageBreadcrumbLabelFunction: BreadCrumbLabelFunction<EntityDetailsPageComponent>
  = ((route, translate, component) => {
  switch (component.entitiesTableConfig.entityType) {
    case EntityType.TB_RESOURCE:
    case EntityType.OTA_PACKAGE:
      return (component.entity as ResourceInfo | OtaPackage)?.title;
    default:
      return component.entity?.name;
  }
});
