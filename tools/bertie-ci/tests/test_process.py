import sys
import time
from pathlib import Path

from bertie_ci.process import run


def test_completion_marker_terminates_process_group(tmp_path: Path) -> None:
    log = tmp_path / "process.log"
    started = time.monotonic()

    run(
        [
            sys.executable,
            "-c",
            "import time; print('SCENARIO_OK', flush=True); time.sleep(60)",
        ],
        cwd=tmp_path,
        log=log,
        completion_marker="SCENARIO_OK",
        timeout_seconds=5,
        stream_output=False,
    )

    assert time.monotonic() - started < 5
    assert log.read_text(encoding="utf-8") == "SCENARIO_OK\n"
