import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const root = new URL("../", import.meta.url);

test("low-height landscape play uses a compact, safe-area-aware HUD", async () => {
  const css = await readFile(new URL("app/globals.css", root), "utf8");

  assert.match(css, /@media \(orientation: landscape\) and \(max-height: 600px\)/);
  assert.match(css, /\.topbar\s*\{[^}]*height:\s*calc\(48px \+ env\(safe-area-inset-top\)\)/s);
  assert.match(css, /\.race-card\s*\{[^}]*width:\s*210px/s);
  assert.match(css, /\.footer-note\s*\{\s*display:\s*none/s);
  assert.match(css, /\.touch-mode \.touch-move-pad\s*\{[^}]*width:\s*92px[^}]*height:\s*92px/s);
});

test("portrait rotation guidance sits below the race HUD", async () => {
  const css = await readFile(new URL("app/globals.css", root), "utf8");

  assert.match(css, /@media \(orientation: portrait\) and \(pointer: coarse\)/);
  assert.match(css, /\.rotate-hint\s*\{[^}]*top:\s*calc\(238px \+ env\(safe-area-inset-top\)\)/s);
});

test("roadmap treats mobile as a supported release surface", async () => {
  const roadmap = await readFile(new URL("docs/roadmap.md", root), "utf8");

  assert.match(roadmap, /## Mobile roadmap/);
  assert.match(roadmap, /orientation changes/i);
  assert.match(roadmap, /thermal/i);
  assert.match(roadmap, /handedness/i);
});
