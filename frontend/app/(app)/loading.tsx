import { LoadingBlock } from "@/components/ui/States";

export default function Loading() {
  return (
    <div className="grid g2">
      <LoadingBlock />
      <LoadingBlock lines={2} />
    </div>
  );
}
