#!/usr/bin/env python3
"""Dump current UI and tap a node. Maps uiautomator bounds onto wm size."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

SERIAL = "1431df87"
ASSETS = Path("/home/priyanshuchawda/retenqo/pitch-assets")


def adb(*args, check=True):
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=check,
        text=True,
        capture_output=True,
    )


def wm_size():
    out = adb("shell", "wm", "size").stdout
    m = re.search(r"(\d+)x(\d+)", out)
    return int(m.group(1)), int(m.group(2))


def dump_xml():
    adb("shell", "uiautomator", "dump", "/sdcard/uidump.xml")
    adb("pull", "/sdcard/uidump.xml", "/tmp/uidump.xml")
    return Path("/tmp/uidump.xml").read_text()


def nodes(xml: str):
    out = []
    for m in re.finditer(r"<node[^>]*>", xml):
        tag = m.group(0)
        text = re.search(r'text="([^"]*)"', tag)
        desc = re.search(r'content-desc="([^"]*)"', tag)
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        if not b:
            continue
        x1, y1, x2, y2 = map(int, b.groups())
        out.append(
            {
                "text": text.group(1) if text else "",
                "desc": desc.group(1) if desc else "",
                "x1": x1,
                "y1": y1,
                "x2": x2,
                "y2": y2,
            }
        )
    return out


def dump_root(xml: str):
    m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        w, h = wm_size()
        return w, h
    return int(m.group(3)), int(m.group(4))


def to_screen(x, y, dw, dh, sw, sh):
    """Scale dump coords onto physical wm size (handles swapped landscape dumps)."""
    return int(x * sw / dw), int(y * sh / dh)


def tap_label(want, substr=False, nth=-1):
    xml = dump_xml()
    dw, dh = dump_root(xml)
    sw, sh = wm_size()
    hits = []
    for n in nodes(xml):
        blob = n["text"] or n["desc"]
        ok = (want.lower() in blob.lower()) if substr else (blob == want)
        if ok and (n["x2"] - n["x1"]) > 4 and (n["y2"] - n["y1"]) > 4:
            hits.append(n)
    if not hits:
        print(f"NOT FOUND {want!r} dump={dw}x{dh} screen={sw}x{sh}")
        for n in nodes(xml):
            if n["text"]:
                print(" ", repr(n["text"][:80]), n["x1"], n["y1"], n["x2"], n["y2"])
        return False
    n = hits[nth]
    cx, cy = (n["x1"] + n["x2"]) // 2, (n["y1"] + n["y2"]) // 2
    x, y = to_screen(cx, cy, dw, dh, sw, sh)
    print(
        f"TAP {n['text'] or n['desc']!r} dump=({cx},{cy}) screen=({x},{y}) "
        f"map {dw}x{dh}->{sw}x{sh}"
    )
    adb("shell", "input", "tap", str(x), str(y))
    return True


def tap_xy(x, y):
    print(f"TAP xy {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))


def swipe(x1, y1, x2, y2, ms=400):
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))


def screencap(name):
    path = ASSETS / name
    p = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        check=True,
        capture_output=True,
    )
    path.write_bytes(p.stdout)
    print("saved", path, "bytes", len(p.stdout))


def list_texts():
    xml = dump_xml()
    dw, dh = dump_root(xml)
    sw, sh = wm_size()
    print(f"dump={dw}x{dh} screen={sw}x{sh}")
    for n in nodes(xml):
        if n["text"]:
            x, y = to_screen(
                (n["x1"] + n["x2"]) // 2, (n["y1"] + n["y2"]) // 2, dw, dh, sw, sh
            )
            print(f"  {n['text'][:70]!r} dump=({n['x1']},{n['y1']}) screen=({x},{y})")


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "tap":
        sys.exit(0 if tap_label(sys.argv[2], substr="--substr" in sys.argv) else 1)
    if cmd == "xy":
        tap_xy(int(sys.argv[2]), int(sys.argv[3]))
    if cmd == "shot":
        screencap(sys.argv[2])
    if cmd == "dump":
        list_texts()
    if cmd == "swipe-up":
        swipe(540, 1800, 540, 700, 500)
