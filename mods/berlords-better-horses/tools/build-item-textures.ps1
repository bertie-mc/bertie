# Native-grid cleanup of the ImageGen concepts recorded in art-prompts.json.
# Each character is exactly one pixel. Never downsample these finished sprites.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures/item'
function Save-Sprite($name, [string[]]$rows, $palette) {
    if ($rows.Count -ne 16) { throw "$name needs 16 rows" }
    $bitmap = New-Object System.Drawing.Bitmap 16,16
    try {
        for ($y=0; $y -lt 16; $y++) {
            if ($rows[$y].Length -ne 16) { throw "$name row $y needs 16 pixels: $($rows[$y])" }
            for ($x=0; $x -lt 16; $x++) {
                $key = [string]$rows[$y][$x]
                if ($key -ne '.') {
                    if (-not $palette.ContainsKey($key)) { throw "Unknown color $key in $name" }
                    $bitmap.SetPixel($x,$y,[System.Drawing.ColorTranslator]::FromHtml($palette[$key]))
                }
            }
        }
        $bitmap.Save((Join-Path $assetRoot "$name.png"),[System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}

# The four materials share every silhouette, highlight and nail pixel.
# Straight arms occupy x=1..5 and x=10..14; nails are at (3,4/7), (12,4/7).
$shoe = @(
    '................',
    '..OOO......OOO..',
    '.OHHHO....OHHHO.',
    '.OLLMO....OLLMO.',
    '.OLNMO....OLNMO.',
    '.OLLMO....OLLMO.',
    '.OLLMO....OLLMO.',
    '.OLNMO....OLNMO.',
    '.OLLMO....OLLMO.',
    '.OLLMO....OLLMO.',
    '.OMLLHO..OHLLMO.',
    '..OMLLHOOHLLMO..',
    '..OOMLLLLLMMOO..',
    '...OOMMMMMMOO...',
    '....OOOOOOOO....',
    '................'
)
$materials = @{
    iron = @{ O='#4d545b'; H='#f1f5f3'; L='#c8cfd0'; M='#929ca2'; N='#252a30' }
    gold = @{ O='#85501c'; H='#fff4a0'; L='#f5c63e'; M='#c78b24'; N='#51301b' }
    diamond = @{ O='#14666d'; H='#baffed'; L='#39d9cb'; M='#239da9'; N='#083b49' }
    netherite = @{ O='#30272d'; H='#ab969d'; L='#78666f'; M='#574951'; N='#160f17' }
}
foreach ($material in $materials.Keys) { Save-Sprite "${material}_horseshoes" $shoe $materials[$material] }

# Big connected leather/cloth areas, one-pixel straps and open stirrups.
Save-Sprite 'passenger_saddle' @(
    '................',
    '................',
    '..OO.......OOO..',
    '.OHHLO.OO.OHHLO.',
    '.OLLMOOLLOLLLMO.',
    '..OLLMLLLMLLMO..',
    '.OCOMMLLMMLMCO..',
    '.OCCTOOOOOOTCCO.',
    '..OTTCMTTCMTTTO.',
    '...OTCMTTCMTTO..',
    '....OOMOOOMO....',
    '......M...M.....',
    '.....GGO.GGO....',
    '.....G.O.G.O....',
    '.....GGG.GGG....',
    '................'
) @{ O='#42281f'; H='#e9b477'; L='#ba7b47'; M='#86502e'; C='#56c4c2'; T='#267e89'; G='#e4bc5a' }

# Keep the right-hand stirrup facing the viewer; two identify the passenger saddle.
Save-Sprite 'warrior_saddle' @(
    '................',
    '..OO............',
    '.OHIO......OOO..',
    '..ISMO....OLLO..',
    '..OSLIO..OLLSIO.',
    '.OIOMLIOOLMMSIO.',
    '.ORIMMLLLMMIIO..',
    '.ORRIIOMMIIORRO.',
    '..ORRIMMMIRRRO..',
    '...ORMMMOMRRO...',
    '....OOOO.MOO....',
    '.........M......',
    '.........HIH....',
    '.........I.O....',
    '.........III....',
    '................'
) @{ O='#321e24'; H='#f0eded'; I='#a5aab2'; S='#656875'; L='#b94a51'; M='#73333f'; R='#992c39' }

Save-Sprite 'wanderer_saddle' @(
    '................',
    '..........OOOO..',
    '..OO.....OKMKHO.',
    '.OHLO....OKMKMO.',
    '..OLMO..OLLLMO..',
    '..OLLMOOLLLMO...',
    '.OCOMLLLLLMCOO..',
    '.OCCTOMMMMOLHLO.',
    '..OCTCMOCCOLLMO.',
    '...OTCMOTCOMGMO.',
    '....OTMTO.OLLMO.',
    '.....OMO..OMMMO.',
    '.....GGG...OOO..',
    '.....G.O........',
    '.....GGG........',
    '................'
) @{ O='#392c25'; H='#dba56b'; L='#a56d41'; M='#714b30'; C='#769258'; T='#3f6242'; K='#dfcca0'; G='#d5c78a' }
