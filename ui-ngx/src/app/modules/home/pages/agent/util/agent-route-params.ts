// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { resolveGroupParams } from '@shared/models/entity-group.models';

// Walk the activated route's ancestor chain to find the `agentId` param.
// Angular's default `paramsInheritanceStrategy` is `emptyOnly`, so nested
// routes with their own path segments do not auto-inherit params from
// ancestors like `:agentId/applications`.
export function resolveAgentIdParam(route: ActivatedRouteSnapshot): string | undefined {
  let current: ActivatedRouteSnapshot | null = route;
  while (current) {
    const id = current.params?.agentId;
    if (id) {
      return id;
    }
    current = current.parent;
  }
  return undefined;
}

// Build the absolute URL to an agent within its current entity-group scope,
// preserving the tenant/customer prefix and all / groups/:gid / shared/:gid scope
// — mirroring the edge feature's navigateToChildEdgePage (edge-group-config.factory).
// Scope is derived from the canonical resolveGroupParams() rather than parsing the
// URL, so it stays correct under the customer hierarchy too.
// `tail` are trailing segments, e.g. 'applications', appId, 'units', unitId, 'logs'.
export function agentEntityUrl(route: ActivatedRouteSnapshot, agentId: string, ...tail: Array<string>): string {
  const params = resolveGroupParams(route);
  const groups = params?.shared ? 'shared' : 'groups';
  let url: string;
  if (params?.customerId) {
    if (params.childEntityGroupId) {
      url = `customers/${groups}/${params.entityGroupId}/${params.customerId}` +
        `/edgeManagement/agents/groups/${params.childEntityGroupId}/${agentId}`;
    } else {
      const scope = params.entityGroupId ? `groups/${params.entityGroupId}` : 'all';
      url = `customers/all/${params.customerId}/edgeManagement/agents/${scope}/${agentId}`;
    }
  } else if (params?.entityGroupId) {
    url = `edgeManagement/agents/${groups}/${params.entityGroupId}/${agentId}`;
  } else {
    url = `edgeManagement/agents/all/${agentId}`;
  }
  return '/' + [url, ...tail].join('/');
}

// Deepest activated route snapshot — for callers (dialogs, table configs) that
// only have a Router and need the scope of the page currently displayed.
export function currentAgentRouteSnapshot(router: Router): ActivatedRouteSnapshot {
  let snapshot = router.routerState.snapshot.root;
  while (snapshot.firstChild) {
    snapshot = snapshot.firstChild;
  }
  return snapshot;
}
