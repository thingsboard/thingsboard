///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { ComponentFactory, Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { forkJoin, Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  ResolvedRuleChainMetaData,
  RuleChain,
  RuleChainConnectionInfo,
  RuleChainMetaData,
  ruleChainNodeComponent, RuleChainType,
  ruleNodeTypeComponentTypes,
  unknownNodeComponent
} from '@shared/models/rule-chain.models';
import { ComponentDescriptorService } from './component-descriptor.service';
import {
  IRuleNodeConfigurationComponent,
  LinkLabel,
  RuleNodeComponentDescriptor,
  TestScriptInputParams,
  TestScriptResult
} from '@app/shared/models/rule-node.models';
import { ResourcesService } from '../services/resources.service';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone, snakeCase } from '@core/utils';
import { DebugRuleNodeEventBody } from '@app/shared/models/event.models';

@Injectable({
  providedIn: 'root'
})
export class RuleChainService {

  private ruleNodeComponents: Array<RuleNodeComponentDescriptor>;
  private ruleNodeConfigFactories: {[directive: string]: ComponentFactory<IRuleNodeConfigurationComponent>} = {};

  constructor(
    private http: HttpClient,
    private componentDescriptorService: ComponentDescriptorService,
    private resourcesService: ResourcesService,
    private translate: TranslateService
  ) { }

  public getRuleChains(pageLink: PageLink, type: string, config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/ruleChains${pageLink.toQuery()}&type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.get<RuleChain>(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
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

  public getResolvedRuleChainMetadata(ruleChainId: string, config?: RequestConfig): Observable<ResolvedRuleChainMetaData> {
    return this.getRuleChainMetadata(ruleChainId, config).pipe(
      mergeMap((ruleChainMetaData) => this.resolveRuleChainMetadata(ruleChainMetaData))
    );
  }

  public saveRuleChainMetadata(ruleChainMetaData: RuleChainMetaData, config?: RequestConfig): Observable<RuleChainMetaData> {
    return this.http.post<RuleChainMetaData>('/api/ruleChain/metadata', ruleChainMetaData, defaultHttpOptionsFromConfig(config));
  }

  public saveAndGetResolvedRuleChainMetadata(ruleChainMetaData: RuleChainMetaData,
                                             config?: RequestConfig): Observable<ResolvedRuleChainMetaData> {
    return this.saveRuleChainMetadata(ruleChainMetaData, config).pipe(
      mergeMap((savedRuleChainMetaData) => this.resolveRuleChainMetadata(savedRuleChainMetaData))
    );
  }

  public resolveRuleChainMetadata(ruleChainMetaData: RuleChainMetaData): Observable<ResolvedRuleChainMetaData> {
    return this.resolveTargetRuleChains(ruleChainMetaData.ruleChainConnections).pipe(
      map((targetRuleChainsMap) => {
        const resolvedRuleChainMetadata: ResolvedRuleChainMetaData = {...ruleChainMetaData, targetRuleChainsMap};
        return resolvedRuleChainMetadata;
      })
    );
  }

  public getRuleNodeComponents(ruleNodeConfigResourcesModulesMap: {[key: string]: any}, ruleChainType: RuleChainType, config?: RequestConfig):
    Observable<Array<RuleNodeComponentDescriptor>> {
     if (this.ruleNodeComponents) {
       return of(this.ruleNodeComponents);
     } else {
      return this.loadRuleNodeComponents(ruleChainType, config).pipe(
        mergeMap((components) => {
          return this.resolveRuleNodeComponentsUiResources(components, ruleNodeConfigResourcesModulesMap).pipe(
            map((ruleNodeComponents) => {
              this.ruleNodeComponents = ruleNodeComponents;
              this.ruleNodeComponents.push(ruleChainNodeComponent);
              this.ruleNodeComponents.sort(
                (comp1, comp2) => {
                  let result = comp1.type.toString().localeCompare(comp2.type.toString());
                  if (result === 0) {
                    result = comp1.name.localeCompare(comp2.name);
                  }
                  return result;
                }
              );
              return this.ruleNodeComponents;
            })
          );
        })
      );
    }
  }

  public getRuleNodeConfigFactory(directive: string): ComponentFactory<IRuleNodeConfigurationComponent> {
    return this.ruleNodeConfigFactories[directive];
  }

  public getRuleNodeComponentByClazz(clazz: string): RuleNodeComponentDescriptor {
    const found = this.ruleNodeComponents.filter((component) => component.clazz === clazz);
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

  public getLatestRuleNodeDebugInput(ruleNodeId: string, config?: RequestConfig): Observable<DebugRuleNodeEventBody> {
    return this.http.get<DebugRuleNodeEventBody>(`/api/ruleNode/${ruleNodeId}/debugIn`, defaultHttpOptionsFromConfig(config));
  }

  public testScript(inputParams: TestScriptInputParams, config?: RequestConfig): Observable<TestScriptResult> {
    return this.http.post<TestScriptResult>('/api/ruleChain/testScript', inputParams, defaultHttpOptionsFromConfig(config));
  }

  private resolveTargetRuleChains(ruleChainConnections: Array<RuleChainConnectionInfo>): Observable<{[ruleChainId: string]: RuleChain}> {
    if (ruleChainConnections && ruleChainConnections.length) {
      const tasks: Observable<RuleChain>[] = [];
      ruleChainConnections.forEach((connection) => {
        tasks.push(this.resolveRuleChain(connection.targetRuleChainId.id));
      });
      return forkJoin(tasks).pipe(
        map((ruleChains) => {
          const ruleChainsMap: {[ruleChainId: string]: RuleChain} = {};
          ruleChains.forEach((ruleChain) => {
            ruleChainsMap[ruleChain.id.id] = ruleChain;
          });
          return ruleChainsMap;
        })
      );
    } else {
      return of({} as {[ruleChainId: string]: RuleChain});
    }
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
                                               ruleNodeConfigResourcesModulesMap: {[key: string]: any}):
    Observable<Array<RuleNodeComponentDescriptor>> {
    const tasks: Observable<RuleNodeComponentDescriptor>[] = [];
    components.forEach((component) => {
      tasks.push(this.resolveRuleNodeComponentUiResources(component, ruleNodeConfigResourcesModulesMap));
    });
    return forkJoin(tasks).pipe(
      catchError((err) => {
        return of(components);
      })
    );
  }

  private resolveRuleNodeComponentUiResources(component: RuleNodeComponentDescriptor,
                                              ruleNodeConfigResourcesModulesMap: {[key: string]: any}):
    Observable<RuleNodeComponentDescriptor> {
    const nodeDefinition = component.configurationDescriptor.nodeDefinition;
    const uiResources = nodeDefinition.uiResources;
    if (uiResources && uiResources.length) {
      const commonResources = uiResources.filter((resource) => !resource.endsWith('.js'));
      const moduleResource = uiResources.find((resource) => resource.endsWith('.js'));
      const tasks: Observable<any>[] = [];
      if (commonResources && commonResources.length) {
        commonResources.forEach((resource) => {
          tasks.push(this.resourcesService.loadResource(resource));
        });
      }
      if (moduleResource) {
        tasks.push(this.resourcesService.loadFactories(moduleResource, ruleNodeConfigResourcesModulesMap).pipe(
          map((res) => {
            if (nodeDefinition.configDirective && nodeDefinition.configDirective.length) {
              const selector = snakeCase(nodeDefinition.configDirective, '-');
              const componentFactory = res.find((factory) =>
              factory.selector === selector);
              if (componentFactory) {
                this.ruleNodeConfigFactories[nodeDefinition.configDirective] = componentFactory;
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
        map((res) => {
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

  private resolveRuleChain(ruleChainId: string): Observable<RuleChain> {
    return this.getRuleChain(ruleChainId, {ignoreErrors: true}).pipe(
      map(ruleChain => ruleChain),
      catchError((err) => {
        const ruleChain = {
         id: {
            entityType: EntityType.RULE_CHAIN,
            id: ruleChainId
          }
        } as RuleChain;
        return of(ruleChain);
      })
    );
  }

  public getEdgeRuleChains(edgeId: string, pageLink: PageLink, config?:RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/edge/${edgeId}/ruleChains${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config) )
  }

  public assignRuleChainToEdge(edgeId: string, ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public unassignRuleChainFromEdge(edgeId: string, ruleChainId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edge/${edgeId}/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultRootEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultRootEdge`, defaultHttpOptionsFromConfig(config));
  }

  public addDefaultEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.post<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultEdge`, defaultHttpOptionsFromConfig(config));
  }

  public removeDefaultEdgeRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.delete<RuleChain>(`/api/ruleChain/${ruleChainId}/defaultEdge`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultEdgeRuleChains(config?: RequestConfig): Observable<Array<RuleChain>> {
    return this.http.get<Array<RuleChain>>(`/api/ruleChain/defaultEdgeRuleChains`, defaultHttpOptionsFromConfig(config));
  }

}
