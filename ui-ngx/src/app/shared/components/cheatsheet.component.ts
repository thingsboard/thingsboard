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

import { Component, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import { MousetrapInstance } from 'mousetrap';
import Mousetrap from 'mousetrap';

@Component({
  selector : 'tb-hotkeys-cheatsheet',
  styles : [`
.tb-hotkeys-container {
  display: table !important;
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  color: #333;
  font-size: 1em;
  background-color: rgba(255,255,255,0.9);
  outline: 0;
}
.tb-hotkeys-container.fade {
  z-index: -1024;
  visibility: hidden;
  opacity: 0;
  -webkit-transition: opacity 0.15s linear;
  -moz-transition: opacity 0.15s linear;
  -o-transition: opacity 0.15s linear;
  transition: opacity 0.15s linear;
}
.tb-hotkeys-container.fade.in {
  z-index: 10002;
  visibility: visible;
  opacity: 1;
}
.tb-hotkeys-title {
  font-weight: bold;
  text-align: center;
  font-size: 1.2em;
}
.tb-hotkeys {
  width: 100%;
  height: 100%;
  display: table-cell;
  vertical-align: middle;
}
.tb-hotkeys table {
  margin: auto;
  color: #333;
}
.tb-content {
  display: table-cell;
  vertical-align: middle;
}
.tb-hotkeys-keys {
  padding: 5px;
  text-align: right;
}
.tb-hotkeys-key {
  display: inline-block;
  color: #fff;
  background-color: #333;
  border: 1px solid #333;
  border-radius: 5px;
  text-align: center;
  margin-right: 5px;
  box-shadow: inset 0 1px 0 #666, 0 1px 0 #bbb;
  padding: 5px 9px;
  font-size: 1em;
}
.tb-hotkeys-text {
  padding-left: 10px;
  font-size: 1em;
}
.tb-hotkeys-close {
  position: fixed;
  top: 20px;
  right: 20px;
  font-size: 2em;
  font-weight: bold;
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  min-height: 45px;
  min-width: 45px;
  text-align: center;
}
.tb-hotkeys-close:hover {
  background-color: #fff;
  cursor: pointer;
}
@media all and (max-width: 500px) {
  .tb-hotkeys {
    font-size: 0.8em;
  }
}
@media all and (min-width: 750px) {
  .tb-hotkeys {
    font-size: 1.2em;
  }
}  `],
  template : `<div tabindex="-1" class="tb-hotkeys-container fade" [class.in]="helpVisible" style="display:none"><div class="tb-hotkeys">
  <h4 class="tb-hotkeys-title">{{ title }}</h4>
  <table *ngIf="helpVisible"><tbody>
    <tr *ngFor="let hotkey of hotkeysList">
      <td class="tb-hotkeys-keys">
        <span *ngFor="let key of hotkey.formatted" class="tb-hotkeys-key">{{ key }}</span>
      </td>
      <td class="tb-hotkeys-text">{{ hotkey.description }}</td>
    </tr>
  </tbody></table>
  <div class="tb-hotkeys-close" (click)="toggleCheatSheet()">&#215;</div>
</div></div>`,
})
export class TbCheatSheetComponent implements OnInit, OnDestroy {

  helpVisible = false;
  @Input() title = 'Keyboard Shortcuts:';

  @Input()
  hotkeys: Hotkey[];

  hotkeysList: Hotkey[];

  private mousetrap: MousetrapInstance;

  constructor(private elementRef: ElementRef,
              private hotkeysService: HotkeysService) {
    this.mousetrap = new Mousetrap(this.elementRef.nativeElement);
    this.mousetrap.bind('?', (event: KeyboardEvent, combo: string) => {
      this.toggleCheatSheet();
    });
  }

  public ngOnInit(): void {
    if (this.hotkeys) {
      this.hotkeysList = this.hotkeys.filter(hotkey => hotkey.description);
    }
  }

  public setHotKeys(hotkeys: Hotkey[]) {
    this.hotkeysList = hotkeys.filter(hotkey => hotkey.description);
  }

  public toggleCheatSheet(): void {
    this.helpVisible = !this.helpVisible;
  }

  ngOnDestroy() {
    this.mousetrap.unbind('?');
  }
}
