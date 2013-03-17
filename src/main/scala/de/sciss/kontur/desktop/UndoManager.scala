package de.sciss.kontur
package desktop

import javax.swing.{undo => j}
import swing.Action

trait UndoManager {
  def peer: j.UndoManager

  def canUndo: Boolean
  def canRedo: Boolean
  def canUndoOrRedo: Boolean

  def significant: Boolean

  def undo(): Unit
  def redo(): Unit
  def undoOrRedo(): Unit

  def clear(): Unit
  def add(edit: j.UndoableEdit): Boolean

  def undoAction: Action
  def redoAction: Action
}