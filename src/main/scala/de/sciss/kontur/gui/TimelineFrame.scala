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

package de.sciss.kontur.gui

import de.sciss.app.AbstractWindow
import de.sciss.common.{ BasicMenuFactory, BasicWindowHandler}
import de.sciss.gui.{GUIUtil, MenuAction, PathField => PathF}
import de.sciss.kontur.io.EisenkrautClient
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, Session, SessionUtil, Stake,
                                 ResizableStake, Timeline, Track }
import de.sciss.kontur.util.PrefsUtil
import de.sciss.util.{ DefaultUnitTranslator, Param, ParamSpace }
import java.awt.event.{ ActionEvent, ActionListener, InputEvent, KeyEvent }
import java.awt.{ BorderLayout, Component, Dimension, Point, Rectangle }
import java.io.File
import javax.swing.{ AbstractAction, Action, Box, ButtonGroup, JButton, JComponent, JLabel, JOptionPane, JProgressBar,
                     JRadioButton, KeyStroke, SwingUtilities }
import scala.math._
import de.sciss.io.{AudioFileDescr, AudioFileFormatPane, IOUtil}
import de.sciss.synth.io.{AudioFileType, SampleFormat, AudioFileSpec}
import language.reflectiveCalls
import de.sciss.span.Span
import de.sciss.span.Span.SpanOrVoid

object TimelineFrame {
  protected val lastLeftTop		= new Point()
  protected val KEY_TRACKSIZE	= "tracksize"
}

class TimelineFrame( val doc: Session, tl: Timeline )
extends AppWindow( AbstractWindow.REGULAR ) with SessionFrame {
   frame =>

// private var writeProtected	= false
//	private var wpHaveWarned	= false
// private val actionShowWindow= new ShowWindowAction( this )
   val timelineView           = new BasicTimelineView( doc, tl )
// private val trackList      = new BasicTrackList( doc, timelineView )
   private val timelinePanel  = new TimelinePanel( timelineView )
// private val trailView      = new javax.swing.JLabel( "Trail" )
// private val trailsView     = new BasicTrailsView( doc, tl.tracks )
   val tracksPanel            = new TracksPanel( doc, timelinePanel )
   private val trackTools     = new TrackToolsPanel( doc, tracksPanel, timelineView )

   // ---- constructor ----
   {
      tracksPanel.registerTools( trackTools )
      timelinePanel.viewPort    = Some( tracksPanel.getViewport )

//    app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!
      val cp = getContentPane
      cp.add( tracksPanel, BorderLayout.CENTER )
      val topBox = Box.createHorizontalBox()
      topBox.add( trackTools )
      topBox.add( Box.createHorizontalGlue() )
      topBox.add( new TransportPanel( timelineView ))
      cp.add( topBox, BorderLayout.NORTH )

//    tracksPanel.tracks = Some( tl.tracks )

    	// ---- actions ----
		val imap		= getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
		val amap		= getActionMap
		val myMeta		= if( BasicMenuFactory.MENU_SHORTCUT == InputEvent.CTRL_MASK )
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK else BasicMenuFactory.MENU_SHORTCUT	// META on Mac, CTRL+SHIFT on PC

		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, InputEvent.CTRL_MASK ), "inch" )
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_OPEN_BRACKET, BasicMenuFactory.MENU_SHORTCUT ), "inch" )
		amap.put( "inch", new ActionSpanWidth( 2.0 ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, InputEvent.CTRL_MASK ), "dech" )
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_CLOSE_BRACKET, BasicMenuFactory.MENU_SHORTCUT ), "dech" )
		amap.put( "dech", new ActionSpanWidth( 0.5 ))
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, myMeta ), "samplvl" )
//		amap.put( "samplvl", new ActionSpanWidth( 0.0 ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), "retn" )
		amap.put( "retn", new ActionScroll( ActionScroll.SCROLL_SESSION_START ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, 0 ), "left" )
		amap.put( "left", new ActionScroll( ActionScroll.SCROLL_SELECTION_START ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, 0 ), "right" )
		amap.put( "right", new ActionScroll( ActionScroll.SCROLL_SELECTION_STOP ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.ALT_MASK ), "fit" )
		amap.put( "fit", new ActionScroll( ActionScroll.SCROLL_FIT_TO_SELECTION ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.ALT_MASK ), "entire" )
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, myMeta ), "entire" )
		amap.put( "entire", new ActionScroll( ActionScroll.SCROLL_ENTIRE_SESSION ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK ), "seltobeg" )
		amap.put( "seltobeg", new ActionSelect( ActionSelect.SELECT_TO_SESSION_START ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK + InputEvent.ALT_MASK ), "seltoend" )
		amap.put( "seltoend", new ActionSelect( ActionSelect.SELECT_TO_SESSION_END ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, 0 ), "postoselbegc" )
		amap.put( "postoselbegc", new ActionSelToPos( 0.0, true ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, 0 ), "postoselendc" )
		amap.put( "postoselendc", new ActionSelToPos( 1.0, true ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, InputEvent.ALT_MASK ), "postoselbeg" )
		amap.put( "postoselbeg", new ActionSelToPos( 0.0, false ))
		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, InputEvent.ALT_MASK ), "postoselend" )
		amap.put( "postoselend", new ActionSelToPos( 1.0, false ))
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, 0 ), "dropmark" )
//		amap.put( "dropmark", new ActionDropMarker() )
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_TAB, 0 ), "selnextreg" )
//		amap.put( "selnextreg", new ActionSelectRegion( SELECT_NEXT_REGION ))
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_TAB, InputEvent.ALT_MASK ), "selprevreg" )
//		amap.put( "selprevreg", new ActionSelectRegion( SELECT_PREV_REGION ))
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_TAB, InputEvent.SHIFT_MASK ), "extnextreg" )
//		amap.put( "extnextreg", new ActionSelectRegion( EXTEND_NEXT_REGION ))
//		imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_TAB, InputEvent.ALT_MASK + InputEvent.SHIFT_MASK ), "extprevreg" )
//		amap.put( "extprevreg", new ActionSelectRegion( EXTEND_PREV_REGION ))

//    imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, BasicMenuFactory.MENU_SHORTCUT ), "debuggen" )
//		amap.put( "debuggen", new ActionDebugGenerator )

    	// ---- menus and actions ----
		val mr = app.getMenuBarRoot

      mr.putMimic( "file.bounce", this, new ActionBounce() )

		mr.putMimic( "edit.cut", this, ActionCut )
		mr.putMimic( "edit.copy", this, ActionCopy )
		mr.putMimic( "edit.paste", this, ActionPaste )
		mr.putMimic( "edit.clear", this, ActionDelete )
		mr.putMimic( "edit.selectAll", this, new ActionSelect( ActionSelect.SELECT_ALL ))

//		mr.putMimic( "timeline.trimToSelection", this, doc.getTrimAction() )
		mr.putMimic( "timeline.insertSpan", this, ActionInsertSpan )
		mr.putMimic( "timeline.clearSpan", this, ActionClearSpan )
		mr.putMimic( "timeline.removeSpan", this, ActionRemoveSpan )
      mr.putMimic( "timeline.dupSpanToPos", this, ActionDupSpanToPos )

      mr.putMimic( "timeline.nudgeAmount", this, ActionNudgeAmount )
      mr.putMimic( "timeline.nudgeLeft", this, new ActionNudge( -1 ))
      mr.putMimic( "timeline.nudgeRight", this, new ActionNudge( 1 ))

		mr.putMimic( "timeline.splitObjects", this, new ActionSplitObjects )
      mr.putMimic( "timeline.selFollowingObj", this, new ActionSelectFollowingObjects )
      mr.putMimic( "timeline.alignObjStartToPos", this, new ActionAlignObjectsStartToTimelinePosition )

      mr.putMimic( "timeline.selStopToStart", this, new ActionSelect( ActionSelect.SELECT_BWD_BY_LEN ))
      mr.putMimic( "timeline.selStartToStop", this, new ActionSelect( ActionSelect.SELECT_FWD_BY_LEN ))

      mr.putMimic( "actions.showInEisK", this, new ActionShowInEisK )

      makeUnifiedLook()
      init()
//  	  updateTitle
//      documentUpdate

      initBounds()	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

	override protected def alwaysPackSize() = false

  //	protected def documentUpdate {
  //    // nada
  //  }

   def document: Session = doc
   
   protected def elementName = Some( tl.name )

   protected def windowClosing() { invokeDispose() }

   def nudgeFrames : Long = ActionNudgeAmount.numFrames

	private def initBounds() {
		val cp	= getClassPrefs
		val bwh	= getWindowHandler
		val sr	= bwh.getWindowSpace
      val dt	= /* AppWindow.*/ stringToDimension( cp.get( TimelineFrame.KEY_TRACKSIZE, null ))
		val d	= if( dt == null ) new Dimension() else dt
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
		setSize( new Dimension( w, (h * hf + 0.5f).toInt ))
		val winSize = getSize
		val wr = new Rectangle( TimelineFrame.lastLeftTop.x + 21, TimelineFrame.lastLeftTop.y + 23,
                                winSize.width, winSize.height )
		GUIUtil.wrapWindowBounds( wr, sr )
		TimelineFrame.lastLeftTop.setLocation( wr.getLocation )
		setBounds( wr )
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

   private object ActionNudgeAmount extends ActionQueryDuration {
      protected def timeline : Timeline = timelineView.timeline
      protected def parent : Component = getWindow

      protected def initiate( v: Param, trans: ParamSpace.Translator ) {
         prefs.put( PrefsUtil.KEY_NUDGEAMOUNT, v.toString )
      }

      private def prefs = app.getUserPrefs.node( PrefsUtil.NODE_GUI )

      protected def initialValue : Param = Param.fromPrefs( prefs, PrefsUtil.KEY_NUDGEAMOUNT, new Param( 0.1, ParamSpace.TIME | ParamSpace.SECS ))

      def numFrames: Long = {
         val v       = initialValue
         val trans   = new DefaultUnitTranslator()
         val tl      = timeline
         trans.setLengthAndRate( tl.span.length, tl.rate )
         (trans.translate( v, ParamSpace.spcTimeSmps ).`val` + 0.5).toLong
      }
   }

   private object ActionInsertSpan extends ActionQueryDuration {
      protected def timeline : Timeline = timelineView.timeline
      protected def parent : Component = getWindow
      protected def initialValue : Param = new Param( 60.0, ParamSpace.TIME | ParamSpace.SECS )

      protected def initiate( v: Param, trans: ParamSpace.Translator ) {
         val delta   = (trans.translate( v, ParamSpace.spcTimeSmps ).`val` + 0.5).toLong
         if( delta <= 0L ) return
         val pos     = timelineView.cursor.position
         val span = Span( pos, pos + delta )

    		val tl      = timeline

			require( (pos >= tl.span.start) && (pos <= tl.span.stop), span.toString )

         val affectedSpan = Span( pos, tl.span.stop )

         tl.editor.foreach( ed => {
            val ce = ed.editBegin( editName )
            try {
               ed.editSpan( ce, Span(tl.span.start, tl.span.stop + delta ))
               timelineView.editor.foreach( ved => {
                  if( timelineView.span.isEmpty ) {
        		         ved.editScroll( ce, span )
                  }
                  ved.editSelect( ce, span )
               })
               tracksPanel.filter( _.selected ).foreach( elem => {
                  val t = elem.track // "stable"
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ t.T ]] // que se puede...
                  tvCast.editor.foreach( ed2 => {
                     val stakes = tvCast.trail.getRange( affectedSpan )
                     ed2.editDeselect( ce, stakes: _* )
                     tvCast.trail.editor.foreach( ed3 => {
                        ed3.editRemove( ce, stakes: _* )
                     })
                     val (split, nosplit) = stakes.partition( _.span.contains( pos ))
                     val newStakes = split.flatMap( _ match {
                        case rs: ResizableStake[ _ ] => {
                           val (nomove, move) = rs.split( pos )
                           List( nomove, move.move( delta ))
                        }
                        case x => List( x )
                     }) ++ nosplit.map( _.move( delta ))
                     tvCast.trail.editor.foreach( ed3 => {
                        ed3.editAdd( ce, newStakes: _* )
                     })
                     ed2.editSelect( ce, newStakes: _* )
                  })
               })
               ed.editEnd( ce )
            }
            catch {
               case e: Throwable => { ed.editCancel( ce ); throw e }
            }
         })
      }
   }

   private object ActionRemoveSpan extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      private def editName = getValue( Action.NAME ).toString

      def perform() {
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

   private object ActionClearSpan extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      private def editName = getValue( Action.NAME ).toString

      def perform() {
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

   private object ActionDupSpanToPos extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      private def editName = getValue( Action.NAME ).toString

      def perform() {
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
   private class ActionSpanWidth( factor: Double )
	extends AbstractAction {
	   def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
		   val visiSpan	= timelineView.span
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
	private class ActionSelToPos( weight: Double, deselect: Boolean )
	extends AbstractAction {
		def actionPerformed( e: ActionEvent ) { perform() }

    private def perform() {
      timelineView.selection.span match {
        case sel @ Span(selStart, _) =>
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
	} // class actionSelToPosClass

   private object ActionScroll {
      val SCROLL_SESSION_START    = 0
      val SCROLL_SELECTION_START  = 1
      val SCROLL_SELECTION_STOP   = 2
      val SCROLL_FIT_TO_SELECTION = 3
      val SCROLL_ENTIRE_SESSION   = 4
   }

   private class ActionScroll( mode: Int )
   extends AbstractAction {
      import ActionScroll._

      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
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
      val SELECT_ALL                = 2
      val SELECT_BWD_BY_LEN         = 3
      val SELECT_FWD_BY_LEN         = 4
    }

   private class ActionSelect( mode: Int )
   extends AbstractAction {
      import ActionSelect._

      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
         timelineView.editor.foreach { ed =>
            val pos = timelineView.cursor.position
            val selSpan = timelineView.selection.span match {
              case sp @ Span(_, _) => sp
              case _ => Span(pos, pos)
            }

            val wholeSpan = timelineView.timeline.span
            val newSpan = mode match {
               case SELECT_TO_SESSION_START  => Span( wholeSpan.start, selSpan.stop )
               case SELECT_TO_SESSION_END    => Span( selSpan.start, wholeSpan.stop )
               case SELECT_ALL               => wholeSpan
               case SELECT_BWD_BY_LEN        =>
                  val delta = -math.min( selSpan.start - wholeSpan.start, selSpan.length )
                  selSpan.shift( delta )
               case SELECT_FWD_BY_LEN        =>
                  val delta = math.min( wholeSpan.stop - selSpan.stop, selSpan.length )
                  selSpan.shift( delta )
               case _ => sys.error( mode.toString )
            }
            if( newSpan != selSpan ) {
               val ce = ed.editBegin( "select" )
               ed.editSelect( ce, newSpan )
               ed.editEnd( ce )
            }
         }
      }
   } // class actionSelectClass

//    private class ActionDebugGenerator
//    extends MenuAction( "Debug Generator" ) {
//        def actionPerformed( e: ActionEvent ) : Unit = debugGenerator
//    }

    private object ActionCut
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) { perform() }

        def perform() {
            println( "CUT NOT YET IMPLEMENTED")
        }
    }

    private object ActionCopy
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) { perform() }

        def perform() {
            println( "COPY NOT YET IMPLEMENTED")
        }
    }

    private object ActionPaste
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) { perform() }

        def perform() {
            println( "PASTE NOT YET IMPLEMENTED")
        }
    }

    private object ActionDelete
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) { perform() }

        def perform() {
            tracksPanel.editor.foreach( ed => {
                val ce = ed.editBegin( getValue( Action.NAME ).toString )
                tracksPanel.foreach( elem => {
                     val t = elem.track // "stable"
                     val tvCast = elem.trailView.asInstanceOf[ TrailView[ t.T ]] // que se puede...
                     tvCast.editor.foreach( ed2 => {
                         val stakes = tvCast.selectedStakes.toList
                         ed2.editDeselect( ce, stakes: _* )
                         tvCast.trail.editor.foreach( ed3 => {
                             ed3.editRemove( ce, stakes: _* )
                         })
                     })
                })
                ed.editEnd( ce )
            })
        }
    }

   private class ActionNudge( factor: Double )
   extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
//         val pos	      = timelineView.cursor.position
         val delta      = (factor * nudgeFrames + 0.5).toLong
         transformSelectedStakes( getValue( Action.NAME ).toString, stake => Some( List( stake.move( delta ))))
      }
   }

    private class ActionSplitObjects
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) { perform() }

    	def perform() {
        	val pos	= timelineView.cursor.position
         transformSelectedStakes( getValue( Action.NAME ).toString, stake => stake match {
            case rStake: ResizableStake[ _ ] if( stake.span.contains( pos )) => {
               val (stake1, stake2) = rStake.split( pos )
               Some( List( stake1, stake2 ))
            }
            case _ => None
         })
    	}
   }

   private class ActionSelectFollowingObjects
   extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
         val pos	= timelineView.cursor.position
         val stop = timelineView.timeline.span.length
         val span = Span( pos, stop )
         timelineView.timeline.editor.foreach( ed => {
            val ce = ed.editBegin( getValue( Action.NAME ).toString )
            tracksPanel.foreach( elem => {
               val track = elem.track // "stable"
               if( elem.selected ) {
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ track.T ]]
                  tvCast.editor.foreach( ed2 => {
                     val trail      = tvCast.trail
                     val toSelect   = trail.getRange( span, byStart = true, overlap = false )
                     ed2.editSelect( ce, toSelect: _* )
                  })
               }
            })
            ed.editEnd( ce )
         })
      }
   }

   private class ActionAlignObjectsStartToTimelinePosition extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
         val pos	= timelineView.cursor.position
         transformSelectedStakes( getValue( Action.NAME ).toString, stake => stake match {
            case rStake: ResizableStake[ _ ] if( stake.span.start != pos ) => {
               Some( List( rStake.move( pos - rStake.span.start )))
            }
            case _ => None
         })
      }
   }

   private class ActionShowInEisK extends MenuAction {
      def actionPerformed( e: ActionEvent ) { perform() }

      def perform() {
         tracksPanel.find( _.trailView.selectedStakes.headOption match {
            case Some( ar: AudioRegion ) => {
               val delta      = ar.offset - ar.span.start
               val cursor     = Some( timelineView.cursor.position + delta )
               val selection  = timelineView.selection.span match {
                 case sp @ Span(_, _) if sp.nonEmpty => sp.shift( delta )
                 case _ => Span.Void
               }
               EisenkrautClient.instance.openAudioFile( ar.audioFile.path, cursor, selection )
               true
            }
            case _ => false
         })
      }
   }

   private class ActionBounce extends MenuAction {
      def actionPerformed( e: ActionEvent ) {
         query.foreach {
           case (tls, span @ Span(_, _), path, spec) =>
            perform( tls, span, path, spec )
           case _ =>
         }
      }

      def perform( tracks: List[ Track ], span: Span, path: File, spec: AudioFileSpec ) {
         val ggProgress = new JProgressBar()
         val name = getValue( Action.NAME ).toString
         val ggCancel = new JButton( getResourceString( "buttonAbort" ))
         ggCancel.setFocusable( false )
         val options = Array[ AnyRef]( ggCancel )
         val op = new JOptionPane( ggProgress, JOptionPane.INFORMATION_MESSAGE, 0, null, options )
         def fDispose() { val w = SwingUtilities.getWindowAncestor( op ); if( w != null ) w.dispose() }
         ggCancel.addActionListener( new ActionListener {
            def actionPerformed( e: ActionEvent ) { fDispose() }
         })
         var done = false
         try {
            val process = SessionUtil.bounce( doc, timelineView.timeline, tracks, span, path, spec, msg => msg match {
               case "done" => { done = true; fDispose() }
               case ("progress", i: Int) => ggProgress.setValue( i )
//               case _ => println( "received: " + msg ) 
            })
            BasicWindowHandler.showDialog( op, getWindow, name )
            if( !done ) process.cancel()
         }
         catch { case e: Exception =>
            fDispose()
            BasicWindowHandler.showErrorDialog( frame.getWindow, e, name )}
      }

      def query: Option[ (List[ Track ], SpanOrVoid, File, AudioFileSpec) ] = {
         val trackElems    = tracksPanel.toList
//         val numTracks     = trackElems.size
         val selTrackElems = trackElems.filter( _.selected )
         val span          = timelineView.timeline.span
         val selSpan       = timelineView.selection.span
         val selAllowed    = selTrackElems.nonEmpty && !selSpan.isEmpty

//         val okOption      = new JButton( getResourceString( "buttonOK" ))
//         val cancelOption  = new JButton( getResourceString( "Cancel" ))
//         val options       = Array( cancelOption, okOption )

         val name          = getValue( Action.NAME ).toString
         val pane          = Box.createVerticalBox
         val ggAll         = new JRadioButton( getResourceString( "bounceDlgAll" ))
         val ggSel         = new JRadioButton( getResourceString( "bounceDlgSel" ))
         val bg            = new ButtonGroup()
         bg.add( ggAll )
         bg.add( ggSel )
         bg.setSelected( ggAll.getModel, true )
         val ggPath        = new PathField( PathF.TYPE_OUTPUTFILE, name )
         val affp          = new AudioFileFormatPane( AudioFileFormatPane.FORMAT | AudioFileFormatPane.ENCODING
            /* | AudioFileFormatPane.GAIN_NORMALIZE */ )
         val descr         = new AudioFileDescr
         affp.toDescr( descr )
         val path0         = doc.path getOrElse {
            val home       = new File( System.getProperty( "user.home" ))
            val desktop    = new File( home, "Desktop" )
            new File( if( desktop.isDirectory ) desktop else home, getResourceString( "labelUntitled" ))
         }
         ggPath.setPath( IOUtil.setFileSuffix( path0, AudioFileDescr.getFormatSuffix( descr.`type` )))
         affp.automaticFileSuffix( ggPath )
         pane.add( ggPath )
         pane.add( affp )
         pane.add( ggAll )
         pane.add( ggSel )
         if( !selAllowed ) {
            ggSel.setEnabled( false )
//            val p2 = Box.createHorizontalBox
//            p2.add( Box.createHorizontalStrut( 24 ))
//            p2.add( )
            pane.add( new JLabel( "   " + getResourceString( "bounceDlgNoSel" )))
         }

         val op = new JOptionPane( pane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
         val result = BasicWindowHandler.showDialog( op, getWindow, name )
         if( result != JOptionPane.OK_OPTION ) return None

         val path       = ggPath.getPath
         if( path.exists ) {
            val opCancel      = getResourceString( "buttonCancel" )
            val opOverwrite   = getResourceString( "buttonOverwrite" )
            val options       = Array[ AnyRef ]( opOverwrite, opCancel )
            val op2           = new JOptionPane( getResourceString( "warnFileExists" ) + ":\n" + path.toString + "\n" +
               getResourceString( "warnOverwriteFile" ), JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
               null, options )
            op2.setInitialSelectionValue( opCancel )
            /* val result2 = */ BasicWindowHandler.showDialog( op2, getWindow, name )
            if( op2.getValue != opOverwrite ) return None
         }

         val all        = bg.isSelected( ggAll.getModel )
//         val descr      = new AudioFileDescr
         affp.toDescr( descr )
         val sampleRate = timelineView.timeline.rate
//         descr.file     = path
         val tracks     = (if( all ) trackElems else selTrackElems).map( _.track )
         val numChannels = tracks.collect( _ match { case at: AudioTrack if( at.diffusion.isDefined ) => at.diffusion.get })
            .foldLeft( 0 )( (maxi, diff) => max( maxi, diff.numOutputChannels ))

         val spec          = AudioFileSpec( descr.`type` match {
            case AudioFileDescr.TYPE_AIFF    => AudioFileType.AIFF
            case AudioFileDescr.TYPE_WAVE    => AudioFileType.Wave
            case AudioFileDescr.TYPE_WAVE64  => AudioFileType.Wave64
            case AudioFileDescr.TYPE_IRCAM   => AudioFileType.IRCAM
            case AudioFileDescr.TYPE_SND     => AudioFileType.NeXT
         }, descr.sampleFormat match {
            case AudioFileDescr.FORMAT_INT => descr.bitsPerSample match {
               case 16 => SampleFormat.Int16
               case 24 => SampleFormat.Int24
               case 32 => SampleFormat.Int32
            }
            case AudioFileDescr.FORMAT_FLOAT => descr.bitsPerSample match {
               case 32 => SampleFormat.Float
               case 64 => SampleFormat.Double
            }
         }, numChannels, sampleRate )

         Some( (tracks, if( all ) span else selSpan, path, spec) )
      }
   }
}