import { inject, Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { firstValueFrom } from 'rxjs';
import SockJS from 'sockjs-client';

export type MessageType = 'TEXT' | 'FILE' | 'SYSTEM' | 'SIGNALING';

const useSockJS = true;
const httpWsPath = '/ws-chat'; // proxy.conf.json -> target: chat-service:8082

export interface ChatMessageResponse {
  messageId: string;
  sessionId: string;
  sender: string;
  type: MessageType;
  content: string;
  roomId: string;
  createdAt: string; // ISO
}

export interface CreateChatMessageRequest {
  sessionId: string;
  sender: string;
  type: MessageType;
  content: string;
  roomId: string;
}

export interface RtcSignal {
  type: 'offer' | 'answer' | 'candidate' | 'bye';
  sdp?: string | null;
  candidate?: RTCIceCandidateInit | null;
  fromSessionId: string;
  toSessionId: string;
  roomId?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatWs {
  private http = inject(HttpClient);
  private zone = inject(NgZone);

  private client?: Client;
  private roomSub?: StompSubscription;
  private typingSub?: StompSubscription;
  private rtcInboxSub?: StompSubscription;

  get connected(): boolean {
    return !!this.client?.connected;
  }

  private buildStompUrl(): string {
    const origin = window.location.origin; // http://localhost:4200
    const base = useSockJS ? origin : origin.replace(/^http/, 'ws');
    return `${base}${httpWsPath}`;        // http://localhost:4200/ws-chat (proxy)
  }

  ensureConnected(
    sessionId: string,
    opts: {
      onConnect?: () => void;
      onDisconnect?: () => void;
      onError?: (e: any) => void;
      connectHeaders?: Record<string, string>;
      reconnectDelay?: number;
    } = {}
  ): void {
    if (this.client?.active) return;

    const stompUrl = this.buildStompUrl();

    const client = new Client({
      brokerURL: useSockJS ? undefined : stompUrl,
      webSocketFactory: useSockJS ? () => new SockJS(stompUrl) : undefined,
      reconnectDelay: opts.reconnectDelay ?? 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: { 'x-session-id': sessionId, ...(opts.connectHeaders ?? {}) },
      onConnect: () => this.zone.run(() => opts.onConnect?.()),
      onStompError: (f) => this.zone.run(() => opts.onError?.(f)),
      onWebSocketClose: () => this.zone.run(() => opts.onDisconnect?.()),
    });

    this.client = client;
    client.activate();
  }

  disconnect(): void {
    try { this.roomSub?.unsubscribe(); } catch {}
    try { this.typingSub?.unsubscribe(); } catch {}
    try { this.rtcInboxSub?.unsubscribe(); } catch {}
    this.roomSub = this.typingSub = this.rtcInboxSub = undefined;

    if (this.client?.active) this.client.deactivate();
  }

  subscribeRoom(roomId: string, onMessage: (msg: ChatMessageResponse) => void): void {
    if (!this.client?.connected) throw new Error('STOMP not connected');

    this.roomSub?.unsubscribe();
    this.roomSub = this.client.subscribe(`/topic/rooms.${roomId}.messages`, (frame: IMessage) => {
      try {
        const msg = JSON.parse(frame.body) as ChatMessageResponse;
        this.zone.run(() => onMessage(msg));
      } catch {
        console.warn('Bad message body', frame.body);
      }
    });
  }

  subscribeTyping(roomId: string, onTyping: (ev: any) => void): void {
    if (!this.client?.connected) throw new Error('STOMP not connected');

    this.typingSub?.unsubscribe();
    this.typingSub = this.client.subscribe(`/topic/rooms.${roomId}.typing`, (frame: IMessage) => {
      try {
        const payload = JSON.parse(frame.body);
        this.zone.run(() => onTyping(payload));
      } catch {
        console.warn('Bad typing payload', frame.body);
      }
    });
  }

  subscribeRtcInbox(onSignal: (sig: RtcSignal) => void): void {
    if (!this.client?.connected) throw new Error('STOMP not connected');

    this.rtcInboxSub?.unsubscribe();
    this.rtcInboxSub = this.client.subscribe(`/user/queue/rtc`, (frame: IMessage) => {
      try {
        const payload = JSON.parse(frame.body) as RtcSignal;
        this.zone.run(() => onSignal(payload));
      } catch {
        console.warn('Bad rtc payload', frame.body);
      }
    });
  }

  publish(destination: string, body: string = '', headers: Record<string, string> = {}): void {
    if (!this.client?.connected) return;
    this.client.publish({ destination, body, headers });
  }

  sendMessage(roomId: string, dto: CreateChatMessageRequest): void {
    if (!this.client?.connected) throw new Error('STOMP not connected');
    this.client.publish({
      destination: `/app/rooms/${roomId}/message`,
      body: JSON.stringify(dto),
      headers: { 'content-type': 'application/json' },
    });
  }

  /** Typing helper → backend ma @MessageMapping("/rooms/{roomId}/typing") z @Payload boolean */
  sendTyping(roomId: string, typing: boolean): void {
    // wysyłamy "true"/"false" jako JSON (zgodne z @Payload boolean)
    this.publish(`/app/rooms/${roomId}/typing`, JSON.stringify(typing), { 'content-type': 'application/json' });
  }

  ping(): void {
    this.publish('/app/ping', 'hello');
  }

  async loadHistory(apiBase: string, roomId: string): Promise<ChatMessageResponse[]> {
    const items = await firstValueFrom(
      this.http.get<ChatMessageResponse[]>(`${apiBase}/api/chat/${encodeURIComponent(roomId)}/recent`)
    );
    return [...(items ?? [])].reverse();
  }
}
