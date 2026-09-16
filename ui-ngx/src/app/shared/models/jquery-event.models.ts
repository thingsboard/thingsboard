// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import Timeout = NodeJS.Timeout;

export interface TbContextMenuEvent extends Event {
  clientX: number;
  clientY: number;
  pageX: number;
  pageY: number;
  ctrlKey: boolean;
  metaKey: boolean;
}

const isIOSDevice = (): boolean =>
  /iPhone|iPad|iPod/i.test(navigator.userAgent) || (navigator.userAgent.includes('Mac') && 'ontouchend' in document);

export const initCustomJQueryEvents = () => {
  $.event.special.tbcontextmenu = {
    setup(this: HTMLElement) {
      const el = $(this);
      if (isIOSDevice()) {
        let timeoutId: Timeout;

        el.on('touchstart', (e) => {
          e.stopPropagation();
          timeoutId = setTimeout(() => {
            timeoutId = null;
            const touch = e.originalEvent.changedTouches[0];
            const event = $.Event('tbcontextmenu', {
              clientX: touch.clientX,
              clientY: touch.clientY,
              pageX: touch.pageX,
              pageY: touch.pageY,
              ctrlKey: false,
              metaKey: false,
              originalEvent: e
            });
            el.trigger(event, e);
          }, 500);
        });

        el.on('touchend touchmove', () => {
          if (timeoutId) {
            clearTimeout(timeoutId);
          }
        });
      } else {
        el.on('contextmenu', (e) => {
          const event = $.Event('tbcontextmenu', {
            clientX: e.originalEvent.clientX,
            clientY: e.originalEvent.clientY,
            pageX: e.originalEvent.pageX,
            pageY: e.originalEvent.pageY,
            ctrlKey: e.originalEvent.ctrlKey,
            metaKey: e.originalEvent.metaKey,
            originalEvent: e
          });
          el.trigger(event, e);
        });
      }
    },
    teardown(this: HTMLElement) {
      const el = $(this);
      if (isIOSDevice()) {
        el.off('touchstart touchend touchmove');
      } else {
        el.off('contextmenu');
      }
    }
  };
};
