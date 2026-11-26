import './polyfills.ts';
import { Auth } from '../../../backend/src/auth.mjs';
import { configFromEnv } from '../../../backend/src/config.mjs';
import { runJobs } from '../../../backend/src/jobs.mjs';
import { createFetchHandler } from '../../../backend/src/server.mjs';
import { MatchoraService } from '../../../backend/src/service.mjs';
import { SupabaseMediaStore } from '../../../backend/src/storage.mjs';
import { PostgresStore, ensureProductionRuntimeSchema } from '../../../backend/src/store.mjs';

interface RuntimeState {
  service: InstanceType<typeof MatchoraService>;
  auth: InstanceType<typeof Auth>;
  fetchHandler: (request: Request) => Promise<Response>;
}

let runtimePromise: Promise<RuntimeState> | null = null;
let lastJobRun = 0;
let jobInFlight = false;

async function getRuntime(): Promise<RuntimeState> {
  if (!runtimePromise) {
    runtimePromise = (async () => {
      const env = Deno.env.toObject();
      const config = configFromEnv(env);
      const store = await PostgresStore.open(config.databaseUrl);
      await ensureProductionRuntimeSchema(store, config);
      const media = new SupabaseMediaStore(config.supabaseUrl, config.storageServiceKey, config.storageBucket);
      const service = new MatchoraService(store, config, media);
      const auth = new Auth(store, config);
      const fetchHandler = createFetchHandler(service, auth);
      return { service, auth, fetchHandler };
    })().catch((error) => {
      runtimePromise = null;
      throw error;
    });
  }
  return await runtimePromise;
}

function maybeRunJobs(service: InstanceType<typeof MatchoraService>): void {
  const now = Date.now();
  if (jobInFlight || now - lastJobRun < 30_000) return;
  jobInFlight = true;
  lastJobRun = now;
  const promise = runJobs(service)
    .catch((error) => {
      console.error(JSON.stringify({ level: 'error', task: 'jobs', message: (error as Error)?.message ?? String(error) }));
    })
    .finally(() => {
      jobInFlight = false;
    });
  const edgeRuntime = (globalThis as Record<string, { waitUntil?: (p: Promise<unknown>) => void }>).EdgeRuntime;
  edgeRuntime?.waitUntil?.(promise);
}

Deno.serve(async (request: Request) => {
  const { service, fetchHandler } = await getRuntime();
  maybeRunJobs(service);
  return await fetchHandler(request);
});
