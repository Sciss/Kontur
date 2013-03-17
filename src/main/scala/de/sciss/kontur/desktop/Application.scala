package de.sciss.kontur.desktop

import reflect.ClassTag

object Application {
}
trait Application {
  app =>
  type Document
  def quit(): Unit
  def name: String

  def addComponent(key: String, component: Any): Unit
  def removeComponent(key: String): Unit
  def getComponent[A: ClassTag](key: String): Option[A]
  def documentHandler: DocumentHandler {
    type Document = app.Document
  }

  def userPrefs: Preferences
  def systemPrefs: Preferences
}

trait SwingApplication extends Application {
  def windowHandler: WindowHandler
}