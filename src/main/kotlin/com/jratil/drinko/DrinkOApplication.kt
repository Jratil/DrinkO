package com.jratil.drinko

import javafx.application.Application
import tornadofx.App

class DrinkOApplication : App(DrinkReminderView::class) {

}

fun main() {
    Application.launch(DrinkOApplication::class.java)
}
