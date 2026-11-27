// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { MenuId, MenuSection } from '@core/services/menu.models';
import { AiChatsService } from '@core/services/ai-chats.service';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-menu-link',
    templateUrl: './menu-link.component.html',
    styleUrls: [],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MenuLinkComponent implements OnInit {

  @Input() section: MenuSection;

  @Input()
  @coerceBoolean()
  collapsed = false;

  readonly active = signal(false);

  private chats = inject(AiChatsService);

  private countsChats = signal(false);

  readonly needYou = computed(() => this.countsChats()
    ? this.chats.statusChats().filter(chat => this.chats.needsAttention(chat)).length : 0);

  ngOnInit(): void {
    if (this.section?.id === MenuId.ai_assistant) {
      this.countsChats.set(true);
      this.chats.watchStatus();
    }
  }

}
