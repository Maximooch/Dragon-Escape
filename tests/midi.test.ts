import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import { parseMidi } from "../lib/client/midi";

for (const name of ["embers-at-your-heels.mid", "understone-pulse.mid", "frozen-flight.mid", "isles-in-pursuit.mid"]) {
  test(`${name} is a playable MIDI loop`, async () => {
    const data = await readFile(new URL(`../public/music/${name}`, import.meta.url));
    assert.equal(data.subarray(0, 4).toString("ascii"), "MThd");
    const buffer = data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) as ArrayBuffer;
    const score = parseMidi(buffer);
    assert.ok(score.duration > 10 && score.duration < 30);
    assert.ok(score.notes.length >= 100);
    assert.ok(new Set(score.notes.map((note) => note.channel)).size >= 3);
  });
}
