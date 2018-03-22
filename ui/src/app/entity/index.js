/*
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import EntityAliasesController from './alias/entity-aliases.controller';
import EntityAliasDialogController from './alias/entity-alias-dialog.controller';
import EntityTypeSelectDirective from './entity-type-select.directive';
import EntityTypeListDirective from './entity-type-list.directive';
import EntitySubtypeListDirective from './entity-subtype-list.directive';
import EntitySubtypeSelectDirective from './entity-subtype-select.directive';
import EntitySubtypeAutocompleteDirective from './entity-subtype-autocomplete.directive';
import EntityAutocompleteDirective from './entity-autocomplete.directive';
import EntityListDirective from './entity-list.directive';
import EntitySelectDirective from './entity-select.directive';
import EntityFilterDirective from './entity-filter.directive';
import EntityFilterViewDirective from './entity-filter-view.directive';
import AliasesEntitySelectPanelController from './alias/aliases-entity-select-panel.controller';
import AliasesEntitySelectDirective from './alias/aliases-entity-select.directive';
import AddAttributeDialogController from './attribute/add-attribute-dialog.controller';
import AddWidgetToDashboardDialogController from './attribute/add-widget-to-dashboard-dialog.controller';
import AttributeTableDirective from './attribute/attribute-table.directive';
import RelationFiltersDirective from './relation/relation-filters.directive';
import RelationTableDirective from './relation/relation-table.directive';
import RelationTypeAutocompleteDirective from './relation/relation-type-autocomplete.directive';

export default angular.module('thingsboard.entity', [])
    .controller('EntityAliasesController', EntityAliasesController)
    .controller('EntityAliasDialogController', EntityAliasDialogController)
    .controller('AliasesEntitySelectPanelController', AliasesEntitySelectPanelController)
    .controller('AddAttributeDialogController', AddAttributeDialogController)
    .controller('AddWidgetToDashboardDialogController', AddWidgetToDashboardDialogController)
    .directive('tbEntityTypeSelect', EntityTypeSelectDirective)
    .directive('tbEntityTypeList', EntityTypeListDirective)
    .directive('tbEntitySubtypeList', EntitySubtypeListDirective)
    .directive('tbEntitySubtypeSelect', EntitySubtypeSelectDirective)
    .directive('tbEntitySubtypeAutocomplete', EntitySubtypeAutocompleteDirective)
    .directive('tbEntityAutocomplete', EntityAutocompleteDirective)
    .directive('tbEntityList', EntityListDirective)
    .directive('tbEntitySelect', EntitySelectDirective)
    .directive('tbEntityFilter', EntityFilterDirective)
    .directive('tbEntityFilterView', EntityFilterViewDirective)
    .directive('tbAliasesEntitySelect', AliasesEntitySelectDirective)
    .directive('tbAttributeTable', AttributeTableDirective)
    .directive('tbRelationFilters', RelationFiltersDirective)
    .directive('tbRelationTable', RelationTableDirective)
    .directive('tbRelationTypeAutocomplete', RelationTypeAutocompleteDirective)
    .name;
