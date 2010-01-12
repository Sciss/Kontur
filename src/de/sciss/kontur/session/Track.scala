/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

object Track {
  case class SelectionChanged( oldState: Boolean, newState: Boolean )
}

trait Track[ T <: Stake ] extends SessionElement {
  import Track._
  
  private var selectedVar = false
  def trail: Trail[ T ]
  def selected : Boolean
  def selected_=( newState: Boolean ) {
    if( selectedVar != newState ) {
      val change = SelectionChanged( selectedVar, newState )
      selectedVar = newState
      dispatch( change )
    }
  }
}
