package legacy;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *  This class is an updated (slim) version
 *  of FScape's <code>PathField</code> and provides a GUI object
 *  that displays a file's or folder's path,
 *  optionally with format information, and allows
 *  the user to browse the harddisk to change
 *  to a different file.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.34, 07-Feb-10
 */
public class PathField
extends SpringPanel
implements ActionListener, PathListener, EventManager.Processor
{
// -------- public Variablen --------
	/**
	 *	type flag : the bitmask for all path types
	 */
	public static final int TYPE_BASICMASK	= 0x0F;
	/**
	 *	type flag : file is used for input / reading
	 */
	public static final int TYPE_INPUTFILE	= 0x00;
	/**
	 *	type flag : request an extra gadget to view file information
	 */
	public static final int TYPE_FORMATFIELD= 0x10;
	/**
	 *	type flag : file is used for output / writing
	 */
	public static final int TYPE_OUTPUTFILE	= 0x01;
	/**
	 *	type flag : path to be chosen is a folder
	 */
	public static final int TYPE_FOLDER		= 0x02;

// -------- private Variablen --------

	protected static PathList	userPaths			= null;
	private static final int	USERPATHS_NUM		= 9;		// userPaths capacity
	private static final int	ABBR_LENGTH			= 12;		// constants for abbreviate
//	private static final int	DEFAULT_COLUMN_NUM  = 32;		// constants for IOTextField

	protected final IOTextField	ggPath;
	protected final PathButton	ggChoose;
	protected final JLabel		lbWarn;
	protected ColoredTextField	ggFormat	= null;

	private static final Color  COLOR_ERR   = new Color( 0xFF, 0x00, 0x00, 0x2F );
	private static final Color  COLOR_EXISTS= new Color( 0x00, 0x00, 0xFF, 0x2F );
	protected static final Color COLOR_PROPSET=new Color( 0x00, 0xFF, 0x00, 0x2F );

	private final int		type;
//	private final String	dlgTxt;
	protected String		scheme;
	protected String		protoScheme;
	private PathField		superPaths[];

	private final List				collChildren	= new ArrayList();
	private final EventManager		elm				= new EventManager( this );

	private static final int		MENU_SHORTCUT	= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	protected static final int		myMeta			= MENU_SHORTCUT == InputEvent.CTRL_MASK ?
		InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK : MENU_SHORTCUT;	// META on Mac, CTRL+SHIFT on PC

	private static Icon				icnAlertStop	= null;

	private boolean warnWhenExists			= false;
	private boolean errWhenExistsNot		= false;
	private boolean errWhenWriteProtected	= false;

// -------- public Methoden --------

	public PathField()
	{
		this( TYPE_INPUTFILE );
	}

	public PathField( int type )
	{
		super();
		this.type = type;

		// first instance initialized userPath list
		if( userPaths == null ) {
			userPaths	= new PathList( USERPATHS_NUM, GUIUtil.getUserPrefs(), PathList.KEY_USERPATHS );
			if( userPaths.getPathCount() < USERPATHS_NUM ) {	// prefs not initialized
				File home	= new File( System.getProperty( "user.home" ));
				File[] sub  = home.listFiles();
				userPaths.addPathToHead( home );
				if( sub != null ) {
					for( int i = 0; i < sub.length && userPaths.getPathCount() < USERPATHS_NUM; i++ ) {
						if( sub[i].isDirectory() && !sub[i].isHidden() ) userPaths.addPathToTail( sub[i] );
					}
				}
				while( userPaths.getPathCount() < USERPATHS_NUM ) {
					userPaths.addPathToTail( home );
				}
			}
		}

		lbWarn				= new JLabel();
		gridAdd( lbWarn, 0, 0 );

		ggPath			= new IOTextField();
		ggPath.addActionListener( this );		// High-Level Events: Return-Hit weiterleiten
		ggChoose		= createPathButton( type );
		ggChoose.addPathListener( this );
		gridAdd( ggPath, 1, 0 );

		if( (type & TYPE_FORMATFIELD) != 0 ) {
			ggFormat		= new ColoredTextField();
			ggFormat.setEditable( false );
			ggFormat.setBackground( null );
			gridAdd( ggFormat, 1, 1 );
		}

		gridAdd( ggChoose, 2, 0 );

		makeCompactGrid();

//		deriveFrom( new PathField[0], (ggType != null) ? "$E" : "" );
		deriveFrom( new PathField[0], "" );

		// MacOS X has a bug with
		// the caret position when the
		// font isn't set explicitly for sub containers
		this.addPropertyChangeListener( "font", new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent e )
			{
				Font fnt = ((PathField) e.getSource()).getFont();
				ggPath.setFont( fnt );
				if( ggFormat != null ) ggFormat.setFont( fnt );
			}
		});
	}

	/**
	 *  Create a new <code>PathField</code>
	 *
	 *  @param  type	one of the types covered by TYPE_BASICMASK
	 *					bitwise-OR optional displays like TYPE_FORMATFIELD
	 *  @param  dlgTxt  the text string to display in the filechooser or <code>null</code>
	 */
	public PathField( int type, String dlgTxt )
	{
		this( type );
		ggChoose.setDialogText( dlgTxt );
		if( (type & TYPE_OUTPUTFILE) != 0 ) {
			warnWhenExists = true;
			errWhenWriteProtected = true;
		} else {
			errWhenExistsNot = true;
		}
	}

	/**
	 *  Sets the gadget's path. This is path will be
	 *  used as default setting when the file chooser is shown
	 *
	 *  @param  path	the new path for the button
	 */
	public void setPath( File path )
	{
		setPathIgnoreScheme( path );
		scheme = createScheme( path.getPath() );
	}

	public void setWarnWhenExists( boolean onOff )
	{
		if( warnWhenExists != onOff ) {
			warnWhenExists = onOff;
			updateIconAndColour();
		}
	}

	public void setErrWhenExistsOn( boolean onOff )
	{
		if( errWhenExistsNot != onOff ) {
			errWhenExistsNot = onOff;
			updateIconAndColour();
		}
	}

	public void setErrWhenWriteProtected( boolean onOff )
	{
		if( errWhenWriteProtected != onOff ) {
			errWhenWriteProtected = onOff;
			updateIconAndColour();
		}
	}

	private void setPathIgnoreScheme( File path )
	{
		ggPath.setText( path.getPath() );
		ggChoose.setPath( path );
		for( int i = 0; i < collChildren.size(); i++ ) {
			((PathField) collChildren.get( i )).motherSpeaks( path );
		}
		updateIconAndColour();
	}

	/**
	 *  Sets a new path and dispatches a <code>PathEvent</code>
	 *  to registered listeners
	 *
	 *  @param  path	the new path for the gadget and the event
	 */
	protected void setPathAndDispatchEvent( File path )
	{
		setPathIgnoreScheme( path );
		elm.dispatchEvent( new PathEvent( this, PathEvent.CHANGED, System.currentTimeMillis(), path ));
	}

	/**
	 *  Returns the path displayed in the gadget.
	 *
	 *  @return the <code>File</code> corresponding to the path string in the gadget
	 */
	public File getPath()
	{
		return( new File( ggPath.getText() ));
	}

	/**
	 *  Change the contents of the format gadget.
	 *
	 *  @param  txt			Text to be displayed in the format gadget
	 *  @param  success		<code>false</code> indicates file format parse error
	 *						and will render the format gadget red.
	 *  @throws IllegalStateException   if the path field doesn't have a format gadget
	 */
	public void setFormat( String txt, boolean success )
	{
		if( ggFormat == null ) throw new IllegalStateException();

		ggFormat.setText( txt );
		ggFormat.setPaint( success ? null : COLOR_ERR );
	}

	/**
	 *  Get the string displayed in the format gadget.
	 *
	 *  @return		the currently displayed format text or <code>null</code>
	 *				if the path field has no format gadget.
	 */
	public String getFormat()
	{
		if( ggFormat != null ) {
			return ggFormat.getText();
		} else {
			return null;
		}
	}

	/**
	 *  Gets the type of the path field.
	 *
	 *  @return the gadget's type as specified in the constructor
	 *			use bitwise-AND with <code>TYPE_BASICMASK</code> to query the
	 *			file access type.
	 */
	public int getType()
	{
		return type;
	}

	/**
	 * 	Sets the filter to use for enabling or disabling items
	 * 	in the file dialog.
	 *
	 * 	@param	filter	the new filter or null to remove an existing filter
	 */
	public void setFilter( FilenameFilter filter )
	{
		ggChoose.setFilter( filter );
	}

	/**
	 * 	Queries the current item filter for the file dialog.
	 *
	 * 	@return	the current filter or null if no filter is installed
	 */
	public FilenameFilter getFilter()
	{
		return ggChoose.getFilter();
	}

	/**
	 *	Selects the file name portion of the text contents.
	 *
	 *	@param	extention	whether to include the file name suffix or not
	 */
	public void selectFileName( boolean extention )
	{
		final String	s = ggPath.getText();
		final int		i = s.lastIndexOf( File.separatorChar ) + 1;
		final int		j = s.lastIndexOf( '.' );
		ggPath.select( i, j >= i ? j : s.length() );
	}

	public boolean requestFocusInWindow()
	{
		return ggPath.requestFocusInWindow();
	}

	/**
	 *  <code>PathField</code> offers a mechanism to automatically derive
	 *  a path name from a "mother" <code>PathField</code>. This applies
	 *  usually to output files whose names are derived from
	 *  PathFields which represent input paths. The provided
	 *  'scheme' String can contain the Tags
	 *  <pre>
	 *  $Dx = Directory of superPath x; $Fx = Filename; $E = Extension; $Bx = Brief filename
	 *  </pre>
	 *  where 'x' is the index in the provided array of
	 *  mother PathFields. Whenever the mother contents
	 *  changes, the child PathField will recalculate its
	 *  name. When the user changes the contents of the child
	 *  PathField, an algorithm tries to find out which components
	 *  are related to the mother's pathname, parts that cannot
	 *  be identified will not be automatically changing any more
	 *  unless the user completely clears the PathField (i.e.
	 *  restores full automation).
	 *  <p>
	 *  The user can abbreviate or extend filenames by pressing the appropriate
	 *  key; in this case the $F and $B tags are exchanged in the scheme.
	 *
	 *  @param  sp 		array of mother path fields to listen to
	 *  @param  s		automatic formatting scheme which can incorporate
	 *					placeholders for the mother fields' paths.
	 */
	public void deriveFrom( PathField[] sp, String s )
	{
		this.superPaths 	= sp;
		this.scheme			= s;
		this.protoScheme	= s;

		for( int i = 0; i < sp.length; i++ ) {
			sp[ i ].addChildPathField( this );
		}
	}

	private void addChildPathField( PathField child )
	{
		if( !collChildren.contains( child )) collChildren.add( child );
	}

	private void motherSpeaks( File superPath )
	{
		setPathAndDispatchEvent( new File( evalScheme( scheme )));
	}

	// --- listener registration ---

	/**
	 *  Registers a <code>PathListener</code>
	 *  which will be informed about changes of
	 *  the path (i.e. user selections in the
	 *  file chooser or text editing).
	 *
	 *  @param  listener	the <code>PathListener</code> to register
	 *  @see	de.sciss.app.EventManager#addListener( Object )
	 */
	public void addPathListener( PathListener listener )
	{
		elm.addListener( listener );
	}

	/**
	 *  Unregisters a <code>PathListener</code>
	 *  from receiving path change events.
	 *
	 *  @param  listener	the <code>PathListener</code> to unregister
	 *  @see	de.sciss.app.EventManager#removeListener( Object )
	 */
	public void removePathListener( PathListener listener )
	{
		elm.removeListener( listener );
	}

	public void processEvent( BasicEvent e )
	{
		PathListener listener;
		int i;

		for( i = 0; i < elm.countListeners(); i++ ) {
			listener = (PathListener) elm.getListener( i );
			switch( e.getID() ) {
			case PathEvent.CHANGED:
				listener.pathChanged( (PathEvent) e );
				break;
			default:
				assert false : e.getID();
			}
		} // for( i = 0; i < elm.countListeners(); i++ )
	}

// -------- private Methoden --------

	private static Icon getAlertStopIcon()
	{
		if( icnAlertStop == null ) {
//			icnAlertStop = new ImageIcon( ClassLoader.getSystemClassLoader().getResource( "alertstop.png" ));
			icnAlertStop = new ImageIcon( PathField.class.getResource( "alertstop.png" ));
		}
		return icnAlertStop;
	}

	private void updateIconAndColour()
	{
		final File		path		= getPath();
		final File		parent		= path.getParentFile();
		final boolean	folder		= (type & TYPE_FOLDER) != 0;
		boolean			parentExists= false;
		boolean			exists		= false;
		boolean			wp			= false;
		final Color		c;
		final Icon		icn;
		final String	tt;

		if( warnWhenExists || errWhenExistsNot || errWhenWriteProtected ) {
		try {
			parentExists	= (parent != null) && parent.isDirectory();
			exists			= folder? path.isDirectory() : path.isFile();
			if( errWhenWriteProtected ) {
				wp			= parentExists &&
					((exists && !path.canWrite()) || (!exists && !parent.canWrite()));
			}
		} catch( SecurityException e ) { /* ignore */ }

		if( errWhenWriteProtected && (wp || !parentExists) ) {
			c	= COLOR_ERR;
			icn	= GUIUtil.getNoWriteIcon();
			tt	= GUIUtil.getResourceString( folder ? "ttWarnFolderWriteProtected" : "ttWarnFileWriteProtected" );
		} else if( errWhenExistsNot && !exists ) {
			c	= COLOR_ERR;
			icn = getAlertStopIcon();
			tt	= GUIUtil.getResourceString( folder ? "ttWarnFolderExistsNot" : "ttWarnFileExistsNot" );
		} else if( warnWhenExists && exists ) {
			c	= COLOR_EXISTS;
			icn = getAlertStopIcon();
			tt	= GUIUtil.getResourceString( folder ? "ttWarnFolderExists" : "ttWarnFileExists" );
		} else {
			c	= null;
			icn	= null;
			tt	= null;
		}
		if( c != ggPath.getPaint() ) {
			ggPath.setPaint( c );
		}
		if( lbWarn.getIcon() != icn ) {
			lbWarn.setIcon( icn );
		}
		if( lbWarn.getToolTipText() != tt )
			lbWarn.setToolTipText( tt );
		}
	}

	/*
	 *	Tags: $Dx = Directory of superPath x; $Fx = Filename; $E = Extension; $Bx = Brief filename
	 */
	protected String evalScheme( String s )
	{
		String	txt2;
		int		i, j, k;

		for( i = s.indexOf( "$D" ); (i >= 0) && (i < s.length() - 2); i = s.indexOf( "$D", i )) {
			j		= s.charAt( i + 2 ) - 48;
			try {
				txt2 = superPaths[ j ].getPath().getPath();
			} catch( ArrayIndexOutOfBoundsException e1 ) {
				txt2 = "";
			}
			// sucky java 1.1 stringbuffer is impotent
			s	= s.substring( 0, i ) + txt2.substring( 0, txt2.lastIndexOf( File.separatorChar ) + 1 ) +
					  s.substring( i + 3 );
		}
		for( i = s.indexOf( "$F" ); (i >= 0) && (i < s.length() - 2); i = s.indexOf( "$F", i )) {
			j		= s.charAt( i + 2 ) - 48;
			try {
				txt2 = superPaths[ j ].getPath().getPath();
			} catch( ArrayIndexOutOfBoundsException e1 ) {
				txt2 = "";
			}
			txt2	= txt2.substring( txt2.lastIndexOf( File.separatorChar ) + 1 );
			k		= txt2.lastIndexOf( '.' );
			s	= s.substring( 0, i ) + ((k > 0) ? txt2.substring( 0, k ) : txt2 ) +
					  s.substring( i + 3 );
		}
		for( i = s.indexOf( "$X" ); (i >= 0) && (i < s.length() - 2); i = s.indexOf( "$X", i )) {
			j		= s.charAt( i + 2 ) - 48;
			try {
				txt2 = superPaths[ j ].getPath().getPath();
			} catch( ArrayIndexOutOfBoundsException e1 ) {
				txt2 = "";
			}
			txt2	= txt2.substring( txt2.lastIndexOf( File.separatorChar ) + 1 );
			k		= txt2.lastIndexOf( '.' );
			s	= s.substring( 0, i ) + ((k > 0) ? txt2.substring( k ) : "" ) +
					  s.substring( i + 3 );
		}
		for( i = s.indexOf( "$B" ); (i >= 0) && (i < s.length() - 2); i = s.indexOf( "$B", i )) {
			j		= s.charAt( i + 2 ) - 48;
			try {
				txt2 = superPaths[ j ].getPath().getPath();
			} catch( ArrayIndexOutOfBoundsException e1 ) {
				txt2 = "";
			}
			txt2	= txt2.substring( txt2.lastIndexOf( File.separatorChar ) + 1 );
			k		= txt2.lastIndexOf( '.' );
			txt2	= abbreviate( (k > 0) ? txt2.substring( 0, k ) : txt2 );
			s 	= s.substring( 0, i ) + txt2 + s.substring( i + 3 );
		}
// XXXX
//		for( i = scheme.indexOf( "$E" ); i >= 0; i = scheme.indexOf( "$E", i )) {
//			j		= getType();
//			scheme	= scheme.substring( 0, i ) + GenericFile.getExtStr( j ) + scheme.substring( i + 2 );
//		}

		return s;
	}

	/*
	 *  A filename will be abbreviated. This is not so
	 *  critical on MacOS X any more because filenames
	 *  can be virtually as long as possible; on some
	 *  systems however when filenames are combinations
	 *  of more than one mother file this can be
	 *  crucial to keep the total filename length within
	 *  the file system's allowed bounds.
	 */
	private static String abbreviate( String longStr )
	{
		StringBuffer	shortStr;
		int				i, j;
		char			c;

		j = longStr.length();
		if( j <= ABBR_LENGTH ) return longStr;

		shortStr = new StringBuffer( j );
		for( i = 0; (i < j) && (shortStr.length() + j - i > ABBR_LENGTH); i++ ) {
			c = longStr.charAt( i );
			if( Character.isLetterOrDigit( c )) {
				shortStr.append( c );
			}
		}
		shortStr.append( longStr.substring( i ));
		longStr	= shortStr.toString();
		j		= longStr.length();
		if( j <= ABBR_LENGTH ) return longStr;

		shortStr = new StringBuffer( j );
		shortStr.append( longStr.charAt( 0 ));
		for( i = 1; (i < j - 1) && (shortStr.length() + j - i > ABBR_LENGTH); i++ ) {
			c = longStr.charAt( i );
			if( "aeiou√§√∂√º".indexOf( c ) < 0 ) {
				shortStr.append( c );
			}
		}
		shortStr.append( longStr.substring( i ));
		longStr	= shortStr.toString();
		j		= longStr.length();
		if( j <= ABBR_LENGTH ) return longStr;

		i = (ABBR_LENGTH >> 1) - 1;

		return( longStr.substring( 0, i ) + '\'' + longStr.substring( longStr.length() - i ));
	}

	/*
	 *  Try to analyse a concrete pathname
	 *  with respect to mother pathnames
	 *  to find some sort of scheme behind it.
	 */
	private String createScheme( String applied )
	{
		String	txt2;
		int		i = 0;
		int		k = 0;
		int		m;
		int		checkedAbbrev;
		boolean	checkedFull;

		if( applied.length() == 0 ) return protoScheme;

		for( i = 0; i < superPaths.length; i++ ) {
			txt2 = superPaths[ i ].getPath().getPath();
			txt2 = txt2.substring( 0, txt2.lastIndexOf( File.separatorChar ) + 1 );
			if( applied.startsWith( txt2 )) {
				applied	= "$D" + (char) (i + 48) + applied.substring( txt2.length() );
				k		= 3;
				break;
			}
		}
		k = Math.max( k, applied.lastIndexOf( File.separatorChar ) + 1 );
		for( i = 0, checkedAbbrev = -1; i < superPaths.length; i++ ) {
			txt2	= superPaths[ i ].getPath().getPath();
			txt2	= txt2.substring( txt2.lastIndexOf( File.separatorChar ) + 1 );
			m		= txt2.lastIndexOf( '.' );
			txt2	= (m > 0) ? txt2.substring( 0, m ) : txt2;
			if( (protoScheme.indexOf( "$B" + (char) (i + 48) ) < 0) || (checkedAbbrev == i) ) {
				m	= applied.indexOf( txt2, k );
				if( m >= 0 ) {
					applied = applied.substring( 0, m ) + "$F" + (char) (i + 48) + applied.substring( m + txt2.length() );
					k		= m + 3;
					continue;
				}
				checkedFull	= true;
			} else {
				checkedFull = false;
			}
			if( checkedAbbrev == i ) continue;
			txt2 = abbreviate( txt2 );
			m	 = applied.indexOf( txt2, k );
			if( m >= 0 ) {
				applied = applied.substring( 0, m ) + "$B" + (char) (i + 48) + applied.substring( m + txt2.length() );
				k		= m + 3;
			} else if( !checkedFull ) {
				checkedAbbrev = i;
				i--;				// retry non-abbreviated
			}
		}
// XXX
//		txt2 = GenericFile.getExtStr( getType() );
//		if( applied.endsWith( txt2 )) {
//			applied = applied.substring( 0, applied.length() - txt2.length() ) + "$E";
//		}

		return applied;
	}

	protected String abbrScheme( String orig )
	{
		int i = orig.lastIndexOf( "$F" );
		if( i >= 0 ) {
			return( orig.substring( 0, i ) + "$B" + orig.substring( i + 2 ));
		} else {
			return orig;
		}
	}

	protected String expandScheme( String orig )
	{
		int i = orig.indexOf( "$B" );
		if( i >= 0 ) {
			return( orig.substring( 0, i ) + "$F" + orig.substring( i + 2 ));
		} else {
			return orig;
		}
	}

	protected String udirScheme( String orig, int idx )
	{
		int		i;
		File	udir = userPaths.getPath( idx );

		if( udir == null ) return orig;

		if( orig.startsWith( "$D" )) {
			i = 3;
		} else {
			i = orig.lastIndexOf( File.separatorChar ) + 1;
		}

		return( new File( udir, orig.substring( i )).getPath() );
	}

	protected PathButton createPathButton( int buttonType )
	{
		return new PathButton( buttonType );
	}

// -------- PathListener interface --------
// we're listening to ggChoose

	public void pathChanged( PathEvent e )
	{
		File path = e.getPath();
		scheme = createScheme( path.getPath() );
		setPathAndDispatchEvent( path );
	}

// -------- ActionListener interface --------
// we're listening to ggPath

	public void actionPerformed( ActionEvent e )
	{
		String str = ggPath.getText();
		if( ggPath.getText().length() == 0 ) {				// automatic generation
			str = evalScheme( scheme );
		} else {
			scheme = createScheme( str );
		}
		setPathAndDispatchEvent( new File( str ));
	}

// -------- internal IOTextfeld class --------

	protected class IOTextField
	extends ColoredTextField
	{
		protected IOTextField()
		{
			super( 32 );

			final InputMap		inputMap	= getInputMap();
			final ActionMap		actionMap   = getActionMap();
			String				s;

			inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, myMeta + InputEvent.ALT_MASK ), "abbr" );
			actionMap.put( "abbr", new AbstractAction() {
				public void actionPerformed( ActionEvent e )
				{
					scheme = abbrScheme( scheme );
					setPathAndDispatchEvent( new File( evalScheme( scheme )));
				}
			});
			inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, myMeta + InputEvent.ALT_MASK ), "expd" );
			actionMap.put( "expd", new AbstractAction() {
				public void actionPerformed( ActionEvent e )
				{
					scheme = expandScheme( scheme );
					setPathAndDispatchEvent( new File( evalScheme( scheme )));
				}
			});
			inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, myMeta ), "auto" );
			actionMap.put( "auto", new AbstractAction() {
				public void actionPerformed( ActionEvent e )
				{
					scheme = protoScheme;
					setPathAndDispatchEvent( new File( evalScheme( scheme )));
				}
			});
			inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), "lost" );
			actionMap.put( "lost", new AbstractAction() {
				public void actionPerformed( ActionEvent e )
				{
					final JRootPane rp = SwingUtilities.getRootPane( IOTextField.this );
					if( rp != null ) rp.requestFocus();
				}
			});
			for( int i = 0; i < USERPATHS_NUM; i++ ) {
				s = "sudir" + i;
				inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD1 + i, myMeta + InputEvent.ALT_MASK ), s );
				actionMap.put( s, new SetUserDirAction( i ));
				s = "rudir" + i;
				inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD1 + i, myMeta ), s );
				actionMap.put( s, new RecallUserDirAction( i ));
			}
		}

		private class SetUserDirAction
		extends AbstractAction
		{
			private int idx;
			private javax.swing.Timer visualFeedback;
			private Paint oldPaint = null;

			protected SetUserDirAction( int idx )
			{
				this.idx		= idx;
				visualFeedback  = new javax.swing.Timer( 250, this );
				visualFeedback.setRepeats( false );
			}

			public void actionPerformed( ActionEvent e )
			{
				if( e.getSource() == visualFeedback ) {
					ggPath.setPaint( oldPaint );
				} else {
					File dir = getPath().getParentFile();
					if( dir != null ) {
						userPaths.setPath( idx, dir );
						if( visualFeedback.isRunning() ) {
							visualFeedback.restart();
						} else {
							oldPaint = ggPath.getPaint();
							ggPath.setPaint( COLOR_PROPSET );
							visualFeedback.start();
						}
					}
				}
			}
		}

		private class RecallUserDirAction
		extends AbstractAction
		{
			private int idx;

			protected RecallUserDirAction( int idx )
			{
				this.idx = idx;
			}

			public void actionPerformed( ActionEvent e )
			{
				scheme = udirScheme( scheme, idx );
				setPathAndDispatchEvent( new File( evalScheme( scheme )));
			}
		}
	} // class IOTextField
} // class PathField
