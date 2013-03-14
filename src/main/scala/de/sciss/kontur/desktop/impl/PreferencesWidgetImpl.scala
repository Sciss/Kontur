package de.sciss.kontur
package desktop
package impl

import java.util.prefs.{PreferenceChangeEvent, PreferenceChangeListener}
import java.awt.EventQueue

trait PreferencesWidgetImpl[A] extends DynamicComponentImpl {
  protected def prefs: Preferences
  protected def prefsKey: String
  implicit protected def prefsType: Preferences.Type[A]

  protected def componentShown () { registerPrefs  () }
	protected def componentHidden() { unregisterPrefs() }

  protected var value: A

  private object listener extends PreferenceChangeListener {
    def preferenceChange(e: PreferenceChangeEvent) {
      val s = e.getNewValue
      if (s != null) prefsType.valueOf(s).foreach(updateValue)
    }
  }

  final protected def updatePrefs() {
    val v = value
    prefs.get(prefsKey) match {
      case Some(vStored) if v != vStored  => prefs.put(prefsKey, v)
      case None                           => prefs.put(prefsKey, v)
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
    prefs.addPreferenceChangeListener(listener)
    prefs.get(prefsKey).foreach(updateValue)
  }

  private def unregisterPrefs() {
    prefs.removePreferenceChangeListener(listener)
	}
}