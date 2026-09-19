# Original pixel layouts for the vanilla 64x64 horse hoof UV and equipment slot.
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures'
New-Item -ItemType Directory -Force -Path (Join-Path $assetRoot 'entity'),(Join-Path $assetRoot 'gui') | Out-Null
function Paint-Rect($bitmap, $x, $y, $width, $height, $color) {
    $rgba = [System.Drawing.ColorTranslator]::FromHtml($color)
    for ($yy=$y; $yy -lt ($y+$height); $yy++) { for ($xx=$x; $xx -lt ($x+$width); $xx++) { $bitmap.SetPixel($xx,$yy,$rgba) } }
}
$metals = @{
    iron=@('#6b7277','#c1c8ca','#eff4f2'); gold=@('#965f19','#efba37','#fff097');
    diamond=@('#176f72','#3acecb','#b6fff2'); netherite=@('#30292b','#55474e','#8b7b86')
}
foreach ($kind in $metals.Keys) {
    $bitmap = New-Object System.Drawing.Bitmap 64,64
    $colors = $metals[$kind]
    # All four legs share the 4x11x4 UV. Back face is x=60..63.
    Paint-Rect $bitmap 48 35 16 1 $colors[1]
    foreach ($x in @(48,52,56,60,63)) { Paint-Rect $bitmap $x 35 1 1 $colors[0] }
    Paint-Rect $bitmap 53 35 2 1 $colors[2]
    $bitmap.SetPixel(61,35,[System.Drawing.Color]::Transparent)
    $bitmap.SetPixel(62,35,[System.Drawing.Color]::Transparent)
    # Sole: open U, including the same two-pixel rear gap.
    Paint-Rect $bitmap 56 21 4 1 $colors[1]
    Paint-Rect $bitmap 56 21 1 4 $colors[0]
    Paint-Rect $bitmap 59 21 1 4 $colors[1]
    $bitmap.Save((Join-Path $assetRoot "entity/horseshoes_$kind.png"))
    $bitmap.Dispose()
}
# The shared generated saddle atlas is maintained separately; see saddle-materials.md.
# The horse armor atlas keeps the upstream layout; the item has a two-pixel muzzle correction (NOTICE).
$slot = New-Object System.Drawing.Bitmap 16,16
# Flat outline and four nail pixels, matching vanilla's empty saddle/armor icon color.
$slotRows = @(
    '................',
    '..OOO......OOO..',
    '.O...O....O...O.',
    '.O...O....O...O.',
    '.O.N.O....O.N.O.',
    '.O...O....O...O.',
    '.O...O....O...O.',
    '.O.N.O....O.N.O.',
    '.O...O....O...O.',
    '.O...O....O...O.',
    '.O....O..O....O.',
    '..O....OO....O..',
    '..O..........O..',
    '...O........O...',
    '....OOOOOOOO....',
    '................'
)
$slotPalette = @{ O='#7c7c7c'; N='#7c7c7c' }
for ($y=0; $y -lt 16; $y++) {
    for ($x=0; $x -lt 16; $x++) {
        $key=[string]$slotRows[$y][$x]
        if ($key -ne '.') { Paint-Rect $slot $x $y 1 1 $slotPalette[$key] }
    }
}
$slot.Save((Join-Path $assetRoot 'gui/horseshoe_slot.png'))
$slot.Dispose()
