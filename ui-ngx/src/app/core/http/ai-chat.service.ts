// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { defaultHttpOptions, defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { EMPTY, from, Observable } from 'rxjs';
import { HttpClient, HttpEventType } from '@angular/common/http';
import {
  ApprovalResult,
  ChatConfiguration,
  ChatEvent,
  ChatInfo,
  SendChatMessageRequest,
  ToolApprovalResult
} from '@shared/models/ai-chat.models';
import { filter, mergeMap } from 'rxjs/operators';
import { ChatMessage } from '@shared/models/solution-creator.models';

@Injectable({
  providedIn: 'root'
})
export class AiChatService {

  constructor(
    private http: HttpClient
  ) {
  }

  public listChats(config?: RequestConfig): Observable<Array<ChatInfo>> {
    return this.http.get<Array<ChatInfo>>(`/api/ai/chats`, defaultHttpOptionsFromConfig(config))
  }

  public createChat(chat: ChatConfiguration, config?: RequestConfig): Observable<string> {
    return this.http.post<string>(`/api/ai/chats`, chat, defaultHttpOptionsFromConfig(config));
  }

  public updatedChat(chatId: string, chat: ChatConfiguration, config?: RequestConfig): Observable<void> {
    return this.http.patch<void>(`/api/ai/chats/${chatId}`, chat, defaultHttpOptionsFromConfig(config));
  }

  public deleteChat(chatId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/ai/chats/${chatId}`, defaultHttpOptionsFromConfig(config));
  }

  public getChatMessages(chatId: string, config?: RequestConfig): Observable<Array<ChatMessage>> {
    return this.http.get<Array<ChatMessage>>(`/api/ai/chats/${chatId}/messages`, defaultHttpOptionsFromConfig(config))
  }

  public generateDashboard(deviceId: string, timeseriesKeys?: string[], config?: RequestConfig): Observable<string> {
    return this.http.post<string>(`/api/ai/devices/${deviceId}/dashboard`, { timeseriesKeys }, defaultHttpOptionsFromConfig(config));
  }

  public resolveToolApproval(result: ApprovalResult, config?: RequestConfig): Observable<ToolApprovalResult> {
    return this.http.post<ToolApprovalResult>('/api/ai/tools/resolve-approval', result, defaultHttpOptionsFromConfig(config))
  }

  public sendChatMessage(chatId: string, request: SendChatMessageRequest, config?: RequestConfig): Observable<ChatEvent> {
    let lastLength = 0;
    let buffer = '';
    return this.http.post(`/api/ai/chats/${chatId}/messages`, request, {
      ...defaultHttpOptions(config?.ignoreLoading, config?.ignoreErrors),
      observe: 'events',
      reportProgress: true,
      responseType: 'text'
    }).pipe(
      filter(event => (event.type === HttpEventType.DownloadProgress)),
      mergeMap(event => {
        const fullText = event.partialText ?? '';
        const chunk = fullText.substring(lastLength);
        lastLength = fullText.length;
        buffer += chunk;

        const lastBoundary = buffer.lastIndexOf('\n\n');
        if (lastBoundary === -1) {
          return EMPTY;
        }

        const complete = buffer.substring(0, lastBoundary + 2);
        buffer = buffer.substring(lastBoundary + 2);
        return from(this.parseEventStrings(complete));
      })
    );
  }

  private parseEventStrings(input: string): ChatEvent[] {
    const blocks = input.trim().split(/\n\n+/);
    const events: ChatEvent[] = [];
    for (const block of blocks) {
      if (!block.trim()) continue;
      const event = this.parseSingleEvent(block);
      if (event != null) {
        events.push(event);
      }
    }
    return events;
  }

  private parseSingleEvent(input: string): ChatEvent | null {
    const result: Partial<ChatEvent> = {};

    const lines = input.trim().split('\n');

    for (const line of lines) {
      const trimmedLine = line.trim();

      if (!trimmedLine || trimmedLine.startsWith(':')) continue;

      if (trimmedLine.startsWith('event:')) {
        result.event = trimmedLine.substring(6).trim() as any;
      } else if (trimmedLine.startsWith('data:')) {
        const jsonString = trimmedLine.substring(5).trim();
        try {
          result.data = JSON.parse(jsonString);
        } catch (e) {/**/}
      }
    }

    if (!result.event || !result.data) {
      try {
        return JSON.parse(input);
      } catch (e) {
        return input?.trim() ? input as any : null;
      }
    }
    return result as ChatEvent;
  }
}
