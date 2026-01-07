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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ComponentDescriptor, ComponentType } from '@shared/models/component-descriptor.models';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { RuleNodeType } from '@shared/models/rule-node.models';
import { RuleChainType } from '@shared/models/rule-chain.models';

@Injectable({
  providedIn: 'root'
})
export class ComponentDescriptorService {

  private componentsByTypeByRuleChainType: Map<RuleChainType, Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>> =
    new Map<RuleChainType, Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>>();
  private componentsByClazz: Map<string, ComponentDescriptor> = new Map<string, ComponentDescriptor>();

  constructor(
    private http: HttpClient
  ) {
  }

  public getComponentDescriptorsByType(componentType: ComponentType, ruleChainType: RuleChainType, config?: RequestConfig): Observable<Array<ComponentDescriptor>> {
    if (!this.componentsByTypeByRuleChainType.get(ruleChainType)) {
      this.componentsByTypeByRuleChainType.set(ruleChainType, new Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>());
    }
    const existing = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentType);
    if (existing) {
      return of(existing);
    } else {
      return this.http.get<Array<ComponentDescriptor>>(`/api/components/${componentType}&ruleChainType=${ruleChainType}`, defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptors) => {
          this.componentsByTypeByRuleChainType.get(ruleChainType).set(componentType, componentDescriptors);
          componentDescriptors.forEach((componentDescriptor) => {
            this.componentsByClazz.set(componentDescriptor.clazz, componentDescriptor);
          });
          return componentDescriptors;
        })
      );
    }
  }

  public getComponentDescriptorsByTypes(componentTypes: Array<ComponentType>, ruleChainType: RuleChainType, config?: RequestConfig): Observable<Array<ComponentDescriptor>> {
    if (!this.componentsByTypeByRuleChainType.get(ruleChainType)) {
      this.componentsByTypeByRuleChainType.set(ruleChainType, new Map<ComponentType | RuleNodeType, Array<ComponentDescriptor>>());
    }
    let result: ComponentDescriptor[] = [];
    for (let i = componentTypes.length - 1; i >= 0; i--) {
      const componentType = componentTypes[i];
      const componentDescriptors = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentType);
      if (componentDescriptors) {
        result = result.concat(componentDescriptors);
        componentTypes.splice(i, 1);
      }
    }
    if (!componentTypes.length) {
      return of(result);
    } else {
      return this.http.get<Array<ComponentDescriptor>>(`/api/components?componentTypes=${componentTypes.join(',')}&ruleChainType=${ruleChainType}`,
        defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptors) => {
          componentDescriptors.forEach((componentDescriptor) => {
            let componentsList = this.componentsByTypeByRuleChainType.get(ruleChainType).get(componentDescriptor.type);
            if (!componentsList) {
              componentsList = new Array<ComponentDescriptor>();
              this.componentsByTypeByRuleChainType.get(ruleChainType).set(componentDescriptor.type, componentsList);
            }
            componentsList.push(componentDescriptor);
            this.componentsByClazz.set(componentDescriptor.clazz, componentDescriptor);
          });
          result = result.concat(componentDescriptors);
          return result;
        })
      );
    }
  }

  public getComponentDescriptorByClazz(componentDescriptorClazz: string, config?: RequestConfig): Observable<ComponentDescriptor> {
    const existing = this.componentsByClazz.get(componentDescriptorClazz);
    if (existing) {
      return of(existing);
    } else {
      return this.http.get<ComponentDescriptor>(`/api/component/${componentDescriptorClazz}`, defaultHttpOptionsFromConfig(config)).pipe(
        map((componentDescriptor) => {
          this.componentsByClazz.set(componentDescriptorClazz, componentDescriptor);
          return componentDescriptor;
        })
      );
    }
  }
}
