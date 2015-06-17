package org.overviewproject.jobhandler.filegroup.task.process

import scala.concurrent.ExecutionContext
import org.overviewproject.util.BulkDocumentWriter
import akka.actor.ActorRef

object CreateDocumentsFromConvertedFilePages {
  def apply(documentSetId: Long, filename: String,
            documentIdSupplier: ActorRef, bulkDocumentWriter: BulkDocumentWriter)
  (implicit executor: ExecutionContext) = new UploadedFileProcess {
    override protected val steps =
      DoCreateFileWithView(documentSetId).andThen(
        DoCreatePdfPages(documentSetId).andThen(
          DoRequestDocumentIds(documentIdSupplier, documentSetId, filename).andThen(
            DoWriteDocuments(documentSetId, filename, bulkDocumentWriter))))
  }
}