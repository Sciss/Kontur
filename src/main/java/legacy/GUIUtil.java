package legacy;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 *  This is a helper class containing utility static functions
 *  for common Swing / GUI operations
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.40, 11-Feb-10
 */
public class GUIUtil
{
	private static final double VERSION	= 0.40;
	private static final ResourceBundle resBundle = ResourceBundle.getBundle( "GUIUtilStrings" );
	private static final Preferences prefs = Preferences.userNodeForPackage( GUIUtil.class );

	private static Icon icnNoWrite = null;

    private GUIUtil() { /* empty */ }

	public static final Preferences getUserPrefs()
	{
		return prefs;
	}

	public static final double getVersion()
	{
		return VERSION;
	}

	public static final String getResourceString( String key )
	{
		try {
			return resBundle.getString( key );
		}
		catch( MissingResourceException e1 ) {
			return( "[Missing Resource: " + key + "]" );
		}
	}

	/**
	 *  Displays an error message dialog by
	 *  examining a given <code>Exception</code>. Returns
	 *  after the dialog was closed by the user.
	 *
	 *  @param  component   the component in which to open the dialog.
	 *						<code>null</code> is allowed in which case
	 *						the dialog will appear centered on the screen.
	 *  @param  exception   the exception that was thrown. the message's
	 *						text is displayed using the <code>getLocalizedMessage</code>
	 *						method.
	 *  @param  title		name of the action in which the error occurred
	 *
	 *  @see	javax.swing.JOptionPane#showOptionDialog( Component, Object, String, int, int, Icon, Object[], Object )
	 *  @see	java.lang.Throwable#getLocalizedMessage()
	 */
	public static void displayError( Component component, Exception exception, String title )
	{
		final StringBuffer		strBuf  = new StringBuffer( GUIUtil.getResourceString( "errException" ));
		String[]				options = { GUIUtil.getResourceString( "buttonOk" ),
											GUIUtil.getResourceString( "optionDlgStack" )};

		if( exception != null ) {
			final String			message	= exception.getClass().getName() + " - " + exception.getLocalizedMessage();
			final StringTokenizer	tok		= new StringTokenizer( message );
			int						lineLen = 0;
			String					word;
			strBuf.append( ":\n" );
			while( tok.hasMoreTokens() ) {
				word = tok.nextToken();
				if( lineLen > 0 && lineLen + word.length() > 40 ) {
					strBuf.append( "\n" );
					lineLen = 0;
				}
				strBuf.append( word );
				strBuf.append( ' ' );
				lineLen += word.length() + 1;
			}
		}
		if( JOptionPane.showOptionDialog( component, strBuf.toString(), title, JOptionPane.YES_NO_OPTION,
									      JOptionPane.ERROR_MESSAGE, null, options, options[0] ) == 1 ) {
			exception.printStackTrace();
		}
	}

//	/**
//	 *  Convenience method that will add new
//	 *  corresponding entries in a button's input and action map,
//	 *  such that a given <code>KeyStroke<code> will cause a
//	 *  <code>DoClickAction</code> to be performed on that button.
//	 *  The key stroke is performed whenever the button is in
//	 *  the current focussed window.
//	 *
//	 *  @param  comp	an <code>AbstractButton</code> to which a
//	 *					a new keyboard action is attached.
//	 *  @param  stroke  the <code>KeyStroke</code> which causes a
//	 *					click on the button.
//	 *
//	 *  @see	DoClickAction
//	 *  @see	javax.swing.JComponent#getInputMap( int )
//	 *  @see	javax.swing.JComponent#getActionMap()
//	 *  @see	javax.swing.JComponent#WHEN_IN_FOCUSED_WINDOW
//	 */
//	public static void createKeyAction( AbstractButton comp, KeyStroke stroke )
//	{
//		comp.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( stroke, "shortcut" );
//		comp.getActionMap().put( "shortcut", new DoClickAction( comp ));
//	}

	/**
	 *  Set a font for a container
	 *  and all children we can find
	 *  in this container (calling this
	 *  method recursively). This is
	 *  necessary because calling <code>setFont</code>
	 *  on a <code>JPanel</code> does not
	 *  cause the <code>Font</code> of the
	 *  gadgets contained in the panel to
	 *  change their fonts.
	 *
	 *  @param  c		the container to traverse
	 *					for children whose font is to be changed
	 *  @param  fnt		the new font to apply; if <code>null</code>
	 *					the current application's window handler's
	 *					default font is used
	 *
	 *  @see	java.awt.Component#setFont( Font )
	 */
	public static void setDeepFont( Container c, Font fnt )
	{
		final Component[] comp = c.getComponents();

//		if( fnt == null ) {
//			final Application app = AbstractApplication.getApplication();
//			if( app == null ) return;
//			fnt = app.getGraphicsHandler().getFont( GraphicsHandler.FONT_SYSTEM | GraphicsHandler.FONT_SMALL );
//		}

		c.setFont( fnt );
		for( int i = 0; i < comp.length; i++ ) {
			if( comp[ i ] instanceof Container ) {
				setDeepFont( (Container) comp[i], fnt );
			} else {
				comp[ i ].setFont( fnt );
			}
		}
	}

	public static void setPreferences( Container c, Preferences prefs )
	{
		final Component[] comp = c.getComponents();

		if( c instanceof PreferenceEntrySync ) {
			((PreferenceEntrySync) c).setPreferenceNode( prefs );
		}
		for( int i = 0; i < comp.length; i++ ) {
			if( comp[ i ] instanceof Container ) {
				setPreferences( (Container) comp[i], prefs );
			} else if( c instanceof PreferenceEntrySync ) {
				((PreferenceEntrySync) c).setPreferenceNode( prefs );
			}
		}
	}

    /**
     *  A debugging utility that prints to stdout the component's
     *  minimum, preferred, and maximum sizes.
	 *  This is taken from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>.
     */
    public static void printSizes( Component c )
	{
        System.err.println( "minimumSize   = " + c.getMinimumSize() );
        System.err.println( "preferredSize = " + c.getPreferredSize() );
        System.err.println( "maximumSize   = " + c.getMaximumSize() );
    }

    /**
     *  Aligns the first <code>rows</code> * <code>cols</code>
     *  components of <code>parent</code> in
     *  a grid. Each component is as big as the maximum
     *  preferred width and height of the components.
     *  The parent is made just big enough to fit them all.
	 *  <p>
	 *  The code is taken from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>.
	 *
     *  @param  rows		number of rows
     *  @param  cols		number of columns
     *  @param  initialX	x location to start the grid at
     *  @param  initialY	y location to start the grid at
     *  @param  xPad		x padding between cells
     *  @param  yPad		y padding between cells
     */
    public static void makeSpringGrid( Container parent, int rows, int cols,
									   int initialX, int initialY, int xPad, int yPad )
	{
        SpringLayout				layout;
		Spring						xPadSpring, yPadSpring, initialXSpring, initialYSpring;
		Spring						maxWidthSpring, maxHeightSpring;
		SpringLayout.Constraints	cons;
        SpringLayout.Constraints	lastCons		= null;
        SpringLayout.Constraints	lastRowCons		= null;
		int							max				= rows * cols;

		if( max == 0 ) return;

        try {
            layout = (SpringLayout) parent.getLayout();
        } catch( ClassCastException e1 ) {
            System.err.println( "The first argument to makeGrid must use SpringLayout." );
            return;
        }

        xPadSpring		= Spring.constant( xPad );
        yPadSpring		= Spring.constant( yPad );
        initialXSpring  = Spring.constant( initialX );
        initialYSpring  = Spring.constant( initialY );

        // Calculate Springs that are the max of the width/height so that all
        // cells have the same size.
        maxWidthSpring  = layout.getConstraints( parent.getComponent( 0 )).getWidth();
        maxHeightSpring = layout.getConstraints( parent.getComponent( 0 )).getWidth();
        for( int i = 1; i < max; i++ ) {
            cons			= layout.getConstraints( parent.getComponent( i ));
            maxWidthSpring  = Spring.max( maxWidthSpring, cons.getWidth() );
            maxHeightSpring = Spring.max( maxHeightSpring, cons.getHeight() );
        }

        // Apply the new width/height Spring. This forces all the
        // components to have the same size.
        for( int i = 0; i < max; i++ ) {
            cons = layout.getConstraints( parent.getComponent( i ));
            cons.setWidth( maxWidthSpring );
            cons.setHeight( maxHeightSpring );
        }

    	// Then adjust the x/y constraints of all the cells so that they
    	// are aligned in a grid.
        for( int i = 0; i < max; i++ ) {
        	cons = layout.getConstraints( parent.getComponent( i ));
        	if( i % cols == 0 ) {   // start of new row
        		lastRowCons = lastCons;
        		cons.setX( initialXSpring );
        	} else {				// x position depends on previous component
        		cons.setX( Spring.sum( lastCons.getConstraint( SpringLayout.EAST ), xPadSpring ));
        	}

        	if( i / cols == 0 ) {   // first row
        		cons.setY( initialYSpring );
        	} else {				// y position depends on previous row
        		cons.setY( Spring.sum( lastRowCons.getConstraint( SpringLayout.SOUTH ), yPadSpring ));
        	}
        	lastCons = cons;
        }

        // Set the parent's size.
        cons = layout.getConstraints( parent );
        cons.setConstraint( SpringLayout.SOUTH, Spring.sum( Spring.constant( yPad ),
                            lastCons.getConstraint( SpringLayout.SOUTH )));
        cons.setConstraint( SpringLayout.EAST, Spring.sum( Spring.constant( xPad ),
                            lastCons.getConstraint( SpringLayout.EAST )));
    }

    /**
     *  Aligns the first <code>rows</code> * <code>cols</code>
     *  components of <code>parent</code> in
     *  a grid. Each component in a column is as wide as the maximum
     *  preferred width of the components in that column;
     *  height is similarly determined for each row.
     *  The parent is made just big enough to fit them all.
	 *  <p>
	 *  The code is based on one from the
	 *  <A HREF="http://java.sun.com/docs/books/tutorial/uiswing/layout/example-1dot4/SpringUtilities.java">
	 *  Sun Swing Tutorial Site</A>. It was optimized and includes support for hidden components.
     *
     *  @param  rows		number of rows
     *  @param  cols		number of columns
     *  @param  initialX	x location to start the grid at
     *  @param  initialY	y location to start the grid at
     *  @param  xPad		x padding between cells
     *  @param  yPad		y padding between cells
	 *
	 *	@warning	the spring layout seems to accumulate information; this method should not be called
	 *				many times on the same spring layout, it will substantially slow down the layout
	 *				process. though it's not very elegant, to solve this problem - e.g. in TimelineFrame -,
	 *				simply create set a panel's layout manager to a new SpringLayout before calling this
	 *				method!
     */
    public static void makeCompactSpringGrid( Container parent, int rows, int cols,
											  int initialX, int initialY, int xPad, int yPad )
	{
		SpringLayout				layout;
		Spring						x, y, width, height;
		SpringLayout.Constraints	constraints;
		Component					comp;
		boolean						anyVisible;

		try {
			layout = (SpringLayout) parent.getLayout();
		} catch( ClassCastException e1 ) {
			System.err.println( "The first argument to makeCompactGrid must use SpringLayout." );
			return;
		}

		// Align all cells in each column and make them the same width.
		x = Spring.constant( initialX );
		for( int col = 0; col < cols; col++ ) {
			width		= Spring.constant( 0 );
			anyVisible	= false;
			for( int row = 0; row < rows; row++ ) {
				comp	= parent.getComponent( row * cols + col );
				if( comp.isVisible() ) {
					width		= Spring.max( width, layout.getConstraints( comp ).getWidth() );
					anyVisible	= true;
				}
			}
			for( int row = 0; row < rows; row++ ) {
				comp		= parent.getComponent( row * cols + col );
				constraints = layout.getConstraints( comp );
				constraints.setX( x );
				if( comp.isVisible() ) constraints.setWidth( width );
			}
			if( anyVisible) x = Spring.sum( x, Spring.sum( width, Spring.constant( xPad )));
        }

		// Align all cells in each row and make them the same height.
		y = Spring.constant( initialY );
		for( int row = 0; row < rows; row++ ) {
			height		= Spring.constant( 0 );
			anyVisible	= false;
			for( int col = 0; col < cols; col++ ) {
				comp	= parent.getComponent( row * cols + col );
				if( comp.isVisible() ) {
					height		= Spring.max( height, layout.getConstraints( comp ).getHeight() );
					anyVisible	= true;
				}
			}
			for( int col = 0; col < cols; col++ ) {
				comp		= parent.getComponent( row * cols + col );
				constraints = layout.getConstraints( comp );
				constraints.setY( y );
				if( comp.isVisible() ) constraints.setHeight( height );
			}
			if( anyVisible ) y = Spring.sum( y, Spring.sum( height, Spring.constant( yPad )));
		}

		// Set the parent's size.
		constraints = layout.getConstraints( parent );
		constraints.setConstraint( SpringLayout.SOUTH, y );
		constraints.setConstraint( SpringLayout.EAST, x );
	}

	/**
	 *	Returns an <code>Icon</code> for a no-write
	 *	or write-protection indicator. The icon has
	 *	a dimension of 16x16 pixels with transparent background.
	 *
	 *	@return	the write-protected icon
	 */
	public static Icon getNoWriteIcon()
	{
		if( icnNoWrite == null ) {
//			icnNoWrite = new ImageIcon( ClassLoader.getSystemClassLoader().getResource( "nowrite.png" ));
			icnNoWrite = new ImageIcon( GUIUtil.class.getResource( "nowrite.png" ));
		}
		return icnNoWrite;
	}

	public static GradientPanel createGradientPanel()
	{
		final GradientPanel		gp		= new GradientPanel();
		final LookAndFeel		laf		= UIManager.getLookAndFeel();
		final boolean			isAqua	= laf == null ? false : laf.getID().equals( "Aqua" );
		final GradientPaint		grad	= isAqua ? new GradientPaint( 0f, 0f, new Color( 0xF3, 0xF3, 0xF3 ), 0f, 69f, new Color( 0xC4, 0xC4, 0xC4 )) : null;

		gp.setLayout( new BoxLayout( gp, BoxLayout.X_AXIS ));
		gp.setGradient( grad );
		gp.setBorder( BorderFactory.createEmptyBorder( 2, 2, 2, 2 ));

		return gp;
	}

	/**
	 *	Adjusts minimum, maximum and preferred size
	 *	of a component so as to constrain its width
	 *	to a given value.
	 *
	 *	@param	c	the component to constrain
	 *	@param	w	the width in pixels
	 */
	public static void constrainWidth( JComponent c, int w )
	{
		Dimension d;

		d = c.getMinimumSize();
		c.setMinimumSize( new Dimension( w, d.height ));
		d = c.getMaximumSize();
		c.setMaximumSize( new Dimension( w, d.height ));
		d = c.getPreferredSize();
		c.setPreferredSize( new Dimension( w, d.height ));
	}

	/**
	 *	Adjusts minimum, maximum and preferred size
	 *	of a component so as to constrain its height
	 *	to a given value.
	 *
	 *	@param	c	the component to constrain
	 *	@param	h	the height in pixels
	 */
	public static void constrainHeight( JComponent c, int h )
	{
		Dimension d;

		d = c.getMinimumSize();
		c.setMinimumSize( new Dimension( d.width, h ));
		d = c.getMaximumSize();
		c.setMaximumSize( new Dimension( d.width, h ));
		d = c.getPreferredSize();
		c.setPreferredSize( new Dimension( d.width, h ));
	}

	/**
	 *	Sets minimum, maximum and preferred size
	 *	to given values.
	 *
	 *	@param	c	the component to constrain
	 *	@param	w	the width in pixels
	 *	@param	h	the height in pixels
	 */
	public static void constrainSize( JComponent c, int w, int h )
	{
		final Dimension d = new Dimension( w, h );

		c.setMinimumSize( d );
		c.setMaximumSize( d );
		c.setPreferredSize( d );
	}

	public static void wrapWindowBounds( Rectangle wr, Rectangle sr )
	{
		if( sr == null ) {
			sr = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		}
//		if( i == null ) {
//			final boolean isMacOS = System.getProperty( "os.name" ).indexOf( "Mac OS" ) >= 0;
////			final boolean isWindows = System.getProperty( "os.name" ).indexOf( "Windows" ) >= 0;
//			i = new Insets( isMacOS ? 61 : 42, 42, 42, 42 );
//			// XXX should take dock size and position into account
//			// $ defaults read com.apple.dock tilesize
//			// $ defaults read com.apple.dock orientation
//		}
//		sr.x		+= i.left;
//		sr.y		+= i.top;
//		sr.width	-= (i.left + i.right);
//		sr.height	-= (i.top + i.bottom);
		if( (wr.x < sr.x) || ((wr.x + wr.width) > (sr.x + sr.width)) ) {
			wr.x		= sr.x;
			if( wr.width > sr.width ) wr.width = sr.width;
		}
		if( (wr.y < sr.y) || ((wr.y + wr.height) > (sr.y + sr.height)) ) {
			wr.y		= sr.y;
			if( wr.height > sr.height ) wr.height = sr.height;
		}
	}

	/**
	 *	Passes keyboard focus to a given component when
	 *	that component is to be presented in a dialog.
	 *	This temporarily adds an <code>AncestorListener</code>
	 *	to the component, detecting when its parent container
	 *	is made visible. This is usefull for defining an
	 *	initial focus owner in <code>JOptionPane</code> calls for example.
	 *
	 *	@param	c	the component to make focussed once its parent container is shown
	 */
	public static void setInitialDialogFocus( final JComponent c )
	{
		c.addAncestorListener( new AncestorListener() {
			public void ancestorAdded( AncestorEvent e ) {
				c.requestFocusInWindow();
				c.removeAncestorListener( this );
			}

            public void ancestorRemoved(AncestorEvent e) {}
            public void ancestorMoved(AncestorEvent e) {}
        });
	}

	public static boolean setAlwaysOnTop( Component c, boolean b )
	{
		// setAlwaysOnTop doesn't exist in Java 1.4
		try {
			final Method m = c.getClass().getMethod( "setAlwaysOnTop", new Class[] { Boolean.TYPE });
			m.invoke( c, new Object[] { new Boolean( b )});
			return true;
		}
		catch( NoSuchMethodException e1 ) { /* ingore */ }
		catch( NullPointerException e1 ) { /* ingore */ }
		catch( SecurityException e1 ) { /* ingore */ }
		catch( IllegalAccessException e1 ) { /* ingore */ }
		catch( IllegalArgumentException e1 ) { /* ingore */ }
		catch( InvocationTargetException e1 ) { /* ingore */ }
		catch( ExceptionInInitializerError e1 ) { /* ingore */ }
		return false;
	}

	public static boolean isAlwaysOnTop( Component c )
	{
		// setAlwaysOnTop doesn't exist in Java 1.4
		try {
			final Method m = c.getClass().getMethod( "isAlwaysOnTop", null );
			final Object result = m.invoke( c, null );
			if( result instanceof Boolean ) {
				return ((Boolean) result).booleanValue();
			}
		}
		catch( NoSuchMethodException e1 ) { /* ingore */ }
		catch( NullPointerException e1 ) { /* ingore */ }
		catch( SecurityException e1 ) { /* ingore */ }
		catch( IllegalAccessException e1 ) { /* ingore */ }
		catch( IllegalArgumentException e1 ) { /* ingore */ }
		catch( InvocationTargetException e1 ) { /* ingore */ }
		catch( ExceptionInInitializerError e1 ) { /* ingore */ }
		return false;
	}

	/**
	 *	Same as SwingUtilities.convertPoint, but handles JViewports properly
	 */
    public static Point convertPoint( Component source, Point aPoint, Component destination )
    {
    	final Point p;

        if( (source == null) && (destination == null) ) return aPoint;
        if( source == null ) {
            source = SwingUtilities.getWindowAncestor( destination );
            if( source == null ) {
                throw new Error( "Source component not connected to component tree hierarchy" );
            }
        }
        p = new Point( aPoint );
        convertPointToScreen( p, source );
        if( destination == null ) {
            destination = SwingUtilities.getWindowAncestor( source );
            if( destination == null ) {
                throw new Error( "Destination component not connected to component tree hierarchy" );
            }
        }
        convertPointFromScreen( p, destination );
        return p;
    }

	/**
	 *	Same as SwingUtilities.convertPointToScreen, but handles JViewports properly
	 */
    public static void convertPointToScreen( Point p, Component c )
    {
        int			x, y;
        Container	parent;
        boolean		isWindowOrApplet;

        do {
            parent				= c.getParent();
            isWindowOrApplet	= (c instanceof Applet) || (c instanceof Window);
            if( (parent == null) || !(parent instanceof JViewport) ) {
	            if( c instanceof JComponent ) {
	                x = ((JComponent) c).getX();
	                y = ((JComponent) c).getY();
	            } else if( isWindowOrApplet ) {
	                try {
	                    final Point pp = c.getLocationOnScreen();
	                    x = pp.x;
	                    y = pp.y;
	                } catch( IllegalComponentStateException icse ) {
	                	x = c.getX();
	                	y = c.getY();
	                }
	            } else {
	                x = c.getX();
	                y = c.getY();
	            }

// System.out.println( "toScreen. c = " + c + "; dx " + x + "; dy " + y );
	            p.x += x;
	            p.y += y;
            }
            c = parent;
        } while( !isWindowOrApplet && (c != null) );
    }

	/**
	 *	Same as SwingUtilities.convertPointFromScreen, but handles JViewports properly
	 */
    public static void convertPointFromScreen( Point p,Component c )
    {
        int			x, y;
        Container	parent;
        boolean		isWindowOrApplet;

        do {
            parent				= c.getParent();
            isWindowOrApplet	= (c instanceof Applet) || (c instanceof Window);
            if( (parent == null) || !(parent instanceof JViewport) ) {
	            if( c instanceof JComponent ) {
	                x = ((JComponent) c).getX();
	                y = ((JComponent) c).getY();
	            }  else if( isWindowOrApplet ) {
	                try {
	                	final Point pp = c.getLocationOnScreen();
	                    x = pp.x;
	                    y = pp.y;
	                } catch( IllegalComponentStateException icse ) {
	                	x = c.getX();
	                	y = c.getY();
	                }
	            } else {
	            	x = c.getX();
	            	y = c.getY();
	            }
// System.out.println( "fromScreen. c = " + c + "; dx " + -x + "; dy " + -y );
	            p.x -= x;
	            p.y -= y;
            }
            c = parent;
        } while( !isWindowOrApplet && (c != null) );
    }

    public static Rectangle convertRectangle( Component source, Rectangle r, Component destination )
    {
    	final Point p = convertPoint( source, new Point( r.x, r.y ), destination );
        return new Rectangle( p.x, p.y, r.width, r.height );
    }

//    /**
//     * 	Removes from a component's inputmap all bindings which may conflict
//     * 	with menu accelerators, that is those whose modifiers are equal to
//     * 	the secondary menu modifier, defined as control on mac and control+alt
//     * 	on windows and linux.
//     *
//     *	@param c	the component to inspect
//     */
//    public static void removeMenuModifierBindings( JComponent c, MenuGroup mg )
//    {
//    	final int primary	= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
//    	final int secondary = InputEvent.CTRL_MASK |
//			(primary == InputEvent.CTRL_MASK ? InputEvent.ALT_MASK : 0);
//
//    	final Set keySet = new HashSet();
//    	gatherAccelerators( keySet, mg, secondary );
//    	removeMenuModifierBindings( c.getInputMap(), primary, keySet );
//    }

//    private static void gatherAccelerators( Set keySet, MenuItem mi, int mod )
//    {
//    	final Action a = mi.getAction();
//    	if( a != null ) {
//    		final KeyStroke strk = (KeyStroke) a.getValue( Action.ACCELERATOR_KEY );
//    		if( (strk != null) && ((strk.getModifiers() & mod) == mod) )
//    			keySet.add( strk );
//    	}
//    	if( mi instanceof MenuGroup ) {
//    		final MenuGroup mg = (MenuGroup) mi;
//    		for( int i = 0; i < mg.size(); i++ ) {
//    			final MenuNode mn = mg.get( i );
//    			if( mn instanceof MenuItem )
//    				gatherAccelerators( keySet, (MenuItem) mn, mod );
//    		}
//    	}
//    }

    private static void removeMenuModifierBindings( InputMap imap, int primary, Set keySet )
    {
    	if( imap == null ) return;
    	final KeyStroke[] keys = imap.keys();
    	if( keys != null ) {
    		for( int i = 0; i < keys.length; i++ ) {
    			final KeyStroke k = keys[ i ];
    			if( (k.getModifiers() != primary) && keySet.contains( k )) {
    				imap.remove( k );
    			}
    		}
    	}
    	removeMenuModifierBindings( imap.getParent(), primary, keySet );
    }
}