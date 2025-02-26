///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation
} from '@angular/core';
import { TripTimelineSettings } from '@home/components/widget/lib/maps/models/map.models';

@Component({
  selector: 'tb-map-timeline-panel',
  templateUrl: './map-timeline-panel.component.html',
  styleUrls: ['./map-timeline-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapTimelinePanelComponent implements OnInit, OnDestroy {

  @Input()
  settings: TripTimelineSettings;

  @Input()
  disabled = false;

  @Input()
  min = 0;

  @Input()
  max = 10000;

  @Output()
  timeChanged = new EventEmitter<number>();

  currentTime = 0;

  constructor(public element: ElementRef<HTMLElement>) {
  }

  ngOnInit() {
  }

  ngOnDestroy() {
  }

  public onTimeChange() {
    //this.updateValueText();
    this.timeChanged.next(this.currentTime);
  }

}
