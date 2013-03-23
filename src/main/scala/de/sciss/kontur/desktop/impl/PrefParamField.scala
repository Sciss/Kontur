package de.sciss.kontur
package desktop
package impl

import legacy.{Param, ParamSpace, DefaultUnitTranslator}
import de.sciss.desktop.Preferences

class PrefParamField(protected val prefs: Preferences.Entry[Param], default: Param)
                    (translator: ParamSpace.Translator = new DefaultUnitTranslator)
  extends BasicParamField(translator)
  with PreferencesWidgetImpl[Param] {

  override protected def fireValueChanged(adjusting: Boolean) {
    super.fireValueChanged(adjusting)
    if (!adjusting) updatePrefs()
  }

  protected def dynamicComponent = this

//  def readPrefsFromString(prefsStr: Option[String]) {
//    val s = prefsStr.getOrElse {
//      defaultValue.foreach { v =>
//        value = v
//        if (_writePrefs) writePrefs()
//      }
//      return
//    }
//
//		val sepIdx		= s.indexOf( ' ' )
//		val guiValue	= value
//
//		val (prefsValue, newSpace) = try {
//			val _prefsParam = if( sepIdx >= 0 ) {
//				new Param(s.substring(0, sepIdx).toDouble,
//				          ParamSpace.stringToUnit(s.substring(sepIdx + 1)))
//			} else {
//				new Param(s.toDouble, guiValue.unit)	  // backward compatibility to number fields
//			}
//
//			if( _prefsParam.unit != guiValue.unit ) {	// see if there's another space
//        val _newSpace = spaces.find(_.unit == _prefsParam.unit)
//        if (_newSpace.isDefined) {
//          (_prefsParam, _newSpace)
//        } else {
//          (translator.translate(_prefsParam, currentSpace.get), None)
//        }
//      } else {
//        (_prefsParam, None)
//      }
//		}
//		catch {
//      case e: NumberFormatException => (guiValue, None)
//		}
//
//    if (prefsValue != guiValue) {
//      // thow we filter out events when preferences effectively
//      // remain unchanged, it's more clean and produces less
//      // overhead to temporarily remove our ParamListener
//      // so we don't produce potential loops
//      if (isListening && _writePrefs) this.removeListener(listener)
//      if (newSpace.isDefined) {
//        space = newSpace
//        fireSpaceChanged()
//      }
//      value = prefsValue
//      fireValueChanged(adjusting = false)
//      if (isListening && _writePrefs) this.addListener(listener)
//    }
//  }
	
  override def setItem(it: AnyRef) {
    if (!comboGate || it == null) return
		super.setItem(it)
    updatePrefs()
  }
}