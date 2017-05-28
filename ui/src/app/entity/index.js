/*
 * Copyright © 2016-2017 The Thingsboard Authors
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

import EntityAliasesController from './entity-aliases.controller';
import EntityTypeSelectDirective from './entity-type-select.directive';
import EntitySubtypeSelectDirective from './entity-subtype-select.directive';
import EntitySubtypeAutocompleteDirective from './entity-subtype-autocomplete.directive';
import EntityFilterDirective from './entity-filter.directive';
import AliasesEntitySelectPanelController from './aliases-entity-select-panel.controller';
import AliasesEntitySelectDirective from './aliases-entity-select.directive';
import AddAttributeDialogController from './attribute/add-attribute-dialog.controller';
import AddWidgetToDashboardDialogController from './attribute/add-widget-to-dashboard-dialog.controller';
import AttributeTableDirective from './attribute/attribute-table.directive';

export default angular.module('thingsboard.entity', [])
    .controller('EntityAliasesController', EntityAliasesController)
    .controller('AliasesEntitySelectPanelController', AliasesEntitySelectPanelController)
    .controller('AddAttributeDialogController', AddAttributeDialogController)
    .controller('AddWidgetToDashboardDialogController', AddWidgetToDashboardDialogController)
    .directive('tbEntityTypeSelect', EntityTypeSelectDirective)
    .directive('tbEntitySubtypeSelect', EntitySubtypeSelectDirective)
    .directive('tbEntitySubtypeAutocomplete', EntitySubtypeAutocompleteDirective)
    .directive('tbEntityFilter', EntityFilterDirective)
    .directive('tbAliasesEntitySelect', AliasesEntitySelectDirective)
    .directive('tbAttributeTable', AttributeTableDirective)
    .name;
