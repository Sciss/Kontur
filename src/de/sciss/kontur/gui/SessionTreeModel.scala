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

import de.sciss.app.{ AbstractApplication, DynamicListening }
import de.sciss.common.{ BasicWindowHandler }
import de.sciss.gui.{ MenuGroup, MenuItem }
import de.sciss.io.{ AudioFile, AudioFileDescr }
import de.sciss.kontur.{ Main }
import de.sciss.kontur.session.{ AudioFileElement, AudioTrack, BasicTimeline, Diffusion, DiffusionFactory,
   Renameable, Session, SessionElement, SessionElementSeq, Stake, Timeline, Track, TrackEditor }
import java.awt.{ Component, FileDialog, Frame }
import java.awt.datatransfer.{ DataFlavor, Transferable }
import java.awt.dnd.{ DnDConstants }
import java.awt.event.{ ActionEvent }
import java.io.{ File, FilenameFilter, IOException }
import javax.swing.{ AbstractAction, Action, JOptionPane }
import javax.swing.tree.{ DefaultMutableTreeNode, DefaultTreeModel, MutableTreeNode,
                         TreeModel, TreeNode }
import scala.collection.JavaConversions.{ JEnumerationWrapper }

abstract class DynamicTreeNode( model: SessionTreeModel, obj: AnyRef, canExpand: Boolean )
extends DefaultMutableTreeNode( obj, canExpand )
with DynamicListening {

//  type Tr = Track[ _ <: Stake[ _ ]]

  private var isListening = false

    def startListening {
      isListening = true
// some version of scalac cannot deal with Enumeration
// that is not generified ....
//      new JEnumerationWrapper( children() ).foreach( _ match {
//          case d: DynamicTreeNode => d.startListening
//          case _ =>
//      })
//      val enum = children(); while( enum.hasMoreElements ) enum.nextElement match {
//         case d: DynamicTreeNode => d.startListening
//      }
       for( i <- 0 until getChildCount ) getChildAt( i ) match {
          case d: DynamicTreeNode => d.startListening
       }
   }

  def stopListening {
      isListening = false
//      new JEnumerationWrapper( children() ).foreach( _ match {
//        case d: DynamicTreeNode => d.stopListening
//        case _ =>
//      })
//     val enum = children(); while( enum.hasMoreElements ) enum.nextElement match {
//        case d: DynamicTreeNode => d.stopListening
//     }
     for( i <- 0 until getChildCount ) getChildAt( i ) match {
        case d: DynamicTreeNode => d.stopListening
     }
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

abstract class SessionElementSeqTreeNode[ El <: SessionElement ](
  model: SessionTreeModel, seq: SessionElementSeq[ El ])
extends DynamicTreeNode( model, seq, true )
// with HasContextMenu
{
    protected def wrap( elem: El ) : DynamicTreeNode

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
             val tl = BasicTimeline.newEmpty( model.doc )
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
with HasContextMenu with CanBeDropTarget {

    def createContextMenu() : Option[ PopupRoot ] = {
     val root = new PopupRoot()
     val miAddNew = new MenuItem( "new", new AbstractAction( "Add Audio File" )
                                 with FilenameFilter {
        def actionPerformed( a: ActionEvent ) {
           audioFiles.editor.foreach( ed => {
             getPath.foreach( path => {
               val name = getValue( Action.NAME ).toString
               try {
                  val af = AudioFile.openAsRead( path )
                  val afd = af.getDescr()
                  af.close
                  val ce = ed.editBegin( name )
                  val afe = new AudioFileElement( path,
                     afd.length, afd.channels, afd.rate )
                  ed.editInsert( ce, audioFiles.size, afe )
                  ed.editEnd( ce )
               }
               catch { case e1: IOException => BasicWindowHandler.showErrorDialog( null, e1, name )}
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

   // ---- CanBeDropTarget ----
    def pickImport( flavors: List[ DataFlavor ], actions: Int ) : Option[ Tuple2[ DataFlavor, Int ]] = {
       flavors.find( _ == AudioFileElement.flavor ).map( f => {
          val action  = actions & DnDConstants.ACTION_COPY
          (f, action)
       })
    }

    def importData( data: AnyRef, flavor: DataFlavor, action: Int ) : Boolean = data match {
       case afe: AudioFileElement => {
          audioFiles.editor.map( ed => {
             val success = !audioFiles.contains( afe )
             if( success ) {
                val ce = ed.editBegin( "addAudioFile" )
                ed.editInsert( ce, audioFiles.size, afe )
                ed.editEnd( ce )
             }
             success
          }) getOrElse false
       }
       case _ => false
    }
}

class DiffusionsTreeIndex( model: SessionTreeModel, diffusions: SessionElementSeq[ Diffusion ])
extends SessionElementSeqTreeNode( model, diffusions )
with HasContextMenu with CanBeDropTarget {

   def createContextMenu() : Option[ PopupRoot ] = {
      val root = new PopupRoot()
      val strNew = "New" // XXX getResourceString
      val mgAdd = new MenuGroup( "new", strNew )
      diffusions.editor.foreach( ed => {
         DiffusionGUIFactory.registered.foreach( entry => {
            val (name, gf) = entry
            val fullName = strNew + " " + gf.factory.humanReadableName 
            val miAddNew = new MenuItem( name, new AbstractAction( gf.factory.humanReadableName ) {
               def actionPerformed( a: ActionEvent ) {
                  val panel = gf.createPanel( model.doc )
                  val op = new JOptionPane( panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
                  val result = BasicWindowHandler.showDialog( op, null, fullName )
                  if( result == JOptionPane.OK_OPTION ) {
                     val diffO = gf.fromPanel( panel )
                     diffO.foreach( diff => {
                        val ce = ed.editBegin( fullName )
                        ed.editInsert( ce, diffusions.size, diff )
                        ed.editEnd( ce )
                     })
                  }
               }
            })
            mgAdd.add( miAddNew )
         })
      })
      root.add( mgAdd )
      Some( root )
   }
   
   protected def wrap( elem: Diffusion ): DynamicTreeNode =
      new DiffusionTreeLeaf( model, elem ) // with UniqueCount

   // ---- CanBeDropTarget ----
    def pickImport( flavors: List[ DataFlavor ], actions: Int ) : Option[ Tuple2[ DataFlavor, Int ]] = {
       flavors.find( _ == Diffusion.flavor ).map( f => {
          val action  = actions & DnDConstants.ACTION_COPY
          (f, action)
       })
    }

    def importData( data: AnyRef, flavor: DataFlavor, action: Int ) : Boolean = data match {
       case d: Diffusion => {
          diffusions.editor.map( ed => {
             val success = !diffusions.contains( d )
             if( success ) {
                val ce = ed.editBegin( "addDiffusion" )
                ed.editInsert( ce, diffusions.size, d )
                ed.editEnd( ce )
             }
             success
          }) getOrElse false
       }
       case _ => false
    }
}

class SessionElementTreeNode( model: SessionTreeModel, elem: SessionElement, canExpand: Boolean )
extends DynamicTreeNode( model, elem, canExpand ) {
  override def toString() = elem.name
}

class TimelineTreeIndex( model: SessionTreeModel, tl: Timeline )
extends SessionElementTreeNode( model, tl, true )
with HasContextMenu {

   private val tracks = new TracksTreeIndex( model, tl )

   // ---- constructor ----
   addDyn( new TimelineTreeLeaf( model, tl ))
   addDyn( tracks )

   def createContextMenu() : Option[ PopupRoot ] = {
      tl.editor.map( ed => {
         tl match {
            case r: Renameable => {
               val root = new PopupRoot()
               root.add( new MenuItem( "rename", new EditRenameAction( r, ed )))
               Some( root )
            }
            case _ => None
         }
      }) getOrElse None
   }
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
with HasContextMenu with CanBeDropTarget {
   def createContextMenu() : Option[ PopupRoot ] = tl match {
      case btl: BasicTimeline => {
         val root = new PopupRoot()
         val miAddNewAudio = new MenuItem( "new", new AbstractAction( "New Audio Track" ) {
            def actionPerformed( a: ActionEvent ) {
               tl.tracks.editor.foreach( ed => {
                  val ce = ed.editBegin( getValue( Action.NAME ).toString )
                  val t = new AudioTrack( model.doc, btl )
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

   // ---- CanBeDropTarget ----
   def pickImport( flavors: List[ DataFlavor ], actions: Int ) : Option[ Tuple2[ DataFlavor, Int ]] = {
      flavors.find( _ == Track.flavor ).map( f => {
         val action  = actions & DnDConstants.ACTION_COPY_OR_MOVE
         (f, action)
      })
   }

   def importData( data: AnyRef, flavor: DataFlavor, action: Int ) : Boolean = data match {
      case t: Track => {
         tl.tracks.editor.map( ed => {
            val success = !tl.tracks.contains( t )
            if( success ) {
// XXX
//               if( !model.doc.diffusions.contains( t.diffusion )) ...
//               if( !model.doc.audioFiles.contains( t.trail.audiofiles....)) ...
               val ce = ed.editBegin( "addTrack" )
               ed.editInsert( ce, tl.tracks.size, t )
               ed.editEnd( ce )
            }
            success
         }) getOrElse false
      }
      case _ => false
   }
}

class TrackTreeLeaf( model: SessionTreeModel, t: Track )
extends SessionElementTreeNode( model, t, false )
with HasContextMenu with CanBeDragSource {
   def createContextMenu() : Option[ PopupRoot ] = {
      t.editor.map( ed => {
         t match {
            case r: Renameable => {
               val root = new PopupRoot()
               root.add( new MenuItem( "rename", new EditRenameAction( r, ed )))
               Some( root )
            }
            case _ => None
         }
      }) getOrElse None
   }

   // ---- CanBeDragSource ----
   
   def transferDataFlavors = List( Track.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case Track.flavor => t
   }
}

class AudioFileTreeLeaf( model: SessionTreeModel, afe: AudioFileElement )
extends SessionElementTreeNode( model, afe, false )
with HasDoubleClickAction with CanBeDragSource {
    def doubleClickAction {
      println( "DANG" )
   }

   // ---- CanBeDragSource ----

   def transferDataFlavors = List( AudioFileElement.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case AudioFileElement.flavor => afe
   }
}

class DiffusionTreeLeaf( model: SessionTreeModel, diff: Diffusion )
extends SessionElementTreeNode( model, diff, false )
with HasDoubleClickAction with CanBeDragSource {
    def doubleClickAction {
/*
        val page      = DiffusionObserverPage.instance
        val observer  = AbstractApplication.getApplication()
          .getComponent( Main.COMP_OBSERVER ).asInstanceOf[ ObserverFrame ]
        page.setObjects( diff )
        observer.selectPage( page.id )
*/
   }

   // ---- CanBeDragSource ----

   def transferDataFlavors = List( Diffusion.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case Diffusion.flavor => diff
   }
}