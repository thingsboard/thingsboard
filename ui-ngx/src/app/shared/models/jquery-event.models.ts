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
