/**
 * Friendly empty/placeholder state with icon, heading, description,
 * and an optional call-to-action rendered via `children`.
 */
export default function EmptyState({ icon, title, description, children, role }) {
  return (
    <div className="empty-state" role={role}>
      {icon ? <div className="empty-state-icon">{icon}</div> : null}
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
      {children}
    </div>
  );
}
