/*
 *  EditRenameAction.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur.gui

import java.awt.event.ActionEvent
import javax.swing.JOptionPane
import de.sciss.common.BasicWindowHandler
import de.sciss.gui.MenuAction
import de.sciss.kontur.edit.Editor
import de.sciss.kontur.session.Renamable

class EditRenameAction( r: Renamable, ed: Editor, name: String = "Rename..." )
extends MenuAction( name ) {
   def actionPerformed( a: ActionEvent ): Unit = {
      val op = new JOptionPane( "Enter new name:", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
      op.setWantsInput( true )
      op.setInitialSelectionValue( r.name )
      val result = BasicWindowHandler.showDialog( op, null, name )

      if( result == JOptionPane.OK_OPTION ) {
         val newName = op.getInputValue.toString
         val ce = ed.editBegin( name )
         r.editRename( ce, newName )
//         val edit = new SimpleEdit( name ) {
//            lazy val oldName = r.name
//            def apply() { oldName; r.name = newName }
//            def unapply() { r.name = oldName }
//         }
//         ce.addPerform( edit )
         ed.editEnd( ce )
      }
   }
}