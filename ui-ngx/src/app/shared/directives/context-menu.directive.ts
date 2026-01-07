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

import { Directive, ElementRef, EventEmitter, OnDestroy, Output } from '@angular/core';
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

@Directive({
  selector: '[tbcontextmenu]'
})
export class ContextMenuDirective implements OnDestroy {

  @Output()
  tbcontextmenu = new EventEmitter<TbContextMenuEvent>();

  constructor(private el: ElementRef) {
    $(this.el.nativeElement).on('tbcontextmenu', (e: TbContextMenuEvent) => this.tbcontextmenu.emit(e));
  }

  ngOnDestroy() {
    $(this.el.nativeElement).off('tbcontextmenu');
  }
}
