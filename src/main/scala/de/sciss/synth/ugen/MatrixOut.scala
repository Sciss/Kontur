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
