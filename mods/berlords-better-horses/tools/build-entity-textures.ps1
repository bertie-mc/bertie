# Original pixel layouts for the vanilla 64x64 horse UV and our saddle geometry.
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
foreach ($kind in @('passenger','warrior','wanderer')) {
    $bitmap = New-Object System.Drawing.Bitmap 64,64
    $leather = if ($kind -eq 'warrior') { '#652f30' } else { '#97633f' }
    $cloth = switch ($kind) { 'passenger' { '#2c747a' }; 'warrior' { '#853b3b' }; 'wanderer' { '#4b6543' } }
    Paint-Rect $bitmap 0 0 64 64 $leather
    # Saddle top at (9,0), front/back and sides across y=9..17.
    Paint-Rect $bitmap 0 9 38 9 $cloth
    Paint-Rect $bitmap 0 9 38 1 '#d5b47a'
    Paint-Rect $bitmap 0 17 38 1 '#c29a60'
    Paint-Rect $bitmap 9 0 10 9 '#b47c4e'
    Paint-Rect $bitmap 10 1 8 7 $leather
    foreach ($x in @(3,4,32,33)) { Paint-Rect $bitmap $x 10 1 7 '#593c2b' }
    foreach ($x in @(3,32)) { Paint-Rect $bitmap $x 14 2 2 '#d6b86e' }
    if ($kind -eq 'warrior') {
        Paint-Rect $bitmap 0 32 32 8 '#5f6369'
        Paint-Rect $bitmap 2 32 10 2 '#bec2c4'
        Paint-Rect $bitmap 0 36 24 1 '#93949a'
    } elseif ($kind -eq 'wanderer') {
        Paint-Rect $bitmap 0 32 24 16 '#795036'
        Paint-Rect $bitmap 0 39 20 1 '#c09860'
        Paint-Rect $bitmap 9 39 1 6 '#3d3026'
        Paint-Rect $bitmap 9 41 2 2 '#dbbb70'
        Paint-Rect $bitmap 32 32 30 8 '#c3af7d'
        Paint-Rect $bitmap 38 32 1 8 '#6c5039'
        Paint-Rect $bitmap 46 32 1 8 '#6c5039'
    }
    $bitmap.Save((Join-Path $assetRoot "entity/${kind}_saddle.png"))
    $bitmap.Dispose()
}
$armor = New-Object System.Drawing.Bitmap 64,64
function Armor-Panel($x,$y,$w,$h) {
    Paint-Rect $armor $x $y $w $h '#40383e'
    Paint-Rect $armor $x $y $w 1 '#9c8a91'
    if ($h -gt 2) {
        Paint-Rect $armor $x ($y+1) $w 1 '#6a5b64'
        Paint-Rect $armor $x ($y+$h-1) $w 1 '#29252a'
    }
    for ($xx=$x+3; $xx -lt ($x+$w-1); $xx+=5) { Paint-Rect $armor $xx ($y+$h-2) 1 1 '#8b7b77' }
}
# Body/croup, neck, chamfron and muzzle; no copied vanilla texture pixels.
Armor-Panel 22 32 10 22
Armor-Panel 0 54 22 9
Armor-Panel 22 54 10 9
Armor-Panel 32 54 22 9
Armor-Panel 54 54 10 9
Armor-Panel 0 42 7 11
Armor-Panel 7 42 4 11
Armor-Panel 11 42 7 11
Armor-Panel 18 42 4 11
Armor-Panel 7 13 6 7
Armor-Panel 0 20 7 5
Armor-Panel 7 20 6 5
Armor-Panel 13 20 7 5
Armor-Panel 20 20 6 5
foreach ($x in @(2,3,16,17)) { $armor.SetPixel($x,21,[System.Drawing.Color]::Transparent); $armor.SetPixel($x,22,[System.Drawing.Color]::Transparent) }
Armor-Panel 5 25 4 5
Armor-Panel 0 30 5 4
Armor-Panel 5 30 4 4
Armor-Panel 9 30 5 4
$armor.Save((Join-Path $assetRoot 'entity/horse_armor_netherite.png'))
$armor.Dispose()
$slot = New-Object System.Drawing.Bitmap 16,16
Paint-Rect $slot 3 3 2 7 '#606060'
Paint-Rect $slot 11 3 2 7 '#606060'
Paint-Rect $slot 4 10 2 2 '#606060'
Paint-Rect $slot 10 10 2 2 '#606060'
Paint-Rect $slot 5 12 6 2 '#606060'
$slot.Save((Join-Path $assetRoot 'gui/horseshoe_slot.png'))
$slot.Dispose()
