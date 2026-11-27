#!/usr/bin/env python3
"""Download and validate the CC0 GLB candidates for the Ramadan 3D scene.

The source pages are intentionally used instead of guessed CDN paths. The
script discovers the published GLB download link from each page and fails
closed if it cannot identify and validate a binary glTF file.
"""
from __future__ import annotations

import json
import re
import struct
import sys
import urllib.parse
import urllib.request
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "models"

SOURCES = [
    {
        "file": "mosque_two_minarets.glb",
        "page": "https://www.innerscene.com/tools/library/3d-parts/ottoman-classical-mosque-central-dome-and-2-minarets-d1141513",
        "title": "Ottoman classical mosque, central dome and 2 minarets",
        "license": "CC0 1.0",
        "notes": "Full architectural model; two pencil minarets and central dome.",
    },
    {
        "file": "brass_hurricane_lantern.glb",
        "page": "https://www.innerscene.com/tools/library/3d-parts/brass-hurricane-lantern-c91f8ff4",
        "title": "Brass hurricane lantern",
        "license": "CC0 1.0",
        "notes": "PBR brass and glass lantern model.",
    },
    {
        "file": "sleeping_cat.glb",
        "page": "https://www.innerscene.com/tools/library/3d-parts/domestic-cat-curled-asleep-73a8a293",
        "title": "Domestic cat curled asleep",
        "license": "CC0 1.0",
        "notes": "Curled sleeping domestic cat model.",
    },
    {
        "file": "broadleaf_canopy_tree.glb",
        "page": "https://www.innerscene.com/tools/library/3d-parts/broadleaf-rainforest-canopy-tree-25-m-b42c69c8",
        "title": "Broadleaf rainforest canopy tree, 25 m",
        "license": "CC0 1.0",
        "notes": "Wide mature canopy silhouette; artistic reference only, not a botanical identification.",
    },
]

class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self._href = None
        self._text = []

    def handle_starttag(self, tag, attrs):
        if tag.lower() == "a":
            self._href = dict(attrs).get("href")
            self._text = []

    def handle_data(self, data):
        if self._href is not None:
            self._text.append(data)

    def handle_endtag(self, tag):
        if tag.lower() == "a" and self._href is not None:
            self.links.append((self._href, " ".join(self._text).strip()))
            self._href = None
            self._text = []

def request(url: str) -> bytes:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "RamadanDreamLauncher-3DAssetBuilder/1.0"},
    )
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()

def discover_glb(page_url: str, html: bytes) -> str:
    page = html.decode("utf-8", "replace")
    parser = Links()
    parser.feed(page)
    candidates = []
    for href, label in parser.links:
        absolute = urllib.parse.urljoin(page_url, href)
        low = (absolute + " " + label).lower()
        if ".glb" in low or ("download" in low and "glb" in low):
            candidates.append(absolute)

    # Some pages expose the download URL in JSON data rather than an anchor.
    for match in re.findall(r"""["']([^"'<> ]+(?:\.glb)(?:\?[^"'<> ]*)?)["']""", page, re.I):
        candidates.append(urllib.parse.urljoin(page_url, match.replace("\\/", "/")))

    # Prefer an actual GLB URL over a generic download/viewer route.
    candidates = list(dict.fromkeys(candidates))
    print("Candidate download links:")
    for url in candidates:
        print("  " + url)
    # Prefer the model-specific API download endpoint published by the page.
    for url in candidates:
        parsed = urllib.parse.urlparse(url)
        if "/api/library/" in parsed.path and parsed.path.rstrip("/").endswith("/download"):
            return url
    page_path = urllib.parse.urlparse(page_url).path
    for url in candidates:
        parsed = urllib.parse.urlparse(url)
        if re.search(r"\.glb(?:$|[?#])", url, re.I) and parsed.path != page_path:
            return url
    if candidates:
        return candidates[0]
    raise RuntimeError(f"No published GLB download link found on {page_url}")

def validate_glb(data: bytes, name: str) -> dict:
    if len(data) < 20 or data[:4] != b"glTF":
        raise RuntimeError(f"{name}: downloaded file is not a binary glTF (GLB)")
    version, declared_length = struct.unpack_from("<II", data, 4)
    if version != 2 or declared_length != len(data):
        raise RuntimeError(
            f"{name}: invalid GLB header (version={version}, "
            f"declared={declared_length}, actual={len(data)})"
        )
    chunk_length, chunk_type = struct.unpack_from("<II", data, 12)
    if chunk_type != 0x4E4F534A or 20 + chunk_length > len(data):
        raise RuntimeError(f"{name}: missing or invalid glTF JSON chunk")
    gltf = json.loads(data[20:20 + chunk_length].decode("utf-8").rstrip(" \t\r\n\0"))
    return {
        "bytes": len(data),
        "version": version,
        "meshes": len(gltf.get("meshes", [])),
        "materials": len(gltf.get("materials", [])),
        "textures": len(gltf.get("textures", [])),
        "animations": len(gltf.get("animations", [])),
        "extensions": gltf.get("extensionsUsed", []),
    }

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    manifest = []
    for item in SOURCES:
        print(f"Discovering: {item['title']}", flush=True)
        page = request(item["page"])
        download_url = discover_glb(item["page"], page)
        print(f"Downloading: {download_url}", flush=True)
        data = request(download_url)
        facts = validate_glb(data, item["file"])
        target = OUT / item["file"]
        target.write_bytes(data)
        manifest.append({**item, "download_url": download_url, **facts})
        print(f"Validated {target.name}: {facts}", flush=True)

    (OUT / "ASSET_MANIFEST.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Validated {len(manifest)} GLB assets.")

if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"3D asset acquisition failed: {exc}", file=sys.stderr)
        raise
