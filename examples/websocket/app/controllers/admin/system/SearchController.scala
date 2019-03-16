package controllers.admin.system

import java.util.UUID

import com.google.inject.Injector
import com.kyleu.projectile.controllers.AuthController
import com.kyleu.projectile.models.Application
import com.kyleu.projectile.models.auth.{AuthActions, UserCredentials}
import com.kyleu.projectile.util.tracing.TraceData
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@javax.inject.Singleton
class SearchController @javax.inject.Inject() (override val app: Application, authActions: AuthActions, injector: Injector) extends AuthController("search") {
  def search(q: String) = withSession("admin.search", admin = true) { implicit request => implicit td =>
    val creds = UserCredentials.fromRequest(request)
    val results = try {
      searchInt(creds, q, q.toInt)
    } catch {
      case _: NumberFormatException => try {
        searchUuid(creds, q, UUID.fromString(q))
      } catch {
        case _: IllegalArgumentException => searchString(creds, q)
      }
    }
    results.map { r =>
      Ok(com.kyleu.projectile.views.html.admin.explore.searchResults(q, r, request.identity, authActions))
    }
  }

  private[this] def searchInt(creds: UserCredentials, q: String, id: Int)(implicit timing: TraceData) = {
    val intSearches = Seq[Future[Seq[Html]]]() ++
      /* Start int searches */
      /* End int searches */
      Nil

    Future.sequence(intSearches).map(_.flatten)
  }

  private[this] def searchUuid(creds: UserCredentials, q: String, id: UUID)(implicit timing: TraceData) = {
    val uuidSearches = Seq[Future[Seq[Html]]]() ++
      /* Start uuid searches */
      /* Projectile export section [websocket] */
      Seq(
        injector.getInstance(classOf[services.note.NoteRowService]).getByPrimaryKey(creds, id).map(_.map(model => views.html.admin.note.noteRowSearchResult(model, s"Note [${model.id}] matched [$q]")).toSeq),
        injector.getInstance(classOf[services.auth.SystemUserRowService]).getByPrimaryKey(creds, id).map(_.map(model => views.html.admin.auth.systemUserRowSearchResult(model, s"System User [${model.id}] matched [$q]")).toSeq)
      ) ++
        /* End uuid searches */
        Nil

    Future.sequence(uuidSearches).map(_.flatten)
  }

  private[this] def searchString(creds: UserCredentials, q: String)(implicit timing: TraceData) = {
    val stringSearches = Seq[Future[Seq[Html]]]() ++
      /* Start string searches */
      /* Projectile export section [websocket] */
      Seq(
        injector.getInstance(classOf[services.note.NoteRowService]).searchExact(creds, q = q, limit = Some(5)).map(_.map(model => views.html.admin.note.noteRowSearchResult(model, s"Note [${model.id}] matched [$q]"))),
        injector.getInstance(classOf[services.auth.Oauth2InfoRowService]).searchExact(creds, q = q, limit = Some(5)).map(_.map(model => views.html.admin.auth.oauth2InfoRowSearchResult(model, s"Oauth2 Info [${model.provider}, ${model.key}] matched [$q]"))),
        injector.getInstance(classOf[services.auth.PasswordInfoRowService]).searchExact(creds, q = q, limit = Some(5)).map(_.map(model => views.html.admin.auth.passwordInfoRowSearchResult(model, s"Password Info [${model.provider}, ${model.key}] matched [$q]"))),
        injector.getInstance(classOf[services.auth.SystemUserRowService]).searchExact(creds, q = q, limit = Some(5)).map(_.map(model => views.html.admin.auth.systemUserRowSearchResult(model, s"System User [${model.id}] matched [$q]")))
      ) ++
        /* End string searches */
        Nil

    Future.sequence(stringSearches).map(_.flatten)
  }
}
