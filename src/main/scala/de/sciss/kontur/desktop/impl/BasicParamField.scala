package de.sciss.kontur
package desktop
package impl

import javax.swing.{Action, Icon, BorderFactory, JPanel, ComboBoxEditor}
import legacy.{UnitLabel, NumberField, StringItem, NumberEvent, NumberListener, Param, BasicEvent, Jog, ParamSpace, DefaultUnitTranslator, EventManager}
import java.beans.{PropertyChangeEvent, PropertyChangeListener}
import java.awt.{Component, GridBagLayout, GridBagConstraints}
import collection.immutable.{IndexedSeq => IIdxSeq}
import java.util.EventListener
import java.awt.event.{ActionEvent, ActionListener}

object BasicParamField {
  trait UnitViewFactory {
 		def createView(unit: Int): Any
 	}

  object Event {
    final val VALUE = 0
    final val SPACE = 1
  }
 	final class Event(source: Any, id: Int, when: Long, val value: Param, val space: Option[ParamSpace],
                    translator: ParamSpace.Translator, val isAdjusting: Boolean)
 	  extends BasicEvent(source, id, when) {

    def getTranslatedValue(newSpace: ParamSpace): Param = translator.translate(value, newSpace)

    def incorporate(that: BasicEvent): Boolean =
      that.isInstanceOf[BasicParamField.Event] && this.getSource == that.getSource &&
        this.getID == that.getID
   }

  trait Listener extends EventListener {
    def paramValueChanged(e: Event): Unit
    def paramSpaceChanged(e: Event): Unit
  }
}
class BasicParamField(var translator: ParamSpace.Translator = new DefaultUnitTranslator)
  extends JPanel with PropertyChangeListener with EventManager.Processor with ComboBoxEditor {

  private val jog                  = new Jog()
  final protected val numberField  = new NumberField()
	final protected val unitLabel    = new UnitLabel()

	protected var spaces          = IIdxSeq.empty[ParamSpace]
	protected var currentSpace    = Option.empty[ParamSpace]

	private var elm = Option.empty[EventManager]

	var comboGate		  = true

  private val lay   = new GridBagLayout()
  private val con		= new GridBagConstraints()


  addPropertyChangeListener("JComponent.sizeVariant", new PropertyChangeListener {
    def propertyChange(pce: PropertyChangeEvent): Unit =
      numberField.putClientProperty(pce.getPropertyName, pce.getNewValue)
  })

  setLayout(lay)
  con.anchor  = GridBagConstraints.WEST
  con.fill    = GridBagConstraints.HORIZONTAL

  jog.addListener(new NumberListener() {
    def numberChanged(e: NumberEvent): Unit =
      currentSpace.foreach { space =>
        val inc     = e.getNumber.doubleValue() * space.inc
        val num     = numberField.getNumber
        val newNum  = if (space.isInteger) {
          space.fitValue(num.longValue() + inc).toLong
        } else {
          space.fitValue(num.doubleValue() + inc)
        }

        val changed = !newNum.equals(num)
        if (changed) {
          numberField.setNumber(newNum)
        }
        if (changed || !e.isAdjusting) {
          fireValueChanged(e.isAdjusting)
        }
      }
  })

  numberField.addListener(new NumberListener() {
    def numberChanged(e: NumberEvent): Unit = fireValueChanged(e.isAdjusting)
  })

  unitLabel.addActionListener(new ActionListener() {
    def actionPerformed(e: ActionEvent): Unit = {
      selectSpace(unitLabel.getSelectedIndex)

      fireSpaceChanged()
      fireValueChanged(adjusting = false)
    }
  })

  con.gridwidth   = 1
  con.gridheight  = 1
  con.gridx       = 1
  con.gridy       = 1
  con.weightx     = 0.0
  con.weighty     = 0.0
  lay.setConstraints(jog, con)
  jog.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2))
  add(jog)

  con.gridx      += 1
  con.weightx     = 1.0
  lay.setConstraints(numberField, con)
  add(numberField)

  con.gridx      += 1
  con.weightx     = 0.0
  con.gridwidth   = GridBagConstraints.REMAINDER
  lay.setConstraints(unitLabel, con)
  unitLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0))
  add(unitLabel)

  addPropertyChangeListener("font", this)
  addPropertyChangeListener("enabled", this)

	override def requestFocusInWindow(): Boolean = numberField.requestFocusInWindow()

	def addSpace(space: ParamSpace): Unit = {
		spaces :+= space
    val view = DefaultUnitViewFactory.createView(space.unit)
    view match {
      case icn: Icon  => unitLabel.addUnit(icn)
      case _          => unitLabel.addUnit(view.toString)
    }

    if (spaces.size == 1) {
      currentSpace = Some(space)
      numberField.setSpace(space)
    }
  }

  def value: Param = new Param(numberField.getNumber.doubleValue(), currentSpace.map(_.unit).getOrElse(ParamSpace.NONE))

  def value_=(p: Param): Unit = {
    val oldNum    = numberField.getNumber
    val newParam  = currentSpace.map(s => translator.translate(p, s)).getOrElse(p)
//    val spc       = currentSpace.getOrElse(p.space)

    val newNum = if (currentSpace.map(_.isInteger).getOrElse(false)) {
      newParam.value.toLong
    } else {
      newParam.value
    }
    if (newNum != oldNum) numberField.setNumber(newNum)
  }

  def setValueAndSpace(newValue: Param): Unit = {
    val spcIdx = spaces.indexWhere(spc => Some(spc) != currentSpace && spc.unit == newValue.unit)
    val newSpc = spcIdx >= 0
    if (newSpc) {
      val spc = spaces(spcIdx)
      currentSpace = Some(spc)
      numberField.setSpace(spc)
      numberField.setFlags(spc.unit & ParamSpace.SPECIAL_MASK)
      if (spcIdx != unitLabel.getSelectedIndex) {
        unitLabel.setSelectedIndex(spcIdx)
      }
    }
    value = newValue
    if (newSpc) {
      fireSpaceChanged()
    }
  }

	def space: Option[ParamSpace] = currentSpace

  def space_=(value: Option[ParamSpace]): Unit = {
    value match {
      case Some(spc) =>
        val i = spaces.indexOf(value)
        if (i < 0) new IllegalArgumentException(s"Illegal space switch $value")
        selectSpace(i)

      case _ =>
        currentSpace = None
    }
  }

  def cycling: Boolean = unitLabel.getCycling
  def cycling_=(value: Boolean): Unit = unitLabel.setCycling(value)

	protected def selectSpace(selectedIdx: Int): Unit = {
		if( selectedIdx < 0 || selectedIdx >= spaces.size ) {
      currentSpace = None
      return
    }

    val oldSpace  = currentSpace
    val spc       = spaces(selectedIdx)
    currentSpace  = Some(spc)

    val oldNum = Option(numberField.getNumber)
    val oldParam = new Param(
      oldNum.map(_.doubleValue()).getOrElse(oldSpace.map(_.reset).getOrElse(0.0)),
      oldSpace.map(_.unit).getOrElse(ParamSpace.NONE)
    )
    val newParam = translator.translate(oldParam, spc)

    val newNum = if (spc.isInteger) {
      newParam.value.toLong
    } else {
      newParam.value
    }

    numberField.setSpace(spc)
    numberField.setFlags(spc.unit & ParamSpace.SPECIAL_MASK)
    if (!newNum.equals(oldNum)) numberField.setNumber(newNum)

    if (selectedIdx != unitLabel.getSelectedIndex) {
      unitLabel.setSelectedIndex(selectedIdx)
    }
  }

  // --- listener registration ---

	/**
	 *  Register a <code>NumberListener</code>
	 *  which will be informed about changes of
	 *  the gadgets content.
	 *
	 *  @param  listener	the <code>NumberListener</code> to register
	 *  @see	de.sciss.app.EventManager#addListener( Object )
	 */
  def addListener(listener: BasicParamField.Listener): Unit = this.synchronized {
    val e = elm.getOrElse {
      val res = new EventManager(this)
      elm = Some(res)
      res
    }
    e.addListener(listener)
  }

  /**
	 *  Unregister a <code>NumberListener</code>
	 *  from receiving number change events.
	 *
	 *  @param  listener	the <code>NumberListener</code> to unregister
	 *  @see	de.sciss.app.EventManager#removeListener( Object )
	 */
  def removeListener(listener: BasicParamField.Listener): Unit =
    elm.foreach(_.removeListener(listener))

  def processEvent(e: BasicEvent): Unit =
    for (e1 <- elm; i <- 0 until e1.countListeners) {
      val listener = e1.getListener(i).asInstanceOf[BasicParamField.Listener]
      e match {
        case pe: BasicParamField.Event if pe.getID == BasicParamField.Event.VALUE =>
          listener.paramValueChanged(pe)

        case pe: BasicParamField.Event if pe.getID == BasicParamField.Event.SPACE =>
          listener.paramSpaceChanged(pe)

        case _ =>
      }
    }

	protected def fireValueChanged(adjusting: Boolean): Unit =
    elm.foreach(_.dispatchEvent(
      new BasicParamField.Event(this, BasicParamField.Event.VALUE, System.currentTimeMillis(),
        value, space, translator, isAdjusting = adjusting)
    ))

	protected def fireSpaceChanged(): Unit =
    elm.foreach(_.dispatchEvent( new BasicParamField.Event( this, BasicParamField.Event.SPACE, System.currentTimeMillis(),
				value, space, translator, false ))
		)

// ------------------- PropertyChangeListener interface -------------------

	/*
	 *  Forwards <code>Font</code> property
	 *  changes to the child gadgets
	 */
  def propertyChange(e: PropertyChangeEvent): Unit =
    if (e.getPropertyName == "font") {
      val fnt = getFont
      numberField.setFont(fnt)
      unitLabel.setFont(fnt)
    } else if (e.getPropertyName == "enabled") {
      val enabled = isEnabled
      jog.setEnabled(enabled)
      numberField.setEnabled(enabled)
      unitLabel.setEnabled(enabled)
    }

  // ------------------- ComboBoxEditor interface -------------------

	def getEditorComponent: Component = this

	def getItem: AnyRef = {
		val a		  = unitLabel.getSelectedUnit
		val unit	= if (a == null) null else a.getValue( Action.NAME ).toString
		new StringItem( value.toString, if (unit == null)
				numberField.getText else s"${numberField.getText} $unit")
	}

  def setItem(it: AnyRef): Unit = {
 		if( !comboGate || (it == null) ) return

    it match {
      case p: Param => value = p
      case si: StringItem => value = Param.valueOf(si.getKey)
    }
 	}

  def selectAll(): Unit = {
    numberField.requestFocus()
    numberField.selectAll()
  }

  def addActionListener(l: ActionListener): Unit =
    numberField.addActionListener(l)   // XXX only until we have multiple units!

  def removeActionListener(l: ActionListener): Unit =
    numberField.removeActionListener(l)

  override def getBaseline(width: Int, height: Int) =
    numberField.getBaseline(width, height) + numberField.getY

  def setEditable(b: Boolean): Unit = numberField.setEditable(b)

  def isEditable = numberField.isEditable
}