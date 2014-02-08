/*
 *  MatrixOut.scala
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

package de.sciss.synth
package ugen

import de.sciss.kontur.util.Matrix2D

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
      val outs = Seq.tabulate[ GE ]( m.numColumns ) { outCh =>
         val exp = Mix.mono( (ins: GE) * Seq.tabulate( numIns )( inCh => m( inCh, outCh ))).expand
         replaceZeroesWithSilence( exp.flatOutputs ) : GE   // !!!
      }
      outs.expand
   }
}
