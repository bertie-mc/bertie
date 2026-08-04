from pathlib import Path

import pytest
from bertie_ci.config import (
    WaylandTools,
    load_java,
    load_packwiz_installer,
    load_wayland_tools,
)


def _tool(tmp_path: Path, monkeypatch: pytest.MonkeyPatch, variable: str) -> Path:
    path = tmp_path / variable.lower()
    path.touch()
    monkeypatch.setenv(variable, str(path))
    return path


def test_wayland_tools_require_sway_seat_keyboard_and_native_glfw(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    sway = _tool(tmp_path, monkeypatch, "BERTIE_CI_SWAY")
    seat_keyboard = _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_SEAT_KEYBOARD")
    glfw = _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_GLFW")
    monkeypatch.delenv("BERTIE_CI_PACKWIZ_INSTALLER_JAR", raising=False)

    assert load_wayland_tools() == WaylandTools(sway, seat_keyboard, glfw)


def test_wayland_tools_can_find_sway_and_seat_keyboard_on_path(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    sway = tmp_path / "sway"
    sway.touch()
    seat_keyboard = tmp_path / "bertie-wayland-seat-keyboard"
    seat_keyboard.touch()
    glfw = _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_GLFW")
    monkeypatch.delenv("BERTIE_CI_SWAY", raising=False)
    monkeypatch.delenv("BERTIE_CI_WAYLAND_SEAT_KEYBOARD", raising=False)
    monkeypatch.setattr(
        "bertie_ci.config.shutil.which",
        lambda name: str(
            {"sway": sway, "bertie-wayland-seat-keyboard": seat_keyboard}[name]
        ),
    )

    assert load_wayland_tools() == WaylandTools(sway, seat_keyboard, glfw)


def test_wayland_tools_load_nix_private_environment(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    sway = _tool(tmp_path, monkeypatch, "BERTIE_CI_SWAY")
    seat_keyboard = _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_SEAT_KEYBOARD")
    glfw = _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_GLFW")
    monkeypatch.setenv("BERTIE_CI_WAYLAND_LIBRARY_PATH", "/nix/wayland-libs")
    monkeypatch.setenv("BERTIE_CI_WAYLAND_GL_DRIVERS_PATH", "/nix/mesa/dri")
    monkeypatch.setenv(
        "BERTIE_CI_WAYLAND_EGL_VENDOR_LIBRARY_FILENAMES", "/nix/mesa/egl.json"
    )

    assert load_wayland_tools() == WaylandTools(
        sway,
        seat_keyboard,
        glfw,
        "/nix/wayland-libs",
        "/nix/mesa/dri",
        "/nix/mesa/egl.json",
    )


def test_wayland_tools_reject_legacy_glfw_variable(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _tool(tmp_path, monkeypatch, "BERTIE_CI_SWAY")
    _tool(tmp_path, monkeypatch, "BERTIE_CI_WAYLAND_SEAT_KEYBOARD")
    _tool(tmp_path, monkeypatch, "BERTIE_CI_GLFW")
    monkeypatch.delenv("BERTIE_CI_WAYLAND_GLFW", raising=False)

    with pytest.raises(RuntimeError, match="BERTIE_CI_WAYLAND_GLFW"):
        load_wayland_tools()


def test_packwiz_installer_only_requires_its_jar(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    installer = _tool(tmp_path, monkeypatch, "BERTIE_CI_PACKWIZ_INSTALLER_JAR")
    monkeypatch.delenv("BERTIE_CI_SWAY", raising=False)

    assert load_packwiz_installer() == installer


def test_java_is_loaded_from_java_home(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    home = tmp_path / "jdk"
    java = home / "bin" / "java"
    java.parent.mkdir(parents=True)
    java.touch()
    monkeypatch.setenv("JAVA_HOME", str(home))
    monkeypatch.delenv("BERTIE_CI_JAVA_HOME", raising=False)

    assert load_java() == java
