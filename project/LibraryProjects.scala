import sbt.Keys._
import sbt._
import Dependencies._

import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

import org.scalajs.sbtplugin.ScalaJSPlugin
import webscalajs.ScalaJSWeb

object LibraryProjects {
  lazy val `projectile-lib-scala` = (project in file("libraries/projectile-lib-scala")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-scala",
    description := "Common classes used by code generated from Projectile",
    libraryDependencies ++= Serialization.all ++ Seq(Utils.enumeratum, Utils.slf4j, Utils.commonsCodec),
    (sourceGenerators in Compile) += ProjectVersion.writeConfig(Shared.projectId, Shared.projectName, Shared.projectPort).taskValue
  )

  lazy val `projectile-lib-tracing` = (project in file("libraries/projectile-lib-tracing")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-tracing",
    description := "Common OpenTracing classes used by code generated from Projectile",
    libraryDependencies ++= Seq(
      Metrics.micrometerCore, Metrics.micrometerStatsd, Metrics.micrometerPrometheus,
      Tracing.datadogTracing, Tracing.jaegerCore, Tracing.jaegerThrift, Tracing.jaegerMetrics,
      Utils.javaxInject, Utils.typesafeConfig
    )
  ).dependsOn(`projectile-lib-scala`)

  lazy val `projectile-lib-jdbc` = (project in file("libraries/projectile-lib-jdbc")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-jdbc",
    description := "Common database classes used by code generated from Projectile",
    libraryDependencies ++= Seq(Database.postgres, Database.hikariCp, Utils.commonsCodec, Utils.typesafeConfig)
  ).dependsOn(`projectile-lib-scala`)

  lazy val `projectile-lib-doobie` = (project in file("libraries/projectile-lib-doobie")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-doobie",
    description := "Common Doobie classes used by code generated from Projectile",
    libraryDependencies ++= Database.Doobie.all
  ).dependsOn(`projectile-lib-jdbc`)

  lazy val `projectile-lib-slick` = (project in file("libraries/projectile-lib-slick")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-slick",
    description := "Common Slick classes used by code generated from Projectile",
    libraryDependencies ++= Database.Slick.all
  ).dependsOn(`projectile-lib-jdbc`)

  lazy val `projectile-lib-service` = (project in file("libraries/projectile-lib-service")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-service",
    description := "Common service classes used by code generated from Projectile",
    libraryDependencies ++= Seq(Utils.csv, Utils.javaxInject, Utils.scalaGuice)
  ).dependsOn(`projectile-lib-jdbc`, `projectile-lib-tracing`)

  lazy val `projectile-lib-graphql` = (project in file("libraries/projectile-lib-graphql")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-graphql",
    description := "Common GraphQL classes used by code generated from Projectile",
    libraryDependencies ++= Seq(GraphQL.circe, GraphQL.playJson, GraphQL.sangria, Utils.guice)
  ).dependsOn(`projectile-lib-service`)

  lazy val `projectile-lib-play` = (project in file("libraries/projectile-lib-play")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-play",
    description := "Common Play Framework classes used by code generated from Projectile",
    resolvers += Resolver.bintrayRepo("stanch", "maven"),
    libraryDependencies ++= Seq(Play.lib, Utils.reftree, play.sbt.PlayImport.ws)
  ).dependsOn(`projectile-lib-service`, `projectile-lib-graphql`)

  lazy val `projectile-lib-scalajs` = (project in file("libraries/projectile-lib-scalajs")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-scalajs",
    description := "Common ScalaJS classes used by code generated from Projectile",
    libraryDependencies ++= Serialization.projects.map(c => "io.circe" %%% c % Serialization.version) ++ Seq(
      "be.doeraene" %%% "scalajs-jquery" % ScalaJS.jQueryVersion,
      "com.lihaoyi" %%% "scalatags" % ScalaJS.scalatagsVersion,
      "com.beachape" %%% "enumeratum-circe" % Utils.enumeratumCirceVersion,
      "io.github.cquiroz" %%% "scala-java-time" % ScalaJS.javaTimeVersion
    )
  ).enablePlugins(ScalaJSPlugin, ScalaJSWeb)

  lazy val `projectile-lib-auth` = (project in file("libraries/projectile-lib-auth")).settings(Shared.commonSettings: _*).settings(
    name := "projectile-lib-auth",
    description := "Common Silhouette authentication classes used by code generated from Projectile",
    libraryDependencies ++= Authentication.all
  ).dependsOn(`projectile-lib-play`)

  lazy val all: Seq[ProjectReference] = Seq(
    `projectile-lib-scala`, `projectile-lib-tracing`, `projectile-lib-jdbc`, `projectile-lib-doobie`, `projectile-lib-slick`,
    `projectile-lib-service`, `projectile-lib-graphql`, `projectile-lib-play`, `projectile-lib-scalajs`, `projectile-lib-auth`
  )
}
