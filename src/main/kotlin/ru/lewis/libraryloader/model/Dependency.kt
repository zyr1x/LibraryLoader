package ru.lewis.libraryloader.model

data class Dependency(
    val group: String,
    val artifact: String,
    val version: String
) {
    fun toPath(): String {
        val groupPath = group.replace('.', '/')
        return "$groupPath/$artifact/$version/$artifact-$version.jar"
    }

    fun toFileName(): String = "$artifact-$version.jar"

    fun toPomPath(): String {
        val groupPath = group.replace('.', '/')
        return "$groupPath/$artifact/$version/$artifact-$version.pom"
    }

    override fun toString(): String = "$group:$artifact:$version"

    companion object {
        fun of(notation: String): Dependency {
            val parts = notation.split(":")
            require(parts.size == 3) { "Invalid dependency: $notation" }
            return Dependency(parts[0], parts[1], parts[2])
        }
    }
}