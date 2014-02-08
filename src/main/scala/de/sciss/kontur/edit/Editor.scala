/*
 *  Editor.scala
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

import java.awt.EventQueue
import legacy.{BasicCompoundEdit, AbstractCompoundEdit}
import de.sciss.desktop.UndoManager

trait Editor {
  def undoManager: UndoManager

  def editBegin(name: String): AbstractCompoundEdit = {
    if (!EventQueue.isDispatchThread) throw new IllegalMonitorStateException()
    new BasicCompoundEdit(name)
  }

  def editEnd(ce: AbstractCompoundEdit): Unit = {
    if (!EventQueue.isDispatchThread) throw new IllegalMonitorStateException()
    ce.perform()
    ce.end()
    undoManager.add(ce)
  }

  def editCancel(ce: AbstractCompoundEdit): Unit = {
    if (!EventQueue.isDispatchThread) throw new IllegalMonitorStateException()
    ce.cancel()
  }
}