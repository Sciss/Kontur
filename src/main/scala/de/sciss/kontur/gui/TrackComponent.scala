/*
 *  TrackComponent.scala
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
package gui

import java.awt.{Color, Dimension, Graphics, Graphics2D, Rectangle, RenderingHints, TexturePaint}
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.{DnDConstants, DropTarget, DropTargetAdapter, DropTargetDragEvent, DropTargetDropEvent, DropTargetEvent}
import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.geom.Path2D
import java.awt.image.{BufferedImage, ImageObserver}
import java.io.{File, IOException}
import javax.swing.JComponent
import collection.IterableLike
import session.{AudioFileElement, AudioRegion, AudioTrack,
                                BasicTrail, FadeSpec, RegionTrait,
                                ResizableStake, Session, SlidableStake, Stake, Track}
import de.sciss.dsp.Util.ampdb
import de.sciss.synth.{curveShape, linShape}
import util.Model
import de.sciss.span.Span
import Span.SpanOrVoid
import legacy.AbstractCompoundEdit
import desktop.impl.DynamicComponentImpl
import de.sciss.sonogram

object DefaultTrackComponent {
  protected[gui] case class PaintContext(g2: Graphics2D, x: Int, y: Int, p_off: Long, p_scale: Double, height: Int,
                                         viewSpan: Span, clip: Rectangle) {

    def virtualToScreen (pos: Long) = ((pos + p_off) * p_scale + 0.5).toInt + x
    def virtualToScreenD(pos: Long) =  (pos + p_off) * p_scale + x

    def screenToVirtual (loc: Int) = ((loc - x) / p_scale - p_off + 0.5).toLong
    def screenToVirtualD(loc: Int) =  (loc - x) / p_scale - p_off
  }

  protected[gui] trait Painter {
    def paint(pc: PaintContext): Unit
  }

  val MIN_SPAN_LEN    = 64  // XXX somewhat arbitrary (one control block in default scsynth)
  val colrBg          = new Color(0x68, 0x68, 0x68)
  val colrBgSel       = Color.blue

  val hndlExtent      = 15
  val hndlBaseline    = 12

  var forceFullPaint  = false
}

trait TrackComponent {
  def track: Track
  def paintTrack(g2: Graphics2D, x: Int, y: Int, width: Int, height: Int, span: Span): Unit
}

class DefaultTrackComponent(doc: Session, val track: Track, trackList: TrackList, timelineView: TimelineView)
  extends JComponent with TrackComponent with TrackToolsListener with DynamicComponentImpl {

  import DefaultTrackComponent._

  protected def dynamicComponent = this

  //    protected val track     = t // necessary for trail.trail (DO NOT ASK WHY)
  protected val trail = track.trail
  // "stable"
  protected lazy val trackListElement = trackList.getElement(track).get
  protected lazy val trailView        = trackListElement.trailView.asInstanceOf[TrailView[track.T]]
  protected lazy val trailViewEditor  = trailView.editor
  /*
      protected val p_rect    = new Rectangle()
      protected var p_off     = -timelineView.timeline.span.start
      protected var p_scale   = getWidth.toDouble / timelineView.timeline.span.getLength
  */
  protected var trackTools: Option[TrackTools] = None

  protected var painter: Painter = createDefaultPainter

  private var visualBoostVar = 1f
  private var fadeViewModeVar: FadeViewMode = FadeViewMode.Curve
  private var stakeBorderViewModeVar: StakeBorderViewMode = StakeBorderViewMode.TitledBox

  //    // finally we use some powerful functional shit. coooool
  //    protected val isSelected: track.T /* Stake[ _ ]*/ => Boolean =
  //        (trailView.map( _.isSelected _ ) getOrElse (_ => false))

  protected def checkSpanRepaint(span: Span, outcode: Int = 2, tm: Long = 0L) {
    if (span.overlaps(timelineView.span)) {
      repaint(span, outcode, tm)
    }
  }

  private val trailViewListener: Model.Listener = {
    //    case TrailView.SelectionChanged( span, stakes @ _* ) => checkSpanRepaint( span )
    case TrailView.SelectionChanged(span) => checkSpanRepaint(span)
  }

  private val trailListener: Model.Listener = {
    case trail.StakesAdded(span, stakes@_*) => checkSpanRepaint(span)
    case trail.StakesRemoved(span, stakes@_*) => checkSpanRepaint(span)
  }

  private val mia = new MouseAdapter {
    override def mousePressed(e: MouseEvent) {
      trackTools.foreach(tt => {
        val pos = screenToVirtual(e.getX)
        val span = Span(pos, pos + 1)
        val stakes = trail.getRange(span)
        val stakeO = stakes.headOption
        tt.currentTool.handleSelect(e, trackListElement, pos, stakeO)
        if ((e.getClickCount == 2) && !stakes.isEmpty) showObserverPage()
      })
    }
  }

   private val moveResizeToolListener: Model.Listener = {
        case TrackStakeTool.DragBegin =>
            unionSpan( trailView.selectedStakes ) match {
              case union @ Span(_, _) => painter = createMoveResizePainter( union, painter )
              case _ =>
            }

        case TrackMoveTool.Move( deltaTime, deltaVertical, copy ) => painter match {
           case mrp: MoveResizePainter => {
                 mrp.adjustMove( deltaTime, deltaVertical, copy )
                 mrp.adjusted()
           }
           case _ =>
        }
        case TrackResizeTool.Resize( deltaStart, deltaStop ) => painter match {
           case mrp: MoveResizePainter => {
                 mrp.adjustResize( deltaStart, deltaStop )
                 mrp.adjusted()
           }
           case _ =>
        }
        case TrackSlideTool.Slide( deltaOuter, deltaInner ) => painter match {
           case mrp: MoveResizePainter => {
                 mrp.adjustSlide( deltaOuter, deltaInner )
                 mrp.adjusted()
           }
           case _ =>
        }
        case TrackStakeTool.DragEnd( ce ) => painter match {
           case mrp: MoveResizePainter => mrp.finish( ce )
           case _ =>
        }
        case TrackStakeTool.DragCancel => painter match {
           case mrp: MoveResizePainter => mrp.cancel()
           case _ =>
        }
   }

   private var toolListener: Option[ Model.Listener ] = None

   private val trackToolsListener: Model.Listener = {
      case TrackTools.ToolChanged( oldTool, newTool ) =>
         toolListener.foreach( tl => oldTool.removeListener( tl ))
         toolListener = selectToolListener( newTool )
         toolListener.foreach( tl => newTool.addListener( tl ))

      case TrackTools.VisualBoostChanged( _, boost )        => visualBoost          = boost
      case TrackTools.FadeViewModeChanged( _, mode )        => fadeViewMode         = mode
      case TrackTools.StakeBorderViewModeChanged( _, mode ) => stakeBorderViewMode  = mode
   }
   
   protected def createDefaultPainter = new DefaultPainterTrait {}

   protected def createMoveResizePainter( initialUnion: Span, oldPainter: Painter ) =
      new MoveResizePainter( initialUnion, oldPainter )

   protected def unionSpan( stakes: IterableLike[ Stake[ _ ], _ ]) : SpanOrVoid = {
      val (start, stop) = stakes.foldLeft(
         (Long.MaxValue, Long.MinValue) )( (tup, stake) =>
            (math.min( tup._1, stake.span.start ), math.max( tup._2, stake.span.stop)) )
      if( start < stop ) {
         Span( start, stop )
      } else Span.Void
   }

   private def showObserverPage() {
//       val page      = StakeObserverPage.instance
//       val observer  = AbstractApplication.getApplication()
//          .getComponent( Main.COMP_OBSERVER ).asInstanceOf[ ObserverFrame ]
//       page.setObjects( trailView.selectedStakes: _* )
//       observer.selectPage( page.id )
   }

   protected def selectToolListener( t: TrackTool ): Option[ Model.Listener ] =
      t match {
         case _ : TrackMoveTool   => Some( moveResizeToolListener )
         case _ : TrackResizeTool => Some( moveResizeToolListener )
         case _ : TrackSlideTool  => Some( moveResizeToolListener )
         case _ => None
      }
   
//      setFont( AbstractApplication.getApplication.getGraphicsHandler
//        .getFont( GraphicsHandler.FONT_LABEL | GraphicsHandler.FONT_MINI ))

// WARNING: would destroy laziness
//       if( trailViewEditor.isDefined ) {
//           addMouseListener( mia )
//           addMouseMotionListener( mia )
//       }

//       new DynamicAncestorAdapter( this ).addTo( this )

  def registerTools(tools: TrackTools) {
    visualBoostVar = tools.visualBoost
    fadeViewModeVar = tools.fadeViewMode
    stakeBorderViewModeVar = tools.stakeBorderViewMode
    trackTools = Some(tools)
    tools.addListener(trackToolsListener)
    trackToolsListener(TrackTools.ToolChanged(tools.currentTool, tools.currentTool))
  }

  protected def componentShown() {
    trailView.addListener(trailViewListener)
    trail.addListener(trailListener)
    if (trailViewEditor.isDefined) {
      addMouseListener(mia)
    }
  }

  protected def componentHidden() {
    removeMouseListener(mia)
    trail.removeListener(trailListener)
    trailView.removeListener(trailViewListener)
  }

  protected def visualBoost = visualBoostVar
  protected def visualBoost_=(boost: Float) {
    visualBoostVar = boost
  }

  protected def fadeViewMode = fadeViewModeVar
   protected def fadeViewMode_=( mode: FadeViewMode ) {
      fadeViewModeVar = mode
      checkSpanRepaint( timelineView.span )
   }

   protected def stakeBorderViewMode = stakeBorderViewModeVar
   protected def stakeBorderViewMode_=( mode: StakeBorderViewMode ) {
      stakeBorderViewModeVar = mode
      checkSpanRepaint( timelineView.span )
   }

   protected def screenToVirtual( x: Int ) : Long = {
      val width   = getWidth
      if( width == 0 ) return 0L
      val tlSpan  = timelineView.timeline.span
      val scale   = tlSpan.length / width.toDouble
      (x.toLong * scale + tlSpan.start + 0.5).toLong
   }

   protected def virtualToScreen( pos: Long ) : Int = {
      val tlSpan  = timelineView.timeline.span
      val tlLen   = tlSpan.length
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

    override def getPreferredSize : Dimension = {
       val dim = super.getPreferredSize
       dim.height = 64
       dim
    }

    override def getMinimumSize : Dimension = {
       val dim = super.getMinimumSize
       dim.height = 64
       dim
    }

    override def getMaximumSize : Dimension = {
       val dim = super.getMaximumSize
       dim.height = 64
       dim
    }

   private val clipRect = new Rectangle // avoid re-allocation
   override def paintComponent( g: Graphics ) {
      super.paintComponent( g )
      val span = if( forceFullPaint ) {
         timelineView.timeline.span
      } else {
         timelineView.span
      }
      paintTrack( g.asInstanceOf[ Graphics2D ], 0, 0, getWidth, getHeight, span )
   }

   def paintTrack( g2: Graphics2D, x: Int, y: Int, width: Int, height: Int, viewSpan: Span ) {
      val tlSpan = timelineView.timeline.span
      g2.getClipBounds( clipRect )
      val pc = PaintContext( g2, x, y, -tlSpan.start, width.toDouble / tlSpan.length, height, viewSpan, clipRect )
      g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
      painter.paint( pc )
   }

//   protected class DefaultPainter extends DefaultPainterTrait

   protected trait DefaultPainterTrait extends Painter {
      def paintStake( pc: PaintContext, stake: track.T, selected: Boolean ) {
         val x = pc.virtualToScreen( stake.span.start )
         val y = pc.y
         val width = ((stake.span.stop + pc.p_off) * pc.p_scale + 0.5).toInt - x
         val g2 = pc.g2
         g2.setColor( if( selected ) colrBgSel else colrBg )
         if( stakeBorderViewMode != StakeBorderViewMode.None ) {
            g2.fillRoundRect( x, y, width, pc.height, 5, 5 )
            if( stakeBorderViewMode == StakeBorderViewMode.TitledBox ) stake match {
               case reg: RegionTrait[ _ ] => {
                  val clipOrig = g2.getClip
                  g2.clipRect( x + 2, y + 2, width - 4, pc.height - 4 )
                  g2.setColor( Color.white )
                  g2.drawString( reg.name, x + 4, y + hndlBaseline )
                  g2.setClip( clipOrig )
               }
               case _ =>
            }
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
      private var lastDraggedUnion: SpanOrVoid = initialUnion
      protected var copyTransform = false
      protected var copyTransformChanged = false

      protected var dragTrail: Option[ BasicTrail[ track.T ]] = None

     def adjusted() {
       val tTrail = new BasicTrail[track.T](doc)
       dragTrail = Some(tTrail)
       val tStakes = trailView.selectedStakes.toList.map(transform _)
       tTrail.add(tStakes: _*)
       val newDraggedUnion = unionSpan(tStakes)
       val repaintSpan = if (copyTransformChanged) {
         copyTransformChanged = false
         newDraggedUnion.union(lastDraggedUnion).union(initialUnion)
       } else {
         newDraggedUnion.union(lastDraggedUnion)
       }
       lastDraggedUnion = newDraggedUnion
       repaintSpan match {
         case sp @ Span(_, _) => checkSpanRepaint(sp)
         case _ =>
       }
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
                if( !copyTransform ) ed.editRemove( ce, oldStakesF: _* )
                ed.editAdd( ce, newStakesF: _* )
            })
            trailView.editor.foreach( ed => {
                ed.editSelect( ce, newStakesF: _* )
            })
         })
      }

     def cancel() {
       val repaintSpan = lastDraggedUnion.union(initialUnion)
       painter = oldPainter
       repaintSpan match {
         case sp @ Span(_, _) => checkSpanRepaint(sp)
         case _ =>
       }
     }

     override def paint( pc: PaintContext ) {
         if( dragTrail.isEmpty ) {
            super.paint( pc )
            return
         }

//         val tlSpan = timelineView.timeline.span
         val vwSpan = pc.viewSpan
         trail.visitRange( vwSpan )( stake => {
            if( copyTransform || !trailView.isSelected( stake )) paintStake( pc, stake, selected = false )
         })
         dragTrail.foreach( _.visitRange( vwSpan )( stake =>
            paintStake( pc, stake, selected = true )
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

      def adjustMove( newMove: Long, newMoveVertical: Int, newCopy: Boolean ) {
         move           = newMove
         moveVertical   = newMoveVertical
         if( copyTransform != newCopy ) {
            copyTransform        = newCopy
            copyTransformChanged = true
         }
      }

      def adjustResize( newMoveStart: Long, newMoveStop: Long ) {
         moveStart   = newMoveStart
         moveStop    = newMoveStop
      }

      def adjustSlide( newMoveOuter: Long, newMoveInner: Long ) {
         moveOuter   = newMoveOuter
         moveInner   = newMoveInner
      }

      protected def transform( stake: track.T ) : track.T = {
         val tlSpan = timelineView.timeline.span
         if( move != 0L ) {
            val m = if( move < 0)
               math.max( tlSpan.start - stake.span.start, move )
            else
               math.min( tlSpan.stop - stake.span.stop, move )
            stake.move( m )
         } else if( moveOuter != 0L ) {
            stake match {
               case sStake: SlidableStake[ _ ] => {
                  val mOuter = if( moveOuter < 0)
                     math.max( tlSpan.start - stake.span.start, moveOuter )
                  else
                     math.min( tlSpan.stop - stake.span.stop, moveOuter )
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
                     math.max( tlSpan.start - stake.span.start, moveStart )
                  else
                     math.min( stake.span.length - MIN_SPAN_LEN, moveStart )

//println( "stake " + stake.span + "; moveStart " + moveStart + "; mStart " + mStart )
                  rStake.moveStart( mStart )
               }
               case _ => stake
            }
         } else if( moveStop != 0L ) {
            stake match {
               case rStake: ResizableStake[ _ ] => {
                  val mStop = if( moveStop < 0 )
                     math.max( -stake.span.length + MIN_SPAN_LEN, moveStop )
                  else
                     math.min( tlSpan.stop - stake.span.stop, moveStop )
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
//   private val hndlExtent        = 15
//   private val hndlBaseline      = 12
//   private val cmpMuted          = AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f )
   private val colrBgMuted      = new Color( 0xFF, 0xFF, 0xFF, 0x60 )
}

class AudioTrackComponent(doc: Session, audioTrack: AudioTrack, trackList: TrackList,
                          timelineView: TimelineView)
  extends DefaultTrackComponent(doc, audioTrack, trackList, timelineView)
  /* with SonagramPaintController */ {
  component =>

  import AudioTrackComponent._
  import DefaultTrackComponent._

  private var dropPos: Option[Long] = None

  // ---- constructor ----
  new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter {
    override def dragEnter(dtde: DropTargetDragEvent) {
      process(dtde)
    }

    override def dragOver(dtde: DropTargetDragEvent) {
      process(dtde)
    }

    override def dragExit(dte: DropTargetEvent) {
      dropPos.foreach(pos => {
        dropPos = None
        repaint(Span(pos, pos))
      })
    }

    private def process(dtde: DropTargetDragEvent) {
      val newLoc = if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        dtde.acceptDrag(DnDConstants.ACTION_COPY)
        Some(screenToVirtual(dtde.getLocation.x))
      } else {
        dtde.rejectDrag()
        None
      }
      if (newLoc != dropPos) {
        val dirtySpan = if (dropPos.isDefined) {
          val pos1 = dropPos.get
          val pos2 = newLoc getOrElse pos1
          Span(math.min(pos1, pos2), math.max(pos1, pos2))
        } else {
          val pos1 = newLoc.get
          Span(pos1, pos1)
        }
        dropPos = newLoc
        repaint(dirtySpan)
      }
    }

    def drop(dtde: DropTargetDropEvent) {
      dropPos.foreach(pos => {
        dropPos = None
        repaint(Span(pos, pos))
      })
      if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY)
        val str = dtde.getTransferable.getTransferData(DataFlavor.stringFlavor).toString
        val arr = str.split(":")
        if (arr.length == 3) {
          try {
            val path = new File(arr(0))
            val span = Span(arr(1).toLong, arr(2).toLong)
            val tlSpan = timelineView.timeline.span
            val insPos = math.max(tlSpan.start, math.min(tlSpan.stop,
              screenToVirtual(dtde.getLocation.x)))
            pasteExtern(path, span, insPos)
          }
          catch {
            case _: NumberFormatException =>
          }
        }
        dtde.dropComplete(true)
      } else {
        dtde.rejectDrop()
      }
    }
  })

  // XXX eventually we could save space and let the painter
   // do the msg matching, no?
   private val gainToolListener: Model.Listener = {
      case TrackStakeTool.DragBegin =>
         unionSpan( trailView.selectedStakes ) match {
           case union @ Span(_, _) => painter = new GainPainter( union, painter )
           case _ =>
         }

      case TrackGainTool.Gain( factor ) => painter match {
         case gp: GainPainter =>
            gp.adjustGain( factor )
            gp.adjusted()

         case _ =>
      }
      case TrackStakeTool.DragEnd( ce ) => painter match {
         case gp: GainPainter => gp.finish( ce )
         case _ =>
      }
      case TrackStakeTool.DragCancel => painter match {
         case gp: GainPainter => gp.cancel()
         case _ =>
      }
   }

   private val muteToolListener: Model.Listener = {
      case TrackMuteTool.Mute( ce, muted ) =>
         unionSpan( trailView.selectedStakes ) match {
           case union @ Span(_, _) => painter = new MutePainter( union, painter, ce, muted )
           case  _ =>
         }
   }

   private val fadeToolListener: Model.Listener = {
      case TrackStakeTool.DragBegin =>
         unionSpan( trailView.selectedStakes ) match {
           case union @ Span(_, _) => painter = new FadePainter( union, painter )
           case _ =>
         }

      case TrackFadeTool.Fade( inDelta, outDelta, inCurve, outCurve ) => painter match {
         case fp: FadePainter =>
            fp.adjustFade( inDelta, outDelta, inCurve, outCurve )
            fp.adjusted()
         case _ =>
      }

      case TrackStakeTool.DragEnd( ce ) => painter match {
         case fp: FadePainter => fp.finish( ce )
         case _ =>
      }

      case TrackStakeTool.DragCancel => painter match {
         case fp: FadePainter => fp.cancel()
         case _ =>
      }
   }

   override protected def visualBoost_=( boost: Float ) {
      super.visualBoost_=( boost )
      checkSpanRepaint( timelineView.span, tm = 200L )
   }

      private def pasteExtern( path: File, fileSpan: Span, insPos: Long ) {
         try {
//            val spec = AudioFile.readSpec( path )

            doc.audioFiles.editor.foreach( aed => {
                val afe = doc.audioFiles.find( afe => afe.path == path ) getOrElse {
                   val afeNew = AudioFileElement.fromPath( doc, path )
                   val ce = aed.editBegin( "editAddAudioFile" )
                   aed.editInsert( ce, doc.audioFiles.size, afeNew )
                   aed.editEnd( ce )
                   afeNew
                }

                audioTrack.trail.editor.foreach( ted => {
                     val insSpan = Span( insPos,
                                math.min( insPos + fileSpan.length,
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

   override protected def createDefaultPainter = new DefaultAudioStakePainter

   override protected def createMoveResizePainter( initialUnion: Span, oldPainter: Painter ) =
      new MoveResizePainter( initialUnion, oldPainter ) with AudioStakePainter

   override protected def selectToolListener( t: TrackTool ): Option[ Model.Listener ] =
      t match {
         case _ : TrackGainTool   => Some( gainToolListener )
         case _ : TrackFadeTool   => Some( fadeToolListener )
         case _ : TrackMuteTool   => Some( muteToolListener )
         case _ => super.selectToolListener( t )
      }

   // ---- SonagramPaintController ----
   private class SonoPaint( boost: Float ) extends sonogram.PaintController {
     def adjustGain(amp: Float, pos: Double) = amp * boost
     def imageObserver: ImageObserver = component
   }

//   private class SonoFadePaint( boost: Float, offset: Long, numFrames: Long, fadeIn: FadeSpec, fadeOut: FadeSpec )
//   extends SonagramPaintController {
//      private val doFadeIn    = fadeIn  != null && fadeIn.numFrames > 0
//      private val doFadeOut   = fadeOut != null && fadeOut.numFrames > 0
//      def imageObserver: ImageObserver = component
//      def sonogramGain( pos: Double ) = {
//         var gain = boost
//         if( doFadeIn ) {
//            val f = ((pos - offset) / fadeIn.numFrames).toFloat
//            if( f < 1f ) gain *= fadeIn.shape.levelAt( math.max( 0f, f ), 0f, 1f )
//         }
//         if( doFadeOut ) {
//            val f = ((pos - offset - (numFrames - fadeOut.numFrames)) / fadeOut.numFrames).toFloat
//            if( f > 0f ) gain *= fadeOut.shape.levelAt( math.min( 1f, f ), 1f, 0f )
//         }
//         gain
//      }
//   }

   protected class MutePainter( protected val initialUnion: Span, protected val oldPainter: Painter,
                                ce: AbstractCompoundEdit, dragMute: Boolean )
   extends AudioStakePainter with TransformativePainter {

      // ---- constructor ----
      adjusted()
      finish( ce )

      protected def transform( stake: track.T ) = stake match {
         case ar: AudioRegion => {
            val tStake = ar.mute( dragMute )
            tStake.asInstanceOf[ track.T ]
         }
         case _ => stake
      }
   }

   protected class GainPainter( protected val initialUnion: Span, protected val oldPainter: Painter )
   extends AudioStakePainter with TransformativePainter {
      private var dragGain       = 1f

      def adjustGain( newGain: Float ) {
         dragGain = newGain
      }

      protected def transform( stake: track.T ) = stake match {
         case ar: AudioRegion if( dragGain != 1f ) => {
            val tStake = ar.replaceGain( ar.gain * dragGain )
            tStake.asInstanceOf[ track.T ]
         }
         case _ => stake
      }

      override protected def stakeInfo( stake: AudioRegion ) : Option[ String ] = {
         val db = ampdb( stake.gain ) // * dragGain
         val cb = (math.abs( db ) * 10 + 0.5).toInt
         Some( (if( db > 0 ) "+" else if( db < 0) "-" else "") + (cb/10) + "." + (cb%10) + " dB" )
      }
   }

   protected class FadePainter( protected val initialUnion: Span, protected val oldPainter: Painter )
   extends AudioStakePainter with TransformativePainter {
      private var dragFdInTime   = 0L  // actually delta
      private var dragFdInCurve  = 0f  // actually delta
      private var dragFdOutTime  = 0L  // actually delta
      private var dragFdOutCurve = 0f  // actually delta

      def adjustFade( newInTime: Long, newOutTime: Long, newInCurve: Float, newOutCurve: Float ) {
         dragFdInTime   = newInTime
         dragFdOutTime  = newOutTime
         dragFdInCurve  = newInCurve
         dragFdOutCurve = newOutCurve
      }

      protected def transform( stake: track.T ) = stake match {
         case ar: AudioRegion => {
            import math._
            var tStake: AudioRegion = ar
            val fadeInChange  = dragFdInTime  != 0L || dragFdInCurve != 0f
            val fadeOutChange = dragFdOutTime != 0L || dragFdOutCurve != 0f
            if( fadeInChange || fadeOutChange ) {
               var fadeInSpec  = ar.fadeIn  getOrElse FadeSpec( 0L, linShape )
               var fadeOutSpec = ar.fadeOut getOrElse FadeSpec( 0L, linShape )
               // marika, this should go somewhere, most like AudioRegion ?
               if( fadeInChange ) {
                  val newShape = if( dragFdInCurve != 0f ) fadeInSpec.shape match {
                     case `linShape` => curveShape( dragFdInCurve )
                     case `curveShape`( curvature ) => curveShape( max( -20, min( 20, curvature + dragFdInCurve )))
                     case other => other
                  } else fadeInSpec.shape
                  fadeInSpec = FadeSpec(
                     max( 0L, min( ar.span.length - fadeOutSpec.numFrames,
                     fadeInSpec.numFrames + dragFdInTime )), newShape )
                  tStake = tStake.replaceFadeIn( Some( fadeInSpec ))
               }
               if( fadeOutChange ) {
                  val newShape = if( dragFdOutCurve != 0f ) fadeOutSpec.shape match {
                     case `linShape` => curveShape( dragFdOutCurve )
                     case `curveShape`( curvature ) => curveShape( max( -20, min( 20, curvature + dragFdOutCurve )))
                     case other => other
                  } else fadeOutSpec.shape
                  fadeOutSpec = FadeSpec(
                     max( 0L, min( ar.span.length - fadeInSpec.numFrames,
                     fadeOutSpec.numFrames + dragFdOutTime )), newShape )
                  tStake = tStake.replaceFadeOut( Some( fadeOutSpec ))
               }
            }
            // WHHHHHHHHYYYYYYYYYYYYYYYYYYYYYYYYYYY THE CAST?
            tStake.asInstanceOf[ track.T ]
         }
         case _ => stake
      }
   }

  protected class DefaultAudioStakePainter extends AudioStakePainter

  protected trait AudioStakePainter extends DefaultPainterTrait {
    //      DefaultPainter =>
    private def paintFade(f: FadeSpec, pc: PaintContext, y1: Float, y2: Float, x: Float, y: Float, h: Float, x0: Float) {
      import math._
      val shpFill = new Path2D.Float()
      val shpDraw = new Path2D.Float()
      val px = (f.numFrames * pc.p_scale).toFloat
      val vscale = h / -3
      //       val y1s = (1f - y1) * h + y
      val y1s = max(-3, log10(y1)) * vscale + y
      shpFill.moveTo(x, y1s)
      shpDraw.moveTo(x, y1s)
      //         if( f.shape.id != 1 ) {
      var xs = 4
      while (xs < px) {
        //             val ys = (1 - f.shape.levelAt( xs / px, y1, y2 )) * h + y
        val ys = max(-3, log10(f.shape.levelAt(xs / px, y1, y2))) * vscale + y
        shpFill.lineTo(x + xs, ys)
        shpDraw.lineTo(x + xs, ys)
        xs += 3
      }
      //         }
      //       val y2s = (1f - y2) * h + y
      val y2s = max(-3, log10(y2)) * vscale + y
      shpFill.lineTo(x + px, y2s)
      shpDraw.lineTo(x + px, y2s)
      shpFill.lineTo(x0, y)
      pc.g2.setPaint(pntFade)
      pc.g2.fill(shpFill)
      pc.g2.setColor(colrFade)
      pc.g2.draw(shpDraw)
    }

    protected def stakeInfo(stake: AudioRegion): Option[String] = None

    override def paintStake(pc: PaintContext, stake: track.T, selected: Boolean) {
      import math._
      stake match {
        case ar: AudioRegion => {
          // man, no chance to skip this matching??
          val x = pc.virtualToScreen(ar.span.start)
          val y = pc.y
          //               val width = ((ar.span.stop + pc.p_off) * pc.p_scale + 0.5).toInt - x
          val width = (ar.span.length * pc.p_scale + 0.5).toInt
          //             val x1C = max( x, pc.clip.x - 2 )
          val x1C = max(x + 1, pc.clip.x - 2) // + 1 for left margin
          val x2C = min(x + width, pc.clip.x + pc.clip.width + 3)
          if (x1C < x2C) {
            // skip this if we are not overlapping with clip
            val g2 = pc.g2
            if (stakeBorderViewMode != StakeBorderViewMode.None) {
              g2.setColor(if (selected) colrBgSel else colrBg)
              g2.fillRoundRect(x, y, width, pc.height, 5, 5)
            }

            val clipOrig = g2.getClip
            val hndl = stakeBorderViewMode match {
              case StakeBorderViewMode.None => 0
              case StakeBorderViewMode.Box => 1
              case StakeBorderViewMode.TitledBox => hndlExtent
            }
            val innerH = pc.height - (hndl + 1)
            g2.clipRect(x + 1, y + hndl, width - 1, innerH)

            // --- sonagram ---
            ar.audioFile.sona.foreach {
              sona =>
                val dStart = ar.offset - ar.span.start
                val startC = max(0.0, pc.screenToVirtualD(x1C))
                val stopC = pc.screenToVirtualD(x2C)
                val ctl = if (fadeViewMode == FadeViewMode.Sonogram) {
                  SonogramFadePaint(component, ar, visualBoost)
                  //                        new SonoFadePaint( boost, ar.offset, ar.span.getLength, ar.fadeIn.orNull, ar.fadeOut.orNull )
                } else {
                  val boost = ar.gain * visualBoost
                  new SonoPaint(boost)
                }
                sona.paint(startC + dStart, stopC + dStart, g2, x1C, y + hndl, x2C - x1C, innerH, ctl)
            }

            // --- fades ---
            if (fadeViewMode == FadeViewMode.Curve) {
              ar.fadeIn.foreach(f => {
                paintFade(f, pc, f.floor, 1f, x, y + hndl, innerH, x)
              })
              ar.fadeOut.foreach(f => {
                val px = (f.numFrames * pc.p_scale).toFloat
                paintFade(f, pc, 1f, f.floor, x + width - 1 - px, y + hndl, innerH, x + width - 1)
              })
            }
            g2.setClip(clipOrig)

            // --- label ---
            if (stakeBorderViewMode == StakeBorderViewMode.TitledBox) {
              g2.clipRect(x + 2, y + 2, width - 4, pc.height - 4)
              g2.setColor(Color.white)
              // possible unicodes: 2327 23DB 24DC 25C7 2715 29BB
              g2.drawString(if (ar.muted) "\u23DB " + ar.name else ar.name, x + 4, y + hndlBaseline)
              stakeInfo(ar).foreach(info => {
                g2.setColor(Color.yellow)
                g2.drawString(info, x + 4, y + hndlBaseline + hndlExtent)
              })
              g2.setClip(clipOrig)
            }

            if (ar.muted) {
              g2.setColor(colrBgMuted)
              g2.fillRoundRect(x, y, width, pc.height, 5, 5)
            }
          }
        }
        case _ => super.paintStake(pc, stake, selected)
      }
    }
  }
}