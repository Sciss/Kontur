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
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, BasicDiffusion,
                                Diffusion, Session, Timeline, Track, Transport }
import de.sciss.io.{ Span }
import de.sciss.util.{ Disposable }
import de.sciss.tint.sc._
import SC._
import de.sciss.tint.sc.ugen._
import scala.math._

class SuperColliderPlayer( client: SuperColliderClient, val doc: Session )
extends Disposable {

//    private var server: Option[ Server ] = client.server
    private var online: Option[ Online ] = None

    private val clientListener = (msg: AnyRef) => msg match {
       case Server.Running => serverRunning
       case Server.Offline => serverOffline
//       case SuperColliderClient.ServerChanged( s ) => serverChanged( s )
    }

    // ---- constructor ----
    {
       clientListener( client.serverCondition )
       client.addListener( clientListener )
    }

    private def serverRunning {
      client.server.foreach( s => {
         online = Some( new Online( s ))
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

    private class Online( val server: Server ) {
       val group      = Group.head( server.defaultGroup )
       val panGroup   = Group.after( group )
       var timelines  = Map[ Timeline, OnlineTimeline ]()
       var diffusions = Map[ Diffusion, OnlineDiffusion ]()

       private val diffListener = (msg: AnyRef) => msg match {
          case doc.diffusions.ElementAdded( idx, diff ) => addDiffusion( diff )
          case doc.diffusions.ElementRemoved( idx, diff ) => removeDiffusion( diff )
       }

       private val timeListener = (msg: AnyRef) => msg match {
          case doc.timelines.ElementAdded( idx, diff ) => addTimeline( diff )
          case doc.timelines.ElementRemoved( idx, diff ) => removeTimeline( diff )
       }

       // ---- constructor ----
       {
         doc.timelines.foreach( tl => addTimeline( tl ))
         doc.timelines.addListener( timeListener )
         doc.diffusions.foreach( diff => addDiffusion( diff ))
         doc.diffusions.addListener( diffListener )

         // XXX dirty dirty testin
         for( numChannels <- 1 to 2 ) {
            val synDef = SynthDef( "disk_" + numChannels ) {
                 val out        = "out".kr
                 val i_bufNum   = "i_bufNum".ir
                 val i_dur      = "i_dur".ir
//                 val i_fadeIn   = "i_fadeIn".ir
//                 val i_fadeOut  = "i_fadeOut".ir
//                 val amp        = "amp".kr( 1 )
val amp = 1
//                 val i_finTyp   = "i_finTyp".ir( 1 )
//                 val i_foutTyp  = "i_foutTyp".ir( 1 )

//                 val env = new Env( List( 0, 1, 1, 0 ),
//				          List( i_fadeIn, i_dur - (i_fadeIn + i_fadeOut), i_fadeOut ),
//				          List( i_finTyp, 1, i_foutTyp ))
//                 val envGen = EnvGen.kr( env, doneAction = freeSelf ) * amp
val envGen = Line.kr( amp, amp, i_dur, doneAction = freeSelf )
				Out.ar( out, DiskIn.ar( numChannels, i_bufNum ) /* * envGen */)
			 }
//             synDef.writeDefFile( "/Users/rutz/Desktop" )
             synDef.send( server )
         }
       }

       def dispose {
          doc.timelines.removeListener( timeListener )
          doc.diffusions.removeListener( diffListener )
          timelines.keysIterator.foreach( tl => removeTimeline( tl ))
          diffusions.keysIterator.foreach( diff => removeDiffusion( diff ))
          group.free; panGroup.free
       }

       private def addDiffusion( diff: Diffusion ) {
          try {
             diffusions += diff -> new OnlineDiffusion( diff )
          } catch { case e1: AllocatorExhaustedException => {
             e1.printStackTrace()
          }}
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

       private def diffusionChanged( diff: Diffusion ) {
          removeDiffusion( diff )
          addDiffusion( diff )
       }

       class OnlineDiffusion( val diff: Diffusion ) {
          val inBus   = Bus.audio( server, diff.numInputChannels )
          val outBus  = try {
            Bus.audio( server, diff.numOutputChannels )
          } catch { case e1: AllocatorExhaustedException => {
            inBus.free
            throw e1
          }}
          private var synth: Option[ Synth ] = None
          private var synDefCount = 1

          private val diffusionListener = (msg: AnyRef) => msg match {
              case Diffusion.NumInputChannelsChanged( _, _ )  => diffusionChanged( diff )
              case Diffusion.NumOutputChannelsChanged( _, _ ) => diffusionChanged( diff )
              case BasicDiffusion.MatrixChanged( _, _ ) => newSynth
          }

          // ---- constructor ----
          {
              diff.addListener( diffusionListener )
              newSynth
          }

          def dispose {
             // XXX async, might not yet have been created
             synth.foreach( _.free ); synth = None
             inBus.free; outBus.free
             diff.removeListener( diffusionListener )
          }

          private def newSynth {
              synth.foreach( _.free ); synth = None
              diff match {
                case bdiff: BasicDiffusion => newSynth( bdiff )
                case _=> {
                    println( "ERROR: Unknown diffusion type (" + diff + "). Muted" )
                }
              }
          }

          private def newSynth( bdiff: BasicDiffusion ) {
               val defName = "diff_" + diff.numInputChannels + "x" +
                 diff.numOutputChannels + "_id" + diff.id + "_" + synDefCount
               synDefCount += 1
               val matrix = bdiff.matrix
               val synDef = SynthDef( defName ) {
                   val in         = "in".ir
                   val out        = "out".ir
                   val inSig      = In.ar( in, diff.numInputChannels )
                   var outSig: Array[ GE ] = Array.fill( diff.numOutputChannels )( 0 )
                   for( inCh <- (0 until diff.numInputChannels) ) {
                      for( outCh <- (0 until diff.numOutputChannels) ) {
                          val w = matrix( inCh, outCh )
                          outSig( outCh ) += inSig( inCh ) * w
                      }
                   }
                   Out.ar( out, outSig.toList )
               }
               synth = Some( synDef.play( panGroup, List( "in" -> inBus.index ))) // XXX out bus
          }
       }

       class OnlineTimeline( val tl: Timeline ) extends ActionListener {
          val verbose = false
          
          private val tracks = tl.tracks
//          private val player = new OnlinePlayer( tl )

          // transport
          private val bufferLatency 	= 0.2
          private val transportDelta 	= 0.1
          private val sampleRate        = server.counts.sampleRate
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
             val player = t match {
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
              timer.stop()
          }

           trait TrackPlayer extends Disposable {
              def track: Track
              def step( span: Span )
           }

           class DummyPlayer( val track: Track )
           extends TrackPlayer {
              def dispose {}
              def step( span: Span ) {}
           }

           class AudioTrackPlayer( val track: AudioTrack )
           extends TrackPlayer {
              private var synths = Set[ Synth ]()
              private var stakesMap = Map[ AudioRegion, Synth ]()

              def step( span: Span ) {
//println( "step : " + span )
                track.diffusion.foreach( diff => {
//println( "---1" )
                  track.trail.visitRange( span )( stake => {
//println( "---2 " + stake )
                    if( !stakesMap.contains( stake )) {
//println( "---3 " + stake.audioFile.descr )
                      stake.audioFile.descr.foreach( descr => {
//println( "---4 " + descr.channels )
                        if( descr.channels == diff.numInputChannels ) {
//println( "---5" )
                          val bndl        = new MixedBundle
                          val frameOffset = Math.max( 0L, start - stake.span.start )
                          val buffer      = new Buffer( server, 32768, descr.channels )
                          bndl.addPrepare( buffer.allocMsg )
                          bndl.addPrepare( buffer.cueSoundFileMsg( stake.audioFile.path.getAbsolutePath,
                                                                  (stake.offset + frameOffset).toInt )) // XXX toInt
                          val defName     = "disk_" + descr.channels
                          val synth       = new Synth( defName, server )
                          val durFrames   = Math.max( 0, stake.span.getLength - frameOffset )
                          val durSecs     = durFrames / sampleRate

                          val fadeFrames  = 0f // if( frameOffset == 0 ) min( durFrames, fadeIn.numFrames ) else 0  // XXX a little bit cheesy!
                          val fadeInSecs  = 0f // fadeFrames / s.sampleRate
                          val fadeOutSecs = 0f // min( durFrames - fadeFrames, fadeOut.numFrames ) / s.sampleRate
                          bndl.add( synth.newMsg( group, List( "i_bufNum" -> buffer.bufNum,
                              "i_dur" -> durSecs.toFloat, "i_fadeIn" -> fadeInSecs,
                              /* "i_finTyp" -> fadeIn.mode,*/ "i_fadeOut" -> fadeOutSecs,
                              /* "i_foutMode" -> fadeOut.mode, "amp" -> gain,*/
                              "out" -> diffusions( diff ).inBus.index )))
      //                    player.nw.register( synth )
                          synths += synth
                          stakesMap += (stake -> synth)
                          synth.onEnd {
                                buffer.close
                                buffer.free
                                stakesMap -= stake
                                synths -= synth
                          }
                          val bndlTime = Math.max( 0.0, (stake.span.start - start) / sampleRate ) + bufferLatency
                          bndl.send( server, bndlTime )
                        }
                      })
                    }
                  })
                })
              }

              def dispose {
                synths.foreach( _.free )
                synths = Set[ Synth ]()
              }
           }
        }
    }
}