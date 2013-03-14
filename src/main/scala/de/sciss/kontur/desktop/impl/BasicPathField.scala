package de.sciss.kontur
package desktop
package impl

import java.awt.Dialog
import legacy.PathButton

object BasicPathField {
  private final class Button(tpe: Int) extends PathButton(tpe) {
    protected def showDialog(dlg: Dialog) {
      BasicWindowHandler.showDialog(dlg)
    }
  }
}
class BasicPathField(tpe: Int, dialogText: String) extends PrefPathField(tpe, dialogText) {
	protected def createPathButton(tpe: Int): PathButton  = new BasicPathField.Button(tpe)
}
