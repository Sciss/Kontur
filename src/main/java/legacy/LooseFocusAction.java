package legacy;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class LooseFocusAction
extends AbstractAction
{
	private final JComponent c;

	public LooseFocusAction( JComponent c )
	{
		super();
		this.c	= c;
		c.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), "lost" );
		c.getActionMap().put( "lost", this );
	}

	public void actionPerformed( ActionEvent e )
	{
		final JRootPane rp = SwingUtilities.getRootPane( c );
		if( rp != null ) rp.requestFocus();
	}
}