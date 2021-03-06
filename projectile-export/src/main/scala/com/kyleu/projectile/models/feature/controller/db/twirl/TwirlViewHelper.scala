// scalastyle:off file.size.limit
package com.kyleu.projectile.models.feature.controller.db.twirl

import com.kyleu.projectile.models.database.schema.ForeignKey
import com.kyleu.projectile.models.export.config.ExportConfiguration
import com.kyleu.projectile.models.export.typ.FieldType
import com.kyleu.projectile.models.export.{ExportField, ExportModel}
import com.kyleu.projectile.models.output.CommonImportHelper
import com.kyleu.projectile.models.output.file.TwirlFile

object TwirlViewHelper {
  def addButtons(config: ExportConfiguration, model: ExportModel, file: TwirlFile) = {
    val args = model.pkFields.map(field => s"model.${field.propertyName}").mkString(", ")
    if (model.pkFields.nonEmpty) {
      val url = TwirlHelper.routesClass(config, model)
      file.add(s"""<div class="right"><a class="btn @cfg.user.buttonColor" href="@$url.editForm($args)">Edit</a></div>""")
      val rc = TwirlHelper.routesClass(config, model)

      val onClick = s"""onclick="return confirm('Are you sure you want to remove this ${model.title}?')""""
      file.add(s"""<div class="right"><a class="btn-flat remove-link" $onClick href="@$rc.remove($args)">Remove</a></div>""")
    }
  }

  def addFields(config: ExportConfiguration, model: ExportModel, file: TwirlFile) = {
    file.add("<table class=\"highlight responsive-table\">", 1)
    file.add("<tbody>", 1)
    model.fields.foreach { field =>
      file.add("<tr>", 1)
      val clipboard = if (field.required) {
        s"""@model.${field.propertyName}.toString.replaceAllLiterally("'", "")"""
      } else {
        s"""@model.${field.propertyName}.map(_.toString.replaceAllLiterally("'", ""))"""
      }
      val thContent = s"""<div title="Click to copy" onclick="ClipboardUtils.writeClipboard('$clipboard')" style="cursor: pointer;">${field.title}</div>"""
      file.add(s"""<th style="vertical-align: top;">$thContent</th>""")
      val fkOpt = model.foreignKeys.find(_.references.forall(_.source == field.key)).filter(fk => config.getModelOpt(fk.targetTable).isDefined)
      fkOpt match {
        case Some(fk) => refRow(config, file, field, fk)
        case _ => file.add(s"<td>${forField(config, field)}</td>")
      }
      file.add("</tr>", -1)
    }
    file.add("</tbody>", -1)
    file.add("</table>", -1)
  }

  private[this] def refRow(config: ExportConfiguration, file: TwirlFile, field: ExportField, fk: ForeignKey) = {
    file.add("<td>", 1)
    file.add("<div>", 1)
    val tgt = config.getModel(fk.targetTable, s"foreign key ${fk.name}")
    if (!tgt.pkFields.forall(f => fk.references.map(_.target).contains(f.key))) {
      throw new IllegalStateException(s"FK [$fk] does not match PK [${tgt.pkFields.map(_.key).mkString(", ")}]...")
    }
    file.add(forField(config, field))
    val icon = TwirlHelper.iconHtml(config = config, propertyName = tgt.propertyName, provided = tgt.provided)
    file.add("</div>", -1)
    file.add(s"@${field.propertyName}R.map { r =>", 1)
    file.add("""<div class="card-panel grey lighten-5" style="padding: 12px; margin: 12px 0 0 0;">""", 1)
    file.add("""<div>""", 1)
    if (field.required) {
      file.add(s"""<a href="@${TwirlHelper.routesClass(config, tgt)}.view(model.${field.propertyName})">""", 1)
    } else {
      file.add(s"@model.${field.propertyName}.map { v =>", 1)
      file.add(s"""<a href="@${TwirlHelper.routesClass(config, tgt)}.view(v)">""", 1)
    }
    file.add(s"$icon <strong>${tgt.title}</string>")
    file.add("</a>", -1)
    if (!field.required) {
      file.add("}", -1)
    }
    file.add("</div>", -1)
    file.add(s"@${(config.systemViewPackage :+ "html").mkString(".")}.components.dataSummary(r.toSummary)")
    file.add("</div>", -1)
    file.add("}", -1)
    file.add("</td>", -1)
  }

  private[this] def forField(config: ExportConfiguration, field: ExportField) = field.t match {
    case FieldType.BooleanType if field.required => s"@com.kyleu.projectile.views.html.components.form.booleanDisplay(model.${field.propertyName})"
    case FieldType.BooleanType => s"@model.${field.propertyName}.map(x => com.kyleu.projectile.views.html.components.form.booleanDisplay(x))"

    case FieldType.ListType(_) if field.required => s"""@model.${field.propertyName}.mkString(", ")"""
    case FieldType.ListType(_) => s"""@model.${field.propertyName}.map(_.mkString(", "))"""
    case FieldType.SetType(_) if field.required => s"""@model.${field.propertyName}.mkString(", ")"""
    case FieldType.SetType(_) => s"""@model.${field.propertyName}.map(_.mkString(", "))"""
    case FieldType.MapType(_, _) if field.required => s"""@model.${field.propertyName}.map(x => x._2 + "=" + x._1).mkString(", ")"""
    case FieldType.MapType(_, _) => s"""@model.${field.propertyName}.map(_.map(x => x._2 + "=" + x._1).mkString(", "))"""
    case FieldType.TagsType if field.required => s"""@model.${field.propertyName}.map(x => x.k + "=" + x.v).mkString(", ")"""
    case FieldType.TagsType => s"""@model.${field.propertyName}.map(_.map(x => x.k + "=" + x.v).mkString(", "))"""

    case FieldType.CodeType | FieldType.JsonType if field.required => s"<pre>@model.${field.propertyName}</pre>"
    case FieldType.CodeType | FieldType.JsonType => s"<pre>@model.${field.propertyName}.getOrElse(com.kyleu.projectile.util.NullUtils.str)</pre>"

    case _ if field.required => s"@model.${field.propertyName}"
    case _ =>
      val nu = CommonImportHelper.getString(config, "NullUtils")
      s"@model.${field.propertyName}.getOrElse($nu.str)"
  }
}
