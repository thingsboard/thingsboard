/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import './nav-tree.scss';

/* eslint-disable import/no-unresolved, import/default */

import navTreeTemplate from './nav-tree.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.navTree', [])
    .directive('tbNavTree', NavTree)
    .name;

/*@ngInject*/
function NavTree() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            loadNodes: '=',
            editCallbacks: '=',
            enableSearch: '@?',
            onNodeSelected: '&',
            onNodesInserted: '&',
            searchCallback: '&?'
        },
        controller: NavTreeController,
        controllerAs: 'vm',
        templateUrl: navTreeTemplate
    };
}

/*@ngInject*/
function NavTreeController($scope, $element, types) {

    var vm = this;
    vm.types = types;

    $scope.$watch('vm.loadNodes', (newVal) => {
        if (newVal) {
            initTree();
        }
    });

    function initTree() {
        var config = {
            core: {
                multiple: false,
                check_callback: true,
                themes: { name: 'proton', responsive: true },
                data: vm.loadNodes
            }
        };

        if (vm.enableSearch) {
            config.plugins = ["search"];
            config.search = {
                case_sensitive: false,
                show_only_matches: true,
                show_only_matches_children: false,
                search_leaves_only: false
            };
            if (vm.searchCallback) {
                config.search.search_callback = (searchText, node) => vm.searchCallback({searchText: searchText, node: node});
            }
        }

        vm.treeElement = angular.element('.tb-nav-tree-container', $element)
            .jstree(config);

        vm.treeElement.on("changed.jstree", function (e, data) {
            if (vm.onNodeSelected) {
                vm.onNodeSelected({node: data.instance.get_selected(true)[0], event: e});
            }
        });

        vm.treeElement.on("model.jstree", function (e, data) {
            if (vm.onNodesInserted) {
                vm.onNodesInserted({nodes: data.nodes, parent: data.parent});
            }
        });

        if (vm.editCallbacks) {
            vm.editCallbacks.selectNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('deselect_all', true);
                    vm.treeElement.jstree('select_node', node);
                }
            };
            vm.editCallbacks.deselectAll = () => {
                vm.treeElement.jstree('deselect_all');
            };
            vm.editCallbacks.getNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                return node;
            };
            vm.editCallbacks.getParentNodeId = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('get_parent', node);
                }
            };
            vm.editCallbacks.openNode = (id, cb) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('open_node', node, cb);
                }
            };
            vm.editCallbacks.nodeIsOpen = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('is_open', node);
                } else {
                    return true;
                }
            };
            vm.editCallbacks.nodeIsLoaded = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    return vm.treeElement.jstree('is_loaded', node);
                } else {
                    return true;
                }
            };
            vm.editCallbacks.refreshNode = (id) => {
                if (id === '#') {
                    vm.treeElement.jstree('refresh');
                    vm.treeElement.jstree('redraw');
                } else {
                    var node = vm.treeElement.jstree('get_node', id);
                    if (node) {
                        var opened = vm.treeElement.jstree('is_open', node);
                        vm.treeElement.jstree('refresh_node', node);
                        vm.treeElement.jstree('redraw');
                        if (node.children && opened/* && !node.children.length*/) {
                            vm.treeElement.jstree('open_node', node);
                        }
                    }
                }
            };
            vm.editCallbacks.updateNode = (id, newName) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('rename_node', node, newName);
                }
            };
            vm.editCallbacks.createNode = (parentId, node, pos) => {
                var parentNode = vm.treeElement.jstree('get_node', parentId);
                if (parentNode) {
                    vm.treeElement.jstree('create_node', parentNode, node, pos);
                }
            };
            vm.editCallbacks.deleteNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('delete_node', node);
                }
            };
            vm.editCallbacks.disableNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('disable_node', node);
                }
            };
            vm.editCallbacks.enableNode = (id) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    vm.treeElement.jstree('enable_node', node);
                }
            };
            vm.editCallbacks.setNodeHasChildren = (id, hasChildren) => {
                var node = vm.treeElement.jstree('get_node', id);
                if (node) {
                    if (!node.children || !node.children.length) {
                        node.children = hasChildren;
                        node.state.loaded = !hasChildren;
                        node.state.opened = false;
                        vm.treeElement.jstree('_node_changed', node.id);
                        vm.treeElement.jstree('redraw');
                    }
                }
            };
            vm.editCallbacks.search = (searchText) => {
                vm.treeElement.jstree('search', searchText);
            };
            vm.editCallbacks.clearSearch = () => {
                vm.treeElement.jstree('clear_search');
            };
        }
    }
}
