//package de.sciss.kontur.desktop.impl
//
//import javax.swing.Timer
//import java.awt.event.{ActionEvent, ActionListener}
//
///**
// *	Regular windows and floating palettes register themselves with a
// *	a <code>FloatingPaletteHandler</code>. The handler then takes care
// *	of hiding and showing the palettes when the application becomes
// *	active or inactive (i.e. one of the regular windows has focus or none of them).
// *
// *	The class is closely based on <code>FloatingPaletteHandler14</code> v2.0 by
// *	Werner Randelshofer (Quaqua project).
// *
// *	@todo				selecting an item from the right menu bar
// *						(e.g. clock) causes a palette flicker
// */
//object FloatingPaletteHandlerImpl {
//  private final val TIMEOUT = 100
//
//  private var palettes        = Set.empty
//  private var frames          = Set.empty
//  private var hiddenPalettes  = Set.empty
//  private val timer           = new Timer(TIMEOUT, new ActionListener {
//    def actionPerformed(e: ActionEvent) { timeOut() }
//  })
//  timer.setRepeats(false)
//  private var modalDlgs		    = 0
//
//		winListener = new AbstractWindow.Adapter() {
//			public void windowActivated( AbstractWindow.Event e )
//			{
//				final AbstractWindow w = e.getWindow();
//
//				timer.stop();
//
//				if( frames.contains( w )) {
//					setFocusedWindow( w );
//				}
//			}
//
//			public void windowDeactivated( AbstractWindow.Event e )
//			{
//				// since a focus traversal always contains
//				// a focus loss and a focus gain, we have to
//				// wait a little to make sure that no new
//				// window was focussed. the timer is executed
//				// after 100ms.
//				timer.restart();
//			}
//		};
//    }
//
//	public void setListening( boolean b )
//	{
//		listen = b;
//		if( !palettes.isEmpty() || !frames.isEmpty() ) throw new IllegalStateException( "Must only be called initially" );
//	}
//
//    /**
//     * Registers a project window with the FloatingPaletteHandler.
//     *
//     * When none of the registered windows has focus, the FloatingPaletteHandler
//     * hides all registered palette windows.
//     * When at least one of the registered windows has focus, the
//     * FloatingPaletteHandler shows all registered palette windows.
//     */
//	public void add( AbstractWindow w )
//	{
//    	if( DEBUG ) System.out.println( "add : " + w.getClass().getName() );
//
//    	if( w.isFloating() ) {
//			if( !palettes.add( w )) {
//				throw new IllegalArgumentException( "Palette was already registered" );
//			}
//			if( listen ) w.addListener( winListener );
//		} else {
//			if( !frames.add( w )) {
//				throw new IllegalArgumentException( "Frame was already registered" );
//			}
//
//			if( listen ) {
//				w.addListener( winListener );
//				if( isFocused( w )) {
//focusedWindow = w;
//				}
//			}
//		}
//	}
//
//	public void addModalDialog()
//	{
//		modalDlgs++;
//	}
//
//	public void removeModalDialog()
//	{
//		modalDlgs--;
//	}
//
//    /**
//     * Unregisters a project window with the FloatingPaletteHandler.
//     */
//    public void remove( AbstractWindow w )
//	{
//    	if( DEBUG ) System.out.println( "remove : " + w.getClass().getName() );
//
//    	if( w.isFloating() ) {
//			if( !palettes.remove( w )) {
//				throw new IllegalArgumentException( "Palette was not registered" );
//			}
//			if( listen ) w.removeListener( winListener );
//			hiddenPalettes.remove( w );
//
//		} else {
//			if( !frames.remove( w )) {
//				throw new IllegalArgumentException( "Frame was not registered" );
//			}
//
//			if( listen ) {
//				w.removeListener( winListener );
//				if(	isFocused( w )) {
//focusedWindow = null;
//timer.restart();
//				}
//			}
//		}
//    }
//
//    /**
//     * Returns the current applicatin window (the window which was the last to
//     * gain focus). Floating palettes may use this method to determine on which
//     * window they need to act on.
//     */
//    public AbstractWindow getFocussedWindow() {
//        return focusedWindow;
//    }
//
//	// --------------- ActionListener interface ---------------
//
//	public void actionPerformed( ActionEvent e )
//	{
//		setFocusedWindow( null );
//	}
//
//	// --------------- private methods ---------------
//
//    private static boolean isFocused( AbstractWindow w )
//	{
//        if( w.isActive() ) return true;
//
//        final Window[] owned = w.getOwnedWindows();
//
//        for( int i = 0; i < owned.length; i++ ) {
//            if( isFocused( owned[ i ])) return true;
//        }
//        return false;
//    }
//
//    private static boolean isFocused( Window w )
//	{
//        if( w.isFocused() ) return true;
//
//        final Window[] owned = w.getOwnedWindows();
//
//        for( int i = 0; i < owned.length; i++ ) {
//            if( isFocused( owned[ i ])) return true;
//        }
//        return false;
//    }
//
//    protected void setFocusedWindow( AbstractWindow w )
//	{
//        focusedWindow = w;
//		if( DEBUG ) System.out.println( "focusedWindow : " + (w == null ? null : w.getClass().getName()) );
//
//		if( modalDlgs == 0 ) {
//			if( w != null ) {
//	            showPalettes();
//			} else {
//				hidePalettes();
//			}
//		}
//	}
//
//    private void showPalettes()
//	{
//		if( !hiddenPalettes.isEmpty() ) {
//			AbstractWindow w;
//			for( Iterator iter = hiddenPalettes.iterator(); iter.hasNext(); ) {
//				w = (AbstractWindow) iter.next();
//				if( DEBUG ) System.out.println( "setVisible( true ) : " + w.getClass().getName() );
//				w.setVisible( true );
//			}
//			if( DEBUG ) System.out.println( "hiddenPalettes.clear" );
//			hiddenPalettes.clear();
//
//			// for some reason focusedWindow can be null, although
//			// showPalettes is only called when focusedWindow is not null
//			// (bug 1953444). the only explanation is that setVisible will
//			// immediately invoke one of windowActivated or windowDeactivated
//			// without deferal to the event-queue... anyway, we're safe
//			// by checking again against null...
//			if( focusedWindow != null ) {
//				if( DEBUG ) System.out.println( "setVisible( true ) : " + focusedWindow.getClass().getName() );
//				focusedWindow.setVisible( true );
//			}
//		}
//    }
//
//    private void hidePalettes()
//	{
//		AbstractWindow w;
//
//		if( !frames.isEmpty() ) {
//			for( Iterator iter = frames.iterator(); iter.hasNext(); ) {
//				w = (AbstractWindow) iter.next();
//				if( isFocused( w )) return;
//			}
//		}
//		if( !palettes.isEmpty() ) {
//            for( Iterator iter = palettes.iterator(); iter.hasNext(); ) {
//                w = (AbstractWindow) iter.next();
//                if( isFocused( w )) return;
//            }
//        }
//
//		for( Iterator iter = palettes.iterator(); iter.hasNext(); ) {
//            w = (AbstractWindow) iter.next();
//			if( w.isVisible() ) {
//				if( DEBUG ) {
//					System.out.println( "hiddenPalettes.add : " + w.getClass().getName() );
//					System.out.println( "setVisible( false ) : " + w.getClass().getName() );
//				}
//				hiddenPalettes.add( w );
//				w.setVisible( false );
//            }
//        }
//    }
//}