import { TestBed } from '@angular/core/testing';
import { SseService, SseEvent } from './sse.service';
import { vi } from 'vitest';

/* Minimal EventSource stub */
class MockEventSource {
  static instances: MockEventSource[] = [];
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  onmessage: ((e: MessageEvent) => void) | null = null;
  onerror:   ((e: Event) => void) | null = null;
  closed = false;

  constructor(public url: string) { MockEventSource.instances.push(this); }

  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    (this.listeners[type] ??= []).push(fn);
  }
  removeEventListener(type: string, fn: (e: MessageEvent) => void) {
    this.listeners[type] = (this.listeners[type] ?? []).filter(f => f !== fn);
  }
  close() { this.closed = true; }

  emit(type: string, data: unknown) {
    const event = { data: JSON.stringify(data) } as MessageEvent;
    (this.listeners[type] ?? []).forEach(fn => fn(event));
    if (type === 'message') this.onmessage?.(event);
  }
}

describe('SseService', () => {
  let service: SseService;

  beforeEach(() => {
    MockEventSource.instances = [];
    vi.stubGlobal('EventSource', MockEventSource);
    TestBed.configureTestingModule({ providers: [SseService] });
    service = TestBed.inject(SseService);
  });

  afterEach(() => vi.unstubAllGlobals());

  it('opens an EventSource to the given URL on subscribe', () => {
    service.stream('http://test/stream').subscribe();
    expect(MockEventSource.instances).toHaveLength(1);
    expect(MockEventSource.instances[0].url).toBe('http://test/stream');
  });

  it('emits parsed JSON for named TICKET_UPDATED events', () => {
    const received: SseEvent[] = [];
    service.stream<SseEvent>('http://test/stream').subscribe(e => received.push(e));
    const src = MockEventSource.instances[0];
    src.emit('TICKET_UPDATED', { type: 'TICKET_UPDATED', requestId: 5, detail: 'ASSIGNED' });
    expect(received).toHaveLength(1);
    expect(received[0]).toEqual({ type: 'TICKET_UPDATED', requestId: 5, detail: 'ASSIGNED' });
  });

  it('emits for TICKET_ASSIGNED events', () => {
    const received: SseEvent[] = [];
    service.stream<SseEvent>('http://test/stream').subscribe(e => received.push(e));
    MockEventSource.instances[0].emit('TICKET_ASSIGNED', { type: 'TICKET_ASSIGNED', requestId: 2, detail: 'ASSIGNED' });
    expect(received).toHaveLength(1);
  });

  it('emits for SLA_BREACHED events', () => {
    const received: SseEvent[] = [];
    service.stream<SseEvent>('http://test/stream').subscribe(e => received.push(e));
    MockEventSource.instances[0].emit('SLA_BREACHED', { type: 'SLA_BREACHED', requestId: 7, detail: 'HIGH' });
    expect(received).toHaveLength(1);
  });

  it('closes the EventSource when unsubscribed', () => {
    const sub = service.stream('http://test/stream').subscribe();
    const src = MockEventSource.instances[0];
    expect(src.closed).toBe(false);
    sub.unsubscribe();
    expect(src.closed).toBe(true);
  });

  it('silently ignores non-JSON frames', () => {
    const received: unknown[] = [];
    service.stream('http://test/stream').subscribe(e => received.push(e));
    const src = MockEventSource.instances[0];
    const badEvent = { data: 'not-json' } as MessageEvent;
    src.onmessage?.(badEvent);
    expect(received).toHaveLength(0);
  });
});
