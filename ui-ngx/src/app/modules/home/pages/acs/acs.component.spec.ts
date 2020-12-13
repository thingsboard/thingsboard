import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcsComponent } from './acs.component';

describe('AcsComponent', () => {
  let component: AcsComponent;
  let fixture: ComponentFixture<AcsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AcsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AcsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
