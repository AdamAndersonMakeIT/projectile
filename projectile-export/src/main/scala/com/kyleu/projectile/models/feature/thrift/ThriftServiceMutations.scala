package com.kyleu.projectile.models.feature.thrift

import com.kyleu.projectile.models.export.ExportField
import com.kyleu.projectile.models.export.config.ExportConfiguration
import com.kyleu.projectile.models.output.file.ThriftFile

object ThriftServiceMutations {
  private[this] val credsParam = "1: common.Credentials creds,"

  def writeMutations(config: ExportConfiguration, file: ThriftFile, pkFields: List[ExportField], retType: String) = {
    file.add(s"$retType insert(", 1)
    file.add(credsParam)
    file.add(s"2: required $retType model")
    file.add(")", -1)
    file.add("common.int insertBatch(", 1)
    file.add(credsParam)
    file.add(s"2: required list<$retType> models")
    file.add(")", -1)

    pkFields match {
      case Nil => // noop
      case pkf :: Nil =>
        val thriftType = ExportFieldThrift.thriftType(pkf.t, config)
        val thriftVisibility = if (pkf.required) { "required" } else { "optional" }

        file.add(s"$retType create(", 1)
        file.add(credsParam)
        file.add(s"2: $thriftVisibility  $thriftType ${pkf.propertyName},")
        file.add("3: list<common.DataField> fields")
        file.add(")", -1)

        file.add(s"$retType remove(", 1)
        file.add(credsParam)
        file.add(s"2: $thriftVisibility  $thriftType ${pkf.propertyName}")
        file.add(")", -1)

        file.add(s"$retType update(", 1)
        file.add(credsParam)
        file.add(s"2: $thriftVisibility  $thriftType ${pkf.propertyName},")
        file.add("3: list<common.DataField> fields")
        file.add(")", -1)
      case _ => // noop
    }
  }
}
