package de.sciss.kontur.gui

import de.sciss.gui.MenuAction
import java.awt.event.ActionEvent
import de.sciss.kontur.session.SessionElementSeqEditor

class EditRemoveSessionElementAction[ T ]( elem: T, ed: SessionElementSeqEditor[ T ], name: String = "Remove element" )
extends MenuAction( name ) {
   def actionPerformed( a: ActionEvent ) {
      val ce = ed.editBegin( name )
      ed.editRemove( ce, elem )
      ed.editEnd( ce )
   }
}