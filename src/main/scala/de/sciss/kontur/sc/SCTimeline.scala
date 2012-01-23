/*
 *  SCTimeline.scala
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

package de.sciss.kontur.sc

import scala.collection.mutable.ArrayBuffer
import java.awt.event.{ ActionEvent, ActionListener }
import javax.swing.{ Timer => SwingTimer }
import de.sciss.synth.Model

//import de.sciss.synth._
import de.sciss.kontur.session.{ AudioTrack, Timeline, Track, Transport }
import de.sciss.io.Span
import SynthContext._

class SCTimeline( val scDoc: SCSession, val tl: Timeline )
extends ActionListener {
   val verbose = false

   val context = current 
   private val tracks = tl.tracks
//          private val player = new OnlinePlayer( tl )

   // transport
   private val bufferLatency 	   = 0.2
   private val transportDelta 	= 0.1
   private val latencyFrames     = (bufferLatency * sampleRate).toInt
   private val deltaFrames       = (transportDelta * sampleRate).toInt
   private var start             = 0L
   private var currentPos        = 0L
   private var startTime         = 0L
   private val timer             = new SwingTimer( (transportDelta * 1000).toInt, this )
   private val players           = new ArrayBuffer[ SCTrackPlayer ]()
   private var mapPlayers        = Map[ Track, SCTrackPlayer ]()

   private val tracksListener: Model.Listener = {
      case tracks.ElementAdded( idx, t ) => context.perform { addTrack( idx, t )}
      case tracks.ElementRemoved( idx, t ) => context.perform { removeTrack( idx, t )}
   }

   private val transportListener: Model.Listener = {
      case Transport.Play( from, rate ) => context.perform { play( from, rate )}
      case Transport.Stop( pos ) => context.perform { stop() }
   }

   // ---- constructor ----
   {
//             server.dumpOSC( 3 )
      timer.setInitialDelay( 0 )
//      timer.setCoalesce( false )

      if( realtime ) {
         { var idx = 0; tracks.foreach( t => { addTrack( idx, t ); idx += 1 })}
         tracks.addListener( tracksListener )
         tl.transport.foreach( _.addListener( transportListener ))
      }
   }

   def dispose() {
      stop()
      tracks.foreach( t => removeTrack( 0, t ))
      if( realtime ) {
         tl.transport.foreach( _.removeListener( transportListener ))
         tracks.removeListener( tracksListener )
      }
   }

   // triggered by timer
   def actionPerformed( e: ActionEvent ) {
if( verbose ) println( "| | | | | timer " + currentPos )
      val sr         = context.sampleRate
      // sucyk swing timer exhibits drift... we need to refer to systemtime
      val stopFrame  = start + ((System.currentTimeMillis - startTime) * sr / 1000 + 0.5).toLong
      val latencyStop = stopFrame + latencyFrames
      val latentStart = currentPos + latencyFrames
//      val span = new Span( latentStart, latentStart + deltaFrames )
      val span = new Span( latentStart, latencyStop )
      context.timebase = (currentPos - start) / sr
      context.perform { step( currentPos, span )}
//      currentPos += deltaFrames
      currentPos = stopFrame
   }

   def step( currentPos: Long, span: Span ) {
      inGroup( scDoc.diskGroup ) {
         players.foreach( _.step( currentPos, span ))
      }
   }

   private def addTrack( idx: Int, t: Track ) {
      val player: SCTrackPlayer = t match {
         case at: AudioTrack => new SCAudioTrackPlayer( scDoc, at )
         case _ => new SCDummyPlayer( t )
      }
      mapPlayers += (t -> player)
      players.insert( idx, player )
   }

   def addTrack( t: Track ) {
      addTrack( players.size, t )
   }

   private def removeTrack( idx: Int, t: Track ) {
      mapPlayers.get( t ).foreach( ptest => {
         mapPlayers -= t
         val player = players.remove( idx )
         assert( ptest == player )
         player.dispose()
      })
   }

   def play( from: Long, rate: Double ) {
if( verbose ) println( "play ; deltaFrames = " + deltaFrames )
      start = from // + latencyFrames
      startTime = System.currentTimeMillis // sucky swing timer is imprecise
      currentPos = start
//      if( realtime ) context.timebase = 0.0
      players.foreach( _.play() )
      if( realtime ) timer.start()
   }

   def stop() {
if( verbose ) println( "stop" )
players.foreach( _.stop() )
      if( realtime ) timer.stop()
   }
}
 