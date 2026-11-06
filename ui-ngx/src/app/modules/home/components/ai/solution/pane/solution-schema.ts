// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ClusterNode, Edge, Node } from '@swimlane/ngx-graph';
import { SolutionDescriptor, SolutionDescriptorEntityBase } from '@shared/models/solution-creator.models';

export interface SolutionSchema {
  nodes: Node[];
  links: Edge[];
  clusters: ClusterNode[];
}

export function buildSolutionSchema(descriptor: SolutionDescriptor, profiles: SolutionDescriptorEntityBase[]): SolutionSchema {
  const nodes: Node[] = [];
  const links: Edge[] = [];
  const clusters: ClusterNode[] = [];
  if (!descriptor) {
    return {nodes, links, clusters};
  }

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
      const clusterNode: ClusterNode = { id, label: scope, childNodeIds: [], data: {tone: scope === 'Tenant' ? 'tenant' : 'customer'} };
      clusterByScope.set(scope, clusterNode);
      clusters.push(clusterNode);
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

    nodes.push(node);
    nodeIdSet.add(id);
    nodeByName.set(entity.name, node);
    return id;
  };

  const pushLink = (sourceId: string, targetId: string, label?: string) => {
    const lbl = (label || '').trim();
    const id = linkIdOf(sourceId, targetId, lbl || undefined);
    if (linkIdSet.has(id)) return;
    links.push({ id, source: sourceId, target: targetId, label: lbl || undefined });
    linkIdSet.add(id);
  };

  const allProfiles: SolutionDescriptorEntityBase[] = profiles;

  for (const entity of allProfiles) {
    addNode(entity);
  }

  for (const relation of (descriptor.relations ?? [])) {
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

  for (const t of tenantUsers) {
    const tId = nodeByName.get(t.name)?.id;
    if (!tId) continue;
    for (const c of customers) {
      if (!(descriptor.relations ?? []).some(x => x.child === c.name)) {
        const cId = nodeByName.get(c.name)?.id;
        if (cId) pushLink(tId, cId, 'Manages');
      }
    }
  }

  for (const c of customers) {
    const cId = nodeByName.get(c.name)?.id;
    if (!cId) continue;
    for (const a of assets) {
      if (a.scope === c.name || a.scope === c.scope) {
        if (!(descriptor.relations ?? []).some(x => x.child === a.name)) {
          const aId = nodeByName.get(a.name)?.id;
          if (aId) pushLink(cId, aId, 'Own');
        }
      }
    }
  }

  for (const c of customers) {
    const cId = nodeByName.get(c.name)?.id;
    if (!cId) continue;
    for (const d of devices) {
      if (d.scope === c.name || d.scope === c.scope) {
        if (!(descriptor.relations ?? []).some(x => x.child === d.name)) {
          const aId = nodeByName.get(d.name)?.id;
          if (aId) pushLink(cId, aId, 'Own');
        }
      }
    }
  }

  for (const c of customers) {
    const cId = nodeByName.get(c.name)?.id;
    if (!cId) continue;
    for (const u of customerUsers) {
      if (u.scope === c.name || u.scope === c.scope) {
        if (!(descriptor.relations ?? []).some(x => x.child === u.name)) {
          const uId = nodeByName.get(u.name)?.id;
          if (uId) pushLink(cId, uId, 'Manages');
        }
      }
    }
  }
  return {nodes, links, clusters};
}
