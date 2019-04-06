package com.kyleu.projectile.models.typescript.output.parse

import better.files.File
import com.kyleu.projectile.models.export.config.ExportConfiguration
import com.kyleu.projectile.models.export.typ.FieldType
import com.kyleu.projectile.models.output.file.{MarkdownFile, ScalaFile}
import com.kyleu.projectile.models.output.{ExportHelper, OutputPath}
import com.kyleu.projectile.models.typescript.input.TypeScriptInput
import com.kyleu.projectile.models.typescript.node.{SourceFileHeader, TypeScriptNode}
import com.kyleu.projectile.models.typescript.node.TypeScriptNode.SourceFile
import com.kyleu.projectile.models.typescript.output.OutputHelper
import com.kyleu.projectile.models.typescript.output.parse.ModuleParser.filter

object SourceFileParser {
  def forSourceFiles(ctx: ParseContext, nodes: Seq[SourceFile]) = {
    forHeader(ctx = ctx, header = nodes.foldLeft(SourceFileHeader())((l, r) => l.merge(r.header)), nodes = nodes)
  }

  private[this] def forHeader(ctx: ParseContext, header: SourceFileHeader, nodes: Seq[SourceFile]) = {
    val file = MarkdownFile(OutputPath.SharedSource, ctx.pkg, "index")
    file.add()

    val nameLink = header.projectName.map(n => header.projectUrl match {
      case Some(url) => s"[$n]($url)"
      case None => n
    }).orElse(nodes.headOption.map(_.path).map(x => TypeScriptInput.stripName(x.substring(x.lastIndexOf('/') + 1)))).getOrElse("Ad-hoc")

    file.add(s"# $nameLink Scala.js facade")
    file.add()

    file.add("## Generated code")
    file.add()
    file.add(s"The contents of this package were generated by [Projectile](https://kyleu.com/projectile)")
    file.add()
    nodes.foreach(node => file.add(s"  - ${ctx.root.relativize(File(node.path)).toString}"))

    if (header.authors.nonEmpty) {
      file.add()
      file.add("## Definition authors")
      file.add()
      header.authors.foreach(a => file.add(s"  - $a"))
    }

    if (header.content.nonEmpty) {
      file.add()
      file.add("## Original TypeScript comments")
      file.add()
      file.add("```typescript")
      header.content.foreach(file.add(_))
      file.add("```")
    }

    file
  }

  def parse(ctx: ParseContext, config: ExportConfiguration, node: SourceFile) = {
    val path = node.path.split('/').reverse.dropWhile(x => x == "index" || x == "index.ts" || x == "index.d.ts").reverse.toList
    val fn = ExportHelper.escapeKeyword(TypeScriptInput.stripName(path.last))
    val cn = ExportHelper.toClassName(fn)

    val filterResult = MemberParser.filter(filter(node.statements))
    val filteredMembers = filterResult.members
    val objFile = if (filteredMembers.isEmpty) {
      Nil
    } else {
      val file = ScalaFile(path = OutputPath.SharedSource, dir = config.mergedApplicationPackage(ctx.pkg), key = cn)

      OutputHelper.printContext(file, node.ctx)
      MemberHelper.addGlobal(file, config, ctx, Some(fn), filterResult.globalScoped)
      file.add(s"object $cn extends js.Object {", 1)
      filteredMembers.foreach(m => MemberParser.print(ctx = ctx, config = config, tsn = m, file = file, last = filteredMembers.lastOption.contains(m)))
      file.add("}", -1)
      file.add()
      Seq(file)
    }

    filterResult.extraClasses.foldLeft(ctx -> config.withAdditional(objFile: _*)) { (carry, decl) =>
      ObjectTypeParser.parseLiteral(carry._1, carry._2, decl._1, decl._2, decl._3, decl._4)
    }
  }
}

