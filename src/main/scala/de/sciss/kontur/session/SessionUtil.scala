/*
 *  SessionUtil.scala
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

package de.sciss.kontur.session

import java.beans.{ PropertyChangeEvent, PropertyChangeListener }
import javax.swing.SwingWorker
import de.sciss.app.AbstractApplication
import de.sciss.util.Param
import de.sciss.kontur.sc.{ BounceSynthContext, SCSession, SCTimeline }
import de.sciss.kontur.util.PrefsUtil
import de.sciss.synth.io.AudioFileSpec
import java.io.{File, IOException}
import de.sciss.synth.Server
import de.sciss.span.Span

object SessionUtil {
   private def getResourceString( key: String ) =
      AbstractApplication.getApplication.getResourceString( key )

   @throws( classOf[ IOException ])
   def bounce( doc: Session, tl: Timeline, tracks: List[ Track ], span: Span, path: File, spec: AudioFileSpec,
               upd: AnyRef => Unit = _ => () ) : { def cancel(): Unit } = {
      
      val so                        = Server.Config()
      so.nrtOutputPath              = path.getCanonicalPath
      so.outputBusChannels          = spec.numChannels // 2   // XXX
      so.nrtHeaderFormat            = spec.fileType
      so.nrtSampleFormat            = spec.sampleFormat
      val sampleRate                = spec.sampleRate
      so.sampleRate                 = sampleRate.toInt
      val audioPrefs                = AbstractApplication.getApplication.getUserPrefs.node( PrefsUtil.NODE_AUDIO )
      val pMemSize = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCMEMSIZE, null )
      if( pMemSize != null ) {
         so.memorySize              = pMemSize.`val`.toInt << 10
      }
      so.blockSize                  = 1
      so.loadSynthDefs              = false
      val appPath = audioPrefs.get( PrefsUtil.KEY_SUPERCOLLIDERAPP, null )
      if( appPath == null ) {
         throw new Exception( getResourceString( "errSCSynthAppNotFound" ))
      }
      so.programPath              = appPath

      // karlheinz bounce core
      val context = BounceSynthContext( so )
      var scDoc: SCSession = null // XXX stupid
      var scTL: SCTimeline = null // XXX stupid

      context.perform {
         scDoc    = new SCSession( doc )
         scTL     = new SCTimeline( scDoc, tl )
         scDoc.addTimeline( scTL )
         tracks.foreach( t => scTL.addTrack( t ))
         scTL.play( span.start, 1.0 )
      }
      // "play" XXX should be in a worker thread...
      val stepSize = (sampleRate * 0.1).toLong
      var pos     = span.start
      while( pos < span.stop ) {
         val chunkLen      = math.min( stepSize, span.stop - pos )
         val procSpan      = Span( pos, pos + chunkLen )
         context.perform {
            scTL.step( pos, procSpan )
         }
         pos += chunkLen
         context.timebase  = (pos - span.start).toDouble / sampleRate
      }

      // "stop"
      context.perform {
         scTL.stop()
         scDoc.dispose()
      }

      // run NRT
      val worker  = context.render
      worker.addPropertyChangeListener( new PropertyChangeListener {
         def propertyChange( e: PropertyChangeEvent ) {
            try {
//               println( "prop name '" + e.getPropertyName + "'; val = '" + e.getNewValue + "'" )
               val info: AnyRef = e.getPropertyName match {
                  case "state" => e.getNewValue match {
                     case SwingWorker.StateValue.DONE    => "done"
                     case SwingWorker.StateValue.STARTED => "started"
                  }
                  case "progress" => e.getNewValue match {
                     case i: java.lang.Integer => ("progress", i.intValue)
                  }
               }
               upd.apply( info )
            } catch { case e: MatchError => }
         }
      })
      worker.execute()
      new {
         def cancel() {
            println( "WARNING: CANCEL DOES NOT WORK YET PROPERLY" )
            worker.cancel( true )
         }
      }
   }
}