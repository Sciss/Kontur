/*
 *  SimpleEdit.scala
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

package de.sciss.kontur.edit

import legacy.{BasicUndoableEdit, PerformableEdit}

abstract class SimpleEdit(name: String, override val isSignificant: Boolean = true)
  extends BasicUndoableEdit {

  def perform(): PerformableEdit = {
    apply()
    this
  }

  def apply() : Unit
  def unapply() : Unit

  override def undo(): Unit = {
    super.undo()
    unapply()
  }

  override def redo(): Unit = {
    super.redo()
    apply()
  }

  override def getPresentationName: String = {
    name // AbstractApplication.getApplication.getResourceString( name )
  }
}
