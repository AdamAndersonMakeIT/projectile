package com.projectile.services.project.audit

import better.files.File
import com.projectile.models.export.ExportModel
import com.projectile.models.export.config.ExportConfiguration
import com.projectile.models.export.typ.FieldType.EnumType
import com.projectile.models.project.ProjectOutput
import com.projectile.models.project.audit.{AuditMessage, AuditResult}

object ProjectAuditService {
  def audit(projectRoot: File, c: ExportConfiguration, o: ProjectOutput) = {
    val missing = c.models.flatMap(checkMissing(c, _))
    val configMessages = missing

    val orphans = ExportValidation.validate(projectRoot = projectRoot, config = c, result = o).map { o =>
      AuditMessage(srcModel = o._1, src = o._1, t = "orphan", tgt = o._1, message = o._2)
    }
    val outputMessages = orphans

    AuditResult(config = c, configMessages = configMessages, output = o, outputMessages = outputMessages)
  }

  private[this] def checkMissing(c: ExportConfiguration, m: ExportModel) = {
    val missingEnums = m.fields.flatMap(_.t match {
      case EnumType(key) => c.getEnumOpt(key) match {
        case None => Some(AuditMessage(
          srcModel = m.key, src = key, t = "enum", tgt = key, message = "Missing enum definition"
        ))
        case _ => None
      }
      case _ => None
    })

    val missingModels = m.references.flatMap {
      case r if c.getModelOpt(r.srcTable).isEmpty => Some(AuditMessage(
        srcModel = m.key, src = r.name, t = "model", tgt = r.srcTable, message = "Missing model definition"
      ))
      case _ => None
    }
    missingEnums ++ missingModels
  }
}
