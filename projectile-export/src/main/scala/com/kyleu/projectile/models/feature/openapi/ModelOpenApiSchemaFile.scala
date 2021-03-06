package com.kyleu.projectile.models.feature.openapi

import com.kyleu.projectile.models.export.config.ExportConfiguration
import com.kyleu.projectile.models.export.ExportModel
import com.kyleu.projectile.models.output.OutputPath
import com.kyleu.projectile.models.output.file.JsonFile

object ModelOpenApiSchemaFile {
  def export(config: ExportConfiguration, model: ExportModel) = {
    val file = JsonFile(path = OutputPath.OpenAPIJson, dir = Seq("components", "schema") ++ model.pkg, model.propertyName)
    file.add("{", 1)

    file.add(s""""${model.fullClassName}": {""", 1)

    val required = model.fields.filter(_.required).map(_.propertyName)

    if (required.nonEmpty) {
      file.add("\"required\": [", 1)
      required.foreach { r =>
        val comma = if (required.lastOption.contains(r)) { "" } else { "," }
        file.add("\"" + r + "\"" + comma)
      }
      file.add("],", -1)
    }

    file.add("\"properties\": {", 1)
    model.fields.foreach(f => OpenApiPropertyHelper.propertyFor(f, file, model.fields.lastOption.contains(f), config.enums))
    file.add("}", -1)

    file.add("},", -1)

    file.add(s""""${model.fullClassName}Result": {""", 1)
    file.add("\"properties\": {", 1)

    file.add("\"filters\": {", 1)
    file.add("\"type\": \"array\",")
    file.add("\"items\": {", 1)
    file.add("\"$ref\": \"#/components/schemas/common.Filter\"")
    file.add("}", -1)
    file.add("},", -1)

    file.add("\"orderBys\": {", 1)
    file.add("\"type\": \"array\",")
    file.add("\"items\": {", 1)
    file.add("\"$ref\": \"#/components/schemas/common.OrderBy\"")
    file.add("}", -1)
    file.add("},", -1)

    file.add("\"totalCount\": {", 1)
    file.add("\"type\": \"integer\"")
    file.add("},", -1)

    file.add("\"paging\": {", 1)
    file.add("\"$ref\": \"#/components/schemas/common.PagingOptions\"")
    file.add("},", -1)

    file.add("\"results\": {", 1)
    file.add("\"type\": \"array\",")
    file.add("\"items\": {", 1)
    file.add("\"$ref\": \"#/components/schemas/" + model.fullClassName + "\"")
    file.add("}", -1)
    file.add("},", -1)

    file.add("\"durationMs\": {", 1)
    file.add("\"type\": \"integer\"")
    file.add("},", -1)

    file.add("\"occurred\": {", 1)
    file.add("\"type\": \"string\",")
    file.add("\"example\": \"2018-01-01 00:00:00\"")
    file.add("}", -1)

    file.add("}", -1)
    file.add("}", -1)

    file.add("}", -1)
    file
  }
}
