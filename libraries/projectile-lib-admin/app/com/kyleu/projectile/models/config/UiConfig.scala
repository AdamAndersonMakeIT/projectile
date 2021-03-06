package com.kyleu.projectile.models.config

import java.util.UUID

import com.kyleu.projectile.models.menu.NavMenu
import com.kyleu.projectile.models.notification.Notification

final case class UiConfig(
    projectName: String,
    projectVersion: String,
    userId: Option[UUID] = None,
    username: Option[String] = None,
    menu: Seq[NavMenu] = Nil,
    urls: NavUrls = NavUrls(),
    html: NavHtml = NavHtml(),
    content: NavContent = NavContent(),
    user: UserSettings = UserSettings.empty,
    notifications: Seq[Notification] = Nil,
    breadcrumbs: Seq[BreadcrumbEntry] = Nil
)
