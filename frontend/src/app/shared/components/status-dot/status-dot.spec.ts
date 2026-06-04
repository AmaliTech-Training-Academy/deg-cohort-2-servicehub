import { TestBed } from '@angular/core/testing';
import { StatusDotComponent } from './status-dot';

describe('StatusDotComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusDotComponent],
    }).compileComponents();
  });

  function render(status: string) {
    const fixture = TestBed.createComponent(StatusDotComponent);
    fixture.componentInstance.status = status;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it.each([
    ['OPEN',        'Open'],
    ['ASSIGNED',    'Assigned'],
    ['IN_PROGRESS', 'In progress'],
    ['RESOLVED',    'Resolved'],
    ['CLOSED',      'Closed'],
  ])('displays "%s" label for status %s', (status, label) => {
    const el = render(status);
    expect(el.querySelector('span')?.textContent?.trim()).toBe(label);
  });

  it.each(['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'])(
    'applies st-%s CSS class for %s', (status) => {
      const el = render(status);
      expect(el.querySelector('span')?.classList.contains(`st-${status}`)).toBe(true);
    }
  );

  it('falls back to the raw status string for unknown values', () => {
    const el = render('UNKNOWN_STATUS');
    expect(el.querySelector('span')?.textContent?.trim()).toBe('UNKNOWN_STATUS');
  });
});
