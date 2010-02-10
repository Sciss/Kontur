/*
 *  Diffusions.scala
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

package de.sciss.kontur.session

import java.awt.datatransfer.{ DataFlavor }
import javax.swing.undo.{ UndoManager }
import scala.xml.{ Node }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.util.{ SerializerContext }

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

  def fromXML( c: SerializerContext, parent: Node ) {
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
}

abstract class BasicDiffusion( doc: Session )
extends Diffusion with DiffusionEditor with Renameable {
    import Diffusion._

    protected var nameVar = "Diffusion"

    def undoManager: UndoManager = doc.getUndoManager

    def editor: Option[ DiffusionEditor ] = Some( this )
    // ---- DiffusionEditor ----

    def editRename( ce: AbstractCompoundEdit, newName: String ) {
        val edit = new SimpleEdit( "editRenameDiffusion" ) {
           lazy val oldName = name
           def apply { oldName; name = newName }
           def unapply { name = oldName }
        }
        ce.addPerform( edit )
    }
}
