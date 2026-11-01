import math
import struct
import wave
import os
import random

SAMPLE_RATE = 44100

class SynthHelper:
    @staticmethod
    def clamp(val, low=-1.0, high=1.0):
        return max(low, min(high, val))

    @staticmethod
    def soft_clip(x):
        # Soft saturation curve to prevent harsh digital clipping
        if x > 1.2: return 0.95
        if x < -1.2: return -0.95
        return x - (x ** 3) / 6.0

class BiquadFilter:
    """Standard 2-pole digital IIR filter."""
    def __init__(self, filter_type='lowpass', f0=1000.0, q=1.0, fs=SAMPLE_RATE):
        self.fs = fs
        self.set_params(filter_type, f0, q)
        self.x1 = self.x2 = self.y1 = self.y2 = 0.0

    def set_params(self, filter_type, f0, q):
        f0 = max(20.0, min(self.fs * 0.48, f0))
        omega = 2.0 * math.pi * f0 / self.fs
        alpha = math.sin(omega) / (2.0 * max(0.1, q))
        cos_w = math.cos(omega)

        if filter_type == 'lowpass':
            b0 = (1.0 - cos_w) / 2.0
            b1 = 1.0 - cos_w
            b2 = (1.0 - cos_w) / 2.0
            a0 = 1.0 + alpha
            a1 = -2.0 * cos_w
            a2 = 1.0 - alpha
        elif filter_type == 'bandpass':
            b0 = alpha
            b1 = 0.0
            b2 = -alpha
            a0 = 1.0 + alpha
            a1 = -2.0 * cos_w
            a2 = 1.0 - alpha
        elif filter_type == 'highpass':
            b0 = (1.0 + cos_w) / 2.0
            b1 = -(1.0 + cos_w)
            b2 = (1.0 + cos_w) / 2.0
            a0 = 1.0 + alpha
            a1 = -2.0 * cos_w
            a2 = 1.0 - alpha
        else:
            b0, b1, b2, a0, a1, a2 = 1, 0, 0, 1, 0, 0

        self.b0 = b0 / a0
        self.b1 = b1 / a0
        self.b2 = b2 / a0
        self.a1 = a1 / a0
        self.a2 = a2 / a0

    def process(self, x):
        y = self.b0 * x + self.b1 * self.x1 + self.b2 * self.x2 - self.a1 * self.y1 - self.a2 * self.y2
        self.x2 = self.x1
        self.x1 = x
        self.y2 = self.y1
        self.y1 = y
        return y

def save_wav(filepath, buffer_mono, sample_rate=SAMPLE_RATE):
    # Normalize with headroom
    peak = max(abs(s) for s in buffer_mono) if buffer_mono else 1.0
    gain = 0.90 / peak if peak > 0.001 else 1.0

    frames = bytearray()
    for s in buffer_mono:
        val = int(SynthHelper.soft_clip(s * gain) * 32767.0)
        clamped = max(-32767, min(32767, val))
        frames.extend(struct.pack('<h', clamped))

    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with wave.open(filepath, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sample_rate)
        w.writeframes(frames)

# ==========================================
# Sound Generators
# ==========================================

def gen_click():
    """Snappy tactile UI click (25ms)."""
    duration = 0.035
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    rnd = random.Random(42)
    bpf = BiquadFilter('bandpass', 1400.0, q=2.5)

    for i in range(n):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 120.0)
        # Fast descending chirp
        freq = 1800.0 * math.exp(-t * 90.0) + 400.0
        sine = math.sin(2.0 * math.pi * freq * t)
        noise = bpf.process(rnd.uniform(-1.0, 1.0)) * 0.4
        out[i] = (sine * 0.7 + noise) * env
    return out

def gen_tap():
    """Organic wooden block / marimba tap for arrow selection (60ms)."""
    duration = 0.070
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    for i in range(n):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 65.0)
        f0 = 480.0 * math.exp(-t * 30.0)
        # Fundamental + resonant harmonic
        s1 = math.sin(2.0 * math.pi * f0 * t)
        s2 = math.sin(2.0 * math.pi * (f0 * 2.1) * t) * 0.35
        s3 = math.sin(2.0 * math.pi * (f0 * 3.8) * t) * 0.15
        out[i] = (s1 + s2 + s3) * env
    return out

def gen_slide():
    """Aerodynamic smooth whoosh for arrow fly-out (220ms)."""
    duration = 0.220
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    rnd = random.Random(123)
    bpf = BiquadFilter('bandpass', 600.0, q=1.5)

    phase = 0.0
    for i in range(n):
        t = i / SAMPLE_RATE
        # Envelope: smooth attack ramp, gentle decay
        if t < 0.035:
            env = math.sin((t / 0.035) * (math.pi / 2.0))
        else:
            env = math.exp(-(t - 0.035) * 16.0)

        # Sweeping cutoff
        fc = 350.0 + 800.0 * (1.0 - math.exp(-t * 15.0))
        bpf.set_params('bandpass', fc, q=1.8)

        noise = bpf.process(rnd.uniform(-1.0, 1.0))

        # Tonal carrier
        freq = 280.0 + 380.0 * (t / duration)
        phase += 2.0 * math.pi * freq / SAMPLE_RATE
        tone = math.sin(phase) * 0.3

        out[i] = (noise * 0.75 + tone) * env
    return out

def gen_wrong():
    """Gentle low double-thud / collision error (200ms)."""
    duration = 0.200
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    for i in range(n):
        t = i / SAMPLE_RATE
        # First thud at 0ms, second subtle thud at 45ms
        env1 = math.exp(-t * 32.0)
        f1 = 145.0 * math.exp(-t * 20.0)
        s1 = math.sin(2.0 * math.pi * f1 * t) + 0.4 * math.sin(2.0 * math.pi * (f1 * 0.65) * t)

        s2 = 0.0
        if t > 0.045:
            t2 = t - 0.045
            env2 = math.exp(-t2 * 35.0)
            f2 = 120.0 * math.exp(-t2 * 25.0)
            s2 = math.sin(2.0 * math.pi * f2 * t2) * env2 * 0.7

        out[i] = (s1 * env1 + s2)
    return out

def gen_coin():
    """Sparkling dual-tone chime for coin reward (320ms)."""
    duration = 0.320
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # Note 1: B5 (987.77 Hz), Note 2: E6 (1318.51 Hz) after 30ms
    f_b5 = 987.77
    f_e6 = 1318.51

    for i in range(n):
        t = i / SAMPLE_RATE
        # First chime
        env1 = math.exp(-t * 14.0)
        s1 = math.sin(2.0 * math.pi * f_b5 * t) + 0.25 * math.sin(2.0 * math.pi * (f_b5 * 2.7) * t)

        # Second chime
        s2 = 0.0
        if t > 0.032:
            t2 = t - 0.032
            env2 = math.exp(-t2 * 12.0)
            s2 = math.sin(2.0 * math.pi * f_e6 * t2) + 0.3 * math.sin(2.0 * math.pi * (f_e6 * 2.5) * t2)
            s2 *= env2

        out[i] = (s1 * env1 * 0.6 + s2 * 0.8)
    return out

def gen_star():
    """Radiant high pentatonic sparkle for star reward (550ms)."""
    duration = 0.550
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    notes = [(0.00, 1046.50),  # C6
             (0.04, 1318.51),  # E6
             (0.08, 1567.98),  # G6
             (0.12, 2093.00)]  # C7

    for i in range(n):
        t = i / SAMPLE_RATE
        val = 0.0
        for delay, freq in notes:
            if t >= delay:
                dt = t - delay
                env = math.exp(-dt * 9.0)
                # Pure bell tone with shimmer
                vibrato = 1.0 + 0.003 * math.sin(2.0 * math.pi * 7.0 * dt)
                s = math.sin(2.0 * math.pi * freq * vibrato * dt)
                s += 0.2 * math.sin(2.0 * math.pi * (freq * 2.0) * dt)
                val += s * env * 0.35
        out[i] = val
    return out

def gen_win():
    """Triumphant 4-note victory fanfare with warm brass/chime resonance (1.2s)."""
    duration = 1.200
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # C Major Fanfare: C5 -> E5 -> G5 -> C6 chord
    arpeggio = [
        (0.000, 523.25, 0.25),  # C5
        (0.130, 659.25, 0.25),  # E5
        (0.260, 783.99, 0.25),  # G5
        (0.400, 1046.50, 0.70), # C6 (sustained root)
        (0.400, 523.25, 0.70),  # C5 bass support
        (0.400, 659.25, 0.70),  # E5 third
        (0.400, 783.99, 0.70)   # G5 fifth
    ]

    for i in range(n):
        t = i / SAMPLE_RATE
        val = 0.0
        for start, freq, length in arpeggio:
            if t >= start:
                dt = t - start
                if dt <= length:
                    env = math.sin(min(1.0, dt / 0.02) * (math.pi / 2.0)) * math.exp(-dt * 3.5)
                    # Warm harmonic blend (fundamental + octaves)
                    s = math.sin(2.0 * math.pi * freq * dt)
                    s += 0.35 * math.sin(2.0 * math.pi * (freq * 2.0) * dt)
                    s += 0.15 * math.sin(2.0 * math.pi * (freq * 3.0) * dt)
                    val += s * env * 0.25
        out[i] = val
    return out

def gen_fail():
    """Gentle descending minor triad for game over / out of lives (750ms)."""
    duration = 0.750
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # Minor descent: Eb5 -> C5 -> G4
    notes = [
        (0.000, 622.25, 0.28), # Eb5
        (0.180, 523.25, 0.28), # C5
        (0.360, 392.00, 0.36)  # G4
    ]

    for i in range(n):
        t = i / SAMPLE_RATE
        val = 0.0
        for start, freq, length in notes:
            if t >= start:
                dt = t - start
                if dt <= length:
                    env = math.sin(min(1.0, dt / 0.025) * (math.pi / 2.0)) * math.exp(-dt * 4.2)
                    s = math.sin(2.0 * math.pi * freq * dt) + 0.2 * math.sin(2.0 * math.pi * (freq * 2.0) * dt)
                    val += s * env * 0.38
        out[i] = val
    return out

def gen_hint():
    """Pleasant luminous magic chime for hint (380ms)."""
    duration = 0.380
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # Dual crystal tone: A5 (880Hz) + E6 (1318Hz) with soft shimmer
    for i in range(n):
        t = i / SAMPLE_RATE
        env = math.sin(min(1.0, t / 0.025) * (math.pi / 2.0)) * math.exp(-t * 9.5)
        s1 = math.sin(2.0 * math.pi * 880.0 * t)
        s2 = math.sin(2.0 * math.pi * 1318.5 * t) * 0.65
        s3 = math.sin(2.0 * math.pi * 2637.0 * t) * 0.25
        out[i] = (s1 + s2 + s3) * env * 0.5
    return out

def gen_hammer():
    """Tactile heavy impact punch for hammer booster (280ms)."""
    duration = 0.280
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    rnd = random.Random(999)

    for i in range(n):
        t = i / SAMPLE_RATE
        # Deep sub-punch starting at 190Hz dropping to 55Hz
        f = 190.0 * math.exp(-t * 25.0)
        env = math.exp(-t * 18.0)
        body = math.sin(2.0 * math.pi * f * t)

        # Crisp surface crack (first 15ms)
        crack = 0.0
        if t < 0.018:
            c_env = math.exp(-t * 180.0)
            crack = rnd.uniform(-1.0, 1.0) * c_env * 0.8

        out[i] = body * env * 0.85 + crack
    return out

def gen_booster():
    """Magical ascending whoosh with sparkle sweep (420ms)."""
    duration = 0.420
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    rnd = random.Random(777)
    bpf = BiquadFilter('bandpass', 600.0, q=2.0)

    for i in range(n):
        t = i / SAMPLE_RATE
        # Ascending sweep from 350Hz up to 1800Hz
        sweep_f = 350.0 + 1450.0 * (t / duration) ** 1.5
        bpf.set_params('bandpass', sweep_f, q=2.5)

        env = math.sin((t / duration) * math.pi)
        noise = bpf.process(rnd.uniform(-1.0, 1.0)) * 0.6
        sine = math.sin(2.0 * math.pi * sweep_f * t) * 0.4

        # High sparkle accents
        sparkle = 0.0
        if t > 0.15:
            sp_t = t - 0.15
            sparkle = math.sin(2.0 * math.pi * 2400.0 * sp_t) * math.exp(-sp_t * 12.0) * 0.25

        out[i] = (noise + sine + sparkle) * env
    return out

def gen_dot():
    """Soft subtle water droplet / bubble pop (50ms)."""
    duration = 0.050
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    for i in range(n):
        t = i / SAMPLE_RATE
        env = math.exp(-t * 70.0)
        # Upward bubble chirp (650Hz -> 1150Hz)
        f = 650.0 + 500.0 * (t / duration)
        out[i] = math.sin(2.0 * math.pi * f * t) * env
    return out

def gen_medal_popup():
    """Celebratory rank medal popup fanfare (600ms)."""
    duration = 0.600
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n

    # Chord burst: G5 (784Hz) -> C6 (1046Hz) + E6 (1318Hz) chord
    notes = [
        (0.000, 783.99, 0.20),
        (0.080, 1046.50, 0.50),
        (0.080, 1318.51, 0.50),
        (0.080, 1567.98, 0.50)
    ]
    for i in range(n):
        t = i / SAMPLE_RATE
        val = 0.0
        for start, freq, length in notes:
            if t >= start:
                dt = t - start
                if dt <= length:
                    env = math.sin(min(1.0, dt / 0.02) * (math.pi / 2.0)) * math.exp(-dt * 6.0)
                    s = math.sin(2.0 * math.pi * freq * dt) + 0.25 * math.sin(2.0 * math.pi * (freq * 2.0) * dt)
                    val += s * env * 0.30
        out[i] = val
    return out

# ==========================================
# Combo Push Generators (Ascending Pentatonic Whooshes)
# ==========================================

# 9-step ascending pentatonic scale:
# Tier 1 (push1_1 to push1_4): C4, D4, E4, G4
# Tier 2 (push2_1 to push2_3): A4, C5, D5
# Tier 3 (push3_1 to push3_2): E5, G5
PUSH_STEPS = [
    # step, f_tone, whoosh_start, whoosh_end, duration
    ('push1_1', 261.63, 240.0, 520.0, 0.170), # C4
    ('push1_2', 293.66, 270.0, 580.0, 0.175), # D4
    ('push1_3', 329.63, 300.0, 650.0, 0.180), # E4
    ('push1_4', 392.00, 340.0, 750.0, 0.185), # G4
    ('push2_1', 440.00, 380.0, 880.0, 0.190), # A4
    ('push2_2', 523.25, 430.0, 1020.0, 0.195),# C5
    ('push2_3', 587.33, 480.0, 1150.0, 0.200),# D5
    ('push3_1', 659.25, 540.0, 1320.0, 0.210),# E5
    ('push3_2', 783.99, 600.0, 1500.0, 0.220),# G5
]

def gen_push_combo(name, f_tone, w_start, w_end, duration, tier):
    n = int(SAMPLE_RATE * duration)
    out = [0.0] * n
    rnd = random.Random(hash(name) & 0xffffffff)
    bpf = BiquadFilter('bandpass', w_start, q=1.6)

    phase = 0.0
    for i in range(n):
        t = i / SAMPLE_RATE
        # Envelope: punchy 15ms attack, smooth ring-out
        if t < 0.015:
            env = math.sin((t / 0.015) * (math.pi / 2.0))
        else:
            decay_rate = 14.0 - (tier * 1.5) # higher tiers ring slightly longer
            env = math.exp(-(t - 0.015) * decay_rate)

        # Sweeping whoosh
        fc = w_start + (w_end - w_start) * (t / duration)
        bpf.set_params('bandpass', fc, q=2.0)
        noise = bpf.process(rnd.uniform(-1.0, 1.0)) * 0.45

        # Melodic bell/marimba tone
        s = math.sin(2.0 * math.pi * f_tone * t)
        # Harmonic overtone
        s += 0.3 * math.sin(2.0 * math.pi * (f_tone * 2.0) * t)
        if tier >= 2:
            # Add shimmer for higher combos
            s += 0.15 * math.sin(2.0 * math.pi * (f_tone * 3.0) * t)

        out[i] = (noise * 0.55 + s * 0.65) * env
    return out

def main():
    dest_dir = r"arrow_native\app\src\main\res\raw"
    generated_files = []

    # 1. SFX files
    sfx_map = {
        'sfx_click.wav': gen_click(),
        'sfx_tap.wav': gen_tap(),
        'sfx_slide.wav': gen_slide(),
        'sfx_wrong.wav': gen_wrong(),
        'sfx_coin.wav': gen_coin(),
        'sfx_star.wav': gen_star(),
        'sfx_win.wav': gen_win(),
        'sfx_fail.wav': gen_fail(),
        'sfx_hint.wav': gen_hint(),
        'sfx_hammer.wav': gen_hammer(),
        'sfx_booster.wav': gen_booster(),
        'sfx_dot.wav': gen_dot(),
        'rank_medal_popup.wav': gen_medal_popup()
    }

    # 2. Push combo files (9 files)
    for name, f_tone, w_start, w_end, dur in PUSH_STEPS:
        tier = 1 if 'push1' in name else (2 if 'push2' in name else 3)
        sfx_map[f"{name}.wav"] = gen_push_combo(name, f_tone, w_start, w_end, dur, tier)

    print(f"Total audio files to generate: {len(sfx_map)}")
    for filename, buffer in sfx_map.items():
        out_path = os.path.join(dest_dir, filename)
        save_wav(out_path, buffer)
        sz = os.path.getsize(out_path)
        print(f"  Generated {filename:22s} ({sz:6d} bytes)")
        generated_files.append(filename)

    # Clean up old .mp3 files in res/raw to avoid duplicate resource names
    for f in os.listdir(dest_dir):
        if f.endswith('.mp3'):
            base = f[:-4]
            # If we now have base.wav, remove base.mp3
            wav_name = base + '.wav'
            if wav_name in sfx_map:
                mp3_path = os.path.join(dest_dir, f)
                os.remove(mp3_path)
                print(f"  Removed obsolete ripped MP3: {f}")

    print(f"\nAll {len(generated_files)} audio files successfully created and old MP3s cleaned!")

if __name__ == '__main__':
    main()
