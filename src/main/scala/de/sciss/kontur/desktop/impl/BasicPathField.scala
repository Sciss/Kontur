package de.sciss.kontur
package desktop
package impl

import swing.{Dialog, Component}
import java.io.File
import de.sciss.desktop.{Window, Preferences}

object BasicPathField {
  private final class Button(mode: PathField.Mode) extends PathButton(mode) {
    protected def showDialog(dialog: Dialog): Unit =
      Window.showDialog(Component.wrap(this), dialog)
  }
}
class BasicPathField(prefs: Preferences.Entry[File], default: File)(mode: PathField.Mode = PathField.Input)
  extends PrefPathField(prefs, default: File)(mode) {

	override protected def createPathButton(): PathButton  = new BasicPathField.Button(mode)
}
