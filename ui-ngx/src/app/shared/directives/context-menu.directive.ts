// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Directive, ElementRef, EventEmitter, OnDestroy, Output } from '@angular/core';
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

@Directive({
    selector: '[tbcontextmenu]',
    standalone: false
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
