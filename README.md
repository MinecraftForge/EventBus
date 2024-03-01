# EventBus

EventBus is designed to be a simple subscriber-publisher framework. Originally inspired by [Guava], with intentions to be faster by replacing reflection with generated accessor classes. EventBus is intended to be a generic library usable in any project. It has grown larger API/Feature/Complexity/Dependency wise then it was ever intended, and thus will be receiving some redesigns and simplification eventually. 

The major focus of EventBus is fast event dispatching. As such any changes should have benchmarks run. See [Benchmarking].

### Thanks
[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

YourKit for providing us access to their [YourKit Java Profiler] which helps identify bottlenecks and places for improvment.


[Guava]: https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/eventbus/EventBus.html
[Benchmarking]: https://github.com/MinecraftForge/EventBus/blob/master/Benchmarking.md
[YourKit Java Profiler]: https://www.yourkit.com/java/profiler/