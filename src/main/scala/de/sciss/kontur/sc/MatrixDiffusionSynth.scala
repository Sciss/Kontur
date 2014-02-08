/*
 *  MatrixDiffusionSynth.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.kontur
package sc

import de.sciss.synth._
import ugen._
import session.{ Diffusion, MatrixDiffusion }
import util.Model

class MatrixDiffusionSynthFactory( diff: MatrixDiffusion )
extends DiffusionSynthFactory {
   def create: DiffusionSynth = new MatrixDiffusionSynth( diff )
}

class MatrixDiffusionSynth( val diffusion: MatrixDiffusion )
extends DiffusionSynth {
   diffSynth =>

   import SynthContext._

   val inBus   = audioBus( diffusion.numInputChannels )
   val outBus  = audioBus( diffusion.numOutputChannels )

   private var synth: Option[ RichSynth ]    = None

   private val diffusionListener: Model.Listener = {
      case Diffusion.NumInputChannelsChanged( _, _ )  => invalidate( diffSynth )
      case Diffusion.NumOutputChannelsChanged( _, _ ) => invalidate( diffSynth )
      case MatrixDiffusion.MatrixChanged( _, _ )      => invalidate( diffSynth )
   }

   // ---- constructor ----
   {
       diffusion.addListener( diffusionListener )
   }

   def play(): Unit = {
      val d    = diffusion
      val df   = graph( "diff_matrix", d.numInputChannels, d.numOutputChannels, d.matrix ) {
         val in      = "in".ir
         val out     = "out".ir
         val inSig   = In.ar( in, d.numInputChannels )
         Out.ar( out, MatrixOut( inSig, d.matrix ))

//         val outSig  = Array.fill[ GE ]( d.numOutputChannels )( 0: GE )
//         for( inCh <- (0 until d.numInputChannels) ) {
//            for( outCh <- (0 until d.numOutputChannels) ) {
//               val w = d.matrix( inCh, outCh )
//               outSig( outCh ) += inSig \ inCh * w
//            }
//         }
//         Out.ar( out, outSig.toList )
      }

//df.synthDef.write( "/Users/hhrutz/Desktop/gaga/" )

      val syn = df.play( "in" -> inBus.index ) // XXX out bus

      synth = Some( syn )
   }

   def stop(): Unit = {
      synth.foreach( _.free() )
      synth = None
   }

   def dispose(): Unit = {
      diffusion.removeListener( diffusionListener )
      stop()
      inBus.free()
      outBus.free()
   }
}