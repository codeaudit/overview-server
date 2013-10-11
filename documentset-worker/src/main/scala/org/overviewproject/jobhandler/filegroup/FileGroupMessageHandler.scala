package org.overviewproject.jobhandler.filegroup

import akka.actor._
import akka.actor.SupervisorStrategy._
import org.overviewproject.jobhandler.filegroup.TextExtractorProtocol.ExtractText
import org.overviewproject.jobhandler.JobProtocol._
import org.overviewproject.jobhandler.MessageHandlerProtocol._

import FileGroupMessageHandlerFSM._
import scala.concurrent.duration.Duration

trait FileGroupMessageHandlerComponent {
  val actorCreator: ActorCreator
  val storage: Storage
  
  trait ActorCreator {
    def produceTextExtractor: Props
  }
  
  trait Storage {
    def writeFileInErrorState(fileGroupId: Long, uploadedFileId: Long): Unit
  }
}

object FileGroupMessageHandlerProtocol {
  sealed trait Command
  case class ProcessFileCommand(fileGroupId: Long, uploadedFileId: Long) extends Command
}

object FileGroupMessageHandlerFSM {
  sealed trait State
  case object Idle extends State
  case object Working extends State

  sealed trait Data
  case object NoData extends Data
  case class Job(worker: ActorRef, fileGroupId: Long, uploadedFileId: Long) extends Data

}

/**
 * Spawns actors to process files.
 */
class FileGroupMessageHandler(jobMonitor: ActorRef) extends Actor with FSM[State, Data] {
  this: FileGroupMessageHandlerComponent =>

  import FileGroupMessageHandlerProtocol._

  override val supervisorStrategy =
    OneForOneStrategy(0, Duration.Inf) {
      case _: Throwable => Stop
    }

  startWith(Idle, NoData)

  when(Idle) {
    case Event(ProcessFileCommand(fileGroupId, uploadedFileId), _) => {
      val fileHandler = context.actorOf(actorCreator.produceTextExtractor)
      context.watch(fileHandler)
      fileHandler ! ExtractText(fileGroupId, uploadedFileId)

      goto(Working) using Job(fileHandler, fileGroupId, uploadedFileId)
    }
  }

  when(Working) {
    case Event(JobDone(fileGroupId), job: Job) => {
      jobMonitor ! JobDone(fileGroupId)
      context.unwatch(job.worker)
      
      goto(Idle)
    }
    case Event(Terminated(worker), job: Job) => {
      storage.writeFileInErrorState(job.fileGroupId, job.uploadedFileId)
      jobMonitor ! JobDone(job.fileGroupId)
      goto(Idle)
    }

  }

  initialize
}

trait FileGroupMessageHandlerComponentImpl extends FileGroupMessageHandlerComponent {
  override val actorCreator = new TextExtractorCreator
  override val storage = new DatabaseStorage 
  
  class TextExtractorCreator extends ActorCreator {
    override def produceTextExtractor: Props = Props[TextExtractorImpl]
  }
  
  class DatabaseStorage extends Storage {
    override def writeFileInErrorState(fileGroupId: Long, uploadedFileId: Long): Unit = ???
  }

}

object FileGroupMessageHandler {
  class FileGroupMessageHandlerImpl(jobMonitor: ActorRef) extends FileGroupMessageHandler(jobMonitor) with FileGroupMessageHandlerComponentImpl

  def apply(jobMonitor: ActorRef): Props = Props(new FileGroupMessageHandlerImpl(jobMonitor))
}
