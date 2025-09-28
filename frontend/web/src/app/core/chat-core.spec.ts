import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { ChatWs, RtcSignal } from './chat-core';

class NoopNgZone extends NgZone {
  constructor() {
    super({ enableLongStackTrace: false });
  }

  override run<T>(fn: (...args: any[]) => T): T {
    return fn();
  }
}

describe('ChatWs', () => {
  let service: ChatWs;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{ provide: NgZone, useClass: NoopNgZone }],
    });
    service = TestBed.inject(ChatWs);
  });

  it('forwards parsed RTC signals to subscribers', () => {
    let capturedCallback: ((frame: IMessage) => void) | undefined;
    let capturedDestination: string | undefined;

    (service as any).client = {
      connected: true,
      subscribe: (_destination: string, cb: (frame: IMessage) => void) => {
        capturedDestination = _destination;
        capturedCallback = cb;
        return { unsubscribe() {} } as StompSubscription;
      },
    } as unknown as Client;

    const handler = jasmine.createSpy('handler');
    service.subscribeRtcInbox(handler);

    expect(capturedCallback).toBeDefined();
    expect(capturedDestination).toBe('/user/queue/rtc');

    const signal: RtcSignal = {
      type: 'offer',
      sdp: 'v=0',
      fromSessionId: 'alice',
      toSessionId: 'bob',
      roomId: 'room-1',
    };

    capturedCallback!({ body: JSON.stringify(signal) } as IMessage);

    expect(handler).toHaveBeenCalledWith(jasmine.objectContaining(signal));
  });
});
