import { Component, OnInit, Input, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { MapWidgetController } from '../lib/maps/map-widget2';
import { MapProviders } from '../lib/maps/map-models';
import { parseArray } from '@app/core/utils';
import { interpolateArray } from '../lib/maps/maps-utils';

@Component({
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit {

  @Input() ctx;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData;
  intervals;
  normalizationStep = 500;
  interpolatedData = [];


  constructor(private cd: ChangeDetectorRef) { }

  ngOnInit(): void {
    let subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) subscription.callbacks.onDataUpdated = (updated) => {
      this.historicalData = parseArray(this.ctx.data);
      this.historicalData.forEach(el => {
      console.log("TripAnimationComponent -> if -> el", el)
        el.longitude += (Math.random() - 0.5)
        el.latitude += (Math.random() - 0.5)
      });
      this.calculateIntervals();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, this.ctx, this.mapContainer.nativeElement);
    this.mapWidget.data
  }

  timeUpdated(time) {
     //this.mapWidget.ma 
     const currentPosition = this.interpolatedData.map(dataSource=>dataSource[time]);
     console.log("TripAnimationComponent -> timeUpdated -> currentPosition", currentPosition)
  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.intervals = [];
      for (let time = dataSource[0]?.time; time < dataSource[dataSource.length - 1]?.time; time += this.normalizationStep) {
        this.intervals.push(time);
      }
      this.intervals.push(dataSource[dataSource.length - 1]?.time);
      this.interpolatedData[index] = interpolateArray(dataSource, this.intervals);
      console.log("TripAnimationComponent -> calculateIntervals -> this.intervals", this.intervals)

    });
  }

}
