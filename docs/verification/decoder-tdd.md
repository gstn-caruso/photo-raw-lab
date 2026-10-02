# Decoder TDD evidence

RgbImage owns dimensions and immutable RGB pixels; RawImageDecoder defines the domain service. LibRawDecoder owns native lifetime and translates native failures; ProcessedBitmap owns ABI validation and RGB conversion. Domain has no Swing or FFM dependencies.

Domain expectation: positive dimensions, matching pixel count and defensive copies. `mvn -B -pl domain test` first failed compilation because RgbImage did not exist; after implementing the contract, two tests passed. Overflow-sized dimensions are rejected using long multiplication.

Criterion: intention-revealing-selector (#4). Names express ownership and conversion rather than generic helpers. No initial tidying was needed in the empty source tree.
