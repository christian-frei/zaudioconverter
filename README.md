# zaudioconvert

Converts E-mu Emulator X / Proteus X `.ebl` samples to 16-bit PCM `.wav` files. Zero-dependency, single-file Java script.

## Prerequisites

A Java 25+ JDK with `java` on the `PATH` — the script has no build step and runs via the `#!/usr/bin/env -S java --source 25` shebang. A JRE-only runtime is not sufficient: source-file mode compiles in memory and requires the `jdk.compiler` module.

```
java -version
```

## Install

```
chmod +x zaudioconvert
sudo ln -s $(pwd)/zaudioconvert /usr/local/bin/zaudioconvert
```

## Usage

```
zaudioconvert <file.ebl|directory>... [-out <directory>]
```

- Directories are searched recursively for `.ebl` files
- Without `-out`, each `.wav` is written next to its source; with `-out`, all files go into one directory (equal file names overwrite each other)
- Loop points of looped samples are written as a `smpl` chunk (unity note 60)
- Exits with `1` if any file fails; remaining files are still converted

## EBL Format

The format is undocumented; the parser is reverse-engineered:

- IFF container: `FORM` / `E5B0`, a `TOC2` chunk, and one `E5S1` sample chunk
- `E5S1` holds the UTF-16LE sample name, little-endian start/end/loop byte addresses per channel, sample rate, flags, and raw 16-bit little-endian PCM
- A sample is treated as stereo when left and right addresses differ — not yet verified against a real stereo file

## Test

```
zunit
```

Runs [zunit](https://github.com/AdamBien/zunit) from the project root. The test launches the script as a process against `test/resources/` and verifies the WAV header, the PCM payload byte-for-byte, and the loop points.
