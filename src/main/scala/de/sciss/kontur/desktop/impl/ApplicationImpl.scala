package de.sciss.kontur
package desktop
package impl

import java.util.prefs.Preferences

trait ApplicationImpl extends Application {
  private lazy val _systemPrefs = Preferences.systemNodeForPackage(getClass)
  private lazy val _userPrefs   = Preferences.userNodeForPackage  (getClass)

  final protected def systemPrefs: Preferences = _systemPrefs
  final protected def userPrefs  : Preferences = _userPrefs
}