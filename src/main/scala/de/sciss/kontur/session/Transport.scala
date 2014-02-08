/*
 *  Transport.scala
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
