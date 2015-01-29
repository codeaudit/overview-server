package org.overviewproject.background.filegroupcleanup

import akka.actor.{ Actor, ActorRef }

object FileGroupRemovalRequestQueueProtocol {
  case class RemoveFileGroup(fileGroupId: Long)
}


/**
 * Queue for [[FileGroup]] removal requests. When one request is complete, the next one is sent.
 * 
 * No checks for duplicate requests are made
 */
trait FileGroupRemovalRequestQueue extends Actor {
  import FileGroupRemovalRequestQueueProtocol._
  import FileGroupCleanerProtocol._

  import scala.collection.mutable.Queue
  protected val requests: Queue[Long] = Queue[Long]() // The first element in the queue is in progress

  override def receive = {
    case RemoveFileGroup(fileGroupId) => {
      requests.enqueue(fileGroupId)

      if (readyToSubmitRequest) submitNextRequest
    }
    case CleanComplete(fileGroupId) => {
      requests.dequeue
      submitNextRequest
    }
  }

  private def readyToSubmitRequest: Boolean = requests.size == 1
  private def submitNextRequest: Unit = requests.headOption.map(fileGroupCleaner ! Clean(_))
  
  protected val fileGroupCleaner: ActorRef
}