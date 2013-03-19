package de.sciss.kontur
package desktop
package impl

import java.io.File
import de.sciss.desktop.Preferences

class PrefPathField(protected val prefs: Preferences.Entry[File], default: File)(mode: PathField.Mode = PathField.Input)
  extends PathField(mode) with PreferencesWidgetImpl[File] {

  override protected def setFileAndDispatchEvent(f: File) {
    super.setFileAndDispatchEvent(f)
    updatePrefs()
  }

  override def file_=(value: File) {
    super.file_=(value)
    updatePrefs()
  }


//	private def readPrefsFromString(prefsValue: Option[String]) {
//    val s = prefsValue.getOrElse {
//      defaultValue.foreach { v =>
//        setPath(v)
//        if (_writePrefs) writePrefs()
//      }
//      return
//    }
//
//    val prefsPath = new File(s)
//
//    //System.err.println( "lim : "+prefsPath );
//		if( prefsPath != getPath ) {
//			if( isListening && _writePrefs ) removePathListener( listener )
//			setPathAndDispatchEvent( prefsPath )
//			if( isListening && _writePrefs ) addPathListener( listener )
//		}
//	}
}