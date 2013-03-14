package de.sciss.kontur
package desktop

import java.util.{prefs => j}
import impl.{PreferencesImpl => Impl}

object Preferences {
  object Type {
    import scala.{Int => SInt}
    import java.lang.{String => SString}

    implicit object String extends Type[SString] {
      private[desktop] def put(prefs: j.Preferences, key: SString, value: SString) {
        prefs.put(key, value)
      }

      private[desktop] def get(prefs: j.Preferences, key: SString): Option[SString] = Option(prefs.get(key))

      private[desktop] def getOrElse(prefs: j.Preferences, key: SString, default: SString): SString =
        prefs.get(key, default)
    }

    implicit object Int extends Type[SInt] {
      private[desktop] def put(prefs: j.Preferences, key: SString, value: SInt) {
        prefs.put(key, value.toString)
      }

      private[desktop] def get(prefs: j.Preferences, key: SString): Option[SInt] = try {
        val s = prefs.get(key, null)
        if (s == null) None else Some(s.toInt)
      } catch {
        case _: NumberFormatException => None
      }

      private[desktop] def getOrElse(prefs: j.Preferences, key: SString, default: SInt): SInt = try {
        val s = prefs.get(key, null)
        if (s == null) default else s.toInt
      } catch {
        case _: NumberFormatException => default
      }
    }
  }
  sealed trait Type[A] {
    private[desktop] def put(prefs: j.Preferences, key: String, value: A): Unit
    private[desktop] def get(prefs: j.Preferences, key: String): Option[A]
    private[desktop] def getOrElse(prefs: j.Preferences, key: String, default: A): A
  }

  def user  (clazz: Class[_]): Preferences = Impl.user  (clazz)
  def system(clazz: Class[_]): Preferences = Impl.system(clazz)
}
trait Preferences {
  import Preferences.Type

  def get[A: Type](key: String): Option[A]
  def getOrElse[A: Type](key: String, default: A): A
  def put[A: Type](key: String, value: A): Unit
}