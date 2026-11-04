// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AliasFilterType, EntityAliasFilter } from '@shared/models/alias.models';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { defaultSchedulerEventConfigTypes } from '@home/components/scheduler/scheduler-event-config.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-entity-filter-view',
    templateUrl: './entity-filter-view.component.html',
    styleUrls: ['./entity-filter-view.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityFilterViewComponent),
            multi: true
        }
    ],
    standalone: false
})
export class EntityFilterViewComponent implements ControlValueAccessor {

  @Input()
  @coerceBoolean()
  reportMode = false;

  @Input()
  @coerceBoolean()
  subReport = false;

  constructor(private translate: TranslateService) {}

  filterDisplayValue: string;
  filter: EntityAliasFilter;

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
  }

  writeValue(filter: EntityAliasFilter): void {
    this.filter = filter;
    if (this.filter && this.filter.type) {
      let entityType: EntityType | AliasEntityType;
      let prefix: string;
      let allEntitiesText;
      let anyRelationText;
      let relationTypeText;
      let rootEntityText;
      let directionText;
      let count: number;
      switch (this.filter.type) {
        case AliasFilterType.singleEntity:
          entityType = this.filter.singleEntity.entityType;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).list,
            {count: 1});
          break;
        case AliasFilterType.entityGroup:
          if (this.filter.groupStateEntity) {
            if (this.reportMode) {
              this.filterDisplayValue = this.translate.instant(this.subReport ? 'alias.entities-of-group-master-report-entity' : 'alias.entities-of-group-originator-entity');
            } else {
              this.filterDisplayValue = this.translate.instant('alias.entities-of-group-state-entity');
            }
          } else {
            entityType = this.filter.groupType;
            this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).group);
          }
          break;
        case AliasFilterType.entityList:
          entityType = this.filter.entityType;
          count = this.filter.entityList.length;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).list,
            {count});
          break;
        case AliasFilterType.entityName:
          entityType = this.filter.entityType;
          prefix = this.filter.entityNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).nameStartsWith,
            {prefix});
          break;
        case AliasFilterType.entityType:
          entityType = this.filter.entityType;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).typePlural);
          break;
        case AliasFilterType.entityGroupList:
          entityType = this.filter.groupType;
          count = this.filter.entityGroupList.length;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).groupList,
            {count});
          break;
        case AliasFilterType.entityGroupName:
          entityType = this.filter.groupType;
          prefix = this.filter.entityGroupNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).groupNameStartsWith,
            {prefix});
          break;
        case AliasFilterType.entitiesByGroupName:
          entityType = this.filter.groupType;
          prefix = this.filter.entityGroupNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).group) + ': ' + prefix;
          break;
        case AliasFilterType.stateEntity:
          if (this.reportMode) {
            this.filterDisplayValue = this.translate.instant(this.subReport ? 'alias.filter-type-state-entity-master-report-description' : 'alias.filter-type-state-entity-originator-description');
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-state-entity-description');
          }
          break;
        case AliasFilterType.stateEntityOwner:
          if (this.reportMode) {
            this.filterDisplayValue = this.translate.instant(this.subReport ? 'alias.filter-type-state-entity-owner-master-report-description' : 'alias.filter-type-state-entity-owner-originator-description');
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-state-entity-owner-description');
          }
          break;
        case AliasFilterType.assetType:
          const assetTypesQuoted = [];
          this.filter.assetTypes.forEach((filterAssetType) => {
            assetTypesQuoted.push(`'${filterAssetType}'`);
          });
          const assetTypes = assetTypesQuoted.join(', ');
          prefix = this.filter.assetNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-type-and-name-description',
              {assetTypes, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-type-description',
              {assetTypes});
          }
          break;
        case AliasFilterType.deviceType:
          const deviceTypesQuoted = [];
          this.filter.deviceTypes.forEach((filterDeviceType) => {
            deviceTypesQuoted.push(`'${filterDeviceType}'`);
          });
          const deviceTypes = deviceTypesQuoted.join(', ');
          prefix = this.filter.deviceNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-type-and-name-description',
              {deviceTypes, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-type-description',
              {deviceTypes});
          }
          break;
        case AliasFilterType.apiUsageState:
          this.filterDisplayValue = this.translate.instant('alias.filter-type-apiUsageState');
          break;
        case AliasFilterType.entityViewType:
          const entityViewTypesQuoted = [];
          this.filter.entityViewTypes.forEach((entityViewType) => {
            entityViewTypesQuoted.push(`'${entityViewType}'`);
          });
          const entityViewTypes = entityViewTypesQuoted.join(', ');
          prefix = this.filter.entityViewNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-type-and-name-description',
              {entityViewTypes, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-type-description',
              {entityViewTypes});
          }
          break;
        case AliasFilterType.edgeType:
          const edgeTypesQuoted = [];
          this.filter.edgeTypes.forEach((filterEdgeType) => {
            edgeTypesQuoted.push(`'${filterEdgeType}'`);
          });
          const edgeTypes = edgeTypesQuoted.join(', ');
          prefix = this.filter.edgeNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-type-and-name-description',
              {edgeTypes, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-type-description',
              {edgeTypes});
          }
          break;
        case AliasFilterType.relationsQuery:
          allEntitiesText = this.translate.instant('alias.all-entities');
          anyRelationText = this.translate.instant('alias.any-relation');
          if (this.filter.rootStateEntity) {
            if (this.reportMode) {
              rootEntityText = this.translate.instant(this.subReport ? 'alias.master-report-entity' : 'alias.originator-entity');
            } else {
              rootEntityText = this.translate.instant('alias.state-entity');
            }
          } else {
            rootEntityText = this.translate.instant(entityTypeTranslations.get(this.filter.rootEntity.entityType).type);
          }
          directionText = this.translate.instant('relation.direction-type.' + this.filter.direction);
          const relationFilters = this.filter.filters;
          if (relationFilters && relationFilters.length) {
            const relationFiltersDisplayValues = [];
            relationFilters.forEach((relationFilter) => {
              let entitiesText;
              if (relationFilter.entityTypes && relationFilter.entityTypes.length) {
                const entitiesNamesList = [];
                relationFilter.entityTypes.forEach((filterEntityType) => {
                  entitiesNamesList.push(
                    this.translate.instant(entityTypeTranslations.get(filterEntityType).typePlural)
                  );
                });
                entitiesText = entitiesNamesList.join(', ');
              } else {
                entitiesText = allEntitiesText;
              }
              if (relationFilter.relationType && relationFilter.relationType.length) {
                relationTypeText = `'${relationFilter.relationType}'`;
              } else {
                relationTypeText = anyRelationText;
              }
              const relationFilterDisplayValue = this.translate.instant('alias.filter-type-relations-query-description',
                {
                  entities: entitiesText,
                  relationType: relationTypeText,
                  direction: directionText,
                  rootEntity: rootEntityText
                }
              );
              relationFiltersDisplayValues.push(relationFilterDisplayValue);
            });
            this.filterDisplayValue = relationFiltersDisplayValues.join(', ');
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-relations-query-description',
              {
                entities: allEntitiesText,
                relationType: anyRelationText,
                direction: directionText,
                rootEntity: rootEntityText
              }
            );
          }
          break;
        case AliasFilterType.assetSearchQuery:
        case AliasFilterType.deviceSearchQuery:
        case AliasFilterType.edgeSearchQuery:
        case AliasFilterType.entityViewSearchQuery:
          allEntitiesText = this.translate.instant('alias.all-entities');
          anyRelationText = this.translate.instant('alias.any-relation');
          if (this.filter.rootStateEntity) {
            if (this.reportMode) {
              rootEntityText = this.translate.instant(this.subReport ? 'alias.master-report-entity' : 'alias.originator-entity');
            } else {
              rootEntityText = this.translate.instant('alias.state-entity');
            }
          } else {
            rootEntityText = this.translate.instant(entityTypeTranslations.get(this.filter.rootEntity.entityType).type);
          }
          directionText = this.translate.instant('relation.direction-type.' + this.filter.direction);
          if (this.filter.relationType && this.filter.relationType.length) {
            relationTypeText = `'${filter.relationType}'`;
          } else {
            relationTypeText = anyRelationText;
          }

          const translationValues: any = {
            relationType: relationTypeText,
            direction: directionText,
            rootEntity: rootEntityText
          };

          if (this.filter.type === AliasFilterType.assetSearchQuery) {
            const assetTypesQuoted = [];
            this.filter.assetTypes.forEach((filterAssetType) => {
              assetTypesQuoted.push(`'${filterAssetType}'`);
            });
            const assetTypesText = assetTypesQuoted.join(', ');
            translationValues.assetTypes = assetTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.deviceSearchQuery) {
            const deviceTypesQuoted = [];
            this.filter.deviceTypes.forEach((filterDeviceType) => {
              deviceTypesQuoted.push(`'${filterDeviceType}'`);
            });
            const deviceTypesText = deviceTypesQuoted.join(', ');
            translationValues.deviceTypes = deviceTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.edgeSearchQuery) {
            const edgeTypesQuoted = [];
            this.filter.edgeTypes.forEach((filterEdgeType) => {
              edgeTypesQuoted.push(`'${filterEdgeType}'`);
            });
            const edgeTypesText = edgeTypesQuoted.join(', ');
            translationValues.edgeTypes = edgeTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.entityViewSearchQuery) {
            const entityViewTypesQuoted = [];
            this.filter.entityViewTypes.forEach((filterEntityViewType) => {
              entityViewTypesQuoted.push(`'${filterEntityViewType}'`);
            });
            const entityViewTypesText = entityViewTypesQuoted.join(', ');
            translationValues.entityViewTypes = entityViewTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-search-query-description',
              translationValues
            );
          }
          break;
        case AliasFilterType.schedulerEvent:
          if (this.filter.eventType) {
            let eventTypeName = this.filter.eventType;
            if (defaultSchedulerEventConfigTypes[this.filter.eventType]) {
              eventTypeName = defaultSchedulerEventConfigTypes[this.filter.eventType].name;
            }
            const interpolateParams = {
              eventType: eventTypeName
            };
            if (this.filter.originator || this.filter.originatorStateEntity) {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-type-originator-description',
                interpolateParams
              );
            } else {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-type-description',
                interpolateParams
              );
            }
          } else {
            if (this.filter.originator || this.filter.originatorStateEntity) {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-originator-description');
            } else {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event');
            }
          }
          break;
        default:
          this.filterDisplayValue = this.filter.type;
          break;
      }
    } else {
      this.filterDisplayValue = '';
    }
  }
}
