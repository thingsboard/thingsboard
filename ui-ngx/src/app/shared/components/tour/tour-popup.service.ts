// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, Renderer2, ViewContainerRef } from '@angular/core';
import { TbPopoverService } from '@shared/components/popover.service';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { PopoverPreferredPlacement } from '@shared/components/popover.models';
import { TourPopupComponent } from '@shared/components/tour/tour-popup.component';

export interface TourPopupConfig {
  target: Element;
  hostView: ViewContainerRef;
  renderer: Renderer2;
  placement: PopoverPreferredPlacement;
  title: string;
  text: string;
  nextLabel: string;
  count?: string | null;
  skipLabel?: string | null;
  closable?: boolean;
  hint?: boolean;
  foot?: string | null;
  onNext?: () => void;
  onSkip?: () => void;
  onClose?: () => void;
  onEscape?: () => void;
  onHidden?: () => void;
}

export const escapeTourText = (text: string): string =>
  text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

export interface TourPopupRef {
  close(): void;
}

@Injectable()
export class TourPopupService {

  constructor(private popoverService: TbPopoverService) {}

  show(config: TourPopupConfig): TourPopupRef {
    let open = true;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && open) {
        event.preventDefault();
        config.onEscape();
      }
    };
    if (config.onEscape) {
      document.addEventListener('keydown', onKey, true);
    }
    const popover: TbPopoverComponent<TourPopupComponent> = this.popoverService.displayPopover({
      trigger: config.target,
      renderer: config.renderer,
      componentType: TourPopupComponent,
      hostView: config.hostView,
      preferredPlacement: config.placement,
      hideOnClickOutside: false,
      showCloseButton: false,
      context: {
        title: config.title,
        text: config.text,
        nextLabel: config.nextLabel,
        count: config.count ?? null,
        skipLabel: config.skipLabel ?? null,
        closable: config.closable ?? true,
        hint: config.hint ?? false,
        foot: config.foot ?? null
      },
      visibleFn: visible => {
        if (!visible) {
          document.removeEventListener('keydown', onKey, true);
        }
        if (!visible && open) {
          open = false;
          config.onHidden?.();
        }
      }
    });
    popover.tbDestroy.subscribe(() => {
      open = false;
      document.removeEventListener('keydown', onKey, true);
    });
    popover.tbOverlayClassName = 'tb-tour-popover';
    popover.updateStyles();
    const instance = popover.tbComponentRef.instance;
    instance.next.subscribe(() => config.onNext?.());
    instance.skip.subscribe(() => config.onSkip?.());
    instance.closed.subscribe(() => config.onClose?.());
    return {
      close: () => {
        document.removeEventListener('keydown', onKey, true);
        if (open) {
          open = false;
          popover.hide();
        }
      }
    };
  }
}
