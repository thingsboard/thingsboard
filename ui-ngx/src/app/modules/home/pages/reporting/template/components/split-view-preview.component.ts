// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild, ViewEncapsulation } from '@angular/core';
import {
  AbstractReportComponentPreviewContainer,
  IReportComponent
} from '@home/pages/reporting/template/components/report-component.component';
import { ReportComponentConfig, SplitViewReportComponentConfig } from '@shared/models/report-component.models';
import { ReportDropBlockComponent } from '@home/pages/reporting/template/components/report-drop-block.component';
import { alignment } from '@shared/models/widget-settings.models';

@Component({
    selector: 'tb-split-view-preview',
    templateUrl: './split-view-preview.component.html',
    styleUrls: ['./split-view-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SplitViewPreviewComponent extends AbstractReportComponentPreviewContainer<SplitViewReportComponentConfig> implements AfterViewInit, OnDestroy {

  splitContainerEl = viewChild('splitContainer', {
    read: ElementRef<HTMLElement>,
  });

  leftView = viewChild('leftView', {
    read: ReportDropBlockComponent,
  });

  rightView = viewChild('rightView', {
    read: ReportDropBlockComponent,
  });

  private splitContainerResize$: ResizeObserver;

  leftWidth = '50%';
  centerWidth = 8;
  rightWidth= '50%';

  leftVerticalAlignment: alignment;
  rightVerticalAlignment: alignment;

  onComponentUpdated() {
    const splitPosition = this.reportComponent.splitPosition;
    this.centerWidth = this.reportComponent.splitGap;
    this.leftWidth = splitPosition + '%';
    this.rightWidth = (100 - splitPosition) + '%';
    this.leftVerticalAlignment = this.reportComponent.leftVerticalAlignment;
    this.rightVerticalAlignment = this.reportComponent.rightVerticalAlignment;
  }

  ngAfterViewInit() {
    this.splitContainerResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.splitContainerResize$.observe(this.splitContainerEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.splitContainerResize$) {
      this.splitContainerResize$.disconnect();
    }
  }

  childComponentEdit(leftElseRight: boolean): void {
    this.componentEdit.emit(leftElseRight ? this.reportComponent.leftView : this.reportComponent.rightView);
  }

  childComponentRemoved(component: ReportComponentConfig, leftElseRight: boolean) {
    if (leftElseRight) {
      this.reportComponent.leftView = null;
    } else {
      this.reportComponent.rightView = null;
    }
    if (component) {
      this.componentRemoved.emit(component);
    }
    this.componentsChanged.emit();
  }

  childComponentAdded(component: ReportComponentConfig, leftElseRight: boolean) {
    if (leftElseRight) {
      this.reportComponent.leftView = component;
    } else {
      this.reportComponent.rightView = component;
    }
    this.componentsChanged.emit();
  }

  public getAllChildReportComponentConfigs(): ReportComponentConfig[] {
    const reportComponents: ReportComponentConfig[] = [];
    if (this.reportComponent.leftView) {
      reportComponents.push(this.reportComponent.leftView);
    }
    if (this.reportComponent.rightView) {
      reportComponents.push(this.reportComponent.rightView);
    }
    return reportComponents;
  }

  protected getAllChildReportComponents(): IReportComponent[] {
    const reportComponents: IReportComponent[] = [];
    let comp = this.leftView();
    if (comp) {
      reportComponents.push(comp);
    }
    comp = this.rightView();
    if (comp) {
      reportComponents.push(comp);
    }
    return reportComponents;
  }

  protected findChildReportComponent(reportComponent: ReportComponentConfig): IReportComponent {
    if (this.reportComponent.leftView === reportComponent) {
      return this.leftView();
    } else if (this.reportComponent.rightView === reportComponent) {
      return this.rightView();
    }
    return null;
  }

}
