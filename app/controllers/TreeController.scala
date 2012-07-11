package controllers

import scala.collection.JavaConversions._
import scala.io
import scala.util.Random

import play.api.Play
import play.api.Play.current
import play.api.mvc._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json._
import models._
import views.json.Tree.JsonHelpers

object TreeController extends Controller {
	
	def temporarySetup() = Action {
	  val root = new Node()
	  root.description = "root"

	  val documentSet = new DocumentSet()
	  for (i <- 1 to 2200) {
	    val document = new Document("document-" + i, "textUrl-" + i, "viewUrl-" + i)
	    documentSet.addDocument(document)
	    document.save
	  }

      documentSet.save

	  generateTreeLevel(root, documentSet.documents.toSeq, 12)

	  val tree = new Tree()
	  tree.root = root
	  tree.save()

	  Ok("Setup complete")
	}
	
	def generateTreeLevel(root: Node, documents: Seq[Document], depth: Int) = {
    documents.foreach(root.addDocument)

	  if ((depth > 1) && (documents.size > 1)) {
	    val numberOfChildren = Random.nextInt(scala.math.min(7, documents.size)) + 1
	    val children = generateChildren(numberOfChildren, documents, depth)
	    children.foreach(root.addChild)
	  }
	}

	def generateChildren(numberOfSiblings: Int, documents: Seq[Document], depth: Int) : Seq[Node] = {
    val child = new Node()
    child.description = "node height " + depth

	  if (numberOfSiblings > 1) {
	    val splitPoint = 
	      if (documents.size > numberOfSiblings) Random.nextInt(documents.size - numberOfSiblings) + 1 else 1 
	    
	    val (childDocuments, siblingDocuments) = documents.splitAt(splitPoint)
	    
	    generateTreeLevel(child, childDocuments, depth - 1)
	    
	    Seq[Node](child) ++ generateChildren(numberOfSiblings - 1, siblingDocuments, depth)
	  }
	  else {
      generateTreeLevel(child, documents, depth - 1)
      Seq[Node](child)
	  }
	}
	
    def root(id: Long) = Action {
// Leaving this here in case the complete JSON from the file is needed in testing the UI      
//        val file = Play.application.getFile("conf/stub-tree-root.json")
//        val json = io.Source.fromFile(file).mkString
//      SimpleResult(
//            header = ResponseHeader(200, Map(CONTENT_TYPE -> "application/json")),
//            body = Enumerator(json)
//        )
      val tree = Tree.find.byId(id) // FIXME handle security
      val json = JsonHelpers.rootNodeToJsValue(tree.root)
      Ok(json)
    }

    def node(treeId: Long, nodeId: Long) = Action {
      val node = Node.find.byId(nodeId) // FIXME handle security
      val json = JsonHelpers.rootNodeToJsValue(node)
      Ok(json)
    }
}
