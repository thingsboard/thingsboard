///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
