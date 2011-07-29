/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 03.02.2010
 * Time: 00:13:19
 */
package de.sciss.kontur.gui

import java.awt.event.ActionEvent
import javax.swing.JOptionPane
import de.sciss.common.BasicWindowHandler
import de.sciss.gui.MenuAction
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.session.Renameable

class EditRenameAction( r: Renameable, ed: Editor, name: String = "Rename..." )
extends MenuAction( name ) {
   def actionPerformed( a: ActionEvent ) {
      val op = new JOptionPane( "Enter new name:", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
      op.setWantsInput( true )
      op.setInitialSelectionValue( r.name )
      val result = BasicWindowHandler.showDialog( op, null, name )

      if( result == JOptionPane.OK_OPTION ) {
         val newName = op.getInputValue.toString
         val ce = ed.editBegin( name )
         val edit = new SimpleEdit( name ) {
            lazy val oldName = r.name
            def apply() { oldName; r.name = newName }
            def unapply() { r.name = oldName }
         }
         ce.addPerform( edit )
         ed.editEnd( ce )
      }
   }
}