import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.steppschuh.markdowngenerator.table.Table
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

    @OutputFile
    abstract RegularFileProperty getOutputCsv()

    @TaskAction
    void exec() throws IOException {
        final versions = [] as SortedSet
        final javas = [:] as TreeMap
        final results = [:] as TreeMap
        final pastFile = !inputData.isPresent() ? null : inputData.asFile.get()
        final resultsPast = pastFile == null || !pastFile.exists() ? [:] : new JsonSlurper().parse(pastFile)

        int javaCount = 0
        for (def file : this.inputs.files) {
            if (file == pastFile)
                continue
            def (javaName,javaVersion) = file.name.substring('jmh-'.length(), file.name.length() - 5).split('-')

            javaCount++
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

        boolean onlyOneSuite = results.keySet().size() == 1 && results.values().iterator().next().keySet().size() == 1
        boolean loadOld = collate.isPresent() && collate.get()

        def markdown,csv = 'Something went fucky'
        if (javaCount == 1) {
            if (loadOld && onlyOneSuite) // If we only ran one benchmark suite load data from old input
                mergePast(results, resultsPast)
            (markdown,csv) = formatCollated(results, resultsPast, timeUnit.get())
        } else {
            if (loadOld && onlyOneSuite) // If we only ran one benchmark suite load data from old input
                mergePast(results, resultsPast)
            (markdown,csv) = formatBulk(results, resultsPast, timeUnit.get(), javas, versions)
        }

        output.asFile.get().text = markdown
        logger.lifecycle(markdown)
        //logger.lifecycle(csv)

        mergeMaps(results, resultsPast)
        results.values().forEach { it.removeAll { k,v -> k[-1] == '*'} }
        outputData.asFile.get().text = JsonOutput.prettyPrint(JsonOutput.toJson(results))
        outputCsv.asFile.get().text = csv
    }

    static def rsplit(def str, String del, int limit = -1) {
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

    static void mergePast(def to, def from) {
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

    /*
     * For Single, or Multiple benchmarks that are ran on a single JVM  we can build a compact table such as:
     * ### Collated Benchmarks (ns)
     * |                    |  NoLoader | Change | Concurrent | Relative |
     * | ------------------ |----------:| ------:|-----------:| --------:|
     * | postStatic         |     94.11 |   -22% |      42.94 |     -54% |
     * | postStaticDozen    |    452.17 |   -25% |     176.57 |     -60% |
     * | postStaticHundred  |   6589.86 |   +33% |    2597.93 |     -60% |
     *
     * This allows for comparison between multiple environments and implementations
     */
    static def formatCollated(def results, def resultsPast, String timeUnit) {
        def columns = [' ']
        def rows = []
        def base = [:]
        results.values().iterator().next().values().iterator().next().keySet().forEach { bench -> rows.add([bench]) }

        results.forEach { pkg, bySuite ->
            bySuite.forEach { suite, byBench ->
                columns += [suite.replace('Benchmark', ''), 'Change', 'Relative']

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
                    } else {
                        base[bench] = current
                        rows[x].add('+0%')
                    }
                }
            }
        }

        def markdown = '### Collated Benchmarks (' + timeUnit + ')\n' +
                        buildTable(columns, rows)

        def csv = buildCsv(columns, rows, true, '')

        return [markdown, csv]
    }

    static def cleanTable(def columns, def rows) {
        columns = [] + columns
        rows = [] + rows
        for (int x = 0; x < rows.size(); x++)
            rows[x] = [] + rows[x]

        for (int x = 0; x < columns.size(); x++) {
            def stat = columns[x] == 'Change' || columns[x] == 'Relative'
            if (stat && rows.every{ row -> row[x] == '+0%'}) {
                columns.remove(x)
                rows.forEach{ row -> row.remove(x) }
                x--
            }
        }
        return [columns, rows]
    }

    static def buildTable(def columns, def rows) {
        (columns, rows) = cleanTable(columns, rows)
        def alignments = [Table.ALIGN_RIGHT] * columns.size()
        alignments[0] = Table.ALIGN_LEFT

        def table = new Table.Builder().withAlignments(alignments)
        table.addRow(columns.toArray())
        rows.forEach {row -> table.addRow(row.toArray()) }
        def markdown = table.build().toString()
        if (columns.any {it.endsWith('*')})
            markdown += '\n* - Loaded from past results'
        if (columns.any { it == 'Change' })
            markdown += '\nChange - Since past results'
        if (columns.any { it == 'Relative' })
            markdown += '\nRelative - Percentage change vs first suite'
        return markdown
    }

    static String buildCsv(def columns, def rows, boolean headers = true, String prefix = '') {
        columns = [] + columns
        rows = [] + rows
        for (int x = 0; x < rows.size(); x++)
            rows[x] = [] + rows[x]

        for (int x = 0; x < columns.size(); x++) {
            if (columns[x] == 'Change' || columns[x] == 'Relative') {
                columns.remove(x)
                rows.each{ it.remove(x) }
                x--
            }
        }
        String csv = ''
        if (headers)
            csv += columns.join('\t') + '\n'
        for (def row : rows)
            csv += prefix + row.join('\t') + '\n'
        return csv
    }

    /*
     * Formats the reuslts of a run that contains multiple java versions and potentially multiple suits/distros.
     * Each suite and distro will get its own output matric.
     *
     * ### ModLauncher Benchmarks (ns) (Adoptium)
     * |                    |         17 |         18 | Relative |
     * | postStatic         |     14.835 |     15.087 |      +1% |
     * | postStaticDozen    |     99.023 |    101.647 |      +2% |
     * | postStaticHundred  |   1306.996 |   1326.775 |      +1% |
     * Relative - Percentage change vs first suite
     *
     * ### NoLoader Benchmarks (ns) (Adoptium)
     * |                    |         17 |         18 | Relative |
     * | postStatic         |     96.500 |     96.868 |      +0% |
     * | postStaticDozen    |    457.923 |    453.689 |      +0% |
     * | postStaticHundred  |   5878.851 |   5325.055 |      -9% |
     * Relative - Percentage change vs first suite
     */
    static def formatBulk(def results, def resultsPast, String timeUnit, def javas, def versions) {
        String markdown = ''
        boolean onlyOneSuite = results.keySet().size() == 1 && results.values().iterator().next().keySet().size() == 1
        def columns = [' ']
        def csvTable = [:]
        versions.forEach { version ->  columns += [version, 'Change', 'Relative'] }
        versions.forEach { version -> csvTable[version] = [:] }

        def base = [:]
        def rows = []

        javas.keySet().forEach { java ->
            results.forEach { pkg, bySuite ->
                bySuite.forEach { suite, byBench ->
                    byBench.forEach {bench, byJava ->
                        def name = onlyOneSuite ? bench : suite + '.' + bench
                        def csvName = suite.replace('Benchmark', '')
                        if (javas.size() > 1) {
                            name = java + '.' + name
                            csvName = java + '.' + csvName
                        }
                        def row = [bench]
                        versions.forEach { version ->
                            def current = byJava?[java]?[version]
                            csvTable[version].computeIfAbsent(bench, { [:] as TreeMap })[csvName] = current
                            if (current) {
                                row.add(current)
                                def past = resultsPast?[pkg]?[suite]?[bench]?[java]?[version]

                                if (past) {
                                    int change = (int)(((current - past) / past) * 100)
                                    row.add((change >= 0 ? '+' : '') + change + '%')
                                } else
                                    row.add('+0%')

                                if (base[name] != null) {
                                    int rel = (int)(((current - base[name]) / base[name]) * 100)
                                    row.add((rel >= 0 ? '+' : '') + rel + '%')
                                } else {
                                    base[name] = current
                                    row.add('+0%')
                                }
                            } else {
                                row.addAll([0, '+0%', '+0%'])
                            }
                        }
                        rows.add(row)
                    }

                    markdown += '### ' + suite.replace('Benchmark', '') + ' Benchmarks (' + timeUnit + ') (' + java  + ')'
                    markdown += '\n' + buildTable(columns, rows) + '\n'
                    if (!onlyOneSuite)
                        markdown += '\n\n'
                    rows.clear()
                }
            }
        }
        def csv = [[]]
        versions.forEach { version -> csv.add([]) }
        for (int x = 0; x < versions.size(); x++) {
            columns = csv[0]
            def row = csv[x + 1]
            def benches = csvTable[versions[x]].keySet().toArray()
            for (int y = 0; y < benches.length; y++) {
                if (y > 0) {
                    columns.add('')
                    row.add('')
                }
                if (x == 0)
                    columns.add(benches[y])
                row.add(versions[x])
                csvTable[versions[x]][benches[y]].forEach { name, value ->
                    if (x == 0)
                        columns.add(name)
                    row.add(value)
                }
            }
        }
        String csvData = ''
        for (def line : csv)
            csvData += line.join('\t') + '\n'

        return [markdown, csvData]
    }
}