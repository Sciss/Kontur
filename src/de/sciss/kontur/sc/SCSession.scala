/*
 *  SCSession.scala
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

class SCSession( val doc: Session, val context: SynthContext ) {
   online =>

   var timelines  = Map[ Timeline, SCTimeline ]()
   var diffusions = Map[ Diffusion, DiffusionSynth ]()

   val diskGroup  = group()
   val panGroup   = groupAfter( diskGroup )

   private val diffListener = (msg: AnyRef) => msg match {
      case doc.diffusions.ElementAdded( idx, diff ) => {
         context.perform { inGroup( panGroup ) { addDiffusion( diff )}}
      }
      case doc.diffusions.ElementRemoved( idx, diff ) => context.perform { removeDiffusion( diff )}
   }

   private val timeListener: AnyRef => Unit = (msg: AnyRef) => msg match {
      case doc.timelines.ElementAdded( idx, diff ) => addTimeline( diff )
      case doc.timelines.ElementRemoved( idx, diff ) => removeTimeline( diff )
   }

  // ---- constructor2 ----
  {
     inGroup( panGroup ) {
        doc.diffusions.foreach( diff => addDiffusion( diff ))
     }
     if( realtime ) {
        doc.timelines.foreach( tl => addTimeline( tl ))
        doc.timelines.addListener( timeListener )
        doc.diffusions.addListener( diffListener )
     }
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
     if( realtime ) {
        doc.timelines.removeListener( timeListener )
        doc.diffusions.removeListener( diffListener )
     }
     timelines.keysIterator.foreach( tl => removeTimeline( tl ))
     diffusions.keysIterator.foreach( diff => removeDiffusion( diff ))
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

   private def addTimeline( tl: Timeline ) : SCTimeline = {
      val sctl = new SCTimeline( this, tl, context )
      timelines += tl -> sctl
      sctl
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
}