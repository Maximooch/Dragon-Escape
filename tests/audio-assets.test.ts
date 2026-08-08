import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

for (const name of ["audio-check.wav", "dragon-roar.wav", "wing-beat.wav"]) {
  test(`${name} has an audible PCM signal`, async () => {
    const data = await readFile(new URL(`../public/audio/${name}`, import.meta.url));
    assert.equal(data.subarray(0, 4).toString("ascii"), "RIFF");
    assert.equal(data.subarray(8, 12).toString("ascii"), "WAVE");

    let sumSquares = 0;
    let peak = 0;
    let count = 0;
    for (let offset = 44; offset + 1 < data.length; offset += 2) {
      const sample = data.readInt16LE(offset) / 32768;
      sumSquares += sample * sample;
      peak = Math.max(peak, Math.abs(sample));
      count += 1;
    }
    assert.ok(Math.sqrt(sumSquares / count) > 0.1, "RMS should survive laptop speakers");
    assert.ok(peak > 0.75, "asset should be normalized near full scale");
  });
}
