import os
import signal
import socket
import subprocess
import sys
from pathlib import Path

import bertie_ci.wayland as wayland_module
import pytest
from bertie_ci.config import WaylandTools
from bertie_ci.process import TerminationRequested
from bertie_ci.wayland import _wait_for_keyboard, wayland_session


def test_wayland_session_starts_headless_sway_and_hides_x11(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    sway = tmp_path / "sway"
    sway.touch()
    seat_keyboard = tmp_path / "bertie-wayland-seat-keyboard"
    seat_keyboard.touch()
    glfw = tmp_path / "libglfw.so"
    glfw.touch()
    observed: dict[str, object] = {}

    class FakeInput:
        closed = False

        def close(self) -> None:
            self.closed = True

    class FakeOutput:
        closed = False

        def close(self) -> None:
            self.closed = True

    class FakeProcess:
        pid = 123

        def __init__(self, command: list[object], **kwargs: object) -> None:
            self.command = command
            self.stdin = FakeInput() if kwargs.get("stdin") is subprocess.PIPE else None
            self.stdout = (
                FakeOutput() if kwargs.get("stdout") is subprocess.PIPE else None
            )
            environment = kwargs["env"]
            assert isinstance(environment, dict)
            self.sockets: list[socket.socket] = []
            if command[0] == sway:
                observed["compositor"] = {"command": command, **kwargs}
                observed["config"] = Path(command[2]).read_text(encoding="utf-8")
                for name in ("wayland-7", "sway-ipc.1000.123.sock"):
                    created = socket.socket(socket.AF_UNIX)
                    created.bind(os.fspath(Path(environment["XDG_RUNTIME_DIR"], name)))
                    self.sockets.append(created)
            else:
                observed["keyboard"] = {"command": command, **kwargs}

        def poll(self) -> None:
            return None

    processes: list[FakeProcess] = []

    def popen(command: list[object], **kwargs: object) -> FakeProcess:
        process = FakeProcess(command, **kwargs)
        processes.append(process)
        return process

    terminated: list[object] = []
    monkeypatch.setenv("DISPLAY", ":99")
    monkeypatch.setenv("SWAYSOCK", "/run/user/1000/sway.sock")
    monkeypatch.setenv("WAYLAND_DISPLAY", "wayland-0")
    monkeypatch.setenv("WLR_BACKENDS", "drm")
    monkeypatch.setenv("JAVA_TOOL_OPTIONS", "-Dexisting.option=true")
    monkeypatch.setattr(wayland_module.subprocess, "Popen", popen)
    ready: list[object] = []
    monkeypatch.setattr(
        wayland_module,
        "_wait_for_keyboard",
        lambda process, log: ready.extend((process, log)),
    )
    monkeypatch.setattr(wayland_module, "terminate", terminated.append)

    tools = WaylandTools(
        sway,
        seat_keyboard,
        glfw,
        "/nix/wayland-libs",
        "/nix/mesa/dri",
        "/nix/mesa/egl.json",
    )
    with pytest.raises(TerminationRequested):
        with wayland_session(tools, tmp_path / "wayland.log") as env:
            assert "DISPLAY" not in env
            assert "SWAYSOCK" not in env
            assert env["WAYLAND_DISPLAY"] == "wayland-7"
            assert env["XDG_SESSION_TYPE"] == "wayland"
            assert env["LIBGL_ALWAYS_SOFTWARE"] == "true"
            assert "WLR_BACKENDS" not in env
            assert env["JAVA_TOOL_OPTIONS"] == (
                f"-Dexisting.option=true -Dorg.lwjgl.glfw.libname={glfw}"
            )
            assert env["LD_LIBRARY_PATH"].startswith("/nix/wayland-libs")
            assert env["LIBGL_DRIVERS_PATH"] == "/nix/mesa/dri"
            assert env["__EGL_VENDOR_LIBRARY_FILENAMES"] == "/nix/mesa/egl.json"
            assert Path(env["XDG_RUNTIME_DIR"], env["WAYLAND_DISPLAY"]).exists()
            raise TerminationRequested(signal.SIGTERM)

    compositor = observed["compositor"]
    assert isinstance(compositor, dict)
    command = compositor["command"]
    assert isinstance(command, list)
    assert command[:2] == [sway, "--config"]
    assert observed["config"] == (
        "xwayland disable\noutput * mode 1280x720\ndefault_border none\n"
    )
    compositor_environment = compositor["env"]
    assert isinstance(compositor_environment, dict)
    assert "DISPLAY" not in compositor_environment
    assert "WAYLAND_DISPLAY" not in compositor_environment
    assert "SWAYSOCK" not in compositor_environment
    assert compositor_environment["WLR_BACKENDS"] == "headless"
    assert compositor_environment["WLR_HEADLESS_OUTPUTS"] == "1"
    assert compositor_environment["WLR_RENDERER"] == "pixman"
    assert compositor["stderr"] is subprocess.STDOUT
    assert compositor["start_new_session"] is (os.name == "posix")

    keyboard = observed["keyboard"]
    assert isinstance(keyboard, dict)
    assert keyboard["command"] == [seat_keyboard]
    assert keyboard["stdin"] is subprocess.PIPE
    assert keyboard["stdout"] is subprocess.PIPE
    assert keyboard["stderr"] is not subprocess.STDOUT
    assert keyboard["env"]["WAYLAND_DISPLAY"] == "wayland-7"
    assert ready == [processes[1], tmp_path / "wayland.log"]
    assert terminated == [processes[1], processes[0]]
    assert processes[1].stdin is not None and processes[1].stdin.closed
    assert processes[1].stdout is not None and processes[1].stdout.closed
    for process in processes:
        for created in process.sockets:
            created.close()


def test_seat_keyboard_readiness_uses_the_helper_protocol(tmp_path: Path) -> None:
    process = subprocess.Popen(
        [sys.executable, "-c", "print('ready', flush=True)"],
        stdout=subprocess.PIPE,
    )
    try:
        _wait_for_keyboard(process, tmp_path / "wayland.log")
    finally:
        process.wait()
        assert process.stdout is not None
        process.stdout.close()


def test_seat_keyboard_readiness_rejects_eof(tmp_path: Path) -> None:
    process = subprocess.Popen(
        [sys.executable, "-c", "pass"],
        stdout=subprocess.PIPE,
    )
    try:
        with pytest.raises(RuntimeError, match="exited before becoming ready"):
            _wait_for_keyboard(process, tmp_path / "wayland.log")
    finally:
        process.wait()
        assert process.stdout is not None
        process.stdout.close()
