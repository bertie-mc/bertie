from __future__ import annotations

import os
import shutil
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class WaylandTools:
    sway: Path
    seat_keyboard: Path
    glfw: Path
    library_path: str | None = None
    gl_drivers_path: str | None = None
    egl_vendor_library_filenames: str | None = None


def load_packwiz() -> Path:
    configured = os.environ.get("BERTIE_CI_PACKWIZ")
    executable = Path(configured) if configured else None
    if executable is None:
        discovered = shutil.which("packwiz")
        executable = Path(discovered) if discovered else None
    if executable is None or not executable.is_file():
        raise RuntimeError(
            "packwiz is unavailable; set BERTIE_CI_PACKWIZ or run through the Nix flake"
        )
    return executable


def load_packwiz_installer() -> Path:
    configured = os.environ.get("BERTIE_CI_PACKWIZ_INSTALLER_JAR")
    if not configured:
        raise RuntimeError(
            "packwiz-installer is unavailable; set BERTIE_CI_PACKWIZ_INSTALLER_JAR "
            "or run through the Nix flake"
        )
    installer = Path(configured)
    if not installer.is_file():
        raise RuntimeError(f"packwiz-installer not found at {installer}")
    return installer


def load_java() -> Path:
    java_home = os.environ.get("BERTIE_CI_JAVA_HOME") or os.environ.get("JAVA_HOME")
    java_name = "java.exe" if os.name == "nt" else "java"
    if not java_home:
        raise RuntimeError(
            "Java 21 is unavailable; set JAVA_HOME or run through the Nix flake"
        )
    java = Path(java_home) / "bin" / java_name
    if not java.is_file():
        raise RuntimeError(f"Java not found at {java}")
    return java


def load_wayland_tools() -> WaylandTools:
    configured_sway = os.environ.get("BERTIE_CI_SWAY")
    sway = Path(configured_sway) if configured_sway else None
    if sway is None:
        discovered = shutil.which("sway")
        sway = Path(discovered) if discovered else None
    if sway is None or not sway.is_file():
        raise RuntimeError(
            "Sway is unavailable; set BERTIE_CI_SWAY or run through the Nix flake"
        )

    configured_keyboard = os.environ.get("BERTIE_CI_WAYLAND_SEAT_KEYBOARD")
    seat_keyboard = Path(configured_keyboard) if configured_keyboard else None
    if seat_keyboard is None:
        discovered = shutil.which("bertie-wayland-seat-keyboard")
        seat_keyboard = Path(discovered) if discovered else None
    if seat_keyboard is None or not seat_keyboard.is_file():
        raise RuntimeError(
            "The bertie-ci Wayland seat keyboard is unavailable; set "
            "BERTIE_CI_WAYLAND_SEAT_KEYBOARD or run through the Nix flake"
        )

    configured_glfw = os.environ.get("BERTIE_CI_WAYLAND_GLFW")
    if not configured_glfw:
        raise RuntimeError(
            "Wayland-capable GLFW is unavailable; set BERTIE_CI_WAYLAND_GLFW or "
            "run through the Nix flake"
        )
    glfw = Path(configured_glfw)
    if not glfw.is_file():
        raise RuntimeError(f"Wayland-capable GLFW not found at {glfw}")

    return WaylandTools(
        sway,
        seat_keyboard,
        glfw,
        os.environ.get("BERTIE_CI_WAYLAND_LIBRARY_PATH"),
        os.environ.get("BERTIE_CI_WAYLAND_GL_DRIVERS_PATH"),
        os.environ.get("BERTIE_CI_WAYLAND_EGL_VENDOR_LIBRARY_FILENAMES"),
    )
