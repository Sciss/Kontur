package legacy;

import java.util.prefs.Preferences;

/**
 *  Objects implementing this interface
 *  state that they will store their serialized
 *  representation in a given preference entry
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.27, 25-Sep-05
 */
public interface PreferenceEntrySync
{
	/**
	 *  Enables Preferences synchronization.
	 *  This method is not thread safe and
	 *  must be called from the event thread.
	 *  When a preference change is received,
	 *  the GUI is updated and dispatches an event
	 *  to registered listeners.
	 *  Likewise, if the user adjusts the GUI
	 *  value, the preference will be
	 *  updated. The same is true, if you
	 *  call one of the value changing methods.
	 *
	 *  @param  prefs   the preferences node in which
	 *					the value is stored, or null
	 *					to disable prefs sync.
	 *  @param  key		the key used to store and recall
	 *					prefs. the value is converted
	 *					into a string.
	 */
	public void setPreferences( Preferences prefs, String key );

	public void setPreferenceNode( Preferences prefs );
	public void setPreferenceKey( String key );

	/**
	 *  Gets the recently set preference node
	 *
	 *  @return the node set with setPreferences or
	 *			null if no prefs were set
	 */
	public Preferences getPreferenceNode();

	/**
	 *  Gets the recently set preference key
	 *
	 *  @return the key set with setPreferences or
	 *			null if no prefs were set
	 */
	public String getPreferenceKey();

	public void setReadPrefs( boolean b );
	public void setWritePrefs( boolean b );
	public boolean getReadPrefs();
	public boolean getWritePrefs();
	public void readPrefs();
	public void writePrefs();
}