# Native 16x16 cleanup of the built-in ImageGen apple concepts (apple-prompts.json).
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures/item'
$rows = @(
    '................',
    '........OO......',
    '........OTO.GG..',
    '.........OGGHG..',
    '....OOO..OOO....',
    '...OLHLOOLLMO...',
    '..OLHHLLLLMMMO..',
    '.OLHHLLLLLLMMMO.',
    '.OLHLLLLLLLMMMO.',
    '.OLLLLLLLLLMMMO.',
    '..OLLLLLLLMMMO..',
    '..OMLLLLLMMMMO..',
    '...OMLLMMMMMO...',
    '....OMMMMMMO....',
    '.....OOMMOO.....',
    '.......OO.......'
)
$palettes = @{
    zombie = @{ O='#343523'; T='#73472b'; G='#567633'; H='#bdc866'; L='#899342'; M='#5c652d' }
    skeleton = @{ O='#66615a'; T='#73472b'; G='#567633'; H='#fffbed'; L='#e1decb'; M='#aaa596' }
    breed = @{ O='#40304b'; T='#73472b'; G='#567633'; H='#ffe4b0'; L='#ef5c32'; M='#b72e36' }
}
$quadrants = @(
    @{ H='#ffe4b0'; L='#ef5c32'; M='#b72e36' },
    @{ H='#fff59b'; L='#f2d03b'; M='#c89423' },
    @{ H='#9bf1ef'; L='#31c4d4'; M='#247a99' },
    @{ H='#d6adff'; L='#a65fe2'; M='#663798' }
)
function Pixel($bitmap, $x, $y, $hex) { $bitmap.SetPixel($x,$y,[Drawing.ColorTranslator]::FromHtml($hex)) }
foreach ($kind in @('zombie','skeleton','breed')) {
    $bitmap=[Drawing.Bitmap]::new(16,16)
    try {
        for ($y=0;$y -lt 16;$y++) {
            if ($rows[$y].Length -ne 16) { throw "Apple row $y must have 16 pixels" }
            for ($x=0;$x -lt 16;$x++) {
                $key=[string]$rows[$y][$x]
                if ($key -eq '.') { continue }
                $hex=$palettes[$kind][$key]
                if ($y -le 3 -and $key -eq 'H') { $hex='#8db44c' }
                if ($kind -eq 'breed' -and $y -ge 4 -and $key -in @('H','L','M')) {
                    $quadrant=[int]($x -ge 8) + 2*[int]($y -ge 10)
                    $hex=$quadrants[$quadrant][$key]
                }
                Pixel $bitmap $x $y $hex
            }
        }
        if ($kind -eq 'zombie') {
            foreach($point in @(@(4,8),@(5,8),@(4,9),@(8,12),@(9,12))) { Pixel $bitmap $point[0] $point[1] '#4b3526' }
            foreach($point in @(@(13,8),@(14,8),@(13,9),@(14,9))) { $bitmap.SetPixel($point[0],$point[1],[Drawing.Color]::Transparent) }
            Pixel $bitmap 12 8 '#c7b78a'
            Pixel $bitmap 12 9 '#c7b78a'
        }
        if ($kind -eq 'skeleton') {
            foreach($point in @(@(5,8),@(6,8),@(5,9),@(6,9),@(10,8),@(11,8),@(10,9),@(11,9),@(8,10),@(8,11))) {
                Pixel $bitmap $point[0] $point[1] '#39383a'
            }
        }
        $bitmap.Save((Join-Path $assetRoot "${kind}_apple.png"),[Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}
