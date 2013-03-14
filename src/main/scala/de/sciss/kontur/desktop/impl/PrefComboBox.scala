package de.sciss.kontur
package desktop
package impl

import legacy.{StringItem, LaterInvocationManager, PreferenceEntrySync}
import java.util.prefs.{PreferenceChangeEvent, Preferences, PreferenceChangeListener}
import javax.swing.JComboBox
import java.awt.event.{ActionListener, ActionEvent}

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
class PrefComboBox extends JComboBox with DynamicComponentImpl with PreferenceChangeListener
  with LaterInvocationManager.Listener with PreferenceEntrySync {

  private var prefs = Option.empty[Preferences]
  private var key   = Option.empty[String]
  private val lim   = new LaterInvocationManager(this)

  private var defaultValue = Option.empty[Any]

  private var _readPrefs = true
  private var _writePrefs = true

  private val listener = new ActionListener() {
    def actionPerformed(e: ActionEvent) {
      if (_writePrefs) writePrefs()
    }
  }

  def setReadPrefs(b: Boolean) {
    if (b != _readPrefs) {
      _readPrefs = b
      if (isListening) prefs.foreach { p =>
        if (_readPrefs) {
          p.addPreferenceChangeListener(this)
        } else {
          p.removePreferenceChangeListener(this)
        }
      }
    }
  }

  def getReadPrefs: Boolean = _readPrefs

  def setWritePrefs(b: Boolean) {
    if (b != _writePrefs) {
      _writePrefs = b
      if ((prefs != null) && isListening) {
        if (_writePrefs) {
          addActionListener(listener)
        } else {
          removeActionListener(listener)
        }
      }
    }
  }

  def getWritePrefs: Boolean = _writePrefs

	/**
	 *  Because the items in the ComboBox
	 *  can be naturally moved, added and replaced,
	 *  it is crucial to have a non-index-based
	 *  value to store in the preferences. Since
	 *  the actual String representation of the
	 *  the items is likely to be locale specific,
	 *  it is required to add items of class
	 *  StringItem !
	 *
	 *  @param  item	the <code>StringItem</code> to add
	 *  @see	StringItem
	 */
	def addItem(item: Any) {
		super.addItem(validateItem(item))
	}

	/*  Add a new item at a specific index position
	 *  to the gadget. See {@link #addItem( Object ) addItem( Object )}
	 *  for an explanation of the <code>StringItem</code>
	 *  usage.
	 *
	 *  @param  item	the <code>StringItem</code> to add
	 *  @see	StringItem
	 */
	def insertItemAt(item: Any, idx: Int) {
		super.insertItemAt(validateItem(item), idx)
	}

	private def validateItem(item: Any): Any = item match {
    case si: StringItem => si
    case _ => new StringItem(item.toString, item.toString)
  }

	def writePrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) =>
        val item  = Option(getSelectedItem)
        val value = item match {
          case Some(si: StringItem) => Some(si.getKey)
          case _ => None
        }
        val oldValue = Option(p.get(k, null))
        if (value != oldValue) {
          value match {
            case Some(v) => p.put(k, v)
            case _ => p.remove(k)
          }
        }

      case _ =>
    }
	}

  def setPreferenceNode(prefs: Preferences) {
    setPreferences(prefs, key.orNull)
  }

  def setPreferenceKey(key: String) {
    setPreferences(prefs.orNull, key)
  }

  def setPreferences(prefs: Preferences, key: String) {
		if( this.prefs.isEmpty || this.key.isEmpty ) {
			defaultValue = getSelectedItem
		}
    val li = isListening
    if (li) componentHidden()
    this.prefs  = Option(prefs)
    this.key	  = Option(key)
	  if (li) componentShown()
	}

	def getPreferenceNode: Preferences = prefs.orNull
	def getPreferenceKey: String = key.orNull

  protected def componentShown() {
    prefs.foreach { p =>
      if (_writePrefs) addActionListener(listener)
      if (_readPrefs) {
        p.addPreferenceChangeListener(this)
        readPrefs()
      }
    }
  }

  protected def componentHidden() {
    prefs.foreach { p =>
      if (_readPrefs) p.removePreferenceChangeListener(this)
      if (_writePrefs) removeActionListener(listener)
    }
  }

    // o instanceof PreferenceChangeEvent
	def laterInvocation(o: Any) {
    o match {
      case pce: PreferenceChangeEvent =>
        val prefsValue = Option(pce.getNewValue)
        readPrefsFromString(prefsValue)
      case _ =>
    }
	}

	def readPrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) => readPrefsFromString(Option(p.get(k, null)))
      case _ =>
    }
	}

  private def readPrefsFromString (prefsValue: Option[String]) {
    if (prefsValue.isEmpty && defaultValue.isDefined) {
      if (isListening && _writePrefs) removeActionListener(listener)
      setSelectedItem(defaultValue.get)
      if (_writePrefs) writePrefs()
      if (isListening && _writePrefs) addActionListener(listener)
      return
    }
    val guiItem = getSelectedItem

    if (guiItem != null && (guiItem instanceof StringItem)) {
      guiValue = ((StringItem) guiItem).getKey
    }
    if ((prefsValue == null && guiValue != null)) {
      // thow we filter out events when preferences effectively
      // remain unchanged, it's more clean and produces less
      // overhead to temporarily remove our ActionListener
      // so we don't produce potential loops
      if (isListening && _writePrefs) removeActionListener(listener)
      super.setSelectedItem(null)   // will notify action listeners
      if (isListening && _writePrefs) addActionListener(listener)

    } else if ((prefsValue != null && guiValue == null) ||
      (prefsValue != null && !prefsValue.equals(guiValue))) {

      for (i <- 0 until getItemCount) {
        val guiItem = getItemAt(i)
        if (guiItem != null && ((StringItem) guiItem).getKey().equals(prefsValue)) {
          prefsItem = guiItem
          break
        }
      }

      // thow we filter out events when preferences effectively
      // remain unchanged, it's more clean and produces less
      // overhead to temporarily remove our ActionListener
      // so we don't produce potential loops
      if (isListening && _writePrefs) removeActionListener(listener)
      super.setSelectedItem(prefsItem)  // will notify action listeners
      if (isListening && _writePrefs) addActionListener(listener)
    }
  }

  def preferenceChange(e: PreferenceChangeEvent) {
    if (Option(e.getKey) == key) {
      lim.queue(e)
    }
  }
}