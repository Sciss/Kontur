package de.sciss.kontur
package desktop

import java.util.{prefs => j}
import impl.{PreferencesImpl => Impl}

object Preferences {
  object Type {
    import scala.{Int => SInt}
    import java.lang.{String => SString}
    import java.io.{File => SFile}

    implicit object String extends Type[SString] {
      private[desktop] def toString(value: SString) = value
      private[desktop] def valueOf(string: SString): Option[SString] = Some(string)
    }

    implicit object File extends Type[SFile] {
      private[desktop] def toString(value: SFile) = value.getPath
      private[desktop] def valueOf(string: SString): Option[SFile] = Some(new SFile(string))
    }

    implicit object Int extends Type[SInt] {
      private[desktop] def toString(value: SInt) = value.toString
      private[desktop] def valueOf(string: SString): Option[SInt] = try {
        Some(string.toInt)
      } catch {
        case _: NumberFormatException => None
      }
    }
  }
  sealed trait Type[A] {
    private[desktop] def toString(value: A): String
    private[desktop] def valueOf(string: String): Option[A]

//    private[desktop] def put(prefs: j.Preferences, key: String, value: A): Unit
//    private[desktop] def get(prefs: j.Preferences, key: String): Option[A]
//    private[desktop] def getOrElse(prefs: j.Preferences, key: String, default: A): A
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