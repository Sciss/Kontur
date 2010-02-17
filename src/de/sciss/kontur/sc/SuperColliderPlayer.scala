/*
 *  SuperColliderPlayer.scala
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

import java.awt.event.{ ActionEvent, ActionListener }
import scala.collection.mutable.{ ArrayBuffer }
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, Diffusion, MatrixDiffusion,
                                 Session, Stake, Timeline, Track, Transport }
import de.sciss.io.{ Span }
import de.sciss.util.{ Disposable }
import de.sciss.tint.sc.{ Async, EnvSeg => S, _ }
import SC._
import de.sciss.tint.sc.ugen._
import de.sciss.scalaosc.{ OSCMessage }
import scala.math._
import SynthContext._

//import Track.Tr

class SuperColliderPlayer( client: SuperColliderClient, val doc: Session )
extends Disposable {

//    private var server: Option[ Server ] = client.server
    private var online: Option[ Online ] = None

    private val clientListener = (msg: AnyRef) => msg match {
       case Server.Running => serverRunning
       case Server.Offline => serverOffline
       case _ => // because we call the listener directly!
//       case SuperColliderClient.ServerChanged( s ) => serverChanged( s )
    }

    // ---- constructor ----
    {
       clientListener( client.serverCondition )
       client.addListener( clientListener )
    }

    private def serverRunning {
      client.server.foreach( s => {
         val context = new SynthContext( s )
         online = Some( new Online( context ))
      })
    }

    private def serverOffline {
       // XXX stop all transports
       online = None
    }

    def dispose {
       client.removeListener( clientListener )
       online.foreach( _.dispose )
       online = None
    }

    def context: Option[ SynthContext ] = online.map( _.context )

    class Online( val context: SynthContext ) {
       online =>

       val group      = Group.head( context.server.defaultGroup )
       val panGroup   = Group.after( group )
       var timelines  = Map[ Timeline, OnlineTimeline ]()
       var diffusions = Map[ Diffusion, DiffusionSynth ]()

       private val diffListener = (msg: AnyRef) => msg match {
          case doc.diffusions.ElementAdded( idx, diff ) => {
             context.perform { inGroup( panGroup ) { addDiffusion( diff )}}
          }
          case doc.diffusions.ElementRemoved( idx, diff ) => context.perform { removeDiffusion( diff )}
       }

       private val timeListener = (msg: AnyRef) => msg match {
          case doc.timelines.ElementAdded( idx, diff ) => addTimeline( diff )
          case doc.timelines.ElementRemoved( idx, diff ) => removeTimeline( diff )
       }

      // ---- constructor ----
      {
         context.perform {
            doc.timelines.foreach( tl => addTimeline( tl ))
            doc.timelines.addListener( timeListener )
            inGroup( panGroup ) {
               doc.diffusions.foreach( diff => addDiffusion( diff ))
               doc.diffusions.addListener( diffListener )
            }
         }

         // XXX dirty dirty testin
         for( numChannels <- 1 to 2 ) {
            for( monoMix <- (if( numChannels > 1 ) List( false, true ) else List( false ))) {
            val synDef = SynthDef( "disk_" + numChannels + (if( monoMix ) "M" else "") ) {
               val out           = "out".kr
               val i_bufNum      = "i_bufNum".ir
               val smpDur        = SampleDur.ir
//             val regionDurSecs = "i_frames".ir * smpDur
               val i_frames      = "i_frames".ir
               val i_frameOff    = "i_frameOff".ir
//               val durSecs       = regionDurSecs - regionOffSecs
//               val fadeInSecs    = "i_fadeIn".ir * smpDur
//               val fadeOutSecs   = "i_fadeOut".ir * smpDur
               val i_fadeIn      = "i_fadeIn".ir
               val i_fadeOut     = "i_fadeOut".ir
               val amp           = "amp".kr( 1 )
               val i_finShape    = "i_finShape".ir( 1 )
               val i_finCurve    = "i_finCurve".ir( 0 )
               val i_finFloor    = "i_finFloor".ir( 0 )
               val i_foutShape   = "i_foutShape".ir( 1 )
               val i_foutCurve   = "i_foutCurve".ir( 0 )
               val i_foutFloor   = "i_foutFloor".ir( 0 )

//               val regionTime    = Line.ar( regionOffSecs, regionDurSecs, durSecs, freeSelf )
               val frameIndex     = Line.ar( i_frameOff, i_frames, (i_frames - i_frameOff) * smpDur, freeSelf )

//               val env = new IEnv( i_finFloor, List(
//                  S( fadeInSecs, 1, varShape( i_finShape, i_finCurve )),
//                  S( durSecs - (fadeInSecs + fadeOutSecs), 1 ),
//                  S( fadeOutSecs, i_foutFloor, varShape( i_foutShape, i_foutCurve ))))
               val env = new IEnv( i_finFloor, List(
                  S( i_fadeIn, 1, varShape( i_finShape, i_finCurve )),
                  S( i_frames - (i_fadeIn + i_fadeOut), 1 ),
                  S( i_fadeOut, i_foutFloor, varShape( i_foutShape, i_foutCurve ))))

               val envGen = IEnvGen.ar( env, frameIndex ) * amp
               val sig = DiskIn.ar( numChannels, i_bufNum ) 
	            Out.ar( out, (if( monoMix ) Mix( sig ) else sig) * envGen )
			   }
//            synDef.writeDefFile( "/Users/rutz/Desktop" )
            synDef.send( context.server )
         }}
      }

      // ---- SynthContext ----
      def invalidate( obj: AnyRef ) {}
//      def send( msg: OSCMessage ) {
//         val b = bndl getOrElse {
//            val newBundle = new MixedBundle
//            bndl = Some( newBundle )
//            newBundle
//         }
//         msg match {
//            case async: Async => b.addPrepare( msg )
//            case _ => b.add( msg )
//         }
//      }
//
//      def flush {
//
//      }

      def dispose {
         context.perform {
            doc.timelines.removeListener( timeListener )
            doc.diffusions.removeListener( diffListener )
            timelines.keysIterator.foreach( tl => removeTimeline( tl ))
            diffusions.keysIterator.foreach( diff => removeDiffusion( diff ))
         }
         group.free; panGroup.free
      }

       private def addDiffusion( diff: Diffusion ) {
          DiffusionSynthFactory.get( diff ).foreach( dsf => {
             val ds = dsf.create
             ds.play
             diffusions += diff -> ds
          })
       }

       private def removeDiffusion( diff: Diffusion ) {
          val odiff = diffusions( diff )
          diffusions -= diff
          odiff.dispose
       }

       private def addTimeline( tl: Timeline ) {
          timelines += tl -> new OnlineTimeline( tl )
       }

       private def removeTimeline( tl: Timeline ) {
          val otl = timelines( tl )
          timelines -= tl
          otl.dispose
       }

//       private def diffusionChanged( diff: Diffusion ) {
//          removeDiffusion( diff )
//          addDiffusion( diff )
//       }

       class OnlineTimeline( val tl: Timeline ) extends ActionListener {
          val verbose = false
          
          private val tracks = tl.tracks
//          private val player = new OnlinePlayer( tl )

          // transport
          private val bufferLatency 	= 0.2
          private val transportDelta 	= 0.1
          private val sampleRate        = context.server.counts.sampleRate
          private val latencyFrames     = (bufferLatency * sampleRate).toInt
          private val deltaFrames       = (transportDelta * sampleRate).toInt
          private var start             = 0L
          private val timer             = new javax.swing.Timer( (transportDelta * 1000).toInt, this )
          private val players           = new ArrayBuffer[ TrackPlayer ]()
          private var mapPlayers        = Map[ Track, TrackPlayer ]()

           private val tracksListener = (msg: AnyRef) => msg match {
              case tracks.ElementAdded( idx, t ) => addTrack( idx, t )
              case tracks.ElementRemoved( idx, t ) => removeTrack( idx, t )
           }

          private val transportListener = (msg: AnyRef) => msg match {
              case Transport.Play( from, rate ) => play( from, rate )
              case Transport.Stop( pos ) => stop
           }

          // ---- constructor ----
          {
//             server.dumpOSC( 3 )
             timer.setInitialDelay( 0 )
             timer.setCoalesce( false )

             { var idx = 0; tracks.foreach( t => { addTrack( idx, t ); idx += 1 })}

             tracks.addListener( tracksListener )
             tl.transport.foreach( _.addListener( transportListener ))
          }

          def dispose {
            stop
            tracks.foreach( t => removeTrack( 0, t ))
            tl.transport.foreach( _.removeListener( transportListener ))
            tracks.removeListener( tracksListener )
          }

          // triggered by timer
          def actionPerformed( e: ActionEvent ) {
if( verbose ) println( "| | | | | timer " + start )
              val span		= new Span( start, start + deltaFrames )
              players.foreach( _.step( span ))
              start += deltaFrames
          }

          private def addTrack( idx: Int, t: Track ) {
             val player: TrackPlayer = t match {
             case at: AudioTrack => new AudioTrackPlayer( at )
             case _ => new DummyPlayer( t )
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
              start = from + latencyFrames
              timer.start()
          }

          def stop {
if( verbose ) println( "stop" )
players.foreach( _.stop )
              timer.stop()
          }

           trait TrackPlayer extends Disposable {
              def track: Track
              def step( span: Span )
              def stop
           }

           class DummyPlayer( val track: Track )
           extends TrackPlayer {
              def dispose {}
              def step( span: Span ) {}
              def stop {}
           }

           class AudioTrackPlayer( val track: AudioTrack )
           extends TrackPlayer {
              private var synths = Set[ Synth ]()
              private var stakesMap = Map[ AudioRegion, Synth ]()

              def step( span: Span ) {
                track.diffusion.foreach( diff => {
                  track.trail.visitRange( span )( stake => {
                    if( !stake.muted && !stakesMap.contains( stake )) {
                       val numChannels = stake.audioFile.numChannels
                       val equalChans  = numChannels == diff.numInputChannels
                       val monoMix     = !equalChans && (diff.numInputChannels == 1) 
                        if( equalChans || monoMix ) {
                          val bndl        = new MixedBundle
                          val frameOffset = Math.max( 0L, start - stake.span.start )
                          val buffer      = new Buffer( context.server, 32768, numChannels )
                          bndl.addPrepare( buffer.allocMsg )
                          bndl.addPrepare( buffer.cueSoundFileMsg( stake.audioFile.path.getAbsolutePath,
                                                                  (stake.offset + frameOffset).toInt )) // XXX toInt
                          val defName     = "disk_" + numChannels + (if( monoMix ) "M" else "")
                          val synth       = new Synth( defName, context.server )
//                        val durFrames   = Math.max( 0, stake.span.getLength - frameOffset )
//                        val durSecs     = durFrames / sampleRate

                          val L = List[ Tuple2[ String, Float ]] _

                          bndl.add( synth.newMsg( group, L( "i_bufNum" -> buffer.bufNum,
                             "i_frames" -> stake.span.getLength.toFloat,
                             "i_frameOff" -> frameOffset.toFloat,
                             "amp" -> stake.gain,
                             "out" -> diffusions( diff ).inBus.index ) :::
                             stake.fadeIn.map( f => L(
                                "i_fadeIn" -> f.numFrames.toFloat, // XXX should contrain if necessary
                                "i_finShape" -> f.shape.id, "i_finCurve" -> f.shape.curvature,
                                "i_finFloor" -> f.floor )).getOrElse( Nil ) :::
                             stake.fadeOut.map( f => L(
                                "i_fadeOut" -> f.numFrames.toFloat, // XXX should contrain if necessary
                                "i_foutShape" -> f.shape.id, "i_foutCurve" -> f.shape.curvature,
                                "i_foutFloor" -> f.floor )).getOrElse( Nil )
                          ))
      //                    player.nw.register( synth )
                          synths += synth
                          stakesMap += (stake -> synth)
                          synth.onEnd {
//println( "onEnd : " + synth )
                                buffer.close
                                buffer.free
                                stakesMap -= stake
                                synths -= synth
                          }
                          val bndlTime = Math.max( 0.0, (stake.span.start - start) / sampleRate ) + bufferLatency
                          bndl.send( context.server, bndlTime )
                        }
                    }
                  })
                })
              }

              def stop {
                synths.foreach( _.free )
                synths = Set[ Synth ]()
              }

              def dispose {
                 stop
              }
           }
        }
    }
}