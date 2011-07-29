/*
 *  TreeDragSource.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import java.awt.datatransfer.{ DataFlavor, Transferable, UnsupportedFlavorException }
import java.awt.dnd.{ DnDConstants, DragGestureEvent, DragGestureListener, DragSource,
                     DragSourceAdapter, InvalidDnDOperationException }
import javax.swing.JTree
import DnDConstants._

trait CanBeDragSource extends Transferable {
   def transferDataFlavors: List[ DataFlavor ]
   def transferData( flavor: DataFlavor ) : AnyRef

   def isDataFlavorSupported( flavor: DataFlavor ) : Boolean =
      transferDataFlavors.contains( flavor )

   def getTransferDataFlavors : Array[ DataFlavor ] =
     transferDataFlavors.toArray

   def getTransferData( flavor: DataFlavor ) : AnyRef = try {
         transferData( flavor )
      } catch { case e1: MatchError => throw new UnsupportedFlavorException( flavor )}
}

class TreeDragSource( tree: JTree, actions: Int = ACTION_COPY_OR_MOVE | ACTION_LINK )
extends DragSourceAdapter with DragGestureListener {

//    // ---- constructor ----
//    {
       /* val dgr = */ DragSource.getDefaultDragSource.createDefaultDragGestureRecognizer(
          tree, actions, this )
//    }

    // ---- DragGestureListener ----
   def dragGestureRecognized( dge: DragGestureEvent ) {
      val path = tree.getSelectionPath
      if( path == null ) return
      path.getLastPathComponent match {
         case cbds: CanBeDragSource => {
             try {
               DragSource.getDefaultDragSource.startDrag( dge, null, cbds, this )
             }
             catch { case e1: InvalidDnDOperationException => /* ignore */}
         }
         case _ =>
      }
    }
}
