/**
 * Consistent page title block used at the top of every page.
 * `actions` (optional) are rendered on the right side on wide screens.
 */
export default function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div className="container page-header-row">
        <div>
          <h1>{title}</h1>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {actions ? <div className="page-header-actions">{actions}</div> : null}
      </div>
    </div>
  );
}
