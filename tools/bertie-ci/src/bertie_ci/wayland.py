from __future__ import annotations

import os
import selectors
import subprocess
import tempfile
import time
from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

from .config import WaylandTools
from .process import terminate

_SWAY_CONFIG = """\
xwayland disable
output * mode 1280x720
default_border none
"""


def _wayland_socket(runtime: Path) -> Path | None:
    return next(
        (
            candidate
            for candidate in sorted(runtime.glob("wayland-*"))
            if candidate.is_socket()
        ),
        None,
    )


def _wait_for_keyboard(process: subprocess.Popen[bytes], log: Path) -> None:
    assert process.stdout is not None
    deadline = time.monotonic() + 10
    with selectors.DefaultSelector() as selector:
        selector.register(process.stdout, selectors.EVENT_READ)
        remaining = deadline - time.monotonic()
        if remaining <= 0 or not selector.select(remaining):
            raise RuntimeError(
                f"The Wayland seat keyboard did not become ready; see {log}"
            )

    if process.stdout.readline() != b"ready\n":
        raise RuntimeError(
            f"The Wayland seat keyboard exited before becoming ready; see {log}"
        )


@contextmanager
def wayland_session(
    tools: WaylandTools,
    log: Path,
) -> Iterator[dict[str, str]]:
    """Provide one isolated native-Wayland desktop for a CI client task."""
    log = log.resolve()
    log.parent.mkdir(parents=True, exist_ok=True)
    log.unlink(missing_ok=True)

    with tempfile.TemporaryDirectory(prefix="bertie-ci-wayland-") as temporary:
        runtime = Path(temporary)
        runtime.chmod(0o700)
        config = runtime / "sway.conf"
        config.write_text(_SWAY_CONFIG, encoding="utf-8")
        environment = {
            **os.environ,
            "XDG_RUNTIME_DIR": os.fspath(runtime),
            "XDG_SESSION_TYPE": "wayland",
            "LIBGL_ALWAYS_SOFTWARE": "true",
        }
        glfw_option = f"-Dorg.lwjgl.glfw.libname={os.fspath(tools.glfw)}"
        existing_java_options = environment.get("JAVA_TOOL_OPTIONS", "").strip()
        environment["JAVA_TOOL_OPTIONS"] = " ".join(
            option for option in (existing_java_options, glfw_option) if option
        )
        if tools.library_path:
            existing = environment.get("LD_LIBRARY_PATH")
            environment["LD_LIBRARY_PATH"] = (
                f"{tools.library_path}:{existing}" if existing else tools.library_path
            )
        if tools.gl_drivers_path:
            environment["LIBGL_DRIVERS_PATH"] = tools.gl_drivers_path
        if tools.egl_vendor_library_filenames:
            environment["__EGL_VENDOR_LIBRARY_FILENAMES"] = (
                tools.egl_vendor_library_filenames
            )
        for variable in (
            "DISPLAY",
            "SWAYSOCK",
            "WAYLAND_DISPLAY",
            "WLR_BACKENDS",
            "WLR_HEADLESS_OUTPUTS",
            "WLR_RENDERER",
        ):
            environment.pop(variable, None)

        compositor_environment = {
            **environment,
            "WLR_BACKENDS": "headless",
            "WLR_HEADLESS_OUTPUTS": "1",
            "WLR_RENDERER": "pixman",
        }

        with log.open("wb") as compositor_log:
            process = subprocess.Popen(
                [tools.sway, "--config", config],
                env=compositor_environment,
                stdout=compositor_log,
                stderr=subprocess.STDOUT,
                start_new_session=os.name == "posix",
            )
            keyboard: subprocess.Popen[bytes] | None = None
            try:
                deadline = time.monotonic() + 10
                socket = _wayland_socket(runtime)
                while socket is None:
                    if process.poll() is not None:
                        raise RuntimeError(
                            f"Sway exited before becoming ready; see {log}"
                        )
                    if time.monotonic() >= deadline:
                        raise RuntimeError(f"Sway did not become ready; see {log}")
                    time.sleep(0.05)
                    socket = _wayland_socket(runtime)

                environment["WAYLAND_DISPLAY"] = socket.name
                keyboard = subprocess.Popen(
                    [tools.seat_keyboard],
                    env=environment,
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=compositor_log,
                    start_new_session=os.name == "posix",
                )
                _wait_for_keyboard(keyboard, log)

                yield environment
                if process.poll() is not None:
                    raise RuntimeError(
                        f"Sway exited while the task was running; see {log}"
                    )
                if keyboard.poll() is not None:
                    raise RuntimeError(
                        f"The Wayland seat keyboard exited while the task was "
                        f"running; see {log}"
                    )
            finally:
                if keyboard is not None:
                    if keyboard.stdin is not None:
                        keyboard.stdin.close()
                    terminate(keyboard)
                    if keyboard.stdout is not None:
                        keyboard.stdout.close()
                terminate(process)
