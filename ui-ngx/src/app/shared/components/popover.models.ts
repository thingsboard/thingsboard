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

import { animate, AnimationTriggerMetadata, style, transition, trigger } from '@angular/animations';
import { ConnectedOverlayPositionChange } from '@angular/cdk/overlay';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { POSITION_MAP } from '@shared/models/overlay.models';
import { ComponentRef, Injector, Renderer2, Type, ViewContainerRef } from '@angular/core';

export const popoverMotion: AnimationTriggerMetadata = trigger('popoverMotion', [
  transition('void => active', [
    style({ opacity: 0, transform: 'scale(0.8)' }),
    animate(
      '0.2s cubic-bezier(0.08, 0.82, 0.17, 1)',
      style({
        opacity: 1,
        transform: 'scale(1)'
      })
    )
  ]),
  transition('active => void', [
    style({ opacity: 1, transform: 'scale(1)' }),
    animate(
      '0.2s cubic-bezier(0.78, 0.14, 0.15, 0.86)',
      style({
        opacity: 0,
        transform: 'scale(0.8)'
      })
    )
  ])
]);

export const PopoverPlacements = ['top', 'topLeft', 'topRight', 'right', 'rightTop',
  'rightBottom', 'bottom', 'bottomLeft', 'bottomRight', 'left', 'leftTop', 'leftBottom'] as const;
type PopoverPlacementTuple = typeof PopoverPlacements;
export type PopoverPlacement = PopoverPlacementTuple[number];

export const StrictPopoverPlacements = ['topOnly', 'topLeftOnly', 'topRightOnly', 'rightOnly', 'rightTopOnly',
  'rightBottomOnly', 'bottomOnly', 'bottomLeftOnly', 'bottomRightOnly', 'leftOnly', 'leftTopOnly', 'leftBottomOnly'] as const;

type StrictPopoverPlacementTuple = typeof StrictPopoverPlacements;
export type StrictPopoverPlacement = StrictPopoverPlacementTuple[number];

export type PopoverPreferredPlacement = PopoverPlacement | PopoverPlacement[] | StrictPopoverPlacement | StrictPopoverPlacement[];

export const DEFAULT_POPOVER_POSITIONS = [POSITION_MAP.top, POSITION_MAP.right, POSITION_MAP.bottom, POSITION_MAP.left];

export const isStrictPopoverPlacement = (placement: string): boolean =>
  StrictPopoverPlacements.includes(placement as StrictPopoverPlacement);

export const convertStrictPopoverPlacement = (placement: StrictPopoverPlacement): PopoverPlacement => {
  const result = placement.substring(0, placement.length - 4);
  return result as PopoverPlacement;
};

export const getPlacementName = (position: ConnectedOverlayPositionChange): PopoverPlacement | undefined => {
  for (const placement in POSITION_MAP) {
    if (
      position.connectionPair.originX === POSITION_MAP[placement].originX &&
      position.connectionPair.originY === POSITION_MAP[placement].originY &&
      position.connectionPair.overlayX === POSITION_MAP[placement].overlayX &&
      position.connectionPair.overlayY === POSITION_MAP[placement].overlayY
    ) {
      return placement as PopoverPlacement;
    }
  }
  return undefined;
};

export interface PropertyMapping {
  // @ts-ignore
  [key: string]: [string, () => unknown];
}

export interface PopoverWithTrigger {
  trigger: Element;
  popoverComponent: TbPopoverComponent;
}

export interface DisplayPopoverConfig<T> extends Omit<DisplayPopoverWithComponentRefConfig<T>, 'componentRef'>{
  hostView: ViewContainerRef;
}

export interface DisplayPopoverWithComponentRefConfig<T> {
  componentRef: ComponentRef<TbPopoverComponent>
  trigger: Element;
  renderer: Renderer2;
  componentType: Type<T>;
  preferredPlacement?: PopoverPreferredPlacement;
  hideOnClickOutside?: boolean;
  injector?: Injector;
  context?: any;
  overlayStyle?: any;
  popoverStyle?: any;
  style?: any,
  showCloseButton?: boolean;
  visibleFn?: (visible: boolean) => void;
  popoverContentStyle?: any;
  isModal?: boolean;
}

export const defaultPopoverConfig: Partial<DisplayPopoverWithComponentRefConfig<any>> = {
  preferredPlacement: 'top',
  hideOnClickOutside: true,
  overlayStyle: {},
  popoverStyle: {},
  showCloseButton: true,
  visibleFn: () => {},
  popoverContentStyle: {},
  isModal: false
};
