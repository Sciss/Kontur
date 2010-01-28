/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.io

import java.awt.{ Dimension, Graphics2D }
import java.awt.image.{ BufferedImage, DataBufferInt, ImageObserver }
import java.beans.{ PropertyChangeEvent, PropertyChangeListener }
import java.io.{ File, IOException }
import java.util.{ Arrays }
import javax.swing.{ SwingWorker }
import scala.collection.immutable.{ Queue, Vector }
import scala.collection.mutable.{ ListBuffer }
import scala.math._
import de.sciss.dsp.{ ConstQ, FastLog }
import de.sciss.io.{ AudioFile, AudioFileDescr, IOUtil, Span }
import de.sciss.kontur.gui.{ IntensityColorScheme }

case class SonagramSpec( sampleRate: Double, minFreq: Float, maxFreq: Float,
                         bandsPerOct: Int, maxTimeRes: Float, maxFFTSize: Int ) {

}

trait SonagramPaintController {
   def imageObserver: ImageObserver
   def adjustGain( amp: Float, pos: Double ) : Float
}

object SonagramOverview {
   var verbose = false

   private var constQCache   = Map[ SonagramSpec, ConstQCache ]()
   private var imageCache    = Map[ Dimension, ImageCache ]()
   private var fileBufCache  = Map[ SonagramImageSpec, FileBufCache ]()
   private val sync  = new AnyRef

   @throws( classOf[ IOException ])
   def fromPath( path: File, cacheFolders: Seq[ File ]) : SonagramOverview = {
      sync.synchronized {
         val cachePath  = IOUtil.setFileSuffix(
            new File( cacheFolders.head, path.getName ), "sona" )
         // XXX first try, forget cache reuse! So create a fresh file each time:
         val cachePath2 = IOUtil.nonExistentFileVariant( cachePath, -1, null, null )
         new SonagramOverview( path, cachePath2 )
      }
   }

   private def allocateConstQ( spec: SonagramSpec, createKernels: Boolean = true ) : ConstQ = {
      sync.synchronized {
         val entry = constQCache.get( spec ) getOrElse
            new ConstQCache( constQFromSpec( spec, createKernels ))
         entry.useCount += 1
         constQCache += (spec -> entry)  // in case it was newly created
         entry.constQ
      }
   }

   private def allocateSonaImage( spec: SonagramImageSpec ) : SonagramImage = {
      sync.synchronized {
         val img     = allocateImage( spec.dim )
         val fileBuf = allocateFileBuf( spec )
         SonagramImage( img, fileBuf )
      }
   }

   private def releaseSonaImage( spec: SonagramImageSpec ) {
      sync.synchronized {
         releaseImage( spec.dim )
         releaseFileBuf( spec )
      }
   }

   private[this] def allocateImage( dim: Dimension ) : BufferedImage = {
      sync.synchronized {
         val entry = imageCache.get( dim ) getOrElse
            new ImageCache( new BufferedImage( dim.width, dim.height, BufferedImage.TYPE_INT_RGB ))
         entry.useCount += 1
         imageCache += (dim -> entry)  // in case it was newly created
         entry.img
      }
   }

   private[this] def allocateFileBuf( spec: SonagramImageSpec ) : Array[ Array[ Float ]] = {
      sync.synchronized {
         val entry = fileBufCache.get( spec ) getOrElse
            new FileBufCache( Array.ofDim[ Float ]( spec.numChannels, spec.dim.width * spec.dim.height ))
         entry.useCount += 1
         fileBufCache += (spec -> entry)  // in case it was newly created
         entry.buf
      }
   }

//   private def releaseConstQ( constQ: ConstQ ) : Unit =
//      releaseConstQ( specFromConstQ( constQ ))

   private def releaseConstQ( spec: SonagramSpec ) {
      sync.synchronized {
         val entry   = constQCache( spec ) // let it throw an exception if not contained
         entry.useCount -= 1
         if( entry.useCount == 0 ) {
            constQCache -= spec
         }
      }
   }

   private[this] def releaseImage( dim: Dimension ) {
      sync.synchronized {
         val entry = imageCache( dim ) // let it throw an exception if not contained
         entry.useCount -= 1
         if( entry.useCount == 0 ) {
            imageCache -= dim
         }
      }
   }

   private[this] def releaseFileBuf( spec: SonagramImageSpec ) {
      sync.synchronized {
         val entry = fileBufCache( spec ) // let it throw an exception if not contained
         entry.useCount -= 1
         if( entry.useCount == 0 ) {
            fileBufCache -= spec
         }
      }
   }

//   private def specFromConstQ( constQ: ConstQ ) =
//      SonagramSpec( constQ.getSampleRate, constQ.getMinFreq, constQ.getMaxFreq,
//                    constQ.getBandsPerOct, constQ.getMaxTimeRes,
//                    constQ.getMaxFFTSize )

   private[this] def constQFromSpec( spec: SonagramSpec, createKernels: Boolean ) : ConstQ = {
   	val constQ  = new ConstQ
		constQ.setSampleRate( spec.sampleRate )
      constQ.setMinFreq( spec.minFreq )
      constQ.setMaxFreq( spec.maxFreq )
      constQ.setBandsPerOct( spec.bandsPerOct )
      constQ.setMaxTimeRes( spec.maxTimeRes )
      constQ.setMaxFFTSize( spec.maxFFTSize )
//		println( "Creating ConstQ Kernels..." )
		if( createKernels ) constQ.createKernels()
      constQ
   }

   private[this] var workerQueue = Queue[ WorkingSonagram ]()
   private[this] var runningWorker: Option[ WorkingSonagram ] = None

   private def queue( sona: SonagramOverview ) {
      sync.synchronized {
         workerQueue = workerQueue.enqueue( new WorkingSonagram( sona ))
         checkRun
      }
   }

   private[this] def dequeue( ws: WorkingSonagram ) {
      sync.synchronized {
         val (s, q) = workerQueue.dequeue
         workerQueue = q
         assert( ws == s )
         checkRun
      }
   }

   private[this] def checkRun {
      sync.synchronized {
         if( runningWorker.isEmpty ) {
            workerQueue.headOption.foreach( next => {
               runningWorker = Some( next )
               next.addPropertyChangeListener( new PropertyChangeListener {
                  def propertyChange( e: PropertyChangeEvent ) {
if( verbose ) println( "WorkingSonagram got in : " + e.getPropertyName + " / " + e.getNewValue )
                     if( e.getNewValue == SwingWorker.StateValue.DONE ) {
                        runningWorker = None
                        dequeue( next )
                     }
                  }
               })
               next.execute()
            })
         }
      }
   }

   private[this] class ConstQCache( val constQ: ConstQ ) {
      var useCount: Int = 0
   }

   private[this] class FileBufCache( val buf: Array[ Array[ Float ]]) {
      var useCount: Int = 0
   }

   private[this] class ImageCache( val img: BufferedImage ) {
      var useCount: Int = 0
   }

   private case class SonagramImage( img: BufferedImage, fileBuf: Array[ Array[ Float ]])

   private class WorkingSonagram( val sona: SonagramOverview )
   extends SwingWorker[ Unit, Unit ] {
      override protected def doInBackground() : Unit = {
         sona.render( this )
      }
   }

   private lazy val log10 = new FastLog( 10, 11 )

   private case class SonagramImageSpec( numChannels: Int, dim: Dimension )
}

class SonagramOverview @throws( classOf[ IOException ]) private (
   audioPath: File, cachePath: File ) {

   import SonagramOverview._

   // if this fails, just let the exception go, no other
   // resources have been allocated yet
   private val sync              = new AnyRef
   private val masterFile        = AudioFile.openAsRead( audioPath )
   private val masterDescr       = masterFile.getDescr
   private val numChannels       = masterDescr.channels
   // at the moment, let us just use a moderate fixed spec
   private val fftStepSize       = max( 64, (masterDescr.rate * 0.0116 + 0.5).toInt ) // 11.6ms spacing
   private val spec              = SonagramSpec( masterDescr.rate, 32, 16384, 24,
                                (fftStepSize / masterDescr.rate * 1000).toFloat, 4096 )
   private var constQReleased    = false
   private val constQ            = allocateConstQ( spec )
   private val numKernels        = constQ.getNumKernels
   private val imgSpec           = SonagramImageSpec( numChannels, new Dimension( 128, numKernels ))
   private val sonaImg           = allocateSonaImage( imgSpec )
//   private val imgDim            = new Dimension( 128, numKernels )
//   private val bufImg            = allocateImage( imgDim )
   private val imgData           = sonaImg.img.getRaster.getDataBuffer.asInstanceOf[ DataBufferInt ].getData()
	private val fftSize        	= constQ.getFFTSize
//   private val numSteps          = (masterDescr.length + fftStepSize - 1) / fftStepSize
//   private val numDecim          = 3
//   private val decimFactor       = 8   // e.g. 6 seconds per pixel max coarse resolution

   // for all prospective IOExceptions, guarantee cleanup
//   private val sonaFiles         = try { createFiles( 8, 8, 8 )
//   private val sonaFiles         = try { createFiles( 5, 5, 5, 5 )
   private val sonaFiles         = try { createFiles( 6, 6, 6, 6 )
   } catch { case e1: IOException => { dispose; throw e1 }}

   // ---- constructor ----
   {
if( verbose ) println( "fftSize = " + fftSize + "; numKernels = " + numKernels + "; fftStepSize = " + fftStepSize )
      queue( this )
   }


   private def getBestDecim( idealDecim: Float ) : SonagramFile = {
      var best = sonaFiles.head
      var i = 0; while( (i < sonaFiles.size) && (sonaFiles( i ).totalDecim < idealDecim) ) {
         best = sonaFiles( i )
         i += 1
      }
      best
   }

   val rnd = new java.util.Random()
   def paint( spanStart: Double, spanStop: Double, g2: Graphics2D, tx: Int,
              ty: Int, width: Int, height: Int,
              ctrl: SonagramPaintController ) {
      val idealDecim    = ((spanStop - spanStart) / width).toFloat
      val in            = getBestDecim( idealDecim )
//      val scaleW        = idealDecim / in.totalDecim
      val scaleW        = in.totalDecim / idealDecim
      val vDecim        = max( 1, (numChannels * numKernels) / height )
      val numVFull      = numKernels / vDecim
//      val vRemain       = numKernels % vDecim
//      val hasVRemain    = vRemain != 0
      val scaleH        = height.toFloat / (numChannels * numVFull)
      val startD        = spanStart / in.totalDecim
      val start         = startD.toLong  // i.e. trunc
//      val transX        = (-(startD % 1.0) / idealDecim).toFloat
      val transX        = (-(startD % 1.0) * scaleW).toFloat
      val stop          = ceil( spanStop / in.totalDecim ).toLong // XXX include startD % 1.0 ?

      var windowsRead   = 0L
      val imgW          = imgSpec.dim.width
      val l10           = log10
      val c             = IntensityColorScheme.colors

      val pixScale      = 1072f / 6 // 1072 / bels span
      val pixOff        = 6f // bels floor
      val iBuf          = imgData
//      val numK          = numKernels

      val atOrig        = g2.getTransform
      try {
         g2.translate( tx + transX, ty )
         g2.scale( scaleW, scaleH )
         var xOff = 0; var yOff = 0; var fOff = 0; var iOff = 0;
         var x = 0; var v = 0; var i = 0; var sum = 0f
         var xReset = 0
         var firstPass = true
         in.synchronized {
            if( in.windowsReady <= start ) return  // or draw busy-area
            in.seekWindow( start )
            val numWindows = min( in.windowsReady, stop ) - start
            while( windowsRead < numWindows ) {
               val chunkLen2 = min( imgW - xReset, numWindows - windowsRead ).toInt
               val chunkLen = chunkLen2 + xReset
               in.af.readFrames( sonaImg.fileBuf, 0, chunkLen2 * numKernels )
               windowsRead += chunkLen2
               if( firstPass ) {
                  firstPass = false
               } else {
                  xReset = 4  // overlap
                  iOff = 0; v = 0; while( v < numVFull ) {
                     iBuf( iOff )     = iBuf( iOff + imgW - 4)
                     iBuf( iOff + 1 ) = iBuf( iOff + imgW - 3)
                     iBuf( iOff + 2 ) = iBuf( iOff + imgW - 2 )
                     iBuf( iOff + 3 ) = iBuf( iOff + imgW - 1 )
                  v += 1; iOff += imgW }
               }
               yOff = 0; var ch = 0; while( ch < numChannels ) {
                  val fBuf = sonaImg.fileBuf( ch )
                  fOff = 0
                  x = xReset; while( x < chunkLen ) {
                     iOff = x
                     v = 0; while( v < numVFull ) {
                        sum = fBuf( fOff )
                        i = 0; while( i < vDecim ) {
                           sum += fBuf( fOff )
                           fOff += 1; i += 1
                        }
                        val amp = ctrl.adjustGain( sum / vDecim, (iOff + xOff) / scaleW )
                        iBuf( iOff ) = c( max( 0, min( 1072,
                           ((l10.calc( max( 1.0e-9f, amp )) + pixOff) * pixScale).toInt )))
                        v += 1; iOff += imgW
                     }
/*
                     if( hasVRemain ) {
                        var sum = fBuf( fOff )
                        fOff += 1
                        var i = 0; while( i < vRemain ) {
                           sum += fBuf( fOff )
                           fOff += 1; i += 1
                        }
                        val ampLog = l10.calc( max( 1.0e-9f, sum / vRemain ))
                        iBuf( iOff ) = c( max( 0, min( 1072, ((ampLog + pixOff) * pixScale).toInt )))
                        iOff += imgW
                     }
*/
                  x += 1 }
//                  g2.drawImage( sonaImg.img, xOff, yOff, observer )
                  g2.drawImage( sonaImg.img, xOff, yOff, xOff + chunkLen, yOff + numVFull,
                                             0, 0, chunkLen, numVFull, ctrl.imageObserver )
               ch += 1; yOff += numVFull }
               xOff += chunkLen - 4
            }
         }
      }
      finally {
         g2.setTransform( atOrig )
      }
   }

   protected def render( ws: WorkingSonagram ) {
      try {
val t1 = System.currentTimeMillis
         primaryRender( ws, masterFile, sonaFiles.head )
         releaseCQ() // we do not need it anymore
val t2 = System.currentTimeMillis
         sonaFiles.sliding( 2, 1 ).foreach( pair => {
            if( ws.isCancelled ) return
            secondaryRender( ws, pair.head, pair.last )
         })
val t3 = System.currentTimeMillis
println( "primary : secondary ratio = " + (t2 - t1).toDouble / (t3 - t1) )
      }
      finally {
         masterFile.cleanUp
      }
   }

   private def primaryRender( ws: WorkingSonagram, in: AudioFile, out: SonagramFile ) {
      // first ensure total file size to catch disk-full problem early
//      val primaryNumFrames = out.numWindows * numKernels
//      out.af.setFrameNum( primaryNumFrames )
//      if( ws.isCancelled ) return

//println( "primary len = " + primaryNumFrames + "; secondary len = " + secondaryNumFrames )

//      val inBuf  = new Array[ Array[ Float ]]( numChannels, fftSize )
//      val outBuf = new Array[ Array[ Float ]]( numChannels, numKernels )
      val inBuf  = Array.ofDim[ Float ]( numChannels, fftSize )
      val outBuf = Array.ofDim[ Float ]( numChannels, numKernels )

      var inOff         = fftSize / 2
      var inLen         = fftSize - inOff
      val overLen       = fftSize - fftStepSize
      val numFrames     = masterDescr.length
      var framesRead    = 0L

      { var step = 0; while( step < out.numWindows && !ws.isCancelled ) {
         val chunkLen = min( inLen, numFrames - framesRead ).toInt
         masterFile.readFrames( inBuf, inOff, chunkLen )
         framesRead += chunkLen
         if( chunkLen < inLen ) {
            { var ch = 0; while( ch < numChannels ) {
               Arrays.fill( inBuf( ch ), inOff + chunkLen, fftSize, 0f )
            ch += 1 }}
         }
         { var ch = 0; while( ch < numChannels ) {
            // input, inOff, inLen, output, outOff
            constQ.transform( inBuf( ch ), 0, fftSize, outBuf( ch ), 0 )
         ch += 1 }}

         out.synchronized {
            out.seekWindow( out.windowsReady )
            out.af.writeFrames( outBuf, 0, numKernels )
            out.windowsReady += 1
         }

         { var ch = 0; while( ch < numChannels ) {
            val convBuf = inBuf( ch )
            System.arraycopy( convBuf, fftStepSize, convBuf, 0, overLen )
         ch += 1 }}

         if( step == 0 ) { // stupid one instance case
            inOff = overLen
            inLen = fftStepSize
         }
      step += 1 }}
   }
   
   private def secondaryRender( ws: WorkingSonagram, in: SonagramFile, out: SonagramFile ) {
      val dec           = out.decimFactor
      val bufSize       = dec * numKernels
//    val buf           = new Array[ Array[ Float ]]( numChannels, bufSize )
      val buf           = Array.ofDim[ Float ]( numChannels, bufSize )
      // since dec is supposed to be even, this
      // lands on the beginning of a kernel:
      var inOff         = bufSize / 2
      var inLen         = bufSize - inOff
      var windowsRead   = 0L

      { var step = 0; while( step < out.numWindows && !ws.isCancelled ) {
         val chunkLen = min( inLen, (in.numWindows - windowsRead) * numKernels ).toInt
         in.synchronized {
            in.seekWindow( windowsRead )
            in.af.readFrames( buf, inOff, chunkLen )
         }
         windowsRead += chunkLen / numKernels
         if( chunkLen < inLen ) {
            { var ch = 0; while( ch < numChannels ) {
               Arrays.fill( buf( ch ), inOff + chunkLen, fftSize, 0f )
            ch += 1 }}
         }
         { var ch = 0; while( ch < numChannels ) {
            val convBuf = buf( ch )
            var i = 0; while( i < numKernels ) {
               var sum = 0f
               var j = i; while( j < bufSize ) {
                  sum += convBuf( j )
               j += numKernels }
               convBuf( i ) = sum / dec
            i += 1 }
         ch += 1 }}

         out.synchronized {
            out.seekWindow( out.windowsReady )
            out.af.writeFrames( buf, 0, numKernels )
            out.windowsReady += 1
         }

         if( step == 0 ) { // stupid one instance case
            inOff = 0
            inLen = bufSize
         }
      step += 1 }}
   }

   private def releaseCQ() {
      sync.synchronized {
         if( !constQReleased ) {
            releaseConstQ( spec )
            constQReleased = true
         }
      }
   }

   private var disposed = false
   def dispose {
      if( !disposed ) {
         disposed = true
         releaseCQ()
         releaseSonaImage( imgSpec )
         masterFile.cleanUp
         sonaFiles.foreach( _.af.cleanUp )  // XXX delete
      }
   }

   @throws( classOf[ IOException ])
   private def createFiles( decimFactors: Int* ): Vector[ SonagramFile ] = {
      var files         = Vector[ SonagramFile ]()
      var currentPath   = cachePath
      var totalDecim    = fftStepSize
      var numWindows    = (masterDescr.length + fftStepSize - 1) / fftStepSize

if( decimFactors.exists( _ % 2 != 0 )) println( "WARNING: only even decim factors supported ATM" )
      
      try {
         (1 :: decimFactors.toList).foreach( decimFactor => {
            val d             = new AudioFileDescr()
            d.file            = currentPath
            d.`type`          = AudioFileDescr.TYPE_AIFF
            d.channels        = numChannels
            totalDecim       *= decimFactor
            d.rate            = masterDescr.rate * numKernels / totalDecim // XXX correct?
            d.bitsPerSample   = 32  // XXX really?
            d.sampleFormat    = AudioFileDescr.FORMAT_FLOAT
            val af            = AudioFile.openAsWrite( d ) // XXX eventually should use shared buffer!!
            numWindows        = (numWindows + decimFactor - 1) / decimFactor
            files = files.appendBack( new SonagramFile( af, numWindows, decimFactor, totalDecim ))
            currentPath       = IOUtil.nonExistentFileVariant( cachePath, -1, "_dec", null )
         })
         files
      } catch { case e1: IOException => {
         files.foreach( _.af.cleanUp ) // XXX and delete?
         throw e1
      }}
   }

   // note: totalDecim starts not from 1 but fftStepSize, so
   // denotes the actual scale of each window (pixel-column) to original rate
   private class SonagramFile( val af: AudioFile, val numWindows: Long, val decimFactor: Int, val totalDecim: Int ) {
      var windowsReady = 0L

      // caller must have sync
      def seekWindow( idx: Long ) {
         val framePos = idx * numKernels
         if( af.getFramePosition != framePos ) {
            af.seekFrame( framePos )
         }
      }
   }
}
