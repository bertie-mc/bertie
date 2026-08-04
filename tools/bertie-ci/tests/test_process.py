import os
import signal
import subprocess
import sys
import threading
import time
from pathlib import Path

import pytest
from bertie_ci.process import TerminationRequested, run, unwind_on_sigterm


def test_logged_timeout_survives_early_output_eof_and_preserves_log(
    tmp_path: Path,
) -> None:
    log = tmp_path / "process.log"
    started = time.monotonic()

    with pytest.raises(subprocess.TimeoutExpired):
        run(
            [
                sys.executable,
                "-c",
                "import os, time; print('STARTED', flush=True); "
                "os.close(1); os.close(2); time.sleep(60)",
            ],
            cwd=tmp_path,
            log=log,
            timeout_seconds=1,
            stream_output=False,
        )

    assert time.monotonic() - started < 5
    assert log.read_text(encoding="utf-8") == "STARTED\n"


def test_no_log_inherits_stdout_and_stderr(
    tmp_path: Path, capfd: pytest.CaptureFixture[str]
) -> None:
    script = tmp_path / "emit.py"
    script.write_text(
        "import sys\nprint('STANDARD OUTPUT')\nprint('STANDARD ERROR', file=sys.stderr)\n",
        encoding="utf-8",
    )

    run([sys.executable, script], cwd=tmp_path)

    captured = capfd.readouterr()
    assert captured.out.endswith("STANDARD OUTPUT\n")
    assert captured.err == "STANDARD ERROR\n"


def test_nonzero_exit_is_reported(tmp_path: Path) -> None:
    with pytest.raises(subprocess.CalledProcessError) as error:
        run([sys.executable, "-c", "raise SystemExit(7)"], cwd=tmp_path)

    assert error.value.returncode == 7


@pytest.mark.skipif(os.name != "posix", reason="POSIX signal behavior")
def test_no_log_sigterm_unwinds_and_terminates_supervised_process(
    tmp_path: Path,
) -> None:
    child_pid_file = tmp_path / "child.pid"
    script = """
import os
import pathlib
import sys
import time

pathlib.Path(sys.argv[1]).write_text(str(os.getpid()), encoding="utf-8")
print("STARTED", flush=True)
time.sleep(60)
"""

    def request_termination() -> None:
        deadline = time.monotonic() + 5
        while not child_pid_file.exists() and time.monotonic() < deadline:
            time.sleep(0.01)
        os.kill(os.getpid(), signal.SIGTERM)

    sender = threading.Thread(target=request_termination)
    with unwind_on_sigterm():
        sender.start()
        with pytest.raises(TerminationRequested):
            run(
                [sys.executable, "-c", script, str(child_pid_file)],
                cwd=tmp_path,
            )
    sender.join(timeout=1)

    child_pid = int(child_pid_file.read_text(encoding="utf-8"))
    with pytest.raises(ProcessLookupError):
        os.kill(child_pid, 0)


@pytest.mark.skipif(os.name != "posix", reason="POSIX process-group behavior")
def test_timeout_terminates_detached_descendants(tmp_path: Path) -> None:
    child_pid_file = tmp_path / "child.pid"
    script = """
import pathlib
import subprocess
import sys
import time

child = subprocess.Popen(
    [sys.executable, "-c", "import time; time.sleep(60)"],
    start_new_session=True,
)
pathlib.Path(sys.argv[1]).write_text(str(child.pid), encoding="utf-8")
print("STARTED", flush=True)
time.sleep(60)
"""

    with pytest.raises(subprocess.TimeoutExpired):
        run(
            [sys.executable, "-c", script, str(child_pid_file)],
            cwd=tmp_path,
            log=tmp_path / "process.log",
            timeout_seconds=1,
            stream_output=False,
        )

    child_pid = int(child_pid_file.read_text(encoding="utf-8"))

    def child_exists() -> bool:
        stat = Path(f"/proc/{child_pid}/stat")
        if stat.is_file():
            fields = stat.read_text(encoding="utf-8").split()
            return len(fields) < 3 or fields[2] != "Z"
        try:
            os.kill(child_pid, 0)
            return True
        except ProcessLookupError:
            return False

    deadline = time.monotonic() + 2
    while time.monotonic() < deadline and child_exists():
        time.sleep(0.05)
    assert not child_exists()
