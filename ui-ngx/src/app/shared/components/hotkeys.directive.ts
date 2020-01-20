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

import {Directive, Input, OnInit, OnDestroy, ElementRef} from '@angular/core';
import {Hotkey, ExtendedKeyboardEvent} from 'angular2-hotkeys';
import 'mousetrap';
import { TbCheatSheetComponent } from '@shared/components/cheatsheet.component';

@Directive({
  selector : '[tb-hotkeys]'
})
export class TbHotkeysDirective implements OnInit, OnDestroy {
  @Input() hotkeys: Hotkey[] = [];
  @Input() cheatSheet: TbCheatSheetComponent;

  private mousetrap: MousetrapInstance;
  private hotkeysList: Hotkey[] = [];

  private _preventIn = ['INPUT', 'SELECT', 'TEXTAREA'];

  constructor(private _elementRef: ElementRef) {
    this.mousetrap = new Mousetrap(this._elementRef.nativeElement);
    (this._elementRef.nativeElement as HTMLElement).tabIndex = -1;
    (this._elementRef.nativeElement as HTMLElement).style.outline = '0';
  }

  ngOnInit() {
    for (let hotkey of this.hotkeys) {
      this.hotkeysList.push(hotkey);
      this.bindEvent(hotkey);
    }
    if (this.cheatSheet) {
      let hotkeyObj: Hotkey = new Hotkey(
        '?',
        (event: KeyboardEvent) => {
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
    this.mousetrap.bind((<Hotkey>hotkey).combo, (event: KeyboardEvent, combo: string) => {
      let shouldExecute = true;
      if(event) {
        let target: HTMLElement = <HTMLElement>(event.target || event.srcElement);
        let nodeName: string = target.nodeName.toUpperCase();
        if((' ' + target.className + ' ').indexOf(' mousetrap ') > -1) {
          shouldExecute = true;
        } else if(this._preventIn.indexOf(nodeName) > -1 && (<Hotkey>hotkey).allowIn.map(allow => allow.toUpperCase()).indexOf(nodeName) === -1) {
          shouldExecute = false;
        }
      }

      if(shouldExecute) {
        return (<Hotkey>hotkey).callback.apply(this, [event, combo]);
      }
    });
  }

  ngOnDestroy() {
    for (let hotkey of this.hotkeysList) {
      this.mousetrap.unbind(hotkey.combo);
    }
  }

}
