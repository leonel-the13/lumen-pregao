export function Avatar({ initials, size }: { initials: string; size?: number }) {
  return <span className="avatar" aria-hidden="true" style={size ? { width: size, height: size } : undefined}>{initials}</span>;
}
