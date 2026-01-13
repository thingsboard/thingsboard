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

import {
  ComponentRef,
  ElementRef,
  Inject,
  Injectable,
  Injector,
  Renderer2,
  Type,
  ViewContainerRef
} from '@angular/core';
import {
  defaultPopoverConfig,
  DisplayPopoverConfig,
  DisplayPopoverWithComponentRefConfig,
  PopoverPreferredPlacement,
  PopoverWithTrigger
} from '@shared/components/popover.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ComponentType } from '@angular/cdk/portal';
import { HELP_MARKDOWN_COMPONENT_TOKEN } from '@shared/components/tokens';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';
import { Observable } from 'rxjs';
import { mergeDeep } from '@core/utils';

@Injectable()
export class TbPopoverService {

  private popoverWithTriggers: PopoverWithTrigger[] = [];

  constructor(@Inject(HELP_MARKDOWN_COMPONENT_TOKEN) private helpMarkdownComponent: ComponentType<any>) {
  }

  hasPopover(trigger: Element): boolean {
    const res = this.findPopoverByTrigger(trigger);
    return res !== null;
  }

  hidePopover(trigger: Element): boolean {
    const component: TbPopoverComponent = this.findPopoverByTrigger(trigger);
    if (component && component.tbVisible) {
      component.hide();
      return true;
    } else {
      return false;
    }
  }

  createPopoverRef(hostView: ViewContainerRef): ComponentRef<TbPopoverComponent> {
    return hostView.createComponent(TbPopoverComponent);
  }

  displayPopover<T>(config: DisplayPopoverConfig<T>): TbPopoverComponent<T>;
  displayPopover<T>(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef,
                    componentType: Type<T>, preferredPlacement: PopoverPreferredPlacement,
                    hideOnClickOutside: boolean, injector?: Injector, context?: any, overlayStyle?: any,
                    popoverStyle?: any, style?: any,
                    showCloseButton?: boolean, visibleFn?: (visible: boolean) => void,
                    popoverContentStyle?: any): TbPopoverComponent<T>;
  displayPopover<T>(config: Element | DisplayPopoverConfig<T>, renderer?: Renderer2, hostView?: ViewContainerRef,
                    componentType?: Type<T>, preferredPlacement?: PopoverPreferredPlacement,
                    hideOnClickOutside?: boolean, injector?: Injector, context?: any, overlayStyle?: any,
                    popoverStyle?: any, style?: any,
                    showCloseButton?: boolean, visibleFn?: (visible: boolean) => void,
                    popoverContentStyle?: any): TbPopoverComponent<T> {
    if (!(config instanceof Element) && 'trigger' in config && 'renderer' in config && 'componentType' in config) {
      const componentRef = this.createPopoverRef(config.hostView);
      return this.displayPopoverWithComponentRef<T>({ ...config, componentRef })
    } else if (config instanceof Element) {
      const componentRef = this.createPopoverRef(hostView);
      return this.displayPopoverWithComponentRef<T>(componentRef, config, renderer, componentType, preferredPlacement, hideOnClickOutside,
        injector, context, overlayStyle, popoverStyle, style, showCloseButton, visibleFn, popoverContentStyle);
    } else {
      throw new Error("Invalid configuration provided for displayPopover");
    }
  }

  displayPopoverWithComponentRef<T>(config: DisplayPopoverWithComponentRefConfig<T>): TbPopoverComponent<T>;
  displayPopoverWithComponentRef<T>(componentRef: ComponentRef<TbPopoverComponent>, trigger: Element, renderer: Renderer2,
                                    componentType: Type<T>, preferredPlacement: PopoverPreferredPlacement,
                                    hideOnClickOutside: boolean, injector?: Injector, context?: any, overlayStyle?: any,
                                    popoverStyle?: any, style?: any, showCloseButton?: boolean,
                                    visibleFn?: (visible: boolean) => void, popoverContentStyle?: any): TbPopoverComponent<T>;
  displayPopoverWithComponentRef<T>(config: ComponentRef<TbPopoverComponent> | DisplayPopoverWithComponentRefConfig<T>,
                                    trigger?: Element, renderer?: Renderer2, componentType?: Type<T>,
                                    preferredPlacement?: PopoverPreferredPlacement, hideOnClickOutside?: boolean,
                                    injector?: Injector, context?: any, overlayStyle?: any,
                                    popoverStyle?: any, style?: any, showCloseButton?: boolean,
                                    visibleFn?: (visible: boolean) => void,
                                    popoverContentStyle: any = {}): TbPopoverComponent<T> {
    let popoverConfig: DisplayPopoverWithComponentRefConfig<T>;
    if (!(config instanceof ComponentRef) && 'trigger' in config && 'renderer' in config && 'componentType' in config) {
      popoverConfig = config;
    } else if(config instanceof ComponentRef) {
      popoverConfig = {
        componentRef: config,
        trigger,
        renderer,
        componentType,
        preferredPlacement,
        hideOnClickOutside,
        injector,
        context,
        overlayStyle,
        popoverStyle,
        style,
        showCloseButton,
        visibleFn,
        popoverContentStyle
      }
    } else {
      throw new Error("Invalid configuration provided for displayPopoverWithComponentRef");
    }
    popoverConfig = mergeDeep({} as any, defaultPopoverConfig, popoverConfig);
    return this._displayPopoverWithComponentRef(popoverConfig);
  }


  private _displayPopoverWithComponentRef<T>(conf: DisplayPopoverWithComponentRefConfig<T>): TbPopoverComponent<T> {
    const component = conf.componentRef.instance;
    this.popoverWithTriggers.push({
      trigger: conf.trigger,
      popoverComponent: component
    });
    conf.renderer.removeChild(
      conf.renderer.parentNode(conf.trigger),
      conf.componentRef.location.nativeElement
    );
    const originElementRef = new ElementRef(conf.trigger);
    component.setOverlayOrigin(new CdkOverlayOrigin(originElementRef));
    component.tbPlacement = conf.preferredPlacement;
    component.tbComponent = conf.componentType;
    component.tbComponentInjector = conf.injector;
    component.tbComponentContext = conf.context;
    component.tbOverlayStyle = conf.overlayStyle;
    component.tbModal = conf.isModal;
    component.tbPopoverInnerStyle = conf.popoverStyle;
    component.tbPopoverInnerContentStyle = conf.popoverContentStyle;
    component.tbComponentStyle = conf.style;
    component.tbHideOnClickOutside = conf.hideOnClickOutside;
    component.tbShowCloseButton = conf.showCloseButton;
    component.tbVisibleChange.subscribe((visible: boolean) => {
      if (!visible) {
        conf.componentRef.destroy();
      }
    });
    component.tbDestroy.subscribe(() => {
      this.removePopoverByComponent(component);
    });
    component.tbHideStart.subscribe(() => {
      conf.visibleFn(false);
    });
    component.show();
    conf.visibleFn(true);
    return component;
  }

  toggleHelpPopover(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef, helpId = '',
                    helpContent = '',
                    helpContentBase64 = '',
                    asyncHelpContent: Observable<string> = null,
                    visibleFn: (visible: boolean) => void = () => {},
                    readyFn: (ready: boolean) => void = () => {},
                    preferredPlacement: PopoverPreferredPlacement = 'bottom',
                    overlayStyle: any = {}, helpStyle: any = {}) {
    if (this.hasPopover(trigger)) {
      this.hidePopover(trigger);
    } else {
      readyFn(false);
      const injector = Injector.create({
        parent: hostView.injector, providers: []
      });
      const componentRef = hostView.createComponent(TbPopoverComponent);
      const component = componentRef.instance;
      this.popoverWithTriggers.push({
        trigger,
        popoverComponent: component
      });
      renderer.removeChild(
        renderer.parentNode(trigger),
        componentRef.location.nativeElement
      );
      const originElementRef = new ElementRef(trigger);
      component.tbAnimationState = 'void';
      component.tbOverlayStyle = {...overlayStyle, opacity: '0' };
      component.setOverlayOrigin(new CdkOverlayOrigin(originElementRef));
      component.tbPlacement = preferredPlacement;
      component.tbComponent = this.helpMarkdownComponent;
      component.tbComponentInjector = injector;
      component.tbComponentContext = {
        helpId,
        helpContent,
        helpContentBase64,
        asyncHelpContent,
        style: helpStyle,
        visible: true
      };
      component.tbHideOnClickOutside = true;
      component.tbVisibleChange.subscribe((visible: boolean) => {
        if (!visible) {
          visibleFn(false);
          componentRef.destroy();
        }
      });
      component.tbDestroy.subscribe(() => {
        this.removePopoverByComponent(component);
      });
      const showHelpMarkdownComponent = () => {
        component.tbOverlayStyle = {...component.tbOverlayStyle, opacity: '1' };
        component.tbAnimationState = 'active';
        component.updatePosition();
        readyFn(true);
        setTimeout(() => {
          component.updatePosition();
        });
      };
      const setupHelpMarkdownComponent = (helpMarkdownComponent: any) => {
        if (helpMarkdownComponent.isMarkdownReady) {
          showHelpMarkdownComponent();
        } else {
          helpMarkdownComponent.markdownReady.subscribe(() => {
            showHelpMarkdownComponent();
          });
        }
      };
      if (component.tbComponentRef) {
        setupHelpMarkdownComponent(component.tbComponentRef.instance);
      } else {
        component.tbComponentChange.subscribe((helpMarkdownComponentRef) => {
          setupHelpMarkdownComponent(helpMarkdownComponentRef.instance);
        });
      }
      component.show();
      visibleFn(true);
    }
  }

  private findPopoverByTrigger(trigger: Element): TbPopoverComponent | null {
    const res = this.popoverWithTriggers.find(val => this.elementsAreEqualOrDescendant(trigger, val.trigger));
    if (res) {
      return res.popoverComponent;
    } else {
      return null;
    }
  }

  private removePopoverByComponent(component: TbPopoverComponent): void {
    const index = this.popoverWithTriggers.findIndex(val => val.popoverComponent === component);
    if (index > -1) {
      this.popoverWithTriggers.splice(index, 1);
    }
  }

  private elementsAreEqualOrDescendant(element1: Element, element2: Element): boolean {
    return element1 === element2 || element1.contains(element2) || element2.contains(element1);
  }
}
