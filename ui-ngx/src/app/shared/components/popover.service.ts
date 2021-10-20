///
/// Copyright © 2016-2021 The Thingsboard Authors
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
  ComponentFactory,
  ComponentFactoryResolver, ElementRef, Inject,
  Injectable, Injector,
  Renderer2,
  Type,
  ViewContainerRef
} from '@angular/core';
import { PopoverPlacement, PopoverWithTrigger } from '@shared/components/popover.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ComponentType } from '@angular/cdk/portal';
import { HELP_MARKDOWN_COMPONENT_TOKEN } from '@shared/components/tokens';

@Injectable()
export class TbPopoverService {

  private popoverWithTriggers: PopoverWithTrigger[] = [];

  componentFactory: ComponentFactory<TbPopoverComponent> = this.resolver.resolveComponentFactory(TbPopoverComponent);

  constructor(private resolver: ComponentFactoryResolver,
              @Inject(HELP_MARKDOWN_COMPONENT_TOKEN) private helpMarkdownComponent: ComponentType<any>) {
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

  displayPopover<T>(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef,
                    componentType: Type<T>, preferredPlacement: PopoverPlacement = 'top', hideOnClickOutside = true,
                    injector?: Injector, context?: any, overlayStyle: any = {}, popoverStyle: any = {}, style?: any): TbPopoverComponent {
    const componentRef = hostView.createComponent(this.componentFactory);
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
    component.setOverlayOrigin({ elementRef: originElementRef });
    component.tbPlacement = preferredPlacement;
    component.tbComponentFactory = this.resolver.resolveComponentFactory(componentType);
    component.tbComponentInjector = injector;
    component.tbComponentContext = context;
    component.tbOverlayStyle = overlayStyle;
    component.tbPopoverInnerStyle = popoverStyle;
    component.tbComponentStyle = style;
    component.tbHideOnClickOutside = hideOnClickOutside;
    component.tbVisibleChange.subscribe((visible: boolean) => {
      if (!visible) {
        component.tbAnimationDone.subscribe(() => {
          componentRef.destroy();
        });
      }
    });
    component.tbDestroy.subscribe(() => {
      this.removePopoverByComponent(component);
    });
    component.show();
    return component;
  }

  toggleHelpPopover(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef, helpId = '',
                    helpContent = '',
                    visibleFn: (visible: boolean) => void = () => {},
                    readyFn: (ready: boolean) => void = () => {},
                    preferredPlacement: PopoverPlacement = 'bottom',
                    overlayStyle: any = {}, helpStyle: any = {}) {
    if (this.hasPopover(trigger)) {
      this.hidePopover(trigger);
    } else {
      readyFn(false);
      const injector = Injector.create({
        parent: hostView.injector, providers: []
      });
      const componentRef = hostView.createComponent(this.componentFactory);
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
      component.setOverlayOrigin({ elementRef: originElementRef });
      component.tbPlacement = preferredPlacement;
      component.tbComponentFactory = this.resolver.resolveComponentFactory(this.helpMarkdownComponent);
      component.tbComponentInjector = injector;
      component.tbComponentContext = {
        helpId,
        helpContent,
        style: helpStyle,
        visible: true
      };
      component.tbHideOnClickOutside = true;
      component.tbVisibleChange.subscribe((visible: boolean) => {
        if (!visible) {
          visibleFn(false);
          component.tbAnimationDone.subscribe(() => {
            componentRef.destroy();
          });
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
