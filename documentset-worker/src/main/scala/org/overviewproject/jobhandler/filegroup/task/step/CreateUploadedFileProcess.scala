package org.overviewproject.jobhandler.filegroup.task.step

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.actor.ActorRef
import org.overviewproject.jobhandler.filegroup.task.UploadProcessOptions
import org.overviewproject.jobhandler.filegroup.task.UploadedFileProcessCreator
import org.overviewproject.jobhandler.filegroup.task.process.UploadedFileProcess
import org.overviewproject.models.GroupedFileUpload
import org.overviewproject.util.BulkDocumentWriter



/**
 * Create a process to convert a [[GroupedFileUpload]] into [[Document]](s).
 */
trait CreateUploadedFileProcess extends UploadedFileProcessStep {
  override protected val documentSetId: Long

  protected val uploadedFile: GroupedFileUpload
  protected val options: UploadProcessOptions
  protected val documentIdSupplier: ActorRef
  protected val bulkDocumentWriter: BulkDocumentWriter

  override protected lazy val filename: String = uploadedFile.name

  protected val uploadedFileProcessCreator: UploadedFileProcessCreator

  override protected def doExecute: Future[TaskStep] = for {
    process <- createProcess
    firstStep <- process.start(uploadedFile)
  } yield firstStep

  private def createProcess: Future[UploadedFileProcess] = AsFuture {
    uploadedFileProcessCreator.create(uploadedFile, options, documentSetId,
      documentIdSupplier, bulkDocumentWriter)
  }
}

object CreateUploadedFileProcess {
  def apply(documentSetId: Long, uploadedFile: GroupedFileUpload, options: UploadProcessOptions,
            documentIdSupplier: ActorRef,
            bulkDocumentWriter: BulkDocumentWriter)(implicit executor: ExecutionContext): CreateUploadedFileProcess =
    new CreateUploadedFileProcessImpl(documentSetId, uploadedFile, options,
      documentIdSupplier, bulkDocumentWriter)

  private class CreateUploadedFileProcessImpl(
    override protected val documentSetId: Long,
    override protected val uploadedFile: GroupedFileUpload,
    override protected val options: UploadProcessOptions,
    override protected val documentIdSupplier: ActorRef,
    override protected val bulkDocumentWriter: BulkDocumentWriter
  )(override implicit protected val executor: ExecutionContext) extends CreateUploadedFileProcess {
    override protected val uploadedFileProcessCreator = UploadedFileProcessCreator(bulkDocumentWriter)
  }

}
