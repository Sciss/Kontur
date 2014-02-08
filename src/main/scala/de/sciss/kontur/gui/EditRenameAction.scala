/*
 *  EditRenameAction.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur.gui

import java.awt.event.ActionEvent
import javax.swing.JOptionPane
import de.sciss.kontur.edit.Editor
import de.sciss.kontur.session.Renamable
import swing.Action
import de.sciss.desktop.Window

class EditRenameAction(r: Renamable, ed: Editor, name: String = "Rename...")
  extends Action(name) {

  def apply(): Unit = {
    ???
//    val op = new JOptionPane("Enter new name:", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION)
//    op.setWantsInput(true)
//    op.setInitialSelectionValue(r.name)
//    val result = Window.showDialog(op -> name)
//
//    if (result == JOptionPane.OK_OPTION) {
//      val newName = op.getInputValue.toString
//      val ce = ed.editBegin(name)
//      r.editRename(ce, newName)
//      // val edit = new SimpleEdit( name ) {
//      //   lazy val oldName = r.name
//      //   def apply() { oldName; r.name = newName }
//      //   def unapply() { r.name = oldName }
//      // }
//      // ce.addPerform( edit )
//      ed.editEnd(ce)
//    }
  }
}