export type ResumableAudioContext = {
  state: string;
  resume: () => Promise<void>;
};

export function unlockAudioContext<T extends ResumableAudioContext>(
  current: T | null,
  create: () => T,
) {
  const context = current ?? create();
  if (context.state !== "running") void context.resume();
  return context;
}
