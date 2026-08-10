const VARIANT_CLASS = {
  primary: 'badge-primary',
  success: 'badge-success',
  warning: 'badge-warning',
  danger: 'badge-danger',
  neutral: 'badge-neutral',
  outline: 'badge-outline',
};

/**
 * Small pill label used for statuses, skill levels, and categories.
 */
export default function Badge({ variant = 'neutral', className = '', children }) {
  const classes = [
    'badge',
    VARIANT_CLASS[variant] || VARIANT_CLASS.neutral,
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return <span className={classes}>{children}</span>;
}
