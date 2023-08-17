__Running the benchmark__

The EventBus has JMH Benchmarks.
The gradle jmh tasks run them, but you need to either disable the daemon with `--no-daemon` or stop it before running it again, because otherwise the next benchmark will crash.

Due to windows command argument length limitations, you also need to have the JVM arg jmh.separateClasspathJAR set by default to work around this issue.
This requires the project folder to be on the same partition as your gradle cache folder, though.

If you are a linux/macos user, you can try disabling the flag in the build.gradle jmh section to put this project folder on a different partition than the gradle cache.
