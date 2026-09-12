#!/usr/bin/env python3
"""Build the non-authoritative stream health snapshot for GregoTV.

The snapshot is intentionally a ranking hint, never a block list. GitHub
Actions runs outside Spain and receives geo-block errors for channels that
play perfectly on a user's TV.
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "app/src/main/java/com/gregotv/data/settings/SettingsRepository.kt"
USER_AGENT = "GregoTV verifier/1.0"


def get(url: str, timeout: int, headers: dict[str, str] | None = None) -> tuple[int, bytes]:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, **(headers or {})})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, b""
    except Exception:
        return 0, b""


def default_lists() -> list[str]:
    source = SETTINGS.read_text(encoding="utf-8")
    block = source[source.index("object DefaultLists"):]
    return re.findall(r'"(https://[^"\s]+)"', block)


def stream_urls() -> list[str]:
    urls: set[str] = set()
    for playlist in default_lists():
        code, body = get(playlist, timeout=40)
        if code != 200:
            print(f"warning: list {code}: {playlist}", file=sys.stderr)
            continue
        lines = body.decode("utf-8", errors="replace").splitlines()
        for index, line in enumerate(lines):
            if line.startswith("#EXTINF"):
                for candidate in lines[index + 1:]:
                    candidate = candidate.strip()
                    if not candidate or candidate.startswith("#"):
                        continue
                    if "://" in candidate:
                        urls.add(candidate)
                    break
    return sorted(urls)


def check(url: str) -> tuple[str, str]:
    code, _ = get(url, timeout=6, headers={"Range": "bytes=0-1023"})
    # A tiny byte range is enough to establish that the endpoint is reachable.
    # 200 is accepted because many streaming servers ignore Range entirely.
    return url, "ok" if code in (200, 206) else "dead"


def existing_age_hours(url: str) -> float | None:
    code, body = get(url, timeout=8)
    if code != 200:
        return None
    try:
        generated = json.loads(body)["generatedAt"]
        then = datetime.fromisoformat(generated.replace("Z", "+00:00"))
        return (datetime.now(timezone.utc) - then).total_seconds() / 3600
    except (KeyError, ValueError, TypeError, json.JSONDecodeError):
        return None


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--min-age-hours", type=int, default=96)
    parser.add_argument("--existing-url", default="")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()

    if args.existing_url and not args.force:
        age = existing_age_hours(args.existing_url)
        if age is not None and age < args.min_age_hours:
            print(f"snapshot is {age:.1f}h old; waiting for {args.min_age_hours}h")
            return 0

    urls = stream_urls()
    print(f"checking {len(urls)} unique streams")
    results: dict[str, str] = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=40) as pool:
        for index, (url, status) in enumerate(pool.map(check, urls), start=1):
            results[url] = status
            if index % 100 == 0:
                print(f"checked {index}/{len(urls)}")

    payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "streamCount": len(results),
        "okCount": sum(status == "ok" for status in results.values()),
        "streams": results,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {args.output}: {payload['okCount']}/{payload['streamCount']} reachable")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
