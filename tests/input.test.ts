import assert from "node:assert/strict";
import test from "node:test";
import { beginPointerLockedJoin } from "../lib/client/input";
import { joystickAxes, movementFromAxes, touchLookRotation } from "../lib/client/touch";

test("join captures pointer synchronously before network connection starts", () => {
  const calls: string[] = [];
  beginPointerLockedJoin(
    () => calls.push("reset"),
    () => calls.push("capture"),
    () => calls.push("connect"),
  );
  assert.deepEqual(calls, ["reset", "capture", "connect"]);
});

test("touch joystick clamps movement and preserves its direction", () => {
  const bounds = { left: 100, top: 200, width: 120, height: 120 };
  assert.deepEqual(joystickAxes(160, 260, bounds), { strafe: 0, forward: 0 });
  const axes = joystickAxes(260, 160, bounds);
  assert.ok(Math.hypot(axes.strafe, axes.forward) <= 1.000001);
  assert.ok(axes.strafe > 0);
  assert.ok(axes.forward > 0);
  assert.deepEqual(movementFromAxes(axes), { forward: true, back: false, left: false, right: true });
});

test("touch look follows the desktop camera handedness and clamps pitch", () => {
  const rotation = touchLookRotation(0, 0, 20, -1000);
  assert.ok(rotation.yaw < 0);
  assert.equal(rotation.pitch, 1.2);
});
