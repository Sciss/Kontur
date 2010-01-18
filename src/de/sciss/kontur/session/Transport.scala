/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.kontur.util.{ Model }

object Transport {
   case class Play( pos: Long, rate: Double )
   case class Stop( pos: Long )
}

trait Transport extends Model {
   def timeline: Timeline
   def play( from: Long, rate: Double = 1.0 )
   def stop
   def isPlaying: Boolean
   def currentPos: Long
}

class BasicTransport( val timeline: Timeline )
extends Transport {
   import Transport._

   private var playing = false
   private var startPos = 0L
   private var startTime = 0L
   private var stopPos = 0L

   def play( from: Long, rate: Double = 1.0 ) {
      stop
      playing = true
      startPos = from
      // XXX should use logical time eventually
      startTime = System.currentTimeMillis()
      dispatch( Play( from, rate ))
   }

   def stop {
      if( isPlaying ) {
        playing = false
        dispatch( Stop( currentPos ))
      }
   }

   def isPlaying: Boolean = playing

   def currentPos: Long = if( playing ) {
      // XXX should use logical time eventually
      startPos + ((System.currentTimeMillis() - startTime) * timeline.rate / 1000).toLong
   } else {
      stopPos
   }
}
