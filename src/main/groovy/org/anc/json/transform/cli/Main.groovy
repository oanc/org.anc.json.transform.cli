package org.anc.json.transform.cli

import org.anc.json.transform.Json2Xslt
import org.anc.json.transform.JsonTransformer
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.w3c.dom.Document

/**
 * @author Keith Suderman
 */
class Main {

    void run(String template, String input, def output) {
        JsonTransformer transformer = new JsonTransformer(template)
        output.write(transformer.transform(input))
    }

    void run(String template, String input, File output) {
        // Automagically import the groovy json and xml packages for the user script.
        ImportCustomizer customizer = new ImportCustomizer()
        customizer.addStarImports("groovy.json", "groovy.xml")
        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(customizer)

        // "System" properties can be added to the binding object so they are available
        // inside the user script.
        Binding binding = new Binding()

        GroovyShell shell = new GroovyShell(binding, configuration)
        Script script = shell.parse(template)

        ExpandoMetaClass meta = new ExpandoMetaClass(script.class, false)
        meta.match = { String pattern, Closure cl ->

        }
        script.metaClass = meta
    }

    static void main(args) {
        CliBuilder cli = new CliBuilder()
        cli.header = "Transforms JSON instances with XSLT stylesheets\n"
        cli.usage = "java -jar json-transform-${Version.version}.jar [options] <template> <input>\n"
        cli.s(longOpt: 'stylesheet', 'prints the generated XSLT stylesheet only.')
        cli.x(longOpt: 'xml', 'generates XML output')
        cli.j(longOpt: 'json', 'generates JSON output (default)')
        cli.v(longOpt: 'version', 'displays the version number')
        cli.h(longOpt: 'help', 'displays this help message')

        def params = cli.parse(args)
        if (!params) {
            return
        }

        if (params.h) {
            println()
            cli.usage()
            println()
            return
        }

        if (params.v) {
            println()
            println "LAPPS JsonTransformer v${Version.version}"
            println "Copyright 2015 The Language Application Grid"
            println()
            return
        }

        List<String> files = params.arguments()

        if (params.s) {
            if (files.size() == 0) {
                println "No template provided."
                return
            }
            File template = new File(files[0])
            if (!template.exists()) {
                println "Template not found."
                return
            }

            Json2Xslt xslt = new Json2Xslt()
            Document document = xslt.compile(template.text)
            println JsonTransformer.prettyPrint(document)
            return
        }

        if (files.size() != 2) {
            println "No template and/or input file specified."
            println()
            cli.usage()
            println()
            return
        }

        File template = new File(files[0])
        if (!template.exists()) {
            println "Template file not found."
            return
        }

        File input = new File(files[1])
        if (!input.exists()) {
            println "Input file not found."
            return
        }

        JsonTransformer.Output format = JsonTransformer.Output.JSON
        if (params.x) {
            format = JsonTransformer.Output.XML
        }
        JsonTransformer transformer = new JsonTransformer(template.text, format)
        println transformer.transform(input.text)
    }
}

