/*
 *  MatrixDiffusion.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

import scala.xml.Node
import de.sciss.app.AbstractCompoundEdit
import de.sciss.kontur.edit.SimpleEdit
import de.sciss.kontur.util.{ Matrix2D, SerializerContext }

object MatrixDiffusion extends DiffusionFactory {
   def fromXML( c: SerializerContext, doc: Session, node: Node ) : MatrixDiffusion = {
      val diff = new MatrixDiffusion( doc )
      c.id( diff, node )
      diff.fromXML( node )
      diff
   }

   def factoryName         = "matrix"
   def humanReadableName   = "Matrix Diffusion"

//   def guiFactory = Some( MatrixDiffusionGUI )

   case class MatrixChanged( oldMatrix: Matrix2D[ Float ], newMatrix: Matrix2D[ Float ])
}

// columns correspond to outputs, rows to inputs
class MatrixDiffusion( doc: Session )
extends BasicDiffusion( doc ) {
    import Diffusion._
    import MatrixDiffusion._

    private var numInputChannelsVar  = 1
    private var numOutputChannelsVar = 1
    private var matrixVar = Matrix2D.fill( 1, 1, 1f )

    def factoryName = MatrixDiffusion.factoryName

    def toXML( c: SerializerContext )  =
       <diffusion id={c.id( this ).toString} class={factoryName}>
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
    def numOutputChannels = numOutputChannelsVar
    def numOutputChannels_=( newNum: Int ) {
      if( newNum != numOutputChannelsVar ) {
         val change = NumOutputChannelsChanged( numOutputChannelsVar, newNum )
         numOutputChannelsVar = newNum
         val oldMatrix = resizeMatrix
         dispatch( change )
         dispatchMatrixChange( oldMatrix )
      }
    }
    def matrix = matrixVar
    def matrix_=( newMatrix: Matrix2D[ Float ]) {
        var changes: List[ AnyRef ] = Nil
        if( newMatrix != matrixVar ) {
           changes ::= MatrixChanged( matrixVar, newMatrix )
           matrixVar = newMatrix
        }
        if( newMatrix.numColumns != numOutputChannelsVar ) {
           changes ::= NumOutputChannelsChanged( numOutputChannelsVar, newMatrix.numColumns )
           numOutputChannelsVar = newMatrix.numColumns
        }
        if( newMatrix.numRows != numInputChannelsVar ) {
           changes ::= NumInputChannelsChanged( numInputChannelsVar, newMatrix.numRows )
           numInputChannelsVar = newMatrix.numRows
        }
        changes.foreach( msg => dispatch( msg ))
    }

    // ---- DiffusionEditor ----
    def editSetNumInputChannels( ce: AbstractCompoundEdit, newNum: Int ) {
        val edit = new SimpleEdit( "editSetNumInputChannels" ) {
           lazy val oldNum = numInputChannels
           def apply() { oldNum; numInputChannels = newNum }
           def unapply() { numInputChannels = oldNum }
        }
        ce.addPerform( edit )
    }

    def editSetNumOutputChannels( ce: AbstractCompoundEdit, newNum: Int ) {
        val edit = new SimpleEdit( "editSetNumOutputChannels" ) {
           lazy val oldNum = numOutputChannels
           def apply() { oldNum; numOutputChannels = newNum }
           def unapply() { numOutputChannels = oldNum }
        }
        ce.addPerform( edit )
    }

    def editSetMatrix( ce: AbstractCompoundEdit, newMatrix: Matrix2D[ Float ]) {
        val edit = new SimpleEdit( "editSetMatrix" ) {
           lazy val oldMatrix = matrix
           def apply() { oldMatrix; matrix = newMatrix }
           def unapply() { matrix = oldMatrix }
        }
        ce.addPerform( edit )
    }
}
