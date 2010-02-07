/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 06.02.2010
 * Time: 17:53:15
 */
package de.sciss.kontur.sc

import de.sciss.tint.sc.{ AllocatorExhaustedException, Bus, GE, SC, Server, Synth, SynthDef }
import SC._
import de.sciss.tint.sc.ugen._
import de.sciss.kontur.session.{ Diffusion, MatrixDiffusion }

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

   private val diffusionListener = (msg: AnyRef) => msg match {
      case Diffusion.NumInputChannelsChanged( _, _ )  => invalidate( this )
      case Diffusion.NumOutputChannelsChanged( _, _ ) => invalidate( this )
      case MatrixDiffusion.MatrixChanged( _, _ )      => invalidate( this )
   }

   // ---- constructor ----
   {
       diffusion.addListener( diffusionListener )
   }

   def play {
      val d    = diffusion
      val syn  = graph( "diff_matrix", d.numInputChannels, d.numOutputChannels, d.matrix ) {
         val in      = "in".ir
         val out     = "out".ir
         val inSig   = In.ar( in, d.numInputChannels )
         var outSig: Array[ GE ] = Array.fill( d.numOutputChannels )( 0 )
         for( inCh <- (0 until d.numInputChannels) ) {
            for( outCh <- (0 until d.numOutputChannels) ) {
               val w = d.matrix( inCh, outCh )
               outSig( outCh ) += inSig \ inCh * w
            }
         }
         Out.ar( out, outSig.toList )
      } play( "in" -> inBus.index ) // XXX out bus

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