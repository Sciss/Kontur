//package legacy;
//
//import java.awt.event.ActionEvent;
//
///**
// * 	@version	0.11, 28-Jun-08
// *	@author		Hanns Holger Rutz
// *
// */
//public class ShowWindowAction
//extends MenuAction
//// implements Disposable
//{
//	private final AbstractWindow			w;
//	private final AbstractWindow.Listener	l;
//	protected boolean						disposed	= false;
//
//	public ShowWindowAction( AbstractWindow w )
//	{
//		super( null, null );
//		this.w	= w;
//
//		l = new AbstractWindow.Adapter() {
//			public void windowActivated( AbstractWindow.Event e )
//			{
//				if( !disposed ) ((BasicApplication) AbstractApplication.getApplication()).getMenuFactory().setSelectedWindow( ShowWindowAction.this );
//			}
//		};
//		w.addListener( l );
//	}
//
//	public void actionPerformed( ActionEvent e )
//	{
//		w.setVisible( true );
//		w.toFront();
//	}
//
//	public void dispose()
//	{
//		disposed = true;	// the listener might still be called!
//		w.removeListener( l );
//	}
//}