/*
 *  BounceSynthContext.scala
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

package de.sciss.kontur
package sc

import java.io.{BufferedReader, File, InputStreamReader, IOException, RandomAccessFile}
import java.nio.ByteBuffer
import javax.swing.SwingWorker
import de.sciss.osc
import de.sciss.synth._
import collection.mutable

object BounceSynthContext {
   @throws( classOf[ IOException ])
   def apply( so: Server.ConfigBuilder ) : BounceSynthContext = {
//      val appPath = audioPrefs.get( PrefsUtil.KEY_SUPERCOLLIDERAPP, null )
//      if( appPath == null ) {
//         throw new IOException( AbstractApplication.getApplication.getResourceString( "errSCSynthAppNotFound" ))
//      }
//      so.program.value = appPath
//      val so = new ServerOptions()
//      so.blockSize = 1
//      so.sampleRate
      val oscPath = IOUtil.createTempFile( "kontur", ".osc" )
      val oscFile = new RandomAccessFile( oscPath, "rw" )
//      oscFile.setLength( 0L )
      so.nrtCommandPath = oscPath.getCanonicalPath
      val s = Server.dummy( "Bounce", so.build )
      val context = new BounceSynthContext( s, oscPath, oscFile )
      context
   }

   /*
    * There is a problem with buffers being repeated in NRT when they are freed exactly at
    * the end of the region playing synth. We are heuristically applying a 100 ms delay
    * for the buffer freeing which solves this problem.
    *
    * Still, we should investigate what exact value is needed (as this might change
    * with sampling rate and block size).
    */
   private val SECURITY_BOUND = 0.10
}

class BounceSynthContext private( s: Server, oscPath: File, oscFile: RandomAccessFile )
extends SynthContext( s, false ) {
   import BounceSynthContext._

   private val verbose =  false

   private var timebaseVar = 0.0
   private val bundleQueue = new mutable.PriorityQueue[ Bundle ]()( BundleOrdering )
   private var fileOpen    = true
   private val codec       = osc.PacketCodec().scsynth().build
   private val bb          = ByteBuffer.allocateDirect( 65536 )
   private val fch         = oscFile.getChannel

   // ---- constructor ----
   // XXX initTree missing at the moment
   perform {
      add( server.defaultGroup.newMsg( server.rootNode, addToHead ))
   }

   def timebase = timebaseVar
   def timebase_=( newVal: Double ) {
      if( newVal < timebaseVar ) throw new IllegalArgumentException( newVal.toString )
      if( newVal > timebaseVar ) {
         advanceTo( newVal )
      }
   }

   override def toString = "Offline"

   override val sampleRate : Double = server.config.sampleRate // .value

   private def advanceTo( newTime: Double ) {
      var keepGoing = true
      while( keepGoing ) {
         keepGoing = bundleQueue.headOption.map( b => {
            if( b.time <= newTime ) {
               bundleQueue.dequeue()
               timebaseVar = b.time // important because write calls doAsync
               write( b )
               true
            } else false
         }) getOrElse false
      }
      timebaseVar = newTime
   }

   protected def initBundle( delta: Double ) : AbstractBundle = {
      new Bundle( timebase + math.max( 0.0, delta ))
   }

   @throws( classOf[ IOException ])
   def render: SwingWorker[ _, _ ] = {
      flush()
      close()

      val dur = timebaseVar // in seconds
      val program = server.config.programPath // .value
//      println( "Booting '" + program + "'" )
      val appPath = new File( program )
      // -N cmd-filename input-filename output-filename sample-rate header-format sample-format -o numOutChans
//      server.options.nrtOutputPath.value = descr.file.getCanonicalPath
      val processArgs = server.config.toNonRealtimeArgs.toArray
      println( processArgs.mkString( " " ))
      val pb = new ProcessBuilder( processArgs: _* )
        .directory( appPath.getParentFile )
        .redirectErrorStream( true )

      val w = new SwingWorker[ Int, Unit ]() {
         main =>
         override def doInBackground(): Int = {
//            var pRunning   = true
//println( "PROC WORKER STARTED" )
            val p          = pb.start
//            val inStream	= new BufferedInputStream( p.getInputStream )
            val inReader      = new BufferedReader( new InputStreamReader( p.getInputStream ))
            // we used a SwingWorker before here, but it never ran on Scala 2.8.0...
            // might be connected to the actor starvation problem (using the same thread pool??)
            val printWorker   = new Thread {
               override def run() {
//println( "PRINT WORKER STARTED" )
                  try {
                     var lastProg = 0
                     while( true ) {
                        val line = inReader.readLine
//println( "GOT LINE: '" + line + "'" )
                        if( line == null ) return
                        if( line.startsWith( "nextOSCPacket" )) {
                           val time = line.substring( 14 ).toFloat
                           val prog = (time / dur * 100).toInt
//println( "time = " + time + "; dur = " + dur + "; prog = " + prog )
                           if( prog != lastProg ) {
//                        setProgress( prog )
                              // NOTE: main.setProgress does not work, probably because
                              // the thread is blocking...
                              main.firePropertyChange( "progress", lastProg, prog )
                              lastProg = prog
                           }
                        } else {
                           System.out.println( line )
                        }
                     }
                  } catch { case e: IOException =>
//                     println( "PRINT WORKER IOEXCEPTION" )
                  }
               }
            }
            try {
//println( "STARING PRINT WORKER" )
               printWorker.start() // execute()
//println( "WAITIN FOR PROCESS" )
               p.waitFor()
            } catch { case e: InterruptedException => }
//println( "RETURNED" )

//            printWorker.cancel( true )

            try {
               val resultCode	= p.exitValue
               println( "scsynth terminated (" + resultCode +")" )
               resultCode
            }
            catch { case e: IllegalThreadStateException => -1 } // gets thrown if we call exitValue() while sc still running
         }
      }
//    w.execute()
      w
   }

   @throws( classOf[ IOException ])
   private def close() {
      if( fileOpen ) {
         oscFile.close()
         fileOpen = false
      }
   }

   override def play( rsd: RichSynthDef, args: Seq[ ControlSetMap ]) : RichSynth = {
      val rs = super.play( rsd, args )
      addAsync( rs ) // simulate n_go
      rs
   }

   def dispose() {
      try {
         close()
         if( !oscPath.delete ) oscPath.deleteOnExit()
      }
      catch { case e: IOException => e.printStackTrace() }
   }

   override def endsAfter( rn: RichNode, dur: Double ) {
//println( "endsAfter " + dur + ": " + rn.node )
      delayed( timebase, dur + SECURITY_BOUND ) {
//println( "endsAfter really " + dur + ": " + rn.node )
//         add( rn.node.freeMsg )
//         rn.isOnline = false
         addAsync( new AsyncAction {
            def asyncDone() { rn.isOnline = false }
         })
      } // simulate n_end
   }

   @throws( classOf[ IOException ])
   private def write( b: Bundle ) {
      val bndl = osc.Bundle.secs( b.time, b.messages: _* )
      bb.clear
      bndl.encode( codec, bb )
      bb.flip
      oscFile.writeInt( bb.limit() )   // a little bit strange to use both RandomAccessFile...
      fch.write( bb )                  // ...and FileChannel... but neither has both writeInt and write( bb )
      if( verbose ) {
         osc.Packet.printTextOn( bndl, codec, System.out )
      }
      if( b.hasAsync ) perform { b.doAsync() } // important to check hasAsync, as we create an infinite loop otherwise
   }

   @throws( classOf[ IOException ])
   private def enqueue( b: Bundle ) {
      if( b.time < timebase ) throw new IOException( "Negative bundle time" )
      if( b.time == timebase ) {
         write( b )
      } else {
         bundleQueue.enqueue( b )
      }
   }

   @throws( classOf[ IOException ])
   private def flush() {
      while( bundleQueue.nonEmpty ) {
         val b = bundleQueue.dequeue()
         if( b.time <= timebaseVar ) {
            timebaseVar = b.time // important because write calls doAsync
            write( b )
         }
      }
   }

   private class Bundle( val time: Double )
   extends AbstractBundle {
      @throws( classOf[ IOException ])
      def send() {
         enqueue( this )
      }
   }

   private object BundleOrdering extends Ordering[ Bundle ] {
      def compare( x: Bundle, y: Bundle ) : Int = -Ordering.Double.compare( x.time, y.time ) // low times first
   }
}