from pathlib import Path
from random import Random
from PIL import Image

random = Random(20260714)
size = 128
image = Image.new('RGBA', (size, size))
pixels = []

for _ in range(size * size):
    shade = random.randint(70, 185)
    alpha = random.randint(5, 16)
    pixels.append((shade, shade, shade, alpha))

image.putdata(pixels)
output = Path(__file__).resolve().parents[1] / 'src' / 'assets' / 'images' / 'noise.png'
output.parent.mkdir(parents=True, exist_ok=True)
image.save(output, optimize=True)
print(f'Generated {output}')
