/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ DynamicListening }
import de.sciss.gui.{ MenuItem }
import de.sciss.kontur.session.{ BasicTimeline, Session, SessionElement, SessionElementSeq, Timeline }
import java.awt.{ Component }
import java.awt.event.{ ActionEvent }
import javax.swing.{ AbstractAction }
import javax.swing.tree.{ DefaultMutableTreeNode, DefaultTreeModel, MutableTreeNode, TreeModel }

class SessionTreeModel( val doc: Session )
extends DefaultTreeModel( null )
with DynamicListening {

   private val docRoot = new SessionTreeRoot( this )

  setRoot( docRoot )

  def startListening {
    docRoot.startListening
  }

  def stopListening {
    docRoot.stopListening
  }
}

class SessionTreeRoot( model: SessionTreeModel )
extends DefaultMutableTreeNode( model.doc, true )
with DynamicListening {
  private val timelines = new TimelinesTreeNode( model, model.doc.timelines )

  // ---- constructor ----
  add( timelines )

  def startListening {
    timelines.startListening
  }

  def stopListening {
    timelines.stopListening
  }

  override def toString() = model.doc.displayName
}

class SessionElementSeqTreeNode( seq: SessionElementSeq[ _ ])
extends DefaultMutableTreeNode( seq, true )
// with HasContextMenu
{
   override def toString() = seq.name
}

class TimelinesTreeNode( model: SessionTreeModel, timelines: SessionElementSeq[ Timeline ])
extends SessionElementSeqTreeNode( timelines )
with DynamicListening
with HasContextMenu {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "New Timeline" ) {
        def actionPerformed( a: ActionEvent ) {
           val tl = new BasicTimeline( model.doc )
           timelines += tl
//           new TimelineFrame( doc, tl )
        }
     })
     root.add( miAddNew )
     Some( root )
   }

  private def insertElem( idx: Int, elem: Timeline ) {
    val node = new TimelineTreeNode( model, elem )
    model.insertNodeInto( node, this, idx )
  }

  private def removeElem( idx: Int ) {
    model.removeNodeFromParent( getChildAt( idx ).asInstanceOf[ MutableTreeNode ])
  }

  private val listener: (AnyRef) => Unit = _ match {
    case timelines.ElementAdded( idx, elem ) => insertElem( idx, elem )
    case timelines.ElementRemoved( idx, elem ) => removeElem( idx )
    case _ =>
  }
  
    def startListening {
      timelines.addListener( listener )
      // XXX populate timelines node
    }

  def stopListening {
    timelines.removeListener( listener )
    // XXX clear
  }
}

class SessionElementTreeNode( elem: SessionElement )
extends DefaultMutableTreeNode( elem, false ) {
  override def toString() = elem.name
}

class TimelineTreeNode( model: SessionTreeModel, tl: Timeline )
extends SessionElementTreeNode( tl )
with HasDoubleClickAction {

    def doubleClickAction {
      new TimelineFrame( model.doc, tl )
   }
}