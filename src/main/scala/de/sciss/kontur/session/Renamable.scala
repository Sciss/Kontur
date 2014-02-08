/*
 *  Renameable.scala
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

package de.sciss.kontur
package session

import edit.SimpleEdit
import util.Model
import legacy.AbstractCompoundEdit

object Renamable {
  case class NameChanged( oldName: String, newName: String )
}

trait Renamable { self: Model =>
  import Renamable._

  protected var nameVar: String
  def name: String = nameVar
  def name_=( newName: String ): Unit = {
//    sync.synchronized {
      if( newName != nameVar ) {
        val change = NameChanged( nameVar, newName )
        nameVar = newName
        dispatch( change )
      }
//    }
  }

   protected def editRenameName: String

   def editRename( ce: AbstractCompoundEdit, newName: String ): Unit = {
      val edit = new SimpleEdit( editRenameName ) {
         lazy val oldName = name
         def apply(): Unit = { oldName; name = newName }
         def unapply(): Unit = name = oldName
      }
      ce.addPerform( edit )
   }
}