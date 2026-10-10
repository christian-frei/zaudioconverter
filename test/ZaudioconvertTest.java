/// Black-box test: the script is a single extension-less source file and cannot be
/// put on a classpath, so it is launched as a process. Run from the project root:
/// `zunit` or `java -ea test/ZaudioconvertTest.java`
static final Path SCRIPT = Path.of("zaudioconvert");
static final Path EBL = Path.of("test/resources/AKAI - BASS LegendsSL001.ebl");

// taken from a hex dump of the resource, independent of the script's parser
static final int PCM_OFFSET = 0x124;
static final int FRAMES = 305_993;
static final int LOOP_START = 0;
static final int LOOP_END = 302_249;
static final int SAMPLE_RATE = 44_100;

void main() throws Exception {
    convertsEbl();
    rejectsNonEbl();
}

void convertsEbl() throws Exception {
    var out = Files.createTempDirectory("zaudioconvert");
    var exitCode = run(EBL.toString(), "-out", out.toString());
    assert exitCode == 0 : "expected exit code 0 but got " + exitCode;

    var wav = ByteBuffer.wrap(Files.readAllBytes(out.resolve("AKAI - BASS LegendsSL001.wav"))).order(ByteOrder.LITTLE_ENDIAN);
    assertId(wav, 0, "RIFF");
    assert wav.getInt(4) == wav.capacity() - 8 : "RIFF size %d does not match file size %d".formatted(wav.getInt(4), wav.capacity());
    assertId(wav, 8, "WAVE");
    assertId(wav, 12, "fmt ");
    assertEquals("format", 1, wav.getShort(20));
    assertEquals("channels", 1, wav.getShort(22));
    assertEquals("sample rate", SAMPLE_RATE, wav.getInt(24));
    assertEquals("bits per sample", 16, wav.getShort(34));
    assertId(wav, 36, "data");
    var dataSize = wav.getInt(40);
    assertEquals("data size", FRAMES * 2, dataSize);

    var ebl = Files.readAllBytes(EBL);
    var expectedPcm = Arrays.copyOfRange(ebl, PCM_OFFSET, PCM_OFFSET + dataSize);
    var actualPcm = Arrays.copyOfRange(wav.array(), 44, 44 + dataSize);
    assert Arrays.equals(expectedPcm, actualPcm) : "PCM differs from source at index " + Arrays.mismatch(expectedPcm, actualPcm);

    var smpl = 44 + dataSize;
    assertId(wav, smpl, "smpl");
    assertEquals("loop count", 1, wav.getInt(smpl + 36));
    assertEquals("loop start", LOOP_START, wav.getInt(smpl + 52));
    assertEquals("loop end", LOOP_END, wav.getInt(smpl + 56));
}

void rejectsNonEbl() throws Exception {
    var notEbl = Files.createTempFile("zaudioconvert", ".ebl");
    Files.writeString(notEbl, "RIFF this is not an E-mu file");
    var exitCode = run(notEbl.toString(), "-out", notEbl.getParent().toString());
    assert exitCode == 1 : "expected exit code 1 for invalid input but got " + exitCode;
}

int run(String... args) throws Exception {
    var command = Stream.concat(Stream.of(SCRIPT.toAbsolutePath().toString()), Stream.of(args)).toList();
    var process = new ProcessBuilder(command)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new AssertionError("%s did not finish within 30s".formatted(SCRIPT));
    }
    return process.exitValue();
}

void assertId(ByteBuffer buffer, int offset, String expected) {
    var actual = new String(buffer.array(), offset, 4, StandardCharsets.US_ASCII);
    assert expected.equals(actual) : "expected chunk %s at %d but got %s".formatted(expected, offset, actual);
}

void assertEquals(String what, int expected, int actual) {
    assert expected == actual : "expected %s %d but got %d".formatted(what, expected, actual);
}
