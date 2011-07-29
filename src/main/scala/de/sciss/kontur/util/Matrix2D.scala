/*
 *  Matrix2D.scala
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

package de.sciss.kontur.util

import scala.collection.immutable.IntMap

object Matrix2D {
   def fill[ T ]( numRows: Int, numColumns: Int, fill: T ) : Matrix2D[ T ] = {
      val row  = IntMap( (0 until numColumns).map( i => Tuple2( i, fill )): _* )
      val data = IntMap( (0 until numRows).map( i => Tuple2( i, row )): _* )
      new Matrix2D( numRows, numColumns, data )
   }

   def fromSeq[ T ]( arr: Seq[ Seq[ T ]]) : Matrix2D[ T ] = {
      val numRows = arr.size
      if( numRows == 0 ) return new Matrix2D( 0, 0, IntMap[ IntMap[ T ]]() )
      val numColumns = arr.head.size

      val data = IntMap( (0 until numRows).map( i => Tuple2( i,
          IntMap( (0 until numColumns).map( j => Tuple2( j, arr( i )( j ))): _* ))): _* )
      new Matrix2D( numRows, numColumns, data )
   }
}

class Matrix2D[ T ] private ( val numRows: Int, val numColumns: Int,
                             data: IntMap[ IntMap[ T ]]) {
//   private val data = new Array[ Array[ T ]]( numRows )

   def apply( row: Int, col: Int ) : T = data( row )( col )
   def update[ S >: T ]( row: Int, col: Int, value: S ): Matrix2D[ S ] = {
        if( row < 0 || row >= numRows || col < 0 || col >= numColumns ) {
           throw new IllegalArgumentException( "" + row + ", " + col )
        }
        val oldRow  = data( row )
        val newRow  = oldRow + Tuple2(col, value)
        val newData = data + Tuple2(row, newRow)
        new Matrix2D( numRows, numColumns, newData )
   }

   def toSeq : Seq[ Seq[ T ]] =
      List.tabulate[ T ]( numRows, numColumns )( (row, col) => data( row )( col ))

   def resize[ S >: T ]( newRows: Int, newColumns: Int, fill: S ) : Matrix2D[ S ] = {
      var newData: IntMap[ IntMap[ S ]] = data
      if( newColumns < numColumns ) {
         for( row <- 0 until numRows ) {
            var newRow = data( row )
            for( col <- newColumns until numColumns ) {
              newRow = newRow - col
            }
            newData = newData.updated( row, newRow )
         }
      } else {
         for( row <- 0 until numRows ) {
            var newRow: IntMap[ S ] = data( row )
            for( col <- numColumns until newColumns ) {
              newRow = newRow + Tuple2( col, fill )
            }
            newData = newData.updated( row, newRow )
         }
      }
      if( newRows < numRows ) {
         for( row <- newRows until numRows ) {
            newData = newData - row
         }
      } else {
        val fillRow = IntMap( (0 until newColumns).map( i => Tuple2( i, fill )): _* )
         for( row <- numRows until newRows ) {
            newData = newData + Tuple2( row, fillRow )
         }
      }
      new Matrix2D( newRows, newColumns, newData )
   }
}