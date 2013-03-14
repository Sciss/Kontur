package de.sciss.kontur
package desktop
package impl

import java.util.prefs.{PreferenceChangeEvent, Preferences, PreferenceChangeListener}
import legacy.{LaterInvocationManager, PreferenceEntrySync, Param, ParamSpace, DefaultUnitTranslator}

class PrefParamField(translator: ParamSpace.Translator = new DefaultUnitTranslator)
  extends BasicParamField(translator)
  with DynamicComponentImpl with PreferenceChangeListener with LaterInvocationManager.Listener with PreferenceEntrySync {

	private var prefs = Option.empty[Preferences]
	private var key   = Option.empty[String]
  private val lim   = new LaterInvocationManager(this)

  private var defaultValue  = Option.empty[Param]

	private var _readPrefs    = true
	protected var _writePrefs = true

  private val listener = new BasicParamField.Listener() {
    def paramValueChanged(e: BasicParamField.Event) {
      if (e.isAdjusting) return
      if (_writePrefs) writePrefs()
    }

    def paramSpaceChanged(e: BasicParamField.Event) {
      if (_writePrefs) writePrefs()
    }
  }

	def getReadPrefs = _readPrefs
  def setReadPrefs(b: Boolean) {
    if (b == _readPrefs) return
    _readPrefs = b
    if (isListening) prefs.foreach { p =>
      if (_readPrefs) {
        p.addPreferenceChangeListener(this)
      } else {
        p.removePreferenceChangeListener(this)
      }
    }
  }

  def getWritePrefs = _writePrefs

  def setWritePrefs(b: Boolean) {
		if( b == _writePrefs ) return
    _writePrefs	= b
    if (isListening) prefs.foreach { p =>
      if (_writePrefs) {
        this.addListener(listener)
      } else {
        this.removeListener(listener)
      }
    }
	}

	def writePrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) =>
        val prefsStr    = Option(p.get(k, null))
        val prefsValue	= prefsStr.map(Param.valueOf _)
        val guiValue	  = value
        if (prefsValue != Some(guiValue)) {
          p.put(k, guiValue.toString)
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

  /**
	 *  Enable Preferences synchronization.
	 *  This method is not thread safe and
	 *  must be called from the event thread.
	 *  When a preference change is received,
	 *  the number is updated in the GUI and
	 *  the ParamField dispatches a ParamEvent.
	 *  Likewise, if the user adjusts the number
	 *  in the GUI, the preference will be
	 *  updated. The same is true, if you
	 *  call setParam.
	 *  
	 *  @param  prefs   the preferences node in which
	 *					the value is stored, or null
	 *					to disable prefs sync.
	 *  @param  key		the key used to store and recall
	 *					prefs. the value is the number
	 *					converted to a string.
	 */
	def setPreferences(prefs: Preferences, key: String) {
		if( (this.prefs.isEmpty) || (this.key.isEmpty) ) {
			defaultValue = Some(value)
		}
    val li = isListening
    if (li) unregisterPrefs()
    this.prefs  = Option(prefs)
    this.key	  = Option(key)
    if (li) registerPrefs()
	}

	def getPreferenceNode: Preferences = prefs.orNull
	def getPreferenceKey: String = key.orNull
	
  private def registerPrefs() {
    prefs.foreach { p =>
      if (_writePrefs) this.addListener(listener)
      if (_readPrefs) {
        p.addPreferenceChangeListener(this)
        readPrefs()
      }
    }
  }

  private def unregisterPrefs() {
    prefs.foreach { p =>
      if (_readPrefs) p.removePreferenceChangeListener(this)
      if (_writePrefs) this.removeListener(listener)
    }
	}

  protected def dynamicComponent = this
  protected def componentShown () { registerPrefs  () }
	protected def componentHidden() { unregisterPrefs() }

	// o instanceof PreferenceChangeEvent
	def laterInvocation(o: Any) {
    o match {
      case e: PreferenceChangeEvent => readPrefsFromString(Option(e.getNewValue))
      case _ =>
    }
	}

  def readPrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) => readPrefsFromString(Option(p.get(k, null)))
      case _ =>
    }
  }

  def readPrefsFromString(prefStr: Option[String]) {
    val s = prefsStr.getOrElse {
      defaultValue.foreach { v =>
        value = v
        if (_writePrefs) writePrefs()
      }
      return
    }
		
		val sepIdx		= s.indexOf( ' ' )
		val guiValue	= value

		val (prefsValue, newSpace) = try {
			val _prefsParam = if( sepIdx >= 0 ) {
				new Param(s.substring(0, sepIdx).toDouble,
				          ParamSpace.stringToUnit(s.substring(sepIdx + 1)))
			} else {
				new Param(s.toDouble, guiValue.unit)	  // backward compatibility to number fields
			}
			
			if( _prefsParam.unit != guiValue.unit ) {	// see if there's another space
        val _newSpace = spaces.find(_.unit == _prefsParam.unit)
        if (_newSpace.isDefined) {
          (_prefsParam, _newSpace)
        } else {
          (translator.translate(_prefsParam, currentSpace.get), None)
        }
      } else {
        (_prefsParam, None)
      }
		}
		catch {
      case e: NumberFormatException => (guiValue, None)
		}

    if (prefsValue != guiValue) {
      // thow we filter out events when preferences effectively
      // remain unchanged, it's more clean and produces less
      // overhead to temporarily remove our ParamListener
      // so we don't produce potential loops
      if (isListening && _writePrefs) this.removeListener(listener)
      if (newSpace.isDefined) {
        space = newSpace
        fireSpaceChanged()
      }
      value = prefsValue
      fireValueChanged(adjusting = false)
      if (isListening && _writePrefs) this.addListener(listener)
    }
  }
	
	def preferenceChange(e: PreferenceChangeEvent) {
    if (Option(e.getKey) == key) lim.queue(e)
  }

  override def item_=(it: Any) {
    if (!comboGate || it == null) return
		super.setItem(it)
    if (_writePrefs) writePrefs()
  }
}