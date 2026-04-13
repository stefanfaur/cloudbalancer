#!/usr/bin/env python3
"""Normalize Repowise exports and hand-authored content into wiki/docs/."""
from __future__ import annotations
import shutil
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIR = REPO_ROOT / "docs"
MANUAL_DIR = REPO_ROOT / "wiki" / "manual"
TARGET_DIR = REPO_ROOT / "wiki" / "docs"

KNOWN_MODULES = {
    "dispatcher", "worker", "common", "worker-agent",
    "metrics-aggregator", "web-dashboard",
}

SKIP_PREFIXES = (".claude_worktrees_", ".claude")
SPECIAL_FILES = {
    "repo.md": "index.md",
    "ADA.md": "about.md",
    # architecture.md handled via wiki/manual/ copy, not Repowise export
}


@dataclass
class Route:
    source: Path
    target: Path
    warning: str | None = None


def discover() -> list[Path]:
    return [p for p in SOURCE_DIR.glob("*.md")
            if not any(p.name.startswith(sp) for sp in SKIP_PREFIXES)]


def route(path: Path) -> Route:
    name = path.name
    if name in SPECIAL_FILES:
        return Route(path, TARGET_DIR / SPECIAL_FILES[name])
    stem = name[:-3]  # strip .md
    # module-level doc: <module>.md
    if stem in KNOWN_MODULES:
        return Route(path, TARGET_DIR / stem / "index.md")
    # per-file: <module>_<path>.md
    for mod in sorted(KNOWN_MODULES, key=len, reverse=True):
        prefix = mod + "_"
        if stem.startswith(prefix):
            rest = stem[len(prefix):]
            # decode React __tests__ dir escape
            rest = rest.replace("___tests___", "/__tests__/")
            segments = rest.split("_")
            # reconstruct: last segment = filename, rest = dirs
            if len(segments) == 1:
                target = TARGET_DIR / mod / (segments[0] + ".md")
            else:
                *dirs, filename = segments
                # double-underscore-as-symbol heuristic:
                # if the original had __ (empty segment), merge with filename
                merged_filename = filename
                while dirs and dirs[-1] == "":
                    dirs.pop()
                    if dirs:
                        merged_filename = dirs.pop() + "__" + merged_filename
                target = TARGET_DIR / mod / Path(*dirs) / (merged_filename + ".md")
            return Route(path, target)
    # unparseable
    return Route(
        path,
        TARGET_DIR / "_unparsed" / name,
        warning=f"unparseable filename: {name}",
    )


def copy_manual() -> None:
    if not MANUAL_DIR.exists():
        return
    for src in MANUAL_DIR.rglob("*"):
        if src.is_file():
            rel = src.relative_to(MANUAL_DIR)
            dst = TARGET_DIR / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)


def normalize_h1(target: Path) -> None:
    """Strip '# File: path/to/thing' prefix so nav titles are clean."""
    text = target.read_text(encoding="utf-8")
    lines = text.splitlines()
    if lines and lines[0].startswith("# File: "):
        path_part = lines[0][len("# File: "):]
        filename = path_part.rsplit("/", 1)[-1]
        lines[0] = f"# {filename}"
        target.write_text("\n".join(lines) + "\n", encoding="utf-8")


def synthesize_module_index(module_dir: Path) -> None:
    idx = module_dir / "index.md"
    if idx.exists():
        return
    children = sorted(p.stem for p in module_dir.glob("*.md"))
    idx.write_text(
        f"# {module_dir.name}\n\nGenerated module index.\n",
        encoding="utf-8",
    )


def main() -> int:
    if TARGET_DIR.exists():
        shutil.rmtree(TARGET_DIR)
    TARGET_DIR.mkdir(parents=True)
    warnings: list[str] = []
    for src in discover():
        r = route(src)
        r.target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(r.source, r.target)
        normalize_h1(r.target)
        if r.warning:
            warnings.append(r.warning)
    copy_manual()
    for mod in KNOWN_MODULES:
        mdir = TARGET_DIR / mod
        if mdir.exists():
            synthesize_module_index(mdir)
    for w in warnings:
        print(f"WARN: {w}")
    print(f"Built {sum(1 for _ in TARGET_DIR.rglob('*.md'))} pages -> {TARGET_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
