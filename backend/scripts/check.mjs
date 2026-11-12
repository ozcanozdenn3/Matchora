import { readdir } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
for(const file of await readdir(new URL('../src/',import.meta.url))){if(!file.endsWith('.mjs'))continue;const result=spawnSync(process.execPath,['--check',new URL(`../src/${file}`,import.meta.url).pathname],{stdio:'inherit'});if(result.status)process.exit(result.status);}
