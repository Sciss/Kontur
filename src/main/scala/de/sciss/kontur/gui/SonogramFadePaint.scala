/*
 *  SonogramFadePaint.scala
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

package de.sciss.kontur.gui

import de.sciss.kontur.io.SonagramPaintController
import java.awt.image.ImageObserver
import de.sciss.kontur.session.{AudioRegion, FadeSpec}

object SonogramFadePaint {
   def apply( imageObserver: ImageObserver, ar: AudioRegion, visualBoost: Float = 1f ) =
      new SonogramFadePaint( imageObserver, ar.gain * visualBoost, ar.offset, ar.span.getLength,
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
extends SonagramPaintController {
   private val doFadeIn    = fadeIn  != null && fadeIn.numFrames > 0
   private val doFadeOut   = fadeOut != null && fadeOut.numFrames > 0

   def sonogramGain( pos: Double ) = {
      var gain = boost
      if( doFadeIn ) {
         val f = ((pos - offset) / fadeIn.numFrames).toFloat
         if( f < 1f ) gain *= fadeIn.shape.levelAt( math.max( 0f, f ), 0f, 1f )
      }
      if( doFadeOut ) {
         val f = ((pos - offset - (numFrames - fadeOut.numFrames)) / fadeOut.numFrames).toFloat
         if( f > 0f ) gain *= fadeOut.shape.levelAt( math.min( 1f, f ), 1f, 0f )
      }
      gain
   }
}
