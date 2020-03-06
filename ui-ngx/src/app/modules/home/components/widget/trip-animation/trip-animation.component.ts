import { Component, OnInit, Input, ViewChild, AfterViewInit } from '@angular/core';
import { MapWidgetController } from '../lib/maps/map-widget2';
import { MapProviders } from '../lib/maps/map-models';
import { parseArray } from '@app/core/utils';

@Component({
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit {

  @Input() ctx;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData

  constructor() { }

  ngOnInit(): void {
    console.log(this.ctx);
    this.historicalData = parseArray(this.ctx.data);
    console.log("TripAnimationComponent -> ngOnInit -> this.historicalData",this.ctx.data, this.historicalData)
  }

  ngAfterViewInit() {
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, this.ctx, this.mapContainer.nativeElement);
    this.mapWidget.data
  }
}
