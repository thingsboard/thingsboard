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

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { forkJoin, Observable, of } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  ResolvedRuleChainMetaData,
  RuleChain, RuleChainConnectionInfo,
  RuleChainMetaData,
  ruleChainNodeComponent,
  ruleNodeTypeComponentTypes, unknownNodeComponent
} from '@shared/models/rule-chain.models';
import { ComponentDescriptorService } from './component-descriptor.service';
import { RuleNodeComponentDescriptor } from '@app/shared/models/rule-node.models';
import { ResourcesService } from '../services/resources.service';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { deepClone } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class RuleChainService {

  private ruleNodeComponents: Array<RuleNodeComponentDescriptor>;

  constructor(
    private http: HttpClient,
    private componentDescriptorService: ComponentDescriptorService,
    private resourcesService: ResourcesService,
    private translate: TranslateService
  ) { }

  public getRuleChains(pageLink: PageLink, config?: RequestConfig): Observable<PageData<RuleChain>> {
    return this.http.get<PageData<RuleChain>>(`/api/ruleChains${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getRuleChain(ruleChainId: string, config?: RequestConfig): Observable<RuleChain> {
    return this.http.get<RuleChain>(`/api/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
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
      mergeMap((ruleChainMetaData) => {
        return this.resolveTargetRuleChains(ruleChainMetaData.ruleChainConnections).pipe(
          map((targetRuleChainsMap) => {
            const resolvedRuleChainMetadata: ResolvedRuleChainMetaData = {...ruleChainMetaData, targetRuleChainsMap};
            return resolvedRuleChainMetadata;
          })
        );
      })
    );
  }

  public saveRuleChainMetadata(ruleChainMetaData: RuleChainMetaData, config?: RequestConfig): Observable<RuleChainMetaData> {
    return this.http.post<RuleChainMetaData>('/api/ruleChain/metadata', ruleChainMetaData, defaultHttpOptionsFromConfig(config));
  }

  public saveAndGetResolvedRuleChainMetadata(ruleChainMetaData: RuleChainMetaData,
                                             config?: RequestConfig): Observable<ResolvedRuleChainMetaData> {
    return this.saveRuleChainMetadata(ruleChainMetaData, config).pipe(
      mergeMap((savedRuleChainMetaData) => {
        return this.resolveTargetRuleChains(savedRuleChainMetaData.ruleChainConnections).pipe(
          map((targetRuleChainsMap) => {
            const resolvedRuleChainMetadata: ResolvedRuleChainMetaData = {...savedRuleChainMetaData, targetRuleChainsMap};
            return resolvedRuleChainMetadata;
          })
        );
      })
    );
  }

  public getRuleNodeComponents(config?: RequestConfig): Observable<Array<RuleNodeComponentDescriptor>> {
     if (this.ruleNodeComponents) {
       return of(this.ruleNodeComponents);
     } else {
      return this.loadRuleNodeComponents(config).pipe(
        mergeMap((components) => {
          return this.resolveRuleNodeComponentsUiResources(components).pipe(
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

  private loadRuleNodeComponents(config?: RequestConfig): Observable<Array<RuleNodeComponentDescriptor>> {
    return this.componentDescriptorService.getComponentDescriptorsByTypes(ruleNodeTypeComponentTypes, config).pipe(
      map((components) => {
        const ruleNodeComponents: RuleNodeComponentDescriptor[] = [];
        components.forEach((component) => {
          ruleNodeComponents.push(component as RuleNodeComponentDescriptor);
        });
        return ruleNodeComponents;
      })
    );
  }

  private resolveRuleNodeComponentsUiResources(components: Array<RuleNodeComponentDescriptor>):
    Observable<Array<RuleNodeComponentDescriptor>> {
    const tasks: Observable<RuleNodeComponentDescriptor>[] = [];
    components.forEach((component) => {
      tasks.push(this.resolveRuleNodeComponentUiResources(component));
    });
    return forkJoin(tasks).pipe(
      catchError((err) => {
        return of(components);
      })
    );
  }

  private resolveRuleNodeComponentUiResources(component: RuleNodeComponentDescriptor): Observable<RuleNodeComponentDescriptor> {
    const uiResources = component.configurationDescriptor.nodeDefinition.uiResources;
    if (uiResources && uiResources.length) {
      const tasks: Observable<any>[] = [];
      uiResources.forEach((uiResource) => {
        tasks.push(this.resourcesService.loadResource(uiResource));
      });
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

}
