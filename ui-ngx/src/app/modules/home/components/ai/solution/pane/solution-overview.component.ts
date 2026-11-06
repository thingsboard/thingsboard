// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  computed,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  signal,
  ViewChild
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ClusterNode, Edge, GraphComponent, Node } from '@swimlane/ngx-graph';
import { DialogService } from '@core/services/dialog.service';
import {
  SolutionDescriptor,
  SolutionDescriptorAlarm,
  SolutionDescriptorEntityBase,
  SolutionDescriptorEntityBaseRelation,
  SolutionDescriptorIam
} from '@shared/models/solution-creator.models';
import { aiEntityKind } from '@home/components/ai/ai-action.models';
import { AlarmSeverity, alarmSeverityColors, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { buildSolutionSchema } from '@home/components/ai/solution/pane/solution-schema';

type SectionId = 'profiles' | 'roles' | 'metrics' | 'alarms';

interface Section {
  id: SectionId;
  title: string;
}

const OPERATIONS: Record<string, string> = {C: 'create', R: 'read', U: 'update', D: 'delete'};

@Component({
  selector: 'tb-solution-overview',
  templateUrl: './solution-overview.component.html',
  styleUrls: ['./solution-pane.scss', './solution-overview.component.scss'],
  standalone: false
})
export class SolutionOverviewComponent implements OnChanges, AfterViewInit, OnDestroy {

  @Input({required: true}) descriptor: SolutionDescriptor;

  @Output() descriptorChange = new EventEmitter<SolutionDescriptor>();

  @ViewChild('graphContainer') graphContainer: ElementRef<HTMLElement>;
  @ViewChild(GraphComponent) graphComponent: GraphComponent;

  private translate = inject(TranslateService);
  private dialog = inject(DialogService);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  mode = signal<'list' | 'schema'>('list');
  open = signal<string | null>(null);
  keysOf = signal<'telemetry' | 'relations'>('telemetry');
  active = signal<SectionId>('profiles');
  wide = signal(false);

  profiles: SolutionDescriptorEntityBase[] = [];
  nodes: Node[] = [];
  links: Edge[] = [];
  clusters: ClusterNode[] = [];

  sections = computed<Section[]>(() => [
    {id: 'profiles', title: 'solution-creator.pane.profiles'},
    {id: 'roles', title: 'solution-creator.pane.roles'},
    {id: 'metrics', title: 'solution-creator.pane.calculated-fields'},
    {id: 'alarms', title: 'solution-creator.pane.alarms'}
  ]);

  private resize: ResizeObserver;
  private scroller: HTMLElement | null = null;
  private scrollListener = () => this.scroller && this.onScroll(this.scroller);
  private graphResize: ResizeObserver;

  ngOnChanges(): void {
    this.profiles = this.descriptor ? Object.values(this.descriptor.entityProfiles ?? {}).flat() as SolutionDescriptorEntityBase[] : [];
    if (this.mode() === 'schema') {
      this.buildSchema();
    }
  }

  ngAfterViewInit(): void {
    this.resize = new ResizeObserver(() => this.wide.set(this.host.nativeElement.clientWidth >= 660));
    this.resize.observe(this.host.nativeElement);
    this.scroller = this.host.nativeElement.closest('.sol-pane-body') as HTMLElement;
    this.scroller?.addEventListener('scroll', this.scrollListener, {passive: true});
  }

  ngOnDestroy(): void {
    this.resize?.disconnect();
    this.graphResize?.disconnect();
    this.scroller?.removeEventListener('scroll', this.scrollListener);
  }

  severityColor(severity: string): string {
    return alarmSeverityColors.get(severity?.toUpperCase() as AlarmSeverity) ?? null;
  }

  severityLabel(severity: string): string {
    return alarmSeverityTranslations.get(severity?.toUpperCase() as AlarmSeverity) ?? severity;
  }

  setMode(mode: 'list' | 'schema'): void {
    this.mode.set(mode);
    if (mode === 'schema') {
      this.buildSchema();
      requestAnimationFrame(() => this.observeGraph());
    }
  }

  toggle(key: string): void {
    this.open.set(this.open() === key ? null : key);
    this.keysOf.set('telemetry');
  }

  goTo(section: SectionId): void {
    this.active.set(section);
    const target = this.host.nativeElement.querySelector(`[data-sec="${section}"]`) as HTMLElement;
    const scroller = this.host.nativeElement.closest('.sol-pane-body') as HTMLElement;
    if (target && scroller) {
      const tabs = this.wide() ? 0 : (this.host.nativeElement.querySelector('.sol-tabs') as HTMLElement)?.offsetHeight ?? 0;
      scroller.scrollTo({top: scroller.scrollTop + target.getBoundingClientRect().top - scroller.getBoundingClientRect().top - tabs, behavior: 'smooth'});
    }
  }

  onScroll(scroller: HTMLElement): void {
    const top = scroller.getBoundingClientRect().top + (this.wide() ? 8 : 60);
    let current: SectionId = 'profiles';
    for (const section of this.sections()) {
      const el = this.host.nativeElement.querySelector(`[data-sec="${section.id}"]`) as HTMLElement;
      if (el && el.getBoundingClientRect().top <= top) {
        current = section.id;
      }
    }
    if (current !== this.active()) {
      this.active.set(current);
    }
  }

  kind(entity: SolutionDescriptorEntityBase): {icon: string; palette: string} {
    return aiEntityKind((entity.entityType ?? '').toUpperCase());
  }

  typeLabel(entity: SolutionDescriptorEntityBase): string {
    const type = (entity.entityType ?? '').toLowerCase();
    return type ? type.charAt(0).toUpperCase() + type.slice(1) : '';
  }

  relationsOf(entity: SolutionDescriptorEntityBase): SolutionDescriptorEntityBaseRelation[] {
    return (this.descriptor?.relations ?? []).filter(relation => relation.parent === entity.name || relation.child === entity.name);
  }

  dataOf(entity: SolutionDescriptorEntityBase): string {
    const keys = entity.telemetry?.length ?? 0;
    const relations = this.relationsOf(entity).length;
    const parts = [];
    if (keys) {
      parts.push(this.translate.instant('solution-creator.pane.keys', {count: keys}));
    }
    if (relations) {
      parts.push(this.translate.instant('solution-creator.pane.relations', {count: relations}));
    }
    return parts.join(' · ') || '—';
  }

  operations(iam: SolutionDescriptorIam): string[] {
    return (iam.permissions ?? '').toUpperCase().split('').filter(letter => OPERATIONS[letter]).map(letter => OPERATIONS[letter]);
  }

  alarmKey(alarm: SolutionDescriptorAlarm): string {
    return 'alarm:' + alarm.alarmName;
  }

  removeAlarm(alarm: SolutionDescriptorAlarm): void {
    this.dialog.confirm(
      this.translate.instant('solution-creator.delete-alarm-title'),
      this.translate.instant('solution-creator.delete-alarm-text'),
      null,
      this.translate.instant('action.delete')
    ).subscribe(confirmed => {
      if (confirmed) {
        this.open.set(null);
        this.descriptorChange.emit({...this.descriptor, alarms: this.descriptor.alarms.filter(item => item !== alarm)});
      }
    });
  }

  openFromSchema(node: Node): void {
    const entity = node.data?.entity as SolutionDescriptorEntityBase;
    if (entity) {
      this.open.set(this.open() === 'schema:' + entity.name ? null : 'schema:' + entity.name);
    }
  }

  schemaEntity(): SolutionDescriptorEntityBase | undefined {
    const key = this.open();
    return key?.startsWith('schema:') ? this.profiles.find(profile => 'schema:' + profile.name === key) : undefined;
  }

  private buildSchema(): void {
    const schema = buildSolutionSchema(this.descriptor, this.profiles);
    this.nodes = schema.nodes;
    this.links = schema.links;
    this.clusters = schema.clusters;
  }

  private observeGraph(): void {
    this.graphResize?.disconnect();
    if (this.graphContainer?.nativeElement) {
      this.graphResize = new ResizeObserver(() => this.graphComponent?.basicUpdate());
      this.graphResize.observe(this.graphContainer.nativeElement);
    }
  }
}
