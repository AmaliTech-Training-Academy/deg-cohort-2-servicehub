import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface SseEvent {
  type: string;
  requestId: number;
  detail: string;
}

const SSE_EVENT_TYPES = ['TICKET_UPDATED', 'TICKET_ASSIGNED', 'SLA_BREACHED'] as const;

@Injectable({ providedIn: 'root' })
export class SseService {
  /**
   * Opens an EventSource to the given URL and returns an Observable that emits
   * parsed JSON payloads for all known event types. The connection is closed
   * automatically when the caller unsubscribes (ngOnDestroy). EventSource
   * reconnects on network drops without any extra handling — the teardown only
   * closes an intentional leave.
   *
   * Named SSE events (event: TICKET_UPDATED etc.) are NOT caught by onmessage —
   * each type must be registered via addEventListener, which is done here for all
   * known types so callers receive a unified stream and filter by event.type.
   */
  stream<T = SseEvent>(url: string): Observable<T> {
    return new Observable<T>(observer => {
      const source = new EventSource(url);

      const handle = (e: MessageEvent) => {
        try {
          observer.next(JSON.parse(e.data) as T);
        } catch {
          // non-JSON keep-alive frame — ignore
        }
      };

      // Register each known named event type explicitly.
      // onmessage is kept as a fallback for any unnamed frames.
      SSE_EVENT_TYPES.forEach(type => source.addEventListener(type, handle));
      source.onmessage = handle;

      source.onerror = () => {
        // EventSource auto-reconnects — do NOT call observer.error() here
        // as that would terminate the observable and prevent reconnection.
      };

      return () => {
        SSE_EVENT_TYPES.forEach(type => source.removeEventListener(type, handle));
        source.close();
      };
    });
  }
}
