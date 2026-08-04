# Bertie Screenshot Copy

A client-only NeoForge mod that copies each Minecraft screenshot to the native desktop
clipboard. It uses NeoForge's `ScreenshotEvent` and a native clipboard backend, so it does not
load AWT or require X11.

On Linux, the clipboard backend prefers the Wayland `ext-data-control-v1` or
`wlr-data-control-v1` protocol and falls back to X11 when available.

The implementation is released into the public domain under [The Unlicense](UNLICENSE).
Third-party libraries retain their own licenses as described in [NOTICE](NOTICE).
