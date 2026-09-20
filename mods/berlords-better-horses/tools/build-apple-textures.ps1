# Native 16x16 zombie/breed cleanup, plus the 32x32 skeleton detail pass.
# Vanilla 1.21.1 silhouette reference: apple-style-revision.md.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures/item'
$rows = @(
    '................',
    '.........A......',
    '........BC......',
    '........A.......',
    '.....DDACAA.....',
    '...DEFECDGEAA...',
    '..DEHIFFFIHEAA..',
    '..DEHHHHHHGHEA..',
    '..DEGHGHGGGHGJ..',
    '..AEEGGEEGEHGJ..',
    '..AEEEEEGEEHEJ..',
    '...AEEGEEEGEJ...',
    '...ADEEEGGEDJ...',
    '....JDEDDEDJ....',
    '.....JAAAJJ.....',
    '................'
)
$zombie = @{
    A='#353d22'; B='#7e370e'; C='#542409'; D='#505b2c'; E='#748038'
    F='#d3d77e'; G='#919d46'; H='#adb85c'; I='#c0c96c'; J='#303820'
}
$stem = @{ A='#752802'; B='#7e370e'; C='#542409' }
$spiral = @(
    @{ Edge='#902834'; Base='#ee4d46'; Light='#ffa993' },
    @{ Edge='#a46b23'; Base='#f2ca3d'; Light='#ffec96' },
    @{ Edge='#503070'; Base='#a261d4'; Light='#d6a7ff' },
    @{ Edge='#246276'; Base='#35bfd1'; Light='#a0e8ec' }
)
function Is-ApplePixel([int]$x,[int]$y) {
    return $x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16 -and $rows[$y][$x] -ne '.'
}
function Is-StemPixel([int]$x,[int]$y) {
    return $y -lt 4 -or ($y -eq 4 -and $x -in @(7,8)) -or ($y -eq 5 -and $x -eq 7)
}
function Pixel($bitmap, $x, $y, $hex) {
    $bitmap.SetPixel($x,$y,[Drawing.ColorTranslator]::FromHtml($hex))
}
foreach ($kind in @('zombie','breed')) {
    $bitmap=[Drawing.Bitmap]::new(16,16)
    try {
        for ($y=0;$y -lt 16;$y++) {
            if ($rows[$y].Length -ne 16) { throw "Apple row $y must have 16 pixels" }
            for ($x=0;$x -lt 16;$x++) {
                $key=[string]$rows[$y][$x]
                if ($key -eq '.') { continue }
                if (Is-StemPixel $x $y) {
                    Pixel $bitmap $x $y $stem[$key]
                    continue
                }
                $edge=!(Is-ApplePixel ($x-1) $y) -or !(Is-ApplePixel ($x+1) $y) -or
                      !(Is-ApplePixel $x ($y-1)) -or !(Is-ApplePixel $x ($y+1))
                if ($kind -eq 'zombie') {
                    $hex=$zombie[$key]
                } else {
                    $dx=$x-7.5; $dy=$y-9
                    $radius=[Math]::Sqrt($dx*$dx+$dy*$dy)
                    $angle=[Math]::Atan2($dy,$dx)-$radius*0.58+0.5
                    $turn=2*[Math]::PI
                    $phase=(($angle % $turn)+$turn) % $turn
                    $band=[int][Math]::Floor($phase/([Math]::PI/2))
                    $tone='Base'
                    if ($edge) { $tone='Edge' }
                    elseif (($x -eq 5 -and $y -in @(5,6)) -or ($y -eq 6 -and $x -in @(6,7))) { $tone='Light' }
                    $hex=$spiral[$band][$tone]
                }
                Pixel $bitmap $x $y $hex
            }
        }
        if ($kind -eq 'zombie') {
            foreach($point in @(@(5,8),@(4,9),@(10,11),@(9,12))) {
                Pixel $bitmap $point[0] $point[1] '#59432b'
            }
        }
        $bitmap.Save((Join-Path $assetRoot ($kind+'_apple.png')),[Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}
& (Join-Path $PSScriptRoot 'build-skeleton-apple.ps1')
