package com.kyleu.projectile.services.user

import com.kyleu.projectile.models.queries.auth.PasswordInfoQueries
import com.kyleu.projectile.services.database.ApplicationDatabase
import com.kyleu.projectile.util.tracing.OpenTracingService
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@javax.inject.Singleton
class PasswordInfoService @javax.inject.Inject() (tracingService: OpenTracingService) extends DelegableAuthInfoDAO[PasswordInfo] {

  override def find(loginInfo: LoginInfo) = tracingService.noopTrace("password.find") { implicit td =>
    Future.successful(ApplicationDatabase.query(PasswordInfoQueries.getByPrimaryKey(loginInfo.providerID, loginInfo.providerKey)))
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo) = tracingService.noopTrace("password.add") { implicit td =>
    ApplicationDatabase.executeF(PasswordInfoQueries.CreatePasswordInfo(loginInfo, authInfo)).map(_ => authInfo)
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = tracingService.noopTrace("password.update") { implicit td =>
    ApplicationDatabase.executeF(PasswordInfoQueries.UpdatePasswordInfo(loginInfo, authInfo)).map(_ => authInfo)
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo) = tracingService.noopTrace("password.save") { implicit td =>
    ApplicationDatabase.executeF(PasswordInfoQueries.UpdatePasswordInfo(loginInfo, authInfo))(td).flatMap { rowsAffected =>
      if (rowsAffected == 0) {
        ApplicationDatabase.executeF(PasswordInfoQueries.CreatePasswordInfo(loginInfo, authInfo))(td).map(_ => authInfo)
      } else {
        Future.successful(authInfo)
      }
    }
  }

  override def remove(loginInfo: LoginInfo) = tracingService.topLevelTrace("password.remove") { implicit td =>
    ApplicationDatabase.executeF(PasswordInfoQueries.removeByPrimaryKey(loginInfo.providerID, loginInfo.providerKey)).map(_ => {})
  }
}
