package de.sciss.kontur
package desktop
package impl

import java.util.prefs.{PreferenceChangeEvent, PreferenceChangeListener}
import java.awt.EventQueue

trait PreferencesWidgetImpl[A] extends DynamicComponentImpl {
  protected def prefs: Preferences.Entry[A]

  protected def componentShown () { registerPrefs  () }
	protected def componentHidden() { unregisterPrefs() }

  protected var value: A

  private val listener = { newValue: Option[A] => newValue.foreach(updateValue) }

  final protected def updatePrefs() {
    val v = value
    prefs.get match {
      case Some(vStored) if v != vStored  => prefs.put(v)
      case None                           => prefs.put(v)
      case _ =>
    }
  }

  private def updateValue(v: A) {
    if (EventQueue.isDispatchThread) {
      if (value != v) value = v
    } else EventQueue.invokeLater(new Runnable {
      def run() { updateValue(v) }
    })
  }

  private def registerPrefs() {
    prefs.addListener(listener)
    prefs.get.foreach(updateValue)
  }

  private def unregisterPrefs() {
    prefs.removeListener(listener)
	}
}