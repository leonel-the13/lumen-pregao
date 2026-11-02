import Link from "next/link";

export function Brand({ href = "/" }: { href?: string }) {
  return (
    <Link className="brand" href={href} aria-label="Lúmen Lab — início">
      <i className="dot" aria-hidden="true" />
      <b>LÚMEN</b>
      <span>Lab</span>
    </Link>
  );
}
