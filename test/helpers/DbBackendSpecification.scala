package controllers.backend

import java.sql.Connection
import org.specs2.execute.AsResult
import org.specs2.mutable.{After, Specification}
import org.specs2.specification.{Fragments, Step}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{Await,Future}
import scala.slick.jdbc.JdbcBackend.Session
import scala.slick.jdbc.UnmanagedSession

import org.overviewproject.database.DB
import org.overviewproject.test.DbSpecification
import test.helpers.factories.{Factory,DbFactory}

/** Helps test Backends.
  *
  * Backends read from and write to the database. So our tests will write to
  * and read from the database. Here's how:
  *
  * class MyDbBackendSpec extends DbBackendSpecification {
  *   "MyDbBackend" should {
  *     "find a DocumentSet" in new DbScope {
  *       val backend = new TestDbBackend(session) with MyDbBackend
  *
  *       val documentSet = factory.documentSet()
  *       val foundDocumentSet = backend.findDocumentSet(documentSet.id).headOption
  *       foundDocumentSet.map(_.id) must beSome(documentSet.id)
  *     }
  *   }
  * }
  */
trait DbBackendSpecification
  extends DbSpecification
{
  override def map(fs: => Fragments) = {
    Step(setupDb) ^ super.map(fs) ^ Step(shutdownDb)
  }

  class TestDbBackend(val session: Session) extends DbBackend {
    override def db[A](block: Session => A): Future[A] = Future {
      block(session)
    }
  }

  trait DbScope extends After {
    var connected = false
    lazy val connection: Connection = {
      val ret = DB.getConnection()
      connected = true
      ret.setAutoCommit(false)
      ret
    }
    lazy val session: Session = new UnmanagedSession(connection)
    lazy val factory: Factory = new DbFactory(connection)

    def await[A](f: Future[A]) = Await.result(f, Duration(2, "seconds"))

    def after = {
      if (connected) {
        connection.rollback()
        connection.close()
      }
    }
  }
}