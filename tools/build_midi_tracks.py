#!/usr/bin/env python3
"""Author deterministic, original MIDI loops for Dragon Escape.

The browser synth consumes these standard MIDI files directly. Keeping the
score in MIDI makes the music tiny, editable in any DAW, and easy to replace
with recorded instruments later without changing game timing.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import struct


PPQ = 480


def variable_length(value: int) -> bytes:
    result = bytearray([value & 0x7F])
    value >>= 7
    while value:
        result.insert(0, (value & 0x7F) | 0x80)
        value >>= 7
    return bytes(result)


@dataclass(frozen=True)
class Note:
    beat: float
    duration: float
    pitch: int
    velocity: int
    channel: int = 0


def write_midi(path: Path, bpm: int, bars: int, notes: list[Note]) -> None:
    events: list[tuple[int, int, bytes]] = []
    tempo = 60_000_000 // bpm
    events.append((0, 0, b"\xff\x51\x03" + tempo.to_bytes(3, "big")))
    events.append((0, 0, b"\xff\x58\x04\x04\x02\x18\x08"))
    for note in notes:
        start = round(note.beat * PPQ)
        end = round((note.beat + note.duration) * PPQ)
        events.append((start, 2, bytes((0x90 | note.channel, note.pitch, note.velocity))))
        events.append((end, 1, bytes((0x80 | note.channel, note.pitch, 0))))
    events.append((bars * 4 * PPQ, 3, b"\xff\x2f\x00"))
    events.sort(key=lambda event: (event[0], event[1]))

    track = bytearray()
    previous = 0
    for tick, _, payload in events:
        track.extend(variable_length(tick - previous))
        track.extend(payload)
        previous = tick
    header = b"MThd" + struct.pack(">IHHH", 6, 0, 1, PPQ)
    chunk = b"MTrk" + struct.pack(">I", len(track)) + bytes(track)
    path.write_bytes(header + chunk)


def pulse(root: int, pattern: list[int], bars: int, *, channel: int, velocity: int) -> list[Note]:
    notes = []
    for step in range(bars * 8):
        pitch = root + pattern[step % len(pattern)]
        notes.append(Note(step * 0.5, 0.36, pitch, velocity, channel))
    return notes


def chords(roots: list[int], bars: int, *, channel: int = 1) -> list[Note]:
    notes = []
    for bar in range(bars):
        root = roots[bar % len(roots)]
        for offset in (0, 3, 7):
            notes.append(Note(bar * 4, 3.6, root + offset, 46, channel))
    return notes


def melody(pitches: list[int | None], *, beat: float = 0.5, channel: int = 2) -> list[Note]:
    return [Note(index * beat, beat * 0.72, pitch, 78, channel) for index, pitch in enumerate(pitches) if pitch is not None]


def main() -> None:
    output = Path(__file__).resolve().parents[1] / "public" / "music"
    output.mkdir(parents=True, exist_ok=True)

    # Menu / Salto: D minor ostinato, low fifths, and a terse warning motif.
    bars = 8
    write_midi(
        output / "embers-at-your-heels.mid",
        138,
        bars,
        pulse(38, [0, 0, 7, 3, 0, 10, 7, 3], bars, channel=0, velocity=62)
        + chords([50, 46, 43, 48], bars)
        + melody([62, 65, 69, None, 67, 65, 62, None] * 4),
    )

    # Caves: irregular low pulse and descending Phrygian answer.
    write_midi(
        output / "understone-pulse.mid",
        126,
        bars,
        pulse(34, [0, 1, 7, 0, 10, 7, 1, 0], bars, channel=0, velocity=66)
        + chords([46, 45, 41, 43], bars)
        + melody([58, None, 57, 53, 51, None, 48, 51] * 4),
    )

    # Frost Run: brittle high arpeggio over a determined minor bass.
    write_midi(
        output / "frozen-flight.mid",
        148,
        bars,
        pulse(41, [0, 7, 0, 3, 10, 7, 3, 0], bars, channel=0, velocity=58)
        + chords([53, 48, 50, 46], bars)
        + melody([77, 80, 84, 82, 80, 77, 75, 77] * 4),
    )

    # Archipel: wider intervals and a forward-driving suspended motif.
    write_midi(
        output / "isles-in-pursuit.mid",
        142,
        bars,
        pulse(36, [0, 7, 12, 7, 3, 10, 12, 7], bars, channel=0, velocity=60)
        + chords([48, 43, 46, 41], bars)
        + melody([67, 72, 70, 67, 65, 67, 63, None] * 4),
    )

    for path in sorted(output.glob("*.mid")):
        print(f"{path.name}: {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
