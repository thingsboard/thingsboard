// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { EntityType } from '@shared/models/entity-type.models';
import { ResourceInfo } from '@shared/models/resource.models';
import { OtaPackage } from '@shared/models/ota-package.models';
import { UtilsService } from '@core/services/utils.service';

export const entityDetailsPageBreadcrumbLabelFunction: BreadCrumbLabelFunction<EntityDetailsPageComponent>
  = ((route, translate, component, data,  utils) => {
  let label = '';
  switch (component.entitiesTableConfig.entityType) {
    case EntityType.TB_RESOURCE:
    case EntityType.OTA_PACKAGE:
      label = (component.entity as ResourceInfo | OtaPackage)?.title;
      break;
    default:
      label = component.entity?.name;
  }
  return utils ? utils.customTranslation(label) : label;
});
