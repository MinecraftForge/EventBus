__Running the benchmark__

The EventBus has JMH Benchmarks.
The gradle `jmh` task runs them, and coolates all data into a easily understandable markdown file `jmh_results.md`, as well as provides the `jmh_data_output.json` file which can be renamed to `jmh_data_input.json`. Doing so will cause the markdown file to have easily understandable comparisons. This makes creating reports of changes before and after a commit/refactor really easy.

By default it will run the JMH tests using a single java toolchain. If you specify the `bulk_tests` property it will instead run the tests multiple times using different java distributers and versions. This is not recommended unless you have a lot of time to kill. But it may be useful to verify implementation performance across multiple java versions.