/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ DynamicListening }
import de.sciss.gui.{ MenuItem }
import de.sciss.kontur.session.{ AudioTrack, BasicTimeline, Session, SessionElement,
                                SessionElementSeq, Timeline, Track }
import java.awt.{ Component }
import java.awt.event.{ ActionEvent }
import javax.swing.{ AbstractAction }
import javax.swing.tree.{ DefaultMutableTreeNode, DefaultTreeModel, MutableTreeNode,
                         TreeModel, TreeNode }
import scala.collection.JavaConversions.{ JEnumerationWrapper }

abstract class DynamicTreeNode( obj: AnyRef, canExpand: Boolean )
extends DefaultMutableTreeNode( obj, canExpand )
with DynamicListening {
  private var isListening = false

    def startListening {
      isListening = true
      new JEnumerationWrapper( children() ).foreach( _ match {
          case d: DynamicTreeNode => d.startListening
          case _ =>
      })
    }

  def stopListening {
      isListening = false
      new JEnumerationWrapper( children() ).foreach( _ match {
        case d: DynamicTreeNode => d.stopListening
        case _ =>
      })
    }

  protected def addDyn( elem: DynamicTreeNode ) {
    add( elem )
    if( isListening ) elem.startListening
  }

  protected def insertDyn( idx: Int, elem: DynamicTreeNode ) {
    insert( elem, idx )
    if( isListening ) elem.startListening
  }
  
  protected def removeDyn( idx: Int ) {
    getChildAt( idx ) match {
      case d: DynamicTreeNode => d.stopListening
      case _ =>
    }
    remove( idx )
  }
}

class SessionTreeModel( val doc: Session )
extends DefaultTreeModel( null )
with DynamicListening {

   private val docRoot = new SessionTreeRoot( doc )

  setRoot( docRoot )

  def startListening {
    docRoot.startListening
  }

  def stopListening {
    docRoot.stopListening
  }
}

class SessionTreeRoot( doc: Session )
extends DynamicTreeNode( doc, true ) {
  private val timelines = new TimelinesTreeIndex( doc, doc.timelines )

  // ---- constructor ----
  addDyn( timelines )

  override def toString() = doc.displayName
}

abstract class SessionElementSeqTreeNode[ T <: SessionElement ]( seq: SessionElementSeq[ T ])
extends DynamicTreeNode( seq, true )
// with HasContextMenu
{
    protected def wrap( elem: T ) : DynamicTreeNode

    private def seqListener( msg: AnyRef ) : Unit = msg match {
      case seq.ElementAdded( idx, elem ) => insertDyn( idx, wrap( elem ))
      case seq.ElementRemoved( idx, elem ) => removeDyn( idx )
    }
    
    override def startListening {
      // cheesy shit ...
      // that is why eventually we should use
      // our own tree model instead of the defaulttreemodel...
//      removeAllChildren()
      seq.foreach( elem => add( wrap( elem ))) // not addDyn, because super does that
      seq.addListener( seqListener )
      super.startListening
    }
    
    override def stopListening {
      seq.removeListener( seqListener )
      super.stopListening
      removeAllChildren()
    }

   override def toString() = seq.name
}

class TimelinesTreeIndex( doc: Session, timelines: SessionElementSeq[ Timeline ])
extends SessionElementSeqTreeNode( timelines )
with HasContextMenu {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "New Timeline" ) {
        def actionPerformed( a: ActionEvent ) {
           val tl = new BasicTimeline( doc )
           timelines += tl
        }
     })
     root.add( miAddNew )
     Some( root )
   }

  protected def wrap( elem: Timeline ): DynamicTreeNode =
    new TimelineTreeIndex( doc, elem )
}

class SessionElementTreeNode( elem: SessionElement, canExpand: Boolean )
extends DynamicTreeNode( elem, canExpand ) {
  override def toString() = elem.name
}

class TimelineTreeIndex( doc: Session, tl: Timeline )
extends SessionElementTreeNode( tl, true ) {

  private val tracks = new TracksTreeIndex( doc, tl )

  // ---- constructor ----
  addDyn( new TimelineTreeLeaf( doc, tl ))
  addDyn( tracks )

    def doubleClickAction {
      new TimelineFrame( doc, tl )
   }

  override def toString() = "Timeline"
}

class TimelineTreeLeaf( doc: Session, tl: Timeline )
extends SessionElementTreeNode( tl, false )
with HasDoubleClickAction {
    def doubleClickAction {
      new TimelineFrame( doc, tl )
   }
}

class TracksTreeIndex( doc: Session, tl: Timeline )
extends SessionElementSeqTreeNode( tl.tracks )
with HasContextMenu {
    def createContextMenu() : Option[ PopupRoot ] = tl match {
        case btl: BasicTimeline => {
            val root = new PopupRoot()
            val miAddNewAudio = new MenuItem( "new", new AbstractAction( "New Audio Track" ) {
                def actionPerformed( a: ActionEvent ) {
                    val t = new AudioTrack( doc, btl )
                    tl.tracks += t
                }
            })
            root.add( miAddNewAudio )
            Some( root )
        }
        case _ => None
    }

  protected def wrap( elem: Track[ _ ]): DynamicTreeNode =
    new TrackTreeLeaf( doc, elem )
}

class TrackTreeLeaf( doc: Session, t: Track[ _ ])
extends SessionElementTreeNode( t, false ) {
}
