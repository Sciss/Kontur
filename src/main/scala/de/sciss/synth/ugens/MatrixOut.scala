/*
 *  MatrixOut.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2012 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.synth
package ugens

import de.sciss.kontur.util.Matrix2D
import ugen.Mix

/**
 * Temporary work around the fact the Out.ar is not checking against scalar values
 * which this element thus exchanges for Silent.ar elements.
 */
final case class MatrixOut( in: GE, m: Matrix2D[ Float ]) extends GE.Lazy {
   def displayName = "MatrixOut"
   def rate = audio

   def makeUGens : UGenInLike = {
      val _in     = in.expand
      val ins     = _in.outputs
      val numIns  = ins.size
      require( numIns == m.numRows )
      val outs = Seq.tabulate[ GE ]( m.numRows )( outCh => Mix.mono( ins * Seq.tabulate( numIns )( inCh => m( inCh, outCh ))).expand )
//      val numZeroes = outs0.count {
//         case Constant( 0 ) => true
//         case _ => false
//      }
//      val outs = if( numZeroes > 0 ) {
//         val sil = Silent.ar( numZeroes ).expand.outputs
//         var idx = 0
//         outs0.map {
//            case Constant( 0 ) => val res = sil( idx ); idx += 1; res
//            case x => x
//         }
//      } else outs0
//      val outs = UGenHelper.replaceZeroesWithSilence( outs0 )

      (outs: GE).expand
//
//      val outSig  = Array.fill[ GE ]( m.numColumns )( 0: GE )
//      for( inCh <- (0 until m.numRows) ) {
//         for( outCh <- (0 until m.numColumns) ) {
//            val w = m( inCh, outCh )
//            outSig( outCh ) += ins( inCh ) * w
//         }
//      }
//      outSig.toSeq.expand
////      Silent.ar(1).expand
   }
}
