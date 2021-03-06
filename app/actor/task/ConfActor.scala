package actor.task


import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{ActorLogging, OneForOneStrategy, Props, Actor}
import play.api.libs.json.{JsObject}
import utils.{ProjectTask_v}

/**
 * Created by jinwei on 14/7/14.
 */
class ConfActor extends Actor with ActorLogging{

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch exception: ${e.getMessage} ${e.getStackTraceString}")
      Escalate
  }

//  var _json = Json.obj()
  val _reg = """\{\{ *[^}]+ *\}\}""".r
//  var _hostname = ""
//  var _taskObj: ProjectTask_v = null
//  var _envId = 0
//  var _projectId = 0
//  var _versionId = 0
//  var _order = 0

  def receive = {
    case CopyConfFile(taskId, envId, projectId, versionId, order, json, hostName, taskObj) => {
//      _json = json
//      _hostname = hostname
//      _taskObj = taskObj
//      _envId = envId
//      _projectId = projectId
//      _versionId = versionId
//      _order = order

      self ! GenerateConf(envId, projectId, versionId, order, json, hostName, taskObj)
    }

    case gc: GenerateConf => {
      val actorName = s"clusterActor_${gc.envId}_${gc.projectId}_${gc.hostname}"
      val clusterActor = context.child(actorName).getOrElse(context.actorOf(Props[ClusterActor], actorName))
      clusterActor ! GenerateClusterConfs(gc.envId, gc.projectId, gc.versionId, gc.taskObj, gc.hostname, gc.order)
    }

    case successConf: SuccessReplaceConf => {
//      context.parent ! ExecuteCommand(successConf.taskId, successConf.envId, successConf.projectId, successConf.versionId, successConf.order + 1)
      context.parent ! Execute()
//      context.stop(self)
    }

    case errorConf: ErrorReplaceConf => {
      context.parent ! ConfCopyFailed(errorConf.str)
//      context.stop(self)
    }

    case timeout: TimeoutReplace => {
      context.parent ! ConfCopyFailed(s"${timeout.key} 表达式执行超时!")
//      context.stop(self)
    }
  }
}

case class CopyConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int, order: Int, json: JsObject, hostname: String, taskObj: ProjectTask_v)
case class GenerateConf(envId: Int, projectId: Int, versionId: Int, order: Int, json: JsObject, hostname: String, taskObj: ProjectTask_v)