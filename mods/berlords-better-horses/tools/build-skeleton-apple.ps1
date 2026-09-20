# Final 32x32 Skeleton Apple, cleaned from built-in ImageGen v4.
# Coherent upper-left lighting: connected pale upper surfaces, beige side planes,
# and darker lower/right edges. No independent patches, dirt, or block averaging.
# References and prompt: apple-style-revision.md, skeleton-apple-prompt-v4.json.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$rows = @(
    '................................',
    '................................',
    '..................AA............',
    '..................AA............',
    '................AABC............',
    '................AABC............',
    '................BB..............',
    '................BB..............',
    '..........DEEEAACCDEED..........',
    '.........FDDEEBBCCDEDDF.........',
    '......EEEDF.FDEEEEDF.DDDEDF.....',
    '......EEEE...FDEEEF...DEEDF.....',
    '....EEDEEEF...FDED...FEEEDDD....',
    '....EEFDDEEF...DDF..FEEEDDED....',
    '....EEDFDDEEEEEEEDEEEEDDDFED....',
    '....EED..FFDDDEEEDDDDFF..DED....',
    '....EDEF...FFFEEEDFFF...FEED....',
    '....EDEEF.....DEEF.....FEEDD....',
    '....DFDEEED....EEF...FEEEDFF....',
    '....FDFDDEEF...EEF..FEEDDFFF....',
    '.....ED.DDEEEEEEEDDEEEDFFFD.....',
    '.....EEF.FFDDDEEEDFDDFF..ED.....',
    '......EEF..FFFDEEDFFF..FEE......',
    '......EEDF.....EEF....FEED......',
    '......FDEEF...EEEF...FEEEF......',
    '.......FDEEF..EEEF..FEEDD.......',
    '........FDEEEEEEDDFEEEFFF.......',
    '.........FDEEEEEDDFDDDF.........',
    '..........FDDDEEDFFDFF..........',
    '............DDDDFFFF............',
    '................................',
    '................................'
)
$palette = @{
    A='#884a24'
    B='#6b371b'
    C='#492b13'
    D='#b8ae92'
    E='#ede4c8'
    F='#817760'
}
$bitmap=[Drawing.Bitmap]::new(32,32)
try {
    if ($rows.Count -ne 32) { throw 'Expected 32 rows' }
    for ($y=0;$y -lt 32;$y++) {
        if ($rows[$y].Length -ne 32) { throw "Row $y must have 32 pixels" }
        for ($x=0;$x -lt 32;$x++) {
            $key=[string]$rows[$y][$x]
            if ($key -eq '.') { continue }
            $bitmap.SetPixel($x,$y,[Drawing.ColorTranslator]::FromHtml($palette[$key]))
        }
    }
    $target=Join-Path $PSScriptRoot '../src/main/resources/assets/betterhorses/textures/item/skeleton_apple.png'
    $bitmap.Save($target,[Drawing.Imaging.ImageFormat]::Png)
} finally { $bitmap.Dispose() }
