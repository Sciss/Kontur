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

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.common.{ BasicApplication, BasicMenuFactory, ShowWindowAction,
                        BasicWindowHandler}
import de.sciss.gui.{ GUIUtil, MenuAction, MenuGroup, MenuItem, SpringPanel}
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ AudioRegion, AudioTrack, Session, Stake, Timeline }
import de.sciss.util.{ DefaultUnitTranslator, Param, ParamSpace }
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent }
import java.awt.{ BorderLayout, Dimension, Point, Rectangle }
import java.util.{ StringTokenizer }
import javax.swing.{ AbstractAction, Action, Box, JComponent, JOptionPane, KeyStroke }
import scala.math._

object TimelineFrame {
  protected val lastLeftTop		= new Point()
  protected val KEY_TRACKSIZE	= "tracksize"
}

class TimelineFrame( doc: Session, tl: Timeline )
extends AppWindow( AbstractWindow.REGULAR ) {

//  	private var writeProtected	= false
//	private var wpHaveWarned	= false
//    private val actionShowWindow= new ShowWindowAction( this )
    private val timelineView  = new BasicTimelineView( doc, tl )
//    private val trackList     = new BasicTrackList( doc, timelineView )
    private val timelinePanel = new TimelinePanel( timelineView )
//    private val trailView     = new javax.swing.JLabel( "Trail" )
//    private val trailsView     = new BasicTrailsView( doc, tl.tracks )
    private val tracksPanel    = new TracksPanel( doc, timelinePanel )
    private val trackTools     = new TrackToolsPanel( tracksPanel, timelineView )

    // ---- constructor ----
    {
      tracksPanel.registerTools( trackTools )
      timelinePanel.viewPort    = Some( tracksPanel.getViewport )

//      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!
      val cp = getContentPane
      cp.add( tracksPanel, BorderLayout.CENTER )
      val topBox = Box.createHorizontalBox()
      topBox.add( trackTools )
      topBox.add( Box.createHorizontalGlue() )
      topBox.add( new TransportPanel( timelineView ))
      cp.add( topBox, BorderLayout.NORTH )

//      tracksPanel.tracks = Some( tl.tracks )

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

//    	imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_M, BasicMenuFactory.MENU_SHORTCUT ), "debuggen" )
//		amap.put( "debuggen", new ActionDebugGenerator )

    	// ---- menus and actions ----
		val mr = app.getMenuBarRoot

		mr.putMimic( "edit.undo", this, doc.getUndoManager.getUndoAction )
		mr.putMimic( "edit.redo", this, doc.getUndoManager.getRedoAction )
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

      makeUnifiedLook
      init()
  	  updateTitle
//      documentUpdate

      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

	override protected def alwaysPackSize() = false

  //	protected def documentUpdate {
  //    // nada
  //  }

	/**
	 *  Recreates the main frame's title bar
	 *  after a sessions name changed (clear/load/save as session)
	 */
	def updateTitle {
		setTitle( "Timeline : " + tl.name + " (" + doc.displayName + ")")
	}

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
				value			= Some( v )
				space			= Some( ggDuration.getSpace )
				val durationSmps = timeTrans.translate( v, ParamSpace.spcTimeSmps ).`val`
				if( durationSmps > 0.0 ) {
//					final ProcessingThread proc;

//					proc =
                    val pos = timelineView.cursor.position
                      initiate( new Span( pos, pos + durationSmps.toLong ))
//					if( proc != null ) start( proc );
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

    		val tl = timelineView.timeline

			if( (span.start < tl.span.start) || (span.start > tl.span.stop) ) throw new IllegalArgumentException( span.toString )

        tl.editor.foreach( ed => {
          val ce = ed.editBegin( editName )
          try {
            ed.editSpan( ce, tl.span.replaceStop( tl.span.stop + span.getLength ))
            timelineView.editor.foreach( ved => {
              if( timelineView.span.isEmpty ) {
        		ved.editScroll( ce, span )
              }
              ved.editSelect( ce, span )
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
            timelineView.timeline.editor.foreach( ed => {
                val ce = ed.editBegin( getValue( Action.NAME ).toString )
                val span = new Span( pos, pos )
                tracksPanel.foreach( elem => {
                      val track = elem.track // "stable"
                       val tvCast = elem.trailView.asInstanceOf[ TrailView[ track.T ]]
                       tvCast.editor.foreach( ed2 => {
                           val selectedStakes = ed2.view.selectedStakes
                           val trail = tvCast.trail
                           trail.editor.foreach( ted => {
//                            ted.editClearSpan( ce, span )( stake => tv.isSelected( span ))
//                            var toDeselect = trail.emptyList
                              var toDeselect: List[ track.T ] = Nil // = trail.emptyList
                              var toRemove   = trail.emptyList
                              var toAdd      = trail.emptyList
                              var toSelect   = trail.emptyList
                              trail.visitRange( span )( stake => {
                                  if( tvCast.isSelected( stake )) {
                                      toDeselect ::= stake
                                      toRemove ::= stake
//                                    val splitted = stake.split( pos )
                                      val stake1 = stake.replaceStop( pos )
                                      val stake2 = stake.replaceStart( pos )
                                      toAdd ::= stake1
                                      toAdd ::= stake2
                                      toSelect ::= stake1
                                      toSelect ::= stake2
                                  }
                              })
                              ed2.editDeselect( ce, toDeselect: _* )
                              ted.editRemove( ce, toRemove: _* )
                              ted.editAdd( ce, toAdd: _* )
                              ed2.editSelect( ce, toSelect: _* )
                           })
                   })
                })
                ed.editEnd( ce )
            })
    	}
    }
}