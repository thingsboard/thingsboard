///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, Output } from '@angular/core';
import { MatChip, MatChipList } from '@angular/material/chips';
import Timeout = NodeJS.Timeout;

export interface MatChipDropEvent {
  from: number;
  to: number;
}

@Directive({
  selector: 'mat-chip-list[tb-chip-draggable]',
})
export class MatChipDraggableDirective implements AfterViewInit {

  @Output()
  chipDrop = new EventEmitter<MatChipDropEvent>();

  private draggableChips: Array<DraggableChip> = [];

  constructor(private chipsList: MatChipList,
              private elementRef: ElementRef<HTMLElement>) {
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp() {
    this.draggableChips.forEach((draggableChip) => {
      draggableChip.preventDrag = false;
    });
  }

  ngAfterViewInit(): void {
    this.configureDraggableChipList();
    this.chipsList.chips.changes.subscribe(() => {
      this.configureDraggableChipList();
    });
  }

  private configureDraggableChipList() {
    const toRemove: Array<DraggableChip> = [];
    this.chipsList.chips.forEach((chip) => {
        const found = this.draggableChips.find((draggableChip) => draggableChip.chip === chip);
        if (!found) {
          this.draggableChips.push(new DraggableChip(chip,
            this.chipsList,
            this.elementRef.nativeElement,
            this.chipDrop));
        }
      }
    );
    this.draggableChips.forEach((draggableChip) => {
      const found = this.chipsList.chips.find((chip) => chip === draggableChip.chip);
      if (!found) {
        toRemove.push(draggableChip);
      }
    });
    toRemove.forEach((draggableChip) => {
      const index = this.draggableChips.indexOf(draggableChip);
      this.draggableChips.splice(index, 1);
    });
  }
}

const draggingClassName = 'dragging';
const droppingClassName = 'dropping';
const droppingBeforeClassName = 'dropping-before';
const droppingAfterClassName = 'dropping-after';

let globalDraggingChipListId = null;

class DraggableChip {

  private chipElement: HTMLElement;
  private readonly handle: HTMLElement;

  private dragging = false;
  private counter = 0;

  private dropPosition: 'after' | 'before';

  private dropTimeout: Timeout;

  public preventDrag = false;

  private dropHandler = this.onDrop.bind(this);
  private dragOverHandler = this.onDragOver.bind(this);

  constructor(public chip: MatChip,
              private chipsList: MatChipList,
              private chipListElement: HTMLElement,
              private chipDrop: EventEmitter<MatChipDropEvent>) {
    this.chipElement = chip._elementRef.nativeElement;
    this.chipElement.setAttribute('draggable', 'true');
    this.handle = this.chipElement.getElementsByClassName('tb-chip-drag-handle')[0] as HTMLElement;
    this.chipElement.addEventListener('mousedown', this.onMouseDown.bind(this));
    this.chipElement.addEventListener('dragstart', this.onDragStart.bind(this));
    this.chipElement.addEventListener('dragend', this.onDragEnd.bind(this));
    this.chipElement.addEventListener('dragenter', this.onDragEnter.bind(this));
    this.chipElement.addEventListener('dragleave', this.onDragLeave.bind(this));
  }

  private onMouseDown(event: MouseEvent) {
    if (event.target !== this.handle) {
      this.preventDrag = true;
    }
  }

  private onDragStart(event: Event | any) {
    if (this.preventDrag) {
      event.preventDefault();
    } else {
      this.dragging = true;
      globalDraggingChipListId = this.chipListElement.id;
      this.chipListElement.classList.add(draggingClassName);
      this.chipElement.classList.add(draggingClassName);
      event = (event as any).originalEvent || event;
      const dataTransfer = event.dataTransfer;
      dataTransfer.effectAllowed = 'copyMove';
      dataTransfer.dropEffect = 'move';
      dataTransfer.setData('text', this.index() + '');
    }
  }

  private onDragEnter(event: Event | any) {
    this.counter++;
    if (this.dragging) {
      return;
    }
    this.chipElement.removeEventListener('dragover', this.dragOverHandler);
    this.chipElement.removeEventListener('drop', this.dropHandler);

    this.chipElement.addEventListener('dragover', this.dragOverHandler);
    this.chipElement.addEventListener('drop', this.dropHandler);
  }

  private onDragLeave(event: Event | any) {
    this.counter--;
    if (this.counter <= 0) {
      this.counter = 0;
      this.chipElement.classList.remove(droppingClassName);
      this.chipElement.classList.remove(droppingAfterClassName);
      this.chipElement.classList.remove(droppingBeforeClassName);
    }
  }

  private onDragEnd(event: Event | any) {
    this.dragging = false;
    globalDraggingChipListId = null;
    this.chipListElement.classList.remove(draggingClassName);
    this.chipElement.classList.remove(draggingClassName);
  }

  private onDragOver(event: Event | any) {
    if (this.dragging) {
      return;
    }
    event.preventDefault();
    if (globalDraggingChipListId !== this.chipListElement.id) {
      return;
    }
    const bounds = this.chipElement.getBoundingClientRect();
    event = (event as any).originalEvent || event;
    const props = {
      width: bounds.right - bounds.left,
      height: bounds.bottom - bounds.top,
      x: event.clientX - bounds.left,
      y: event.clientY - bounds.top,
    };

    const horizontalOffset = props.x;
    const horizontalMidPoint = props.width / 2;

    const verticalOffset = props.y;
    const verticalMidPoint = props.height / 2;

    this.chipElement.classList.add(droppingClassName);

    this.chipElement.classList.remove(droppingAfterClassName);
    this.chipElement.classList.remove(droppingBeforeClassName);

    if (horizontalOffset >= horizontalMidPoint || verticalOffset >= verticalMidPoint) {
      this.dropPosition = 'after';
      this.chipElement.classList.add(droppingAfterClassName);
    } else {
      this.dropPosition = 'before';
      this.chipElement.classList.add(droppingBeforeClassName);
    }

  }

  private onDrop(event: Event | any) {
    this.counter = 0;
    event.preventDefault();
    if (globalDraggingChipListId !== this.chipListElement.id) {
      return;
    }
    event = (event as any).originalEvent || event;
    const droppedItemIndex = parseInt(event.dataTransfer.getData('text'), 10);
    const currentIndex = this.index();
    let newIndex;
    if (this.dropPosition === 'before') {
      if (droppedItemIndex < currentIndex) {
        newIndex = currentIndex - 1;
      } else {
        newIndex = currentIndex;
      }
    } else {
      if (droppedItemIndex < currentIndex) {
        newIndex = currentIndex;
      } else {
        newIndex = currentIndex + 1;
      }
    }
    if (this.dropTimeout) {
      clearTimeout(this.dropTimeout);
    }
    this.dropTimeout = setTimeout(() => {
      this.dropPosition = null;

      this.chipElement.classList.remove(droppingClassName);
      this.chipElement.classList.remove(droppingAfterClassName);
      this.chipElement.classList.remove(droppingBeforeClassName);

      this.chipElement.removeEventListener('drop', this.dropHandler);

      const dropEvent: MatChipDropEvent = {
        from: droppedItemIndex,
        to: newIndex
      };
      this.chipDrop.emit(dropEvent);
    }, 1000 / 16);
  }

  private index(): number {
    return this.chipsList.chips.toArray().indexOf(this.chip);
  }

}
