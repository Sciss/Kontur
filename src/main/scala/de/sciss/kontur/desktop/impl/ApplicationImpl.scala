package de.sciss.kontur
package desktop
package impl

import java.util.prefs.Preferences
import reflect.ClassTag

trait ApplicationImpl extends Application {
  private lazy val _systemPrefs = Preferences.systemNodeForPackage(getClass)
  private lazy val _userPrefs   = Preferences.userNodeForPackage  (getClass)

  final def systemPrefs: Preferences = _systemPrefs
  final def userPrefs  : Preferences = _userPrefs

  private val sync          = new AnyRef
  private var componentMap  = Map.empty[String, Any]

  def addComponent(key: String, component: Any) {
    sync.synchronized(componentMap += key -> component)
  }

  def removeComponent(key: String) {
    sync.synchronized(componentMap -= key)
  }

  def getComponent[A: ClassTag](key: String): Option[A] = sync.synchronized(componentMap.get(key) match {
    case Some(c: A) => Some(c)
    case _ => None
  })
}