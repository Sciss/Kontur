/*
 *  TimelineFrame.scala
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

import session.{AudioRegion, AudioTrack, Session, SessionUtil, Stake, ResizableStake, Timeline, Track}
import util.PrefsUtil
import java.awt.event.{ActionEvent, ActionListener, InputEvent, KeyEvent}
import java.awt.{Dimension, Point, Rectangle}
import java.io.File
import javax.swing.{Box, ButtonGroup, JButton, JLabel, JOptionPane, JProgressBar, JRadioButton, KeyStroke, SwingUtilities}
import scala.math._
import de.sciss.synth.io.AudioFileSpec
import de.sciss.span.Span
import Span.SpanOrVoid
import legacy.{DefaultUnitTranslator, Param, ParamSpace, GUIUtil}
import swing.{RootPanel, Action, Component, BorderPanel}
import de.sciss.desktop.{Preferences, Window}
import de.sciss.desktop.impl.WindowImpl
import desktop.impl.PathField
import language.reflectiveCalls

object TimelineFrame {
  protected val lastLeftTop		= new Point()
  protected val KEY_TRACKSIZE	= "tracksize"
}

final class TimelineFrame(val document: Session, tl: Timeline) extends WindowImpl with SessionFrame {
  frame =>

  protected def style = Window.Regular

  def handler = Kontur.windowHandler

  // private var writeProtected	= false
  //	private var wpHaveWarned	= false
  // private val actionShowWindow= new ShowWindowAction( this )
  val timelineView = new BasicTimelineView(document, tl)
  // private val trackList      = new BasicTrackList( doc, timelineView )
  private val timelinePanel = new TimelinePanel(timelineView)
  // private val trailView      = new javax.swing.JLabel( "Trail" )
  // private val trailsView     = new BasicTrailsView( doc, tl.tracks )
  val tracksPanel = new TracksPanel(document, timelinePanel)
  private val trackTools = new TrackToolsPanel(document, tracksPanel, timelineView)

  // ---- constructor ----
  {
    tracksPanel.registerTools(trackTools)
    timelinePanel.viewPort = Some(tracksPanel.getViewport)

    // app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!

    val topBox = Box.createHorizontalBox()
    topBox.add(trackTools)
    topBox.add(Box.createHorizontalGlue())
    topBox.add(new TransportPanel(timelineView))
    val pane = new BorderPanel {
      add(Component.wrap(tracksPanel), BorderPanel.Position.Center)
      add(Component.wrap(topBox), BorderPanel.Position.North)
    }
    contents = pane

    //    tracksPanel.tracks = Some( tl.tracks )

    // ---- actions ----
     //		val imap		= getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
     //		val amap		= getActionMap

    import InputEvent.{CTRL_MASK, SHIFT_MASK, ALT_MASK}
    import KeyEvent._
    import KeyStroke.{getKeyStroke => stroke}

    val meta1 = Window.menuShortcut
    val meta2 = if (meta1 == CTRL_MASK) CTRL_MASK | SHIFT_MASK else meta1 // META on Mac, CTRL+SHIFT on PC

    addAction("inch1",    new ActionSpanWidth(2.0, stroke(VK_LEFT,          CTRL_MASK)))
    addAction("inch2",    new ActionSpanWidth(2.0, stroke(VK_OPEN_BRACKET,  meta1    )))
    addAction("dech1",    new ActionSpanWidth(0.5, stroke(VK_RIGHT,         CTRL_MASK)))
    addAction("dech2",    new ActionSpanWidth(0.5, stroke(VK_CLOSE_BRACKET, meta1    )))
    import ActionScroll._
    addAction("retn",     new ActionScroll(SCROLL_SESSION_START,    stroke(VK_ENTER,  0       )))
    addAction("left",     new ActionScroll(SCROLL_SELECTION_START,  stroke(VK_LEFT,   0       )))
    addAction("right",    new ActionScroll(SCROLL_SELECTION_STOP,   stroke(VK_RIGHT,  0       )))
    addAction("fit",      new ActionScroll(SCROLL_FIT_TO_SELECTION, stroke(VK_F,      ALT_MASK)))
    addAction("entire1",  new ActionScroll(SCROLL_ENTIRE_SESSION,   stroke(VK_A,      ALT_MASK)))
    addAction("entire2",  new ActionScroll(SCROLL_ENTIRE_SESSION,   stroke(VK_LEFT,   meta2   )))
    import ActionSelect._
    addAction("seltobeg", new ActionSelect(SELECT_TO_SESSION_START, stroke(VK_ENTER, SHIFT_MASK           )))
    addAction("seltoend", new ActionSelect(SELECT_TO_SESSION_END,   stroke(VK_ENTER, SHIFT_MASK | ALT_MASK)))

    addAction("posselbegc", new ActionSelToPos(0.0, deselect = true,  stroke(VK_UP,   0       )))
    addAction("posselendc", new ActionSelToPos(1.0, deselect = true,  stroke(VK_DOWN, 0       )))
    addAction("posselbeg",  new ActionSelToPos(0.0, deselect = false, stroke(VK_UP,   ALT_MASK)))
    addAction("posselend",  new ActionSelToPos(1.0, deselect = false, stroke(VK_DOWN, ALT_MASK)))

    // ---- menus and actions ----
    bindMenus(
      "file.bounce"                 -> ActionBounce,
  
      "edit.cut"                    -> ActionCut,
      "edit.copy"                   -> ActionCopy,
      "edit.paste"                  -> ActionPaste,
      "edit.clear"                  -> ActionDelete,
      "edit.selectAll"              -> new ActionSelect(ActionSelect.SELECT_ALL),
  
      "timeline.insertSpan"         -> ActionInsertSpan,
      "timeline.clearSpan"          -> ActionClearSpan,
      "timeline.removeSpan"         -> ActionRemoveSpan,
      "timeline.dupSpanToPos"       -> ActionDupSpanToPos,
  
      "timeline.nudgeAmount"        -> ActionNudgeAmount,
      "timeline.nudgeLeft"          -> new ActionNudge(-1),
      "timeline.nudgeRight"         -> new ActionNudge(1),
  
      "timeline.splitObjects"       -> new ActionSplitObjects,
      "timeline.selFollowingObj"    -> new ActionSelectFollowingObjects,
      "timeline.alignObjStartToPos" -> new ActionAlignObjectsStartToTimelinePosition,
  
      "timeline.selStopToStart"     -> new ActionSelect(ActionSelect.SELECT_BWD_BY_LEN),
      "timeline.selStartToStop"     -> new ActionSelect(ActionSelect.SELECT_FWD_BY_LEN),
  
      "actions.showInEisK"          -> new ActionShowInEisK
    )

    makeUnifiedLook()
    // init()
    // updateTitle
    // documentUpdate

    initBounds() // be sure this is after documentUpdate!

    visible = true
  }

//  override protected def alwaysPackSize() = false

  //	protected def documentUpdate {
  //    // nada
  //  }

  protected def elementName = Some(tl.name)

  protected def windowClosing() {
    invokeDispose()
  }

  def nudgeFrames: Long = ActionNudgeAmount.numFrames

  private def initBounds() {
		val cp	= application.userPrefs / "TimelineFrame"
		val sr	= Window.availableSpace
    import de.sciss.desktop.Implicits._
    val d 	= cp.getOrElse[Dimension](TimelineFrame.KEY_TRACKSIZE, new Dimension())(Preferences.Type.dimension)
		val hf	= 1f // Math.sqrt( Math.max( 1, waveView.getNumChannels() )).toFloat
		var w	= d.width
		var h	= d.height
		sr.x		+= 36
		sr.y		+= 36
		sr.width	-= 60
		sr.height	-= 60
		if( w <= 0 ) {
			w = sr.width*2/3 // - AudioTrackRowHeader.ROW_WIDTH
		}
		if( h <= 0 ) {
			h = (sr.height - 106) / 4 // 106 = approx. extra space for title bar, tool bar etc.
		}
//		waveView.setPreferredSize( new Dimension( w, (int) (h * hf + 0.5f) ));
//		pack();
		size = new Dimension( w, (h * hf + 0.5f).toInt )
		val winSize = size
		val wr = new Rectangle( TimelineFrame.lastLeftTop.x + 21, TimelineFrame.lastLeftTop.y + 23,
                                winSize.width, winSize.height )
		GUIUtil.wrapWindowBounds( wr, sr )
		TimelineFrame.lastLeftTop.setLocation( wr.getLocation )
		bounds = wr
//		waveView.addComponentListener( new ComponentAdapter() {
//			public void componentResized( ComponentEvent e )
//			{
//				if( waveExpanded ) {
//					final Dimension dNew = e.getComponent().getSize();
//					dNew.height = (int) (dNew.height / hf + 0.5f);
//					if( !dNew.equals( d )) {
//						d.setSize( dNew );
//						cp.put( KEY_TRACKSIZE, AppWindow.dimensionToString( dNew ));
//					}
//				}
//			}
//		});
	}

   // welcome back to generics hell....
   protected def transformSelectedStakes( name: String, func: Stake[ _ ] => Option[ List[ _ ]]) {
      timelineView.timeline.editor.foreach( ed => {
         val ce = ed.editBegin( name )
         tracksPanel.foreach( elem => {
            val track = elem.track // "stable"
            val tvCast = elem.trailView.asInstanceOf[ TrailView[ track.T ]]
            tvCast.editor.foreach( ed2 => {
               val selectedStakes = ed2.view.selectedStakes
               val trail = tvCast.trail
               trail.editor.foreach( ted => {
                  var toDeselect: List[ track.T ] = Nil // = trail.emptyList
                  var toRemove   = trail.emptyList
                  var toAdd      = trail.emptyList
                  var toSelect   = trail.emptyList

                  selectedStakes.foreach( stake => {
                     func( stake ).foreach( list => {
                        val castList = list.asInstanceOf[ List[ track.T ]]
                        toDeselect ::= stake
                        toRemove   ::= stake
                        toAdd     :::= castList
                        toSelect  :::= castList
                     })
                  })

                  if( toDeselect.nonEmpty )  ed2.editDeselect( ce, toDeselect: _* )
                  if( toRemove.nonEmpty )    ted.editRemove( ce, toRemove: _* )
                  if( toAdd.nonEmpty )       ted.editAdd( ce, toAdd: _* )
                  if( toSelect.nonEmpty )    ed2.editSelect( ce, toSelect: _* )
               })
            })
         })
         ed.editEnd( ce )
      })
    }

   private object ActionNudgeAmount extends ActionQueryDuration("Nudge Amount") {
     protected def timeline: Timeline   = timelineView.timeline
     protected def parent               = frame.component

     protected def initiate(v: Param, trans: ParamSpace.Translator) {
       prefs.put(PrefsUtil.KEY_NUDGEAMOUNT, v.toString)(Preferences.Type.string)
     }

     private def prefs = application.userPrefs / PrefsUtil.NODE_GUI

     protected def initialValue: Param = {
       import desktop.Implicits._
       prefs.getOrElse(PrefsUtil.KEY_NUDGEAMOUNT, new Param(0.1, ParamSpace.TIME | ParamSpace.SECS))
     }

     def numFrames: Long = {
       val v      = initialValue
       val trans  = new DefaultUnitTranslator()
       val tl     = timeline
       trans.setLengthAndRate(tl.span.length, tl.rate)
       (trans.translate(v, ParamSpace.spcTimeSmps).value + 0.5).toLong
     }
   }

  private object ActionInsertSpan extends ActionQueryDuration("Insert Span") {
    protected def timeline: Timeline = timelineView.timeline

    protected def parent: RootPanel = frame.component

    protected def initialValue: Param = new Param(60.0, ParamSpace.TIME | ParamSpace.SECS)

    protected def initiate(v: Param, trans: ParamSpace.Translator) {
      val delta = (trans.translate(v, ParamSpace.spcTimeSmps).value + 0.5).toLong
      if (delta <= 0L) return
      val pos   = timelineView.cursor.position
      val span  = Span(pos, pos + delta)
      val tl    = timeline

      require((pos >= tl.span.start) && (pos <= tl.span.stop), span.toString)

      val affectedSpan = Span(pos, tl.span.stop)

      tl.editor.foreach { ed =>
        val ce = ed.editBegin(editName)
        try {
          ed.editSpan(ce, Span(tl.span.start, tl.span.stop + delta))
          timelineView.editor.foreach { ved =>
            if (timelineView.span.isEmpty) {
              ved.editScroll(ce, span)
            }
            ved.editSelect(ce, span)
          }
          tracksPanel.filter(_.selected).foreach { elem =>
            val t = elem.track // "stable"
            val tvCast = elem.trailView.asInstanceOf[TrailView[t.T]] // que se puede...
            tvCast.editor.foreach { ed2 =>
              val stakes = tvCast.trail.getRange(affectedSpan)
              ed2.editDeselect(ce, stakes: _*)
              tvCast.trail.editor.foreach { ed3 =>
                ed3.editRemove(ce, stakes: _*)
              }
              val (split, nosplit) = stakes.partition(_.span.contains(pos))
              val newStakes = split.flatMap(_ match {
                case rs: ResizableStake[_] =>
                  val (nomove, move) = rs.split(pos)
                  List(nomove, move.move(delta))
                case x => List(x)
              }) ++ nosplit.map(_.move(delta))
              tvCast.trail.editor.foreach { ed3 =>
                ed3.editAdd(ce, newStakes: _*)
              }
              ed2.editSelect(ce, newStakes: _*)
            }
          }
          ed.editEnd(ce)
        }
        catch {
          case e: Throwable => {
            ed.editCancel(ce); throw e
          }
        }
      }
    }
  }

  private object ActionRemoveSpan extends Action("Remove Span") {
      private def editName = title

      def apply() {
         timelineView.selection.span match {
           case sp @ Span(_, _) if sp.nonEmpty => perform(sp)
           case _ =>
         }
      }

     private def perform(span: Span) {
         val tlSpan        = tl.span
         val affectedSpan  = Span( span.start, tlSpan.stop )
         val delta         = -span.length

         tl.editor.foreach( ed => {
            val ce = ed.editBegin( editName )
            try {
               val (selTracksE, unselTracksE) = tracksPanel.toList.partition( _.selected )
               val newTlStop = unselTracksE.foldLeft( math.max( tlSpan.start, tlSpan.stop - span.length )) { (mx, tr) =>
                  val st = tr.track.trail.getRange( affectedSpan )
                  if( st.isEmpty ) mx else math.max( mx, st.map( _.span.stop ).max )
               }

               selTracksE.foreach { elem =>
                  val t = elem.track // "stable"
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ t.T ]]
                  tvCast.editor.foreach { ed2 =>
                     val stakes              = tvCast.trail.getRange( affectedSpan )
                     val (toErase, stakes0)  = stakes.partition( s => span.contains( s.span ))
                     val (toSplit, toMove)   = stakes0.partition( _.span.overlaps( affectedSpan ))
                     val obsolete            = toErase ++ toSplit ++ toMove
                     ed2.editDeselect( ce, obsolete: _* )
                     tvCast.trail.editor.foreach( _.editRemove( ce, obsolete: _* ))
                     val split: Seq[ t.T ] = toSplit.flatMap( s => s match {
                        case rs0: ResizableStake[ _ ] => {
                           val rs = rs0.asInstanceOf[ ResizableStake[ t.T ]]
//                           val rsCast = rs.asInstanceOf[ t.T wit hResizableStake[ _ ]] // ouch
                           if( rs.span.contains( span.start )) {
                              val (a, b0) = rs.split( span.start)
                              if( b0.span.contains( span.stop )) {
                                 b0 match {
                                    case b1: ResizableStake[ _ ] =>
                                       val (_, b) = b1.asInstanceOf[ ResizableStake[ t.T ]].split( span.stop )
                                       Seq( a, b.move( delta ))
                                    case _ =>
                                       Seq( s )
                                 }
                              } else {
                                 Seq( a )
                              }
                           } else {
                              if( rs.span.contains( span.stop )) {
                                 val (_, b) = rs.split( span.stop )
                                 Seq( b.move( delta ))
                              } else Seq( rs.move( delta )) // Seq.empty
                           }
                        }
                        case _ => Seq( s )
                     })
                     val moved: Seq[ t.T ] = toMove.map( _.move( delta ))
                     val newStakes  = split ++ moved

//println( "in " + stakes.size + "; split before = " + toSplit.size + "; after = " + split.size + "; erased " + toErase.size + "; moved = " + moved.size )

                     tvCast.trail.editor.foreach( _.editAdd( ce, newStakes: _* ))
                     ed2.editSelect( ce, newStakes: _* )
                  }
               }

               timelineView.editPosition( ce, span.start )
               timelineView.editSelect( ce, Span.Void )

               if( newTlStop != tlSpan.stop ) {
                  tl.editor.foreach { ed =>
                     val vSpan = timelineView.span
                     if( newTlStop < vSpan.stop ) {
                        timelineView.editScroll( ce, Span( math.max( tlSpan.start, vSpan.start + delta ), newTlStop ))
                     }
                     ed.editSpan( ce, Span(tlSpan.start, newTlStop ))
                  }
               }

               ed.editEnd( ce )
            }
            catch {
               case e: Throwable => { ed.editCancel( ce ); throw e }
            }
         })
      }
   }

   private object ActionClearSpan extends Action("Clear Span") {
      private def editName = title

      def apply() {
         timelineView.selection.span match {
           case sp @ Span(_, _) if sp.nonEmpty => perform(sp)
           case _ =>
         }
      }

     private def perform(span: Span) {
         tl.editor.foreach( ed => {
            val ce = ed.editBegin( editName )
            try {
               tracksPanel.filter( _.selected ).foreach { elem =>
                  val t = elem.track // "stable"
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ t.T ]]
                  tvCast.editor.foreach { ed2 =>
                     val stakes              = tvCast.trail.getRange( span )
                     val (erased, stakes0)   = stakes.partition( s => span.contains( s.span ))
                     val split               = stakes0.filter( _.span.overlaps( span ))
                     val obsolete            = erased ++ split
                     ed2.editDeselect( ce, obsolete: _* )
                     tvCast.trail.editor.foreach( _.editRemove( ce, obsolete: _* ))
                     val newStakes: Seq[ t.T ] = split.flatMap( s => s match {
                        case rs0: ResizableStake[ _ ] => {
                           val rs = rs0.asInstanceOf[ ResizableStake[ t.T ]]
//                           val rsCast = rs.asInstanceOf[ t.T wit hResizableStake[ _ ]] // ouch
                           if( rs.span.contains( span.start )) {
                              val (a, b0) = rs.split( span.start)
                              if( b0.span.contains( span.stop )) {
                                 b0 match {
                                    case b1: ResizableStake[ _ ] =>
                                       val (_, b) = b1.asInstanceOf[ ResizableStake[ t.T ]].split( span.stop )
                                       Seq( a, b )
                                    case _ =>
                                       Seq( s )
                                 }
                              } else {
                                 Seq( a )
                              }
                           } else {
                              if( rs.span.contains( span.stop )) {
                                 val (_, b) = rs.split( span.stop )
                                 Seq( b )
                              } else Seq.empty
                           }
                        }
                        case _ => Seq( s )
                     })
                     tvCast.trail.editor.foreach( _.editAdd( ce, newStakes: _* ))
                     ed2.editSelect( ce, newStakes: _* )
                  }
               }
               ed.editEnd( ce )
            }
            catch {
               case e: Throwable => { ed.editCancel( ce ); throw e }
            }
         })
      }
   }

   private object ActionDupSpanToPos extends Action("Duplicate Span to Position") {
      private def editName = title

      def apply() {
         timelineView.selection.span match {
           case sp @ Span(_, _) if sp.nonEmpty => perform(sp)
           case _ =>
         }
      }

     private def perform(srcSpan: Span) {
         val delta   = timelineView.cursor.position - srcSpan.start
//         val dstSpan = srcSpan.shift( delta )
         val tlSpan  = tl.span
         if( srcSpan.isEmpty ) return
         tl.editor.foreach( ed => {
            val ce = ed.editBegin( editName )
            try {
               tracksPanel.filter( _.selected ).foreach { elem =>
                  val t = elem.track // "stable"
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ t.T ]]
                  tvCast.editor.foreach { ed2 =>
                     val stakes              = tvCast.trail.getRange( srcSpan )
                     val (toMove, stakes0)   = stakes.partition( s => srcSpan.contains( s.span ))
                     val toSplit             = stakes0.filter( _.span.overlaps( srcSpan ))
                     val obsolete            = toMove ++ toSplit
                     ed2.editDeselect( ce, obsolete: _* )
//                     tvCast.trail.editor.foreach( _.editRemove( ce, obsolete: _* ))
                     val split: Seq[ t.T ] = toSplit.map( s => (s match {
                        case rs0: ResizableStake[ _ ] => {
                           val rs = rs0.asInstanceOf[ ResizableStake[ t.T ]]
//                           val rsCast = rs.asInstanceOf[ t.T wit hResizableStake[ _ ]] // ouch
                           if( rs.span.contains( srcSpan.start )) {
                              val (_, b0) = rs.split( srcSpan.start)
                              if( b0.span.contains( srcSpan.stop )) {
                                 b0 match {
                                    case b1: ResizableStake[ _ ] =>
                                       val (b, _) = b1.asInstanceOf[ ResizableStake[ t.T ]].split( srcSpan.stop )
                                       b
                                    case _ =>
                                       b0
                                 }
                              } else b0

                           } else {
                              if( rs.span.contains( srcSpan.stop )) {
                                 val (b, _) = rs.split( srcSpan.stop )
                                 b
                              } else s
                           }
                        }
                        case _ => s
                     }).move( delta ))

                     val moved: Seq[ t.T ] = toMove.map( _.move( delta ))
                     val newStakes = split ++ moved
                     val newMax = if( newStakes.isEmpty ) tlSpan.stop else newStakes.map( _.span.stop ).max

                     if( newMax > tlSpan.stop ) {
                        ed.editSpan( ce, Span(tlSpan.start, newMax ))
                     }
                     tvCast.trail.editor.foreach( _.editAdd( ce, newStakes: _* ))
                     ed2.editSelect( ce, newStakes: _* )
                  }
               }
               ed.editEnd( ce )
            }
            catch {
               case e: Throwable => { ed.editCancel( ce ); throw e }
            }
         })
      }
   }

	/**
	 *  Increase or decrease the width
	 *  of the visible time span
	 */
  private class ActionSpanWidth(factor: Double, stroke: KeyStroke)
    extends Action(s"Span Width $factor") {

    accelerator = Some(stroke)

    def apply() {
      val visiSpan = timelineView.span
      val visiLen		= visiSpan.length
			val pos			= timelineView.cursor.position
         val timelineLen = timelineView.timeline.span.length

         val newVisiSpan = if( factor < 1.0 ) {		// zoom in
            if( visiLen < 4 ) Span.Void
            else {
               // if timeline pos visible -> try to keep it's relative position constant
               if( visiSpan.contains( pos )) {
                  val start   = pos - ((pos - visiSpan.start) * factor + 0.5).toLong
                  val stop    = start + (visiLen * factor + 0.5).toLong
                  Span( start, stop )
               // if timeline pos before visible span, zoom left hand
               } else if( visiSpan.start > pos ) {
                  val start   = visiSpan.start
                  val stop    = start + (visiLen * factor + 0.5).toLong
                  Span( start, stop )
               // if timeline pos after visible span, zoom right hand
               } else {
                  val stop    = visiSpan.stop
                  val start   = stop - (visiLen * factor + 0.5).toLong
                  Span( start, stop )
               }
            }
			} else {			// zoom out
            val start   = max( 0, visiSpan.start - (visiLen * factor/4 + 0.5).toLong )
            val stop    = min( timelineLen, start + (visiLen * factor + 0.5).toLong )
            Span( start, stop )
         }
        newVisiSpan match {
          case sp @ Span(_, _) if sp.nonEmpty =>
            timelineView.editor.foreach { ed =>
               val ce = ed.editBegin( "scroll" )
//println( "NEW VISI SPAN " + newVisiSpan )
               ed.editScroll( ce, sp )
               ed.editEnd( ce )
            }

          case _ =>
         }
		}
   } // class actionSpanWidthClass

	/*
	 *	@warning	have to keep an eye on this. with weight as float
	 *				there were quantization errors. with double seems
	 *				to be fine. haven't checked with really long files!!
	 */
  private class ActionSelToPos(weight: Double, deselect: Boolean, stroke: KeyStroke)
    extends Action("Extends Selection to Position") {

    accelerator = Some(stroke)

    def apply() {
      timelineView.selection.span match {
        case sel@Span(selStart, _) =>
          timelineView.editor.foreach { ed =>
            val ce = ed.editBegin("position")
            if (deselect) ed.editSelect(ce, Span.Void)
            val pos = (selStart + sel.length * weight + 0.5).toLong
            ed.editPosition(ce, pos)
            ed.editEnd(ce)
          }
        case _ =>
      }
    }
  }

  // class actionSelToPosClass

  private object ActionScroll {
      val SCROLL_SESSION_START    = 0
      val SCROLL_SELECTION_START  = 1
      val SCROLL_SELECTION_STOP   = 2
      val SCROLL_FIT_TO_SELECTION = 3
      val SCROLL_ENTIRE_SESSION   = 4
   }

  private class ActionScroll(mode: Int, stroke: KeyStroke)
    extends Action("Scroll") {

    accelerator = Some(stroke)

    import ActionScroll._

    def apply() {
         timelineView.editor.foreach { ed =>
            val pos       = timelineView.cursor.position
            val visiSpan  = timelineView.span
            val wholeSpan = timelineView.timeline.span
            if( mode == SCROLL_SESSION_START ) {
//             if( transport.isRunning ) transport.stop
               val posNotZero = pos != wholeSpan.start
               val zeroNotVisi = !visiSpan.contains( wholeSpan.start )
               if( posNotZero || zeroNotVisi ) {
                  val ce = ed.editBegin( "scroll" )
                  if( posNotZero ) ed.editPosition( ce, wholeSpan.start )
                  if( zeroNotVisi ) {
                     ed.editScroll( ce, Span( wholeSpan.start, wholeSpan.start + visiSpan.length ))
                  }
                  ed.editEnd( ce )
               }
            } else {
               val selSpan = timelineView.selection.span
               val newSpan = mode match {
                  case SCROLL_SELECTION_START =>
                     val selSpanStart = selSpan match {
                       case Span.HasStart(s)  => s
                       case _ => pos
                     }
                     val start = max( wholeSpan.start, selSpanStart - (visiSpan.length >>
                        (if( visiSpan.contains( selSpanStart )) 1 else 3)) )
                     val stop = min( wholeSpan.stop, start + visiSpan.length )
                     Span( start, stop )

                     case SCROLL_SELECTION_STOP =>
                        val selSpanStop = selSpan match {
                          case Span.HasStop(s) => s
                          case _ => pos
                        }
                        val stop = min( wholeSpan.stop, selSpanStop + (visiSpan.length >>
                           (if( visiSpan.contains( selSpanStop )) 1 else 3)) )
                        val start = max( wholeSpan.start, stop - visiSpan.length )
                        Span( start, stop )

                  case SCROLL_FIT_TO_SELECTION => selSpan
                  case SCROLL_ENTIRE_SESSION => timelineView.timeline.span
                  case _ => sys.error( mode.toString )
               }
              newSpan match {
                case sp @ Span(_, _) if sp.nonEmpty && sp != visiSpan =>
                  val ce = ed.editBegin( "scroll" )
                  ed.editScroll( ce, sp )
                  ed.editEnd( ce )
                case _ =>
              }
            }
         }
      }
   } // class actionScrollClass

   private object ActionSelect {
      val SELECT_TO_SESSION_START	= 0
      val SELECT_TO_SESSION_END		= 1
      val SELECT_ALL              = 2
      val SELECT_BWD_BY_LEN       = 3
      val SELECT_FWD_BY_LEN       = 4
    }

  private class ActionSelect(mode: Int, stroke: KeyStroke = null)
    extends Action("Select") {

    accelerator = Option(stroke)

    import ActionSelect._

    def apply() {
      timelineView.editor.foreach { ed =>
        val pos = timelineView.cursor.position
        val selSpan = timelineView.selection.span match {
          case sp @ Span(_, _) => sp
          case _ => Span(pos, pos)
        }

        val wholeSpan = timelineView.timeline.span
        val newSpan = mode match {
          case SELECT_TO_SESSION_START  => Span(wholeSpan.start, selSpan.stop)
          case SELECT_TO_SESSION_END    => Span(selSpan.start, wholeSpan.stop)
          case SELECT_ALL               => wholeSpan
          case SELECT_BWD_BY_LEN =>
            val delta = -math.min(selSpan.start - wholeSpan.start, selSpan.length)
            selSpan.shift(delta)
          case SELECT_FWD_BY_LEN =>
            val delta = math.min(wholeSpan.stop - selSpan.stop, selSpan.length)
            selSpan.shift(delta)
          case _ => sys.error(mode.toString)
        }
        if (newSpan != selSpan) {
          val ce = ed.editBegin("select")
          ed.editSelect(ce, newSpan)
          ed.editEnd(ce)
        }
      }
    }
  }

  // class actionSelectClass

  //    private class ActionDebugGenerator
//    extends MenuAction( "Debug Generator" ) {
//        def actionPerformed( e: ActionEvent ) : Unit = debugGenerator
//    }

  private object ActionCut
    extends Action("Cut") {

    def apply() {
      println("CUT NOT YET IMPLEMENTED")
    }
  }

  private object ActionCopy
    extends Action("Copy") {

    def apply() {
      println("COPY NOT YET IMPLEMENTED")
    }
  }

  private object ActionPaste
    extends Action("Paste") {

    def apply() {
      println("PASTE NOT YET IMPLEMENTED")
    }
  }

  private object ActionDelete
    extends Action("Delete") {

    def apply() {
      tracksPanel.editor.foreach { ed =>
        val ce = ed.editBegin(title)
        tracksPanel.foreach { elem =>
          val t = elem.track // "stable"
          val tvCast = elem.trailView.asInstanceOf[TrailView[t.T]] // que se puede...
          tvCast.editor.foreach { ed2 =>
            val stakes = tvCast.selectedStakes.toList
            ed2.editDeselect(ce, stakes: _*)
            tvCast.trail.editor.foreach { ed3 =>
              ed3.editRemove(ce, stakes: _*)
            }
          }
        }
        ed.editEnd(ce)
      }
    }
  }

  private class ActionNudge(factor: Double)
    extends Action("Nudge") {

    def apply() {
      // val pos	      = timelineView.cursor.position
      val delta = (factor * nudgeFrames + 0.5).toLong
      transformSelectedStakes(title, stake => Some(List(stake.move(delta))))
    }
  }

  private class ActionSplitObjects
    extends Action("Split Objects") {

    def apply() {
      val pos = timelineView.cursor.position
      transformSelectedStakes(title, {
        case rStake: ResizableStake[_] if rStake.span.contains(pos) =>
          val (stake1, stake2) = rStake.split(pos)
          Some(List(stake1, stake2))

        case _ => None
      })
    }
  }

  private class ActionSelectFollowingObjects
    extends Action("Select Following Objects") {

    def apply() {
      val pos   = timelineView.cursor.position
      val stop  = timelineView.timeline.span.length
      val span  = Span(pos, stop)
      timelineView.timeline.editor.foreach { ed =>
        val ce = ed.editBegin(title)
        tracksPanel.foreach { elem =>
          val track = elem.track // "stable"
          if (elem.selected) {
            val tvCast = elem.trailView.asInstanceOf[TrailView[track.T]]
            tvCast.editor.foreach { ed2 =>
              val trail = tvCast.trail
              val toSelect = trail.getRange(span, byStart = true, overlap = false)
              ed2.editSelect(ce, toSelect: _*)
            }
          }
        }
        ed.editEnd(ce)
      }
    }
  }

  private class ActionAlignObjectsStartToTimelinePosition extends Action("Align Objects Start to Timeline Position") {
    def apply() {
      val pos = timelineView.cursor.position
      transformSelectedStakes(title, {
        case rStake: ResizableStake[_] if rStake.span.start != pos =>
          Some(List(rStake.move(pos - rStake.span.start)))

        case _ => None
      })
    }
  }

   private class ActionShowInEisK extends Action("Show in Eisenraut") {
     def apply() {
       tracksPanel.find(_.trailView.selectedStakes.headOption match {
         case Some(ar: AudioRegion) =>
           val delta      = ar.offset - ar.span.start
           val cursor     = Some(timelineView.cursor.position + delta)
           val selection  = timelineView.selection.span match {
             case sp @ Span(_, _) if sp.nonEmpty => sp.shift(delta)
             case _ => Span.Void
           }
           Kontur.eisenkraut.openAudioFile(ar.audioFile.path, cursor, selection)
           true

         case _ => false
       })
     }
   }

  private object ActionBounce extends Action("Bounce") {
    def apply() {
      query.foreach {
        case (tls, span@Span(_, _), path, spec) =>
          perform(tls, span, path, spec)
        case _ =>
      }
    }

    def perform(tracks: List[Track], span: Span, path: File, spec: AudioFileSpec) {
      val ggProgress = new JProgressBar()
      val name = title
      val ggCancel = new JButton("Abort") // getResourceString("buttonAbort"))
      ggCancel.setFocusable(false)
      val options = Array[AnyRef](ggCancel)
      val op = new JOptionPane(ggProgress, JOptionPane.INFORMATION_MESSAGE, 0, null, options)
      def fDispose() {
        val w = SwingUtilities.getWindowAncestor(op); if (w != null) w.dispose()
      }
      ggCancel.addActionListener(new ActionListener {
        def actionPerformed(e: ActionEvent) {
          fDispose()
        }
      })
      var done = false
      try {
        val process = SessionUtil.bounce(document, timelineView.timeline, tracks, span, path, spec, {
          case "done"               => done = true; fDispose()
          case ("progress", i: Int) => ggProgress.setValue(i)
          // case _ => println( "received: " + msg )
        })(application)
        showDialog(op, name)
        if (!done) process.cancel()
      }
      catch {
        case e: Exception =>
          fDispose()
          showDialog(e -> name)
      }
    }

    def query: Option[(List[Track], SpanOrVoid, File, AudioFileSpec)] = {
      val trackElems = tracksPanel.toList
      // val numTracks     = trackElems.size
      val selTrackElems = trackElems.filter(_.selected)
      val span = timelineView.timeline.span
      val selSpan = timelineView.selection.span
      val selAllowed = selTrackElems.nonEmpty && !selSpan.isEmpty

      // val okOption      = new JButton( getResourceString( "buttonOK" ))
      // val cancelOption  = new JButton( getResourceString( "Cancel" ))
      // val options       = Array( cancelOption, okOption )

      val name = title
      val pane = Box.createVerticalBox
      val ggAll = new JRadioButton("All") // getResourceString("bounceDlgAll"))
      val ggSel = new JRadioButton("Selection") // getResourceString("bounceDlgSel"))
      val bg = new ButtonGroup()
      bg.add(ggAll)
      bg.add(ggSel)
      bg.setSelected(ggAll.getModel, true)
      val ggPath = new PathField(PathField.Output)
      ggPath.dialogText = name
      val affp = new AudioFileSpecPane
      affp.fileType     = true
      affp.sampleFormat = true
      val descr         = affp.toSpec
      import io.Implicits._
      val path0 = document.path getOrElse {
        val home    = new File(sys.props("user.home"))
        val desktop = home / "Desktop"
        val dir     = if (desktop.isDirectory) desktop else home
        dir / "Untitled"  // getResourceString("labelUntitled"))
      }
      ggPath.file = path0.updateSuffix(descr.fileType.extension)
      affp.linkedPathField = Some(ggPath)
      pane.add(ggPath)
      pane.add(affp)
      pane.add(ggAll)
      pane.add(ggSel)
      if (!selAllowed) {
        ggSel.setEnabled(false)
        // val p2 = Box.createHorizontalBox
        // p2.add( Box.createHorizontalStrut( 24 ))
        // p2.add( )
        pane.add(new JLabel("   " + "No Selection")) // getResourceString("bounceDlgNoSel")))
      }

      val op = new JOptionPane(pane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION)
      val result = showDialog(op, name)
      if (result != JOptionPane.OK_OPTION) return None

      val path = ggPath.file
      if (path.exists) {
        val opCancel = "Cancel" // getResourceString("buttonCancel")
        val opOverwrite = "Overwrite" // getResourceString("buttonOverwrite")
        val options = Array[AnyRef](opOverwrite, opCancel)
        val op2 = new JOptionPane("Warning: File already exists" /* getResourceString("warnFileExists") */ + ":\n" + path.toString + "\n" +
          "Overwrite file?" /* getResourceString("warnOverwriteFile") */, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
          null, options)
        op2.setInitialSelectionValue(opCancel)
        /* val result2 = */ showDialog(op2, name)
        if (op2.getValue != opOverwrite) return None
      }

      val all = bg.isSelected(ggAll.getModel)
      // val descr      = new AudioFileDescr
      val sampleRate = timelineView.timeline.rate
      // descr.file     = path
      val tracks = (if (all) trackElems else selTrackElems).map(_.track)
      val numChannels = tracks.collect(_ match {
        case at: AudioTrack if (at.diffusion.isDefined) => at.diffusion.get
      })
        .foldLeft(0)((maxi, diff) => max(maxi, diff.numOutputChannels))

      val spec = affp.toSpec(AudioFileSpec(numChannels = numChannels, sampleRate = sampleRate))

      Some((tracks, if (all) span else selSpan, path, spec))
    }
  }
}