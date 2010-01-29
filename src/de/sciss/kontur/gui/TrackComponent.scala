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
 *    26-Jan-10   fiddled around with scalac crashes
 */

package de.sciss.kontur.gui

import java.awt.{ Color, Dimension, Graphics, Graphics2D, Point, Rectangle,
                 RenderingHints, TexturePaint }
import java.awt.datatransfer.{ DataFlavor, Transferable }
import java.awt.dnd.{ DnDConstants, DropTarget, DropTargetAdapter,
                     DropTargetDragEvent, DropTargetDropEvent, DropTargetEvent,
                     DropTargetListener }
import java.awt.event.{ MouseAdapter, MouseEvent }
import java.awt.geom.{ Path2D }
import java.awt.image.{ BufferedImage, ImageObserver }
import java.beans.{ PropertyChangeListener, PropertyChangeEvent }
import java.io.{ File, IOException }
import java.nio.{ CharBuffer }
import javax.swing.{ JComponent, Spring, SpringLayout, TransferHandler }
import javax.swing.event.{ MouseInputAdapter }
import scala.collection.{ IterableLike }
import scala.math._
import de.sciss.kontur.io.{ SonagramPaintController }
import de.sciss.kontur.session.{ AudioFileElement, AudioRegion, AudioTrack,
                                BasicTrail, FadeSpec, Region, RegionTrait,
                                ResizableStake, Session, SlidableStake, Stake,
                                Track, Trail }
import de.sciss.app.{ AbstractApplication, AbstractCompoundEdit,
                      DynamicAncestorAdapter,DynamicListening, GraphicsHandler }
import de.sciss.io.{ AudioFile, Span }

//import Track.Tr

object DefaultTrackComponent {
   protected[gui] case class PaintContext( g2: Graphics2D, p_off: Long,
                                   p_scale: Double, height: Int,
                                   viewSpan: Span, clip: Rectangle ) {

      def virtualToScreen( pos: Long ) = ((pos + p_off) * p_scale + 0.5).toInt
      def virtualToScreenD( pos: Long )= (pos + p_off) * p_scale
      def screenToVirtual( loc: Int )  = (loc / p_scale - p_off + 0.5).toLong
      def screenToVirtualD( loc: Int ) = loc / p_scale - p_off
   }

   protected[gui] trait Painter {
      def paint( pc: PaintContext ) : Unit
   }

   val MIN_SPAN_LEN = 64  // XXX somewhat arbitrary (one control block in default scsynth)
   val colrBg     = new Color( 0x68, 0x68, 0x68 )
   val colrBgSel  = Color.blue
}

class DefaultTrackComponent( doc: Session, protected val track: Track, trackList: TrackList,
                             timelineView: TimelineView )
extends JComponent with TrackToolsListener with DynamicListening {

      import DefaultTrackComponent._

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

    protected var painter: Painter = createDefaultPainter

//    // finally we use some powerful functional shit. coooool
//    protected val isSelected: track.T /* Stake[ _ ]*/ => Boolean =
//        (trailView.map( _.isSelected _ ) getOrElse (_ => false))

   protected def checkSpanRepaint( span: Span, outcode: Int = 2, tm: Long = 0L ) {
      if( span.overlaps( timelineView.span )) {
         repaint( span, outcode, tm )
      }
   }

   private val trailViewListener = (msg: AnyRef) => msg match {
//    case TrailView.SelectionChanged( span, stakes @ _* ) => checkSpanRepaint( span )
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
              if( (e.getClickCount == 2) && !stakes.isEmpty ) showObserverPage
          })
      }
   }

   private val moveResizeToolListener = (msg: AnyRef) => msg match {
        case TrackStakeTool.DragBegin => {
            val union = unionSpan( trailView.selectedStakes )
            if( !union.isEmpty ) {
               painter = createMoveResizePainter( union, painter )
            }
        }
        case TrackMoveTool.Move( deltaTime, deltaVertical ) => painter match {
           case mrp: MoveResizePainter => {
                 mrp.adjustMove( deltaTime, deltaVertical )
                 mrp.adjusted
           }
           case _ =>
        }
        case TrackResizeTool.Resize( deltaStart, deltaStop ) => painter match {
           case mrp: MoveResizePainter => {
                 mrp.adjustResize( deltaStart, deltaStop )
                 mrp.adjusted
           }
           case _ =>
        }
        case TrackStakeTool.DragEnd( ce ) => painter match {
           case mrp: MoveResizePainter => mrp.finish( ce )
           case _ =>
        }
        case TrackStakeTool.DragCancel => painter match {
           case mrp: MoveResizePainter => mrp.cancel
           case _ =>
        }
   }

   private var toolListener: Option[ AnyRef => Unit ] = None

   private val trackToolsListener = (msg: AnyRef) => msg match {
      case TrackTools.ToolChanged( oldTool, newTool ) => {
         toolListener.foreach( tl => oldTool.removeListener( tl ))
         toolListener = selectToolListener( newTool )
         toolListener.foreach( tl => newTool.addListener( tl ))
      }
      case TrackTools.VisualBoostChanged( _, boost ) => {
         setVisualBoost( boost )
      }
   }
   
   protected def createDefaultPainter() = new DefaultPainterTrait {}

   protected def createMoveResizePainter( initialUnion: Span, oldPainter: Painter ) =
      new MoveResizePainter( initialUnion, oldPainter )

   protected def unionSpan( stakes: IterableLike[ Stake[ _ ], _ ]) : Span = {
      val (start, stop) = stakes.foldLeft(
         (Long.MaxValue, Long.MinValue) )( (tup, stake) =>
            (min( tup._1, stake.span.start ), max( tup._2, stake.span.stop)) )
      if( start < stop ) {
         new Span( start, stop )
      } else new Span()
   }

   private def showObserverPage {
//       val page      = StakeObserverPage.instance
//       val observer  = AbstractApplication.getApplication()
//          .getComponent( Main.COMP_OBSERVER ).asInstanceOf[ ObserverFrame ]
//       page.setObjects( trailView.selectedStakes: _* )
//       observer.selectPage( page.id )
   }

   protected def selectToolListener( t: TrackTool ): Option[ AnyRef => Unit ] =
      t match {
         case _ : TrackMoveTool   => Some( moveResizeToolListener )
         case _ : TrackResizeTool => Some( moveResizeToolListener )
         case _ => None
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

   protected def setVisualBoost( boost: Float ) {}

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

   protected def repaint( span: Span, outcode: Int = 2, tm: Long = 0L ) {
        val x1 = virtualToScreen( span.start )
        val x2 = virtualToScreen( span.stop )
//        val r  = new Rectangle( x1 - outcode, 0, x2 - x1 + outcode + outcode, getHeight )
        repaint( tm, x1 - outcode, 0, x2 - x1 + outcode + outcode, getHeight )
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

    private val clipRect = new Rectangle // avoid re-allocation
    override def paintComponent( g: Graphics ) {
        super.paintComponent( g )

        val g2 = g.asInstanceOf[ Graphics2D ]
        g2.getClipBounds( clipRect )
        val pc = PaintContext( g2, -timelineView.timeline.span.start,
                               getWidth.toDouble / timelineView.timeline.span.getLength,
                               getHeight, timelineView.span, clipRect )

        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
        painter.paint( pc )
    }

//   protected class DefaultPainter extends DefaultPainterTrait

   protected trait DefaultPainterTrait extends Painter {
      def paintStake( pc: PaintContext, stake: track.T, selected: Boolean ) {
         val x = pc.virtualToScreen( stake.span.start )
         val width = ((stake.span.stop + pc.p_off) * pc.p_scale + 0.5).toInt - x
         val g2 = pc.g2
         g2.setColor( if( selected ) colrBgSel else colrBg )
         g2.fillRoundRect( x, 0, width, pc.height, 5, 5 )
         stake match {
            case reg: RegionTrait[ _ ] => {
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
         trail.visitRange( pc.viewSpan )(
            stake => paintStake( pc, stake, trailView.isSelected( stake )))
      }
   }

   trait TransformativePainter extends DefaultPainterTrait {
//      this: Painter =>

      protected val initialUnion: Span
      protected val oldPainter: Painter
      private var lastDraggedUnion = initialUnion

      protected var dragTrail: Option[ BasicTrail[ track.T ]] = None

      def adjusted {
         val tTrail = new BasicTrail[ track.T ]( doc )
         dragTrail = Some( tTrail )
         val tStakes = trailView.selectedStakes.toList.map( transform _ )
         tTrail.add( tStakes: _* )
         val newDraggedUnion = unionSpan( tStakes )
         val repaintSpan = newDraggedUnion.union( lastDraggedUnion )
         lastDraggedUnion  = newDraggedUnion
         checkSpanRepaint( repaintSpan )
      }

      protected def transform( stake: track.T ): track.T

      def finish( ce: AbstractCompoundEdit ) {
         painter = oldPainter
         // refresh is handled through stake exchange
         dragTrail.foreach( tTrail => {
            val oldStakes = trailView.selectedStakes // .toList
            val newStakes = tTrail.getAll()
            // remove stakes that have not changed
            val oldStakesF = (oldStakes -- newStakes).toList
            val newStakesF = newStakes.filterNot( st => oldStakes.contains( st ))
            trailView.editor.foreach( ed => {
                ed.editDeselect( ce, oldStakesF: _* )
            })
            trail.editor.foreach( ed => {
                ed.editRemove( ce, oldStakesF: _* )
                ed.editAdd( ce, newStakesF: _* )
            })
            trailView.editor.foreach( ed => {
                ed.editSelect( ce, newStakesF: _* )
            })
         })
      }

      def cancel {
         val repaintSpan = lastDraggedUnion.union( initialUnion )
         painter = oldPainter
         checkSpanRepaint( repaintSpan )
      }

      override def paint( pc: PaintContext ) {
         if( dragTrail.isEmpty ) {
            super.paint( pc )
            return
         }

         val tlSpan = timelineView.timeline.span
         val vwSpan = pc.viewSpan
         trail.visitRange( vwSpan )( stake => {
            if( !trailView.isSelected( stake )) paintStake( pc, stake, false )
         })
         dragTrail.foreach( _.visitRange( vwSpan )( stake =>
            paintStake( pc, stake, true )
         ))
      }
   }

   protected class MoveResizePainter( protected val initialUnion: Span,
                                      protected val oldPainter: Painter )
   extends DefaultPainterTrait with TransformativePainter {
      private var move           = 0L
      private var moveOuter      = 0L
      private var moveInner      = 0L
      private var moveStart      = 0L
      private var moveStop       = 0L
      private var moveVertical   = 0

      def adjustMove( newMove: Long, newMoveVertical: Int ) {
         move           = newMove
         moveVertical   = newMoveVertical
      }

      def adjustResize( newMoveStart: Long, newMoveStop: Long ) {
         moveStart   = newMoveStart
         moveStop    = newMoveStop
      }

      protected def transform( stake: track.T ) : track.T = {
         val tlSpan = timelineView.timeline.span
         if( move != 0L ) {
            val m = if( move < 0)
               max( tlSpan.start - stake.span.start, move )
            else
               min( tlSpan.stop - stake.span.stop, move )
            stake.move( m )
         } else if( moveOuter != 0L ) {
            stake match {
               case sStake: SlidableStake[ _ ] => {
                  val mOuter = if( moveOuter < 0)
                     max( tlSpan.start - stake.span.start, moveOuter )
                  else
                     min( tlSpan.stop - stake.span.stop, moveOuter )
                  sStake.moveOuter( mOuter )
               }
               case _ => stake
            }
         } else if( moveInner != 0L ) {
            stake match {
               case sStake: SlidableStake[ _ ] => sStake.moveInner( moveInner )
               case _ => stake
            }
         } else if( moveStart != 0L ) {
            stake match {
               case rStake: ResizableStake[ _ ] => {
                  val mStart = if( moveStart < 0 )
                     max( tlSpan.start - stake.span.start, moveStart )
                  else
                     min( stake.span.getLength - MIN_SPAN_LEN, moveStart )

//println( "stake " + stake.span + "; moveStart " + moveStart + "; mStart " + mStart )
                  rStake.moveStart( mStart )
               }
               case _ => stake
            }
         } else if( moveStop != 0L ) {
            stake match {
               case rStake: ResizableStake[ _ ] => {
                  val mStop = if( moveStop < 0 )
                     max( -stake.span.getLength + MIN_SPAN_LEN, moveStop )
                  else
                     min( tlSpan.stop - stake.span.stop, moveStop )
                  rStake.moveStop( mStop )
               }
               case _ => stake
            }
         } else {
            stake
         }
      }
   }
}

object AudioTrackComponent {
   private val colrDropRegionBg   = new Color( 0xFF, 0xFF, 0xFF, 0x7F )
//   private val colrDropRegionFg = new Color( 0xFF, 0xFF, 0xFF, 0x7F )
	private val colrFade           = new Color( 0x05, 0xAF, 0x3A )
	private val pntFade            = {
		val img = new BufferedImage( 4, 2, BufferedImage.TYPE_INT_ARGB )
		img.setRGB( 0, 0, 4, 2, Array(
      	0xFF05AF3A, 0x00000000, 0x00000000, 0x00000000,
   		0x00000000, 0x00000000, 0xFF05AF3A, 0x00000000
      ), 0, 4 )
		new TexturePaint( img, new Rectangle( 0, 0, 4, 2 ))
   }
   private val hndlExtent        = 15
   private val hndlBaseline      = 12
}

class AudioTrackComponent( doc: Session, audioTrack: AudioTrack, trackList: TrackList,
                           timelineView: TimelineView )
extends DefaultTrackComponent( doc, audioTrack, trackList, timelineView )
with SonagramPaintController {
    component =>
    
    import AudioTrackComponent._
    import DefaultTrackComponent._

    private var dropPos : Option[ Long ] = None
    private var visualBoost = 1f

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

   // XXX eventually we could save space and let the painter
   // do the msg matching, no?
   private val gainFadeToolListener = (msg: AnyRef) => msg match {
      case TrackStakeTool.DragBegin => {
         val union = unionSpan( trailView.selectedStakes )
         if( !union.isEmpty ) {
            painter = new GainFadePainter( union, painter )
         }
      }
      case TrackGainTool.Gain( factor ) => painter match {
         case gfp: GainFadePainter => {
            gfp.adjustGain( factor )
            gfp.adjusted
         }
         case _ =>
      }
      case TrackFadeTool.Fade( inDelta, outDelta, inCurve, outCurve ) => painter match {
         case gfp: GainFadePainter => {
            gfp.adjustFade( inDelta, outDelta, inCurve, outCurve )
            gfp.adjusted
         }
         case _ =>
      }
      case TrackStakeTool.DragEnd( ce ) => painter match {
         case grp: GainFadePainter => grp.finish( ce )
         case _ =>
      }
      case TrackStakeTool.DragCancel => painter match {
         case grp: MoveResizePainter => grp.cancel
         case _ =>
      }
   }

   override def registerTools( tools: TrackTools ) {
      visualBoost = tools.visualBoost
      super.registerTools( tools )
   }

   override protected def setVisualBoost( boost: Float ) {
      visualBoost = boost
      checkSpanRepaint( timelineView.span, tm = 200L )
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
                         val ar = new AudioRegion( insSpan, afe.name, afe,
                            fileSpan.start, 1.0f, None, None )
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

   override protected def createDefaultPainter() = new AudioStakePainter {}

   override protected def createMoveResizePainter( initialUnion: Span, oldPainter: Painter ) =
      new MoveResizePainter( initialUnion, oldPainter ) with AudioStakePainter

   override protected def selectToolListener( t: TrackTool ): Option[ AnyRef => Unit ] =
      t match {
         case _ : TrackGainTool   => Some( gainFadeToolListener )
         case _ : TrackFadeTool   => Some( gainFadeToolListener )
         case _ => super.selectToolListener( t )
      }

   // ---- SonagramPaintController ----
   private var paintStakeGain = 1f // XXX dirty dirty
   def imageObserver: ImageObserver = this
   def adjustGain( amp: Float, pos: Double ) = amp * visualBoost * paintStakeGain // eventually fades here...

   protected class GainFadePainter( protected val initialUnion: Span,
                                protected val oldPainter: Painter )
   extends AudioStakePainter with TransformativePainter {
      private var dragGain       = 1f
      private var dragFdInTime   = 0L  // actually delta
      private var dragFdInCurve  = 0f  // actually delta
      private var dragFdOutTime  = 0L  // actually delta
      private var dragFdOutCurve = 0f  // actually delta

      def adjustGain( newGain: Float ) {
         dragGain = newGain
      }

      def adjustFade( newInTime: Long, newOutTime: Long, newInCurve: Float, newOutCurve: Float ) {
         dragFdInTime   = newInTime
         dragFdOutTime  = newOutTime
         dragFdInCurve  = newInCurve
         dragFdOutCurve = newOutCurve
      }

      protected def transform( stake: track.T ) = stake match {
         case ar: AudioRegion => {
            var tStake: AudioRegion = ar
            if( dragGain != 1f ) {
               tStake = tStake.replaceGain( ar.gain * dragGain )
            }
            val fadeInChange  = dragFdInTime  != 0L || dragFdInCurve != 0f
            val fadeOutChange = dragFdOutTime != 0L || dragFdOutCurve != 0f
            if( fadeInChange || fadeOutChange ) {
               var fadeInSpec  = ar.fadeIn  getOrElse FadeSpec( 0L, (1, 0f) )
               var fadeOutSpec = ar.fadeOut getOrElse FadeSpec( 0L, (1, 0f) )
               // marika, this should go somewhere, most like AudioRegion ?
               if( fadeInChange ) {
                  fadeInSpec = FadeSpec(
                     max( 0L, min( ar.span.getLength - fadeOutSpec.numFrames,
                     fadeInSpec.numFrames + dragFdInTime )), (fadeInSpec.shape._1,
                     max( -20, min( 20, fadeInSpec.shape._2 + dragFdInCurve ))))
                  tStake = tStake.replaceFadeIn( Some( fadeInSpec ))
               }
               if( fadeOutChange ) {
                  fadeOutSpec = FadeSpec(
                     max( 0L, min( ar.span.getLength - fadeInSpec.numFrames,
                     fadeOutSpec.numFrames + dragFdOutTime )), (fadeOutSpec.shape._1,
                     max( -20, min( 20, fadeOutSpec.shape._2 + dragFdOutCurve ))))
                  tStake = tStake.replaceFadeOut( Some( fadeOutSpec ))
               }
            }
            // WHHHHHHHHYYYYYYYYYYYYYYYYYYYYYYYYYYY THE CAST?
            tStake.asInstanceOf[ track.T ]
         }
         case _ => stake
      }
   }

   protected trait AudioStakePainter extends DefaultPainterTrait {
//      DefaultPainter =>
      override def paintStake( pc: PaintContext, stake: track.T, selected: Boolean ) {
         stake match {
            case ar: AudioRegion => { // man, no chance to skip this matching??
               val x = pc.virtualToScreen( ar.span.start )
//               val width = ((ar.span.stop + pc.p_off) * pc.p_scale + 0.5).toInt - x
               val width = (ar.span.getLength * pc.p_scale + 0.5).toInt
//             val x1C = max( x, pc.clip.x - 2 )
               val x1C = max( x + 1, pc.clip.x - 2 ) // + 1 for left margin
               val x2C = min( x + width, pc.clip.x + pc.clip.width + 3 )
               if( x1C < x2C ) { // skip this if we are not overlapping with clip
                  val g2 = pc.g2
                  g2.setColor( if( selected ) colrBgSel else colrBg )
                  g2.fillRoundRect( x, 0, width, pc.height, 5, 5 )

                  val clipOrig = g2.getClip
                  g2.clipRect( x + 1, hndlExtent, width - 1, pc.height - hndlExtent - 1 )
                  
                  // --- sonagram ---
                  ar.audioFile.sona.foreach( sona => {
//                  sona.paint( new Span( ar.offset, ar.offset + ar.span.getLength ),
//                    g2, x, 15, width, pc.height - 16, component )
                     val dStart = ar.offset - ar.span.start
                     val startC = max( 0.0, pc.screenToVirtualD( x1C ))
                     val stopC  = pc.screenToVirtualD( x2C )
                     paintStakeGain = ar.gain // XXX dirty muthafucka
                     sona.paint( startC + dStart, stopC + dStart, g2,
                        x1C, 15, x2C - x1C, pc.height - 16, component )
//                   g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
                  })
               
                  // --- fades ---
                  ar.fadeIn.foreach( f => {
                     val shpFill = new Path2D.Float()
                     val shpDraw = new Path2D.Float()
                     val px = (f.numFrames * pc.p_scale).toFloat
//         				if( f.shape._1 == 1 ) {
                        shpFill.moveTo( x, pc.height - 1 )
                        shpFill.lineTo( x + px, hndlExtent )
                        shpFill.lineTo( x, hndlExtent )
                        shpDraw.moveTo( x, pc.height - 1 )
                        shpDraw.lineTo( x + px, hndlExtent )
//         				}
                     g2.setPaint( pntFade )
                     g2.fill( shpFill )
                     g2.setColor( colrFade )
                     g2.draw( shpDraw )
                  })
                  ar.fadeOut.foreach( f => {
                     val shpFill = new Path2D.Float()
                     val shpDraw = new Path2D.Float()
         				val px = (f.numFrames * pc.p_scale).toFloat
//         				if( f.shape._1 == 1 ) {
                        shpFill.moveTo( x + width - 1, pc.height - 1 )
                        shpFill.lineTo( x + width - 1 - px, hndlExtent )
         					shpFill.lineTo( x + width - 1, hndlExtent )
                        shpDraw.moveTo( x + width - 1, pc.height - 1 )
                        shpDraw.lineTo( x + width - 1 - px, hndlExtent )
//         				}
                     g2.setPaint( pntFade )
                     g2.fill( shpFill )
                     g2.setColor( colrFade )
                     g2.draw( shpDraw )
                  })

                  g2.setClip( clipOrig )
                  
                  // --- label ---
                  g2.clipRect( x + 2, 2, width - 4, pc.height - 4 )
                  g2.setColor( Color.white )
                  g2.drawString( ar.name, x + 4, hndlBaseline )
                  g2.setClip( clipOrig )
               }
            }
            case _ => super.paintStake( pc, stake, selected )
         }
      }
   }
}