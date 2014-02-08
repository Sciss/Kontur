/*
 *  Renameable.scala
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