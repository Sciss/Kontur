package de.sciss.kontur.sc

import scala.collection.mutable.{ PriorityQueue }
import java.io.{ BufferedInputStream, File, IOException, RandomAccessFile }
import java.nio.{ ByteBuffer }
import javax.swing.{ SwingWorker }
import scala.math._
import de.sciss.app.{ AbstractApplication }
import de.sciss.io.{ AudioFileDescr, IOUtil }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.scalaosc.{ OSCBundle, OSCMessage, OSCPacket, OSCPacketCodec }
import de.sciss.tint.sc._

object BounceSynthContext {
   @throws( classOf[ IOException ])
   def apply( so: ServerOptions ) : BounceSynthContext = {
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
      so.nrtCmdPath.value = oscPath.getCanonicalPath
      val s = new Server( "Bounce", so )
      val context = new BounceSynthContext( s, oscPath, oscFile )
      context
   }
}

class BounceSynthContext private( s: Server, oscPath: File, oscFile: RandomAccessFile )
extends SynthContext( s, false ) {
   private val verbose =  false

   private var timebaseVar = 0.0
   private val bundleQueue = new PriorityQueue[ Bundle ]()( BundleOrdering )
   private var fileOpen    = true
   private val codec       = new OSCPacketCodec( OSCPacketCodec.MODE_GRACEFUL )
   private val bb          = ByteBuffer.allocateDirect( 65536 )
   private val fch         = oscFile.getChannel()

   // ---- constructor ----
   {
      // XXX initTree missing at the moment
      perform {
         add( server.defaultGroup.newMsg( server.rootNode, addToHead ))
      }
   }

   def timebase = timebaseVar
   def timebase_=( newVal: Double ) {
      if( newVal < timebaseVar ) throw new IllegalArgumentException( newVal.toString )
      if( newVal > timebaseVar ) {
         advanceTo( newVal )
      }
   }

   override val sampleRate : Double = server.options.sampleRate.value

   private def advanceTo( newTime: Double ) {
      var keepGoing = true
      while( keepGoing ) {
         keepGoing = bundleQueue.headOption.map( b => {
            if( b.time <= newTime ) {
               bundleQueue.dequeue
               timebaseVar = b.time // important because write calls doAsync
               write( b )
               true
            } else false
         }) getOrElse false
      }
      timebaseVar = newTime
   }

   protected def initBundle( delta: Double ) {
      bundle = new Bundle( timebase + max( 0.0, delta ))
   }

   @throws( classOf[ IOException ])
   def render: SwingWorker[ _, _ ] = {
      flush
      close
      val program = server.options.programPath.value
//      println( "Booting '" + program + "'" )
      val appPath = new File( program )
      // -N cmd-filename input-filename output-filename sample-rate header-format sample-format -o numOutChans
//      server.options.nrtOutputPath.value = descr.file.getCanonicalPath
      val processArgs = server.options.toNonRealtimeArgs.toArray
      println( processArgs.mkString( " " ))
      val pb = new ProcessBuilder( processArgs: _* )
        .directory( appPath.getParentFile )
        .redirectErrorStream( true )
      val w = new SwingWorker[ Int, Unit ]() {
         override def doInBackground: Int = {
            var pRunning   = true
            val p          = pb.start
            val inStream	= new BufferedInputStream( p.getInputStream )
            val printWorker   = new SwingWorker[ Unit, Unit ]() {
               private val buf = new Array[ Byte ]( 128 )
               override def doInBackground {
                  while( true ) {
                     var cnt = 0
                     val byt = inStream.read()
                     if( byt == -1 ) return
                     buf( 0 ) = byt.toByte
                     cnt += 1
                     while( (inStream.available > 0) && (cnt < 128) ) {
                        val num = min( 128 - cnt, inStream.available() )
                        inStream.read( buf, cnt, num )
                        cnt += num
                     }
                     System.out.write( buf, 0, cnt )
                  }
               }
            }
            try {
               printWorker.execute()
               p.waitFor()
            } catch { case e: InterruptedException => }

            printWorker.cancel( true )

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
   private def close {
      if( fileOpen ) {
         oscFile.close
         fileOpen = false
      }
   }

   override def play( rsd: RichSynthDef, args: Seq[ Tuple2[ String, Float ]]) : RichSynth = {
      val rs = super.play( rsd, args )
      addAsync( rs ) // simulate n_go
      rs
   }

   def dispose {
      try {
         close
         if( !oscPath.delete ) oscPath.deleteOnExit()
      }
      catch { case e: IOException => e.printStackTrace }
   }

   override def endsAfter( rn: RichNode, dur: Double ) {
//println( "endsAfter " + dur + ": " + rn.node )
      delayed( timebase, dur ) {
//println( "endsAfter really " + dur + ": " + rn.node )
//         add( rn.node.freeMsg )
//         rn.isOnline = false
         addAsync( new AsyncAction {
            def asyncDone { rn.isOnline = false }
         })
      } // simulate n_end
   }

   @throws( classOf[ IOException ])
   private def write( b: Bundle ) {
      val bndl = OSCBundle.secs( b.time, b.messages: _* )
      bb.clear
      bndl.encode( codec, bb )
      bb.flip
      oscFile.writeInt( bb.limit() )   // a little bit strange to use both RandomAccessFile...
      fch.write( bb )                  // ...and FileChannel... but neither has both writeInt and write( bb )
      if( verbose ) {
         OSCPacket.printTextOn( codec, System.out, bndl )
      }
      if( b.hasAsync ) perform { b.doAsync } // important to check hasAsync, as we create an infinite loop otherwise
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
   private def flush {
      while( bundleQueue.nonEmpty ) {
         val b = bundleQueue.dequeue
         if( b.time <= timebaseVar ) {
            timebaseVar = b.time // important because write calls doAsync
            write( b )
         }
      }
   }

   private class Bundle( val time: Double )
   extends AbstractBundle {
      @throws( classOf[ IOException ])
      def send {
         enqueue( this )
      }
   }

   private object BundleOrdering extends Ordering[ Bundle ] {
      def compare( x: Bundle, y: Bundle ) : Int = -Ordering.Double.compare( x.time, y.time ) // low times first
   }
}