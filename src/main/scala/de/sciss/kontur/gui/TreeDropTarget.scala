package de.sciss.kontur.gui

import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.{ DnDConstants, DropTarget, DropTargetDragEvent, DropTargetDropEvent }
import DnDConstants._
import javax.swing.JTree

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

   override def dragEnter( dtde: DropTargetDragEvent ) {
      process( dtde )
   }

   override def dragOver( dtde: DropTargetDragEvent ) {
      process( dtde )
   }

   private def process( dtde: DropTargetDragEvent ) {
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

   override def drop( dtde: DropTargetDropEvent ) {
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