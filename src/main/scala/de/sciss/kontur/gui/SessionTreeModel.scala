/*
 *  SessionTreeModel.scala
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

package de.sciss.kontur
package gui

import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.io.{File, FilenameFilter, IOException}
import javax.swing.JOptionPane
import javax.swing.tree.{DefaultMutableTreeNode, DefaultTreeModel}
import de.sciss.synth.io.AudioFile
import session.{MatrixDiffusion, Diffusions, AudioFileElement, AudioFileSeq, AudioTrack, BasicTimeline, Diffusion, Renamable, Session, SessionElement, SessionElementSeq, Timeline, Track}
import util.Model
import de.sciss.desktop.{FileDialog, OptionPane, Window, Menu}
import scala.swing.{Component, Action}

abstract class DynamicTreeNode( model: SessionTreeModel, obj: AnyRef, canExpand: Boolean )
extends DefaultMutableTreeNode( obj, canExpand )
/* with DynamicListening */ {

//  type Tr = Track[ _ <: Stake[ _ ]]

  private var isListening = false

  def startListening(): Unit = {
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
    for (i <- 0 until getChildCount) getChildAt(i) match {
      case d: DynamicTreeNode => d.startListening()
    }
  }

  def stopListening(): Unit = {
    isListening = false
    //      new JEnumerationWrapper( children() ).foreach( _ match {
    //        case d: DynamicTreeNode => d.stopListening
    //        case _ =>
    //      })
    //     val enum = children(); while( enum.hasMoreElements ) enum.nextElement match {
    //        case d: DynamicTreeNode => d.stopListening
    //     }
    for (i <- 0 until getChildCount) getChildAt(i) match {
      case d: DynamicTreeNode => d.stopListening()
    }
  }

  protected def addDyn(elem: DynamicTreeNode): Unit = {
    add(elem)
    if (isListening) elem.startListening()
  }

  protected def insertDyn(idx: Int, elem: DynamicTreeNode): Unit = {
    model.insertNodeInto(elem, this, idx)
    if (isListening) elem.startListening()
  }

  protected def removeDyn(idx: Int): Unit = {
    getChildAt(idx) match {
      case d: DynamicTreeNode => {
        d.stopListening()
        model.removeNodeFromParent(d)
      }
      // let it crash
      //      case _ =>
    }
    //    remove( idx )
  }
}

class SessionTreeModel(val doc: Session)
  extends DefaultTreeModel(null)
  /* with DynamicListening */ {

  private val docRoot = new SessionTreeRoot(this)

  setRoot(docRoot)

  def startListening(): Unit = docRoot.startListening()
  def stopListening (): Unit = docRoot.stopListening ()
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

class SessionTreeRoot(val model: SessionTreeModel)
  extends DynamicTreeNode(model, model.doc, true) {

  private val timelines  = new TimelinesTreeIndex (model, model.doc.timelines)
  private val audioFiles = new AudioFilesTreeIndex(model, model.doc.audioFiles)
  private val diffusions = new DiffusionsTreeIndex(model, model.doc.diffusions)

  // ---- constructor ----
  addDyn(timelines)
  addDyn(audioFiles)
  addDyn(diffusions)

  override def toString = model.doc.displayName
}

abstract class SessionElementSeqTreeNode[El <: SessionElement](model: SessionTreeModel, seq: SessionElementSeq[El])
  extends DynamicTreeNode(model, seq, true) {

  protected def wrap(elem: El): DynamicTreeNode

  private val seqListener: Model.Listener = {
    case seq.ElementAdded(idx, elem) => insertDyn(idx, wrap(elem))
    case seq.ElementRemoved(idx, elem) => removeDyn(idx)
  }

  override def startListening(): Unit = {
    // cheesy shit ...
    // that is why eventually we should use
    // our own tree model instead of the defaulttreemodel...
    //      removeAllChildren()
    seq.foreach(elem => add(wrap(elem))) // not addDyn, because super does that
    seq.addListener(seqListener)
    super.startListening()
  }

  override def stopListening(): Unit = {
    seq.removeListener(seqListener)
    super.stopListening()
    removeAllChildren()
  }

  override def toString = seq.name
}

class TimelinesTreeIndex(model: SessionTreeModel, timelines: SessionElementSeq[Timeline])
  extends SessionElementSeqTreeNode(model, timelines)
  with HasContextMenu {

  def createContextMenu(): Option[Menu.Popup] = {
    val root = Menu.Popup()
    val miAddNew = Menu.Item("new", new Action("New Timeline") {
      def apply(): Unit = {
        timelines.editor.foreach { ed =>
          val ce = ed.editBegin(title)
          val tl = BasicTimeline.newEmpty(model.doc)
          //             timelines += tl
          ed.editInsert(ce, timelines.size, tl)
          ed.editEnd(ce)
        }
      }
    })
    root.add(miAddNew)
    Some(root)
  }

  protected def wrap(elem: Timeline): DynamicTreeNode =
    new TimelineTreeIndex(model, timelines, elem) // with UniqueCount
}

class AudioFilesTreeIndex(model: SessionTreeModel, audioFiles: AudioFileSeq)
  extends SessionElementSeqTreeNode(model, audioFiles)
  with HasContextMenu with CanBeDropTarget {

  def createContextMenu(): Option[Menu.Popup] = {
    val root = Menu.Popup()
    val miAddNew = Menu.Item("new", new Action("Add...") with FilenameFilter {
      def apply(): Unit =
        audioFiles.editor.foreach { ed =>
          getPath.foreach { path =>
            val name = "Add Audio File" // getValue( Action.NAME ).toString
            try {
              val spec  = AudioFile.readSpec(path)
              val ce    = ed.editBegin(name)
              val afe   = new AudioFileElement(path, spec.numFrames, spec.numChannels, spec.sampleRate)
              ed.editInsert(ce, audioFiles.size, afe)
              ed.editEnd(ce)
            }
            catch {
              case e1: IOException => Window.showDialog(e1 -> name)
            }
          }
        }

      private def getPath: Option[File] = {
        val dlg = FileDialog.open(title = title)
        dlg.peer.setFilenameFilter(this)
        Window.showDialog(dlg)
      }

      def accept(dir: File, name: String): Boolean = {
        val f = new File(dir, name)
        try {
          AudioFile.identify(f).isDefined
        }
        catch {
          case e1: IOException => false
        }
      }
    })
    root.add(miAddNew)

    val miRemoveUnused = Menu.Item("removeUnused",
      new EditRemoveUnusedElementsAction[AudioFileElement]("Audio Files", audioFiles, audioFiles.unused,
        _.path.getName))
    root.add(miRemoveUnused)

    Some(root)
  }

  protected def wrap(elem: AudioFileElement): DynamicTreeNode =
    new AudioFileTreeLeaf(model, audioFiles, elem) // with UniqueCount

  // ---- CanBeDropTarget ----
  def pickImport(flavors: List[DataFlavor], actions: Int): Option[(DataFlavor, Int)] = {
    flavors.find(_ == AudioFileElement.flavor).map { f =>
      val action = actions & DnDConstants.ACTION_COPY
      (f, action)
    }
  }

  def importData(data: AnyRef, flavor: DataFlavor, action: Int): Boolean = data match {
    case afe: AudioFileElement => {
      audioFiles.editor.map { ed =>
        val success = !audioFiles.contains(afe)
        if (success) {
          val ce = ed.editBegin("addAudioFile")
          ed.editInsert(ce, audioFiles.size, afe)
          ed.editEnd(ce)
        }
        success
      } getOrElse false
    }
    case _ => false
  }
}

class DiffusionsTreeIndex( model: SessionTreeModel, diffusions: Diffusions ) // SessionElementSeq[ Diffusion ]
extends SessionElementSeqTreeNode( model, diffusions )
with HasContextMenu with CanBeDropTarget {

   def createContextMenu() : Option[Menu.Popup] = {
      val root = Menu.Popup()
      val strNew = "New" // XXX getResourceString
      val mgAdd = Menu.Group( "new", strNew )

     DiffusionGUIFactory.registered.foreach { entry =>
       val (name, gf) = entry
       val fullName = strNew + " " + gf.factory.humanReadableName
       val miAddNew = Menu.Item(name, Action(gf.factory.humanReadableName) {
         val panel  = gf.createPanel(model.doc)
         val op = OptionPane.confirmation(message = Component.wrap(panel), messageType = OptionPane.Message.Question,
           optionType = OptionPane.Options.OkCancel)
         op.title = fullName
         val result = Window.showDialog(op)
         if (result == OptionPane.Result.Ok) {
           val diffO = gf.fromPanel(panel)
           diffO.foreach(diff => {
             val ce = diffusions.editBegin(fullName)
             diffusions.editInsert(ce, diffusions.size, diff)
             diffusions.editEnd(ce)
           })
         }
       })
       mgAdd.add(miAddNew)
     }
     root.add(mgAdd)

     val miRemoveUnused = Menu.Item("removeUnused",
       new EditRemoveUnusedElementsAction[Diffusion]("Diffusions", diffusions, diffusions.unused, _.name))
     root.add(miRemoveUnused)

     Some(root)
   }

  protected def wrap( elem: Diffusion ): DynamicTreeNode =
      new DiffusionTreeLeaf( model, elem ) // with UniqueCount

   // ---- CanBeDropTarget ----
    def pickImport( flavors: List[ DataFlavor ], actions: Int ) : Option[ (DataFlavor, Int) ] = {
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
  override def toString = elem.name
}

class TimelineTreeIndex( model: SessionTreeModel, timelines: SessionElementSeq[ Timeline ], tl: Timeline )
extends SessionElementTreeNode( model, tl, true )
with HasContextMenu {

   private val tracks = new TracksTreeIndex( model, tl )

   // ---- constructor ----
   addDyn( new TimelineTreeLeaf( model, tl ))
   addDyn( tracks )

   def createContextMenu() : Option[Menu.Popup] = {
      var items = Vector.empty[ Menu.Item ]
      tl.editor.foreach { ed =>
         tl match {
            case r: Renamable => {
               items :+= Menu.Item( "rename", new EditRenameAction( r, ed ))
            }
            case _ =>
         }
      }
      timelines.editor.foreach { ed =>
         items :+= Menu.Item( "remove", new EditRemoveSessionElementAction( "Timeline", tl, ed ))
      }
      if( items.isEmpty ) None else {
         val root = Menu.Popup()
         items.foreach( root.add( _ ))
         Some( root )
      }
   }
}

class TimelineTreeLeaf(model: SessionTreeModel, tl: Timeline)
  extends SessionElementTreeNode(model, tl, false)
  with HasDoubleClickAction {

  def doubleClickAction(): Unit = new TimelineFrame(model.doc, tl)

  override def toString = "View"
}

final class TracksTreeIndex(model: SessionTreeModel, tl: Timeline)
  extends SessionElementSeqTreeNode(model, tl.tracks)
  with HasContextMenu with CanBeDropTarget {

  def createContextMenu(): Option[Menu.Popup] = {
    val root = Menu.Popup()
    val miAddNewAudio = Menu.Item("new", new Action("New Audio Track") {
      def apply(): Unit =
        tl.tracks.editor.foreach { ed =>
          val ce = ed.editBegin(title)
          val t = new AudioTrack(model.doc)
          ed.editInsert(ce, tl.tracks.size, t)
          ed.editEnd(ce)
        }
    })

    tl.tracks.editor.foreach { ed =>
      root.add(Menu.Item("removeUnused",
        new EditRemoveUnusedElementsAction[Track]("Tracks", ed, tl.tracks.filter(_.trail.isEmpty), _.name)))
    }

    root.add(miAddNewAudio)
    Some(root)
  }

  protected def wrap(elem: Track): DynamicTreeNode =
    new TrackTreeLeaf(model, tl.tracks, elem)

  // ---- CanBeDropTarget ----
  def pickImport(flavors: List[DataFlavor], actions: Int): Option[(DataFlavor, Int)] = {
    flavors.find(_ == Track.flavor).map(f => {
      val action = actions & DnDConstants.ACTION_COPY_OR_MOVE
      (f, action)
    })
  }

  def importData(data: AnyRef, flavor: DataFlavor, action: Int): Boolean = data match {
    case t: Track =>
      tl.tracks.editor.map(ed => {
        val success = !tl.tracks.contains(t)
        if (success) {
          // XXX
          //               if( !model.doc.diffusions.contains( t.diffusion )) ...
          //               if( !model.doc.audioFiles.contains( t.trail.audiofiles....)) ...
          val ce = ed.editBegin("addTrack")
          ed.editInsert(ce, tl.tracks.size, t)
          ed.editEnd(ce)
        }
        success
      }) getOrElse false

    case _ => false
  }
}

class TrackTreeLeaf(model: SessionTreeModel, trs: SessionElementSeq[Track], t: Track)
  extends SessionElementTreeNode(model, t, false)
  with HasContextMenu with CanBeDragSource {

  def createContextMenu() : Option[Menu.Popup] = {
      var items = Vector.empty[ Menu.Item ]
      t.editor.foreach { ed =>
         t match {
            case r: Renamable => items :+= Menu.Item( "rename", new EditRenameAction( r, ed ))
            case _ =>
         }
      }

      trs.editor.foreach { ed =>
         items :+= Menu.Item( "remove", new EditRemoveSessionElementAction[ Track ]( "Track", t, ed ))
      }

      if( items.isEmpty ) None else {
         val root = Menu.Popup()
         items.foreach( root.add( _ ))
         Some( root )
      }
   }

   // ---- CanBeDragSource ----
   
   def transferDataFlavors = List( Track.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case Track.flavor => t
   }
}

class AudioFileTreeLeaf(model: SessionTreeModel, coll: SessionElementSeq[AudioFileElement], afe: AudioFileElement)
  extends SessionElementTreeNode(model, afe, false)
  with HasDoubleClickAction with HasContextMenu with CanBeDragSource {

  def doubleClickAction() = ()

  def createContextMenu() : Option[Menu.Popup] = {
      val root = Menu.Popup()
      coll match {
         case afs: AudioFileSeq => {
            val name = "Replace With Other File"
            val miReplace = Menu.Item( "replace", new Action( name + "..." ) with FilenameFilter {
               def apply(): Unit = {
                  getPath.foreach { path =>
                     try {
                        val newFile = AudioFileElement.fromPath( model.doc, path )
                        var warnings: List[ String ] = Nil
                        if( newFile.numChannels != afe.numChannels ) {
                           warnings ::= ("• Channel mismatch: New file has " + newFile.numChannels + " / old file has " + afe.numChannels)
                        }
                        if( newFile.numFrames != afe.numFrames ) {
                           warnings ::= ("• Frames mismatch: New file has " + newFile.numFrames + " / old file has " + afe.numFrames)
                        }
                        val goAhead = if (warnings.nonEmpty) {
                          val op = OptionPane.confirmation(message = "There are discrepancies between\n\"" + afe.path.getPath +
                            "\" (old file) and\n\"" + newFile.path.getPath + "\" (new file):\n\n" +
                            warnings.mkString("\n") +
                            "\n\nDo you still want to replace it?",
                            messageType = OptionPane.Message.Warning, optionType = OptionPane.Options.YesNo)
                          op.title = name
                          val result = Window.showDialog(op)
                          result == OptionPane.Result.Yes
                        } else true

                        if( goAhead ) {
                           val ce = afs.editBegin( name )
                           afs.editReplace( ce, afe, newFile )
                           afs.editEnd( ce )
                        }
                     }
                     catch { case e: IOException => Window.showDialog( e -> name )}
                  }
               }

              private def getPath: Option[File] = {
                val dlg = FileDialog.open(title = title)
                dlg.peer.setFilenameFilter(this)
                Window.showDialog(dlg)
              }

               def accept( dir: File, name: String ) : Boolean = {
                  val f = new File( dir, name )
                  try {
                     AudioFile.identify( f ).isDefined
                  }
                  catch { case e1: IOException => false }
               }
            })
            root.add( miReplace )
         }

         case _ =>
      }
      Some( root )
   }

   // ---- CanBeDragSource ----

   def transferDataFlavors = List( AudioFileElement.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case AudioFileElement.flavor => afe
   }
}

class DiffusionTreeLeaf(model: SessionTreeModel, diff: Diffusion)
  extends SessionElementTreeNode(model, diff, false)
  with HasContextMenu with HasDoubleClickAction with CanBeDragSource {

  def doubleClickAction() = ()

  def createContextMenu() : Option[Menu.Popup] = {
      var items = Vector.empty[ Menu.Item ]
      diff.editor.foreach { ed =>
         diff match {
            case r: Renamable => items :+= Menu.Item( "rename", new EditRenameAction( r, ed ))
            case _ =>
         }

         diff match {
            case md: MatrixDiffusion =>
               val strEdit    = "Edit"
               val fullName   = strEdit + " " + MatrixDiffusion.humanReadableName + " " + diff.name
               val miEdit     = Menu.Item( "edit", Action( strEdit + "..." ) {
                   val panel   = new MatrixDiffusionGUI()
                   panel.setObjects( diff )
                   val op = OptionPane(message = Component.wrap(panel),
                     messageType = OptionPane.Message.Plain, optionType = OptionPane.Options.Default)
                 op.title = fullName
                   /* val result = */ Window.showDialog( op )
               })
               items :+= miEdit
            case _ =>
         }
      }

      if( items.isEmpty ) None else {
         val root = Menu.Popup()
         items.foreach( root.add( _ ))
         Some( root )
      }
   }

   // ---- CanBeDragSource ----

   def transferDataFlavors = List( Diffusion.flavor )
   def transferData( flavor: DataFlavor ) : AnyRef = flavor match {
      case Diffusion.flavor => diff
   }
}