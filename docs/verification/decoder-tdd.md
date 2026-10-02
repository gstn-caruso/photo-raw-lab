# Decoder TDD evidence

RgbImage owns dimensions and immutable RGB pixels; RawImageDecoder defines the domain service. LibRawDecoder owns native lifetime and translates native failures; ProcessedBitmap owns ABI validation and RGB conversion. Domain has no Swing or FFM dependencies.

Domain expectation: positive dimensions, matching pixel count and defensive copies. `mvn -B -pl domain test` first failed compilation because RgbImage did not exist; after implementing the contract, two tests passed. Overflow-sized dimensions are rejected using long multiplication.

Criterion: intention-revealing-selector (#4). Names express ownership and conversion rather than generic helpers. No initial tidying was needed in the empty source tree.

ProcessedBitmap expectation: copy unsigned RGB bytes to heap and reject null, non-bitmap, zero dimensions, incorrect channels/depth/size and overflow dimensions before accessing pixel data. `mvn -B -pl app -am test -DargLine=--enable-native-access=ALL-UNNAMED` first failed compilation because ProcessedBitmap did not exist; implementation passed both bitmap tests and both domain tests. ABI header source: https://raw.githubusercontent.com/LibRaw/LibRaw/0.21.5/libraw/libraw_types.h . Header reinterpretation reads only 16 bytes; payload reinterpretation follows validation using long arithmetic.
