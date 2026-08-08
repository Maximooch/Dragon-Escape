export type TouchAxes = { strafe: number; forward: number };

export type StickBounds = {
  left: number;
  top: number;
  width: number;
  height: number;
};

export function joystickAxes(clientX: number, clientY: number, bounds: StickBounds): TouchAxes {
  const centerX = bounds.left + bounds.width / 2;
  const centerY = bounds.top + bounds.height / 2;
  const radius = Math.max(1, Math.min(bounds.width, bounds.height) * 0.36);
  const rawX = (clientX - centerX) / radius;
  const rawForward = (centerY - clientY) / radius;
  const length = Math.hypot(rawX, rawForward);
  const scale = length > 1 ? 1 / length : 1;
  return { strafe: rawX * scale, forward: rawForward * scale };
}

export function movementFromAxes(axes: TouchAxes, deadZone = 0.16) {
  return {
    forward: axes.forward > deadZone,
    back: axes.forward < -deadZone,
    left: axes.strafe < -deadZone,
    right: axes.strafe > deadZone,
  };
}

export function touchLookRotation(
  yaw: number,
  pitch: number,
  deltaX: number,
  deltaY: number,
  sensitivity = 0.0042,
) {
  return {
    yaw: yaw - deltaX * sensitivity,
    pitch: Math.max(-1.35, Math.min(1.2, pitch - deltaY * sensitivity * 0.88)),
  };
}
