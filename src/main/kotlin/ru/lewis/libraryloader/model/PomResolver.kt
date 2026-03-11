package ru.lewis.libraryloader.model

import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.net.HttpURLConnection
import java.net.URL

class PomResolver(private val repositories: Map<String, String>) {

    private val reader = MavenXpp3Reader()

    fun resolve(dependency: Dependency, visited: MutableSet<String> = mutableSetOf()): List<Dependency> {
        val key = dependency.toString()
        if (key in visited) return emptyList()
        visited.add(key)

        val model = fetchPom(dependency) ?: return emptyList()
        val result = mutableListOf<Dependency>()

        val resolvedModel = resolveModel(model)
        val deps = resolvedModel.dependencies

        deps.forEach { dep ->
            val scope = dep.scope ?: "compile"
            val optional = dep.isOptional

            if (scope in listOf("test", "provided", "system")) return@forEach
            if (optional) return@forEach

            val version = dep.version ?: return@forEach
            val resolved = Dependency(dep.groupId, dep.artifactId, version)

            result.add(resolved)
            result.addAll(resolve(resolved, visited))
        }

        return result
    }

    private fun fetchPom(dependency: Dependency): Model? {
        val pomPath = dependency.toPomPath()

        for ((_, baseUrl) in repositories) {
            val fullUrl = "${baseUrl.trimEnd('/')}/$pomPath"
            try {
                val connection = URL(fullUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000

                if (connection.responseCode != 200) continue

                return connection.inputStream.use { reader.read(it) }
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    /**
     * Резолвим плейсхолдеры ${...} и dependencyManagement в модели.
     */
    private fun resolveModel(model: Model): Model {
        // Собираем properties: из <properties> + project.version + parent.version
        val props = mutableMapOf<String, String>()

        model.parent?.version?.let {
            props["project.parent.version"] = it
            props["parent.version"] = it
        }
        (model.version ?: model.parent?.version)?.let {
            props["project.version"] = it
        }
        model.properties.forEach { k, v -> props[k.toString()] = v.toString() }

        // dependencyManagement → карта group:artifact -> version
        val managed = model.dependencyManagement?.dependencies
            ?.associate { "${it.groupId}:${it.artifactId}" to it.version.resolve(props) }
            ?: emptyMap()

        // Резолвим версии в зависимостях
        model.dependencies.forEach { dep ->
            dep.groupId = dep.groupId.resolve(props)
            dep.artifactId = dep.artifactId.resolve(props)
            dep.version = (dep.version?.resolve(props))
                ?: managed["${dep.groupId}:${dep.artifactId}"]
        }

        return model
    }

    private fun String?.resolve(props: Map<String, String>): String? {
        if (this == null || !contains("\${")) return this
        var result = this
        props.forEach { (key, value) -> result = result!!.replace("\${$key}", value) }
        return result
    }
}