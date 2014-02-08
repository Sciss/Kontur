/*
 *  PrefPathField.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package desktop
package impl

import java.io.File
import de.sciss.desktop.Preferences

class PrefPathField(protected val prefs: Preferences.Entry[File], default: File)(mode: PathField.Mode = PathField.Input)
  extends PathField(mode) with PreferencesWidgetImpl[File] {

  override protected def setFileAndDispatchEvent(f: File): Unit = {
    super.setFileAndDispatchEvent(f)
    updatePrefs()
  }

  override def file_=(value: File): Unit = {
    super.file_=(value)
    updatePrefs()
  }

  protected def dynamicComponent = this

  protected def value = file
  protected def value_=(file: File): Unit = this.file = file

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