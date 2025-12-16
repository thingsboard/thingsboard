///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Injectable, Type } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  RuleChain,
  RuleChainMetaData,
  RuleChainType,
  ruleNodeTypeComponentTypes,
  unknownNodeComponent
} from '@shared/models/rule-chain.models';
import { ComponentDescriptorService } from './component-descriptor.service';
import {
  IRuleNodeConfigurationComponent,
  LinkLabel,
  RuleNodeComponentDescriptor,
  RuleNodeConfiguration,
  RuleNodeConfigurationComponent,
  ScriptLanguage,
  TestScriptInputParams,
  TestScriptResult
} from '@app/shared/models/rule-node.models';
import { componentTypeBySelector, ResourcesService } from '../services/resources.service';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, snakeCase } from '@core/utils';
import { DebugRuleNodeEventBody } from '@app/shared/models/event.models';
import { Edge } from '@shared/models/edge.models';
import { IModulesMap } from '@modules/common/modules-map.models';

@Injectable({
  providedIn: 'root'
})
export class RuleChainService {

  private ruleNodeComponentsMap: Map<RuleChainType, Array<RuleNodeComponentDescriptor>> =
    new Map<RuleChainType, Array<RuleNodeComponentDescriptor>>();
  private ruleNodeConfigComponents: {[directive: string]: Type<IRuleNodeConfigurationComponent>} = {};

  constructor(
    private http: HttpClient,
    private componentDescriptorService: ComponentDescriptorService,
    private resourcesService: ResourcesService,
    private translate: TranslateService
  ) { }

  public getRuleChains(pageLink: PageLink, type: RuleChainType = RuleChainType.CORE,
                       config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/ruleChains${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRuleChainsByIds(ruleChainIds: Array<string>, config?: RequestConfig): Observable<Array<RuleChain>> {
    return this.http.get<Array<RuleChain>>(`/api/ruleChains?&ruleChainIds=${ruleChainIds.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.get<RuleChain>(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public getRuleChainOutputLabels(ruleChainId: string, config?: RequestConfig): Observable<Array<string>> {
    return this.http.get<Array<string>>(`/api/ruleChain/${ruleChainId}/output/labels`, defaultHttpOptionsFromConfig(config));
  }

  public createDefaultRuleChain(ruleChainName: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>('/api/ruleChain/device/default', {
      name: ruleChainName
    }, defaultHttpOptionsFromConfig(config));
  }

  public saveRuleChain(ruleChain: RuleChain, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>('/api/ruleChain', ruleChain, defaultHttpOptionsFromConfig(config));
  }

  public deleteRuleChain(ruleChainId: string, config?: RequestConfig) {
    return this.http.delete(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setRootRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/root`, null, defaultHttpOptionsFromConfig(config));
  }

  public getRuleChainMetadata(ruleChainId: string, config?: RequestConfig): Observable<RuleChainMetaData> {
    return this.http.get<RuleChainMetaData>(`/api/ruleChain/${ruleChainId}/metadata`, defaultHttpOptionsFromConfig(config));
  }

  public saveRuleChainMetadata(ruleChainMetaData: RuleChainMetaData, config?: RequestConfig): Observable<RuleChainMetaData> {
    return this.http.post<RuleChainMetaData>('/api/ruleChain/metadata', ruleChainMetaData, defaultHttpOptionsFromConfig(config));
  }

  public getRuleNodeComponents(modulesMap: IModulesMap, ruleChainType: RuleChainType, config?: RequestConfig):
    Observable<Array<RuleNodeComponentDescriptor>> {
     if (this.ruleNodeComponentsMap.get(ruleChainType)) {
       return of(this.ruleNodeComponentsMap.get(ruleChainType));
     } else {
      return this.loadRuleNodeComponents(ruleChainType, config).pipe(
        mergeMap((components) => {
          return this.resolveRuleNodeComponentsUiResources(components, modulesMap).pipe(
            map((ruleNodeComponents) => {
              this.ruleNodeComponentsMap.set(ruleChainType, ruleNodeComponents);
              this.ruleNodeComponentsMap.get(ruleChainType).sort(
                (comp1, comp2) => {
                  let result = comp1.type.toString().localeCompare(comp2.type.toString());
                  if (result === 0) {
                    result = comp1.name.localeCompare(comp2.name);
                  }
                  return result;
                }
              );
              return this.ruleNodeComponentsMap.get(ruleChainType);
            })
          );
        })
      );
    }
  }

  public getRuleNodeConfigComponent(directive: string): Type<IRuleNodeConfigurationComponent> {
    return this.ruleNodeConfigComponents[directive];
  }

  public getRuleNodeComponentByClazz(ruleChainType: RuleChainType = RuleChainType.CORE, clazz: string): RuleNodeComponentDescriptor {
    const found = this.ruleNodeComponentsMap.get(ruleChainType).filter((component) => component.clazz === clazz);
    if (found && found.length) {
      return found[0];
    } else {
      const unknownComponent = deepClone(unknownNodeComponent);
      unknownComponent.clazz = clazz;
      unknownComponent.configurationDescriptor.nodeDefinition.details = 'Unknown Rule Node class: ' + clazz;
      return unknownComponent;
    }
  }

  public getRuleNodeSupportedLinks(component: RuleNodeComponentDescriptor): {[label: string]: LinkLabel} {
    const relationTypes = component.configurationDescriptor.nodeDefinition.relationTypes;
    const linkLabels: {[label: string]: LinkLabel} = {};
    relationTypes.forEach((label) => {
      linkLabels[label] = {
        name: label,
        value: label
      };
    });
    return linkLabels;
  }

  public ruleNodeAllowCustomLinks(component: RuleNodeComponentDescriptor): boolean {
    return component.configurationDescriptor.nodeDefinition.customRelations;
  }

  public ruleNodeSourceRuleChainId(component: RuleNodeComponentDescriptor, config: RuleNodeConfiguration): string {
    if (component.configurationDescriptor.nodeDefinition.ruleChainNode) {
      return config?.ruleChainId;
    } else {
      return null;
    }
  }

  public getLatestRuleNodeDebugInput(ruleNodeId: string, config?: RequestConfig): Observable<DebugRuleNodeEventBody> {
    return this.http.get<DebugRuleNodeEventBody>(`/api/ruleNode/${ruleNodeId}/debugIn`, defaultHttpOptionsFromConfig(config));
  }

  public testScript(inputParams: TestScriptInputParams, scriptLang?: ScriptLanguage, config?: RequestConfig): Observable<TestScriptResult> {
    let url = '/api/ruleChain/testScript';
    if (scriptLang) {
      url += `?scriptLang=${scriptLang}`;
    }
    return this.http.post<TestScriptResult>(url, inputParams, defaultHttpOptionsFromConfig(config));
  }

  public registerSystemRuleNodeConfigModule(module: any) {
    Object.assign(this.ruleNodeConfigComponents, this.resourcesService.extractComponentsFromModule<IRuleNodeConfigurationComponent>(module, RuleNodeConfigurationComponent, true));
  }

  private loadRuleNodeComponents(ruleChainType: RuleChainType, config?: RequestConfig): Observable<Array<RuleNodeComponentDescriptor>> {
    return this.componentDescriptorService.getComponentDescriptorsByTypes(ruleNodeTypeComponentTypes, ruleChainType, config).pipe(
      map((components) => {
        const ruleNodeComponents: RuleNodeComponentDescriptor[] = [];
        components.forEach((component) => {
          ruleNodeComponents.push(component as RuleNodeComponentDescriptor);
        });
        return ruleNodeComponents;
      })
    );
  }

  private resolveRuleNodeComponentsUiResources(components: Array<RuleNodeComponentDescriptor>,
                                               modulesMap: IModulesMap):
    Observable<Array<RuleNodeComponentDescriptor>> {
    const tasks: Observable<RuleNodeComponentDescriptor>[] = [];
    components.forEach((component) => {
      tasks.push(this.resolveRuleNodeComponentUiResources(component, modulesMap));
    });
    return forkJoin(tasks).pipe(
      catchError(() => {
        return of(components);
      })
    );
  }

  private resolveRuleNodeComponentUiResources(component: RuleNodeComponentDescriptor,
                                              modulesMap: IModulesMap):
    Observable<RuleNodeComponentDescriptor> {
    const nodeDefinition = component.configurationDescriptor.nodeDefinition;
    const uiResources = nodeDefinition.uiResources;
    if (!this.ruleNodeConfigComponents[nodeDefinition.configDirective] && uiResources && uiResources.length) {
      const commonResources = uiResources.filter((resource) => !resource.endsWith('.js'));
      const moduleResource = uiResources.find((resource) => resource.endsWith('.js'));
      const tasks: Observable<any>[] = [];
      if (commonResources && commonResources.length) {
        commonResources.forEach((resource) => {
          tasks.push(this.resourcesService.loadResource(resource));
        });
      }
      if (moduleResource) {
        tasks.push(this.resourcesService.loadModulesWithComponents(moduleResource, modulesMap).pipe(
          map((res) => {
            if (nodeDefinition.configDirective && nodeDefinition.configDirective.length) {
              const selector = snakeCase(nodeDefinition.configDirective, '-');
              const componentType = componentTypeBySelector(res, selector);
              if (componentType) {
                this.ruleNodeConfigComponents[nodeDefinition.configDirective] = componentType;
              } else {
                component.configurationDescriptor.nodeDefinition.uiResourceLoadError =
                  this.translate.instant('rulenode.directive-is-not-loaded',
                    {directiveName: nodeDefinition.configDirective});
              }
            }
            return of(component);
          })
        ));
      }
      return forkJoin(tasks).pipe(
        map(() => {
          return component;
        }),
        catchError(() => {
          component.configurationDescriptor.nodeDefinition.uiResourceLoadError = this.translate.instant('rulenode.ui-resources-load-error');
          return of(component);
        })
      );
    } else {
      return of(component);
    }
  }

  public getEdgeRuleChains(edgeId: string, pageLink: PageLink, config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/edge/${edgeId}/ruleChains${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignRuleChainToEdge(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignRuleChainFromEdge(edgeId: string, ruleChainId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setEdgeTemplateRootRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/edgeTemplateRoot`, defaultHttpOptionsFromConfig(config));
  }

  public setAutoAssignToEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/autoAssignToEdge`, defaultHttpOptionsFromConfig(config));
  }

  public unsetAutoAssignToEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.delete<RuleChain>(`/api/ruleChain/${ruleChainId}/autoAssignToEdge`, defaultHttpOptionsFromConfig(config));
  }

  public getAutoAssignToEdgeRuleChains(config?: RequestConfig): Observable<Array<RuleChain>> {
    return this.http.get<Array<RuleChain>>(`/api/ruleChain/autoAssignToEdgeRuleChains`, defaultHttpOptionsFromConfig(config));
  }

  public setEdgeRootRuleChain(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<Edge> {
    return this.http.post<Edge>(`/api/edge/${edgeId}/${ruleChainId}/root`, defaultHttpOptionsFromConfig(config));
  }

}
