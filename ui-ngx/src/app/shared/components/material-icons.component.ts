import { PageComponent } from '@shared/components/page.component';
import { OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormControl } from '@angular/forms';
import { BehaviorSubject, Observable, ReplaySubject } from 'rxjs';

export class MaterialIconsComponent extends PageComponent implements OnInit {

  searchIconsControl: UntypedFormControl;
  showAllSubject = new BehaviorSubject<boolean>(false);

  icons$: Observable<Array<string>>;

  constructor(protected store: Store<AppState>) {
    super(store);
    this.searchIconsControl = new UntypedFormControl('');
  }

  ngOnInit(): void {

  }

}
