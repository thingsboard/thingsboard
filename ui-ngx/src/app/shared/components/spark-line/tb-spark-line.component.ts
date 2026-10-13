// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, Input } from '@angular/core';

@Component({
    selector: 'tb-spark-line',
    template: '',
    styleUrls: [],
    standalone: false
})
export class TbSparkLineComponent implements AfterViewInit {

  private viewInit = false;
  private libraryLoad = false;
  private scheduleUpdateLine = false;

  private chartDataValue: number[];
  @Input()
  set chartData(value: number[]) {
    this.chartDataValue = value;
    this.updatedChartData();
  }

  get chartData(): number[] {
    return this.chartDataValue;
  }

  @Input()
  chartProperty: object;

  constructor(private elementRef: ElementRef) {
    import('jquery-sparkline/jquery.sparkline.js').then(() => {
      this.libraryLoad = true;
      if (this.viewInit && this.scheduleUpdateLine) {
        this.updatedChartData();
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewInit = true;
    if (this.libraryLoad && this.scheduleUpdateLine) {
      this.updatedChartData();
    }
  }

  private updatedChartData() {
    if (this.viewInit && this.libraryLoad) {
      ($(this.elementRef.nativeElement) as any).sparkline(this.chartData, this.chartProperty);
    } else {
      this.scheduleUpdateLine = true;
    }
  }
}
