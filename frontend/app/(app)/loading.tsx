import { LoadingBlock } from "@/components/ui/States";

export default function Loading() {
  return (
    <div className="grid g2">
      <LoadingBlock label="A carregar…" />
      <LoadingBlock label="A carregar…" lines={2} />
    </div>
  );
}
