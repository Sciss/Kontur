/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.kontur.util.{ Model }

object Renameable {
  case class NameChanged( oldName: String, newName: String )
}

trait Renameable { self: Model =>
  import Renameable._

  protected var nameVar: String
  def name: String = nameVar
  def name_=( newName: String ) {
//    sync.synchronized {
      if( newName != nameVar ) {
        val change = NameChanged( nameVar, newName )
        nameVar = newName
        dispatch( change )
      }
//    }
  }
}