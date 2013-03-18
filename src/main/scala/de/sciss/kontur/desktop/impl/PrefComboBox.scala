package de.sciss.kontur
package desktop
package impl

import javax.swing.JComboBox
import de.sciss.desktop.Preferences

/**
 *  Equips a normal JComboBox with
 *  preference storing / recalling capabilities.
 *  To preserve maximum future compatibility,
 *  we decided to not override setSelectedItem()
 *  and the like but to install an internal
 *  ActionListener. Thus, there are two ways
 *  to alter the gadget state, either by invoking
 *  the setSelectedIndex/Item() methods or by
 *  changing the associated preferences.
 *  The whole mechanism would be much simpler
 *  if we reduced listening to the preference
 *  changes, but a) this wouldn't track user
 *  GUI activities, b) the PrefComboBox can
 *  be used with preferences set to null.
 *  When a preference change occurs, the
 *  setSelectedItem() method is called, allowing
 *  clients to add ActionListeners to the
 *  gadget in case they don't want to deal
 *  with preferences. However, when possible
 *  it is recommended to use PreferenceChangeListener
 *  mechanisms.
 *
 *  @see		java.util.prefs.PreferenceChangeListener
 *  @see		StringItem
 */
class PrefComboBox[A](protected val prefs: Preferences.Entry[A])
  extends JComboBox with PreferencesWidgetImpl[A] {

//  /**
//	 *  Because the items in the ComboBox
//	 *  can be naturally moved, added and replaced,
//	 *  it is crucial to have a non-index-based
//	 *  value to store in the preferences. Since
//	 *  the actual String representation of the
//	 *  the items is likely to be locale specific,
//	 *  it is required to add items of class
//	 *  StringItem !
//	 *
//	 *  @param  item	the <code>StringItem</code> to add
//	 *  @see	StringItem
//	 */
//	def addItem(item: Any) {
//		super.addItem(validateItem(item))
//	}
//
//	/*  Add a new item at a specific index position
//	 *  to the gadget. See {@link #addItem( Object ) addItem( Object )}
//	 *  for an explanation of the <code>StringItem</code>
//	 *  usage.
//	 *
//	 *  @param  item	the <code>StringItem</code> to add
//	 *  @see	StringItem
//	 */
//	def insertItemAt(item: Any, idx: Int) {
//		super.insertItemAt(validateItem(item), idx)
//	}
//
//	private def validateItem(item: Any): Any = item match {
//    case si: StringItem => si
//    case _ => new StringItem(item.toString, item.toString)
//  }


//  private def readPrefsFromString (prefsValue: Option[String]) {
//    if (prefsValue.isEmpty && defaultValue.isDefined) {
//      if (isListening && _writePrefs) removeActionListener(listener)
//      setSelectedItem(defaultValue.get)
//      if (_writePrefs) writePrefs()
//      if (isListening && _writePrefs) addActionListener(listener)
//      return
//    }
//    val guiItem = getSelectedItem
//
//    if (guiItem != null && (guiItem instanceof StringItem)) {
//      guiValue = ((StringItem) guiItem).getKey
//    }
//    if ((prefsValue == null && guiValue != null)) {
//      // thow we filter out events when preferences effectively
//      // remain unchanged, it's more clean and produces less
//      // overhead to temporarily remove our ActionListener
//      // so we don't produce potential loops
//      if (isListening && _writePrefs) removeActionListener(listener)
//      super.setSelectedItem(null)   // will notify action listeners
//      if (isListening && _writePrefs) addActionListener(listener)
//
//    } else if ((prefsValue != null && guiValue == null) ||
//      (prefsValue != null && !prefsValue.equals(guiValue))) {
//
//      for (i <- 0 until getItemCount) {
//        val guiItem = getItemAt(i)
//        if (guiItem != null && ((StringItem) guiItem).getKey().equals(prefsValue)) {
//          prefsItem = guiItem
//          break
//        }
//      }
//
//      // thow we filter out events when preferences effectively
//      // remain unchanged, it's more clean and produces less
//      // overhead to temporarily remove our ActionListener
//      // so we don't produce potential loops
//      if (isListening && _writePrefs) removeActionListener(listener)
//      super.setSelectedItem(prefsItem)  // will notify action listeners
//      if (isListening && _writePrefs) addActionListener(listener)
//    }
//  }
}