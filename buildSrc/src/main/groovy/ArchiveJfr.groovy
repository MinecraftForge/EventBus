import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

abstract class ArchiveJfr extends DefaultTask {
    @InputDirectory
    abstract DirectoryProperty getInput()

    @OutputDirectory
    abstract DirectoryProperty getOutput()

    @TaskAction
    void exec() throws IOException {
        def timestamp = new Date().format('yyyMMdd.HHmmss')
        for (def java : input.asFile.get().listFiles()) {
            for (def bench : java.listFiles()) {
                logger.lifecycle(bench.toString())
                def file = new File(bench, 'profile.jfr')
                if (file.exists()) {
                    def target = output.get().dir(timestamp).dir(java.name).file(bench.name.replace('net.minecraftforge.eventbus.benchmarks.', '') + '.jfr').getAsFile()
                    if (!target.parentFile.exists())
                        target.parentFile.mkdirs()
                    Files.copy(file.toPath(), target.toPath())
                    file.delete()
                }
                if (bench.listFiles().size() == 0)
                    bench.delete()
            }
            if (java.listFiles().size() == 0)
                java.delete()
        }
        if (output.asFile.get().listFiles().size() == 0)
            output.asFile.get().delete()
    }
}