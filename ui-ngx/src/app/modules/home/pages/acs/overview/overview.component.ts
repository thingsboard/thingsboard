
import { Component, OnInit, } from '@angular/core';
import { ChartSize, ChartType } from 'chart.js';
import { MultiDataSet, Label ,Color} from 'ng2-charts';

@Component({
  selector: 'asc-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.sass']
})

export class AcsOverview implements OnInit{
  ngOnInit(): void {
  }
  hasan(){
    alert(this.doughnutChartLabels)

  }
  title = 'myFirstApp';
  doughnutChartLabels: Label[] = ['Online now', 'Past 24 hours', 'Others'];
  
  doughnutChartData: MultiDataSet = [
    [50, 50, 0]
  ];
  doughnutChartType: ChartType = 'doughnut';  

  colors: Color[] = [
    {
      backgroundColor: [
        'rgba(56,158,13,1)',
        'rgba(149,222,100,1)',
        'rgba(217,247,190,1)',
      ]
    }
  ];
}