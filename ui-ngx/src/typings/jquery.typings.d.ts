// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

interface JQuery {
  terminal(options?: any): any;
  on(events: 'tbcontextmenu', handler: (e: TbContextMenuEvent) => void): this;
}
