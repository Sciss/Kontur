package legacy;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.swing.JTextField;

/**
 *  Subclass of <code>JTextField</code> that
 *  adds <code>setPaint</code> and <code>getPaint</code>
 *  methods and overrides the
 *  <code>paintComponent</code> method. In
 *  <code>paintComponent</code> it simply
 *  fills the components area with the
 *  specified paint. Usually this paint
 *  is translucent allowing a textfield
 *  to get for example a 'blueish texture'
 *  which is what <code>PathField</code> does.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.16, 05-May-06
 *
 *  @see	PathField
 */
public class ColoredTextField
extends JTextField
{
	private Paint p   = null;

	/**
	 *  Constructs a new ColouredTextField
	 */
	public ColoredTextField()
	{
		super();
		init();
	}

	/**
	 *  Constructs a new ColouredTextField
	 *
	 *  @param  columns		number of columns for the textfield
	 */
	public ColoredTextField( int columns )
	{
		super( columns );
		init();
	}

	private void init()
	{
		// nothing
	}

	/**
	 *  Changes the colour of the text field.
	 *
	 *  @param  p   the new colour. Use translucent
	 *				colour (RGBA) for the normal
	 *				text component to shine through.
	 */
	public void setPaint( Paint p )
	{
		this.p = p;
		repaint();
	}

	/**
	 *  Return the current attached colour.
	 *
	 *  @return		the current colour or <code>null</code>
	 *				if no colour was set
	 */
	public Paint getPaint()
	{
		return p;
	}

	/**
	 *  Paint the text field component and
	 *  fill its area with the specified
	 *  paint afterwards. If no paint was
	 *  specified, the text field will just
	 *  look like a normal <code>JTextField</code>.
	 */
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		if( p == null ) return;

		Dimension   d   = getSize();
		Graphics2D  g2  = (Graphics2D) g;
		Paint		op  = g2.getPaint();

		g2.setPaint( p );
		g2.fillRect( 0, 0, d.width, d.height );
		g2.setPaint( op );
	}
}