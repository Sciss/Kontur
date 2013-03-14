package de.sciss.kontur
package desktop
package impl

trait PreferencesWidgetImpl[A] extends DynamicComponentImpl {
  protected def prefs: Preferences
  protected def prefsKey: String
  protected def prefsType: Preferences.Type[A]

}