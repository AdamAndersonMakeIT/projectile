import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport.{scapegoatIgnoredFiles, scapegoatDisabledInspections}
import com.typesafe.sbt.GitPlugin.autoImport.git
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import com.typesafe.sbt.less.Import._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._

object Server {
  private[this] val dependencies = {
    import Dependencies._
    Dependencies.Serialization.all ++ Seq(
      Akka.actor, Akka.logging, Akka.protobuf, Akka.stream, Akka.visualMailbox,
      Play.filters, Play.guice, Play.ws, Play.json, Play.cache,
      Database.postgres, Database.hikariCp, GraphQL.sangria, GraphQL.playJson, GraphQL.circe,
      WebJars.jquery, WebJars.fontAwesome, WebJars.materialize,
      Utils.betterFiles, Utils.commonsIo, Utils.commonsLang, Utils.enumeratum, Utils.scalaGuice, Utils.schwatcher, Utils.scopts,
      Akka.testkit, Play.test, Testing.scalaTest
    )
  }

  private[this] lazy val serverSettings = Shared.commonSettings ++ Seq(
    name := Shared.projectId,
    description := Shared.projectName,

    libraryDependencies ++= dependencies,

    // Play
    RoutesKeys.routesGenerator := InjectedRoutesGenerator,
    RoutesKeys.routesImport ++= Seq("util.web.QueryStringUtils._"),
    PlayKeys.externalizeResources := false,
    PlayKeys.devSettings := Seq("play.server.akka.requestTimeout" -> "infinite"),
    PlayKeys.playDefaultPort := Shared.projectPort,
    PlayKeys.playInteractionMode := PlayUtils.NonBlockingInteractionMode,

    // Sbt-Web
    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
    pipelineStages ++= Seq(digest, gzip),
    includeFilter in (Assets, LessKeys.less) := "*.less",
    excludeFilter in (Assets, LessKeys.less) := "_*.less",
    LessKeys.compress in Assets := true,

    // Source Control
    scmInfo := Some(ScmInfo(url("https://github.com/KyleU/projectile"), "git@github.com:KyleU/projectile.git")),
    git.remoteRepo := scmInfo.value.get.connection,

    // Fat-Jar Assembly
    assemblyJarName in assembly := Shared.projectId + ".jar",
    assemblyMergeStrategy in assembly := {
      case "play/reference-overrides.conf" => MergeStrategy.concat
      case x => (assemblyMergeStrategy in assembly).value(x)
    },
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    mainClass in assembly := Some("ProjectileCLI"),

    (sourceGenerators in Compile) += ProjectVersion.writeConfig(Shared.projectId, Shared.projectName, Shared.projectPort).taskValue,

    // Code Quality
    scapegoatIgnoredFiles := Seq(".*/Routes.scala", ".*/RoutesPrefix.scala", ".*/*ReverseRoutes.scala", ".*/*.template.scala"),
    scapegoatDisabledInspections := Seq("UnusedMethodParameter")
  )

  lazy val server = Project(id = Shared.projectId, base = file(".")).enablePlugins(
    SbtWeb, play.sbt.PlayScala, diagram.ClassDiagramPlugin,
  ).settings(serverSettings: _*)
}