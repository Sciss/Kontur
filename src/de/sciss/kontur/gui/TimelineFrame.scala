/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.common.{ BasicApplication, ShowWindowAction , BasicWindowHandler}
import de.sciss.gui.{ GUIUtil, MenuAction, ParamField, SpringPanel}
import de.sciss.io.{ Span }
import de.sciss.kontur.session.{ Session, Timeline }
import de.sciss.util.{ DefaultUnitTranslator, Param, ParamSpace }
import java.awt.event.{ ActionEvent }
import java.awt.{ BorderLayout, Dimension, Point, Rectangle }
import java.util.{ StringTokenizer }
import javax.swing.{ Action , JOptionPane}

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
    private val timelinePanel = new TimelinePanel( timelineView )
    private val trackPanel    = new TrackPanel( timelinePanel )

    // ---- constructor ----
    {
//      app.getMenuFactory().addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!
      val cp = getContentPane
      cp.add( trackPanel, BorderLayout.CENTER )

    		// ---- menus and actions ----
		val mr = app.getMenuBarRoot

//		mr.putMimic( "edit.cut", this, doc.getCutAction() )
//		mr.putMimic( "edit.copy", this, doc.getCopyAction() )
//		mr.putMimic( "edit.paste", this, doc.getPasteAction() )
//		mr.putMimic( "edit.clear", this, doc.getDeleteAction() )
//		mr.putMimic( "edit.selectAll", this, actionSelectAll )

		mr.putMimic( "timeline.insertSpan", this, new InsertSpanAction )
//		mr.putMimic( "timeline.trimToSelection", this, doc.getTrimAction() )

      init()
  	  updateTitle
      documentUpdate

      initBounds	// be sure this is after documentUpdate!

	  setVisible( true )
	  toFront()
    }

	override protected def alwaysPackSize() = false

  	protected def documentUpdate {
      // nada
    }

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

    private class InsertSpanAction extends MenuAction {
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

            ggDuration.setValue( value getOrElse new Param( 1.0, ParamSpace.TIME | ParamSpace.SECS ))
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
}