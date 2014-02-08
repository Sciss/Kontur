/*
 *  SCAudioTrackPlayer.scala
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

package de.sciss.kontur
package sc

import math._
import de.sciss.synth._
import ugen._
import SynthContext._
import session.{Diffusion, AudioRegion, AudioTrack}
import language.reflectiveCalls
import de.sciss.span.Span

class SCAudioTrackPlayer( val scDoc: SCSession, val track: AudioTrack )
extends SCTrackPlayer {
   private var synths   = Set[ RichSynth ]()
   private var stakes   = Set[ AudioRegion ]()
   private var playing  = false

//private var uniqueSCHNUCKI = 0
//private def uniqueSCHNUCK = {
//   val result = uniqueSCHNUCKI
//   uniqueSCHNUCKI += 1
//   result
//}

   def play( ar: AudioRegion, frameOffset: Long = 0L ) : { def stop() : Unit } = {
      val res = new {
         var syn     = Option.empty[ RichSynth ]
         var stopped = false

         def stop(): Unit = {
            stopped = true
            syn.foreach { s => scDoc.context.perform( s.free() )}
         }

         def ready( synO: Option[ RichSynth ]): Unit = {
            syn = synO
            if( stopped ) stop()
         }
      }

      track.diffusion match {
         case Some( d ) => scDoc.context.perform( play( d, ar, frameOffset, -1.0, force = true )( res.ready ))
         case _ =>
      }

      res
   }

   def step( currentPos: Long, span: Span ): Unit = {
      if( !playing ) throw new IllegalStateException( "Was not playing" )
      
      track.diffusion.foreach { diff =>
         track.trail.visitRange( span ) { ar =>
            if( !ar.muted && !stakes.contains( ar )) {
               val frameOffset   = max( 0L, span.start - ar.span.start )
               val delay         = (span.start - currentPos) / sampleRate
               stakes           += ar
               play( diff, ar, frameOffset, delay, force = false ) {
                  case Some( syn ) =>
                     syn.whenOffline { stakes -= ar }
                  case _           =>
                     stakes -= ar
               }
            }
         }
      }
   }
   
   private def L( elems: ControlSetMap* ) : List[ ControlSetMap ] = List( elems: _* )

   private def play( diff: Diffusion, ar: AudioRegion, frameOffset: Long, delay: Double, force: Boolean )
                   ( whenReady: Option[ RichSynth ] => Unit ) {
      val numChannels = ar.audioFile.numChannels
      val equalChans  = numChannels == diff.numInputChannels
      val monoMix     = !equalChans && (diff.numInputChannels == 1)
      if( !(equalChans || monoMix) ) {
         whenReady( None )
         return
      }

//      val L    = List[ ControlSetMap ] _
      val sd   = graph( "disk", numChannels, monoMix ) {
         val out           = "out".kr
         val i_buf         = "i_buf".ir
         val smpDur        = SampleDur.ir
         val i_frames      = "i_frames".ir
         val i_frameOff    = "i_frameOff".ir
         val i_fadeIn      = "i_fadeIn".ir
         val i_fadeOut     = "i_fadeOut".ir
         val amp           = "amp".kr( 1 )
         val i_finShape    = "i_finShape".ir( 1 )
         val i_finCurve    = "i_finCurve".ir( 0 )
         val i_finFloor    = "i_finFloor".ir( 0 )
         val i_foutShape   = "i_foutShape".ir( 1 )
         val i_foutCurve   = "i_foutCurve".ir( 0 )
         val i_foutFloor   = "i_foutFloor".ir( 0 )

         val frameIndex     = Line.ar( i_frameOff, i_frames, (i_frames - i_frameOff) * smpDur, freeSelf )

         import Env.{ Segment => S }

         val env = new IEnv( i_finFloor, List(
            S( i_fadeIn, 1, Env.Curve( i_finShape, i_finCurve )),
            S( i_frames - (i_fadeIn + i_fadeOut), 1 ),
            S( i_fadeOut, i_foutFloor, Env.Curve( i_foutShape, i_foutCurve ))))

         val envGen = IEnvGen.ar( env, frameIndex ) * amp
         val sig = DiskIn.ar( numChannels, i_buf )
         val sig1: GE = if( monoMix ) Mix( sig ) else sig
         Out.ar( out, sig1 * envGen )
      }

      val tb   = timebase
//      val delay   = (span.start - currentPos) / sampleRate
      val buf  = cue( ar.audioFile, ar.offset + frameOffset )
//      stakes += ar

      def playStake(): Unit = {
         val res = if( force || playing ) {
            val syn = sd.play( L("i_buf" -> buf.id,
              "i_frames" -> ar.span.length.toFloat,
              "i_frameOff" -> frameOffset.toFloat,
              "amp" -> ar.gain,
              "out" -> scDoc.diffusion(diff).inBus.index) :::
              ar.fadeIn.map { f =>
                val cv = f.shape match { case Curve.parametric(c) => c; case _ => 0f }
                L(
                  "i_fadeIn" -> f.numFrames.toFloat, // XXX should constrain if necessary
                  "i_finShape" -> f.shape.id, "i_finCurve" -> cv,
                  "i_finFloor" -> f.floor
                )
              } .getOrElse(Nil) :::
              ar.fadeOut.map { f =>
                val cv = f.shape match { case Curve.parametric(c) => c; case _ => 0f }
                L(
                  "i_fadeOut" -> f.numFrames.toFloat, // XXX should constrain if necessary
                  "i_foutShape" -> f.shape.id, "i_foutCurve" -> cv,
                  "i_foutFloor" -> f.floor
                )
              } .getOrElse(Nil): _* )
            synths += syn
            syn.endsAfter( (ar.span.length - frameOffset) / sampleRate ) // nrt hint

            syn.whenOffline {
//               stakes -= ar
               synths -= syn
               buf.free()
            }
            Some( syn )

         } else None

         whenReady( res )
      }

      buf.whenReady {
         if( delay >= 0.0 ) {
            delayed( tb /* timebase - tb + */, delay )( playStake() )
         } else {
            current.perform( playStake() )
         }
      }
   }

   def play(): Unit =
      if( !playing ) {
         playing = true
      } else {
         throw new IllegalStateException( "Was already playing" )
      }

   def stop(): Unit =
      if( playing ) {
         playing = false
         synths.foreach( _.free() )
         synths = Set[ RichSynth ]()
      }

   def dispose(): Unit = stop()
}
