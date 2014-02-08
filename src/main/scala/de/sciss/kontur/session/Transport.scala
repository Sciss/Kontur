/*
 *  Transport.scala
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
package session

import util.Model

object Transport {
   case class Play( pos: Long, rate: Double )
   case class Stop( pos: Long )
}

trait Transport extends Model {
   def timeline: Timeline
   def play( from: Long, rate: Double = 1.0 )
   def stop() : Unit
   def isPlaying: Boolean
   def currentPos: Long
}

class BasicTransport( val timeline: Timeline )
extends Transport {
   import Transport._

   private var playing = false
   private var startPos = 0L
   private var startTime = 0L
   private val stopPos = 0L

   def play( from: Long, rate: Double = 1.0 ): Unit = {
      stop()
      playing = true
      startPos = from
      // XXX should use logical time eventually
      startTime = System.currentTimeMillis()
      dispatch( Play( from, rate ))
   }

   def stop(): Unit =
      if( isPlaying ) {
        playing = false
        dispatch( Stop( currentPos ))
      }

   def isPlaying: Boolean = playing

   def currentPos: Long = if( playing ) {
      // XXX should use logical time eventually
      startPos + ((System.currentTimeMillis() - startTime) * timeline.rate / 1000).toLong
   } else {
      stopPos
   }
}
