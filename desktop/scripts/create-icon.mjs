import fs from 'node:fs';
import path from 'node:path';

const output = path.resolve('assets', 'icon.ico');
const sizes = [16, 24, 32, 48, 64, 128, 256];
const colors = {
  bg: [247, 251, 255, 255],
  blue: [47, 95, 206, 255],
  amber: [255, 184, 77, 255]
};

const images = sizes.map((size) => createDib(size));
const headerSize = 6 + sizes.length * 16;
let offset = headerSize;

const header = Buffer.alloc(headerSize);
header.writeUInt16LE(0, 0);
header.writeUInt16LE(1, 2);
header.writeUInt16LE(sizes.length, 4);

for (let i = 0; i < sizes.length; i += 1) {
  const size = sizes[i];
  const entryOffset = 6 + i * 16;
  header.writeUInt8(size === 256 ? 0 : size, entryOffset);
  header.writeUInt8(size === 256 ? 0 : size, entryOffset + 1);
  header.writeUInt8(0, entryOffset + 2);
  header.writeUInt8(0, entryOffset + 3);
  header.writeUInt16LE(1, entryOffset + 4);
  header.writeUInt16LE(32, entryOffset + 6);
  header.writeUInt32LE(images[i].length, entryOffset + 8);
  header.writeUInt32LE(offset, entryOffset + 12);
  offset += images[i].length;
}

fs.writeFileSync(output, Buffer.concat([header, ...images]));
console.log(`Created ${output}`);

function createDib(size) {
  const pixels = new Uint8Array(size * size * 4);

  fillRoundedRect(pixels, size, 0, 0, size, size, size * 0.22, colors.bg);
  strokeRect(pixels, size, size * 0.24, size * 0.24, size * 0.48, size * 0.58, size * 0.05, colors.blue);
  strokeRect(pixels, size, size * 0.35, size * 0.16, size * 0.28, size * 0.16, size * 0.05, colors.blue);

  drawLine(pixels, size, size * 0.34, size * 0.43, size * 0.62, size * 0.43, size * 0.045, colors.blue);
  drawLine(pixels, size, size * 0.34, size * 0.55, size * 0.54, size * 0.55, size * 0.045, colors.amber);
  drawLine(pixels, size, size * 0.34, size * 0.67, size * 0.61, size * 0.67, size * 0.045, colors.blue);

  drawLine(pixels, size, size * 0.50, size * 0.61, size * 0.74, size * 0.61, size * 0.05, colors.amber);
  drawLine(pixels, size, size * 0.60, size * 0.50, size * 0.74, size * 0.61, size * 0.05, colors.amber);
  drawLine(pixels, size, size * 0.74, size * 0.61, size * 0.60, size * 0.72, size * 0.05, colors.amber);

  const xorSize = size * size * 4;
  const maskStride = Math.ceil(size / 32) * 4;
  const maskSize = maskStride * size;
  const dib = Buffer.alloc(40 + xorSize + maskSize);

  dib.writeUInt32LE(40, 0);
  dib.writeInt32LE(size, 4);
  dib.writeInt32LE(size * 2, 8);
  dib.writeUInt16LE(1, 12);
  dib.writeUInt16LE(32, 14);
  dib.writeUInt32LE(0, 16);
  dib.writeUInt32LE(xorSize, 20);
  dib.writeInt32LE(0, 24);
  dib.writeInt32LE(0, 28);
  dib.writeUInt32LE(0, 32);
  dib.writeUInt32LE(0, 36);

  let out = 40;
  for (let y = size - 1; y >= 0; y -= 1) {
    for (let x = 0; x < size; x += 1) {
      const input = (y * size + x) * 4;
      dib[out++] = pixels[input + 2];
      dib[out++] = pixels[input + 1];
      dib[out++] = pixels[input];
      dib[out++] = pixels[input + 3];
    }
  }

  return dib;
}

function fillRoundedRect(pixels, size, x, y, width, height, radius, color) {
  const minX = Math.floor(x);
  const maxX = Math.ceil(x + width);
  const minY = Math.floor(y);
  const maxY = Math.ceil(y + height);

  for (let py = minY; py < maxY; py += 1) {
    for (let px = minX; px < maxX; px += 1) {
      if (insideRoundedRect(px + 0.5, py + 0.5, x, y, width, height, radius)) {
        setPixel(pixels, size, px, py, color);
      }
    }
  }
}

function strokeRect(pixels, size, x, y, width, height, stroke, color) {
  drawLine(pixels, size, x, y, x + width, y, stroke, color);
  drawLine(pixels, size, x + width, y, x + width, y + height, stroke, color);
  drawLine(pixels, size, x + width, y + height, x, y + height, stroke, color);
  drawLine(pixels, size, x, y + height, x, y, stroke, color);
}

function drawLine(pixels, size, x1, y1, x2, y2, width, color) {
  const minX = Math.max(0, Math.floor(Math.min(x1, x2) - width));
  const maxX = Math.min(size - 1, Math.ceil(Math.max(x1, x2) + width));
  const minY = Math.max(0, Math.floor(Math.min(y1, y2) - width));
  const maxY = Math.min(size - 1, Math.ceil(Math.max(y1, y2) + width));
  const radius = width / 2;

  for (let y = minY; y <= maxY; y += 1) {
    for (let x = minX; x <= maxX; x += 1) {
      if (distanceToSegment(x + 0.5, y + 0.5, x1, y1, x2, y2) <= radius) {
        setPixel(pixels, size, x, y, color);
      }
    }
  }
}

function insideRoundedRect(px, py, x, y, width, height, radius) {
  const cx = Math.max(x + radius, Math.min(px, x + width - radius));
  const cy = Math.max(y + radius, Math.min(py, y + height - radius));
  return (px - cx) ** 2 + (py - cy) ** 2 <= radius ** 2;
}

function distanceToSegment(px, py, x1, y1, x2, y2) {
  const dx = x2 - x1;
  const dy = y2 - y1;
  const lengthSq = dx * dx + dy * dy;
  const t = lengthSq === 0 ? 0 : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSq));
  const x = x1 + t * dx;
  const y = y1 + t * dy;
  return Math.hypot(px - x, py - y);
}

function setPixel(pixels, size, x, y, color) {
  if (x < 0 || x >= size || y < 0 || y >= size) {
    return;
  }
  const offset = (y * size + x) * 4;
  pixels[offset] = color[0];
  pixels[offset + 1] = color[1];
  pixels[offset + 2] = color[2];
  pixels[offset + 3] = color[3];
}
