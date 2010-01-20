/*
 *  SessionTreeModel.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.app.{ DynamicListening }
import de.sciss.common.{ BasicWindowHandler }
import de.sciss.gui.{ MenuItem }
import de.sciss.io.{ AudioFile, AudioFileDescr }
import de.sciss.kontur.session.{ AudioFileElement, AudioTrack, BasicDiffusion,
                                BasicTimeline, Diffusion, Session, SessionElement,
                                SessionElementSeq, Timeline, Track }
import java.awt.{ Component, FileDialog, Frame }
import java.awt.datatransfer.{ DataFlavor, Transferable }
import java.awt.event.{ ActionEvent }
import java.io.{ File, FilenameFilter, IOException }
import javax.swing.{ AbstractAction, Action }
import javax.swing.tree.{ DefaultMutableTreeNode, DefaultTreeModel, MutableTreeNode,
                         TreeModel, TreeNode }
import scala.collection.JavaConversions.{ JEnumerationWrapper }

abstract class DynamicTreeNode( model: SessionTreeModel, obj: AnyRef, canExpand: Boolean )
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
    model.insertNodeInto( elem, this, idx )
    if( isListening ) elem.startListening
  }
  
  protected def removeDyn( idx: Int ) {
    getChildAt( idx ) match {
      case d: DynamicTreeNode => {
          d.stopListening
          model.removeNodeFromParent( d )
      }
// let it crash
//      case _ =>
    }
//    remove( idx )
  }
}

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

/*
object UniqueCount { var cnt = 0 }
trait UniqueCount {
  val cnt = {
    val res = UniqueCount.cnt
    UniqueCount.cnt += 1
    res
  }
  override def toString() : String = super.toString() + "#" + cnt
}
*/

class SessionTreeRoot( val model: SessionTreeModel )
extends DynamicTreeNode( model, model.doc, true ) {
  private val timelines   = new TimelinesTreeIndex( model, model.doc.timelines )
  private val audioFiles  = new AudioFilesTreeIndex( model, model.doc.audioFiles )
  private val diffusions  = new DiffusionsTreeIndex( model, model.doc.diffusions )

  // ---- constructor ----
  addDyn( timelines )
  addDyn( audioFiles )
  addDyn( diffusions )

  override def toString() = model.doc.displayName
}

abstract class SessionElementSeqTreeNode[ T <: SessionElement ](
  model: SessionTreeModel, seq: SessionElementSeq[ T ])
extends DynamicTreeNode( model, seq, true )
// with HasContextMenu
{
    protected def wrap( elem: T ) : DynamicTreeNode

    private val seqListener = (msg: AnyRef) => msg match {
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

class TimelinesTreeIndex( model: SessionTreeModel, timelines: SessionElementSeq[ Timeline ])
extends SessionElementSeqTreeNode( model, timelines )
with HasContextMenu {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "New Timeline" ) {
        def actionPerformed( a: ActionEvent ) {
           timelines.editor.foreach( ed => {
             val ce = ed.editBegin( getValue( Action.NAME ).toString )
             val tl = new BasicTimeline( model.doc.createID, model.doc )
//             timelines += tl
              ed.editInsert( ce, timelines.size, tl )
              ed.editEnd( ce )
           })
        }
     })
     root.add( miAddNew )
     Some( root )
   }

  protected def wrap( elem: Timeline ): DynamicTreeNode =
    new TimelineTreeIndex( model, elem ) // with UniqueCount
}

class AudioFilesTreeIndex( model: SessionTreeModel, audioFiles: SessionElementSeq[ AudioFileElement ])
extends SessionElementSeqTreeNode( model, audioFiles )
with HasContextMenu {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "Add Audio File" )
                                 with FilenameFilter {
        def actionPerformed( a: ActionEvent ) {
           audioFiles.editor.foreach( ed => {
             getPath.foreach( path => {
               val ce = ed.editBegin( getValue( Action.NAME ).toString )
               val afe = new AudioFileElement( model.doc.createID, path )
               ed.editInsert( ce, audioFiles.size, afe )
               ed.editEnd( ce )
             })
           })
        }

        private def getPath: Option[ File ] = {
          val dlg = new FileDialog( null.asInstanceOf[ Frame ], getValue( Action.NAME ).toString )
          dlg.setFilenameFilter( this )
//          dlg.show
          BasicWindowHandler.showDialog( dlg )
          val dirName   = dlg.getDirectory
          val fileName  = dlg.getFile
          if( dirName != null && fileName != null ) {
            Some( new File( dirName, fileName ))
          } else {
            None
          }
        }

        def accept( dir: File, name: String ) : Boolean = {
          val f = new File( dir, name )
          try {
            AudioFile.retrieveType( f ) != AudioFileDescr.TYPE_UNKNOWN
          }
          catch { case e1: IOException => false }
        }
     })
     root.add( miAddNew )
     Some( root )
   }

  protected def wrap( elem: AudioFileElement ): DynamicTreeNode =
    new AudioFileTreeLeaf( model, elem ) // with UniqueCount
}

class DiffusionsTreeIndex( model: SessionTreeModel, diffusions: SessionElementSeq[ Diffusion ])
extends SessionElementSeqTreeNode( model, diffusions )
with HasContextMenu {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "Add Diffusion" ) {
        def actionPerformed( a: ActionEvent ) {
           diffusions.editor.foreach( ed => {
               val ce = ed.editBegin( getValue( Action.NAME ).toString )
               val diff = new BasicDiffusion( model.doc.createID, model.doc )
               ed.editInsert( ce, diffusions.size, diff )
               ed.editEnd( ce )
           })
        }
     })
     root.add( miAddNew )
     Some( root )
   }

  protected def wrap( elem: Diffusion ): DynamicTreeNode =
    new DiffusionTreeLeaf( model, elem ) // with UniqueCount
}

class SessionElementTreeNode( model: SessionTreeModel, elem: SessionElement, canExpand: Boolean )
extends DynamicTreeNode( model, elem, canExpand ) {
  override def toString() = elem.name
}

class TimelineTreeIndex( model: SessionTreeModel, tl: Timeline )
extends SessionElementTreeNode( model, tl, true ) {

  private val tracks = new TracksTreeIndex( model, tl )

  // ---- constructor ----
  addDyn( new TimelineTreeLeaf( model, tl ))
  addDyn( tracks )
}

class TimelineTreeLeaf( model: SessionTreeModel, tl: Timeline )
extends SessionElementTreeNode( model, tl, false )
with HasDoubleClickAction {
    def doubleClickAction {
      new TimelineFrame( model.doc, tl )
   }
  override def toString() = "View"
}

class TracksTreeIndex( model: SessionTreeModel, tl: Timeline )
extends SessionElementSeqTreeNode( model, tl.tracks )
with HasContextMenu {
    def createContextMenu() : Option[ PopupRoot ] = tl match {
        case btl: BasicTimeline => {
            val root = new PopupRoot()
            val miAddNewAudio = new MenuItem( "new", new AbstractAction( "New Audio Track" ) {
                def actionPerformed( a: ActionEvent ) {
                    tl.tracks.editor.foreach( ed => {
                      val ce = ed.editBegin( getValue( Action.NAME ).toString )
                      val t = new AudioTrack( model.doc.createID, model.doc, btl )
//                      tl.tracks += t
                      ed.editInsert( ce, tl.tracks.size, t )
                      ed.editEnd( ce )
                    })
                }
            })
            root.add( miAddNewAudio )
            Some( root )
        }
        case _ => None
    }

  protected def wrap( elem: Track ): DynamicTreeNode =
    new TrackTreeLeaf( model, elem )
}

class TrackTreeLeaf( model: SessionTreeModel, t: Track )
extends SessionElementTreeNode( model, t, false ) {
}

class AudioFileTreeLeaf( model: SessionTreeModel, afe: AudioFileElement )
extends SessionElementTreeNode( model, afe, false )
with HasDoubleClickAction {
    def doubleClickAction {
      println( "DANG" )
   }
//  override def toString() = "View"
}

class DiffusionTreeLeaf( model: SessionTreeModel, diff: Diffusion )
extends SessionElementTreeNode( model, diff, false )
with HasDoubleClickAction with CanBeDragSource {
    def doubleClickAction {
      println( "DANG" )
   }

   def transferDataFlavors = List( Diffusion.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case Diffusion.flavor => diff
   }
}