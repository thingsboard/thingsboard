import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'tb-history-selector',
  templateUrl: './history-selector.component.html',
  styleUrls: ['./history-selector.component.scss']
})
export class HistorySelectorComponent implements OnInit {

  @Input() settings

  animationTime


  constructor() { }

  ngOnInit(): void {
    console.log(this.settings);
    
  }

}
