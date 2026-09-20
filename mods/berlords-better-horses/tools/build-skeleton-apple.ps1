# Final 32x32 Skeleton Apple, cleaned from built-in ImageGen v3.
# Most opaque pixels form aligned, identical 2x2 blocks. Finer pixels shape the
# curved rib gaps, worn edges, and a few crevice marks; no smoothing or partial alpha.
# References and prompt: apple-style-revision.md, skeleton-apple-prompt-v3.json.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$rows = @(
    '................................',
    '................................',
    '..................AA............',
    '..................AA............',
    '................AABB............',
    '................AABB............',
    '................CC..............',
    '................CC..............',
    '..........DDEECCBBEEDD..........',
    '.........FDDEECCBBEEDDF.F.......',
    '......DDEED.FGHHIIJF.HJJHH......',
    '......DDEE...DHHIIF..FJJHH......',
    '....DDIIIIF...FJGG....EEIIDD....',
    '....DDIIIIIF...HD...FDEEIIDD....',
    '....EEFFFDIIDDGGIIEEGGDFFFEE....',
    '....EEF..FIIDDGGIIEEGGF..FEE....',
    '....GGDD....FFGGGGFF....JJGG....',
    '....GGDDF.....GGGG.....FJJGG....',
    '....IIJJEEF....IGG....GGDDJJ....',
    '....IIJJEEJE..FGGG..IIGGDDJJ....',
    '....JGF.JDIIDDEEDHDDEEDF.FED....',
    '.....FIE..IIDDEEHDDDEE..FIG.....',
    '......EEF.....FGDD.....FII......',
    '......EEFF.....DDD....FFII......',
    '......DDGGF...HHHH...FGGHJ......',
    '.......FGGID..HHHH..DIGGF.......',
    '........FHGGGGEEGGJJGGDF........',
    '.........FGGGGEEDGJJGGF.........',
    '..........FDHHHHDDHHDF..........',
    '............HHHHDDHH............',
    '................................',
    '................................'
)
$palette = @{
    A='#85451e'
    B='#492b13'
    C='#693215'
    D='#827c65'
    E='#d9d0b4'
    F='#38352a'
    G='#beb69e'
    H='#a19a84'
    I='#eee5c8'
    J='#5e5a47'
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
