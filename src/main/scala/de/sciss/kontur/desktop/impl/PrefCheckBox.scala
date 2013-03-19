package de.sciss.kontur.desktop.impl

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.{Icon, Action, JCheckBox}
import de.sciss.desktop.Preferences

/**
 * Equips a normal JCheckBox with
 * preference storing / recalling capabilities.
 * To preserve maximum future compatibility,
 * we decided to not override setSelected()
 * and the like but to install an internal
 * ActionListener. Thus, there are two ways
 * to alter the gadget state, either by invoking
 * the doClick() methods (DON'T USE setSelected()
 * because it doesn't fire events) or by
 * changing the associated preferences.
 * The whole mechanism would be much simpler
 * if we reduced listening to the preference
 * changes, but a) this wouldn't track user
 * GUI activities, b) the PrefCheckBox can
 * be used with preferences set to null.
 * When a preference change occurs, the
 * doClick() method is called, allowing
 * clients to add ActionListeners to the
 * gadget in case they don't want to deal
 * with preferences. However, when possible
 * it is recommended to use PreferenceChangeListener
 * mechanisms.
 */
class PrefCheckBox(protected val prefs: Preferences.Entry[Boolean], protected val default: Boolean)
                  (text: String = null, icon: Icon = null)
  extends JCheckBox(text, icon, default) with PreferencesWidgetImpl[Boolean] {

//  // Java overloading orgy
//  def this(prefs: Preferences.Type[Boolean], default: Boolean)()             { this(prefs, default)(null: String, null: Icon) }
//  def this(prefs: Preferences.Type[Boolean], default: Boolean)(icon: Icon)   { this(prefs, default)(null: String, icon) }
//  def this(prefs: Preferences.Type[Boolean], default: Boolean)(text: String) { this(prefs, default)(text, null: Icon) }
//
//  def this(prefs: Preferences.Type[Boolean], default: Boolean)(a: Action) {
//    this(prefs, default)()
//    setAction(a)
//  }

  private object listener extends ActionListener {
    def actionPerformed(e: ActionEvent) {
      updatePrefs()
    }
  }

  def value: Boolean = isSelected

  def value_=(b: Boolean) {
    val guiState = isSelected
    if (b != guiState) {
      if (isListening) removeActionListener(listener)
      doClick()
      if (isListening) addActionListener(listener)
    }
  }
}

