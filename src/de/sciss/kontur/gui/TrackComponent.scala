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
import java.awt.event.{ MouseAdapter, MouseEvent }
import java.beans.{ PropertyChangeListener, PropertyChangeEvent }
import java.io.{ File, IOException }
import java.nio.{ CharBuffer }
import javax.swing.{ JComponent, Spring, SpringLayout, TransferHandler }
import javax.swing.event.{ MouseInputAdapter }
import scala.math._
import de.sciss.kontur.session.{ AudioFileElement, AudioRegion, AudioTrack,
                                Region, ResizableStake, Session, SlidableStake,
                                Stake, Track, Trail }
import de.sciss.app.{ AbstractApplication, DynamicAncestorAdapter,
                     DynamicListening, GraphicsHandler }
import de.sciss.io.{ AudioFile, Span }

//import Track.Tr

class DefaultTrackComponent( doc: Session, protected val track: Track, trackList: TrackList,
                             timelineView: TimelineView )
extends JComponent with TrackToolsListener with DynamicListening {

//    protected val track     = t // necessary for trail.trail (DO NOT ASK WHY)
    protected val trail     = track.trail // "stable"
    protected lazy val trackListElement = trackList.getElement( track ).get
    protected lazy val trailView = trackListElement.trailView.asInstanceOf[ TrailView[ track.T ]]
    protected lazy val trailViewEditor = trailView.editor
/*
    protected val p_rect    = new Rectangle()
    protected var p_off     = -timelineView.timeline.span.start
    protected var p_scale   = getWidth.toDouble / timelineView.timeline.span.getLength
*/
    protected var trackTools: Option[ TrackTools ] = None

    private var painter: Painter = DefaultPainter

//    // finally we use some powerful functional shit. coooool
//    protected val isSelected: track.T /* Stake[ _ ]*/ => Boolean =
//        (trailView.map( _.isSelected _ ) getOrElse (_ => false))

    private def checkSpanRepaint( span: Span ) {
        if( span.overlaps( timelineView.span )) {
            repaint( span )
        }
    }

    private val trailViewListener = (msg: AnyRef) => msg match {
//        case TrailView.SelectionChanged( span, stakes @ _* ) => checkSpanRepaint( span )
        case TrailView.SelectionChanged( span ) => checkSpanRepaint( span )
    }

    private val trailListener = (msg: AnyRef) => msg match {
        case trail.StakesAdded( span, stakes @ _* ) => checkSpanRepaint( span )
        case trail.StakesRemoved( span, stakes @ _* ) => checkSpanRepaint( span )
    }

    private val mia = new MouseAdapter {
      override def mousePressed( e: MouseEvent ) {
          trackTools.foreach( tt => {
              val pos    = screenToVirtual( e.getX )
              val span   = new Span( pos, pos + 1 )
              val stakes = trail.getRange( span )
              val stakeO = stakes.headOption
              tt.currentTool.handleSelect( e, trackListElement, pos, stakeO )
          })
      }
   }

   private def startToolOnSelectedStakes : Span = {
      val (start, stop) = trailView.selectedStakes.foldLeft(
         (Long.MaxValue, Long.MinValue) )( (tup, stake) =>
            (min( tup._1, stake.span.start ), max( tup._2, stake.span.stop)) )
      if( start < stop ) {
         new Span( start, stop )
      } else new Span()
   }

   private val toolListener = (msg: AnyRef) => msg match {
        case TrackMoveTool.DragBegin( move ) => {
            val union = startToolOnSelectedStakes
            if( !union.isEmpty ) {
               val mrp = new MoveResizePainter( union )
               painter = mrp
               mrp.adjustMove( move.deltaTime, move.deltaVertical )
//               println( "DragBegin" )
//               drag.callRepaint
            }
        }
   }

   protected def participatesInTool( t: TrackTool ) = t match {
      case _ : TrackMoveTool => true
      case _ => false
   }

   private val trackToolsListener = (msg: AnyRef) => msg match {
       case TrackTools.ToolChanged( oldTool, newTool ) => {
            oldTool.removeListener( toolListener )
            if( participatesInTool( newTool )) newTool.addListener( toolListener )
       }
   }
   
    {
      setFont( AbstractApplication.getApplication().getGraphicsHandler()
        .getFont( GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI ))

// WARNING: would destroy laziness
//       if( trailViewEditor.isDefined ) {
//           addMouseListener( mia )
//           addMouseMotionListener( mia )
//       }

       new DynamicAncestorAdapter( this ).addTo( this )
    }

    def registerTools( tools: TrackTools ) {
        trackTools = Some( tools )
        tools.addListener( trackToolsListener )
        trackToolsListener( TrackTools.ToolChanged( tools.currentTool,
            tools.currentTool ))
    }

    def startListening {
       trailView.addListener( trailViewListener )
       trail.addListener( trailListener )
       if( trailViewEditor.isDefined ) {
           addMouseListener( mia )
//           addMouseMotionListener( mia )
       }
    }

    def stopListening {
       removeMouseListener( mia )
//       removeMouseMotionListener( mia )
       trail.removeListener( trailListener )
       trailView.removeListener( trailViewListener )
    }

   protected def screenToVirtual( x: Int ) : Long = {
      val width   = getWidth
      if( width == 0 ) return 0L
      val tlSpan  = timelineView.timeline.span
      val scale   = tlSpan.getLength / width.toDouble
      (x.toLong * scale + tlSpan.start + 0.5).toLong
   }

   protected def virtualToScreen( pos: Long ) : Int = {
      val tlSpan  = timelineView.timeline.span
      val tlLen   = tlSpan.getLength
      if( tlLen == 0L ) return 0
      val scale   = getWidth.toDouble / tlLen
      ((pos - tlSpan.start) * scale + 0.5).toInt
   }

   protected def repaint( span: Span, outcode: Int = 2 ) {
        val x1 = virtualToScreen( span.start )
        val x2 = virtualToScreen( span.stop )
        val r  = new Rectangle( x1 - outcode, 0, x2 - x1 + outcode + outcode, getHeight )
        repaint( r )
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

    override def paintComponent( g: Graphics ) {
        super.paintComponent( g )

        val g2 = g.asInstanceOf[ Graphics2D ]
        val pc = PaintContext( g2, -timelineView.timeline.span.start,
                               getWidth.toDouble / timelineView.timeline.span.getLength,
                               getHeight, timelineView.span )

        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
        painter.paint( pc )
    }

   protected case class PaintContext( g2: Graphics2D, p_off: Long,
                                      p_scale: Double, height: Int,
                                      viewSpan: Span ) {

      def virtualToScreen( pos: Long ) =
        ((pos + p_off) * p_scale + 0.5).toInt
   }

   protected trait Painter {
      def paint( pc: PaintContext ) : Unit
   }

   protected trait DefaultPainter extends Painter {
      def paintStake( pc: PaintContext )( stake: track.T ) {
         val x = pc.virtualToScreen( stake.span.start )
         val width = ((stake.span.stop + pc.p_off) * pc.p_scale + 0.5).toInt - x
         val g2 = pc.g2
         g2.setColor( if( trailView.isSelected( stake )) Color.blue else Color.black )
         g2.fillRoundRect( x, 0, width, pc.height, 5, 5 )
         stake match {
            case reg: Region => {
               val clipOrig = g2.getClip
               g2.clipRect( x + 2, 2, width - 4, pc.height - 4 )
               g2.setColor( Color.white )
               g2.drawString( reg.name, x + 4, 12 )
               g2.setClip( clipOrig )
            }
            case _ =>
         }
      }

      def paint( pc: PaintContext ) {
         trail.visitRange( pc.viewSpan )( paintStake( pc ) _ )
      }
   }

   protected object DefaultPainter extends DefaultPainter

   protected class MoveResizePainter( union: Span )
   extends DefaultPainter {
      private var lastDraggedUnion = union
      private var move           = 0L
      private var moveOuter      = 0L
      private var moveInner      = 0L
      private var moveStart      = 0L
      private var moveStop       = 0L
      private var moveVertical   = 0

      def adjustMove( newMove: Long, newMoveVertical: Int ) {
         move           = newMove
         moveVertical   = newMoveVertical
         adjusted
      }

      private def adjusted {
         val newDraggedUnion = new Span( union.start + move + moveOuter + moveStart,
                                         union.stop + move + moveOuter + moveStop )
         val repaintSpan = newDraggedUnion.union( lastDraggedUnion )
         lastDraggedUnion  = newDraggedUnion
         checkSpanRepaint( repaintSpan )
      }

      override def paint( pc: PaintContext ) {
         val tlSpan = timelineView.timeline.span
         val ps = paintStake( pc ) _
         trail.visitRange( pc.viewSpan )( stake => {
            val tStake = if( trailView.isSelected( stake )) {
               if( move != 0L ) {
                  stake.move( move )
               } else if( moveOuter != 0L ) {
                  stake match {
                     case sStake: SlidableStake[ _ ] => sStake.moveOuter( moveOuter )
                     case _ => stake
                  }
               } else if( moveInner != 0L ) {
                  stake match {
                     case sStake: SlidableStake[ _ ] => sStake.moveInner( moveInner )
                     case _ => stake
                  }
               } else if( moveStart != 0L ) {
                  stake match {
                     case rStake: ResizableStake[ _ ] => rStake.moveStart( moveStart )
                     case _ => stake
                  }
               } else if( moveStop != 0L ) {
                  stake match {
                     case rStake: ResizableStake[ _ ] => rStake.moveStop( moveStop )
                     case _ => stake
                  }
               } else {
                  stake
               }
            } else stake

            ps( stake )
         })
      }
   }
}

object AudioTrackComponent {
   protected val colrDropRegionBg = new Color( 0x00, 0x00, 0x00, 0x7F )
   protected val colrDropRegionFg = new Color( 0xFF, 0xFF, 0xFF, 0x7F )
}

class AudioTrackComponent( doc: Session, audioTrack: AudioTrack, trackList: TrackList,
                           timelineView: TimelineView )
extends DefaultTrackComponent( doc, audioTrack, trackList, timelineView ) {
    import AudioTrackComponent._

    private var dropPos : Option[ Long ] = None

    // ---- constructor ----
    {
        new DropTarget( this, DnDConstants.ACTION_COPY, new DropTargetAdapter {
           override def dragEnter( dtde: DropTargetDragEvent ) {
              process( dtde )
           }

           override def dragOver( dtde: DropTargetDragEvent ) {
              process( dtde )
           }

           override def dragExit( dte: DropTargetEvent ) {
              dropPos.foreach( pos => {
                 dropPos = None
                 repaint( new Span( pos, pos ))
              })
           }

           private def process( dtde: DropTargetDragEvent ) {
              val newLoc = if( dtde.isDataFlavorSupported( DataFlavor.stringFlavor )) {
                  dtde.acceptDrag( DnDConstants.ACTION_COPY )
                  Some( screenToVirtual( dtde.getLocation.x ))
              } else {
                  dtde.rejectDrag()
                  None
              }
              if( newLoc != dropPos ) {
                  val dirtySpan = if( dropPos.isDefined ) {
                     val pos1 = dropPos.get
                     val pos2 = newLoc getOrElse pos1
                     new Span( min( pos1, pos2 ), max( pos1, pos2 ))
                  } else {
                     val pos1 = newLoc.get
                     new Span( pos1, pos1 )
                  }
                  dropPos = newLoc
                  repaint( dirtySpan )
              }
           }

           def drop( dtde: DropTargetDropEvent ) {
              dropPos.foreach( pos => {
                 dropPos = None
                 repaint( new Span( pos, pos ))
              })
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
                          screenToVirtual( dtde.getLocation().x )))
                      pasteExtern( path, span, insPos )
                   }
                   catch { case e1: NumberFormatException => }
                 }
                 dtde.dropComplete( true )
             } else {
                 dtde.rejectDrop()
             }
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
                   val afeNew = AudioFileElement.fromPath( doc, path )
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

      override def paintComponent( g: Graphics ) {
/*
       audioTrack.trail.visitRange( span )( ar => {
          p_rect.x     = ((ar.span.start + p_off) * p_scale + 0.5).toInt
          p_rect.width = ((ar.span.stop + p_off) * p_scale + 0.5).toInt - p_rect.x
//          g2.setColor( Color.black )
          g2.setColor( if( trailView.isSelected( ar.asInstanceOf[ track.T ] )) Color.blue else Color.black )
          g2.fillRoundRect( p_rect.x, 0, p_rect.width, p_rect.height, 5, 5 )
          val clipOrig = g2.getClip
          g2.clipRect( p_rect.x + 2, 2, p_rect.width - 4, p_rect.height - 4 )
          g2.setColor( Color.white )
          g2.drawString( ar.name, p_rect.x + 4, 12 )
          g2.setClip( clipOrig )
        })
*/
        super.paintComponent( g )
        dropPos.foreach( loc => {
             val x = virtualToScreen( loc ) - 1 // loc.getDropPoint().x
             g.setColor( colrDropRegionBg )
             g.fillRect( x, 0, 3, getHeight )
        })
    }
}