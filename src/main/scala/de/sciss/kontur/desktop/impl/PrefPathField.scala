package de.sciss.kontur
package desktop
package impl

import legacy.{PathEvent, PathListener, PathField, LaterInvocationManager, PreferenceEntrySync}
import java.util.prefs.{Preferences, PreferenceChangeEvent, PreferenceChangeListener}
import java.io.File

class PrefPathField(tpe: Int, dialogText: String)
  extends PathField(tpe, dialogText) with DynamicComponentImpl with PreferenceChangeListener
  with LaterInvocationManager.Listener with PreferenceEntrySync {

	private var prefs = Option.empty[Preferences]
	private var key   = Option.empty[String]
	private val lim	  = new LaterInvocationManager( this )

	private var defaultValue    = Option.empty[File]
	private var _readPrefs		  = true
	protected var _writePrefs		= true

	private val listener = new PathListener() {
    def pathChanged(e: PathEvent) {
      if( _writePrefs ) writePrefs()
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
      if (isListening) prefs.foreach { p =>
        if (_writePrefs) {
          addPathListener(listener)
        } else {
          removePathListener(listener)
        }
      }
    }
  }

  def getWritePrefs: Boolean = _writePrefs

  def setPath(f: File) {
    super.setPath(f)
    if (_writePrefs) writePrefs()
  }

  def writePrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) =>
        val oldValue  = p.get(k, "")
        val prefsPath = new File(oldValue)
        val guiPath   = getPath
        if (guiPath != prefsPath) {
          p.put(k, guiPath.getPath)
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
	 *  the path is updated in the GUi and
	 *  the PathField dispatches a PathEvent.
	 *  Likewise, if the user adjusts the path
	 *  in the GUI, the preference will be
	 *  updated. The same is true, if you
	 *  call setPath.
	 *
	 *  @param  prefs   the preferences node in which
	 *					the value is stored, or null
	 *					to disable prefs sync.
	 *  @param  key		the key used to store and recall
	 *					prefs. the value is the path
	 *					converted to a string.
	 */
	def setPreferences(prefs: Preferences, key: String) {
    if (this.prefs.isEmpty || this.key.isDefined) {
      defaultValue = Some(getPath)
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
      if (_writePrefs) addPathListener(listener)
      if (_readPrefs) {
        p.addPreferenceChangeListener(this)
        readPrefs()
      }
    }
  }

  protected def componentHidden() {
    prefs.foreach { p =>
      if (_readPrefs) prefs.removePreferenceChangeListener(this)
      if (_writePrefs) removePathListener(listener)
    }
  }

  // o instanceof PreferenceChangeEvent
  def laterInvocation(o: Any) {
    o match {
      case pce: PreferenceChangeEvent =>
        val prefsValue = pce.getNewValue
        readPrefsFromString(Some(prefsValue))
      case _ =>
    }
  }

  def readPrefs() {
    (prefs, key) match {
      case (Some(p), Some(k)) => readPrefsFromString(Option(p.get( k, null )))
      case _ =>
    }
	}

	private def readPrefsFromString(prefsValue: Option[String]) {
    val s = prefsValue.getOrElse {
      defaultValue.foreach { v =>
        setPath(v)
        if (_writePrefs) writePrefs()
      }
      return
    }

    val prefsPath = new File(s)

    //System.err.println( "lim : "+prefsPath );
		if( prefsPath != getPath ) {
			if( isListening && _writePrefs ) removePathListener( listener )
			setPathAndDispatchEvent( prefsPath )
			if( isListening && _writePrefs ) addPathListener( listener )
		}
	}

  def preferenceChange(e: PreferenceChangeEvent) {
    if (Some(e.getKey) == key) {
      lim.queue(e)
    }
  }
}