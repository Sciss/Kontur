/*
 *  SCTimeline.scala
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

import scala.collection.mutable.{ ArrayBuffer }
import java.awt.event.{ ActionEvent, ActionListener }
import javax.swing.{ Timer => SwingTimer }
import de.sciss.util.{ Disposable }
import de.sciss.tint.sc.{ EnvSeg => S, _ }
import SC._
import de.sciss.tint.sc.ugen._
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, Diffusion, MatrixDiffusion,
                                 Session, Stake, Timeline, Track, Transport }
import de.sciss.io.{ Span }
import SC._
import scala.math._
import SynthContext._

class SCTimeline( val scDoc: SCSession, val tl: Timeline, val context: SynthContext )
extends ActionListener {
   val verbose = false

   private val tracks = tl.tracks
//          private val player = new OnlinePlayer( tl )

   // transport
   private val bufferLatency 	   = 0.2
   private val transportDelta 	= 0.1
   private val latencyFrames     = (bufferLatency * sampleRate).toInt
   private val deltaFrames       = (transportDelta * sampleRate).toInt
   private var start             = 0L
   private val timer             = new SwingTimer( (transportDelta * 1000).toInt, this )
   private val players           = new ArrayBuffer[ SCTrackPlayer ]()
   private var mapPlayers        = Map[ Track, SCTrackPlayer ]()

   private val tracksListener   = (msg: AnyRef) => msg match {
      case tracks.ElementAdded( idx, t ) => context.perform { addTrack( idx, t )}
      case tracks.ElementRemoved( idx, t ) => context.perform { removeTrack( idx, t )}
   }

   private val transportListener = (msg: AnyRef) => msg match {
      case Transport.Play( from, rate ) => context.perform { play( from, rate )}
      case Transport.Stop( pos ) => context.perform { stop }
   }

   // ---- constructor ----
   {
//             server.dumpOSC( 3 )
      timer.setInitialDelay( 0 )
      timer.setCoalesce( false )

      if( realtime ) {
         { var idx = 0; tracks.foreach( t => { addTrack( idx, t ); idx += 1 })}
         tracks.addListener( tracksListener )
         tl.transport.foreach( _.addListener( transportListener ))
      }
   }

   def dispose {
      stop
      tracks.foreach( t => removeTrack( 0, t ))
      if( realtime ) {
         tl.transport.foreach( _.removeListener( transportListener ))
         tracks.removeListener( tracksListener )
      }
   }

   // triggered by timer
   def actionPerformed( e: ActionEvent ) {
if( verbose ) println( "| | | | | timer " + start )
      val latentStart = start + latencyFrames
      val span = new Span( latentStart, latentStart + deltaFrames )
      context.perform { step( start, span )}
      start += deltaFrames
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

   private def removeTrack( idx: Int, t: Track ) {
      val ptest = mapPlayers( t )
      mapPlayers -= t
      val player = players.remove( idx )
      assert( ptest == player )
      player.dispose
   }

   def play( from: Long, rate: Double ) {
if( verbose ) println( "play ; deltaFrames = " + deltaFrames )
      start = from // + latencyFrames
      players.foreach( _.play )
      timer.start()
   }

   def stop {
if( verbose ) println( "stop" )
players.foreach( _.stop )
      timer.stop()
   }
}
 