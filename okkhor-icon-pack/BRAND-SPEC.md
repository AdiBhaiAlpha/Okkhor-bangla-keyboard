# Okkhor (অক্ষর) — Launcher Icon Brand Specification

## Concept: "Matra Keys"
A compact keyboard pictogram: three keys and a spacebar, with a saffron bar above them.
The bar is the **matra** — the headline stroke that Bangla letters hang from. It is the
only Bengali element, kept abstract so the symbol reads as a keyboard first.
Nothing in the artwork is a font glyph; everything is rounded rectangles.

### Concepts evaluated
| Concept | Keyboard recognition @48px | Mono (themed) | Verdict |
|---|---|---|---|
| A  Compact Keys (3 keys + space) | Strong | Strong | Generic; no Bangla identity |
| **B  Matra Keys** | Strong | Strong | **Chosen** — keyboard-first, ownable via the matra bar |
| C  Caret Keycap (text I-beam) | Weak — reads as a text field | Fails — caret disappears | Rejected |
| D  Key-O Ring (O monogram + space glyph) | Weak — reads as a tray/box | Weak | Rejected |

Refinement of B: bar thickened and gap tightened so the bar survives at 48px, then the
whole symbol was scaled to touch the 66dp Android safe circle exactly.

## Palette (3 colors)
| Role | Name | HEX |
|---|---|---|
| Background | Nil Indigo | `#2B2A8C` |
| Accent (matra bar) | Saffron | `#FFB61E` |
| Keys | White | `#FFFFFF` |

Reasoning: indigo (*nil*) is deeply tied to Bengal's history and reads as trustworthy and
technical; it is a deep violet-leaning indigo, clearly apart from Gboard's multi-hue
scheme. Saffron makes the one Bangla element the focal accent. White keys give maximum
legibility.
Measured contrast on indigo: white 11.6:1, saffron 6.6:1. (Saffron on white is only 1.8:1,
so never place the saffron bar directly on white — use the light-background symbol.)

## Geometry (Android adaptive grid, 108 × 108 dp)
- Symbol width 54.8 dp, height 42.0 dp, centered at (54, 54).
- Matra bar 7.6 dp high, full width, pill-ended.
- Key row: 3 keys, each about 15.7 × 15.6 dp, 3.8 dp gaps, corner radius about 4.3 dp.
- Bar-to-keys gap 2.8 dp. Spacebar 12.3 dp high, full width, same radius as keys.
- Farthest artwork point is 33.0 dp from center: it fits the 66 dp safe circle (visible
  mask area is 72 dp). See `okkhor-safe-zone.png`.
- Foreground has no baked container; background is a flat `#2B2A8C` square.

## Files
- `okkhor-icon.svg` — full-bleed master (also 512 PNG for Play Store; stores apply their own mask)
- `okkhor-foreground.svg`, `okkhor-background.svg`, `okkhor-monochrome.svg` — adaptive layers (108-dp grid)
- `okkhor-symbol.svg` / `.png` — transparent symbol for dark or colored surfaces
- `okkhor-symbol-light-bg.svg` / `.png` — single-color indigo for light surfaces
- `okkhor-icon-small.svg` — chunkier variant for 24–48 px contexts
- `android/res/` — drop-in resources (see below)

## Usage rules
- Minimum size: 24 px (use the small variant below 48 px).
- Keep clear space equal to one key width around the symbol.
- Do not recolor the bar, reorder keys, add text, or add shadows, glows or gradients.
- On white or light backgrounds use `okkhor-symbol-light-bg`, never the white-key symbol.
- Monochrome: single tint, all elements the same color; gaps carry the structure.

## Android integration
1. Merge `android/res/` into `app/src/main/res/`. Files are uniquely named
   (`ic_launcher*`); if your project already has these names, back them up first.
2. In `AndroidManifest.xml` `<application>` set:
   `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.
3. Adaptive icons apply on API 26+; the `<monochrome>` layer is used on API 33+ and ignored
   on older versions. Legacy PNGs (mdpi–xxxhdpi) cover API < 26.
4. For Google Play, upload `okkhor-icon-512.png`.

## Notes
- No Android project was available in this workspace, so nothing was integrated or
  overwritten; the resources are ready to merge.
- The small-size variant is only a modest improvement over the main icon at 32 px (heavier
  bar, larger gaps); the main icon already holds up at 48 px.
- All artwork is plain vector geometry with no external fonts or images.
