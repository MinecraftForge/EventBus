import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

abstract class ArchiveJfr extends DefaultTask {
    @InputDirectory
    abstract DirectoryProperty getInput()

    @OutputDirectory
    abstract DirectoryProperty getOutput()

    @TaskAction
    void exec() throws IOException {
        for (def java : input.asFile.get().listFiles()) {
            for (def bench : java.listFiles()) {
                def file = new File(bench, 'profile.jfr')
                if (!file.exists())
                    continue
                def target = output.get().dir(java.name).file(bench.name.replace('net.minecraftforge.eventbus.benchmarks.', '') + '.jfr').getAsFile()
                if (!target.parentFile.exists())
                    target.parentFile.mkdirs()
                file.renameTo(target)
            }
        }
    }
}