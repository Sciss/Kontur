package de.sciss.kontur
package desktop
package impl

import legacy.PathField
import java.io.File

class PrefPathField(protected val prefs: Preferences.Entry[File], default: File)(tpe: Int, dialogText: String)
  extends PathField(tpe, dialogText) with PreferencesWidgetImpl[File] {

  protected def prefsType = Preferences.Type.File

  override protected def setPathAndDispatchEvent(path: File) {
    super.setPathAndDispatchEvent(path)
    updatePrefs()
  }

  override def setPath(f: File) {
    super.setPath(f)
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