/**
 * Lightweight formatting helpers. Kept dependency-free (Intl only).
 */

/** Format an ISO timestamp as "14 Aug 2026". */
export function formatDate(value, options = {}) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';

  const opts = { day: 'numeric', month: 'short', year: 'numeric', ...options };
  try {
    return new Intl.DateTimeFormat('en-GB', opts).format(date);
  } catch {
    return String(value);
  }
}

/** Format a currency value, e.g. 49 -> "$49" or 0 -> "Free". */
export function formatPrice(value, { freeLabel = 'Free' } = {}) {
  if (value === null || value === undefined) return freeLabel;
  const numeric = Number(value);
  if (Number.isNaN(numeric)) return freeLabel;
  if (numeric === 0) return freeLabel;

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: numeric % 1 === 0 ? 0 : 2,
  }).format(numeric);
}

/** Format a duration in hours, e.g. 24 -> "24 h", 1.5 -> "1.5 h". */
export function formatDuration(hours) {
  if (hours === null || hours === undefined) return '—';
  return `${Number(hours)} h`;
}

/** Format minutes into a human duration, e.g. 95 -> "1 h 35 m". */
export function formatMinutes(minutes) {
  if (minutes === null || minutes === undefined) return '—';
  const total = Math.round(Number(minutes));
  const h = Math.floor(total / 60);
  const m = total % 60;
  if (h === 0) return `${m} min`;
  if (m === 0) return `${h} h`;
  return `${h} h ${m} m`;
}

/** Format a percentage (0-100), e.g. 45.5 -> "45.5%". */
export function formatPercent(value, digits = 1) {
  if (value === null || value === undefined) return '0%';
  return `${Number(value).toFixed(digits)}%`;
}

/** Simple pluralization: pluralize(1, 'course') -> "1 course". */
export function pluralize(count, singular, plural) {
  const n = Number(count) || 0;
  const word = n === 1 ? singular : plural || `${singular}s`;
  return `${n} ${word}`;
}

export default {
  formatDate,
  formatPrice,
  formatDuration,
  formatMinutes,
  formatPercent,
  pluralize,
};
