package legacy;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.AbstractBorder;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.10, 11-Mar-06
 */
public class ComboBoxEditorBorder
extends AbstractBorder
{
	private final Insets		insets, offsets;
	private final Image			img;

	private static final Map	mapOffsets	= new HashMap();
	private static final Map	mapInsets	= new HashMap();

	static {
		mapOffsets.put( "Aqua", new Insets( 4, 0, 2, 0 ));
		mapInsets.put( "Aqua", new Insets( 2, 0, 0, 0 ));
		mapOffsets.put( "Metal", new Insets( 0, 0, 1, 0 ));
		mapInsets.put( "Metal", new Insets( 0, 0, 0, 0 ));
		mapOffsets.put( "Motif", new Insets( -1, 0, -1, 0 ));
		mapInsets.put( "Motif", new Insets( 0, 0, 0, 0 ));
	}

//	public ComboBoxEditorBorder()
//	{
//		this( 0, 0, 0, 0 );
//	}
//
	public ComboBoxEditorBorder()
	{
		super();

//		this.offTop		= offTop;
//		this.offLeft	= offLeft;
//		this.offBottom	= offBottom;
//		this.offRight	= offRight;

		final String	id	= javax.swing.UIManager.getLookAndFeel().getID();
		Insets			in;

		in		= (Insets) mapOffsets.get( id );
		offsets	= in == null ? new Insets( 0, 0, 0, 0 ) : in;
		in		= (Insets) mapInsets.get( id );
		insets	= in == null ? new Insets( 0, 0, 0, 0 ) : in;

		final URL url = getClass().getResource( "cbe.png" );
		img = Toolkit.getDefaultToolkit().getImage( url );
		final MediaTracker mt = new MediaTracker( new Container() );
		mt.addImage( img, 0 );

		try {
			mt.waitForAll( 10000 );
		}
		catch( InterruptedException e1 ) { /* ignore */ }
	}

	public Insets getBorderInsets( Component c )
	{
		return new Insets( insets.top, insets.left, insets.bottom, insets.right );
	}

	public Insets getBorderInsets( Component c, Insets i )
	{
		i.top		= this.insets.top;
		i.left		= this.insets.left;
		i.bottom	= this.insets.bottom;
		i.right	= this.insets.right;
		return i;
	}

	public void paintBorder( Component c, Graphics g, int x, int y, int width, int height )
	{
		x		+= offsets.left;
		y		+= offsets.top;
		width	-= offsets.left + offsets.right;
		height	-= offsets.top + offsets.bottom;

//System.out.println( x + ", "+ y+ ", " + width + ", " + height );

		g.drawImage( img, x + width - 20, y, x + width, y + 1, 0, 0, 20, 1, c );
		g.drawImage( img, x + width - 20, y + 1, x + width, y + height - 1, 0, 1, 20, 20, c );
		g.drawImage( img, x + width - 20, y + height - 1, x + width, y + height, 0, 20, 20, 21, c );
	}
}