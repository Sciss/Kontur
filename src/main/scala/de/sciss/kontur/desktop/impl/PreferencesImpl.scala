package de.sciss.kontur
package desktop
package impl

import java.util.{prefs => j}

object PreferencesImpl {
  def user(clazz: Class[_]): Preferences =
    new Impl(j.Preferences.userNodeForPackage(clazz), isSystem = false, clazz.getName)

  def system(clazz: Class[_]): Preferences =
    new Impl(j.Preferences.systemNodeForPackage(clazz), isSystem = true, clazz.getName)

  private final class Impl(peer: j.Preferences, isSystem: Boolean, name: String) extends Preferences {
    import Preferences.Type

    override def toString = s"Preferences.${if (isSystem) "system" else "user"}($name)"

    def get[A](key: String)(implicit tpe: Type[A]): Option[A] = tpe.get(peer, key)
    def getOrElse[A](key: String, default: A)(implicit tpe: Type[A]): A = tpe.getOrElse(peer, key, default)
    def put[A](key: String, value: A)(implicit tpe: Type[A]) { tpe.put(peer, key, value) }
  }
}