# Final 32x32 Skeleton Apple, cleaned from built-in ImageGen v5.
# Dark contours follow the outer bone silhouette and every transparent rib gap.
# The vanilla stem is reproduced exactly as uniform 2x2 pixel blocks. Finer pixels
# are reserved for bone curves and contours; internal directional shading is retained.
# References and prompt: apple-style-revision.md, skeleton-apple-prompt-v5.json.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$rows = @(
    '................................',
    '................................',
    '..................AA............',
    '..................AA............',
    '................BBCC............',
    '................BBCC............',
    '................AA..............',
    '................AA..............',
    '..........DDDDAACCDDDD..........',
    '.........DEFGGAACCEGFED.........',
    '......DDDEF.FECCGGEF.FEDDDD.....',
    '......DGGF...FCCGGF...FGGED.....',
    '....DDEGGGF...FEGF...FGGGEED....',
    '....DGHEEGGF...FEF..FGGGEEGD....',
    '....DGEFFEGGFFFGGEFFGGEFFHGD....',
    '....DGF..FFEEEGGGEEEEFF..FGD....',
    '....DEGF...FFFGGGEFFF...FGGD....',
    '....DEGGF.....FGGF.....FGGED....',
    '....DHEGGFF....FGF...FFGGEHD....',
    '....DEHFEGGF...FGF..FGGEEHHD....',
    '.....DF.FEGGFFFGGEFFGGEFFHD.....',
    '.....DGF.FFEEEGGGEHEEFF..FD.....',
    '......DGF..FFFFGGEFFF..FFD......',
    '......DGEF.....FGF....FGGD......',
    '......DEGGF...FGGF...FGGGD......',
    '.......DEGGF..FGGF..FGGED.......',
    '........DEGGFFGGEEFFGGHDD.......',
    '.........DEGGGGGEEHEEED.........',
    '..........DDEEGGEHHEDD..........',
    '............DDDDDDDD............',
    '................................',
    '................................'
)
$palette = @{
    A='#752802'
    B='#7e370e'
    C='#542409'
    D='#443c2e'
    E='#b8ae92'
    F='#5c503b'
    G='#ede4c8'
    H='#817760'
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
