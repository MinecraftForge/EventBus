import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class AggregateTest extends DefaultTask {
    @InputDirectory
    abstract DirectoryProperty getInput()

    @OutputFile
    abstract RegularFileProperty getOutput()

    @TaskAction
    void exec() throws IOException {
        final javas = [:] as TreeMap
        final results = [:] as TreeMap

        for (def java : input.asFile.get().listFiles()) {
            for (def test : java.listFiles()) {
                if (!test.name.startsWith('TEST-') || !test.name.endsWith('.xml'))
                    continue

                String dirName = test.parentFile.name
                if (!dirName.contains('-')) continue
                def (javaName, javaVersion) = dirName.split('-')
                javas.computeIfAbsent(javaName, { [] as SortedSet }).add(javaVersion)

                def data = DOMBuilder.parse(new StringReader(test.text)).documentElement
                use(DOMCategory) {
                    def suite = data['@name']
                    suite = suite.substring(suite.lastIndexOf('.') + 1)
                    def byTest = results.computeIfAbsent(suite, { [:] })
                    for (def testcase : data.testcase) {
                        def name = testcase['@name']
                        if (name.startsWith('repetition '))
                            continue
                        def byJava = byTest.computeIfAbsent(name, { [:] })
                        def byVersion = byJava.computeIfAbsent(javaName, { [:] })
                        byVersion.put(javaVersion, testcase.failure.isEmpty())
                    }
                }
                test.delete()
            }
            if (java.listFiles().length === 0)
                java.deleteDir()
        }

        def buffer = new StringBuilder()
        buffer.append("""
<html>
  <style>
    .tooltip-text {
      visibility: hidden;
      position: absolute;
      z-index: 1;
      width: 100px;
      color: white;
      font-size: 12px;
      background-color: #192733;
      border-radius: 10px;
      padding: 10px 15px 10px 15px;
      top: -40px;
      left: -50%;
    }

    .hover-text:hover .tooltip-text {
      visibility: visible;
    }

    .success {
      background-color: #008000;
    }

    .failure {
      background-color: #b60808;
    }

    .hover-text {
      font-size: 16px;
      position: relative;
      display: inline;
      font-family: monospace;
      text-align: center;
    }

    table, th, td {
      border: 1px solid black;
      border-collapse: collapse;
    }

    th, td {
       padding-left: 3px;
       padding-right: 3px;
    }

    .result {
        font-size: 0px;
    }
  </style>
  <body>
""")
        results.forEach{ suite, byTest ->
            buffer << "<h1>${suite}</h1>\n"
            buffer << "<table>\n"
            buffer << "  <thead>\n"
            buffer << "    <th>Test</th>\n"
            javas.keySet().forEach{ javaName ->
                buffer << "    <th>${javaName}</th>\n"
            }
            buffer << "  </thead>\n"
            buffer << "  <tbody>\n"
            byTest.forEach{ testName, byJava ->
                buffer << "    <tr>\n"
                buffer << "      <td>${testName}</td>\n"
                javas.forEach{ javaName, versions ->
                    buffer << "      <td class=\"result\">\n"
                    def byVersion = byJava.get(javaName)
                    versions.forEach { ver ->
                        if (byVersion.containsKey(ver) && byVersion.get(ver)) {
                            buffer << "        <span class=\"hover-text success\">O<span class=\"tooltip-text success\" id=\"failure\">${javaName} v${ver}</span></span>\n"
                        } else {
                            buffer << "        <span class=\"hover-text failure\">X<span class=\"tooltip-text failure\">${javaName} v${ver}</span></span>\n"
                        }
                    }
                    buffer << "      </td>\n"
                }
                buffer << "    </tr>\n"
            }
            buffer << "  </tbody>\n"
            buffer << "</table>\n"

        }
        buffer << "</body></html>"

        output.asFile.get().text = buffer
    }
}