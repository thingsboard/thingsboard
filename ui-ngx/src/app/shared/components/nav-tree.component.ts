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

import { Component, ElementRef, Input, NgZone, OnInit, ViewEncapsulation } from '@angular/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { deepClone } from '@core/utils';

export interface NavTreeNodeState {
  disabled?: boolean;
  opened?: boolean;
  loaded?: boolean;
}

export interface NavTreeNode {
  id: string;
  icon?: boolean;
  text?: string;
  state?: NavTreeNodeState;
  children?: NavTreeNode[] | boolean;
  data?: any;
}

export interface NavTreeEditCallbacks {
  selectNode?: (id: string) => void;
  deselectAll?: () => void;
  getNode?: (id: string) => NavTreeNode;
  getParentNodeId?: (id: string) => string;
  openNode?: (id: string, cb?: () => void) => void;
  nodeIsOpen?: (id: string) => boolean;
  nodeIsLoaded?: (id: string) => boolean;
  refreshNode?: (id: string) => void;
  updateNode?: (id: string, newName: string, updatedData?: any) => void;
  createNode?: (parentId: string, node: NavTreeNode, pos: number | string) => void;
  deleteNode?: (id: string) => void;
  disableNode?: (id: string) => void;
  enableNode?: (id: string) => void;
  setNodeHasChildren?: (id: string, hasChildren: boolean) => void;
  search?: (searchText: string) => void;
  clearSearch?: () => void;
}

export type NodesCallback = (nodes: NavTreeNode[]) => void;
export type LoadNodesCallback = (node: NavTreeNode, cb: NodesCallback) => void;
export type NodeSearchCallback = (searchText: string, node: NavTreeNode) => boolean;
export type NodeSelectedCallback = (node: NavTreeNode, event: Event) => void;
export type NodesInsertedCallback = (nodes: string[], parent: string) => void;

@Component({
  selector: 'tb-nav-tree',
  templateUrl: './nav-tree.component.html',
  styleUrls: ['./nav-tree.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class NavTreeComponent implements OnInit {

  private enableSearchValue: boolean;
  get enableSearch(): boolean {
    return this.enableSearchValue;
  }
  @Input()
  set enableSearch(value: boolean) {
    this.enableSearchValue = coerceBooleanProperty(value);
  }

  @Input()
  loadNodes: LoadNodesCallback;

  @Input()
  searchCallback: NodeSearchCallback;

  @Input()
  onNodeSelected: NodeSelectedCallback;

  @Input()
  onNodesInserted: NodesInsertedCallback;

  @Input()
  editCallbacks: NavTreeEditCallbacks;

  private treeElement: JSTree;

  constructor(private elementRef: ElementRef<HTMLElement>,
              private ngZone: NgZone) {
  }

  ngOnInit(): void {
    this.initTree();
  }

  private initTree() {

    const loadNodes: LoadNodesCallback = (node, cb) => {
      const outCb = (nodes: NavTreeNode[]) => {
        const copied: NavTreeNode[] = [];
        if (nodes) {
          nodes.forEach((n) => {
            copied.push(deepClone(n, ['data']));
          });
        }
        cb(copied);
      };
      this.ngZone.runOutsideAngular(() => {
        this.loadNodes(node, outCb);
      });
    };

    const config: JSTreeStaticDefaults = {
      core: {
        worker: false,
        multiple: false,
        check_callback: true,
        themes: { name: 'proton', responsive: true },
        data: loadNodes,
        error: () => {
          console.error('Unexpected jstree error!');
        }
      },
      plugins: []
    };

    if (this.enableSearch) {
      config.plugins.push('search');
      config.search = {
        ajax: false,
        fuzzy: false,
        close_opened_onclear: true,
        case_sensitive: false,
        show_only_matches: true,
        show_only_matches_children: false,
        search_leaves_only: false,
        search_callback: this.searchCallback
      };
    }

    import('jstree').then(() => {

      this.treeElement = $('.tb-nav-tree-container', this.elementRef.nativeElement).jstree(config);

      this.treeElement.on('changed.jstree', (e: any, data) => {
        if (this.onNodeSelected && data.action !== 'ready') {
          const node: NavTreeNode = data.instance.get_selected(true)[0];
          this.ngZone.run(() => this.onNodeSelected(node, e as Event));
        }
      });

      this.treeElement.on('model.jstree', (e: any, data) => {
        if (this.onNodesInserted) {
          this.ngZone.run(() => this.onNodesInserted(data.nodes, data.parent));
        }
      });

      if (this.editCallbacks) {
        this.editCallbacks.selectNode = id => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('deselect_all', true);
            this.treeElement.jstree('select_node', node);
          }
        };
        this.editCallbacks.deselectAll = () => {
          this.treeElement.jstree('deselect_all');
        };
        this.editCallbacks.getNode = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          return node;
        };
        this.editCallbacks.getParentNodeId = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            return this.treeElement.jstree('get_parent', node);
          }
        };
        this.editCallbacks.openNode = (id, cb) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('open_node', node, cb);
          }
        };
        this.editCallbacks.nodeIsOpen = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            return this.treeElement.jstree('is_open', node);
          } else {
            return true;
          }
        };
        this.editCallbacks.nodeIsLoaded = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            return this.treeElement.jstree('is_loaded', node);
          } else {
            return true;
          }
        };
        this.editCallbacks.refreshNode = (id) => {
          if (id === '#') {
            this.treeElement.jstree('refresh');
            this.treeElement.jstree('redraw');
          } else {
            const node: NavTreeNode = this.treeElement.jstree('get_node', id);
            if (node) {
              const opened = this.treeElement.jstree('is_open', node);
              this.treeElement.jstree('refresh_node', node);
              this.treeElement.jstree('redraw');
              if (node.children && opened/* && !node.children.length*/) {
                this.treeElement.jstree('open_node', node);
              }
            }
          }
        };
        this.editCallbacks.updateNode = (id, newName, updatedData) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('rename_node', node, newName);
          }
          if (updatedData && node.data) {
            Object.assign(node.data, updatedData);
          }
        };
        this.editCallbacks.createNode = (parentId, node, pos) => {
          const parentNode: NavTreeNode = this.treeElement.jstree('get_node', parentId);
          if (parentNode) {
            this.treeElement.jstree('create_node', parentNode, node, pos);
          }
        };
        this.editCallbacks.deleteNode = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('delete_node', node);
          }
        };
        this.editCallbacks.disableNode = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('disable_node', node);
          }
        };
        this.editCallbacks.enableNode = (id) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            this.treeElement.jstree('enable_node', node);
          }
        };
        this.editCallbacks.setNodeHasChildren = (id, hasChildren) => {
          const node: NavTreeNode = this.treeElement.jstree('get_node', id);
          if (node) {
            if (!node.children || (Array.isArray(node.children) && !node.children.length)) {
              node.children = hasChildren;
              node.state.loaded = !hasChildren;
              node.state.opened = false;
              this.treeElement.jstree('_node_changed', node.id);
              this.treeElement.jstree('redraw');
            }
          }
        };
        this.editCallbacks.search = (searchText) => {
          this.treeElement.jstree('search', searchText);
        };
        this.editCallbacks.clearSearch = () => {
          this.treeElement.jstree('clear_search');
        };
      }
    });
  }
}
