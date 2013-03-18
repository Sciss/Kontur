package de.sciss.kontur
package desktop
package impl

import legacy.PathButton
import swing.{Dialog, Component}
import java.io.File
import de.sciss.desktop.{Window, Preferences}

object BasicPathField {
  private final class Button(tpe: Int, title: String) extends PathButton(tpe, title) {
    protected def showDialog(dialog: Dialog) {
      Window.showDialog(Component.wrap(this), dialog)
    }
  }
}
class BasicPathField(prefs: Preferences.Entry[File], default: File)(tpe: Int, dialogText: String)
  extends PrefPathField(prefs, default: File)(tpe, dialogText) {

	protected def createPathButton(tpe: Int): PathButton  = new BasicPathField.Button(tpe, dialogText)
}
