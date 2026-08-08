export type ResumableAudioContext = {
  state: string;
  resume: () => Promise<void>;
};

export function unlockAudioContext<T extends ResumableAudioContext>(
  current: T | null,
  create: () => T,
  onRejected: () => void = () => undefined,
) {
  const context = current ?? create();
  if (context.state !== "running") void context.resume().catch(onRejected);
  return context;
}
