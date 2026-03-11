package ru.lewis.libraryloader.model

import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

class PomResolver(private val repositories: Map<String, String>) {
    fun resolve(dependency: Dependency, visited: MutableSet<String> = mutableSetOf()): List<Dependency> {
        val key = dependency.toString()
        if (key in visited) return emptyList()
        visited.add(key)

        val pom = fetchPom(dependency) ?: return emptyList()
        val result = mutableListOf<Dependency>()
        val properties = parseProperties(pom)
        val managedVersions = parseDependencyManagement(pom, properties)
        val deps = parseDependencies(pom, properties, managedVersions)

        deps.forEach { dep ->
            result.add(dep)
            result.addAll(resolve(dep, visited))
        }

        return result
    }

    private fun fetchPom(dependency: Dependency): org.w3c.dom.Document? {
        val pomPath = dependency.toPomPath()

        for ((_, url) in repositories) {
            val fullUrl = "${url.trimEnd('/')}/$pomPath"
            try {
                val connection = URL(fullUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000

                if (connection.responseCode != 200) continue

                return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(connection.inputStream)
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    private fun parseProperties(doc: org.w3c.dom.Document): Map<String, String> {
        val properties = mutableMapOf<String, String>()

        // Добавляем project.version и project.parent.version
        doc.documentElement.getTagValue("version")?.let {
            properties["project.version"] = it
        }

        val parentNodes = doc.getElementsByTagName("parent")
        if (parentNodes.length > 0) {
            val parent = parentNodes.item(0) as Element
            parent.getTagValue("version")?.let {
                properties["project.parent.version"] = it
                // иногда используется просто ${parent.version}
                properties["parent.version"] = it
            }
        }

        // Остальные properties из <properties>
        val nodes = doc.getElementsByTagName("properties")
        if (nodes.length > 0) {
            val propsElement = nodes.item(0) as Element
            val children = propsElement.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    properties[node.nodeName] = node.textContent.trim()
                }
            }
        }

        return properties
    }

    private fun parseDependencyManagement(
        doc: org.w3c.dom.Document,
        properties: Map<String, String>
    ): Map<String, String> {
        val managed = mutableMapOf<String, String>()
        val mgmtNodes = doc.getElementsByTagName("dependencyManagement")
        if (mgmtNodes.length == 0) return managed

        val depNodes = (mgmtNodes.item(0) as Element)
            .getElementsByTagName("dependency")

        for (i in 0 until depNodes.length) {
            val dep = depNodes.item(i) as Element
            val group    = dep.getTagValue("groupId") ?: continue
            val artifact = dep.getTagValue("artifactId") ?: continue
            val version  = dep.getTagValue("version")?.resolvePlaceholder(properties) ?: continue
            managed["$group:$artifact"] = version
        }

        return managed
    }

    private fun parseDependencies(
        doc: org.w3c.dom.Document,
        properties: Map<String, String>,
        managedVersions: Map<String, String>
    ): List<Dependency> {
        val result = mutableListOf<Dependency>()

        val root = doc.documentElement
        val depsNodes = root.childNodes

        var inDependencies = false
        for (i in 0 until depsNodes.length) {
            val node = depsNodes.item(i)
            if (node.nodeName == "dependencies") {
                inDependencies = true
                val depNodes = (node as Element).getElementsByTagName("dependency")

                for (j in 0 until depNodes.length) {
                    val dep = depNodes.item(j) as Element

                    val group    = dep.getTagValue("groupId") ?: continue
                    val artifact = dep.getTagValue("artifactId") ?: continue
                    val scope    = dep.getTagValue("scope") ?: "compile"
                    val optional = dep.getTagValue("optional") ?: "false"

                    // Пропускаем test, provided, system и optional
                    if (scope in listOf("test", "provided", "system")) continue
                    if (optional == "true") continue

                    val version = dep.getTagValue("version")
                        ?.resolvePlaceholder(properties)
                        ?: managedVersions["$group:$artifact"]
                        ?: continue

                    result.add(Dependency(group, artifact, version))
                }
                break
            }
        }

        return result
    }

    private fun Element.getTagValue(tag: String): String? {
        val nodes = getElementsByTagName(tag)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent.trim().ifBlank { null }
    }

    private fun String.resolvePlaceholder(properties: Map<String, String>): String {
        if (!contains("\${")) return this
        var result = this
        properties.forEach { (key, value) ->
            result = result.replace("\${$key}", value)
        }
        return result
    }
}