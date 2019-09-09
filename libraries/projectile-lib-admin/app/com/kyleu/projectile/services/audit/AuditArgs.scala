package com.kyleu.projectile.services.audit

import java.util.UUID

import com.kyleu.projectile.util.DateUtils
import enumeratum.values.{StringEnum, StringEnumEntry}

object AuditArgs {
  def getArg(id: IndexedSeq[String], i: Int) = {
    if (id.lengthCompare(i) <= 0) { throw new IllegalStateException(s"Needed at least [${i + 1}] id arguments, only have [${id.length}]") }
    id(i)
  }

  def stringArg(s: String) = s
  def booleanArg(s: String) = s.toBoolean
  def integerArg(s: String) = s.toInt
  def longArg(s: String) = s.toLong
  def floatArg(s: String) = s.toFloat
  def uuidArg(s: String) = UUID.fromString(s)
  def byteArg(s: String) = s.toInt.toByte
  def dateArg(s: String) = DateUtils.fromDateString(s)
  def timeArg(s: String) = DateUtils.fromTimeString(s)
  def timestampArg(s: String) = DateUtils.fromIsoString(s)
  def enumArg[T <: StringEnumEntry](t: StringEnum[T])(s: String) = t.withValue(s)
}
