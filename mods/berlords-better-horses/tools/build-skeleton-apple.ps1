# Skeleton Apple: native 16x16 pixels.
# Broad diagonal ribs, connected shadows and dark contours follow ImageGen v6.
# The eight finer contour pixels from v6 were removed manually; the stem stays vanilla.
# References and prompts: apple-style-revision.md, apple-prompts-v6.json.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$rows = @(
    '................',
    '.........A......',
    '........BC......',
    '........A.......',
    '.....DDACDD.....',
    '...DGGGCGGGED...',
    '..DGD..GE..DED..',
    '..DGGD.GE.DGED..',
    '..DGEGEGEEGEHD..',
    '..DG..DGED..ED..',
    '..DGD..GE..GED..',
    '...DGGEGEGEHD...',
    '...DG..GE..ED...',
    '....DGGGEEHD....',
    '.....DDDDDD.....',
    '................'
)
$palette = @{
    A='#752802'; B='#7e370e'; C='#542409'
    D='#443c2e'; E='#b8ae92'; G='#ede4c8'; H='#817760'
}
$bitmap=[Drawing.Bitmap]::new(16,16)
try {
    if ($rows.Count -ne 16) { throw 'Expected 16 base rows' }
    for ($y=0;$y -lt 16;$y++) {
        if ($rows[$y].Length -ne 16) { throw "Row $y must have 16 pixels" }
        for ($x=0;$x -lt 16;$x++) {
            $key=[string]$rows[$y][$x]
            if ($key -eq '.') { continue }
            $color=[Drawing.ColorTranslator]::FromHtml($palette[$key])
            $bitmap.SetPixel($x,$y,$color)
        }
    }
    $target=Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures/item/skeleton_apple.png'
    $bitmap.Save($target,[Drawing.Imaging.ImageFormat]::Png)
} finally { $bitmap.Dispose() }
