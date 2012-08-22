package models.orm

import org.squeryl.PrimitiveTypeMode._

object Schema extends org.squeryl.Schema {
  val users = table[User]("user")
  val documentSets = table[DocumentSet]("document_set")
  val documentSetCreationJobs = table[DocumentSetCreationJob]("document_set_creation_job")
  
  val documentSetUsers =
    manyToManyRelation(documentSets, users, "document_set_user").
      via[DocumentSetUser]((ds, u, dsu) => 
        (dsu.documentSetId === ds.id, dsu.userId === u.id))

  val documentSetDocumentSetCreationJobs =
    oneToManyRelation(documentSets, documentSetCreationJobs).
      via((ds, dscj) => ds.id === dscj.documentSetId)
}
