package legacy;

import java.util.Comparator;

/**
 *  A simple structure encompassing
 *  a string and an associated value.
 *  While the string is usually used
 *  as an internal, locale-independant
 *  lookup key, the object is usually
 *  something that gets presented to
 *  the user on the GUI.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.14, 27-Jan-09
 *
 *  @see	de.sciss.gui.PrefComboBox#addItem( Object )
 *  @see	de.sciss.io.AudioFileDescr#getFormatItems()
 */
public class StringItem
{
	private final String key;
	private final Object value;

	public static final Comparator keyComparator = new Comparator() {
		public int compare( Object a, Object b )
		{
			final StringItem sa = (StringItem) a;
			final StringItem sb = (StringItem) b;

			return sa.key.compareTo( sb.key );
		}
	};

	public static final Comparator valueComparator = new Comparator() {
		public int compare( Object a, Object b )
		{
			final StringItem sa = (StringItem) a;
			final StringItem sb = (StringItem) b;

			if( (sa.value instanceof Comparable) &&
				(sb.value instanceof Comparable) ) {

				return ((Comparable) sa.value).compareTo( (Comparable) sb.value );
			} else {
				return sa.value.toString().compareTo( sb.value.toString() );
			}
		}
	};

	/**
	 *  Constructs a new immutable StringItem.
	 *
	 *  @param  key		lookup key
	 *  @param  value   associated object, usually
	 *					human readable
	 */
	public StringItem( String key, Object value )
	{
		this.key	= key;
		this.value  = value;
	}

	/**
	 *  Queries the key.
	 *
	 *  @return the key string which was used to construct the item
	 */
	public String getKey()
	{
		return key;
	}

	/**
	 *  Queries the value object.
	 *
	 *  @return the value object which was used to construct the item
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 *  Compares this object
	 *  to another object
	 *
	 *  @param  o   an object which shall be compared to the StringItem
	 *  @return <code>true</code>, if the provided object is a
	 *			StringItem whose key and value match this item's key and
	 *			value (using their respective <code>equals</code> methods)
	 */
	public boolean equals( Object o )
	{
		if( o instanceof StringItem ) {
			return( this.key.equals( ((StringItem) o).key ) &&
					this.value.equals( ((StringItem) o).value ));
		} else {
			return false;
		}
	}

	public int hashCode()
	{
		return( key.hashCode() ^ value.hashCode() );
	}

	/**
	 *  Returns a string representation
	 *  of the <strong>value</strong>.
	 *  This is essential, since it allows
	 *  us to add StringItems directly to
	 *  ComboBoxes and the like whise call
	 *  this method to get a GUI representation
	 *  text.
	 *
	 *  @return		basically <code>value.toString()</code>
	 */
	public String toString()
	{
		return value.toString();
	}
}
