import { createHash } from 'node:crypto';
import { mkdtemp, readdir, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';
import { execFile as execFileCallback } from 'node:child_process';

const execFile = promisify(execFileCallback);
const bucket = process.env.FILE_S3_BUCKET ?? 'poolc-files-s3';
const profile = process.env.AWS_PROFILE ?? 'poolc';
const source = process.env.LEGACY_FILE_SOURCE ?? 'poolc:/home/ubuntu/file/';
const concurrency = Number(process.env.UPLOAD_CONCURRENCY ?? 4);

const javaUrlEncode = (value) =>
  encodeURIComponent(value)
    .replace(/[!'()~]/g, (character) => `%${character.charCodeAt(0).toString(16).toUpperCase()}`)
    .replace(/%20/g, '+');

const quoteCsv = (value) => `"${value.replaceAll('"', '""')}"`;
const fileIdFor = (name) => {
  const hash = createHash('sha256').update(name).digest('hex');
  return `${hash.slice(0, 8)}-${hash.slice(8, 12)}-${hash.slice(12, 16)}-${hash.slice(16, 20)}-${hash.slice(20, 32)}`;
};

const workDir = await mkdtemp(join(tmpdir(), 'poolc-files-s3-migration-'));
const sourceDir = join(workDir, 'source');

console.log(`Copying ${source} to ${sourceDir}`);
await execFile('rsync', ['-a', source, `${sourceDir}/`], { maxBuffer: 1024 * 1024 });

const entries = await readdir(sourceDir);
const files = [];
for (const name of entries) {
  const path = join(sourceDir, name);
  if ((await stat(path)).isFile()) {
    files.push({ name, path, size: (await stat(path)).size, id: fileIdFor(name) });
  }
}

if (files.length === 0) {
  throw new Error('No files found in the legacy source directory.');
}

console.log(`Uploading ${files.length} files to s3://${bucket}/objects/`);
let nextIndex = 0;
let completed = 0;
const uploadOne = async () => {
  while (nextIndex < files.length) {
    const file = files[nextIndex++];
    await execFile(
      'aws',
      [
        's3',
        'cp',
        file.path,
        `s3://${bucket}/objects/${file.id}`,
        '--profile',
        profile,
        '--only-show-errors',
        '--checksum-algorithm',
        'SHA256',
      ],
      { maxBuffer: 1024 * 1024 },
    );
    completed += 1;
    console.log(`${completed}/${files.length} ${file.name}`);
  }
};
await Promise.all(Array.from({ length: concurrency }, uploadOne));

const mappingPath = join(workDir, 'file-url-mapping.csv');
await writeFile(
  mappingPath,
  [
    'old_uri,new_uri',
    ...files.map(({ id, name }) => {
      const encodedName = javaUrlEncode(name);
      return `${quoteCsv(`/files/${encodedName}`)},${quoteCsv(`/files/${id}/${encodedName}`)}`;
    }),
  ].join('\n') + '\n',
  'utf8',
);

console.log(`MAPPING_CSV=${mappingPath}`);
console.log(`SOURCE_COPY=${sourceDir}`);
