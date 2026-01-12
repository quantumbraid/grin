import { GrinFile } from "./grin-file";

export class GrinLoader {
  static async fromURL(url: string): Promise<GrinFile> {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to fetch ${url}: ${response.status}`);
    }
    if (response.body) {
      const buffer = await readStream(response.body);
      return GrinFile.load(buffer);
    }
    const buffer = await response.arrayBuffer();
    return GrinFile.load(buffer);
  }

  static async fromFile(file: File): Promise<GrinFile> {
    const buffer = await file.arrayBuffer();
    return GrinFile.load(buffer);
  }

  static async fromBlob(blob: Blob): Promise<GrinFile> {
    const buffer = await blob.arrayBuffer();
    return GrinFile.load(buffer);
  }
}

async function readStream(stream: ReadableStream<Uint8Array>): Promise<ArrayBuffer> {
  const reader = stream.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    if (value) {
      chunks.push(value);
      total += value.length;
    }
  }

  const output = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    output.set(chunk, offset);
    offset += chunk.length;
  }
  return output.buffer;
}
