package com.kyleu.projectile.models.cli

import com.kyleu.projectile.models.command.ProjectileResponse
import com.kyleu.projectile.models.command.ProjectileResponse._
import com.kyleu.projectile.models.input.{Input, InputSummary}
import com.kyleu.projectile.models.output.OutputWriteResult
import com.kyleu.projectile.models.project.audit.AuditResult
import com.kyleu.projectile.models.project.codegen.CodegenResult
import com.kyleu.projectile.models.project.{Project, ProjectOutput, ProjectSummary}
import com.kyleu.projectile.util.JacksonUtils.printJackson

object CommandLineOutput {
  def logResponse(r: ProjectileResponse) = logsFor(r).foreach(s => println(s)) // scalastyle:ignore
  def logsFor(r: ProjectileResponse): Seq[String] = r match {
    case OK(msg) => Seq(s"Success: $msg")
    case Error(msg) => Seq(s"Error: $msg")
    case JsonResponse(json) => Seq(printJackson(json))
    case InputList(inputs) => inputs.map(logForInputSummary)
    case InputDetail(input) => Seq(logForInput(input))
    case InputResults(results) => results.map(r => logForInput(r.input))
    case ProjectList(projects) => projects.map(logForProjectSummary)
    case ProjectDetail(p) => Seq(logForProject(p))
    case ProjectUpdateResult(key, resp) => s"[$key] Updated:" +: resp.map(" - " + _)
    case ProjectExportResult(output, files) => logForExportResult(output, files)
    case ProjectAuditResult(result, fixed) => logForAuditResult(result, fixed)
    case ProjectCodegenResult(result) => logForCodegenResult(result)
    case CompositeResult(results) => logForCompositeResult(results)
  }

  private[this] def logForInputSummary(is: InputSummary) = s"[${is.key}]: ${is.template.title}"
  private[this] def logForInput(input: Input) = {
    def process(keySingle: String, keyPlural: String, xs: Seq[String]) = if (xs.isEmpty) {
      Nil
    } else {
      s"  [${xs.size}] ${if (xs.size == 1) { keySingle } else { keyPlural }}:" +: xs.map("    " + _)
    }
    val enums = process("enum", "enums", input.enums.map(_.key))
    val models = process("model", "models", input.models.map(_.key))
    val services = process("service", "services", input.services.map(_.key))
    val unions = process("union", "unions", input.unions.map(_.key))
    (Seq(s"[${input.key}]: $input") ++ enums ++ models ++ services ++ unions).mkString("\n")
  }
  private[this] def logForProjectSummary(ps: ProjectSummary) = s"[${ps.key}]: ${ps.template.title}"
  private[this] def logForProject(project: Project) = {
    def process(keySingle: String, keyPlural: String, xs: Seq[(Seq[String], String)]) = if (xs.isEmpty) {
      Nil
    } else {
      s"  [${xs.size}] ${if (xs.size == 1) { keySingle } else { keyPlural }}:" +: {
        xs.groupBy(_._1.mkString(".")).map(x => x._1 -> x._2.map(_._2).sorted).toSeq.sortBy(_._1).map { v =>
          "  - [" + (if (v._1.isEmpty) { "root package" } else { v._1 }) + "]: " + v._2.mkString(", ")
        }
      }
    }
    val enums = process("enum", "enums", project.enums.map(e => e.pkg -> e.key))
    val models = process("model", "models", project.models.map(m => m.pkg -> m.key))
    val services = process("service", "services", project.services.map(s => s.pkg -> s.key))
    val unions = process("union", "unions", project.unions.map(u => u.pkg -> u.key))
    (Seq(s"[${project.key}]: $project") ++ enums ++ models ++ services ++ unions).mkString("\n")
  }

  def logForExportResult(output: ProjectOutput, files: Seq[OutputWriteResult]) = {
    val filesFiltered = files.filter(_.logs.nonEmpty)
    val fileMessages = if (filesFiltered.isEmpty) {
      Seq(" - No changes required")
    } else {
      filesFiltered.flatMap(f => s" - ${f.file}" +: f.logs.map(l => "   - " + l))
    }
    s"[${output.project.key}] Exported:" +: fileMessages
  }

  private[this] def logForAuditResult(result: AuditResult, fixed: Seq[String]) = {
    val fixMsgs = if (fixed.isEmpty) {
      Nil
    } else {
      s" - [${fixed.size}] Issues Fixed:" +: fixed.map(m => s"   - $m")
    }
    val cfgMsgs = if (result.configMessages.isEmpty) {
      Nil
    } else {
      s" - [${result.configMessages.size}] Config Messages:" +: result.configMessages.map(m => s"   - ${m.tgt}: ${m.message}")
    }
    val outputMsgs = if (result.outputMessages.isEmpty) {
      Nil
    } else {
      s" - [${result.outputMessages.size}] Output Messages:" +: result.outputMessages.map(m => s"   - ${m.tgt}: ${m.message}")
    }
    "Audit Result:" +: (fixMsgs ++ cfgMsgs ++ outputMsgs)
  }

  private[this] def logForCodegenResult(result: CodegenResult) = {
    val filteredExportResults = result.exportResults.filter(er => er._2.exists(_.logs.nonEmpty))
    val exportMessages = if (filteredExportResults.isEmpty) {
      Nil
    } else {
      s"Exported [${filteredExportResults.size}] projects:" +: filteredExportResults.flatMap { er =>
        s"  ${er._1.project.key}:" +: er._2.filter(_.logs.nonEmpty).map(msg => s"  - ${msg.file}: ${msg.logs.mkString(", ")}")
      }
    }
    val auditMessages = result.auditResults.toSeq.flatMap { aud =>
      val msgs = aud.configMessages ++ aud.outputMessages
      if (msgs.isEmpty) {
        Nil
      } else {
        s"[${msgs.size}] audit messages:" +: msgs.map(m => "  - " + m.t + "/" + m.tgt + ": " + m.message)
      }
    }
    result.updates ++ exportMessages ++ auditMessages
  }
  def logForCompositeResult(results: Seq[ProjectileResponse]): Seq[String] = results.size match {
    case 0 => Seq("No results")
    case 1 => logsFor(results.headOption.getOrElse(throw new IllegalStateException()))
    case _ => results.zipWithIndex.flatMap(r => s"Result [${r._2}]:" +: logsFor(r._1))
  }
}
