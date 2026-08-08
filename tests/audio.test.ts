import assert from "node:assert/strict";
import test from "node:test";
import { unlockAudioContext } from "../lib/client/audio";

test("audio is created and resumed inside the user gesture", () => {
  const calls: string[] = [];
  const context = {
    state: "suspended",
    resume: async () => { calls.push("resume"); },
  };

  const result = unlockAudioContext(null, () => {
    calls.push("create");
    return context;
  });

  assert.equal(result, context);
  assert.deepEqual(calls, ["create", "resume"]);
});

test("an already running audio context is not resumed again", () => {
  const calls: string[] = [];
  const context = {
    state: "running",
    resume: async () => { calls.push("resume"); },
  };

  assert.equal(unlockAudioContext(context, () => context), context);
  assert.deepEqual(calls, []);
});
