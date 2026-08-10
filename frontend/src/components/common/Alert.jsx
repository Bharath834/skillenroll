const VARIANT_CLASS = {
  info: 'alert-info',
  success: 'alert-success',
  warning: 'alert-warning',
  danger: 'alert-danger',
};

/**
 * Inline message banner for notices and errors.
 */
export default function Alert({ variant = 'info', role, className = '', children }) {
  const classes = ['alert', VARIANT_CLASS[variant] || VARIANT_CLASS.info, className]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={classes} role={role || (variant === 'danger' ? 'alert' : 'status')}>
      {children}
    </div>
  );
}
