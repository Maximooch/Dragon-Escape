import assert from "node:assert/strict";
import test from "node:test";
import { beginPointerLockedJoin } from "../lib/client/input";

test("join captures pointer synchronously before network connection starts", () => {
  const calls: string[] = [];
  beginPointerLockedJoin(
    () => calls.push("reset"),
    () => calls.push("capture"),
    () => calls.push("connect"),
  );
  assert.deepEqual(calls, ["reset", "capture", "connect"]);
});
