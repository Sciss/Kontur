/*
 *  TrackComponent.scala
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

package de.sciss.kontur.gui

import java.awt.{ Color, Dimension, Graphics, Graphics2D, Point, Rectangle,
                 RenderingHints }
import java.awt.datatransfer.{ DataFlavor, Transferable }
import java.awt.dnd.{ DnDConstants, DropTarget, DropTargetAdapter,
                     DropTargetDragEvent, DropTargetDropEvent, DropTargetEvent,
                     DropTargetListener }
import java.beans.{ PropertyChangeListener, PropertyChangeEvent }
import java.io.{ File, IOException }
import java.nio.{ CharBuffer }
import javax.swing.{ JComponent, Spring, SpringLayout, TransferHandler }
import scala.math._
import de.sciss.kontur.session.{ AudioFileElement, AudioRegion, AudioTrack,
                                Region, Session, Stake, Track }
import de.sciss.app.{ AbstractApplication, GraphicsHandler }
import de.sciss.io.{ AudioFile, Span }

class DefaultTrackComponent( doc: Session, t: Track, tracksView: TracksView, timelineView: TimelineView )
extends JComponent {

    {
//        val rnd = new java.util.Random()
//        setBackground( Color.getHSBColor( rnd.nextFloat(), 0.5f, 1f ))
/*        val dim = getPreferredSize()
        dim.height = 64 // XXX
        setPreferredSize( dim )
        val dim2 = getPreferredSize()
        dim2.height = 64 // XXX
        setMinimumSize( dim2 )
        val dim3 = getPreferredSize()
        dim3.height = 64 // XXX
        setMaximumSize( dim3 ) */

//        val lay = new SpringLayout()
//        val cons = lay.getConstraints( this )
//        cons.setWidth( Spring.constant( 64 ))
//        cons.setHeight( Spring.constant( 64 ))  // XXX
      setFont( AbstractApplication.getApplication().getGraphicsHandler()
        .getFont( GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI ))
    }

    override def getPreferredSize() : Dimension = {
       val dim = super.getPreferredSize()
       dim.height = 64
       dim
    }

    override def getMinimumSize() : Dimension = {
       val dim = super.getMinimumSize()
       dim.height = 64
       dim
    }

    override def getMaximumSize() : Dimension = {
       val dim = super.getMaximumSize()
       dim.height = 64
       dim
    }

    protected val p_rect  = new Rectangle()
    protected var p_off   = 0L
    protected var p_scale = 0.0

    override def paintComponent( g: Graphics ) {
        super.paintComponent( g )

        val g2 = g.asInstanceOf[ Graphics2D ]
//        val clipOrig = g2.getClip

       getBounds( p_rect )

//g2.setColor( Color.yellow )
//g2.fillRect( 1, 1, p_rect.width - 2, p_rect.height - 2 )

       p_off    = -timelineView.timeline.span.start
       p_scale  = getWidth.toDouble / timelineView.timeline.span.getLength
       g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
       paintSpan( g2, timelineView.span )


//        g2.setClip( clipOrig )
    }

    protected def paintSpan( g2: Graphics2D, span: Span ) {
        t.trail.visitRange( span )( stake => {
          p_rect.x     = ((stake.span.start + p_off) * p_scale + 0.5).toInt
          p_rect.width = ((stake.span.stop + p_off) * p_scale + 0.5).toInt - p_rect.x
          g2.setColor( Color.black )
          g2.fillRect( p_rect.x, 0, p_rect.width, p_rect.height )
          val clipOrig = g2.getClip
          g2.clipRect( p_rect.x, 0, p_rect.width, p_rect.height )
          g2.setColor( Color.white )
          stake match {
            case reg: Region => g2.drawString( reg.name, p_rect.x + 4, 12 )
            case _ =>
          }
          g2.setClip( clipOrig )
        })
    }
}

object AudioTrackComponent {
   protected val colrDropRegionBg = new Color( 0x00, 0x00, 0x00, 0x7F )
   protected val colrDropRegionFg = new Color( 0xFF, 0xFF, 0xFF, 0x7F )
}

class AudioTrackComponent( doc: Session, audioTrack: AudioTrack, tracksView: TracksView, timelineView: TimelineView )
extends DefaultTrackComponent( doc, audioTrack, tracksView, timelineView ) {
    import AudioTrackComponent._

//    private var stringDragEntry: Option[ StringDragEntry ] = None
//    private var dropLoc : Option[ TransferHandler.DropLocation ] = None
    private var dropLoc : Option[ Point ] = None
//    private var dropT : Option[ Transferable ] = None

/*
    private val th = new TransferHandler {
          override def canImport( c: JComponent, fs: Array[ DataFlavor ]) =
            fs.exists( _ == DataFlavor.stringFlavor )

          override def canImport( sup: TransferHandler.TransferSupport ) : Boolean = {
             val t = sup.getTransferable()
             if( t.isDataFlavorSupported( DataFlavor.stringFlavor )) {
                println( "supported" )
                dropLoc = Some( sup.getDropLocation )
//                dropT   = Some( sup.getTransferable )
//                val o = t.getTransferData( DataFlavor.stringFlavor )
//                println( "dragging '" + o + "'" )
                repaint()
                true
             } else
               if( dropLoc != None ) {
                   dropLoc = None
//                   dropT   = None
                   repaint()
               }
               false
             }

        	override def importData( c: JComponent, t: Transferable ) : Boolean = {
//println ("IMPORT")
               dropLoc = None
//               dropT = None
        		try {
                    if( t.isDataFlavorSupported( DataFlavor.stringFlavor )) {
        				val o = t.getTransferData( DataFlavor.stringFlavor )
                        println( "imported '" + o + "'" )
                        true
                    } else {
                        false
                    }
                }
                catch { case _ => false }
            }

//        	override def getTransferDataFlavors() : Array[ DataFlavor ] =
//              Array( DataFlavor.stringFlavor )
//          }
          }
*/
    // ---- constructor ----
    {
//println( "AudioTrackComponent" )
//setBackground( Color.yellow )
      
//        setTransferHandler( th )
/*
        addPropertyChangeListener( "dropLocation", new PropertyChangeListener {
            def propertyChange( pce: PropertyChangeEvent ) {
                val loc = pce.getNewValue().asInstanceOf[ TransferHandler.DropLocation ]
                dropLoc = if( loc != null ) Some( loc ) else None
                repaint()
            }
        })
*/

        new DropTarget( this, DnDConstants.ACTION_COPY, new DropTargetAdapter {
/*
           private def getStringDragEntry( t: Transferable ) {
             val textFlavor = DataFlavor.selectBestTextFlavor( t.getTransferDataFlavors )
             if( textFlavor != null ) {
println( "got flavor : " + textFlavor )
                  try {
                      val reader = textFlavor.getReaderForText( t )
println( "got reader : " + reader )
                      val cb = CharBuffer.allocate( 256 )
                      reader.read( cb )
                      cb.flip()
                       val str = cb.toString()
println( "string is '" + str + "'" )
                         val arr = str.split( ':' )
println( "--- 1 " + arr.toList + "; " + arr.size )
                         if( arr.size == 3 ) {
println( "--- 2" )
                           stringDragEntry = Some( new StringDragEntry( new File( arr(0) ),
                                        new Span( arr( 1 ).toLong, arr( 2 ).toLong )))
println( "--- 3" )
                           return
                         }
                  }
                  catch { case _ => }
              }
              if( stringDragEntry != None ) {
                  stringDragEntry = None
                  repaint() // XXX dirty region
              }
           }
*/
           override def dragEnter( dtde: DropTargetDragEvent ) {
              process( dtde )
           }

           override def dragOver( dtde: DropTargetDragEvent ) {
              process( dtde )
           }

           override def dragExit( dte: DropTargetEvent ) {
              if( dropLoc.isDefined ) {
                 dropLoc = None
                 repaint()
              }
           }

           private def process( dtde: DropTargetDragEvent ) {
              val newLoc = if( dtde.isDataFlavorSupported( DataFlavor.stringFlavor )) {
                  dtde.acceptDrag( DnDConstants.ACTION_COPY )
                  Some( dtde.getLocation )
              } else {
                  dtde.rejectDrag()
                  None
              }
              if( newLoc != dropLoc ) {
                  dropLoc = newLoc
                  repaint() // XXX dirty region
              }
           }

           def drop( dtde: DropTargetDropEvent ) {
              dropLoc = None
              if( dtde.isDataFlavorSupported( DataFlavor.stringFlavor )) {
                 dtde.acceptDrop( DnDConstants.ACTION_COPY )
                 val str = dtde.getTransferable().getTransferData( DataFlavor.stringFlavor ).toString()
                 val arr = str.split( ':' )
                 if( arr.size == 3 ) {
                   try {
                      val path   = new File( arr( 0 ))
                      val span   = new Span( arr( 1 ).toLong, arr( 2 ).toLong )
                      val tlSpan = timelineView.timeline.span
                      val insPos = max( tlSpan.start, min( tlSpan.stop,
                          (dtde.getLocation().x.toLong / p_scale - p_off + 0.5).toLong ))
                      pasteExtern( path, span, insPos )
                   }
                   catch { case e1: NumberFormatException => }
                 }
                 dtde.dropComplete( true )
             } else {
                 dtde.rejectDrop()
             }
             repaint() // XXX dirty region
           }
        })
      }

      private def pasteExtern( path: File, fileSpan: Span, insPos: Long ) {
         try {
            val af = AudioFile.openAsRead( path )
            af.close()
            val descr = af.getDescr()

            doc.audioFiles.editor.foreach( aed => {
                var afe = doc.audioFiles.find( afe => afe.path == path ) getOrElse {
                   val afeNew = new AudioFileElement( doc.createID, path )
                   val ce = aed.editBegin( "editAddAudioFile" )
                   aed.editInsert( ce, doc.audioFiles.size, afeNew )
                   aed.editEnd( ce )
                   afeNew
                }

                audioTrack.trail.editor.foreach( ted => {
                     val insSpan = new Span( insPos,
                                min( insPos + fileSpan.getLength,
                                     timelineView.timeline.span.stop ))
                      if( !insSpan.isEmpty ) {
                         val ar = new AudioRegion( insSpan, afe.name, afe, fileSpan.start )
                         val ce2 = ted.editBegin( "editAddAudioRegion" )
                         ted.editAdd( ce2, ar )
                         ted.editEnd( ce2 )
                      }
                 })
              })
         }
         catch { case e: IOException => e.printStackTrace() }
      }

      override protected def paintSpan( g2: Graphics2D, span: Span ) {
//        var i = 0
//println( "paint " + dropLoc )
//dropT.foreach( t => {
//    try {
//      val o = t.getTransferData( DataFlavor.stringFlavor )
//      println( "----> '" + o + "'" )
//    }
//    catch { case e => e.printStackTrace() }
//})

        audioTrack.trail.visitRange( span )( ar => {
//println( "i = " + i )
//i+=1
          p_rect.x     = ((ar.span.start + p_off) * p_scale + 0.5).toInt
          p_rect.width = ((ar.span.stop + p_off) * p_scale + 0.5).toInt - p_rect.x
          g2.setColor( Color.black )
          g2.fillRoundRect( p_rect.x, 0, p_rect.width, p_rect.height, 5, 5 )
          val clipOrig = g2.getClip
          g2.clipRect( p_rect.x + 2, 2, p_rect.width - 4, p_rect.height - 4 )
          g2.setColor( Color.white )
          g2.drawString( ar.name, p_rect.x + 4, 12 )
          g2.setClip( clipOrig )
        })

/*
         // dnd
         stringDragEntry.foreach( entry => {
             p_rect.x     = entry.pt.x
             p_rect.width = ((entry.span.getLength + p_off) * p_scale + 0.5).toInt - p_rect.x
             g2.setColor( colrDropRegionBg )
             g2.fillRoundRect( p_rect.x, p_rect.y, p_rect.width, p_rect.height, 5, 5 )
             val clipOrig = g2.getClip
             g2.clipRect( p_rect.x + 2, p_rect.y + 2, p_rect.width - 4, p_rect.height - 4 )
             g2.setColor( colrDropRegionFg )
             g2.drawString( entry.file.getName(), p_rect.x + 4, p_rect.y + 12 )
             g2.setClip( clipOrig )
         })
*/
        dropLoc.foreach( loc => {
             p_rect.x     = loc.x - 1 // loc.getDropPoint().x
             p_rect.width = 3
             g2.setColor( colrDropRegionBg )
             g2.fillRect( p_rect.x, 0, p_rect.width, p_rect.height )
        })
    }

//    private class StringDragEntry( val file: File, val span: Span ) {
//        var pt = new Point() // XXX should translate directly to timeline position
//    }
}