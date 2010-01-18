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

import de.sciss.kontur.session.{ BasicDiffusion, Diffusion, Session, Timeline,
                                Transport }
import de.sciss.util.{ Disposable }
import de.sciss.tint.sc.{ AllocatorExhaustedException, Bus, GE, Group, SC, Server,
                         Synth, SynthDef }
import SC._
import de.sciss.tint.sc.ugen._

class SuperColliderPlayer( client: SuperColliderClient, val doc: Session )
extends Disposable {

//    private var server: Option[ Server ] = client.server
    private var online: Option[ Online ] = None

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

    private def clientListener( msg: AnyRef ) : Unit = msg match {
       case Server.Running => serverRunning
       case Server.Offline => serverOffline
//       case SuperColliderClient.ServerChanged( s ) => serverChanged( s )
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

       // ---- constructor ----
       {
         doc.timelines.foreach( tl => addTimeline( tl ))
         doc.diffusions.foreach( diff => addDiffusion( diff ))
       }

       def dispose {
//          doc.timelines.foreach( tl => removeTimeline( tl ))
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
               synth = Some( synDef.play( panGroup ))
          }

          private def diffusionListener( msg: AnyRef ) : Unit = msg match {
              case Diffusion.NumInputChannelsChanged( _, _ )  => diffusionChanged( diff )
              case Diffusion.NumOutputChannelsChanged( _, _ ) => diffusionChanged( diff )
              case BasicDiffusion.MatrixChanged( _, _ ) => newSynth
          }
       }

       class OnlineTimeline( val tl: Timeline ) {
          private val tracks = tl.tracks
          
          // ---- constructor ----
          {
            tracks.addListener( tracksListener )
            tl.transport.foreach( _.addListener( transportListener ))
          }

          def dispose {
            tl.transport.foreach( _.removeListener( transportListener ))
            tracks.removeListener( tracksListener )
          }

          private def tracksListener( msg: AnyRef ) : Unit = msg match {
              case tracks.ElementAdded( idx, elem ) =>
              case tracks.ElementRemoved( idx, elem ) =>
           }

          private def transportListener( msg: AnyRef ) : Unit = msg match {
              case Transport.Play( from, rate ) =>
              case Transport.Stop( pos ) =>
           }
       }
    }
}