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

import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Hotkey } from 'angular2-hotkeys';
import { MousetrapInstance } from 'mousetrap';
import Mousetrap from 'mousetrap';
import { TbCheatSheetComponent } from '@shared/components/cheatsheet.component';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: '[tb-hotkeys]',
    standalone: false
})
export class TbHotkeysDirective implements OnInit, OnDestroy {
  @Input('tb-hotkeys') hotkeys: Hotkey[] = [];
  @Input() cheatSheet: TbCheatSheetComponent;

  private mousetrap: MousetrapInstance;
  private hotkeysList: Hotkey[] = [];

  private preventIn = ['INPUT', 'SELECT', 'TEXTAREA'];

  constructor(private elementRef: ElementRef) {
    this.mousetrap = new Mousetrap(this.elementRef.nativeElement);
    (this.elementRef.nativeElement as HTMLElement).tabIndex = -1;
    (this.elementRef.nativeElement as HTMLElement).style.outline = '0';
  }

  ngOnInit() {
    for (const hotkey of this.hotkeys) {
      this.hotkeysList.push(hotkey);
      this.bindEvent(hotkey);
    }
    if (this.cheatSheet) {
      const hotkeyObj: Hotkey = new Hotkey(
        '?',
        () => {
          this.cheatSheet.toggleCheatSheet();
          return false;
        },
        [],
        'Show / hide this help menu',
      );
      this.hotkeysList.unshift(hotkeyObj);
      this.bindEvent(hotkeyObj);
      this.cheatSheet.setHotKeys(this.hotkeysList);
    }
  }

  private bindEvent(hotkey: Hotkey): void {
    this.mousetrap.bind((hotkey as Hotkey).combo, (event: KeyboardEvent, combo: string) => {
      let shouldExecute = true;
      if (event) {
        const target: HTMLElement = event.target as HTMLElement;
        const nodeName: string = target.nodeName.toUpperCase();
        if ((' ' + target.className + ' ').indexOf(' mousetrap ') > -1) {
          shouldExecute = true;
        } else if (this.preventIn.indexOf(nodeName) > -1 && (hotkey as Hotkey).
                   allowIn.map(allow => allow.toUpperCase()).indexOf(nodeName) === -1) {
          shouldExecute = false;
        }
      }

      if (shouldExecute) {
        return (hotkey as Hotkey).callback.apply(this, [event, combo]);
      }
    });
  }

  ngOnDestroy() {
    for (const hotkey of this.hotkeysList) {
      this.mousetrap.unbind(hotkey.combo);
    }
  }

}
