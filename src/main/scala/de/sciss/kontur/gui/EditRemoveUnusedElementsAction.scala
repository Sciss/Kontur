/*
 *  EditRemoveUnusedElementsAction.scala
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

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import de.sciss.kontur.session.SessionElementSeqEditor
import javax.swing.{JPanel, JLabel, JList, DefaultListSelectionModel, JScrollPane, JOptionPane}
import legacy.MenuAction

class EditRemoveUnusedElementsAction[ T ]( elemName: String, ed: SessionElementSeqEditor[ T ],
                                           collect: => Seq[ T ], display: T => String = (e: T) => e.toString,
                                           nameInAction: Boolean = false )
extends MenuAction( "Remove Unused " + (if( nameInAction ) elemName else "") + "..." ) {
   private def fullName    = "Remove Unused " + elemName
//   private def actionName  = getValue( Action.NAME ).toString

   def actionPerformed( e: ActionEvent ) {
      val unused = collect //
      if( unused.isEmpty ) {
         val op = new JOptionPane( "There are currently no unused " + elemName + ".", JOptionPane.INFORMATION_MESSAGE )
         BasicWindowHandler.showDialog( op, null, fullName )
      } else {
         val pane = new JPanel( new BorderLayout( 4, 4 ))
         pane.add( new JLabel( "The following " + elemName + " will be removed:" ), BorderLayout.NORTH )
         val list = new JList( unused.map( display( _ )).toArray[ AnyRef ])
         list.setSelectionModel( new DefaultListSelectionModel {
            override def addSelectionInterval( index0: Int, index1: Int ) {}
            override def setSelectionInterval( index0: Int, index1: Int ) {}
         })
         pane.add( new JScrollPane( list ), BorderLayout.CENTER )
         val op = new JOptionPane( pane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
         val result = BasicWindowHandler.showDialog( op, null, fullName )
         if( result == JOptionPane.OK_OPTION ) {
            val ce = ed.editBegin( fullName )
            unused.foreach( ed.editRemove( ce, _ ))
            ed.editEnd( ce )
         }
      }
   }
}