/*
 *  ActionQueryDuration.scala
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
package gui

import session.Timeline
import legacy.{GUIUtil, SpringPanel, DefaultUnitTranslator, ParamSpace, Param}
import desktop.impl.BasicParamField
import swing.{UIElement, Action, Component}
import de.sciss.desktop.{OptionPane, Window}

abstract class ActionQueryDuration(title: String) extends Action(title) {
  private var value = Option.empty[Param]
  private var space = Option.empty[ParamSpace]

  final def apply(): Unit = {
    val msgPane     = new SpringPanel(4, 2, 4, 2)
    val timeTrans   = new DefaultUnitTranslator()
    val ggDuration  = new BasicParamField(timeTrans)
    ggDuration.addSpace(ParamSpace.spcTimeHHMMSS)
    ggDuration.addSpace(ParamSpace.spcTimeSmps)
    ggDuration.addSpace(ParamSpace.spcTimeMillis)
    ggDuration.addSpace(ParamSpace.spcTimePercentF)
    msgPane.gridAdd(ggDuration, 0, 0)
    msgPane.makeCompactGrid()
    GUIUtil.setInitialDialogFocus(ggDuration)

    val tl = timeline // timelineView.timeline
    timeTrans.setLengthAndRate(tl.span.length, tl.rate)

    ggDuration.value = value getOrElse initialValue
    ggDuration.space = space

    val op  = OptionPane.confirmation(message = Component.wrap(msgPane), messageType = OptionPane.Message.Question,
      optionType = OptionPane.Options.OkCancel)
    op.title    = title
    val result  = Window.showDialog(parent, op)

    if (result == OptionPane.Result.Ok) {
      val v = ggDuration.value
      value = Some(v)
      space = ggDuration.space
      // val durationSmps = timeTrans.translate( v, ParamSpace.spcTimeSmps ).`val`
      // initiate( durationSmps.toLong )
      initiate(v, timeTrans)
    } else {
      value = None
      space = None
    }
  }

  final protected def editName: String = {
    val name = title
    if (name endsWith "...") name.substring(0, name.length - 3) else name
  }

  protected def initialValue: Param
  protected def timeline: Timeline
  protected def initiate(v: Param, trans: ParamSpace.Translator): Unit
  protected def parent: UIElement
}