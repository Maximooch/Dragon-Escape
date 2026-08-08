#!/usr/bin/env python3
"""Generate deterministic prototype WAV cues with laptop-speaker-safe energy."""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path

RATE = 44_100


def write_wav(path: Path, samples: list[float]) -> None:
    peak = max(abs(value) for value in samples) or 1
    gain = 0.82 / peak
    pcm = b"".join(struct.pack("<h", max(-32767, min(32767, int(value * gain * 32767)))) for value in samples)
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(pcm)


def check_cue() -> list[float]:
    duration = 0.24
    result = []
    for index in range(int(RATE * duration)):
        t = index / RATE
        envelope = math.sin(math.pi * min(1, t / duration)) ** 0.55
        frequency = 620 + 330 * (t / duration)
        result.append(envelope * (math.sin(2 * math.pi * frequency * t) + 0.28 * math.sin(2 * math.pi * frequency * 2 * t)))
    return result


def dragon_roar() -> list[float]:
    rng = random.Random(1701)
    duration = 1.85
    result = []
    brown = 0.0
    previous_noise = 0.0
    for index in range(int(RATE * duration)):
        t = index / RATE
        attack = min(1, t / 0.09)
        release = min(1, (duration - t) / 0.52)
        envelope = max(0, attack * release) ** 0.72
        wobble = math.sin(2 * math.pi * 4.2 * t) * 10
        fundamental = 105 - 28 * (t / duration) + wobble
        voiced = (
            0.55 * math.sin(2 * math.pi * fundamental * t)
            + 0.42 * math.sin(2 * math.pi * fundamental * 1.97 * t)
            + 0.30 * math.sin(2 * math.pi * fundamental * 3.08 * t)
            + 0.18 * math.sin(2 * math.pi * fundamental * 5.2 * t)
        )
        white = rng.uniform(-1, 1)
        brown = brown * 0.985 + white * 0.015
        high_noise = white - previous_noise * 0.72
        previous_noise = white
        rasp = brown * 2.3 + high_noise * (0.13 + 0.16 * math.sin(math.pi * t / duration))
        result.append(envelope * (voiced + rasp))
    return result


def wing_beat() -> list[float]:
    rng = random.Random(4242)
    duration = 0.48
    result = []
    low = 0.0
    for index in range(int(RATE * duration)):
        t = index / RATE
        impulse = math.sin(math.pi * t / duration) ** 1.7
        white = rng.uniform(-1, 1)
        low = low * 0.91 + white * 0.09
        body = math.sin(2 * math.pi * (145 - 55 * t / duration) * t)
        result.append(impulse * (low * 1.7 + body * 0.42))
    return result


def main() -> None:
    output = Path(__file__).resolve().parents[1] / "public" / "audio"
    output.mkdir(parents=True, exist_ok=True)
    write_wav(output / "audio-check.wav", check_cue())
    write_wav(output / "dragon-roar.wav", dragon_roar())
    write_wav(output / "wing-beat.wav", wing_beat())


if __name__ == "__main__":
    main()
