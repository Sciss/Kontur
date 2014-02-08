/*
 *  TreeDragSource.scala
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

package de.sciss.kontur.gui

import java.awt.datatransfer.{ DataFlavor, Transferable, UnsupportedFlavorException }
import java.awt.dnd.{ DnDConstants, DragGestureEvent, DragGestureListener, DragSource,
                     DragSourceAdapter, InvalidDnDOperationException }
import javax.swing.JTree
import DnDConstants._

trait CanBeDragSource extends Transferable {
  def transferDataFlavors: List[DataFlavor]

  def transferData(flavor: DataFlavor): AnyRef

  def isDataFlavorSupported(flavor: DataFlavor): Boolean =
    transferDataFlavors.contains(flavor)

  def getTransferDataFlavors: Array[DataFlavor] =
    transferDataFlavors.toArray

  def getTransferData(flavor: DataFlavor): AnyRef = try {
    transferData(flavor)
  } catch {
    case _: MatchError => throw new UnsupportedFlavorException(flavor)
  }
}

class TreeDragSource(tree: JTree, actions: Int = ACTION_COPY_OR_MOVE | ACTION_LINK)
  extends DragSourceAdapter with DragGestureListener {

  //    // ---- constructor ----
  //    {
  /* val dgr = */ DragSource.getDefaultDragSource.createDefaultDragGestureRecognizer(
    tree, actions, this)

  //    }

  // ---- DragGestureListener ----
  def dragGestureRecognized(dge: DragGestureEvent): Unit = {
    val path = tree.getSelectionPath
    if (path == null) return
    path.getLastPathComponent match {
      case cbds: CanBeDragSource =>
        try {
          DragSource.getDefaultDragSource.startDrag(dge, null, cbds, this)
        }
        catch {
          case e1: InvalidDnDOperationException => /* ignore */
        }

      case _ =>
    }
  }
}
