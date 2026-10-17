"""Usage: python scripts/make_fonts.py <folder with inter_variable.ttf and bricolage_grotesque_variable.ttf> <output folder>
(needs `pip install fonttools brotli`; the variable fonts are the upstream releases, SIL OFL: see licenses/fonts/)

Static, Latin-only instances of the two variable fonts of the app (Inter, Bricolage Grotesque), one file per weight used.

The variable fonts carry every script and every weight (1.25 MB); Android only needs a few weights, in Latin. Each instance is
pinned to the axis values the font family XML files used to ask for (wght, opsz), then subset to Latin + the punctuation the app uses.
"""
import os
import sys

from fontTools.subset import Options, Subsetter
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FONT_DIR = sys.argv[1]
OUT_DIR = sys.argv[2]
os.makedirs(OUT_DIR, exist_ok=True)

# Latin-1 and Latin Extended-A (French, and most European names a user may give to an issue), the spacing modifiers used by the
# accents, general punctuation (’ “ ” – … and the narrow no-break space of French), currency, arrows, minus, vertical ellipsis.
UNICODES = (
    list(range(0x20, 0x180)) + list(range(0x2C6, 0x2DE)) + list(range(0x2000, 0x2070)) + list(range(0x20A0, 0x20C1))
    + [0x2122, 0x2212, 0x22EE, 0xFFFD] + list(range(0x2190, 0x2200))
)

INSTANCES = [
    # source, output, axis values
    ('inter_variable.ttf', 'inter_w400.ttf', {'wght': 400, 'opsz': 14}),
    ('inter_variable.ttf', 'inter_w500.ttf', {'wght': 500, 'opsz': 14}),
    ('inter_variable.ttf', 'inter_w600.ttf', {'wght': 600, 'opsz': 14}),
    ('inter_variable.ttf', 'inter_w700.ttf', {'wght': 700, 'opsz': 14}),
    ('bricolage_grotesque_variable.ttf', 'bricolage_w400.ttf', {'wght': 400, 'opsz': 24, 'wdth': 100}),
    ('bricolage_grotesque_variable.ttf', 'bricolage_w700.ttf', {'wght': 700, 'opsz': 24, 'wdth': 100}),
    ('bricolage_grotesque_variable.ttf', 'bricolage_w800.ttf', {'wght': 800, 'opsz': 48, 'wdth': 100}),
]

for src, out, axes in INSTANCES:
    font = TTFont(os.path.join(FONT_DIR, src))
    static = instantiateVariableFont(font, axes, updateFontNames=True)
    options = Options()
    # Default OpenType features (kerning, ligatures, marks...): the stylistic sets and character variants are left out.
    options.name_IDs = ['*']
    options.notdef_outline = True
    options.glyph_names = False
    options.hinting = False          # Android barely uses TrueType hinting
    subsetter = Subsetter(options)
    subsetter.populate(unicodes=UNICODES)
    subsetter.subset(static)
    path = os.path.join(OUT_DIR, out)
    static.save(path)
    print('%-20s %6.1f KB  %d glyphs' % (out, os.path.getsize(path) / 1024, static['maxp'].numGlyphs))
