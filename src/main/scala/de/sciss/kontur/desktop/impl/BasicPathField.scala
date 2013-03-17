package de.sciss.kontur
package desktop
package impl

import legacy.PathButton
import swing.{Dialog, Component}

object BasicPathField {
  private final class Button(tpe: Int, title: String) extends PathButton(tpe, title) {
    protected def showDialog(dialog: Dialog) {
      WindowHandler.showDialog(Component.wrap(this), dialog)
    }
  }
}
class BasicPathField(tpe: Int, dialogText: String) extends PrefPathField(tpe, dialogText) {
	protected def createPathButton(tpe: Int): PathButton  = new BasicPathField.Button(tpe, dialogText)
}
