# Texture sources

`make_textures.py` is the maintained source for the item textures shipped by the mod. It writes
generated assets to `src/main/resources` and disposable previews to the ignored `out` directory.

The generator needs Pillow and these vanilla Minecraft item textures under `vanilla/`:

- `bowl.png`
- `glass_bottle.png`
- `potion_overlay.png`

Vanilla textures are deliberately not committed. From this directory, run the generator with:

```console
nix shell nixpkgs#python3Packages.pillow -c python3 make_textures.py
```

Review the resulting resource changes before committing them. Candidate renders and comparison
images are local design work and do not belong in the repository.
