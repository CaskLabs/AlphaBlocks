#!/usr/bin/env python3
"""Original procedural app icon; only the Python standard library is needed."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parent.parent
BLOCKS = [(1, 0, (88, 234, 210)), (0, 1, (88, 234, 210)),
          (2, 1, (88, 234, 210)), (0, 2, (88, 234, 210)),
          (1, 2, (181, 154, 255)), (2, 2, (181, 154, 255)),
          (0, 3, (181, 154, 255)), (2, 3, (181, 154, 255))]

def rounded(x, y, x0, y0, w, h, r):
    dx = max(x0 + r - x, 0, x - (x0 + w - r))
    dy = max(y0 + r - y, 0, y - (y0 + h - r))
    return x0 <= x <= x0+w and y0 <= y <= y0+h and dx*dx+dy*dy <= r*r

def pixel(x, y):
    if not rounded(x, y, 8, 8, 240, 240, 48):
        return (0, 0, 0, 0)
    color = (int(16+8*y/256), int(25+12*y/256), int(43+15*y/256), 255)
    for col, row, rgb in BLOCKS:
        bx, by = 65+col*43, 45+row*43
        if rounded(x, y, bx, by, 38, 38, 7):
            color = rgb+(255,)
            if by+4 < y < by+7 and bx+7 < x < bx+31:
                color = tuple(min(255, v+35) for v in rgb)+(255,)
    return color

def save(path, size):
    data = bytearray()
    for y in range(size):
        data.append(0)
        for x in range(size):
            samples = [pixel((x+(sx+.5)/2)*256/size, (y+(sy+.5)/2)*256/size)
                       for sy in range(2) for sx in range(2)]
            data.extend(sum(s[c] for s in samples)//4 for c in range(4))
    def chunk(kind, payload):
        return struct.pack('!I', len(payload))+kind+payload+struct.pack('!I', zlib.crc32(kind+payload)&0xffffffff)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR', struct.pack('!2I5B', size, size, 8, 6, 0, 0, 0))
                     +chunk(b'IDAT', zlib.compress(bytes(data), 9))+chunk(b'IEND', b''))

save(ROOT/'app/src/main/res/drawable-nodpi/ic_launcher.png', 128)
save(ROOT/'docs/icon.png', 512)
