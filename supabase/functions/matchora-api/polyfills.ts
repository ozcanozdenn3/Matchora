import { Buffer } from 'node:buffer';
import process from 'node:process';

(globalThis as Record<string, unknown>).Buffer ??= Buffer;
(globalThis as Record<string, unknown>).process ??= process;
