/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.edit

import de.sciss.app.{ AbstractApplication, BasicUndoableEdit, PerformableEdit }

abstract class SimpleEdit( name: String, override val isSignificant: Boolean = true )
extends BasicUndoableEdit {
  def perform() : PerformableEdit = {
    apply()
    this
  }

  def apply() : Unit
  def unapply() : Unit

  override def undo() : Unit = {
    super.undo()
    unapply()
  }

  override def redo() : Unit = {
    super.redo()
    apply()
  }

  override def getPresentationName() : String = {
     AbstractApplication.getApplication().getResourceString( name )
  }
}
