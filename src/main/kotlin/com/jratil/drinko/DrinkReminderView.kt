package com.jratil.drinko

import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import javafx.scene.control.ToggleGroup
import kotlinx.coroutines.Job
import org.controlsfx.control.Notifications
import tornadofx.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @author jj_wen
 * @date 2023/3/29 11:13
 **/
class DrinkReminderView : View() {
    private val interval = SimpleStringProperty()
    private val unit = SimpleBooleanProperty(true) // true for seconds, false for minutes
    private val nextReminderTime = SimpleStringProperty()
    private val remainingTime = SimpleStringProperty()
    private var timerJob: Task<*>? = null
    private var lastReminderTime: LocalDateTime? = null
    private val timerRunning = SimpleBooleanProperty(false)

    private lateinit var toggleGroup: ToggleGroup

    override fun onDock() {
        super.onDock()
        unit.bind(toggleGroup.selectedValueProperty())
    }

    override val root = vbox {
        paddingAll = 20.0
        spacing = 10.0

        hbox {
            spacing = 10.0
            textfield(interval) {
                promptText = "Interval"
                prefWidth = 100.0
            }
            togglegroup {
                toggleGroup = this
                spacing = 5.0
                radiobutton("Seconds", value = true)
                radiobutton("Minutes", value = false)
            }
        }

        hbox {
            spacing = 10.0
            button("Start") {
                action { startTimer() }
                disableProperty().bind(timerRunning)
            }
            button("Stop") {
                action { stopTimer() }
                disableProperty().bind(timerRunning.not())
            }
        }

        hbox {
            spacing = 10.0
            label("Next reminder:")
            label(nextReminderTime)
        }

        hbox {
            spacing = 10.0
            label("Remaining time:")
            label(remainingTime)
        }
    }

    private fun startTimer() {
        timerRunning.set(true)

        val duration = interval.get().toLongOrNull() ?: return
        val timeUnit = if (unit.get()) Duration.ofSeconds(1) else Duration.ofMinutes(1)
        val intervalDuration = Duration.ofSeconds(duration).multipliedBy(timeUnit.seconds)

        // 计算下一次提醒的时间
        var scheduledReminderTime = LocalDateTime.now().plus(intervalDuration)

        // 将下一次提醒的时间和剩余时间显示出来
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        this@DrinkReminderView.nextReminderTime.set(scheduledReminderTime.format(formatter))

        timerJob?.cancel()
        timerJob = runAsync {
            while (true) {
                val now = LocalDateTime.now()
                var remainingDuration = Duration.between(now, scheduledReminderTime)

                runAsync {
                    if (remainingDuration.isNegative) {
                        scheduledReminderTime.plus(intervalDuration).let {
                            showNotification()
                            scheduledReminderTime = it
                            remainingDuration = Duration.between(now, scheduledReminderTime)
                        }
                    }
                }
                runLater {
                    remainingDuration.toMinutes().let {
                        val minutes = remainingDuration.toMinutes()
                        val seconds = remainingDuration.seconds % 60 + 1
                        remainingTime.set("$minutes min $seconds sec")
                        nextReminderTime.set(scheduledReminderTime.format(DateTimeFormatter.ofPattern("yyyy-dd-MM HH:mm:ss")))
                    }
                }

                Thread.sleep(500)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        lastReminderTime = null
        nextReminderTime.set("")
        remainingTime.set("")
        timerRunning.set(false)
    }

    private fun showNotification() {
        runLater {
            Notifications.create()
                .title("Drink Reminder")
                .text("It's time to drink some water!")
                .showInformation()
        }
    }
}
