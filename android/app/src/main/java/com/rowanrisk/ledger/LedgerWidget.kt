package com.rowanrisk.ledger

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate

/** Dark olive surface, same palette as the web app's dark theme. Fixed so it reads on any wallpaper. */
private object P {
    val bg = ColorProvider(Color(0xFF1E2117))
    val ink = ColorProvider(Color(0xFFE9E7DA))
    val dim = ColorProvider(Color(0xFFA3A693))
    val accent = ColorProvider(Color(0xFF8FA564))
    val gold = ColorProvider(Color(0xFFCFA956))
}

val HabitIdKey = ActionParameters.Key<String>("habit_id")

class LedgerWidget : GlanceAppWidget() {

    // Glance picks the largest of these that fits the widget as placed, so one widget
    // covers phone cells, the Fold cover screen and the big inner screen.
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, MEDIUM, LARGE, XL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = Store.load(context)
        provideContent { WidgetBody(data) }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 180.dp)
        val LARGE = DpSize(250.dp, 280.dp)
        val XL = DpSize(250.dp, 400.dp)
    }
}

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LedgerWidget()
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        Store.toggle(context, habitId)
        LedgerWidget().updateAll(context)
    }
}

@Composable
private fun WidgetBody(data: LedgerData) {
    val size = LocalSize.current
    val context = LocalContext.current
    val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
    val today = LocalDate.now()
    val done = Logic.doneCount(data, today)
    val total = data.habits.size
    val streak = Logic.overallStreak(data, today)
    val streakText = if (streak > 0) "$streak-day streak" else "no streak yet"

    val root = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(P.bg)
        .cornerRadius(24.dp)
        .padding(14.dp)

    when {
        size.height >= 400.dp -> ListLayout(root, openApp, data, today, done, total, streakText, rows = 8, quoteLines = 3)
        size.height >= 280.dp -> ListLayout(root, openApp, data, today, done, total, streakText, rows = 5, quoteLines = 2)
        size.height >= 180.dp -> ListLayout(root, openApp, data, today, done, total, streakText, rows = 3, quoteLines = 0)
        size.width >= 250.dp -> WideLayout(root, openApp, data, today, done, total, streakText)
        else -> SmallLayout(root, openApp, done, total, streakText)
    }
}

@Composable
private fun SmallLayout(root: GlanceModifier, openApp: Action, done: Int, total: Int, streakText: String) {
    Column(modifier = root.clickable(openApp)) {
        Text(
            text = "LEDGER",
            style = TextStyle(color = P.dim, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "$done/$total",
            style = TextStyle(color = P.accent, fontSize = 34.sp, fontWeight = FontWeight.Bold),
        )
        Text(text = "done today", style = TextStyle(color = P.dim, fontSize = 12.sp))
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = streakText,
            style = TextStyle(color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun WideLayout(
    root: GlanceModifier,
    openApp: Action,
    data: LedgerData,
    today: LocalDate,
    done: Int,
    total: Int,
    streakText: String,
) {
    Row(modifier = root, verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = GlanceModifier.defaultWeight().clickable(openApp),
        ) {
            Text(
                text = "$done/$total",
                style = TextStyle(color = P.accent, fontSize = 34.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = streakText,
                style = TextStyle(color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            val next = Logic.nextUndone(data, today)
            if (next != null) {
                Text(text = "NEXT UP", style = TextStyle(color = P.dim, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                HabitRow(next, done = false, streak = 0, showStreak = false)
            } else if (total > 0) {
                Text(text = "All done", style = TextStyle(color = P.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(text = "Nothing left today", style = TextStyle(color = P.dim, fontSize = 12.sp))
            } else {
                Text(text = "No habits yet", style = TextStyle(color = P.dim, fontSize = 13.sp))
                Text(text = "Tap to add some", style = TextStyle(color = P.dim, fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun ListLayout(
    root: GlanceModifier,
    openApp: Action,
    data: LedgerData,
    today: LocalDate,
    done: Int,
    total: Int,
    streakText: String,
    rows: Int,
    quoteLines: Int,
) {
    Column(modifier = root) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$done/$total",
                style = TextStyle(color = P.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(text = "today", style = TextStyle(color = P.dim, fontSize = 12.sp))
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = streakText,
                style = TextStyle(color = P.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))

        if (data.habits.isEmpty()) {
            Text(
                text = "No habits yet. Tap the header to open the app and add some.",
                style = TextStyle(color = P.dim, fontSize = 13.sp),
            )
        }

        data.habits.take(rows).forEach { habit ->
            HabitRow(
                habit = habit,
                done = Logic.isDone(data, habit.id, today),
                streak = Logic.habitStreak(data, habit.id, today),
                showStreak = true,
            )
        }

        val hidden = data.habits.size - rows
        if (hidden > 0) {
            Text(text = "+$hidden more in the app", style = TextStyle(color = P.dim, fontSize = 11.sp))
        }

        if (quoteLines > 0) {
            val push = Content.items[Content.indexFor()]
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "“${push.quote}”",
                style = TextStyle(color = P.dim, fontSize = 11.sp),
                maxLines = quoteLines,
            )
            Text(
                text = "— ${push.author}",
                style = TextStyle(color = P.dim, fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, done: Boolean, streak: Int, showStreak: Boolean) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to habit.id))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(if (done) R.drawable.box_on else R.drawable.box_off),
            contentDescription = if (done) "Done: ${habit.name}" else "Not done: ${habit.name}",
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Text(
            text = habit.name,
            style = TextStyle(color = if (done) P.dim else P.ink, fontSize = 14.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (showStreak && streak > 0) {
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(text = "${streak}d", style = TextStyle(color = P.gold, fontSize = 12.sp))
        }
    }
}
