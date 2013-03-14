package legacy;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;

/**
 *  @version	0.37, 25-Feb-08
 */
public class TimeFormatter
extends DefaultFormatter
{
	private final DocumentFilter	docFilter	= new DocFilter();
	private final NavigationFilter	navFilter	= new NavFilter();

	private static final String	sepChars		= ":.";
	private static final String	numChars		= "0123456789";

	private TimeFormat				tf			= null;

	public TimeFormatter()
	{
		super();
		setAllowsInvalid( true );
//		setOverwriteMode( true );	// fucked up because we use our own filter
		setCommitsOnValidEdit( false );
	}

	public void setFormat( TimeFormat tf )
	{
		this.tf	= tf;
	}

	public Object stringToValue( String string )
	throws ParseException
	{
		if( tf != null ) {
			return tf.parseTime( string );
		} else {
			throw new ParseException( "Format hasn't been set", 0 );
		}
	}

	public String valueToString( Object value )
    throws ParseException
	{
		if( tf != null ) {
			return tf.formatTime( (Number) value );
		} else {
			return value.toString();
		}
	}

	protected JFormattedTextField getFormattedTextField()
	{
		return super.getFormattedTextField();
	}

	protected DocumentFilter getDocumentFilter()
	{
		return docFilter;
	}

	protected NavigationFilter getNavigationFilter()
	{
		return navFilter;
	}

	private class DocFilter
	extends DocumentFilter
	{
		protected DocFilter() { /* empty */ }

		public void insertString( DocumentFilter.FilterBypass fb, int off, String s, AttributeSet attr )
		throws BadLocationException
		{
			replace( fb, off, s.length(), s, attr );
//System.err.println( "insertString "+off+" : "+s );
		}

		public void remove( DocumentFilter.FilterBypass fb, int off, int len )
		throws BadLocationException
		{
//System.err.println( "remove "+off+" : "+len );
//			super.remove( fb, off, len );
			final String s = fb.getDocument().getText( 0, off + 1 );

			for( int i = 0; i <= off; i++ ) {
				if( sepChars.indexOf( s.charAt( i )) >= 0 ) return;
			}

			super.remove( fb, off, len );
		}

		public void replace( DocumentFilter.FilterBypass fb, int off, int len, String s, AttributeSet attr )
		throws BadLocationException
		{
//			final String oldTxt = fb.getDocument().getText( 0, off + len );
			final String oldTxt = getFormattedTextField().getText();

			for( int i = 0; i < off; i++ ) {
				if( sepChars.indexOf( oldTxt.charAt( i )) >= 0 ) {
					len = s.length();
					if( off + len > oldTxt.length() ) return;
					break;
				}
			}
			if( !(s.equals( "-" ) && (off == 0)) ) {
				for( int i = 0; i < s.length(); i++ ) {
					if( numChars.indexOf( s.charAt( i )) == -1 ) return;
				}
			}
//			super.insertString( fb, off, s, attr );
//
//			len = s.length();	// overwrite

			char			ch1, ch2;

			for( int i = 0, j = off; i < Math.min( s.length(), len ); i++, j++ ) {
				ch1 = oldTxt.charAt( j );
				ch2	= s.charAt( i );
				if( sepChars.indexOf( ch1 ) >= 0 ) {
					if( sepChars.indexOf( ch2 ) == -1 ) {
						replace( fb, off + 1, len, s, attr );
						try {
							getFormattedTextField().setCaretPosition( off + 1 + len );
						}
						catch( IllegalArgumentException e1 ) { /* ignored */ }
						return;
					}
				} else {
					if( numChars.indexOf( ch2 ) >= 0 ) continue;
//					if( ch2 == ' ' ) {
//						s2	= fb.getDocument().getText( 0, off + i );
//						for( int j = 0; j < s2.length(); j++ ) {
//							if( s2.charAt( j ) != ' ' ) return;
//						}
//					}
					return;
				}
			}

//System.err.println( "replace "+off+" : "+len+" : "+s );
//			super.replace( fb, off, len, s, attr );
			fb.replace( off, len, s, attr );
//			fb.remove( off, len );
//			fb.insertString( off, s, attr );
		}
	}

	private static class NavFilter
	extends NavigationFilter
	implements SwingConstants
	{
		protected NavFilter() { /* empty */ }

		public void setDot( NavigationFilter.FilterBypass fb, int dot, Position.Bias bias )
		{
			super.setDot( fb, dot, bias );
		}

		public void moveDot( NavigationFilter.FilterBypass fb, int dot, Position.Bias bias )
		{
			super.moveDot( fb, dot, bias );
		}

		public int getNextVisualPositionFrom( JTextComponent c, int pos, Position.Bias bias,
											  int dir, Position.Bias[] biasRet )
		throws BadLocationException
		{
			final String s = c.getText();
			if( (dir == WEST) && (pos > 0) && (pos <= s.length()) ) {
				if( sepChars.indexOf( s.charAt( pos - 1 )) >= 0 ) {
					pos--;
				}
			} else if( (dir == EAST) && (pos + 1 < s.length()) ) {
				if( sepChars.indexOf( s.charAt( pos + 1 )) >= 0 ) {
					pos++;
				}
			}
			return super.getNextVisualPositionFrom( c, pos, bias, dir, biasRet );
		}
	}
}