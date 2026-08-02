from __future__ import annotations

import os
import queue
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path
from typing import Mapping, Sequence


def run(
    command: Sequence[str | Path],
    *,
    cwd: Path,
    env: Mapping[str, str] | None = None,
    log: Path | None = None,
    timeout_seconds: int | None = None,
    stream_output: bool = True,
    completion_marker: str | None = None,
) -> None:
    rendered = [os.fspath(part) for part in command]
    print(f"+ {subprocess.list2cmdline(rendered)}", flush=True)

    if log is None:
        subprocess.run(rendered, cwd=cwd, env=env, check=True, timeout=timeout_seconds)
        return

    log.parent.mkdir(parents=True, exist_ok=True)
    lines: queue.Queue[str | None] = queue.Queue()
    with log.open("w", encoding="utf-8", errors="replace") as log_file:
        process = subprocess.Popen(
            rendered,
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            start_new_session=os.name == "posix",
        )
        assert process.stdout is not None

        def read_output() -> None:
            for line in process.stdout:
                lines.put(line)
            lines.put(None)

        threading.Thread(target=read_output, daemon=True).start()
        deadline = time.monotonic() + timeout_seconds if timeout_seconds else None

        try:
            while True:
                if deadline is not None and time.monotonic() >= deadline:
                    _terminate(process)
                    raise subprocess.TimeoutExpired(rendered, timeout_seconds)
                try:
                    line = lines.get(timeout=0.25)
                except queue.Empty:
                    if process.poll() is not None:
                        continue
                    continue
                if line is None:
                    break
                if stream_output:
                    sys.stdout.write(line)
                    sys.stdout.flush()
                log_file.write(line)
                log_file.flush()
                if completion_marker is not None and completion_marker in line:
                    _terminate(process)
                    return
        except KeyboardInterrupt:
            _terminate(process)
            raise

        return_code = process.wait()
        if return_code:
            raise subprocess.CalledProcessError(return_code, rendered)


def _signal(process: subprocess.Popen[str], requested: signal.Signals) -> None:
    try:
        if os.name == "posix":
            os.killpg(process.pid, requested)
        elif requested == signal.SIGTERM:
            process.terminate()
        else:
            process.kill()
    except ProcessLookupError:
        pass


def _terminate(process: subprocess.Popen[str]) -> None:
    if process.poll() is not None:
        return
    _signal(process, signal.SIGTERM)
    try:
        process.wait(timeout=30)
    except subprocess.TimeoutExpired:
        _signal(process, signal.SIGKILL)
        process.wait()
