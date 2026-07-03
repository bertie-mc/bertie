# FdLib Post-Shader Fix

Wraps *fdlib*'s post-shader initialization in a try/catch so an intermittent shader-load race is logged and skipped instead of crashing the client.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge
- **Mod ID:** `fdshaderfix`
- **Dependency:** Requires **fdlib**. Helps mods such as Qliphoth Awakening, Cinematic Cataclysm, and AnvilCraft boot reliably.

## License

Released under the MIT License — see [LICENSE](LICENSE).
