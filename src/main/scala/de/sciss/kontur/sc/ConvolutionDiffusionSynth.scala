/*
 *  ConvolutionDiffusionSynth.scala
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

package de.sciss.kontur.sc

import de.sciss.dsp.MathUtil
import de.sciss.synth._
import de.sciss.synth.ugen._
import de.sciss.kontur.session.{ Diffusion, ConvolutionDiffusion }

/**
 *    @version 0.11, 20-Apr-10
 */
class ConvolutionDiffusionSynthFactory( diff: ConvolutionDiffusion )
extends DiffusionSynthFactory {
   def create: DiffusionSynth = new ConvolutionDiffusionSynth( diff )
}

class ConvolutionDiffusionSynth( val diffusion: ConvolutionDiffusion )
extends DiffusionSynth {
   diffSynth =>

   import SynthContext._

   val inBus   = audioBus( diffusion.numInputChannels )
   val outBus  = audioBus( diffusion.numOutputChannels )

   private var synth: Option[ RichSynth ] = None

   private val diffusionListener = (msg: AnyRef) => msg match {
      case Diffusion.NumInputChannelsChanged( _, _ )        => invalidate( this )
      case Diffusion.NumOutputChannelsChanged( _, _ )       => invalidate( this )
      case ConvolutionDiffusion.PathChanged( _, _ )         => invalidate( this )
      case ConvolutionDiffusion.GainChanged( _, newGain )   => invalidate( this ) 
//      synth.foreach( syn => perform {
//         syn.set( "amp" -> newGain )})
      case ConvolutionDiffusion.DelayChanged( _, newDelay)  => invalidate( this )
   }

   // ---- constructor ----
   {
       diffusion.addListener( diffusionListener )
   }

   def play {
      val d       = diffusion
//      val bufSize = MathUtil.nextPowerOfTwo( min( 32768, d.numFrames ).toInt )
      val bufSize = MathUtil.nextPowerOfTwo( d.numFrames.toInt )  // u should know what u are doing
      val buf     = emptyMultiBuffer( bufSize, d.numOutputChannels )

      d.path.foreach( p => buf.read( p ))

      val syn     = graph( "diff_conv", d.numInputChannels, d.numOutputChannels, d.path ) {
         val in         = "in".ir
         val out        = "out".ir
         val amp        = "amp".kr( 1 )
         val dly        = "dly".ir
//         val dlyFrames  = dlySec * SampleRate.ir
         val inSig      = In.ar( in, d.numInputChannels )
         val delayed    = DelayN.ar( inSig, dly, dly )
         var outSig: Array[ GE ] = Array.fill( d.numOutputChannels )( 0 )
         val trig       = Impulse.kr( 0 )
         val bufNum     = "buf".ir
         val bufFrames  = BufFrames.ir( bufNum ) 
         for( inCh <- (0 until d.numInputChannels) ) {
            for( outCh <- (0 until d.numOutputChannels) ) {
               // XXX could try to optimize with StereoConvolutionL ?
               // last time examined it was pretty much broken though...
               val conv = Convolution2.ar( delayed \ inCh, bufNum + outCh, trig, bufFrames )
               outSig( outCh ) += conv * amp
            }
         }
         Out.ar( out, outSig.toList )
      } play( "in" -> inBus.index, "amp" -> d.gain, "dly" -> d.delay, "buf" -> buf.id ) // XXX out bus

      syn.whenOffline { buf.free }

      synth = Some( syn )
   }

   def stop {
      synth.foreach( _.free )
      synth = None
   }

   def dispose {
      diffusion.removeListener( diffusionListener )
      stop
      inBus.free
      outBus.free
   }
}