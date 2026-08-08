export type MidiNote = {
  start: number;
  duration: number;
  pitch: number;
  velocity: number;
  channel: number;
};

export type MidiScore = { duration: number; notes: MidiNote[] };

function readVariableLength(view: DataView, offset: number) {
  let value = 0;
  let cursor = offset;
  for (let count = 0; count < 4; count += 1) {
    const byte = view.getUint8(cursor++);
    value = (value << 7) | (byte & 0x7f);
    if ((byte & 0x80) === 0) return { value, offset: cursor };
  }
  throw new Error("Invalid MIDI variable-length value");
}

export function parseMidi(buffer: ArrayBuffer): MidiScore {
  const view = new DataView(buffer);
  const text = (offset: number, length: number) => String.fromCharCode(...new Uint8Array(buffer, offset, length));
  if (text(0, 4) !== "MThd" || view.getUint32(4) < 6) throw new Error("Invalid MIDI header");
  const trackCount = view.getUint16(10);
  const division = view.getUint16(12);
  if (division & 0x8000) throw new Error("SMPTE MIDI timing is not supported");

  let cursor = 8 + view.getUint32(4);
  let tempo = 500_000;
  const rawNotes: Array<{ tick: number; durationTicks: number; pitch: number; velocity: number; channel: number }> = [];
  let finalTick = 0;
  for (let trackIndex = 0; trackIndex < trackCount; trackIndex += 1) {
    if (text(cursor, 4) !== "MTrk") throw new Error("Invalid MIDI track");
    const end = cursor + 8 + view.getUint32(cursor + 4);
    cursor += 8;
    let tick = 0;
    let runningStatus = 0;
    const active = new Map<string, { tick: number; velocity: number }>();
    while (cursor < end) {
      const delta = readVariableLength(view, cursor);
      tick += delta.value;
      finalTick = Math.max(finalTick, tick);
      cursor = delta.offset;
      let status = view.getUint8(cursor);
      if (status & 0x80) {
        cursor += 1;
        runningStatus = status;
      } else {
        status = runningStatus;
      }
      if (status === 0xff) {
        const type = view.getUint8(cursor++);
        const length = readVariableLength(view, cursor);
        cursor = length.offset;
        if (type === 0x51 && length.value === 3) tempo = view.getUint8(cursor) * 65_536 + view.getUint8(cursor + 1) * 256 + view.getUint8(cursor + 2);
        cursor += length.value;
        continue;
      }
      if (status === 0xf0 || status === 0xf7) {
        const length = readVariableLength(view, cursor);
        cursor = length.offset + length.value;
        continue;
      }
      const command = status & 0xf0;
      const channel = status & 0x0f;
      const data1 = view.getUint8(cursor++);
      const data2 = command === 0xc0 || command === 0xd0 ? 0 : view.getUint8(cursor++);
      const key = `${channel}:${data1}`;
      if (command === 0x90 && data2 > 0) {
        active.set(key, { tick, velocity: data2 });
      } else if (command === 0x80 || (command === 0x90 && data2 === 0)) {
        const started = active.get(key);
        if (started) {
          rawNotes.push({ tick: started.tick, durationTicks: Math.max(1, tick - started.tick), pitch: data1, velocity: started.velocity, channel });
          active.delete(key);
        }
      }
    }
    cursor = end;
  }
  const secondsPerTick = tempo / 1_000_000 / division;
  return {
    duration: Math.max(0.1, finalTick * secondsPerTick),
    notes: rawNotes.map((note) => ({
      start: note.tick * secondsPerTick,
      duration: note.durationTicks * secondsPerTick,
      pitch: note.pitch,
      velocity: note.velocity,
      channel: note.channel,
    })),
  };
}

export class MidiMusicPlayer {
  private context: AudioContext;
  private output: GainNode;
  private sources = new Set<OscillatorNode>();
  private loopTimer: ReturnType<typeof setTimeout> | null = null;
  private generation = 0;

  constructor(context: AudioContext) {
    this.context = context;
    this.output = context.createGain();
    this.output.gain.value = 0.16;
    this.output.connect(context.destination);
  }

  async play(url: string) {
    const generation = ++this.generation;
    this.stopSources();
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Unable to load MIDI: ${response.status}`);
    const score = parseMidi(await response.arrayBuffer());
    if (generation !== this.generation) return;
    this.scheduleLoop(score, generation);
  }

  stop() {
    this.generation += 1;
    this.stopSources();
  }

  destroy() {
    this.stop();
    this.output.disconnect();
  }

  private stopSources() {
    if (this.loopTimer) clearTimeout(this.loopTimer);
    this.loopTimer = null;
    for (const source of this.sources) {
      try { source.stop(); } catch { /* already stopped */ }
      source.disconnect();
    }
    this.sources.clear();
  }

  private scheduleLoop(score: MidiScore, generation: number) {
    if (generation !== this.generation) return;
    const begins = this.context.currentTime + 0.06;
    for (const note of score.notes) this.scheduleNote(note, begins);
    this.loopTimer = setTimeout(() => this.scheduleLoop(score, generation), Math.max(100, (score.duration - 0.08) * 1000));
  }

  private scheduleNote(note: MidiNote, begins: number) {
    const oscillator = this.context.createOscillator();
    const gain = this.context.createGain();
    oscillator.type = note.channel === 0 ? "square" : note.channel === 1 ? "sawtooth" : "triangle";
    oscillator.frequency.value = 440 * 2 ** ((note.pitch - 69) / 12);
    const start = begins + note.start;
    const end = start + Math.max(0.04, note.duration);
    const level = (note.velocity / 127) * (note.channel === 1 ? 0.035 : 0.055);
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(level, start + 0.012);
    gain.gain.setValueAtTime(level, Math.max(start + 0.013, end - 0.045));
    gain.gain.exponentialRampToValueAtTime(0.0001, end);
    oscillator.connect(gain).connect(this.output);
    oscillator.start(start);
    oscillator.stop(end + 0.01);
    this.sources.add(oscillator);
    oscillator.onended = () => {
      this.sources.delete(oscillator);
      oscillator.disconnect();
      gain.disconnect();
    };
  }
}
