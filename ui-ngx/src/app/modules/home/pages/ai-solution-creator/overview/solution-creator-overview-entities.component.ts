// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterContentInit,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  Renderer2,
  Type,
  ViewChild,
  ViewContainerRef
} from "@angular/core";
import {
  SolutionDescriptor,
  SolutionDescriptorAlarm,
  SolutionDescriptorEntityBase,
  SolutionDescriptorEntityBaseRelation,
  SolutionDescriptorIam,
  SolutionDescriptorMetric,
  SolutionEntity
} from '@shared/models/solution-creator.models';
import { FormBuilder } from '@angular/forms';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepClone } from '@core/utils';
import { alarmSeverityColors } from '@shared/models/alarm.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatIconButton } from '@angular/material/button';
import { AlarmInfoPanelComponent } from './alarm-info-panel.component';
import { EntityInfoPanelComponent } from './entity-info-panel.component';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { ClusterNode, Edge, GraphComponent, Node } from '@swimlane/ngx-graph';
import { MetricInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/metric-info-panel.component';
import { IamInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/iam-info-panel.component';
import { StrictPopoverPlacements } from '@shared/components/popover.models';

@Component({
  selector: 'tb-solution-creator-overview-entities',
  templateUrl: './solution-creator-overview-entities.component.html',
  styleUrls: ['./solution-creator-overview.scss', './solution-creator-overview-entities.component.scss'],
  standalone: false
})
export class SolutionCreatorOverviewEntitiesComponent implements OnChanges, AfterContentInit, OnDestroy {

  @Input({required: true})
  description: SolutionDescriptor;

  @Input({required: true})
  solutionName: string;

  @Output()
  updatedDescription: EventEmitter<SolutionDescriptor> = new EventEmitter<SolutionDescriptor>();

  @HostBinding('class.entering')
  initComponent = false;

  readonly solutionEntities = [
    {value: 'entityProfiles', title: 'solution-creator.entity-profiles'},
    {value: 'iam', title: 'solution-creator.iam', fullTitle: 'solution-creator.iam-full'},
    {value: 'metrics', title: 'entity.type-calculated-fields'},
    {value: 'alarms', title: 'alarm.alarms'},
  ];

  protected readonly Object = Object;

  solutionEntity = this.fb.control(['entityProfiles'], {nonNullable: true});
  solutionEntityTitle = 'solution-creator.entity-profiles';
  solutionDescriptor = this.fb.control<SolutionDescriptor>(null);
  solutionMode = this.fb.control('basic');
  profileMode = this.fb.control('table');
  nodes: Node[] = [];
  links: Edge[] = [];
  clusters: ClusterNode[] = [];
  @ViewChild('graphContainer', {static: false}) graphContainer: ElementRef<HTMLElement>;
  @ViewChild(GraphComponent) graphComponent: GraphComponent;
  private graphResizeObserver: ResizeObserver;

  displayedColumns: string[] = ['name', 'entityType', 'data', 'entityActions'];
  dataSource = new EntityOverviewDatasource();

  constructor(private fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private dialog: DialogService,
              private translate: TranslateService,
              ) {
    this.solutionEntity.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (this.profileMode.value !== 'table') {
        this.profileMode.setValue('table');
      } else {
        this.prepareEntityToDatasource(value[0]);
      }
    })

    this.solutionMode.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value === 'basic') {
        this.dataSource = new EntityOverviewDatasource();
        this.prepareEntityToDatasource(this.solutionEntity.value[0]);
      } else {
        this.solutionDescriptor.setValue(deepClone(this.description));
        this.solutionDescriptor.markAsPristine();
      }
    })

    this.profileMode.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value === 'table') {
        this.dataSource = new EntityOverviewDatasource();
        this.prepareEntityToDatasource(this.solutionEntity.value[0]);
      } else {
        this.prepareSchemaData();
        requestAnimationFrame(() => this.observeGraphResize());
      }
    })
  }

  ngOnChanges(): void {
    if (this.description) {
      if (this.description && this.solutionEntity.value[0] !== 'entityProfiles'
        && !this.description[this.solutionEntity.value[0]].length) {
        this.solutionEntity.setValue(['entityProfiles'], {emitEvent: false});
      }
      this.prepareEntityToDatasource(this.solutionEntity.value[0]);
      if (this.solutionDescriptor.pristine) {
        this.solutionDescriptor.setValue(deepClone(this.description));
      }
      if (this.profileMode.value !== 'table') {
        this.prepareSchemaData();
      }
    }
  }

  ngAfterContentInit() {
    requestAnimationFrame(() => {
      this.initComponent = true;
    })
  }

  ngOnDestroy(): void {
    this.graphResizeObserver?.disconnect();
  }

  observeGraphResize(): void {
    this.graphResizeObserver?.disconnect();
    if (this.graphContainer?.nativeElement) {
      this.graphResizeObserver = new ResizeObserver(() => {
        this.graphComponent?.basicUpdate();
      });
      this.graphResizeObserver.observe(this.graphContainer.nativeElement);
    }
  }

  getAlarmSeverityColor(severity: string): string {
    const preparedSeverity: any = severity.toUpperCase();
    return alarmSeverityColors.has(preparedSeverity) ? alarmSeverityColors.get(preparedSeverity) : null;
  }

  getEntityRelations(entityName: string): SolutionDescriptorEntityBaseRelation[] {
    return this.description?.relations?.filter(r => r.parent === entityName) ?? [];
  }

  openAlarmInfo(event: Event, matButton: MatIconButton, alarm: SolutionDescriptorAlarm): void {
    this.openInfoPopover(event, matButton, AlarmInfoPanelComponent, true, { alarm });
  }

  openEntityInfo(event: Event, ref: MatIconButton | HTMLElement, entity: SolutionEntity, fixedPosition = true): void {
    const relations = this.getEntityRelations(entity.name);
    this.openInfoPopover(event, ref, EntityInfoPanelComponent, fixedPosition, { entity, relations });
  }

  openMetricInfo(event: Event, matButton: MatIconButton, metric: SolutionDescriptorMetric): void {
    this.openInfoPopover(event, matButton, MetricInfoPanelComponent, true, { metric });
  }

  openIamInfo(event: Event, matButton: MatIconButton, iam: SolutionDescriptorIam): void {
    this.openInfoPopover(event, matButton, IamInfoPanelComponent, true, { iam });
  }

  deleteAlarm($event: Event, alarm: SolutionDescriptorAlarm): void {
    $event?.stopPropagation();
    this.dialog.confirm(
      this.translate.instant('solution-creator.delete-alarm-title'),
      this.translate.instant('solution-creator.delete-alarm-text'),
      null,
      this.translate.instant('action.delete')
    ).subscribe((value) => {
      if (value) {
        const updated = { ...this.description, alarms: this.description.alarms.filter(item => item !== alarm) };
        this.updatedDescription.emit(updated);
      }
    });
  }

  updatedSolutionDescriptor($event: MouseEvent) {
    $event?.stopPropagation();
    this.solutionDescriptor.markAsPristine();
    this.updatedDescription.emit(this.solutionDescriptor.value);
  }

  private openInfoPopover<T>(event: Event, ref: MatIconButton | HTMLElement, componentType: Type<T>, fixedPosition: boolean,
                             context: Record<string, any>): void {
    event?.stopPropagation();
    const trigger = ref instanceof HTMLElement ? ref : ref._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType,
        hostView: this.viewContainerRef,
        preferredPlacement: fixedPosition
          ? ['leftTopOnly', 'leftOnly', 'leftBottomOnly']
          : StrictPopoverPlacements as any,
        context,
        showCloseButton: true,
        overlayStyle: {maxHeight: '80vh', height: '100%'}
      });
    }
  }

  private prepareEntityToDatasource(type: string) {
    const findEntity = this.solutionEntities.find(entity => entity.value === type);
    this.solutionEntityTitle = findEntity?.fullTitle ?? findEntity?.title ?? '';
    switch (type) {
      case 'entityProfiles':
        this.displayedColumns = ['name', 'entityType', 'telemetry', 'entityActions'];
        this.dataSource.loadData(this.prepareEntityProfiles());
        break;
      case 'metrics':
        this.displayedColumns = ['calculatedFieldName', 'profileName', 'metricActions'];
        this.dataSource.loadData(this.description.metrics);
        break;
      case 'alarms':
        this.displayedColumns = ['alarmName', 'profileNames', 'severities', 'alarmActions'];
        this.dataSource.loadData(this.description.alarms);
        break;
      case 'iam':
        this.displayedColumns = ['name', 'permissions', 'iamActions'];
        this.dataSource.loadData(this.description.iam);
        break;
      default:
        this.dataSource.loadData([]);
    }
  }

  private prepareEntityProfiles(): SolutionDescriptorEntityBase[] {
    const data: SolutionDescriptorEntityBase[] = [];
    Object.values(this.description.entityProfiles).forEach((entities: SolutionEntity[]) => {
      entities.forEach((entity: SolutionEntity) => {
        data.push(entity);
      })
    });
    return data;
  }

  private prepareSchemaData() {
    this.links = [];
    this.nodes = [];
    this.clusters = [];

    const slug = (s: string) =>
      s.toLowerCase().normalize('NFKD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\s+/g, '-')
        .replace(/[^a-z0-9_-]/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-+|-+$/g, '');

    const cssSafeId = (base: string) => {
      const v = slug(base);
      return v && !/^[a-z_]/.test(v) ? `n-${v}` : (v || 'n');
    };

    const nodeIdOf = (type: string, name: string) =>
      `n-${cssSafeId(type)}__${cssSafeId(name)}`;

    const linkIdOf = (sourceNodeId: string, targetNodeId: string, label?: string) =>
      `e-${sourceNodeId}__to__${targetNodeId}${label ? `__${cssSafeId(label)}` : ''}`;

    const nodeByName = new Map<string, Node>();
    const nodeIdSet = new Set<string>();
    const linkIdSet = new Set<string>();
    const clusterByScope = new Map<string, ClusterNode>();
    const ensureCluster = (scope?: string | null) => {
      if (!scope) return undefined;
      if (!clusterByScope.has(scope)) {
        const id = `c-${cssSafeId(scope)}`;
        const clusterNode = { id, label: scope, childNodeIds: [] };
        clusterByScope.set(scope, clusterNode);
        this.clusters.push(clusterNode);
      }
      return clusterByScope.get(scope);
    };

    const addNode = (entity: SolutionDescriptorEntityBase) => {
      const id = nodeIdOf(entity.entityType || 'Entity', entity.name || 'Unnamed');
      if (nodeIdSet.has(id)) return id;

      const node: Node = {
        id,
        label: entity.name || 'Unnamed',
        dimension: {
          width: 200,
          height: 56
        },
        data: {
          entityType: entity.entityType || 'Entity',
          scope: entity.scope,
          entity
        }
      };

      const scope = entity.entityType === 'Customer' ? entity.name : entity.scope;
      const cluster = ensureCluster(scope);
      if (cluster) {
        cluster.childNodeIds?.push(id);
      }

      this.nodes.push(node);
      nodeIdSet.add(id);
      nodeByName.set(entity.name, node);
      return id;
    };

    const pushLink = (sourceId: string, targetId: string, label?: string) => {
      const lbl = (label || '').trim();
      const id = linkIdOf(sourceId, targetId, lbl || undefined);
      if (linkIdSet.has(id)) return;
      this.links.push({ id, source: sourceId, target: targetId, label: lbl || undefined });
      linkIdSet.add(id);
    };

    const allProfiles: SolutionDescriptorEntityBase[] = Object.values(this.prepareEntityProfiles()).flat();

    for (const entity of allProfiles) {
      addNode(entity);
    }

    for (const relation of this.description.relations) {
      const fromNode = nodeByName.get(relation.parent);
      const toNode = nodeByName.get(relation.child);
      if (fromNode && toNode) {
        const label = `${relation.type || ''}${relation.cardinality ? ` (${relation.cardinality})` : ''}`.trim();
        pushLink(fromNode.id, toNode.id, label || undefined);
      }
    }

    const byType = (t: string) =>
      allProfiles.filter(x => (x.entityType || '').toLowerCase() === t.toLowerCase());
    const tenantUsers = byType('User').filter(x => x.scope === 'Tenant');
    const customers = byType('Customer');
    const assets = byType('Asset');
    const devices = byType('Device');
    const customerUsers = byType('User').filter(x => x.scope !== 'Tenant');

    // Tenant -> Customer (manages), if both exist
    for (const t of tenantUsers) {
      const tId = nodeByName.get(t.name)?.id;
      if (!tId) continue;
      for (const c of customers) {
        if (!this.description.relations.some(x => x.child === c.name)) {
          const cId = nodeByName.get(c.name)?.id;
          if (cId) pushLink(tId, cId, 'Manages');
        }
      }
    }

    // Customer -> Asset (owns) when scope aligns
    for (const c of customers) {
      const cId = nodeByName.get(c.name)?.id;
      if (!cId) continue;
      for (const a of assets) {
        if (a.scope === c.name || a.scope === c.scope) {
          if (!this.description.relations.some(x => x.child === a.name)) {
            const aId = nodeByName.get(a.name)?.id;
            if (aId) pushLink(cId, aId, 'Own');
          }
        }
      }
    }

    // Customer -> Device (owns) when scope aligns
    for (const c of customers) {
      const cId = nodeByName.get(c.name)?.id;
      if (!cId) continue;
      for (const d of devices) {
        if (d.scope === c.name || d.scope === c.scope) {
          if (!this.description.relations.some(x => x.child === d.name)) {
            const aId = nodeByName.get(d.name)?.id;
            if (aId) pushLink(cId, aId, 'Own');
          }
        }
      }
    }

    // Customer -> User (role) when scope aligns
    for (const c of customers) {
      const cId = nodeByName.get(c.name)?.id;
      if (!cId) continue;
      for (const u of customerUsers) {
        if (u.scope === c.name || u.scope === c.scope) {
          if (!this.description.relations.some(x => x.child === u.name)) {
            const uId = nodeByName.get(u.name)?.id;
            if (uId) pushLink(cId, uId, 'Manages');
          }
        }
      }
    }
  }
}


class EntityOverviewDatasource extends TbTableDatasource<any> {
}
