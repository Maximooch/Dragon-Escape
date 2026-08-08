import assert from "node:assert/strict";
import test from "node:test";
import { configureSceneFog } from "../lib/client/rendering";

test("fog configuration works when the scene fog property is read-only", () => {
  const fog = { type: "none", color: "black", density: 0 };
  const scene = Object.defineProperty({}, "fog", {
    enumerable: true,
    get: () => fog,
  }) as { readonly fog: typeof fog };

  configureSceneFog(scene, "exp2", "purple", 0.0018);

  assert.deepEqual(fog, { type: "exp2", color: "purple", density: 0.0018 });
});
