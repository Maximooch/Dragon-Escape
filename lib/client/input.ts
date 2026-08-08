export function beginPointerLockedJoin(
  resetRace: () => void,
  capturePointer: () => void,
  connect: () => void,
) {
  resetRace();
  capturePointer();
  connect();
}
