/*
 *  Diffusions.scala
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

package de.sciss.kontur.session

import java.awt.datatransfer.DataFlavor
import scala.xml.Node
import de.sciss.kontur.edit.Editor
import de.sciss.kontur.util.SerializerContext
import legacy.AbstractCompoundEdit
import de.sciss.desktop.UndoManager

object Diffusion {
   case class NumInputChannelsChanged( oldNum: Int, newNum: Int )
   case class NumOutputChannelsChanged( oldNum: Int, newNum: Int )
   val flavor = new DataFlavor( classOf[ Diffusion ], "Diffusion" )
}

trait Diffusion extends SessionElement {
   def numInputChannels: Int
   def numOutputChannels: Int
   def editor: Option[ DiffusionEditor ]
   def factoryName: String

   def factory: Option[ DiffusionFactory ] = DiffusionFactory.registered.get( factoryName )
}

trait DiffusionEditor extends Editor {
//    def editSetNumInputChannels( ce: AbstractCompoundEdit, newNum: Int ) : Unit
//    def editSetNumOutputChannels( ce: AbstractCompoundEdit, newNum: Int ) : Unit
    def editRename( ce: AbstractCompoundEdit, newName: String ) : Unit
}

object DiffusionFactory {
   var registered = Map[ String, DiffusionFactory ]()

   // ---- constructor ----
   // XXX eventually this needs to be decentralized
   registered += MatrixDiffusion.factoryName -> MatrixDiffusion
   registered += ConvolutionDiffusion.factoryName -> ConvolutionDiffusion
}

trait DiffusionFactory {
   def fromXML( c: SerializerContext, doc: Session, node: Node ) : Diffusion
   def factoryName: String
   def humanReadableName: String
//   def guiFactory: Option[ DiffusionGUIFactory ]
}

class Diffusions( doc: Session )
extends BasicSessionElementSeq[ Diffusion ]( doc, "Diffusions" ) {
  def toXML( c: SerializerContext ) = <diffusions>
  {innerToXML( c )}
</diffusions>

  def fromXML( c: SerializerContext, parent: Node ): Unit = {
     val innerXML = SessionElement.getSingleXML( parent, "diffusions" )
     innerFromXML( c, innerXML )
  }

  protected def elementsFromXML( c: SerializerContext, node: Node ) : Seq[ Diffusion ] =
     (node \ "diffusion").map( n => {
        val clazz = (n \ "@class").text 
        DiffusionFactory.registered.get( clazz ).map( _.fromXML( c, doc, n )) getOrElse {
           println( "ERROR: Omitting diffusion '" + (n \ "name").text + "' due to unknown class '" + clazz + "'" )
           null
        }
     }).filter( _ != null )

   /**
    *  Smart detection across tracks
    */
   def unused: Seq[ Diffusion ] = {
      val diffSet = toList.toSet
      if( diffSet.isEmpty ) return Seq.empty

      val used = doc.timelines.toList.flatMap { tl =>
         tl.tracks.toList.collect({
            case at: AudioTrack => at.diffusion
         }).collect({
            case Some( diff ) => diff
         })
      }

      val unused = diffSet.diff( used.toSet )
      unused.toSeq
   }
}

abstract class BasicDiffusion( doc: Session )
extends Diffusion with DiffusionEditor with Renamable {
   protected var nameVar = "Diffusion"

   def undoManager: UndoManager = doc.undoManager

   def editor: Option[ DiffusionEditor ] = Some( this )

   // ---- DiffusionEditor ----

   protected def editRenameName = "editRenameDiffusion"
}
