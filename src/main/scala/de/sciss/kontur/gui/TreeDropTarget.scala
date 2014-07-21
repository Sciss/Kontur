/*
 *  TreeDropTarget.scala
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
package gui

import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.{ DnDConstants, DropTarget, DropTargetDragEvent, DropTargetDropEvent }
import DnDConstants._
import javax.swing.JTree
import language.reflectiveCalls

trait CanBeDropTarget {
   def pickImport( flavors: List[ DataFlavor ], actions: Int ) : Option[ (DataFlavor, Int) ]
   def importData( data: AnyRef, flavor: DataFlavor, action: Int ) : Boolean
}

class TreeDropTarget( tree: JTree, actions: Int = ACTION_COPY_OR_MOVE | ACTION_LINK )
extends DropTarget {

   // ---- constructor ----
   {
      setComponent( tree )
      setDefaultActions( actions )
   }

   override def dragEnter( dtde: DropTargetDragEvent ): Unit = {
      process( dtde )
   }

   override def dragOver( dtde: DropTargetDragEvent ): Unit = {
      process( dtde )
   }

   private def process( dtde: DropTargetDragEvent ): Unit = {
      findDropTarget( dtde ).map( tup => {
         dtde.acceptDrag( tup._3 )
      }) getOrElse {
         dtde.rejectDrag()
      }
   }

   private def findDropTarget( dtde: DropTargetDragDropEvent ) : Option[ (CanBeDropTarget, DataFlavor, Int) ] = {
      val loc   = dtde.getLocation()
      val path  = tree.getPathForLocation( loc.x, loc.y )
      if( path == null ) return None
      val child = path.getLastPathComponent

      child match {
         case cbdt: CanBeDropTarget => {
            cbdt.pickImport( dtde.getCurrentDataFlavors().toList, dtde.getSourceActions() ).map( tup => {
               (cbdt, tup._1, tup._2)
            })
         }
         case _ => None
      }
   }

   override def drop( dtde: DropTargetDropEvent ): Unit = {
      findDropTarget( dtde ).map( tup => {
         val (cbdt, flavor, action) = tup
         dtde.acceptDrop( action )
         val data = dtde.getTransferable.getTransferData( flavor )
         val success = cbdt.importData( data, flavor, dtde.getDropAction )
         dtde.dropComplete( success )
      }) getOrElse {
         dtde.rejectDrop()
      }
   }

   // some idiot designed the events...
   // join them with structural typing. thank you scala...
   private type DropTargetDragDropEvent = {
      def getLocation() : Point
      def getCurrentDataFlavors() : Array[ DataFlavor ]
      def getSourceActions() : Int
   }
}