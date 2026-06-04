import { TestBed } from '@angular/core/testing';
import { PriorityChipComponent } from './priority-chip';

describe('PriorityChipComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriorityChipComponent],
    }).compileComponents();
  });

  function render(priority: string) {
    const fixture = TestBed.createComponent(PriorityChipComponent);
    fixture.componentInstance.priority = priority;
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('renders a span with the priority text', () => {
    const el = render('HIGH');
    const span = el.querySelector('span')!;
    expect(span.textContent?.trim()).toBe('HIGH');
  });

  it.each(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'])('applies class p-%s for priority', (p) => {
    const el = render(p);
    expect(el.querySelector('.chip')?.classList.contains(`p-${p}`)).toBe(true);
  });
});
