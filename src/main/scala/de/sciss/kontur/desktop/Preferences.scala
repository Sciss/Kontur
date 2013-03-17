package de.sciss.kontur
package desktop

import java.util.{prefs => j}
import impl.{PreferencesImpl => Impl}
import scala.util.control.NonFatal

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
  trait Type[A] {
    private[desktop] def toString(value: A): String
    private[desktop] def valueOf(string: String): Option[A]

//    private[desktop] def put(prefs: j.Preferences, key: String, value: A): Unit
//    private[desktop] def get(prefs: j.Preferences, key: String): Option[A]
//    private[desktop] def getOrElse(prefs: j.Preferences, key: String, default: A): A
  }

  def user  (clazz: Class[_]): Preferences = Impl.user  (clazz)
  def system(clazz: Class[_]): Preferences = Impl.system(clazz)

  type Listener[A] = Option[A] => Unit

  final case class Entry[A](prefs: Preferences, key: String)(implicit tpe: Type[A]) {
    private var listeners = Vector.empty[Listener[A]]

    private object prefsListener extends j.PreferenceChangeListener {
      def preferenceChange(e: j.PreferenceChangeEvent) {
        if (e.getKey == key) {
          val newValue = Option(e.getNewValue).flatMap(tpe.valueOf _)
          this.synchronized(listeners.foreach { l => try {
            l(newValue)
          } catch {
            case NonFatal(e1) => e1.printStackTrace()
          }})
        }
      }
    }

    def addListener(listener: Listener[A]) {
      prefsListener.synchronized {
        val add = listeners.isEmpty
        listeners :+= listener
        if (add) prefs.peer.addPreferenceChangeListener(prefsListener)
      }
    }

    def removeListener(listener: Listener[A]) {
      prefsListener.synchronized {
        val i = listeners.indexOf(listener)
        if (i >= 0) {
          listeners = listeners.patch(0, Vector.empty, 1)
          val remove = listeners.isEmpty
          if (remove) prefs.peer.removePreferenceChangeListener(prefsListener)
        }
      }
    }

    def get: Option[A] = prefs.get(key)
    def getOrElse(default: => A): A = prefs.getOrElse(key, default)
    def put(value: A) { prefs.put(key, value) }
  }
}
trait Preferences {
  import Preferences.Type

  def peer: j.Preferences

  def get[A: Type](key: String): Option[A]
  def getOrElse[A: Type](key: String, default: => A): A
  def put[A: Type](key: String, value: A): Unit
  def node(key: String): Preferences

  def apply[A: Type](key: String): Preferences.Entry[A]
}