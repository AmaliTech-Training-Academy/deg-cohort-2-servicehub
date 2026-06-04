export const STATUS_LABELS: Record<string, string> = {
  OPEN:        'Open',
  ASSIGNED:    'Assigned',
  IN_PROGRESS: 'In progress',
  RESOLVED:    'Resolved',
  CLOSED:      'Closed',
};

export function formatStatus(status: string | undefined): string {
  if (!status) return '';
  return STATUS_LABELS[status] ?? status;
}
