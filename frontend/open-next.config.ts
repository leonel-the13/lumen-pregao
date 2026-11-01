// Adaptador OpenNext para Cloudflare Workers. Todas as páginas são dinâmicas (pedidos no-store ao backend),
// por isso não há cache incremental (sem R2).
import { defineCloudflareConfig } from "@opennextjs/cloudflare";

export default defineCloudflareConfig({});
