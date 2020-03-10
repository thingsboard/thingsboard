import { Component, OnInit, OnChanges, Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { interval } from 'rxjs';
import { filter, tap } from 'rxjs/operators';

@Component({
  selector: 'tb-history-selector',
  templateUrl: './history-selector.component.html',
  styleUrls: ['./history-selector.component.scss']
})
export class HistorySelectorComponent implements OnInit, OnChanges {

  @Input() settings
  @Input() intervals = [];

  @Output() onTimeUpdated = new EventEmitter();

  animationTime;
  minTimeIndex = 0;
  maxTimeIndex = 0;
  speed = 1;
  index = 0;
  playing = false;
  interval;


  constructor(private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
  }

  ngOnChanges() {
    this.maxTimeIndex = this.intervals?.length - 1;
  }

  play() {
    this.playing = true;
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
        })
  }

  pause() {
    this.playing = false;
  }
  /*
    setSpeed() {
      if (this.interval) this.interval.unsubscribe();
      this.interval = interval(1000 / this.speed)
        .pipe(
          takeWhile(() => this.index < this.maxTimeIndex),
          filter(() => this.play),
          tap(() => this.index++))
        .subscribe(() => {
          console.log(this.intervals);
  
          this.onTimeUpdated.emit(this.intervals[this.index]);
  
        }, err => {
          console.log(err);
        }, () => {
          this.index = this.minTimeIndex;
          this.play = false;
        })
    }*/




  /*
   
    playMove(play) {
      if (play && this.isPlaying) return;
      if (play || this.isPlaying) this.isPlaying = true;
      if (this.isPlaying) {
        moveInc(1);
        this.timeout = $timeout(function () {
          this.playMove();
        }, 1000 / this.speed)
      }
    };
   
    moveNext() {
      this.stopPlay();
      if (this.staticSettings.usePointAsAnchor) {
        let newIndex = this.maxTimeIndex;
        for (let index = this.index + 1; index < this.maxTimeIndex; index++) {
          if (this.trips.some(function (trip) {
            return calculateCurrentDate(trip.timeRange, index).hasAnchor;
          })) {
            newIndex = index;
            break;
          }
        }
        this.moveToIndex(newIndex);
      } else moveInc(1);
    };
   
    movePrev() {
      this.stopPlay();
      if (this.staticSettings.usePointAsAnchor) {
        let newIndex = this.minTimeIndex;
        for (let index = this.index - 1; index > this.minTimeIndex; index--) {
          if (this.trips.some(function (trip) {
            return calculateCurrentDate(trip.timeRange, index).hasAnchor;
          })) {
            newIndex = index;
            break;
          }
        }
        this.moveToIndex(newIndex);
      } else moveInc(-1);
    };
   
    moveStart = function () {
      this.stopPlay();
      this.moveToIndex(this.minTimeIndex);
    };
   
    moveEnd = function () {
      this.stopPlay();
      this.moveToIndex(this.maxTimeIndex);
    };
   
    stopPlay = function () {
      if (this.isPlaying) {
        this.isPlaying = false;
        $timeout.cancel(this.timeout);
      }
    };
   
    moveInc(inc) {
      let newIndex = this.index + inc;
      this.moveToIndex(newIndex);
    }
   
    moveToIndex(newIndex) {
      if (newIndex > this.maxTimeIndex || newIndex < this.minTimeIndex) return;
      this.index = newIndex;
      this.animationTime = this.minTime + this.index * this.staticSettings.normalizationStep;
      recalculateTrips();
    }
  */
  recalculateTrips() {

  }

}
