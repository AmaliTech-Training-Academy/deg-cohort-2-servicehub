import { TestBed } from '@angular/core/testing';
import { SlaTagComponent } from './sla-tag';

function future(minutes: number): string {
  return new Date(Date.now() + minutes * 60_000).toISOString();
}
function past(minutes: number): string {
  return new Date(Date.now() - minutes * 60_000).toISOString();
}

describe('SlaTagComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SlaTagComponent],
    }).compileComponents();
  });

  function render(isOverdue: boolean, slaDeadline?: string) {
    const fixture = TestBed.createComponent(SlaTagComponent);
    fixture.componentRef.setInput('isOverdue', isOverdue);
    if (slaDeadline !== undefined) fixture.componentRef.setInput('slaDeadline', slaDeadline);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows "—" when no deadline and not overdue', () => {
    const el = render(false, undefined);
    expect(el.textContent?.trim()).toBe('—');
    expect(el.querySelector('.sla')).toBeNull();
  });

  it('shows "Overdue" in breach state when isOverdue=true and no deadline', () => {
    const el = render(true, undefined);
    const span = el.querySelector('.sla')!;
    expect(span).not.toBeNull();
    expect(span.classList.contains('breach')).toBe(true);
    expect(span.textContent?.trim()).toBe('Overdue');
  });

  it('shows overdue duration when isOverdue=true with a past deadline', () => {
    const el = render(true, past(90));
    const span = el.querySelector('.sla')!;
    expect(span).not.toBeNull();
    expect(span.classList.contains('breach')).toBe(true);
    expect(span.textContent).toContain('overdue');
  });

  it('shows met state (teal) when more than 60 min remain', () => {
    const el = render(false, future(120));
    const span = el.querySelector('.sla')!;
    expect(span).not.toBeNull();
    expect(span.classList.contains('met')).toBe(true);
    expect(span.textContent).toContain('left');
  });

  it('shows ok state (blue) when less than 60 min remain', () => {
    const el = render(false, future(30));
    const span = el.querySelector('.sla')!;
    expect(span).not.toBeNull();
    expect(span.classList.contains('ok')).toBe(true);
    expect(span.textContent).toContain('m left');
  });

  it('shows breach state when deadline is in the past and not flagged overdue', () => {
    const el = render(false, past(10));
    const span = el.querySelector('.sla')!;
    expect(span).not.toBeNull();
    expect(span.classList.contains('breach')).toBe(true);
  });
});
