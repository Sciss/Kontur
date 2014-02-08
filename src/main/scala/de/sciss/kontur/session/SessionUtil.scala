/*
 *  SessionUtil.scala
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
package session

import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import javax.swing.SwingWorker
import sc.{BounceSynthContext, SCSession, SCTimeline}
import util.PrefsUtil
import de.sciss.synth.io.AudioFileSpec
import java.io.File
import de.sciss.synth.Server
import de.sciss.span.Span
import legacy.Param

object SessionUtil {
   private def getResourceString( key: String ) =
      key // XXX TODO AbstractApplication.getApplication.getResourceString( key )

  trait Process {
    def cancel(): Unit
  }

  def bounce(doc: Session, tl: Timeline, tracks: List[Track], span: Span, path: File, spec: AudioFileSpec,
             upd: AnyRef => Unit = _ => ())(implicit application: de.sciss.desktop.Application): Process = {

    import desktop.Implicits._

    val so                        = Server.Config()
    so.nrtOutputPath              = path.getCanonicalPath
    so.outputBusChannels          = spec.numChannels // 2   // XXX
    so.nrtHeaderFormat            = spec.fileType
    so.nrtSampleFormat            = spec.sampleFormat
    val sampleRate                = spec.sampleRate
    so.sampleRate                 = sampleRate.toInt
    val audioPrefs                = application.userPrefs / PrefsUtil.NODE_AUDIO
    audioPrefs.get[Param](PrefsUtil.KEY_SCMEMSIZE).foreach { p =>
       so.memorySize              = p.value.toInt << 10
    }
    so.blockSize                  = 1
    so.loadSynthDefs              = false
    audioPrefs.get[File](PrefsUtil.KEY_SUPERCOLLIDERAPP).foreach { p =>
      so.programPath = p.getPath
    }

    // karlheinz bounce core
    val context = BounceSynthContext(so)
    var scDoc: SCSession = null // XXX stupid
    var scTL: SCTimeline = null // XXX stupid

    context.perform {
      scDoc = new SCSession(doc)
      scTL  = new SCTimeline(scDoc, tl)
      scDoc.addTimeline(scTL)
      tracks.foreach(t => scTL.addTrack(t))
      scTL.play(span.start, 1.0)
    }
    // "play" XXX should be in a worker thread...
    val stepSize = (sampleRate * 0.1).toLong
    var pos = span.start
    while (pos < span.stop) {
      val chunkLen = math.min(stepSize, span.stop - pos)
      val procSpan = Span(pos, pos + chunkLen)
      context.perform {
        scTL.step(pos, procSpan)
      }
      pos += chunkLen
      context.timebase = (pos - span.start).toDouble / sampleRate
    }

    // "stop"
    context.perform {
      scTL.stop()
      scDoc.dispose()
    }

    // run NRT
    val worker = context.render
    worker.addPropertyChangeListener(new PropertyChangeListener {
      def propertyChange(e: PropertyChangeEvent): Unit = {
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
          upd.apply(info)
        } catch {
          case e: MatchError =>
        }
      }
    })
    worker.execute()

    new Process {
      def cancel(): Unit = {
        println("WARNING: CANCEL DOES NOT WORK YET PROPERLY")
        worker.cancel(true)
      }
    }
  }
}