package auth

package object model {
  case class User(
      id: String,
      email: String,
      name: String,
      password: String,
      public_id: PublicId,
      role: String
  )

  type PublicId = String

  object roles {
    val Admin: String = "Admin"
    val User: String = "User"
    val Manager: String = "Manager"

    val all: Set[String] = Set(Admin, User, Manager)

  }

}
