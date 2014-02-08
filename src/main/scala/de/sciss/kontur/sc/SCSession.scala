/*
 *  SCSession.scala
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
package sc

import session.{ Diffusion, Session, Timeline }
import SynthContext._
import util.Model

class SCSession( val doc: Session ) {
   online =>

   val context    = current
//println( "SESSION >>>>> " + context )
   private var timelines  = Map[ Timeline, SCTimeline ]()
   private var diffusions = Map[ Diffusion, DiffusionSynth ]()

   val diskGroup  = group
   val panGroup   = groupAfter( diskGroup )

   private val diffListener: Model.Listener = {
      case doc.diffusions.ElementAdded( idx, diff ) => {
         context.perform { inGroup( panGroup ) { addDiffusion( diff )}}
      }
      case doc.diffusions.ElementRemoved( idx, diff ) => context.perform { removeDiffusion( diff )}
   }

   private val timeListener: Model.Listener = {
      case doc.timelines.ElementAdded( idx, tl ) => context.perform { addTimeline( new SCTimeline( this, tl ))}
      case doc.timelines.ElementRemoved( idx, tl ) => context.perform { removeTimeline( tl )}
   }

   // ---- constructor2 ----
   inGroup( panGroup ) {
      doc.diffusions.foreach( diff => addDiffusion( diff ))
   }
   if( realtime ) {
      doc.timelines.foreach( tl => addTimeline( new SCTimeline( this, tl )))
      doc.timelines.addListener( timeListener )
      doc.diffusions.addListener( diffListener )
   }

//println( "SESSION <<<<<< " )

  // ---- SynthContext ----
  def invalidate( obj: AnyRef ) = ()
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

   def diffusion( diff: Diffusion ) : DiffusionSynth = diffusions( diff )
   def timeline( tl: Timeline ) : SCTimeline = timelines( tl )

   def dispose(): Unit = {
      if( realtime ) {
         doc.timelines.removeListener( timeListener )
         doc.diffusions.removeListener( diffListener )
      }
      timelines.keysIterator.foreach( tl => removeTimeline( tl ))
      diffusions.keysIterator.foreach( diff => removeDiffusion( diff ))
      group.free(); panGroup.free()
   }

   private def addDiffusion( diff: Diffusion ): Unit =
      DiffusionSynthFactory.get( diff ).foreach( dsf => {
         val ds = dsf.create
         ds.play()
         diffusions += diff -> ds
      })

   private def removeDiffusion( diff: Diffusion ): Unit = {
      val odiff = diffusions( diff )
      diffusions -= diff
      odiff.dispose()
   }

   def addTimeline( sctl: SCTimeline ): Unit = timelines += sctl.tl -> sctl

   private def removeTimeline( tl: Timeline ): Unit = {
      val otl = timelines( tl )
      timelines -= tl
      otl.dispose()
   }

//       private def diffusionChanged( diff: Diffusion ) {
//          removeDiffusion( diff )
//          addDiffusion( diff )
//       }
}