package models.orm

import org.joda.time.DateTime
import org.squeryl.annotations.Column
import org.squeryl.dsl.OneToMany
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import models.orm.Dsl.{crypt,gen_hash}

class User(
    var email: String,
    //var role: UserRole.UserRole,
    @Column("password_hash")
    var passwordHash: String
    //@Column("confirmation_token")
    //var confirmationToken: Option[String],
    // FIXME get DateTime mapped
    //@Column("confirmation_sent_at")
    //var confirmationSentAt: Option[DateTime],
    //@Column("confirmed_at")
    //var confirmedAt: Option[DateTime],
    //@Column("reset_password_token")
    //var resetPasswordToken: Option[String],
    //@Column("reset_password_sent_at")
    //var resetPasswordSentAt: Option[DateTime],
    //@Column("current_sign_in_at")
    //var currentSignInAt: Option[DateTime],
    //@Column("current_sign_in_ip")
    //var currentSignInIp: Option[String],
    //@Column("last_sign_in_at")
    //var lastSignInAt: Option[DateTime],
    //@Column("last_sign_in_ip")
    //var lastSignInIp: Option[String]
    ) extends KeyedEntity[Long] {

  val id: Long = 0

  lazy val documentSets = Schema.documentSetUsers.right(this)
}

object User {
  def findById(id: Long) = Schema.users.lookup(id)

  def authenticate(email: String, password: String) : Option[User] = {
    Schema.users.where(
      u => u.email === email and crypt(password, u.passwordHash) === u.passwordHash
    ).headOption
  }
}
