package com.kyleu.projectile.models.feature

import better.files.File
import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}
import com.kyleu.projectile.models.export.config.ExportConfiguration
import com.kyleu.projectile.models.output.OutputPath._
import com.kyleu.projectile.models.feature.audit.AuditLogic
import com.kyleu.projectile.models.feature.controller.ControllerLogic
import com.kyleu.projectile.models.feature.core.CoreLogic
import com.kyleu.projectile.models.feature.datamodel.DataModelLogic
import com.kyleu.projectile.models.feature.doobie.DoobieLogic
import com.kyleu.projectile.models.feature.graphql.GraphQLLogic
import com.kyleu.projectile.models.feature.openapi.OpenApiLogic
import com.kyleu.projectile.models.feature.service.ServiceLogic
import com.kyleu.projectile.models.feature.slick.SlickLogic
import com.kyleu.projectile.models.feature.thrift.ThriftLogic
import com.kyleu.projectile.models.feature.wiki.WikiLogic
import com.kyleu.projectile.models.input.{InputTemplate, InputType}
import com.kyleu.projectile.models.output.{OutputLog, OutputPath}

sealed abstract class ProjectFeature(
    override val value: String,
    val title: String,
    val tech: String,
    val logic: Option[FeatureLogic],
    val paths: Set[OutputPath],
    val appliesTo: Set[InputTemplate],
    val description: String
) extends StringEnumEntry {

  def export(projectRoot: File, config: ExportConfiguration, verbose: Boolean) = {
    val startMs = System.currentTimeMillis

    val logs = collection.mutable.ArrayBuffer.empty[OutputLog]

    def info(s: String) = {
      logs += OutputLog(s, System.currentTimeMillis - startMs)
    }
    def debug(s: String) = if (verbose) { info(s) }

    val files = logic.map(_.export(config = config, info = info, debug = debug)).getOrElse(Nil)
    val markers = files.map(_.markers).foldLeft(Map.empty[String, Seq[String]]) { (l, r) =>
      (l.keys.toSeq ++ r.keys.toSeq).distinct.map(k => k -> (l.getOrElse(k, Nil) ++ r.getOrElse(k, Nil))).toMap
    }
    val injections = logic.map(_.inject(config = config, projectRoot = projectRoot, markers = markers, info = info, debug = debug)).getOrElse(Nil)
    val duration = System.currentTimeMillis - startMs
    info(s"Feature [$title] produced [${files.length}] files in [${duration}ms]")
    FeatureOutput(feature = this, files = files, injections = injections, logs = logs, duration = duration)
  }
}

object ProjectFeature extends StringEnum[ProjectFeature] with StringCirceEnum[ProjectFeature] {
  case object Core extends ProjectFeature(
    value = "core", title = "Core", tech = "Scala", logic = Some(CoreLogic), paths = Set(Root, SharedSource, ServerSource),
    appliesTo = InputTemplate.all, description = "Scala case classes and Circe Json serializers"
  )

  case object DataModel extends ProjectFeature(
    value = "datamodel", title = "Data Model", tech = "Scala", logic = Some(DataModelLogic), paths = Set(SharedSource, ServerSource),
    appliesTo = InputTemplate.all, description = "Defines methods to export models to a common schema, and creates search result response classes"
  )

  case object ScalaJS extends ProjectFeature(
    value = "scalajs", title = "Scala.js", tech = "Scala", logic = None, paths = Set(SharedSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Exports models to Scala.js for use from JavaScript"
  )

  case object Slick extends ProjectFeature(
    value = "slick", title = "Slick", tech = "Scala", logic = Some(SlickLogic), paths = Set(ServerSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Slick JDBC classes and supporting queries"
  )

  case object Doobie extends ProjectFeature(
    value = "doobie", title = "Doobie", tech = "Scala", logic = Some(DoobieLogic), paths = Set(ServerSource, ServerTest),
    appliesTo = Set(InputTemplate.Postgres), description = "Doobie JDBC classes and supporting queries"
  )

  case object Service extends ProjectFeature(
    value = "service", title = "Service", tech = "Scala", logic = Some(ServiceLogic), paths = Set(ServerSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Custom service and supporting queries for common operations"
  )

  case object GraphQL extends ProjectFeature(
    value = "graphql", title = "GraphQL", tech = "Scala", logic = Some(GraphQLLogic), paths = Set(GraphQLOutput),
    appliesTo = InputTemplate.all, description = "Sangria bindings for an exported GraphQL schema"
  )

  case object Controller extends ProjectFeature(
    value = "controller", title = "Controller", tech = "Scala", logic = Some(ControllerLogic), paths = Set(ServerSource, ServerResource),
    appliesTo = InputTemplate.all, description = "Play Framework controller for common operations"
  )

  case object Audit extends ProjectFeature(
    value = "audit", title = "Audit", tech = "Scala", logic = Some(AuditLogic), paths = Set(ServerSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Logs audits of changed properties for models"
  )

  case object Notes extends ProjectFeature(
    value = "notes", title = "Notes", tech = "Scala", logic = None, paths = Set(ServerSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Allows the creation and viewing of notes for any model instance"
  )

  case object Tests extends ProjectFeature(
    value = "tests", title = "Tests", tech = "Scala", logic = None, paths = Set(ServerSource),
    appliesTo = Set(InputTemplate.Postgres), description = "Scalatest unit tests for selected features"
  )

  case object Thrift extends ProjectFeature(
    value = "thrift", title = "Thrift", tech = "Thrift", logic = Some(ThriftLogic), paths = Set(ThriftOutput),
    appliesTo = Set(InputTemplate.Postgres), description = "Thrift definitions for exported models and services"
  )

  case object OpenAPI extends ProjectFeature(
    value = "openapi", title = "OpenAPI", tech = "JSON", logic = Some(OpenApiLogic), paths = Set(OpenAPIJson),
    appliesTo = Set(InputTemplate.Postgres), description = "OpenAPI/Swagger documentation of generated methods"
  )

  case object Wiki extends ProjectFeature(
    value = "wiki", title = "Wiki", tech = "Markdown", logic = Some(WikiLogic), paths = Set(WikiMarkdown),
    appliesTo = Set(InputTemplate.Postgres), description = "Markdown documentation in GitHub wiki format"
  )

  override val values = findValues
  val set = values.toSet
}
