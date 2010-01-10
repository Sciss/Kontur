/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ BasicStroke, BorderLayout, Color, Dimension, FontMetrics, Graphics,
                 Graphics2D, Paint, Rectangle, RenderingHints, Stroke, TexturePaint,
                 Toolkit }
import java.awt.event.{ ActionEvent, KeyAdapter, KeyEvent, KeyListener, MouseEvent }
import java.awt.geom.{ GeneralPath }
import java.awt.image.{ BufferedImage }
import java.io.{ IOException }
import javax.swing.{ Action, ActionMap, InputMap, JButton, JComponent, JLabel,
                    JOptionPane, JPanel, JTextField, KeyStroke }
import javax.swing.event.{ AncestorEvent, MouseInputAdapter, MouseInputListener }
import scala.collection.mutable.{ ListBuffer }
import scala.math._

import de.sciss.app.{ AbstractApplication, AncestorAdapter, Application, BasicEvent,
                     DynamicAncestorAdapter, DynamicListening, EventManager,
                     GraphicsHandler }
import de.sciss.common.{ BasicWindowHandler }
import de.sciss.gui.{ ComponentHost, DoClickAction, MenuAction, ParamField,
                     SpringPanel }
import de.sciss.io.{ Span }
import de.sciss.util.{ DefaultUnitTranslator, Disposable, Param, ParamSpace }
import de.sciss.kontur.session.{ Marker, Trail, TrailEditor }

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.11, 09-Jan-10
 *
 *	@todo		uses TimelineListener to
 *				not miss document changes. should use
 *				a document change listener!
 *
 *	@todo		marker sortierung sollte zentral von session o.ae. vorgenommen
 *				werden sobald neues file geladen wird!
 *
 *	@todo		had to add 2 pixels to label y coordinate in java 1.5 ; have to check look back in 1.4
 *
 *	@todo		repaintMarkers : have to provide dirtySpan that accounts for flag width, esp. for dnd!
 *
 *	@todo		actionEditPrev/NextClass shortcuts funktionieren nicht
 */
object MarkerAxis {
	private val pntBarGradientPixels = Array[Int]( 0xFFB8B8B8, 0xFFC0C0C0, 0xFFC8C8C8, 0xFFD3D3D3,
												   0xFFDBDBDB, 0xFFE4E4E4, 0xFFEBEBEB, 0xFFF1F1F1,
												   0xFFF6F6F6, 0xFFFAFAFA, 0xFFFBFBFB, 0xFFFCFCFC,
												   0xFFF9F9F9, 0xFFF4F4F4, 0xFFEFEFEF )
	private val barExtent = pntBarGradientPixels.length

	private val pntMarkGradientPixels = Array[Int]( 0xFF5B8581, 0xFF618A86, 0xFF5D8682, 0xFF59827E,
													0xFF537D79, 0xFF4F7975, 0xFF4B7470, 0xFF47716D,
													0xFF446E6A, 0xFF426B67, 0xFF406965, 0xFF3F6965,
													0xFF3F6864 )

    private val pntMarkDragPixels = pntMarkGradientPixels.map( _ & 0xBFFFFFFF ) // = 50% alpha

	private val colrLabel		= Color.white
	private val colrLabelDrag	= new Color( 0xFF, 0xFF, 0xFF, 0xBF )

	private val pntMarkStick    = new Color( 0x31, 0x50, 0x4D, 0x7F );
	private val pntMarkStickDrag = new Color( 0x31, 0x50, 0x4D, 0x5F );
	private val strkStick       = new BasicStroke( 1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
		1.0f, Array( 4.0f, 4.0f ), 0.0f )

	private val markExtent = pntMarkGradientPixels.length
}

class MarkerAxis( timelineView: TimelineView, host: ComponentHost )
extends JComponent
with DynamicListening with Disposable {

    import MarkerAxis._

//	private String[]			markLabels		= new String[0];
//	private int[]				markFlagPos		= new int[0];
//	private var numMarkers		= 0
	protected val shpFlags		= new GeneralPath()
	private var recentWidth		= -1
	private var doRecalc		= true
	protected var visibleSpan = timelineView.span

	private val img1		= new BufferedImage( 1, barExtent, BufferedImage.TYPE_INT_ARGB )
	img1.setRGB( 0, 0, 1, barExtent, pntBarGradientPixels, 0, 1 )
	private val pntBackground = new TexturePaint( img1, new Rectangle( 0, 0, 1, barExtent ))
	private val img2		= new BufferedImage( 1, markExtent, BufferedImage.TYPE_INT_ARGB )
	img2.setRGB( 0, 0, 1, markExtent, pntMarkGradientPixels, 0, 1 );
	private val pntMarkFlag	= new TexturePaint( img2, new Rectangle( 0, 0, 1, markExtent ))
	private val img3		= new BufferedImage( 1, markExtent, BufferedImage.TYPE_INT_ARGB )
	img3.setRGB( 0, 0, 1, markExtent, pntMarkDragPixels, 0, 1 );
	private val pntMarkFlagDrag = new TexturePaint( img3, new Rectangle( 0, 0, 1, markExtent ))

	private var isListening	= false

    private val marks       = new ListBuffer[ Mark ]()

	// ----- Edit-Marker Dialog -----
	private var editMarkerPane: Option[ JPanel ] = None
//	private Object[]					editOptions		= null;
//	private ParamField					ggMarkPos;
//	protected JTextField				ggMarkName;
//	private JButton						ggEditPrev, ggEditNext;
//	protected int						editIdx			= -1;
//	private DefaultUnitTranslator		timeTrans;

	// ---- dnd ----
    private var drag: Option[ Drag ] = None

    private var trailVar: Option[ Trail[ Marker ]] = None
    private var editorVar: Option[ TrailEditor[ Marker ]] = None

//	private val elm = new EventManager( this )
//	protected Trail.Editor				editor			= null;

	private val mil = new MouseInputAdapter() {
			override def mousePressed( e: MouseEvent ) {
				val scale	= visibleSpan.getLength.toDouble / max( 1, getWidth )
				val pos		= (e.getX * scale + visibleSpan.start + 0.5).toLong

				if( shpFlags.contains( e.getPoint )) {
					if( e.isAltDown ) {					// delete marker
						removeMarkerLeftTo( pos + 1 )
					} else if( e.getClickCount == 2 ) {	// rename
//						editMarkerLeftTo( pos + 1 )
					} else {								// start drag
                        getMarkerLeftTo( pos + 1 ).foreach( m => {
                           drag = Some( new Drag( m, e.getX ))
//							dispatchEvent( Event.DRAGSTARTED, dragMark.span )
							requestFocus()
						})
					}

				} else if( !e.isAltDown && (e.getClickCount == 2) ) {		// insert marker
					addMarker( pos )
				}
			}

      		override def mouseReleased( e: MouseEvent ) {
                drag.foreach( d => {
//					dispatchEvent( Event.DRAGSTOPPED, (d.lastMark getOrElse d.firstMark).span )

                     d.lastMark.foreach( lastMark => {
                         editorVar.foreach( ed => {
        					val id = ed.editBegin( MarkerAxis.this, getResourceString( "editMoveMarker" ))
        					try {
                        		ed.editRemove( id, d.firstMark )
                                ed.editAdd( id, lastMark )
                            	ed.editEnd( id )
                            }
                            catch { case e1: IOException => {
        						System.err.println( e1 )
                				ed.editCancel( id )
                            }}
                         })
					})
                    drag = None
				})
			}

			override def mouseDragged( e: MouseEvent ) {
              drag.foreach( d => {
				if( !d.started ) {
					if( abs( e.getX - d.startX ) < 5 ) return
					d.started = true
				}

				val oldPos  = (d.lastMark getOrElse d.firstMark).pos
				val scale	= getWidth.toDouble / visibleSpan.getLength
				val newPos	= timelineView.timeline.span.clip( ((e.getX - d.startX) / scale + d.firstMark.pos + 0.5).toLong )

				if( oldPos == newPos ) return

				val dirtySpan = new Span( min( oldPos, newPos ), max( oldPos, newPos ))
				d.lastMark = Some( Marker( newPos, d.firstMark.name ))
//				dispatchEvent( Event.DRAGADJUSTED, dirtySpan )
			  })
            }
		}

	private val kl = new KeyAdapter() {
		    override def keyPressed( e: KeyEvent ) {
				if( e.getKeyCode == KeyEvent.VK_ESCAPE ) {
                  if( drag.isDefined ) {
                    drag = None
//					dispatchEvent( Event.DRAGSTOPPED, visibleSpan )
                  }
				}
			}
    }

	/**
	 *  Constructs a new object for
	 *  displaying the timeline ruler
	 *
	 *  @param  root	application root
	 *  @param  doc		session Session
	 */
	{
		setMaximumSize( new Dimension( getMaximumSize.width, barExtent ))
		setMinimumSize( new Dimension( getMinimumSize.width, barExtent ))
		setPreferredSize( new Dimension( getPreferredSize.width, barExtent ))

		setOpaque( true )
 		setFont( AbstractApplication.getApplication().getGraphicsHandler().getFont(
            GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_MINI ))

        new DynamicAncestorAdapter( this ).addTo( this )
	}

    def trail = trailVar
	def trail_=( newTrail: Option[ Trail[ Marker ]]) {
        trailVar.foreach( t => {
            if( isListening ) t.removeListener( trailListener )
		})
        trailVar = newTrail
        newTrail.foreach( t => {
            if( isListening ) t.addListener( trailListener )
		})
		triggerRedisplay
	}

    def editor = editorVar
	def editor_=( newEditor: Option[ TrailEditor[ Marker ]]) {
		if( editorVar != newEditor ) {
          if( editorVar.isDefined && newEditor.isEmpty ) {
              removeMouseListener( mil )
              removeMouseMotionListener( mil )
              removeKeyListener( kl )
          } else if( editorVar.isEmpty && newEditor.isDefined ) {
              addMouseListener( mil )
              addMouseMotionListener( mil )
              addKeyListener( kl )
          }
          editorVar = newEditor
		}
	}

	protected def getResourceString( key: String ) =
		AbstractApplication.getApplication().getResourceString( key )

	private def recalcDisplay( fm: FontMetrics ) {
		val scale = recentWidth.toDouble / visibleSpan.getLength

		shpFlags.reset()
        marks.clear
        trailVar.foreach( t => {
          t.visitRange( visibleSpan ) { m =>
            val flagPos = (((m.pos - visibleSpan.start) * scale) + 0.5).toInt
            marks += Mark( flagPos, m.name )
			shpFlags.append( new Rectangle( flagPos, 1, fm.stringWidth( m.name ) + 8, markExtent ), false )
          }
        })
		doRecalc = false
	}

	override def paintComponent( g: Graphics ) {
		super.paintComponent( g )

		val g2 = g.asInstanceOf[ Graphics2D ]
		val fm	= g2.getFontMetrics
		val y	= fm.getAscent + 2

		if( doRecalc || (recentWidth != getWidth()) ) {
			recentWidth = getWidth
			recalcDisplay( fm )
		}

		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF )

		g2.setPaint( pntBackground )
		g2.fillRect( 0, 0, recentWidth, barExtent )

		g2.setPaint( pntMarkFlag )
		g2.fill( shpFlags )

		g2.setColor( colrLabel )
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )

        marks.foreach( m => {
			g2.drawString( m.label, m.flagPos + 4, y )
		})

		// handle dnd graphics
        drag.foreach( _.lastMark.foreach( lastMark => {
			val dragMarkFlagPos = (((lastMark.pos - visibleSpan.start) * recentWidth.toDouble / visibleSpan.getLength) + 0.5).toInt
			g2.setPaint( pntMarkFlagDrag )
			g2.fillRect( dragMarkFlagPos, 1, fm.stringWidth( lastMark.name ) + 8, markExtent )
			g2.setColor( colrLabelDrag )
			g2.drawString( lastMark.name, dragMarkFlagPos + 4, y )
        }))
	}

	def paintFlagSticks( g2: Graphics2D, bounds: Rectangle ) {
		if( doRecalc ) {
			recalcDisplay( g2.getFontMetrics )	// XXX nicht ganz sauber (anderer graphics-context!)
		}

		val strkOrig	= g2.getStroke

		g2.setPaint( pntMarkStick )
		g2.setStroke( strkStick )
        marks.foreach( m => {
			g2.drawLine( m.flagPos, bounds.y, m.flagPos, bounds.y + bounds.height )
		})
        drag.foreach( _.lastMark.foreach( lastMark => {
			val dragMarkFlagPos = (((lastMark.pos - visibleSpan.start) * recentWidth.toDouble / visibleSpan.getLength) + 0.5).toInt
			g2.setPaint( pntMarkStickDrag )
			g2.drawLine( dragMarkFlagPos, bounds.y, dragMarkFlagPos, bounds.y + bounds.height )
		}))
		g2.setStroke( strkOrig )
	}

	private def triggerRedisplay() {
		doRecalc = true
//		if( host != null ) {
			host.update( this )
//		} else if( isVisible() ) {
//			repaint();
//		}
	}

	def addMarker( pos: Long ) {
		if( editorVar.isEmpty ) throw new IllegalStateException()

        val ed          = editorVar.get
		val posC		= timelineView.timeline.span.clip( pos )
		val id          = ed.editBegin( this, getResourceString( "editAddMarker" ))
		try {
			ed.editAdd( id, new Marker( posC, "Mark" ))
			ed.editEnd( id )
		}
		catch { case e1: IOException => {	// should never happen
			e1.printStackTrace()
			ed.editCancel( id )
		}}
	}

	protected def removeMarkerLeftTo( pos: Long ) {
		if( editorVar.isEmpty ) throw new IllegalStateException()

		getMarkerLeftTo( pos ).foreach( m => {
          val posC	= timelineView.timeline.span.clip( pos )
          val ed      = editorVar.get
          val id      = ed.editBegin( this, getResourceString( "editDeleteMarker" ))
          try {
              ed.editRemove( id, m )
              ed.editEnd( id )
          }
          catch { case e1: IOException => {	// should never happen
              e1.printStackTrace()
              ed.editCancel( id )
          }}
        })
	}
/*
	protected def editMarkerLeftTo( pos: Long ) {
		if( editor.isEmpty ) throw new IllegalStateException()

		final int result;

		editIdx		= trail.indexOf( pos )
		if( editIdx < 0 ) {
			editIdx = -(editIdx + 2);
			if( editIdx == -1 ) return;
		}

		if( editMarkerPane == null ) {
			final SpringPanel		spring;
			final ActionMap			amap;
			final InputMap			imap;
			JLabel					lb;
			KeyStroke				ks;
			Action					a;

			spring			= new SpringPanel( 4, 2, 4, 2 );
			ggMarkName		= new JTextField( 24 );
			ggMarkName.addAncestorListener( new AncestorAdapter() {
				public void ancestorAdded( AncestorEvent e ) {
					ggMarkName.requestFocusInWindow();
					ggMarkName.selectAll();
				}
			});

			timeTrans		= new DefaultUnitTranslator();
			ggMarkPos		= new ParamField( timeTrans );
			ggMarkPos.addSpace( ParamSpace.spcTimeHHMMSS );
			ggMarkPos.addSpace( ParamSpace.spcTimeSmps );
			ggMarkPos.addSpace( ParamSpace.spcTimeMillis );
			ggMarkPos.addSpace( ParamSpace.spcTimePercentF );

			lb				= new JLabel( getResourceString( "labelName" ));
			spring.gridAdd( lb, 0, 0 );
			spring.gridAdd( ggMarkName, 1, 0 );
			lb				= new JLabel( getResourceString( "labelPosition" ));
			spring.gridAdd( lb, 0, 1 );
			spring.gridAdd( ggMarkPos, 1, 1, -1, 1 );
			spring.makeCompactGrid();
			editMarkerPane	= new JPanel( new BorderLayout() );
			editMarkerPane.add( spring, BorderLayout.NORTH );

			amap			= spring.getActionMap();
			imap			= spring.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
			ks				= KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() );
			imap.put( ks, "prev" );
			a				= new ActionEditPrev();
			ggEditPrev		= new JButton( a );
			amap.put( "prev", new DoClickAction( ggEditPrev ));
			ks				= KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() );
			imap.put( ks, "next" );
			a				= new ActionEditNext();
			ggEditNext		= new JButton( a );
			amap.put( "next", new DoClickAction( ggEditNext ));

			editOptions		= new Object[] { ggEditNext, ggEditPrev, getResourceString( "buttonOk" ), getResourceString( "buttonCancel" )};
		}

		final Timeline tl = view.getTimeline();
		timeTrans.setLengthAndRate( tl.getSpan().getLength(), tl.getRate() );

		updateEditMarker();

		final JOptionPane op = new JOptionPane( editMarkerPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
		                                        null, editOptions, editOptions[ 2 ]);
		result = BasicWindowHandler.showDialog( op, BasicWindowHandler.getWindowAncestor( this ), getResourceString( "inputDlgEditMarker" ));

		if( result == 2 ) {
			commitEditMarker();
		}
	}

	protected void updateEditMarker()
	{
		if( trail == null ) throw new IllegalStateException();

		final MarkerStake mark = (MarkerStake) trail.get( editIdx, true );
		if( mark == null ) return;

		ggMarkPos.setValue( new Param( mark.pos, ParamSpace.TIME | ParamSpace.SMPS ));
		ggMarkName.setText( mark.name );

		ggEditPrev.setEnabled( editIdx > 0 );
		ggEditNext.setEnabled( (editIdx + 1) < trail.getNumStakes() );

		ggMarkName.requestFocusInWindow();
		ggMarkName.selectAll();
	}

	protected void commitEditMarker()
	{
		if( (editor == null) || (trail == null) ) throw new IllegalStateException();

		final MarkerStake mark = (MarkerStake) trail.get( editIdx, true );
		if( mark == null ) return;

		final long positionSmps;

		positionSmps	= (long) timeTrans.translate( ggMarkPos.getValue(), ParamSpace.spcTimeSmps ).val;
		if( (positionSmps == mark.pos) && (ggMarkName.getText().equals( mark.name ))) return; // no change

		final int id = editor.editBegin( this, getResourceString( "editEditMarker" ));
		try {
			editor.editRemove( id, mark );
			editor.editAdd( id, new MarkerStake( positionSmps, ggMarkName.getText() ));
			editor.editEnd( id );
		}
		catch( IOException e1 ) {	// should never happen
			e1.printStackTrace();
			editor.editCancel( id );
		}
	}
*/

	protected def getMarkerLeftTo( pos: Long ) : Option[ Marker ] = {
		if( trailVar.isEmpty ) throw new IllegalStateException()

        trailVar.map( t => {
    		var idx = t.indexOfPos( pos )
            if( idx < 0 ) {
              idx = -(idx + 2)
              if( idx == -1 ) return None
          }
          t.get( idx )
        })
	}

//	protected void dispatchEvent( int id, Span modSpan )
//	{
//		if( elm != null ) {
//			elm.dispatchEvent( new Event( this, id, System.currentTimeMillis(), this, modSpan ));
//		}
//	}

//	public void addListener( Listener l )
//	{
//		elm.addListener( l );
//	}
//
//	public void removeListener( Listener l )
//	{
//		elm.removeListener( l );
//	}

	// -------------- EventManager.Processor interface --------------
//	public void processEvent( BasicEvent e )
//	{
//		for( int i = 0; i < elm.countListeners(); i++ ) {
//			final Listener l = (Listener) elm.getListener( i );
//			switch( e.getID() ) {
//			case Event.DRAGSTARTED:
//				l.markerDragStarted( (Event) e );
//				break;
//			case Event.DRAGSTOPPED:
//				l.markerDragStopped( (Event) e );
//				break;
//			case Event.DRAGADJUSTED:
//				l.markerDragAdjusted( (Event) e );
//				break;
//			default:
//				assert false : e.getID();
//			}
//		}
//	}

	// -------------- Disposable interface --------------

	def dispose {
		stopListening
		editor = None
		trail  = None
//		markLabels	= null;
//		markFlagPos	= null;
		shpFlags.reset()
		img1.flush()
		img2.flush()
		img3.flush()
	}

// ---------------- DynamicListening interface ----------------

    def startListening() {
		if( !isListening ) {
			timelineView.addListener( timelineListener )
            trailVar.foreach( _.addListener( trailListener ))
			triggerRedisplay
			isListening = true
		}
    }

    def stopListening() {
		if( isListening ) {
            trailVar.foreach( _.removeListener( trailListener ))
			timelineView.removeListener( timelineListener )
			isListening = false
		}
    }

// ---------------- MarkerManager.Listener interface ----------------

	private def trailListener( msg: AnyRef ) {
      msg match {
        case Trail.Changed( span ) => if( span.touches( visibleSpan )) triggerRedisplay
        case _ =>
      }
	}

// ---------------- TimelineListener interface ----------------

	private def timelineListener( msg: AnyRef ) {
      msg match {
        case TimelineView.SpanChanged( _, newSpan ) => {
            visibleSpan = newSpan
            triggerRedisplay
        }
        case _ =>
      }
	}
    
// ---------------- internal classes ----------------

//   	public interface Listener
//   	{
//   		public void markerDragStarted( Event e );
//   		public void markerDragStopped( Event e );
//   		public void markerDragAdjusted( Event e );
//   	}

/*
	public static class Event
	extends BasicEvent
	{
		public static final int DRAGSTARTED		= 1;
		public static final int DRAGSTOPPED		= 2;
		public static final int DRAGADJUSTED	= 3;

		private final MarkerAxis	axis;
		private final Span			modSpan;

		public Event( Object source, int id, long when, MarkerAxis axis, Span modSpan )
		{
			super( source, id, when );
			this.axis 		= axis;
			this.modSpan	= modSpan;
		}

		public MarkerAxis getAxis()
		{
			return axis;
		}

		public Span getModificatioSpan()
		{
			return modSpan;
		}

		public boolean incorporate( BasicEvent oldEvent )
		{
			return( oldEvent instanceof Event &&
				this.getSource() == oldEvent.getSource() &&
				this.getID() == oldEvent.getID() &&
				this.axis == ((Event) oldEvent).axis );
			}
	}
*/

/*
	private class ActionEditPrev
	extends MenuAction
	{
		protected ActionEditPrev()
		{
			super( "\u21E0" );
		}

		public void actionPerformed( ActionEvent e )
		{
			commitEditMarker();
			if( editIdx > 0 ) {
				editIdx--;
				updateEditMarker();
			}
		}
	}

	private class ActionEditNext
	extends MenuAction
	{
		protected ActionEditNext()
		{
			super( "\u21E2", KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ));
		}

		public void actionPerformed( ActionEvent e )
		{
			commitEditMarker();
			if( (editIdx + 1) < trail.getNumStakes() ) {
				editIdx++;
				updateEditMarker();
			}
		}
	}
*/
    private class Drag( val firstMark: Marker, val startX: Int ) {
	  var lastMark: Option[ Marker ] = None
      var started = false
    }

    private case class Mark( flagPos: Int, label: String )
}