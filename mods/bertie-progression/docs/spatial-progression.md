# Spatial crafting

Giant Anvil and Miniaturization entries open an individual Ponder from their EMI
button. The scenes assemble the structure, show its final catalyst or falling
anvil, and reveal the output. Labels name the blocks rather than describing the
construction in prose. Output items also expose the guides through Ponder's
normal item entry point. Both entry points expose the same material-icon strip
below the timeline: click an icon to seek to that stage, or hover it for the
material name. Native chapter navigation remains available between alternatives.

The catalogue includes two Giant Anvil guides and thirteen Miniaturization guides:
the Wall, seven Compact Machine templates, and five large Mekanism machines.
The previews use a three-quarter angle with the south face still toward the viewer. Their structures remain
in the captured orientation; camera rotation does not change the recipe.

## Large machine Miniaturization

All five structures fit a 3×3×3 field and produce one machine. Drop one catalyst
after assembly:

| Machine | Catalyst |
| --- | --- |
| Large Heat Generator | Heated Tungsten Block |
| Large Gas-Burning Generator | Advanced Electrolysis Core |
| Large Pigment Mixer | Rosest Quartz |
| Large Chemical Infuser | Space Overcompressor |
| Large Electrolytic Separator | Advanced Electrolysis Core |

The structures preserve the native tall Chemical Tanks and Pigment Mixer's
bounding blocks. Those cells are required physical parts, not extra items to
obtain. Connected and straight Superconductor segments represent the same cable
item and match interchangeably. The Pigment Mixer and Chemical Infuser use two
Superconductors each; the Electrolytic Separator uses one.

The layouts, physical states and ingredient counts are authored in
`texture-work/large_machine_mini_layouts.json`. `reviewed_progression_recipes.py`
emits their native Compact Crafting recipes and ingredient views;
`spatial_guides.py` creates deterministic Ponder structures and scene metadata.
The recipe adapter converts bottom-up world captures into the native top-down,
X/Z-column matrix format. The scan also snapshots positions before storing them
as lookup keys.

## Sentient Casing

Sentient Casing is a placeable iron-tool-tier metal block with the approved
16-pixel materials and animated purple emissive surfaces. Its body, frame and
emissive materials now share flat full-cube faces, without inset or raised
geometry. Texture animation and the existing palette are retained. Its End Table recipe
combines Titanium, Refined Obsidian, Duratium, Unholy Intelligence, Super AI Chips,
Reinforced Carbon Sheets and an Overcharged Crystal. It replaces the single
Steel Casing in the Field Projector recipe, which produces four Projectors.

## Reusable End Table catalyst

Twenty-two Ferrojelly and twelve Ender Metal Ingots around a Void Core produce
three Enderite Ingots. The Void Core remains in the table with its components
and custom name intact. Its remainder is local to this recipe and works for
repeated crafting; ordinary uses of Void Core retain their native behavior.
The existing Enderite Block packing and unpacking remain available.

The recipe extends Avaritia's table type and uses its large-pattern codec.
Its EMI slot marks the Void Core as a catalyst and records the matching remainder.
