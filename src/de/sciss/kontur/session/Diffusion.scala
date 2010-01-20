/*
 *  Diffusion.scala
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
import de.sciss.kontur.util.{ Matrix2D }

object Diffusion {
  case class NumInputChannelsChanged( oldNum: Int, newNum: Int )
  case class NumOutputChannelsChanged( oldNum: Int, newNum: Int )
  val flavor = new DataFlavor( classOf[ Diffusion ], "Diffusion" )
}

trait Diffusion extends SessionElement {
   def numInputChannels: Int
   def numOutputChannels: Int
   def editor: Option[ DiffusionEditor ]
}

trait DiffusionEditor extends Editor {
    def editSetNumInputChannels( ce: AbstractCompoundEdit, newNum: Int )
    def editSetNumOutputChannels( ce: AbstractCompoundEdit, newNum: Int )
}

object BasicDiffusion {
    val XML_NODE = "diffusion"

    def fromXML( doc: Session, node: Node ) : BasicDiffusion = {
       val id       = (node \ "@id").text.toInt
       val diff     = new BasicDiffusion( id, doc )
       diff.fromXML( node )
       diff
    }

    case class MatrixChanged( oldMatrix: Matrix2D[ Float ], newMatrix: Matrix2D[ Float ])
}

// aka matrix.
// columns correspond to outputs, rows to inputs
class BasicDiffusion( val id: Long, doc: Session )
extends Diffusion with DiffusionEditor with Renameable {
    import Diffusion._
    import BasicDiffusion._

    protected var nameVar = "Diffusion"
    private var numInputChannelsVar  = 1
    private var numOutputChannelsVar = 1
    private var matrixVar = Matrix2D.fill( 1, 1, 1f )

    def matrix = matrixVar

    def toXML =
       <diffusion id={id.toString}>
          <name>{name}</name>
          <numInputChannels>{numInputChannels}</numInputChannels>
          <numOutputChannels>{numOutputChannels}</numOutputChannels>
          <matrix>{matrixVar.toSeq.map( row => rowToXML( row ))}</matrix>
       </diffusion>

    private def rowToXML( row: Seq[ Float ]) =
      <row>{row.mkString( " " )}</row>

    def fromXML( node: Node ) {
        nameVar              = (node \ "name").text
        numInputChannelsVar  = (node \ "numInputChannels").text.toInt
        numOutputChannelsVar = (node \ "numOutputChannels").text.toInt
        val rowsN            = (node \ "matrix" \ "row")
        matrixVar = Matrix2D.fromSeq( rowsN.map( _.text.split( ' ' )
          .map[ Float, Seq[ Float ]]( _.toFloat )))
    }

    // does not fire!
    private def resizeMatrix: Matrix2D[ Float ] = {
       val oldMatrix = matrixVar
       matrixVar = matrixVar.resize( numInputChannelsVar, numOutputChannelsVar, 0f )
       oldMatrix
    }

    private def dispatchMatrixChange( oldMatrix: Matrix2D[ Float ]) {
       dispatch( MatrixChanged( oldMatrix, matrixVar ))
    }

    def numInputChannels = numInputChannelsVar
    def numInputChannels_=( newNum: Int ) {
      if( newNum != numInputChannelsVar ) {
         val change = NumInputChannelsChanged( numInputChannelsVar, newNum )
         numInputChannelsVar = newNum
         val oldMatrix = resizeMatrix
         dispatch( change )
         dispatchMatrixChange( oldMatrix )
      }
    }
    def numOutputChannels = numInputChannelsVar
    def numOutputChannels_=( newNum: Int ) {
      if( newNum != numOutputChannelsVar ) {
         val change = NumOutputChannelsChanged( numOutputChannelsVar, newNum )
         numOutputChannelsVar = newNum
         val oldMatrix = resizeMatrix
         dispatch( change )
         dispatchMatrixChange( oldMatrix )
      }
    }

    def undoManager: UndoManager = doc.getUndoManager

    def editor: Option[ DiffusionEditor ] = Some( this )
    // ---- DiffusionEditor ----
    def editSetNumInputChannels( ce: AbstractCompoundEdit, newNum: Int ) {
       throw new RuntimeException( "NOT YET IMPLEMENTED" )
    }

    def editSetNumOutputChannels( ce: AbstractCompoundEdit, newNum: Int ) {
       throw new RuntimeException( "NOT YET IMPLEMENTED" )
    }

    def editSetMatrix( ce: AbstractCompoundEdit, matrix: Array[ Array[ Float ]]) {
       throw new RuntimeException( "NOT YET IMPLEMENTED" )
    }
}

class Diffusions( doc: Session )
extends BasicSessionElementSeq[ Diffusion ]( doc, "Diffusions" ) {
  val id = -1L
  def toXML = <diffusions>
  {innerToXML}
</diffusions>

  def fromXML( parent: Node ) {
     val innerXML = SessionElement.getSingleXML( parent, "diffusions" )
     innerFromXML( innerXML )
  }

  protected def elementsFromXML( node: Node ) : Seq[ Diffusion ] =
     (node \ BasicDiffusion.XML_NODE).map( n => BasicDiffusion.fromXML( doc, n ))
}