package de.sciss.synth
package ugens

import de.sciss.kontur.util.Matrix2D
import ugen.{Mix, Silent}

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
      val outs0 = Seq.tabulate[ GE ]( m.numRows )( outCh => Mix.mono( ins * Seq.tabulate( numIns )( inCh => m( inCh, outCh ))).expand )
      val numZeroes = outs0.count {
         case Constant( 0 ) => true
         case _ => false
      }
      val outs = if( numZeroes > 0 ) {
         val sil = Silent.ar( numZeroes ).expand.outputs
         var idx = 0
         outs0.map {
            case Constant( 0 ) => val res = sil( idx ); idx += 1; res
            case x => x
         }
      } else outs0

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
