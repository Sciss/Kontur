/*
 *  SonogramFadePaint.scala
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

package de.sciss.kontur.gui

import java.awt.image.ImageObserver
import de.sciss.kontur.session.{AudioRegion, FadeSpec}
import de.sciss.sonogram

object SonogramFadePaint {
   def apply( imageObserver: ImageObserver, ar: AudioRegion, visualBoost: Float = 1f ) =
      new SonogramFadePaint( imageObserver, ar.gain * visualBoost, ar.offset, ar.span.length,
         ar.fadeIn.orNull, ar.fadeOut.orNull )
}

/**
 * @param imageObserver required by AWT (e.g. a `java.awt.Component`)
 * @param boost         gain factor linear
 * @param offset        into the underlying audio file
 * @param numFrames     of the painted span
 * @param fadeIn        or `null` if not used
 * @param fadeOut       or `null` if not used
 */
final class SonogramFadePaint( val imageObserver: ImageObserver, boost: Float, offset: Long, numFrames: Long,
                               fadeIn: FadeSpec, fadeOut: FadeSpec )
extends sonogram.PaintController {
   private val doFadeIn    = fadeIn  != null && fadeIn.numFrames > 0
   private val doFadeOut   = fadeOut != null && fadeOut.numFrames > 0


  def adjustGain(amp: Float, pos: Double): Float = {
      var gain = boost
      if( doFadeIn ) {
         val f = ((pos - offset) / fadeIn.numFrames).toFloat
         if( f < 1f ) gain *= fadeIn.shape.levelAt( math.max( 0f, f ), 0f, 1f )
      }
      if( doFadeOut ) {
         val f = ((pos - offset - (numFrames - fadeOut.numFrames)) / fadeOut.numFrames).toFloat
         if( f > 0f ) gain *= fadeOut.shape.levelAt( math.min( 1f, f ), 1f, 0f )
      }
      amp * gain
   }
}
