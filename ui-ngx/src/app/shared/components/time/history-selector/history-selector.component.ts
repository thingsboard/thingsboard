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

import { Component, OnInit, OnChanges, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { interval, Subscription, Subscriber, SubscriptionLike, Observer } from 'rxjs';
import { filter, tap } from 'rxjs/operators';

@Component({
  selector: 'tb-history-selector',
  templateUrl: './history-selector.component.html',
  styleUrls: ['./history-selector.component.scss']
})
export class HistorySelectorComponent implements OnInit, OnChanges {

  @Input() settings
  @Input() intervals = [];

  @Output() onTimeUpdated: EventEmitter<number> = new EventEmitter();

  animationTime;
  minTimeIndex = 0;
  maxTimeIndex = 0;
  speed = 1;
  index = 0;
  playing = false;
  interval;
  speeds = [1, 5, 10, 25];


  constructor(private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
  }

  ngOnChanges() {
    this.maxTimeIndex = this.intervals?.length - 1;
  }

  play() {
    this.playing = true;
    if (!this.interval)
      this.interval = interval(1000 / this.speed)
        .pipe(
          filter(() => this.playing),
          tap(() => this.index++)).subscribe(() => {
            if (this.index < this.maxTimeIndex) {
              this.cd.detectChanges();
              this.onTimeUpdated.emit(this.intervals[this.index]);
            }
            else {
              this.interval.complete();
            }
          }, err => {
            console.log(err);
          }, () => {
            this.index = this.minTimeIndex;
            this.playing = false;
            this.interval = null;
            this.cd.detectChanges();
          });          
  }

  reeneble() {
    if (this.playing) {
      let position = this.index;
      this.interval.complete();
      this.index = position;
      this.play();
    }
  }

  pause() {
    this.playing = false;
    this.cd.detectChanges();
    this.onTimeUpdated.emit(this.intervals[this.index]);
  }

  moveNext() {
    if (this.index < this.maxTimeIndex) {
      this.index++;
    }
    this.pause();
  }

  movePrev() {
    if (this.index > this.minTimeIndex) {
      this.index++;
    }
    this.pause();
  }

  moveStart() {
    this.index = this.minTimeIndex;
    this.pause();
  }

  moveEnd() {
    this.index = this.maxTimeIndex;
    this.pause();
  }

  changeIndex() {
    this.onTimeUpdated.emit(this.intervals[this.index]);
  }
}
