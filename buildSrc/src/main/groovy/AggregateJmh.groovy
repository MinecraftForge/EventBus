import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.steppschuh.markdowngenerator.table.Table
import org.apache.tools.ant.types.selectors.SelectSelector
import org.gradle.api.provider.Property

import java.math.RoundingMode
import org.gradle.api.tasks.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.DefaultTask

abstract class AggregateJmh extends DefaultTask {
    @OutputFile
    abstract RegularFileProperty getOutput()

    @InputFile
    @Optional
    abstract RegularFileProperty getInputData()

    @Input
    abstract Property<String> getTimeUnit();

    @Input @Optional
    abstract Property<Boolean> getCollate();

    @OutputFile
    abstract RegularFileProperty getOutputData()

    @TaskAction
    void exec() throws IOException {
        final versions = [] as SortedSet
        final javas = [:] as TreeMap
        final results = [:] as TreeMap
        final pastFile = !inputData.isPresent() ? null : inputData.asFile.get()
        final resultsPast = pastFile == null || !pastFile.exists() ? [:] : new JsonSlurper().parse(pastFile)

        int count = 0
        for (def file : this.inputs.files) {
            if (file.equals(pastFile))
                continue
            count++
            def (javaName,javaVersion) = file.name.substring('jmh-'.length(), file.name.length() - 5).split('-')
            javas.computeIfAbsent(javaName, { [] }).add(javaVersion)
            versions.add(javaVersion)

            def json = new JsonSlurper().parse(file)
            for (def bench : json) {
                def (cls, method) = rsplit(bench.benchmark, '.', 1)
                def (pkg, suite) = rsplit(cls, '.', 1)
                def score = bench.primaryMetric.score / bench.measurementBatchSize
                def result = score.setScale(3, RoundingMode.CEILING)

                results.computeIfAbsent(pkg, { [:] })
                   .computeIfAbsent(suite, { [:] })
                   .computeIfAbsent(method, { [:] })
                   .computeIfAbsent(javaName, { [:] })
                   .put(javaVersion, result)
            }
        }


        def buffer = 'Something went fucky'
        if (count == 1) {
            if (collate.isPresent() && collate.get()) {
                // If we only ran one benchmark suite
                if (results.keySet().size() == 1 && results.values().iterator().next().keySet().size() == 1)
                    mergePast(results, resultsPast)
                buffer = formatCollated(results, resultsPast, timeUnit.get())
            } else
                buffer = formatSingle(results, resultsPast, timeUnit.get())
        } else {
            buffer = formatBulk(results, resultsPast, versions, javas)
        }

        output.asFile.get().text = buffer
        logger.lifecycle(buffer)

        mergeMaps(results, resultsPast)
        results.values().forEach { it.removeAll { k,v -> k[-1] == '*'} }
        outputData.asFile.get().text = JsonOutput.prettyPrint(JsonOutput.toJson(results))
    }

    static def rsplit(def str, def del, int limit = -1) {
        def lst = []
        def x = 0, idx
        def tmp = str
        while ((idx = tmp.lastIndexOf(del)) != -1 && (limit == -1 || x++ < limit)) {
            lst.add(0, tmp.substring(idx + del.length(), tmp.length()))
            tmp = tmp.substring(0, idx)
        }
        lst.add(0, tmp)
        return lst
    }

    static def mergePast(def to, def from) {
        def pkg = to.keySet().iterator().next()
        if (!from.containsKey(pkg))
            return
        def suite = to[pkg].keySet().iterator().next()
        to[pkg][suite].forEach{ name, byJava ->
            def java = byJava.keySet().iterator().next()
            def javaVer = byJava[java].keySet().iterator().next()
            from[pkg].forEach{ fsuite, bySuite ->
                if (fsuite == suite) return
                def past = bySuite?."$name"."$java"?."$javaVer"
                if (past != null) {
                    to[pkg].computeIfAbsent(fsuite + '*', { [:] })
                            .computeIfAbsent(name, { [:] })
                            .computeIfAbsent(java, { [:] })
                            .put(javaVer, past)
                }

            }
        }
    }

    static def mergeMaps(def left, def right) {
        right.each { k, v -> left[k] = left[k] in Map ? mergeMaps(left[k], v) : v }
        return left
    }

    static def formatCollated(def results, def resultsPast, def timeUnit) {
        def buffer = ''
        def groups = [:] as TreeMap
        def columns = [' ']
        def rows = []
        def base = [:]
        results.values().iterator().next().values().iterator().next().keySet().forEach { bench -> rows.add([bench]) }

        results.forEach { pkg, bySuite ->
            bySuite.forEach { suite, byBench ->
                columns += [suite.replace('Benchmark', '') + ' (' + timeUnit + ')', 'Change']
                if (!base.isEmpty())
                    columns.add('Relative')

                for (int x = 0; x < rows.size(); x++) {
                    def bench = rows[x][0]
                    def java = byBench?[bench]?.keySet()?.iterator()?.next()
                    def version = byBench?[bench]?[java]?.keySet()?.iterator()?.next()
                    def current = byBench?[bench]?[java]?[version]
                    def past = resultsPast?[pkg]?[suite]?[bench]?[java]?[version] ?: 0f
                    rows[x].add(String.format('%1.2f', current))

                    if (past != 0) {
                        int change = (int)(((current - past) / past) * 100)
                        rows[x].add((change >= 0 ? '+' : '') + change + '%')
                    } else
                        rows[x].add('+0%')

                    if (base[bench] != null) {
                        int rel = (int)(((current - base[bench]) / base[bench]) * 100)
                        rows[x].add((rel >= 0 ? '+' : '') + rel + '%')
                    } else
                        base[bench] = current
                }
            }
        }
        cleanChange(columns, rows)

        def alignments = [Table.ALIGN_RIGHT] * rows[0].size()
        alignments[0] = Table.ALIGN_LEFT

        def table = new Table.Builder().withAlignments(alignments)
        table.addRow(columns.toArray())
        rows.forEach {row -> table.addRow(row.toArray()) }
        return '### Collated Benchmarks\n' +
            table.build() + '\n' +
            '* - Loaded from past results\n' +
            'Change - Since past results\n' +
            'Relative - Percentage change vs first suite'
    }

    static def cleanChange(def columns, def rows) {
        for (int x = 0; x < columns.size(); x++) {
            if (columns[x] == 'Change' && rows.every{ row -> row[x] == '+0%'}) {
                columns.remove(x)
                rows.forEach{ row -> row.remove(x) }
                x--
            }
        }
    }

    static def formatSingle(def results, def resultsPast, def timeUnit) {
        def buffer = ''
        def groups = [:] as TreeMap
        results.forEach { pkg, bySuite ->
            bySuite.forEach { suite, byBench ->
                byBench.forEach { bench, byJava ->
                    def java = byJava.keySet().iterator().next()
                    def version = byJava[java].keySet().iterator().next()
                    def current = byJava[java][version]
                    def past = resultsPast?[pkg]?[suite]?[bench]?[java]?[version] ?: 0f
                    def group = groups.computeIfAbsent(suite, { [:] as TreeMap })
                    group[bench] = [current, past]
                }
            }
        }
        groups.forEach {group, bench ->
            def table = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_RIGHT)
                .addRow(' ', 'Time (' + timeUnit + ')', 'Change')
            bench.forEach { name, values ->
                def (current, past) = values
                def row = [name, String.format('%1.2f', current), '+0%']

                if (past != 0) {
                    int change = (int)(((current - past) / past) * 100)
                    row[2] = (change >= 0 ? '+' : '') + change + '%'
                }
                table.addRow(row.toArray())
            }
            buffer += '### `' + group.substring(group.lastIndexOf('.') + 1) + '`\n' +
                    table.build() + '\n' +
                    'Change - Since past results\n' +
                    '\n'
        }
        return buffer
    }

    static def formatBulk(def results, def resultsPast, def versions, def javas) {
        def buffer = ""
        results.forEach { String bench, byJava ->
            def table = new Table.Builder()
                .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
                .addRow((['Vendor'] + versions).toArray())
            def byJavaPast = resultsPast.getOrDefault(bench, [:])

            javas.forEach { javaName, javaVersions ->
                def row = [javaName]
                if (!byJava.containsKey(javaName)) {
                    versions.forEach { javaVersion ->
                        row.add(javaVersions.contains(javaVersion) ? "MISSING" : "")
                    }
                } else {
                    def byVersion = byJava.get(javaName)
                    def byVersionPast = byJavaPast.getOrDefault(javaName, [:])
                    versions.forEach { javaVersion ->
                        if (javaVersions.contains(javaVersion)) {
                            float current = byVersion.getOrDefault(javaVersion, 0f)
                            float past = byVersionPast.getOrDefault(javaVersion, 0f)
                            if (current == 0)
                                row.add("MISSING")
                            else if (past == 0)
                                row.add(current)
                            else {
                                int change = (int)(((current - past) / past) * 100)
                                if (change == 0)
                                    row.add(String.format('%1.2f', current))
                                else
                                    row.add(String.format('%1.2f %d%%', current, change))
                            }
                        } else {
                            row.add("")
                        }
                    }
                }
                table.addRow(row.toArray())
            }

            int idx = bench.lastIndexOf('.', bench.lastIndexOf('.'))
            buffer += '### `' + bench.substring(idx + 1) + '`\n' +
                      table.build() + '\n' +
                      '\n'
        }
        return buffer
    }
}