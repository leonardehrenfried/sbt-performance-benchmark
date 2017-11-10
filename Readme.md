# sbt performance benchmark

A work in progress repository for benchmarking sbt versions.

To run the benchmark run `sbt run`.

This will write a JSON file to the `reports` directory.

## Projects under test

Right now there is only a single sbt project under test ([source](https://github.com/cakesolutions/sbt-cake/tree/sbt-perf-regression)). It contains a large
classpath and many submodules but has no source files to compile.

As of 1.0.3 this is pathologically slow as all dependent JARs are hashed with 
SHA-1. If two submodules share a dependency, it is hashed twice.

## Future work

- Write UI to display results
- Run commands in a loop and average results

