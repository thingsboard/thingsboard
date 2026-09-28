// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  EventEmitter,
  HostBinding,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  SimpleChanges,
  ViewChild,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  isReportComponentConfig,
  ReportComponentConfig,
  ReportComponentType
} from '@shared/models/report-component.models';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragEnter,
  CdkDragExit,
  CdkDragMove,
  CdkDragRelease,
  CdkDragStart,
  CdkDropList,
  moveItemInArray
} from '@angular/cdk/drag-drop';
import {
  ReportComponentContext,
  reportComponentsLibrary,
  reportComponentTypesData
} from '@home/pages/reporting/template/components/report-component.models';
import { deepClone } from '@core/utils';
import { TbReportFormat } from '@shared/models/report.models';
import {
  IReportComponent,
  ReportComponentComponent
} from '@home/pages/reporting/template/components/report-component.component';
import { alignment } from '@shared/models/widget-settings.models';

@Component({
    selector: 'tb-report-drop-block',
    templateUrl: './report-drop-block.component.html',
    styleUrls: ['./report-drop-block.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportDropBlockComponent implements IReportComponent, OnInit, OnChanges, AfterViewInit, OnDestroy {

  @HostBinding('style.display') display = 'block';

  @ViewChild(CdkDropList) dropList?: CdkDropList;

  @ViewChildren(ReportComponentComponent)
  reportComponentComponents: QueryList<ReportComponentComponent>;

  @Input()
  context: ReportComponentContext;

  @Input()
  component: ReportComponentConfig;

  @Input()
  format: TbReportFormat;

  @Input()
  scale = 1;

  @Input()
  verticalAlignment: alignment = 'middle';

  @Input()
  paddingRight = 0;

  @Input()
  paddingLeft = 0;

  @Input()
  selected = false;

  @Output()
  componentAdded = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentRemoved = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  components: ReportComponentConfig[] = [];

  reportComponentHeight = undefined;

  showNoReportComponent = false;

  componentEntering  = false;

  constructor() {
  }

  allowDropPredicate = (drag: CdkDrag, drop: CdkDropList) => {
    return this.isDropAllowed(drag, drop);
  };

  ngOnInit(): void {
    this.updateComponents();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'component') {
          this.updateComponents();
        }
      }
    }
  }

  ngAfterViewInit() {
    if (this.dropList) {
      (this.dropList as any).nestedReportComponentContainer = true;
      (this.dropList as any).reportComponentRemoved = () => {
        this.componentRemove(null);
      };
      (this.dropList as any).reportComponentAdded = (reportComponent: ReportComponentConfig) => {
        this.componentAdd(reportComponent);
      };
      this.context.dragDropCtx.register(this.dropList);
    }
  }

  ngOnDestroy() {
    if (this.dropList) {
      this.context.dragDropCtx.deregister(this.dropList);
    }
  }

  public componentUpdated() {
    if (this.reportComponentComponents?.length) {
      this.reportComponentComponents.get(0).componentUpdated();
    }
  }

  componentDrop(event: CdkDragDrop<any[]>) {
    this.componentEntering = false;
    const item = event.item;
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      if (item.data) {
        if (typeof item.data === 'string'  && !this.components.length) {
          const libraryItem = reportComponentsLibrary.get(item.data);
          if (libraryItem) {
            const reportComponent = deepClone(libraryItem.defaultConfig);
            this.components.push(reportComponent);
            this.componentAdded.emit(reportComponent);
            if (reportComponentTypesData.getReportComponentTypeData(reportComponent.type, reportComponent.subType).editable) {
              setTimeout(() => {
                this.componentEdit.emit(reportComponent);
              }, 0);
            }
          }
        } else if (typeof item.data === 'object') {
          const prevContainer = event.previousContainer as any;
          if (prevContainer.nestedReportComponentContainer) {
            prevContainer.reportComponentRemoved();
            if (this.components.length) {
              prevContainer.reportComponentAdded(this.components[0]);
            }
          } else {
            event.previousContainer.data.splice(event.previousIndex, 1, ...this.components);
            if (prevContainer.reportComponentContainer) {
              prevContainer.reportComponentsUpdated();
            }
          }
          this.components.length = 0;
          const reportComponent: ReportComponentConfig = item.data;
          this.components.push(reportComponent);
          this.componentAdded.emit(reportComponent);
        }
      }
    }
    this.updateHeight();
  }

  isDropAllowed(drag: CdkDrag, _drop: CdkDropList) {
    if (typeof drag.data === 'string' && this.components.length) {
      return false;
    }
    let type: ReportComponentType;
    let subType: string;
    if (drag.data) {
      if (typeof drag.data === 'string') {
        const libraryItem = reportComponentsLibrary.get(drag.data);
        if (libraryItem) {
          type = libraryItem.type;
          subType = libraryItem.defaultConfig.subType;
        }
      } else if (isReportComponentConfig(drag.data)) {
        type = drag.data.type;
        subType = drag.data.subType;
      }
    }
    if (type) {
      const componentData = reportComponentTypesData.getReportComponentTypeData(type, subType);
      return !componentData.container && !componentData.pageBreak;
    }
    return false;
  }

  dragMoved(event: CdkDragMove) {
    this.context.dragDropCtx.dragMoved(event);
  }

  dragReleased(event: CdkDragRelease) {
    this.context.dragDropCtx.dragReleased(event);
  }

  dropListEnter(_event: CdkDragEnter) {
    this.componentEntering = true;
    this.reportComponentHeight = this.reportComponentComponents.length ? this.reportComponentComponents.get(0).elementRef.nativeElement.getBoundingClientRect().height / this.scale : 100;
  }

  dropListExit(event: CdkDragExit) {
    this.componentEntering = false;
    this.updateHeight(event.item.data, event.item.getPlaceholderElement().getBoundingClientRect().height / this.scale);
  }

  onComponentEdit(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  componentRemove(reportComponent: ReportComponentConfig): void {
    this.components.length = 0;
    this.componentRemoved.emit(reportComponent);
    this.updateHeight();
  }

  componentAdd(reportComponent: ReportComponentConfig): void {
    this.components.push(reportComponent);
    this.componentAdded.emit(reportComponent);
    this.updateHeight();
  }

  componentDragStarted(_event: CdkDragStart){
    document.body.style.cursor = 'grabbing';
  }

  componentDragEnded() {
    document.body.style.cursor = 'auto';
  }

  private updateComponents() {
    if (this.component) {
      this.components.length = 0;
      this.components.push(this.component);
    } else {
      this.components.length = 0;
    }
    this.updateHeight();
  }

  private updateHeight(reportComponent?: ReportComponentConfig, targetHeight?: number) {
    if (!this.components?.length || (this.components.length === 1 && this.components[0] === reportComponent)) {
      this.reportComponentHeight = !this.components?.length ? 100 : (targetHeight || 100);
      this.showNoReportComponent = true;
    } else {
      this.reportComponentHeight = undefined;
      this.showNoReportComponent = false;
    }
  }

}
