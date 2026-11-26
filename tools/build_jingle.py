#!/usr/bin/env python3
"""Create a ~3-minute, gentle orchestral Jingle Bells arrangement (public-domain melody)."""
import os
import pretty_midi

TEMPO = 86
Q = 60.0 / TEMPO
BAR = 4 * Q
midi = pretty_midi.PrettyMIDI(initial_tempo=TEMPO, resolution=480)
tracks = {
    "piano": pretty_midi.Instrument(0, name="Warm piano"),
    "strings": pretty_midi.Instrument(48, name="Soft orchestral strings"),
    "flute": pretty_midi.Instrument(73, name="Flute melody"),
    "celesta": pretty_midi.Instrument(8, name="Celesta bells"),
    "bass": pretty_midi.Instrument(32, name="Acoustic bass"),
}
chords = [
    ([48,52,55],[60,64,67]), ([45,48,52],[57,60,64]),
    ([41,45,48],[53,57,60]), ([43,47,50],[55,59,62]),
    ([48,52,55],[60,64,67]), ([41,45,48],[53,57,60]),
    ([43,47,50],[55,59,62]), ([48,52,55],[60,64,67]),
]
melody = [64,64,64,None,64,64,64,None,64,67,60,62,64,None,None,None,
          65,65,65,65,65,64,64,64,64,67,67,65,62,60,None,None]

for cycle in range(8):
    for bar_index in range(8):
        t = (cycle * 8 + bar_index) * BAR
        low, upper = chords[bar_index]
        for beat, pitch in enumerate([low[0], upper[0], upper[1], upper[2]]):
            start = t + beat * Q
            tracks["piano"].notes.append(pretty_midi.Note(
                velocity=39 + (cycle % 3) * 2, pitch=pitch,
                start=start, end=start + Q * 0.86))
        root = low[0]
        for beat, pitch in ((0, root), (2, root + 7)):
            start = t + beat * Q
            tracks["bass"].notes.append(pretty_midi.Note(
                velocity=29 + (cycle % 2) * 2, pitch=pitch,
                start=start, end=start + Q * 1.65))
        if cycle >= 1:
            for pitch in upper:
                tracks["strings"].notes.append(pretty_midi.Note(
                    velocity=27 + (cycle % 2) * 3, pitch=pitch,
                    start=t, end=t + BAR * 0.96))
        if cycle in (1, 2, 4, 5, 7):
            for beat in range(4):
                pitch = melody[bar_index * 4 + beat]
                if pitch is None:
                    continue
                if cycle in (4, 7):
                    pitch += 12
                start = t + beat * Q
                tracks["flute"].notes.append(pretty_midi.Note(
                    velocity=43 if cycle < 4 else 40, pitch=pitch,
                    start=start, end=start + Q * (0.78 if beat < 3 else 0.68)))
        if bar_index in (0, 4) and cycle in (0, 2, 3, 5, 7):
            pitch = melody[bar_index * 4]
            tracks["celesta"].notes.append(pretty_midi.Note(
                velocity=27, pitch=pitch + 24, start=t, end=t + Q * 0.95))

# Gentle bell introduction, then sparse bell answers between the orchestral phrases.
for i, pitch in enumerate((72,76,79,84,79,76)):
    start = i * Q * 0.72
    tracks["celesta"].notes.append(pretty_midi.Note(
        velocity=30, pitch=pitch, start=start, end=start + Q * 1.35))
for cycle in range(8):
    start = cycle * 8 * BAR + 7 * BAR + 3 * Q
    tracks["celesta"].notes.append(pretty_midi.Note(
        velocity=23, pitch=84 if cycle % 2 == 0 else 79,
        start=start, end=start + Q * 1.2))

end = 64 * BAR
for pitch in (48,52,55,60,64,67):
    tracks["strings"].notes.append(pretty_midi.Note(
        velocity=25, pitch=pitch, start=end-BAR, end=end+BAR*0.8))
    tracks["piano"].notes.append(pretty_midi.Note(
        velocity=33, pitch=pitch, start=end-Q, end=end+BAR*0.75))

for instrument in tracks.values():
    midi.instruments.append(instrument)
out = "build/music/jingle_bells.mid"
os.makedirs(os.path.dirname(out), exist_ok=True)
midi.write(out)
print(f"Created three-minute orchestral score: {64 * BAR:.1f} seconds")
