///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { AfterViewInit, Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog, MatExpansionPanel } from '@angular/material';
import { DialogService } from '@core/services/dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import {
  inputNodeComponent,
  ResolvedRuleChainMetaData,
  RuleChain,
  ruleChainNodeComponent
} from '@shared/models/rule-chain.models';
import { FlowchartConstants, UserCallbacks, NgxFlowchartComponent } from 'ngx-flowchart/dist/ngx-flowchart';
import {
  RuleNodeComponentDescriptor,
  RuleNodeType,
  ruleNodeTypeDescriptors,
  ruleNodeTypesLibrary
} from '@shared/models/rule-node.models';
import { FcRuleEdge, FcRuleNode, FcRuleNodeModel, FcRuleNodeType, FcRuleNodeTypeModel } from './rulechain-page.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { fromEvent, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import Timeout = NodeJS.Timeout;
import { ISearchableComponent } from '../../models/searchable-component.models';

@Component({
  selector: 'tb-rulechain-page',
  templateUrl: './rulechain-page.component.html',
  styleUrls: ['./rulechain-page.component.scss']
})
export class RuleChainPageComponent extends PageComponent
  implements AfterViewInit, OnInit, HasDirtyFlag, ISearchableComponent {

  get isDirty(): boolean {
    return this.isDirtyValue || this.isImport;
  }

  @ViewChild('ruleNodeSearchInput', {static: false}) ruleNodeSearchInputField: ElementRef;

  @ViewChild('ruleChainCanvas', {static: true}) ruleChainCanvas: NgxFlowchartComponent;

  @ViewChildren('ruleNodeTypeExpansionPanels',
    {read: MatExpansionPanel}) expansionPanels: QueryList<MatExpansionPanel>;

  ruleNodeTypeDescriptorsMap = ruleNodeTypeDescriptors;
  ruleNodeTypesLibraryArray = ruleNodeTypesLibrary;

  isImport: boolean;
  isDirtyValue: boolean;

  errorTooltips: {[nodeId: string]: JQueryTooltipster.ITooltipsterInstance} = {};
  isFullscreen = false;

  editingRuleNode = null;
  isEditingRuleNode = false;

  editingRuleNodeLink = null;
  isEditingRuleNodeLink = false;

  isLibraryOpen = true;

  ruleNodeSearch = '';
  ruleNodeTypeSearch = '';

  ruleChain: RuleChain;
  ruleChainMetaData: ResolvedRuleChainMetaData;

  ruleChainModel: FcRuleNodeModel = {
    nodes: [],
    edges: []
  };
  selectedObjects = [];

  editCallbacks: UserCallbacks = {
    edgeDoubleClick: (event, edge) => {
      console.log('TODO');
    },
    edgeEdit: (event, edge) => {
      console.log('TODO');
    },
    nodeCallbacks: {
      doubleClick: (event, node) => {
        console.log('TODO');
      },
      nodeEdit: (event, node) => {
        console.log('TODO');
      },
      mouseEnter: this.displayNodeDescriptionTooltip.bind(this),
      mouseLeave: this.destroyTooltips.bind(this),
      mouseDown: this.destroyTooltips.bind(this)
    },
    isValidEdge: (source, destination) => {
      return source.type === FlowchartConstants.rightConnectorType && destination.type === FlowchartConstants.leftConnectorType;
    },
    createEdge: (event, edge) => {
      console.log('TODO');
      return of(edge);
    },
    dropNode: (event, node) => {
      console.log('TODO dropNode');
      console.log(node);
    }
  };

  nextNodeID: number;
  nextConnectorID: number;
  inputConnectorId: number;

  ruleNodeTypesModel: {[type: string]: {model: FcRuleNodeTypeModel, selectedObjects: any[]}} = {};

  nodeLibCallbacks: UserCallbacks = {
    nodeCallbacks: {
      mouseEnter: this.displayLibNodeDescriptionTooltip.bind(this),
      mouseLeave: this.destroyTooltips.bind(this),
      mouseDown: this.destroyTooltips.bind(this)
    }
  };

  ruleNodeComponents: Array<RuleNodeComponentDescriptor>;

  flowchartConstants = FlowchartConstants;

  private tooltipTimeout: Timeout;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private router: Router,
              private ruleChainService: RuleChainService,
              private authService: AuthService,
              private translate: TranslateService,
              public dialog: MatDialog,
              public dialogService: DialogService,
              public fb: FormBuilder) {
    super(store);
    this.init();
  }

  ngOnInit() {
  }

  ngAfterViewInit() {
    fromEvent(this.ruleNodeSearchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.updateRuleChainLibrary();
        })
      )
      .subscribe();
  }

  onSearchTextUpdated(searchText: string) {
    this.ruleNodeSearch = searchText;
    this.updateRuleNodesHighlight();
  }

  private init() {
    this.ruleChain = this.route.snapshot.data.ruleChain;
    if (this.route.snapshot.data.import && !this.ruleChain) {
      this.router.navigateByUrl('ruleChains');
      return;
    }
    this.isImport = this.route.snapshot.data.import;
    this.ruleChainMetaData = this.route.snapshot.data.ruleChainMetaData;
    this.ruleNodeComponents = this.route.snapshot.data.ruleNodeComponents;
    for (const type of ruleNodeTypesLibrary) {
      const desc = ruleNodeTypeDescriptors.get(type);
      if (!desc.special) {
        this.ruleNodeTypesModel[type] = {
          model: {
            nodes: [],
            edges: []
          },
          selectedObjects: []
        };
      }
    }
    this.updateRuleChainLibrary();
    this.createRuleChainModel();
  }

  private updateRuleChainLibrary() {
    const search = this.ruleNodeTypeSearch.toUpperCase();
    const res = this.ruleNodeComponents.filter(
      (ruleNodeComponent) => ruleNodeComponent.name.toUpperCase().includes(search));
    this.loadRuleChainLibrary(res);
  }

  private loadRuleChainLibrary(ruleNodeComponents: Array<RuleNodeComponentDescriptor>) {
    for (const componentType of Object.keys(this.ruleNodeTypesModel)) {
      this.ruleNodeTypesModel[componentType].model.nodes.length = 0;
    }
    ruleNodeComponents.forEach((ruleNodeComponent) => {
      const componentType = ruleNodeComponent.type;
      const model = this.ruleNodeTypesModel[componentType].model;
      const desc = ruleNodeTypeDescriptors.get(RuleNodeType[componentType]);
      let icon = desc.icon;
      let iconUrl = null;
      if (ruleNodeComponent.configurationDescriptor.nodeDefinition.icon) {
        icon = ruleNodeComponent.configurationDescriptor.nodeDefinition.icon;
      }
      if (ruleNodeComponent.configurationDescriptor.nodeDefinition.iconUrl) {
        iconUrl = ruleNodeComponent.configurationDescriptor.nodeDefinition.iconUrl;
      }
      const node: FcRuleNodeType = {
        id: 'node-lib-' + componentType + '-' + model.nodes.length,
        component: ruleNodeComponent,
        name: '',
        nodeClass: desc.nodeClass,
        icon,
        iconUrl,
        x: 30,
        y: 10 + 50 * model.nodes.length,
        connectors: []
      };
      if (ruleNodeComponent.configurationDescriptor.nodeDefinition.inEnabled) {
        node.connectors.push(
          {
            type: FlowchartConstants.leftConnectorType,
            id: (model.nodes.length * 2) + ''
          }
        );
      }
      if (ruleNodeComponent.configurationDescriptor.nodeDefinition.outEnabled) {
        node.connectors.push(
          {
            type: FlowchartConstants.rightConnectorType,
            id: (model.nodes.length * 2 + 1) + ''
          }
        );
      }
      model.nodes.push(node);
    });
    if (this.expansionPanels) {
      for (let i = 0; i < ruleNodeTypesLibrary.length; i++) {
        const panel = this.expansionPanels.find((item, index) => {
          return index === i;
        });
        if (panel) {
          const type = ruleNodeTypesLibrary[i];
          if (!this.ruleNodeTypesModel[type].model.nodes.length) {
            panel.close();
          } else {
            panel.open();
          }
        }
      }
    }
  }

  private createRuleChainModel() {
    this.nextNodeID = 1;
    this.nextConnectorID = 1;

    this.selectedObjects.length = 0;
    this.ruleChainModel.nodes.length = 0;
    this.ruleChainModel.edges.length = 0;

    this.inputConnectorId = this.nextConnectorID++;
    this.ruleChainModel.nodes.push(
      {
        id: 'rule-chain-node-' + this.nextNodeID++,
        component: inputNodeComponent,
        name: '',
        nodeClass: ruleNodeTypeDescriptors.get(RuleNodeType.INPUT).nodeClass,
        icon: ruleNodeTypeDescriptors.get(RuleNodeType.INPUT).icon,
        readonly: true,
        x: 50,
        y: 150,
        connectors: [
          {
            type: FlowchartConstants.rightConnectorType,
            id: this.inputConnectorId + ''
          },
        ]

      }
    );
    const nodes: FcRuleNode[] = [];
    this.ruleChainMetaData.nodes.forEach((ruleNode) => {
      const component = this.ruleChainService.getRuleNodeComponentByClazz(ruleNode.type);
      const descriptor = ruleNodeTypeDescriptors.get(component.type);
      let icon = descriptor.icon;
      let iconUrl = null;
      if (component.configurationDescriptor.nodeDefinition.icon) {
        icon = component.configurationDescriptor.nodeDefinition.icon;
      }
      if (component.configurationDescriptor.nodeDefinition.iconUrl) {
        iconUrl = component.configurationDescriptor.nodeDefinition.iconUrl;
      }
      const node: FcRuleNode = {
        id: 'rule-chain-node-' + this.nextNodeID++,
        ruleNodeId: ruleNode.id,
        additionalInfo: ruleNode.additionalInfo,
        configuration: ruleNode.configuration,
        debugMode: ruleNode.debugMode,
        x: Math.round(ruleNode.additionalInfo.layoutX),
        y: Math.round(ruleNode.additionalInfo.layoutY),
        component,
        name: ruleNode.name,
        nodeClass: descriptor.nodeClass,
        icon,
        iconUrl,
        connectors: []
      };
      if (component.configurationDescriptor.nodeDefinition.inEnabled) {
        node.connectors.push(
          {
            type: FlowchartConstants.leftConnectorType,
            id: (this.nextConnectorID++) + ''
          }
        );
      }
      if (component.configurationDescriptor.nodeDefinition.outEnabled) {
        node.connectors.push(
          {
            type: FlowchartConstants.rightConnectorType,
            id: (this.nextConnectorID++) + ''
          }
        );
      }
      nodes.push(node);
      this.ruleChainModel.nodes.push(node);
    });
    if (this.ruleChainMetaData.firstNodeIndex > -1) {
      const destNode = nodes[this.ruleChainMetaData.firstNodeIndex];
      if (destNode) {
        const connectors = destNode.connectors.filter(connector => connector.type === FlowchartConstants.leftConnectorType);
        if (connectors && connectors.length) {
          const edge: FcRuleEdge = {
            source: this.inputConnectorId + '',
            destination: connectors[0].id
          };
          this.ruleChainModel.edges.push(edge);
        }
      }
    }
    if (this.ruleChainMetaData.connections) {
      const edgeMap: {[edgeKey: string]: FcRuleEdge} = {};
      this.ruleChainMetaData.connections.forEach((connection) => {
        const sourceNode = nodes[connection.fromIndex];
        const destNode = nodes[connection.toIndex];
        if (sourceNode && destNode) {
          const sourceConnectors = sourceNode.connectors.filter(connector => connector.type === FlowchartConstants.rightConnectorType);
          const destConnectors = destNode.connectors.filter(connector => connector.type === FlowchartConstants.leftConnectorType);
          if (sourceConnectors && sourceConnectors.length && destConnectors && destConnectors.length) {
            const sourceId = sourceConnectors[0].id;
            const destId = destConnectors[0].id;
            const edgeKey = sourceId + '_' + destId;
            let edge = edgeMap[edgeKey];
            if (!edge) {
              edge = {
                source: sourceId,
                destination: destId,
                label: connection.type,
                labels: [connection.type]
              };
              edgeMap[edgeKey] = edge;
              this.ruleChainModel.edges.push(edge);
            } else {
              edge.label += ' / ' + connection.type;
              edge.labels.push(connection.type);
            }
          }
        }
      });
    }
    if (this.ruleChainMetaData.ruleChainConnections) {
      const ruleChainsMap = this.ruleChainMetaData.targetRuleChainsMap;
      const ruleChainNodesMap: {[ruleChainNodeId: string]: FcRuleNode} = {};
      const ruleChainEdgeMap: {[edgeKey: string]: FcRuleEdge} = {};
      this.ruleChainMetaData.ruleChainConnections.forEach((ruleChainConnection) => {
        const ruleChain = ruleChainsMap[ruleChainConnection.targetRuleChainId.id];
        if (ruleChainConnection.additionalInfo && ruleChainConnection.additionalInfo.ruleChainNodeId) {
          let ruleChainNode = ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId];
          if (!ruleChainNode) {
            ruleChainNode = {
              id: 'rule-chain-node-' + this.nextNodeID++,
              name: ruleChain.name ? name : 'Unresolved',
              targetRuleChainId: ruleChain.name ? ruleChainConnection.targetRuleChainId.id : null,
              error: ruleChain.name ? undefined : this.translate.instant('rulenode.invalid-target-rulechain'),
              additionalInfo: ruleChainConnection.additionalInfo,
              x: Math.round(ruleChainConnection.additionalInfo.layoutX),
              y: Math.round(ruleChainConnection.additionalInfo.layoutY),
              component: ruleChainNodeComponent,
              nodeClass: ruleNodeTypeDescriptors.get(RuleNodeType.RULE_CHAIN).nodeClass,
              icon: ruleNodeTypeDescriptors.get(RuleNodeType.RULE_CHAIN).icon,
              connectors: [
                {
                  type: FlowchartConstants.leftConnectorType,
                  id: (this.nextConnectorID++) + ''
                }
              ]
            };
            ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId] = ruleChainNode;
            this.ruleChainModel.nodes.push(ruleChainNode);
          }
          const sourceNode = nodes[ruleChainConnection.fromIndex];
          if (sourceNode) {
            const connectors = sourceNode.connectors.filter(connector => connector.type === FlowchartConstants.rightConnectorType);
            if (connectors && connectors.length) {
              const sourceId = connectors[0].id;
              const destId = ruleChainNode.connectors[0].id;
              const edgeKey = sourceId + '_' + destId;
              let ruleChainEdge = ruleChainEdgeMap[edgeKey];
              if (!ruleChainEdge) {
                ruleChainEdge = {
                  source: sourceId,
                  destination: destId,
                  label: ruleChainConnection.type,
                  labels: [ruleChainConnection.type]
                };
                ruleChainEdgeMap[edgeKey] = ruleChainEdge;
                this.ruleChainModel.edges.push(ruleChainEdge);
              } else {
                ruleChainEdge.label += ' / ' + ruleChainConnection.type;
                ruleChainEdge.labels.push(ruleChainConnection.type);
              }
            }
          }
        }
      });
    }
    this.isDirtyValue = false;
  }

  onModelChanged() {
    console.log('Model changed!');
    this.isDirtyValue = true;
  }

  typeHeaderMouseEnter(event: MouseEvent, ruleNodeType: RuleNodeType) {
    const type = ruleNodeTypeDescriptors.get(ruleNodeType);
    this.displayTooltip(event,
      '<div class="tb-rule-node-tooltip tb-lib-tooltip">' +
      '<div id="tb-node-content" layout="column">' +
      '<div class="tb-node-title">' + this.translate.instant(type.name) + '</div>' +
      '<div class="tb-node-details">' + this.translate.instant(type.details) + '</div>' +
      '</div>' +
      '</div>'
    );
  }

  displayLibNodeDescriptionTooltip(event: MouseEvent, node: FcRuleNodeType) {
    this.displayTooltip(event,
      '<div class="tb-rule-node-tooltip tb-lib-tooltip">' +
      '<div id="tb-node-content" layout="column">' +
      '<div class="tb-node-title">' + node.component.name + '</div>' +
      '<div class="tb-node-description">' + node.component.configurationDescriptor.nodeDefinition.description + '</div>' +
      '<div class="tb-node-details">' + node.component.configurationDescriptor.nodeDefinition.details + '</div>' +
      '</div>' +
      '</div>'
    );
  }

  displayNodeDescriptionTooltip(event: MouseEvent, node: FcRuleNode) {
    if (!this.errorTooltips[node.id]) {
      let name: string;
      let desc: string;
      let details: string;
      if (node.component.type === RuleNodeType.INPUT) {
        name = this.translate.instant(ruleNodeTypeDescriptors.get(RuleNodeType.INPUT).name);
        desc = this.translate.instant(ruleNodeTypeDescriptors.get(RuleNodeType.INPUT).details);
      } else {
        name = node.name;
        desc = this.translate.instant(ruleNodeTypeDescriptors.get(node.component.type).name) + ' - ' + node.component.name;
        if (node.additionalInfo) {
          details = node.additionalInfo.description;
        }
      }
      let tooltipContent = '<div class="tb-rule-node-tooltip">' +
        '<div id="tb-node-content" layout="column">' +
        '<div class="tb-node-title">' + name + '</div>' +
        '<div class="tb-node-description">' + desc + '</div>';
      if (details) {
        tooltipContent += '<div class="tb-node-details">' + details + '</div>';
      }
      tooltipContent += '</div>' +
        '</div>';
      this.displayTooltip(event, tooltipContent);
    }
  }

  destroyTooltips() {
    if (this.tooltipTimeout) {
      clearTimeout(this.tooltipTimeout);
      this.tooltipTimeout = null;
    }
    const instances = $.tooltipster.instances();
    instances.forEach((instance) => {
      if (!instance.isErrorTooltip) {
        instance.destroy();
      }
    });
  }

  updateRuleNodesHighlight() {
    for (const ruleNode of this.ruleChainModel.nodes) {
      ruleNode.highlighted = false;
    }
    if (this.ruleNodeSearch) {
      const search = this.ruleNodeSearch.toUpperCase();
      const res = this.ruleChainModel.nodes.filter(node => node.name.toUpperCase().includes(search));
      if (res) {
        for (const ruleNode of res) {
          ruleNode.highlighted = true;
        }
      }
    }
    this.ruleChainCanvas.modelService.detectChanges();
  }

  private displayTooltip(event: MouseEvent, content: string) {
    this.destroyTooltips();
    this.tooltipTimeout = setTimeout(() => {
      const element = $(event.target);
      element.tooltipster(
        {
          theme: 'tooltipster-shadow',
          delay: 100,
          trigger: 'custom',
          triggerOpen: {
            click: false,
            tap: false
          },
          triggerClose: {
            click: true,
            tap: true,
            scroll: true
          },
          side: 'right',
          trackOrigin: true
        }
      );
      const contentElement = $(content);
      const tooltip = element.tooltipster('instance');
      tooltip.content(contentElement);
      tooltip.open();
    }, 500);
  }


}
