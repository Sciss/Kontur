package de.sciss.kontur
package desktop
package impl

import java.awt.EventQueue
import de.sciss.desktop.Preferences

trait PreferencesWidgetImpl[A] extends DynamicComponentImpl {
  protected def prefs: Preferences.Entry[A]

  protected def componentShown (): Unit = registerPrefs  ()
	protected def componentHidden(): Unit = unregisterPrefs()

  protected var value: A

  private val listener: Preferences.Listener[A] = {
    case Some(v) => updateValue(v)
  }

  final protected def updatePrefs(): Unit = {
    val v = value
    prefs.get match {
      case Some(vStored) if v != vStored  => prefs.put(v)
      case None                           => prefs.put(v)
      case _ =>
    }
  }

  private def updateValue(v: A): Unit =
    if (EventQueue.isDispatchThread) {
      if (value != v) value = v
    } else EventQueue.invokeLater(new Runnable {
      def run(): Unit = updateValue(v)
    })

  private def registerPrefs(): Unit = {
    prefs.addListener(listener)
    prefs.get.foreach(updateValue)
  }

  private def unregisterPrefs(): Unit = {
    prefs.removeListener(listener)
	}
}