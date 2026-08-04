from __future__ import annotations

import os
import queue
import signal
import subprocess
import sys
import threading
import time
from contextlib import contextmanager
from pathlib import Path
from types import FrameType
from typing import IO, Iterator, Mapping, Sequence


class TerminationRequested(BaseException):
    """Request normal stack unwinding after an external termination signal."""

    def __init__(self, signal_number: int) -> None:
        super().__init__(signal_number)
        self.signal_number = signal_number


@contextmanager
def unwind_on_sigterm() -> Iterator[None]:
    """Translate SIGTERM into an exception so context-manager cleanup can run."""
    previous = signal.getsignal(signal.SIGTERM)

    def request_termination(signal_number: int, _frame: FrameType | None) -> None:
        raise TerminationRequested(signal_number)

    signal.signal(signal.SIGTERM, request_termination)
    try:
        yield
    finally:
        signal.signal(signal.SIGTERM, previous)


def run(
    command: Sequence[str | Path],
    *,
    cwd: Path,
    env: Mapping[str, str] | None = None,
    log: Path | None = None,
    timeout_seconds: int | None = None,
    stream_output: bool = True,
) -> None:
    rendered = [os.fspath(part) for part in command]
    print(f"+ {subprocess.list2cmdline(rendered)}", flush=True)

    log_file: IO[str] | None = None
    try:
        if log is not None:
            log.parent.mkdir(parents=True, exist_ok=True)
            log_file = log.open("w", encoding="utf-8", errors="replace")
        process = subprocess.Popen(
            rendered,
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE if log_file is not None else None,
            stderr=subprocess.STDOUT if log_file is not None else None,
            text=True,
            encoding="utf-8",
            errors="replace",
            start_new_session=os.name == "posix",
        )
        _supervise(
            process,
            rendered,
            log_file=log_file,
            timeout_seconds=timeout_seconds,
            stream_output=stream_output,
        )
    finally:
        if log_file is not None:
            log_file.close()


def _supervise(
    process: subprocess.Popen[str],
    command: Sequence[str],
    *,
    log_file: IO[str] | None,
    timeout_seconds: int | None,
    stream_output: bool,
) -> None:
    supervised_groups: set[int] = set()
    try:
        supervised_groups.update(_process_groups(process.pid))
        next_group_snapshot = time.monotonic() + 1
        lines: queue.Queue[str | None] | None = None
        output_finished = log_file is None

        if log_file is not None:
            lines = queue.Queue()
            assert process.stdout is not None

            def read_output() -> None:
                for line in process.stdout:
                    lines.put(line)
                lines.put(None)

            threading.Thread(target=read_output, daemon=True).start()

        deadline = (
            time.monotonic() + timeout_seconds if timeout_seconds is not None else None
        )
        while True:
            now = time.monotonic()
            if os.name == "posix" and now >= next_group_snapshot:
                supervised_groups.update(_process_groups(process.pid))
                next_group_snapshot = now + 1
            if deadline is not None and now >= deadline:
                raise subprocess.TimeoutExpired(command, timeout_seconds)

            if output_finished:
                return_code = process.poll()
                if return_code is not None:
                    break
                time.sleep(0.25)
                continue

            assert lines is not None
            try:
                line = lines.get(timeout=0.25)
            except queue.Empty:
                continue
            if line is None:
                output_finished = True
                continue
            if stream_output:
                sys.stdout.write(line)
                sys.stdout.flush()
            assert log_file is not None
            log_file.write(line)
            log_file.flush()

        return_code = process.wait()
        if return_code:
            raise subprocess.CalledProcessError(return_code, command)
    except BaseException:
        terminate(process, supervised_groups)
        raise


def _process_table() -> dict[int, tuple[int, int, str]] | None:
    proc = Path("/proc")
    if proc.is_dir():
        processes: dict[int, tuple[int, int, str]] = {}
        for entry in proc.iterdir():
            if not entry.name.isdigit():
                continue
            try:
                stat = (entry / "stat").read_text(encoding="utf-8")
                fields = stat[stat.rfind(")") + 2 :].split()
                processes[int(entry.name)] = (int(fields[1]), int(fields[2]), fields[0])
            except (FileNotFoundError, PermissionError, ValueError, IndexError):
                continue
        return processes

    try:
        listing = subprocess.run(
            ["ps", "-eo", "pid=,ppid=,pgid=,stat="],
            check=True,
            capture_output=True,
            text=True,
        ).stdout
    except (OSError, subprocess.SubprocessError):
        return None

    processes = {}
    for line in listing.splitlines():
        fields = line.split()
        if len(fields) != 4:
            continue
        pid, parent_pid, group_id = map(int, fields[:3])
        processes[pid] = (parent_pid, group_id, fields[3][:1])
    return processes


def _process_groups(root_pid: int) -> set[int]:
    """Return process groups in the supervised tree, including detached children."""
    if os.name != "posix":
        return set()

    try:
        root_group = os.getpgid(root_pid)
    except ProcessLookupError:
        return set()

    groups = {root_group}
    processes = _process_table()
    if processes is None:
        return groups

    descendants = {root_pid}
    changed = True
    while changed:
        changed = False
        for pid, (parent_pid, _, _) in processes.items():
            if parent_pid in descendants and pid not in descendants:
                descendants.add(pid)
                changed = True
    groups.update(processes[pid][1] for pid in descendants if pid in processes)
    groups.discard(os.getpgrp())
    return groups


def _signal_groups(groups: set[int], requested: signal.Signals) -> None:
    for group in groups:
        try:
            os.killpg(group, requested)
        except ProcessLookupError:
            pass


def _groups_alive(groups: set[int]) -> bool:
    processes = _process_table()
    if processes is not None:
        return any(
            group_id in groups and not state.startswith("Z")
            for _, group_id, state in processes.values()
        )
    for group in groups:
        try:
            os.killpg(group, 0)
            return True
        except ProcessLookupError:
            continue
        except PermissionError:
            return True
    return False


def terminate(
    process: subprocess.Popen[object], supervised_groups: set[int] | None = None
) -> None:
    if os.name != "posix":
        if process.poll() is not None:
            return
        process.terminate()
        try:
            process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
        return

    groups = set(supervised_groups or ())
    groups.update(_process_groups(process.pid))
    if process.poll() is not None and not _groups_alive(groups):
        return
    _signal_groups(groups, signal.SIGTERM)
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        process.poll()
        if not _groups_alive(groups):
            return
        time.sleep(0.05)

    _signal_groups(groups, signal.SIGKILL)
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        pass
