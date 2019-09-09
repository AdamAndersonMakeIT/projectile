package com.kyleu.projectile.services.process

import java.util.UUID

import com.kyleu.projectile.models.auth.UserCredentials
import com.kyleu.projectile.models.process.CachedProc
import com.kyleu.projectile.util.{DateUtils, Logging}

object ProcessService extends Logging {
  private[this] val activeProcesses = collection.mutable.HashMap.empty[UUID, CachedProc]
  private[this] val completedProcesses = collection.mutable.HashMap.empty[UUID, CachedProc]

  def getActive = activeProcesses.values.toIndexedSeq.sortBy(p => p.started.map(DateUtils.toMillis)).reverse

  def getProc(id: UUID) = activeProcesses.get(id).orElse(completedProcesses.get(id)).getOrElse {
    throw new IllegalStateException(s"Cannot find process with id [$id]")
  }

  def isAllowed(creds: UserCredentials, cmd: Seq[String]) = creds.user.role == "admin"

  def start(creds: UserCredentials, cmd: Seq[String], onOutput: CachedProc.Output => Unit, onComplete: (Int, Long) => Unit, async: Boolean = false) = {
    if (!isAllowed(creds, cmd)) {
      throw new IllegalStateException(s"User [${creds.user.id}] is not allowed to run command [${cmd.mkString(" ")}]")
    }
    val p = CachedProc(cmd, onOutput, onComplete)
    activeProcesses(p.id) = p
    if (async) {
      new Thread(() => p.run())
    } else {
      p.run()
    }
    p
  }
}
