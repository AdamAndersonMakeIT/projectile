package com.kyleu.projectile.models.project.member

import com.kyleu.projectile.models.feature.ModelFeature
import com.kyleu.projectile.util.JsonSerializers._

object ModelMember {
  implicit val jsonEncoder: Encoder[ModelMember] = deriveEncoder
  implicit val jsonDecoder: Decoder[ModelMember] = deriveDecoder
}

final case class ModelMember(
    key: String,
    pkg: Seq[String] = Nil,

    features: Set[ModelFeature] = Set.empty,
    ignored: Set[String] = Set.empty,
    overrides: Seq[MemberOverride] = Nil
) {
  def getOverride(key: String, default: => String) = overrides.find(_.k == key).map(_.v).getOrElse(default)
}
