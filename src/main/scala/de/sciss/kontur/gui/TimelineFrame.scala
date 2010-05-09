/*
 *  TimelineFrame.scala
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

import de.sciss.app.{ AbstractWindow }
import de.sciss.common.{ BasicMenuFactory, BasicWindowHandler}
import de.sciss.gui.{ GUIUtil, MenuAction, PathField => PathF, SpringPanel }
import de.sciss.io.{ AudioFileDescr, AudioFileFormatPane, IOUtil, Span }
import de.sciss.kontur.sc.{ BounceSynthContext, SCSession, SCTimeline }
import de.sciss.kontur.io.{ EisenkrautClient }
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, Session, SessionUtil, Stake,
                                 ResizableStake, Timeline, Track }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.util.{ DefaultUnitTranslator, Param, ParamSpace }
import de.sciss.synth.{ ServerOptions }
import java.awt.event.{ ActionEvent, ActionListener, InputEvent, KeyEvent }
import java.awt.{ BorderLayout, Dimension, Point, Rectangle }
import java.io.{ File }
import javax.swing.{ AbstractAction, Action, Box, ButtonGroup, JButton, JComponent, JLabel, JOptionPane, JProgressBar,
                     JRadioButton, KeyStroke, SwingUtilities }
import scala.math._

object TimelineFrame {
  protected val lastLeftTop		= new Point()
  protected val KEY_TRACKSIZE	= "tracksize"
}

class TimelineFrame( protected val doc: Session, tl: Timeline )
extends AppWindow( AbstractWindow.REGULAR ) with SessionFrame {
   frame =>

// private var writeProtected	= false
//	private var wpHaveWarned	= false
// private val actionShowWindow= new ShowWindowAction( this )
   private val timelineView  = new BasicTimelineView( doc, tl )
// private val trackList     = new BasicTrackList( doc, timelineView )
   private val timelinePanel = new TimelinePanel( timelineView )
// private val trailView     = new javax.swing.JLabel( "Trail" )
// private val trailsView     = new BasicTrailsView( doc, tl.tracks )
   private val tracksPanel    = new TracksPanel( doc, timelinePanel )
   private val trackTools     = new TrackToolsPanel( tracksPanel, timelineView )

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
		val amap		= getActionMap()
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

		mr.putMimic( "edit.cut", this, new ActionCut() )
		mr.putMimic( "edit.copy", this, new ActionCopy() )
		mr.putMimic( "edit.paste", this, new ActionPaste() )
		mr.putMimic( "edit.clear", this, new ActionDelete() )
		mr.putMimic( "edit.selectAll", this, new ActionSelect( ActionSelect.SELECT_ALL ))

//		mr.putMimic( "timeline.trimToSelection", this, doc.getTrimAction() )
		mr.putMimic( "timeline.insertSpan", this, new ActionInsertSpan )
//		mr.putMimic( "timeline.clearSpan", this, new ActionClearSpan )
//		mr.putMimic( "timeline.removeSpan", this, new ActionRemoveSpan )
		mr.putMimic( "timeline.splitObjects", this, new ActionSplitObjects )
      mr.putMimic( "timeline.selFollowingObj", this, new ActionSelectFollowingObjects )
      mr.putMimic( "timeline.alignObjStartToPos", this, new ActionAlignObjectsStartToTimelinePosition )

      mr.putMimic( "actions.showInEisK", this, new ActionShowInEisK )

      makeUnifiedLook
      init()
//  	  updateTitle
//      documentUpdate

      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

	override protected def alwaysPackSize() = false

  //	protected def documentUpdate {
  //    // nada
  //  }
   
   protected def elementName = Some( tl.name )

   protected def windowClosing { dispose }

	private def initBounds {
		val cp	= getClassPrefs()
		val bwh	= getWindowHandler()
		val sr	= bwh.getWindowSpace()
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
		val winSize = getSize()
		val wr = new Rectangle( TimelineFrame.lastLeftTop.x + 21, TimelineFrame.lastLeftTop.y + 23,
                                winSize.width, winSize.height )
		GUIUtil.wrapWindowBounds( wr, sr )
		TimelineFrame.lastLeftTop.setLocation( wr.getLocation() )
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

   private class ActionInsertSpan extends MenuAction {
      private var value: Option[ Param ] = None
      private var space: Option[ ParamSpace ] = None

      def actionPerformed( e: ActionEvent ) : Unit = perform

      def perform {
			val msgPane     = new SpringPanel( 4, 2, 4, 2 )
			val timeTrans   = new DefaultUnitTranslator()
			val ggDuration  = new ParamField( timeTrans )
			ggDuration.addSpace( ParamSpace.spcTimeHHMMSS )
			ggDuration.addSpace( ParamSpace.spcTimeSmps )
			ggDuration.addSpace( ParamSpace.spcTimeMillis )
			ggDuration.addSpace( ParamSpace.spcTimePercentF )
			msgPane.gridAdd( ggDuration, 0, 0 )
			msgPane.makeCompactGrid()
			GUIUtil.setInitialDialogFocus( ggDuration )

         val tl = timelineView.timeline
			timeTrans.setLengthAndRate( tl.span.getLength, tl.rate )

         ggDuration.setValue( value getOrElse new Param( 60.0, ParamSpace.TIME | ParamSpace.SECS ))
			space.foreach( sp => ggDuration.setSpace( sp ))

			val op = new JOptionPane( msgPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
			val result = BasicWindowHandler.showDialog( op, getWindow, getValue( Action.NAME ).toString )

			if( result == JOptionPane.OK_OPTION ) {
            val v = ggDuration.getValue
				value	= Some( v )
				space	= Some( ggDuration.getSpace )
				val durationSmps = timeTrans.translate( v, ParamSpace.spcTimeSmps ).`val`
				if( durationSmps > 0.0 ) {
//		         final ProcessingThread proc;

//		   	   proc =
               val pos = timelineView.cursor.position
               initiate( new Span( pos, pos + durationSmps.toLong ))
//				   if( proc != null ) start( proc );
				}
			} else {
            value = None
            space = None
         }
		}

      private def editName : String = {
         val name = getValue( Action.NAME ).toString
         if( name.endsWith( "..." )) name.substring( 0, name.length - 3 ) else name
      }

      def initiate( span: Span ) {
			if( /* !checkProcess() ||*/ span.isEmpty ) return

    		val tl      = timelineView.timeline
         val pos     = span.start
         val delta   = span.getLength

			if( (pos < tl.span.start) || (pos > tl.span.stop) ) error( span.toString )

         val affectedSpan = new Span( pos, tl.span.stop )

         tl.editor.foreach( ed => {
            val ce = ed.editBegin( editName )
            try {
               ed.editSpan( ce, tl.span.replaceStop( tl.span.stop + delta ))
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
//                   val stakes = tvCast.selectedStakes.toList
//                   val moed = stakes.map( _.move( ))
                     val stakes = tvCast.trail.getRange( affectedSpan )
//                   val toDeselect = stakes.filter( tvCast.isSelected( _ ))
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
               case e => { ed.editCancel( ce ); throw e }
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
		def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
			val visiSpan	= timelineView.span
			val visiLen		= visiSpan.getLength
			val pos			= timelineView.cursor.position
            val timelineLen = timelineView.timeline.span.getLength

//			if( factor == 0.0 ) {				// to sample level
//				start	= Math.max( 0, pos - (wavePanel.getWidth() >> 1) );
//				stop	= Math.min( timelineLen, start + wavePanel.getWidth() );
//			} else
            val newVisiSpan = if( factor < 1.0 ) {		// zoom in
				if( visiLen < 4 ) new Span()
                else {
                  // if timeline pos visible -> try to keep it's relative position constant
                  if( visiSpan.contains( pos )) {
					val start	= pos - ((pos - visiSpan.start) * factor + 0.5).toLong
                  	val stop    = start + (visiLen * factor + 0.5).toLong
                    new Span( start, stop )
                  // if timeline pos before visible span, zoom left hand
                  } else if( visiSpan.start > pos ) {
                     val start	= visiSpan.start
					 val stop    = start + (visiLen * factor + 0.5).toLong
                     new Span( start, stop )
                  // if timeline pos after visible span, zoom right hand
                  } else {
					val stop	= visiSpan.stop
					val start   = stop - (visiLen * factor + 0.5).toLong
                    new Span( start, stop )
				}
                }
			} else {			// zoom out
				val start   = max( 0, visiSpan.start - (visiLen * factor/4 + 0.5).toLong )
				val stop    = min( timelineLen, start + (visiLen * factor + 0.5).toLong )
                new Span( start, stop )
			}
			if( !newVisiSpan.isEmpty ) {
                timelineView.editor.foreach( ed => {
                    val ce = ed.editBegin( "scroll" )
                    ed.editScroll( ce, newVisiSpan )
                    ed.editEnd( ce )
                })
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
		def actionPerformed( e: ActionEvent ) : Unit = perform

		private def perform {
			val selSpan = timelineView.selection.span
			if( selSpan.isEmpty ) return

            timelineView.editor.foreach( ed => {
    			val ce = ed.editBegin( "position" )
                if( deselect ) ed.editSelect( ce, new Span() )
                val pos = (selSpan.start + selSpan.getLength * weight + 0.5).toLong
                ed.editPosition( ce, pos )
                ed.editEnd( ce )
            })
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

		def actionPerformed( e: ActionEvent ) : Unit = perform

		def perform {
            timelineView.editor.foreach( ed => {
                val pos       = timelineView.cursor.position
                val visiSpan  = timelineView.span
                val wholeSpan = timelineView.timeline.span
                if( mode == SCROLL_SESSION_START ) {
//                if( transport.isRunning ) transport.stop
                  val posNotZero = pos != wholeSpan.start
                  val zeroNotVisi = !visiSpan.contains( wholeSpan.start )
                  if( posNotZero || zeroNotVisi ) {
                    val ce = ed.editBegin( "scroll" )
                    if( posNotZero ) ed.editPosition( ce, wholeSpan.start )
                    if( zeroNotVisi ) {
                      ed.editScroll( ce, new Span( wholeSpan.start, wholeSpan.start + visiSpan.getLength ))
                    }
          			ed.editEnd( ce )
                  }
                } else {
        		  val selSpan = timelineView.selection.span
                  val newSpan = mode match {
        			case SCROLL_SELECTION_START => {
                		val selSpanStart = if( selSpan.isEmpty ) pos else selSpan.start
                        val start = max( wholeSpan.start, selSpanStart - (visiSpan.getLength >>
                          (if( visiSpan.contains( selSpanStart )) 1 else 3)) )
                        val stop = min( wholeSpan.stop, start + visiSpan.getLength )
                        new Span( start, stop )
                    }
        			case SCROLL_SELECTION_STOP => {
                		val selSpanStop = if( selSpan.isEmpty ) pos else selSpan.stop
                        val stop = min( wholeSpan.stop, selSpanStop + (visiSpan.getLength >>
                          (if( visiSpan.contains( selSpanStop )) 1 else 3)) )
                        val start = max( wholeSpan.start, stop - visiSpan.getLength )
                        new Span( start, stop )
                    }
          			case SCROLL_FIT_TO_SELECTION => selSpan
        			case SCROLL_ENTIRE_SESSION => timelineView.timeline.span
            		case _ => throw new IllegalArgumentException( mode.toString )
	            }
				if( (visiSpan != newSpan) && !newSpan.isEmpty ) {
                    val ce = ed.editBegin( "scroll" )
					ed.editScroll( ce, newSpan )
          			ed.editEnd( ce )
                }
            }
            })
		}
	} // class actionScrollClass

    private object ActionSelect {
      val SELECT_TO_SESSION_START	= 0
      val SELECT_TO_SESSION_END		= 1
      val SELECT_ALL                = 2
    }

	private class ActionSelect( mode: Int )
	extends AbstractAction {
		import ActionSelect._

		def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
          timelineView.editor.foreach( ed => {
            val pos = timelineView.cursor.position
			val selSpan = if( timelineView.selection.span.isEmpty ) {
              new Span( pos, pos )
            } else {
              timelineView.selection.span
            }

            val wholeSpan = timelineView.timeline.span
            val newSpan = mode match {
			case SELECT_TO_SESSION_START => new Span( wholeSpan.start, selSpan.stop )
			case SELECT_TO_SESSION_END => new Span( selSpan.start, wholeSpan.stop )
            case SELECT_ALL => wholeSpan
            case _ => throw new IllegalArgumentException( mode.toString )
			}
			if( newSpan != selSpan ) {
              val ce = ed.editBegin( "select" )
              ed.editSelect( ce, newSpan )
              ed.editEnd( ce )
			}
          })
		}
	} // class actionSelectClass

//    private class ActionDebugGenerator
//    extends MenuAction( "Debug Generator" ) {
//        def actionPerformed( e: ActionEvent ) : Unit = debugGenerator
//    }

    private class ActionCut
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
            println( "CUT NOT YET IMPLEMENTED")
        }
    }

    private class ActionCopy
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
            println( "COPY NOT YET IMPLEMENTED")
        }
    }

    private class ActionPaste
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
            println( "PASTE NOT YET IMPLEMENTED")
        }
    }

    private class ActionDelete
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) : Unit = perform

        def perform {
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

    private class ActionSplitObjects
    extends MenuAction {
        def actionPerformed( e: ActionEvent ) : Unit = perform

    	def perform {
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
      def actionPerformed( e: ActionEvent ) : Unit = perform

      def perform {
         val pos	= timelineView.cursor.position
         val stop = timelineView.timeline.span.getLength
         val span = new Span( pos, stop )
         timelineView.timeline.editor.foreach( ed => {
            val ce = ed.editBegin( getValue( Action.NAME ).toString )
            tracksPanel.foreach( elem => {
               val track = elem.track // "stable"
               if( elem.selected ) {
                  val tvCast = elem.trailView.asInstanceOf[ TrailView[ track.T ]]
                  tvCast.editor.foreach( ed2 => {
                     val trail      = tvCast.trail
                     var toSelect   = trail.getRange( span, true, false )
                     ed2.editSelect( ce, toSelect: _* )
                  })
               }
            })
            ed.editEnd( ce )
         })
      }
   }

   private class ActionAlignObjectsStartToTimelinePosition extends MenuAction {
      def actionPerformed( e: ActionEvent ) : Unit = perform

      def perform {
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
      def actionPerformed( e: ActionEvent ) : Unit = perform

      def perform {
         tracksPanel.find( _.trailView.selectedStakes.headOption match {
            case Some( ar: AudioRegion ) => {
               val delta      = ar.offset - ar.span.start
               val cursor     = Some( timelineView.cursor.position + delta )
               val selection  = if( timelineView.selection.span.isEmpty ) None else
                  Some( timelineView.selection.span.shift( delta ))
               EisenkrautClient.instance.openAudioFile( ar.audioFile.path, cursor, selection )
               true
            }
            case _ => false
         })
      }
   }

   private class ActionBounce extends MenuAction {
      def actionPerformed( e: ActionEvent ) {
         query.foreach( tup => {
            val( tls, span, afd ) = tup
            perform( tls, span, afd )
         })
      }

      def perform( tracks: List[ Track ], span: Span, descr: AudioFileDescr ) {
         val ggProgress = new JProgressBar()
         val name = getValue( Action.NAME ).toString
         val ggCancel = new JButton( getResourceString( "buttonAbort" ))
         ggCancel.setFocusable( false )
         val options = Array[ AnyRef]( ggCancel )
         val op = new JOptionPane( ggProgress, JOptionPane.INFORMATION_MESSAGE, 0, null, options )
         def fDispose() { val w = SwingUtilities.getWindowAncestor( op ); if( w != null ) w.dispose }
         ggCancel.addActionListener( new ActionListener {
            def actionPerformed( e: ActionEvent ) { fDispose }
         })
         var done = false
         try {
            val process = SessionUtil.bounce( doc, timelineView.timeline, tracks, span, descr, msg => msg match {
               case "done" => { done = true; fDispose() }
               case ("progress", i: Int) => ggProgress.setValue( i )
//               case _ => println( "received: " + msg ) 
            })
            BasicWindowHandler.showDialog( op, getWindow, name )
            if( !done ) process.cancel
         }
         catch { case e: Exception =>
            fDispose()
            BasicWindowHandler.showErrorDialog( frame.getWindow, e, name )}
      }

      def query: Option[ Tuple3[ List[ Track ], Span, AudioFileDescr ]] = {
         val trackElems    = tracksPanel.toList
         val numTracks     = trackElems.size
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
            val result2 = BasicWindowHandler.showDialog( op2, getWindow, name )
            if( op2.getValue != opOverwrite ) return None
         }

         val all        = bg.isSelected( ggAll.getModel )
//         val descr      = new AudioFileDescr
         affp.toDescr( descr )
         descr.rate     = timelineView.timeline.rate
         descr.file     = path
         val tracks     = (if( all ) trackElems else selTrackElems).map( _.track )
         descr.channels = tracks.collect( _ match { case at: AudioTrack if( at.diffusion.isDefined ) => at.diffusion.get })
            .foldLeft( 0 )( (maxi, diff) => max( maxi, diff.numOutputChannels ))

         Some( (tracks, if( all ) span else selSpan, descr) )
      }
   }
}