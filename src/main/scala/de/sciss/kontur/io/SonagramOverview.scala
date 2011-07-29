/*
 *  SonagramOverview.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

package de.sciss.kontur.io

import java.awt.{ Dimension, Graphics2D }
import java.awt.image.{ BufferedImage, DataBufferInt, ImageObserver }
import java.beans.{ PropertyChangeEvent, PropertyChangeListener }
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream, File, IOException }
import java.util.{ Arrays }
import javax.swing.{ SwingWorker }
import scala.collection.immutable.{ Queue, Vector }
import scala.collection.mutable.{ ListBuffer }
import scala.math._
import de.sciss.app.{ AbstractApplication }
import de.sciss.io.{ AudioFile, AudioFileDescr, AudioFileCacheInfo, CacheManager, IOUtil, Span }
import de.sciss.kontur.gui.{ IntensityColorScheme }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.dsp.{ ConstQ, FastLog }

object SonagramSpec {
   def decode( dis: DataInputStream ) : Option[ SonagramSpec ] = {
      try {
         val sampleRate    = dis.readDouble()
         val minFreq       = dis.readFloat()
         val maxFreq       = dis.readFloat()
         val bandsPerOct   = dis.readInt()
         val maxTimeRes    = dis.readFloat()
         val maxFFTSize    = dis.readInt()
         val stepSize      = dis.readInt()
         Some( SonagramSpec( sampleRate, minFreq, maxFreq, bandsPerOct, maxTimeRes, maxFFTSize, stepSize ))
      }
      catch { case _ => None }
   }
}

case class SonagramSpec( sampleRate: Double, minFreq: Float, maxFreq: Float,
                         bandsPerOct: Int, maxTimeRes: Float, maxFFTSize: Int, stepSize: Int ) {

   val numKernels = ConstQ.getNumKernels( bandsPerOct, maxFreq, minFreq )

   def encode( dos: DataOutputStream ) {
      dos.writeDouble( sampleRate )
      dos.writeFloat( minFreq )
      dos.writeFloat( maxFreq )
      dos.writeInt( bandsPerOct )
      dos.writeFloat( maxTimeRes )
      dos.writeInt( maxFFTSize )
      dos.writeInt( stepSize )
   }
}

class SonagramDecimSpec( val offset: Long, val numWindows: Long, val decimFactor: Int, val totalDecim: Int ) {
   var windowsReady = 0L
}

object SonagramFileSpec {
   private val COOKIE   = 0x53000000  // 'Ttm ', 'S' version 0

   def decode( blob: Array[ Byte ]) : Option[ SonagramFileSpec ] = {
      val bais    = new ByteArrayInputStream( blob )
      val dis     = new DataInputStream( bais )
      val result  = decode( dis )
      bais.close
      result
   }

   def decode( dis: DataInputStream ) : Option[ SonagramFileSpec ] = {
      try {
         val cookie = dis.readInt()
         if( cookie != COOKIE ) return None
         SonagramSpec.decode( dis ).map( sona => {
            val lastModified  = dis.readLong()
            val audioPath     = new File( dis.readUTF() )
            val numFrames     = dis.readLong()
            val numChannels   = dis.readInt()
            val sampleRate    = dis.readDouble()
            val numDecim      = dis.readShort()
            val decim         = (0 until numDecim).map( i => dis.readShort().toInt ).toList
            SonagramFileSpec( sona, lastModified, audioPath, numFrames, numChannels, sampleRate, decim )
         }) orElse None
      }
      catch { case _ => None }
   }
}

case class SonagramFileSpec( sona: SonagramSpec, lastModified: Long, audioPath: File,
                             numFrames: Long, numChannels: Int, sampleRate: Double, decim: List[ Int ]) {

   import SonagramFileSpec._

   val decimSpecs = {
      var totalDecim    = sona.stepSize
      var numWindows    = (numFrames + totalDecim - 1) / totalDecim
      var offset        = 0L

      if( decim.tail.exists( _ % 2 != 0 )) println( "WARNING: only even decim factors supported ATM" )

      decim.map( decimFactor => {
         totalDecim       *= decimFactor
         numWindows        = (numWindows + decimFactor - 1) / decimFactor
         val decimSpec     = new SonagramDecimSpec( offset, numWindows, decimFactor, totalDecim )
         offset           += numWindows * sona.numKernels
         decimSpec
      })
   }

   def makeAllAvailable {
      decimSpecs.foreach( d => d.windowsReady = d.numWindows )
   }

   def expectedDecimNumFrames =
      decimSpecs.last.offset + decimSpecs.last.numWindows * sona.numKernels
  
   def getBestDecim( idealDecim: Float ) : SonagramDecimSpec = {
      var best = decimSpecs.head
      var i = 0; while( (i < decimSpecs.size) && (decimSpecs( i ).totalDecim < idealDecim) ) {
         best = decimSpecs( i )
         i += 1
      }
      best
   }

   def encode: Array[ Byte ] = {
      val baos = new ByteArrayOutputStream()
      val dos  = new DataOutputStream( baos )
      encode( dos )
      baos.close
      baos.toByteArray
   }

   def encode( dos: DataOutputStream ) {
      dos.writeInt( COOKIE )
      sona.encode( dos )
      dos.writeLong( lastModified )
      dos.writeUTF( audioPath.getCanonicalPath() )
      dos.writeLong( numFrames )
      dos.writeInt( numChannels )
      dos.writeDouble( sampleRate )
      dos.writeShort( decim.size )
      decim.foreach( d => dos.writeShort( d ))
   }
}

trait SonagramPaintController {
   def imageObserver: ImageObserver
   def adjustGain( amp: Float, pos: Double ) : Float
}

object SonagramOverview {
   var verbose = true
   private val APPCODE  = "Ttm "

   private var constQCache    = Map[ SonagramSpec, ConstQCache ]()
   private var imageCache     = Map[ Dimension, ImageCache ]()
   private var fileBufCache   = Map[ SonagramImageSpec, FileBufCache ]()
   private val sync           = new AnyRef
   private lazy val fileCache = {
      val app = AbstractApplication.getApplication 
      new PrefCacheManager(
         app.getUserPrefs.node( PrefsUtil.NODE_IO ).node( PrefsUtil.NODE_SONACACHE ),
         true, new File( System.getProperty( "java.io.tmpdir" ), app.getName ), 10240 ) // XXX 10 GB
   }

   @throws( classOf[ IOException ])
   def fromPath( path: File ) : SonagramOverview = {
      sync.synchronized {
         val af            = AudioFile.openAsRead( path )
         val afDescr       = af.getDescr
         af.close // render loop will re-open it if necessary...
         val sampleRate    = afDescr.rate
         val stepSize      = max( 64, (sampleRate * 0.0116 + 0.5).toInt ) // 11.6ms spacing
         val sonaSpec      = SonagramSpec( sampleRate, 32, min( 16384, sampleRate / 2 ).toFloat, 24,
                                (stepSize / sampleRate * 1000).toFloat, 4096, stepSize )
         val decim         = List( 1, 6, 6, 6, 6 )
         val fileSpec      = new SonagramFileSpec( sonaSpec, afDescr.file.lastModified, afDescr.file,
                             afDescr.length, afDescr.channels, sampleRate, decim )
         val cachePath     = fileCache.createCacheFileName( path )

         // try to retrieve existing overview file from cache
         val decimAFO      = if( cachePath.isFile ) {
            try {
               val cacheAF    = AudioFile.openAsRead( cachePath )
               try {
                  cacheAF.readAppCode()
                  val cacheDescr = cacheAF.getDescr
                  val blob       = cacheDescr.getProperty( AudioFileDescr.KEY_APPCODE ).asInstanceOf[ Array[ Byte ]]
                  if( (cacheDescr.appCode == APPCODE) && (blob != null) && (SonagramFileSpec.decode( blob ) == Some( fileSpec ))
                      && (cacheDescr.length == fileSpec.expectedDecimNumFrames) ) {
                     af.cleanUp // do not need it anymore for reading
                     fileSpec.makeAllAvailable
                     Some( cacheAF )
                  } else {
                     cacheAF.cleanUp
                     None
                  }
               }
               catch { case e: IOException => { cacheAF.cleanUp; None }}
            }
            catch { case e: IOException => { None }}
         } else None

         // on failure, create new cache file
         val decimAF = decimAFO getOrElse {
            val d             = new AudioFileDescr()
            d.file            = cachePath
            d.`type`          = AudioFileDescr.TYPE_AIFF
            d.channels        = afDescr.channels
            d.rate            = afDescr.rate
            d.bitsPerSample   = 32  // XXX really?
            d.sampleFormat    = AudioFileDescr.FORMAT_FLOAT
            d.appCode         = APPCODE
            d.setProperty( AudioFileDescr.KEY_APPCODE, fileSpec.encode )
            AudioFile.openAsWrite( d ) // XXX eventually should use shared buffer!!
         }

         val so = new SonagramOverview( fileSpec, decimAF )
         // render overview if necessary
         if( decimAFO.isEmpty ) queue( so )
         so
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
         try {
            sona.render( this )
         }
         catch { case e => e.printStackTrace }
      }
   }

   private lazy val log10 = new FastLog( 10, 11 )

   private case class SonagramImageSpec( numChannels: Int, dim: Dimension )
}

class SonagramOverview @throws( classOf[ IOException ]) private (
   fileSpec: SonagramFileSpec, decimAF: AudioFile ) {

   import SonagramOverview._

   private val sync              = new AnyRef
   private val numChannels       = fileSpec.numChannels
   private val numKernels        = fileSpec.sona.numKernels
   private val imgSpec           = SonagramImageSpec( numChannels, new Dimension( 128, numKernels ))
   private val sonaImg           = allocateSonaImage( imgSpec )
   private val imgData           = sonaImg.img.getRaster.getDataBuffer.asInstanceOf[ DataBufferInt ].getData()

   // caller must have sync
   private def seekWindow( decim: SonagramDecimSpec, idx: Long ) {
      val framePos = idx * numKernels + decim.offset
      if( /* (decim.windowsReady > 0L) && */ (decimAF.getFramePosition != framePos) ) {
         decimAF.seekFrame( framePos )
      }
   }

//   val rnd = new java.util.Random()
   def paint( spanStart: Double, spanStop: Double, g2: Graphics2D, tx: Int,
              ty: Int, width: Int, height: Int,
              ctrl: SonagramPaintController ) {
      val idealDecim    = ((spanStop - spanStart) / width).toFloat
      val in            = fileSpec.getBestDecim( idealDecim )
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
      val iOffStart     = (numVFull - 1) * imgW
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
         sync.synchronized {
            if( in.windowsReady <= start ) return  // or draw busy-area
            seekWindow( in, start )
            val numWindows = min( in.windowsReady, stop ) - start
            while( windowsRead < numWindows ) {
               val chunkLen2 = min( imgW - xReset, numWindows - windowsRead ).toInt
               val chunkLen = chunkLen2 + xReset
               decimAF.readFrames( sonaImg.fileBuf, 0, chunkLen2 * numKernels )
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
                     iOff = iOffStart + x
                     v = 0; while( v < numVFull ) {
                        sum = fBuf( fOff )
                        i = 0; while( i < vDecim ) {
                           sum += fBuf( fOff )
                           fOff += 1; i += 1
                        }
                        val amp = ctrl.adjustGain( sum / vDecim, (iOff + xOff) / scaleW )
                        iBuf( iOff ) = c( max( 0, min( 1072,
                           ((l10.calc( max( 1.0e-9f, amp )) + pixOff) * pixScale).toInt )))
                        v += 1; iOff -= imgW
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
      val constQ = allocateConstQ( fileSpec.sona )
      val fftSize = constQ.getFFTSize
      val t1 = System.currentTimeMillis
      try {
         val af = AudioFile.openAsRead( fileSpec.audioPath )
         try {
            primaryRender( ws, constQ, af )
         }
         finally {
            af.cleanUp
         }
      }
      finally {
         releaseConstQ( fileSpec.sona )
      }
      val t2 = System.currentTimeMillis
      fileSpec.decimSpecs.sliding( 2, 1 ).foreach( pair => {
         if( ws.isCancelled ) return
//         if( verbose ) println( "start " + pair.head.totalDecim )
         secondaryRender( ws, pair.head, pair.last )
//         if( verbose ) println( "finished " + pair.head.totalDecim )
      })
      decimAF.flush()
      val t3 = System.currentTimeMillis
      if( verbose ) println( "primary : secondary ratio = " + (t2 - t1).toDouble / (t3 - t2) )
   }

   private def primaryRender( ws: WorkingSonagram, constQ: ConstQ, in: AudioFile ) {
      val fftSize       = constQ.getFFTSize
      val stepSize      = fileSpec.sona.stepSize
      val inBuf         = Array.ofDim[ Float ]( numChannels, fftSize )
      val outBuf        = Array.ofDim[ Float ]( numChannels, numKernels )

      var inOff         = fftSize / 2
      var inLen         = fftSize - inOff
      val overLen       = fftSize - stepSize
      val numFrames     = fileSpec.numFrames
      var framesRead    = 0L
      val out           = fileSpec.decimSpecs.head

      { var step = 0; while( step < out.numWindows && !ws.isCancelled ) {
         val chunkLen = min( inLen, numFrames - framesRead ).toInt
         in.readFrames( inBuf, inOff, chunkLen )
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

         sync.synchronized {
            seekWindow( out, out.windowsReady )
            decimAF.writeFrames( outBuf, 0, numKernels )
            out.windowsReady += 1
         }

         { var ch = 0; while( ch < numChannels ) {
            val convBuf = inBuf( ch )
            System.arraycopy( convBuf, stepSize, convBuf, 0, overLen )
         ch += 1 }}

         if( step == 0 ) { // stupid one instance case
            inOff = overLen
            inLen = stepSize
         }
      step += 1 }}
   }

   // XXX THIS NEEDS BIGGER BUFSIZE BECAUSE NOW WE SEEK IN THE SAME FILE
   // FOR INPUT AND OUTPUT!!!
   private def secondaryRender( ws: WorkingSonagram, in: SonagramDecimSpec, out: SonagramDecimSpec ) {
      val dec           = out.decimFactor
      val bufSize       = dec * numKernels
      val buf           = Array.ofDim[ Float ]( numChannels, bufSize )
      // since dec is supposed to be even, this
      // lands on the beginning of a kernel:
      var inOff         = bufSize / 2
      var inLen         = bufSize - inOff
      var windowsRead   = 0L

      { var step = 0; while( step < out.numWindows && !ws.isCancelled ) {
         val chunkLen = min( inLen, (in.numWindows - windowsRead) * numKernels ).toInt
         sync.synchronized {
            seekWindow( in, windowsRead )
            decimAF.readFrames( buf, inOff, chunkLen )
         }
         windowsRead += chunkLen / numKernels
         if( chunkLen < inLen ) {
            { var ch = 0; while( ch < numChannels ) {
               Arrays.fill( buf( ch ), inOff + chunkLen, bufSize, 0f )
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

         sync.synchronized {
            seekWindow( out, out.windowsReady )
            decimAF.writeFrames( buf, 0, numKernels )
            out.windowsReady += 1
         }

         if( step == 0 ) { // stupid one instance case
            inOff = 0
            inLen = bufSize
         }
      step += 1 }}
   }

   private var disposed = false
   def dispose {
      if( !disposed ) {
         disposed = true
         releaseSonaImage( imgSpec )
         decimAF.cleanUp()  // XXX delete?
      }
   }
}
